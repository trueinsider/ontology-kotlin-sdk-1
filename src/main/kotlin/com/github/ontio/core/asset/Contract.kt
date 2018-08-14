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

package com.github.ontio.core.asset

import com.github.ontio.common.Address
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import java.io.ByteArrayInputStream
import java.io.IOException

/**
 *
 */
class Contract : Serializable {
    var version: Byte = 0
    lateinit var constracHash: Address
    lateinit var method: String
    lateinit var args: ByteArray

    private constructor()

    constructor(version: Byte, constracHash: Address, method: String, args: ByteArray) {
        this.version = version
        this.constracHash = constracHash
        this.method = method
        this.args = args
    }

    override fun deserialize(reader: BinaryReader) {
        version = reader.readByte()
        constracHash = reader.readSerializable(Address::class.java)
        method = String(reader.readVarBytes())
        args = reader.readVarBytes()
    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeByte(version)
        writer.writeSerializable(constracHash)
        writer.writeVarBytes(method.toByteArray())
        writer.writeVarBytes(args)
    }

    companion object {
        fun deserializeFrom(value: ByteArray): Contract {
            try {
                val offset = 0
                val ms = ByteArrayInputStream(value, offset, value.size - offset)
                val reader = BinaryReader(ms)
                val contract = Contract()
                contract.deserialize(reader)
                return contract
            } catch (ex: IOException) {
                throw IOException(ex)
            }

        }
    }
}
