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

import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*

/**
 *
 */
class Transfers : Serializable {
    lateinit var states: Array<State>
        private set

    private constructor()

    constructor(states: Array<State>) {
        this.states = states
    }

    override fun deserialize(reader: BinaryReader) {
        val len = reader.readVarInt().toInt()
        states = Array(len) { reader.readSerializable(State::class.java)}
    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeSerializableArray(states)
    }

    fun json(): Any {
        val json = HashMap<Any, Any>()
        val list = ArrayList<Any>()
        for (i in states.indices) {
            list.add(states[i].json())
        }
        json["States"] = list
        return json
    }

    companion object {
        fun deserializeFrom(value: ByteArray): Transfers {
            try {
                val offset = 0
                val ms = ByteArrayInputStream(value, offset, value.size - offset)
                val reader = BinaryReader(ms)
                val transfers = Transfers()
                transfers.deserialize(reader)
                return transfers
            } catch (ex: IOException) {
                throw IOException(ex)
            }
        }
    }
}
