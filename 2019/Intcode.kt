package com.github.petertrr

enum class ParameterMode(private val code: Int) {
    POSITION(0),
    IMMEDIATE(1)
    ;
    
    companion object {
        fun from(code: Int): ParameterMode = ParameterMode.values().find { it.code == code } ?: error("Unknown parameter mode $code")
    }
}

enum class OpcodeType(private val code: Int, val numParams: Int) {
    ADD(1, 3),
    MULT(2, 3),
    READ(3, 1),
    WRITE(4, 1),
    HALT(99, 1)
    ;
    
    companion object {
        fun from(code: Int): OpcodeType = OpcodeType.values().find { it.code == code } ?: error("Unknown opcode $code")
    }
}

class Opcode(code: Int) {
    fun Int.digits() = mutableListOf(this % 10)
        .apply {
            var num: Int = this@digits
            while (num / 10 > 0) {
                num /= 10
                add(num % 10)
            }
        }
            .reversed()
            .toList()
    
    private val codes: Pair<OpcodeType, List<ParameterMode>> by lazy {
        val digits = code.digits()
        val opcode = OpcodeType.from(digits.getOrElse(digits.size - 2) { 0 } * 10 + digits.last())
        opcode to digits
            .dropLast(2)
            .toMutableList()
            .apply {
                while (size != opcode.numParams) add(0, 0)
            }
            .map(ParameterMode::from)
            .reversed()
    }
    
    val opcodeType = codes.first
    val parameterModes = codes.second
}

sealed class Operation(private val opcodeType: OpcodeType) {
    abstract fun execute(position: Int, register: MutableList<Int>): Int
    
    class Halt : Operation(OpcodeType.HALT) {
        override fun execute(position: Int, register: MutableList<Int>) = position + 1
    }
    
    class Add(private val parameterModes: List<ParameterMode>): Operation(OpcodeType.ADD) {
        override fun execute(position: Int, register: MutableList<Int>): Int {
            register[register[position + 3]] =
                register.parameterAt(position + 1, parameterModes[0]) + register.parameterAt(position + 2, parameterModes[1])
            return position + 4
        }
    }
    
    class Mult(private val parameterModes: List<ParameterMode>): Operation(OpcodeType.MULT) {
        override fun execute(position: Int, register: MutableList<Int>): Int {
            register[register[position + 3]] =
                register.parameterAt(position + 1, parameterModes[0]) * register.parameterAt(position + 2, parameterModes[1])
            return position + 4
        }
    }
    
    class Read(private val inputs: MutableList<Int>): Operation(OpcodeType.READ) {
        override fun execute(position: Int, register: MutableList<Int>): Int {
            register[register[position + 1]] = inputs.removeLast()
            return position + 2
        }
    }
    
    class Write(private val parameterMode: ParameterMode, private val outputs: MutableList<Int>): Operation(OpcodeType.WRITE) {
        override fun execute(position: Int, register: MutableList<Int>): Int {
            outputs.add(register.parameterAt(position + 1, parameterMode))
            return position + 2
        }
    }

    fun MutableList<Int>.parameterAt(index: Int, parameterMode: ParameterMode) = let { register ->
        when (parameterMode) {
            ParameterMode.POSITION -> register[register[index]]
            ParameterMode.IMMEDIATE -> register[index]
        }
    }
}

class Intcode(private val register: MutableList<Int>) {
    val inputs = mutableListOf<Int>()
    val outputs = mutableListOf<Int>()
    
    fun runProgram() {
        var position = 0
        while (true) {
            val opcode = register[position].let(::Opcode)
            val operation = when (opcode.opcodeType) {
                OpcodeType.HALT -> break
                OpcodeType.ADD -> Operation.Add(opcode.parameterModes)
                OpcodeType.MULT -> Operation.Mult(opcode.parameterModes)
                OpcodeType.READ -> Operation.Read(inputs)
                OpcodeType.WRITE -> Operation.Write(opcode.parameterModes.single(), outputs)
            }
            position = operation.execute(position, register)
        }
    }
    
    fun setMemoryAt(index: Int, value: Int) {
        register[index] = value
    }
    
    fun memoryAt(index: Int) = register[index]
    
    fun output() = memoryAt(0)
    
    companion object {
        fun parseInput(input: String): Intcode {
            return Intcode(
                input.split(',').map { it.toInt() }.toMutableList()
            )
        }
    }
}
