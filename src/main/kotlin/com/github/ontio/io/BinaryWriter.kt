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

package com.github.ontio.io

import org.bouncycastle.math.ec.ECPoint
import java.io.DataOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryWriter(stream: OutputStream) : AutoCloseable {
    private val writer: DataOutputStream
    private val array = ByteArray(8)
    private val buffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN)

    init {
        this.writer = DataOutputStream(stream)
    }

    override fun close() {
        writer.close()
    }

    fun flush() {
        writer.flush()
    }

    fun write(buffer: ByteArray) {
        writer.write(buffer)
    }

    fun write(buffer: ByteArray, index: Int, length: Int) {
        writer.write(buffer, index, length)
    }

    fun writeBoolean(v: Boolean) {
        writer.writeBoolean(v)
    }

    fun writeByte(v: Byte) {
        writer.writeByte(v.toInt())
    }

    fun writeDouble(v: Double) {
        buffer.putDouble(0, v)
        writer.write(array, 0, 8)
    }

    fun writeECPoint(v: ECPoint) {
        writer.write(v.getEncoded(true))
    }

    fun writeFixedString(v: String, length: Int) {
        if (v.length > length) {
            throw IllegalArgumentException()
        }
        val bytes = v.toByteArray(charset("UTF-8"))
        if (bytes.size > length) {
            throw IllegalArgumentException()
        }
        writer.write(bytes)
        if (bytes.size < length) {
            writer.write(ByteArray(length - bytes.size))
        }
    }

    fun writeFloat(v: Float) {
        buffer.putFloat(0, v)
        writer.write(array, 0, 4)
    }

    fun writeInt(v: Int) {
        buffer.putInt(0, v)
        writer.write(array, 0, 4)
    }

    fun writeLong(v: Long) {
        buffer.putLong(0, v)
        writer.write(array, 0, 8)
    }

    fun writeSerializable(v: Serializable) {
        v.serialize(this)
    }

    fun writeSerializableArray(v: Array<out Serializable>) {
        writeVarInt(v.size.toLong())
        for (i in v.indices) {
            v[i].serialize(this)
        }
    }

    fun writeSerializableArray2(v: Array<Serializable>) {
        writeInt(v.size)
        for (i in v.indices) {
            v[i].serialize(this)
        }
    }

    fun writeShort(v: Short) {
        buffer.putShort(0, v)
        writer.write(array, 0, 2)
    }

    fun writeVarBytes(v: ByteArray) {
        writeVarInt(v.size.toLong())
        writer.write(v)
    }

    fun writeVarInt(v: Long) {
        if (v < 0) {
            throw IllegalArgumentException()
        }
        if (v < 0xFD) {
            writeByte(v.toByte())
        } else if (v <= 0xFFFF) {
            writeByte(0xFD.toByte())
            writeShort(v.toShort())
        } else if (v <= 0xFFFFFFFFL) {
            writeByte(0xFE.toByte())
            writeInt(v.toInt())
        } else {
            writeByte(0xFF.toByte())
            writeLong(v)
        }
    }

    fun writeVarString(v: String) {
        writeVarBytes(v.toByteArray(charset("UTF-8")))
    }
}
