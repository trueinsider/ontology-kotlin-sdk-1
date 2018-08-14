/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.core.scripts

import com.github.ontio.common.Helper
import com.github.ontio.common.UIntBase
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 */
class ScriptBuilder : AutoCloseable {
    private val ms = ByteArrayOutputStream()

    fun add(op: ScriptOp?): ScriptBuilder {
        return add(op!!.byte)
    }

    fun add(op: Byte): ScriptBuilder {
        ms.write(op.toInt())
        return this
    }

    fun add(script: ByteArray): ScriptBuilder {
        ms.write(script, 0, script.size)
        return this
    }

    override fun close() {
        try {
            ms.close()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }

    fun emitPushBool(b: Boolean): ScriptBuilder {
        return if (b) {
            add(ScriptOp.OP_PUSH1)
        } else add(ScriptOp.OP_PUSH0)
    }

    fun emitPushInteger(number: BigInteger): ScriptBuilder {
        if (number == BigInteger.ONE.negate()) {
            return add(ScriptOp.OP_PUSHM1)
        }
        if (number == BigInteger.ZERO) {
            return add(ScriptOp.OP_PUSH0)
        }
        if (number > BigInteger.ZERO && number <= BigInteger.valueOf(16)) {
            return add((ScriptOp.OP_PUSH1.byte - 1 + number.toByte()).toByte())
        }
        val bytes = Helper.BigIntToNeoBytes(number)
        return emitPushByteArray(bytes)
    }

    fun pushNum(num: Short): ScriptBuilder {
        if (num.toInt() == 0) {
            return add(ScriptOp.OP_PUSH0)
        } else if (num < 16) {
            return add(ScriptOp.valueOf(num - 1 + ScriptOp.OP_PUSH1.byte))
        }
        val bint = BigInteger.valueOf(num.toLong())
        return emitPushByteArray(Helper.BigIntToNeoBytes(bint))
    }


    fun emitPushByteArray(data: ByteArray): ScriptBuilder {
        when {
            data.size <= ScriptOp.OP_PUSHBYTES75.byte.toInt() -> {
                ms.write(data.size.toByte().toInt())
                ms.write(data, 0, data.size)
            }
            data.size < 0x100 -> {
                add(ScriptOp.OP_PUSHDATA1)
                ms.write(data.size.toByte().toInt())
                ms.write(data, 0, data.size)
            }
            data.size < 0x10000 -> {
                add(ScriptOp.OP_PUSHDATA2)
                ms.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(data.size.toShort()).array(), 0, 2)
                ms.write(data, 0, data.size)
            }
            data.size < 0x100000000L -> {
                add(ScriptOp.OP_PUSHDATA4)
                ms.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(data.size).array(), 0, 4)
                ms.write(data, 0, data.size)
            }
            else -> throw IllegalArgumentException()
        }
        return this
    }

    fun emit(op: ScriptOp): ScriptBuilder {
        return add(op.byte)
    }

    fun push(hash: UIntBase): ScriptBuilder {
        return emitPushByteArray(hash.toArray())
    }

    fun pushPack(): ScriptBuilder {
        return add(ScriptOp.OP_PACK)
    }

    fun toArray(): ByteArray {
        return ms.toByteArray()
    }
}
