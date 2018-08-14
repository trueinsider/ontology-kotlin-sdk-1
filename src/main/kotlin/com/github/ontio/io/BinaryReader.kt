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

import com.github.ontio.common.ErrorCode
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.crypto.ECC
import org.bouncycastle.math.ec.ECPoint
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
 ******************************************************************************
 * public func for outside calling
 ******************************************************************************
 * 1. WriteVarUint func, depend on the inpute number's Actual number size,
 *    serialize to bytes.
 *      uint8  =>  (LittleEndian)num in 1 byte                 = 1bytes
 *      uint16 =>  0xfd(1 byte) + (LittleEndian)num in 2 bytes = 3bytes
 *      uint32 =>  0xfe(1 byte) + (LittleEndian)num in 4 bytes = 5bytes
 *      uint64 =>  0xff(1 byte) + (LittleEndian)num in 8 bytes = 9bytes
 * 2. ReadVarUint  func, this func will read the first byte to determined
 *    the num length to read.and retrun the uint64
 *      first byte = 0xfd, read the next 2 bytes as uint16
 *      first byte = 0xfe, read the next 4 bytes as uint32
 *      first byte = 0xff, read the next 8 bytes as uint64
 *      other else,        read this byte as uint8
 * 3. WriteVarBytes func, this func will output two item as serialization.
 *      length of bytes (uint8/uint16/uint32/uint64)  +  bytes
 * 4. WriteString func, this func will output two item as serialization.
 *      length of string(uint8/uint16/uint32/uint64)  +  bytes(string)
 * 5. ReadVarBytes func, this func will first read a uint to identify the
 *    length of bytes, and use it to get the next length's bytes to return.
 * 6. ReadString func, this func will first read a uint to identify the
 *    length of string, and use it to get the next bytes as a string.
 * 7. GetVarUintSize func, this func will return the length of a uint when it
 *    serialized by the WriteVarUint func.
 * 8. ReadBytes func, this func will read the specify lenth's bytes and retun.
 * 9. ReadUint8,16,32,64 read uint with fixed length
 * 10.WriteUint8,16,32,64 Write uint with fixed length
 * 11.ToArray SerializableData to ToArray() func.
 ******************************************************************************
 */
class BinaryReader(stream: InputStream) : AutoCloseable {
    private val reader: DataInputStream = DataInputStream(stream)
    private val array = ByteArray(8)
    private val buffer = ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN)

    override fun close() {
        reader.close()
    }

    fun read(buffer: ByteArray) {
        reader.readFully(buffer)
    }

    fun read(buffer: ByteArray, index: Int, length: Int) {
        reader.readFully(buffer, index, length)
    }

    fun readBoolean(): Boolean {
        return reader.readBoolean()
    }

    fun readByte(): Byte {
        return reader.readByte()
    }

    fun readBytes(count: Int): ByteArray {
        val buffer = ByteArray(count)
        reader.readFully(buffer)
        return buffer
    }

    fun readDouble(): Double {
        reader.readFully(array, 0, 8)
        return buffer.getDouble(0)
    }

    fun readECPoint(): ECPoint {
        val encoded: ByteArray
        val fb = reader.readByte()
        when (fb.toInt()) {
            0x00 -> encoded = ByteArray(1)
            0x02, 0x03 -> {
                encoded = ByteArray(33)
                encoded[0] = fb
                reader.readFully(encoded, 1, 32)
            }
            0x04 -> {
                encoded = ByteArray(65)
                encoded[0] = fb
                reader.readFully(encoded, 1, 64)
            }
            else -> throw IOException(ErrorCode.ParamError)
        }
        return ECC.secp256r1.curve.decodePoint(encoded)
    }

    fun readFixedString(length: Int): String {
        val data = readBytes(length)
        var count = -1
        while (data[++count].toInt() != 0);
        return String(data, 0, count)
    }

    fun readFloat(): Float {
        reader.readFully(array, 0, 4)
        return buffer.getFloat(0)
    }

    fun readInt(): Int {
        reader.readFully(array, 0, 4)
        return buffer.getInt(0)
    }

    fun readLong(): Long {
        reader.readFully(array, 0, 8)
        return buffer.getLong(0)
    }

    fun <T : Serializable> readSerializable(t: Class<T>): T {
        val obj = t.newInstance()
        obj.deserialize(this)
        return obj
    }

    fun <T : Serializable> readSerializableArray(t: Class<T>): Array<T> {
        val array = java.lang.reflect.Array.newInstance(t, readVarInt(0x10000000).toInt()) as Array<T>
        for (i in array.indices) {
            array[i] = t.newInstance()
            array[i].deserialize(this)
        }
        return array
    }

    fun readShort(): Short {
        reader.readFully(array, 0, 2)
        return buffer.getShort(0)
    }

    fun readVarBytes2(): ByteArray {
        return readBytes(readVarInt2(0X7fffffc7).toInt())
    }

    fun readVarBytes(max: Int = 0X7fffffc7): ByteArray {
        return readBytes(readVarInt(max.toLong()).toInt())
    }

    fun readVarInt(max: Long = Long.MAX_VALUE): Long {
        val fb = java.lang.Byte.toUnsignedLong(readByte())
        val value: Long
        value = when (fb) {
            0xFDL -> java.lang.Short.toUnsignedLong(readShort())
            0xFEL -> Integer.toUnsignedLong(readInt())
            0xFFL -> readLong()
            else -> fb
        }
        if (java.lang.Long.compareUnsigned(value, max) > 0) {
            throw IOException(ErrorCode.ParamError)
        }
        return value
    }

    fun readVarInt2(max: Long): Long {
        val fb = java.lang.Byte.toUnsignedLong(readByte())
        val value: Long
        value = when (fb) {
            ScriptOp.OP_PUSHDATA1.byte.toLong() -> java.lang.Byte.toUnsignedLong(readByte())
            ScriptOp.OP_PUSHDATA2.byte.toLong() -> java.lang.Short.toUnsignedLong(readShort())
            ScriptOp.OP_PUSHDATA4.byte.toLong() -> Integer.toUnsignedLong(readInt())
            else -> fb
        }
        if (java.lang.Long.compareUnsigned(value, max) > 0) {
            throw IOException(ErrorCode.ParamError)
        }
        return value
    }

    fun readVarString(): String {
        return String(readVarBytes())
    }

    fun available(): Int {
        return reader.available()
    }

    fun Seek(n: Long): Long {
        reader.reset()
        return reader.skip(n)
    }
}
