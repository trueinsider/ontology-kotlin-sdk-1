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

package com.github.ontio.common

import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

/**
 * Custom type base abstract class, it defines the storage and the serialization
 * and deserialization of actual data
 */
abstract class UIntBase protected constructor(bytes: Int, value: ByteArray?) : Serializable {
    protected var data_bytes: ByteArray

    init {
        if (value == null) {
            this.data_bytes = ByteArray(bytes)
        } else {
            if (value.size != bytes) {
                throw IllegalArgumentException()
            }
            this.data_bytes = value
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        if (other === this) {
            return true
        }
        if (other !is UIntBase) {
            return false
        }
        return Arrays.equals(this.data_bytes, other.data_bytes)
    }

    override fun hashCode(): Int {
        return ByteBuffer.wrap(data_bytes).order(ByteOrder.LITTLE_ENDIAN).int
    }

    override fun toArray(): ByteArray {
        return data_bytes
    }

    /**
     * 转为16进制字符串
     *
     * @return 返回16进制字符串
     */
    override fun toString(): String {
        return Helper.toHexString(Helper.reverse(data_bytes))
    }

    override fun toHexString(): String {
        return Helper.reverse(Helper.toHexString(toArray()))
    }

    override fun serialize(writer: BinaryWriter) {
        writer.write(data_bytes)
    }

    override fun deserialize(reader: BinaryReader) {
        reader.read(data_bytes)
    }
}
