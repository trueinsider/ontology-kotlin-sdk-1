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

import com.github.ontio.common.Helper
import com.github.ontio.core.program.Program
import com.github.ontio.core.program.Program.ProgramFromMultiPubKey
import com.github.ontio.core.program.Program.ProgramFromParams
import com.github.ontio.core.program.Program.ProgramFromPubKey
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import java.util.*

/**
 *
 */
class Sig : Serializable {
    var M: Int = 0
    lateinit var pubKeys: Array<ByteArray>
    lateinit var sigData: Array<ByteArray>

    internal constructor()

    constructor(M: Int, pubKeys: Array<ByteArray>, sigData: Array<ByteArray>) {
        this.M = M
        this.pubKeys = pubKeys
        this.sigData = sigData
    }

    override fun deserialize(reader: BinaryReader) {
        val invocationScript = reader.readVarBytes()
        val verificationScript = reader.readVarBytes()
        sigData = Program.getParamInfo(invocationScript)
        val info = Program.getProgramInfo(verificationScript)!!
        pubKeys = info.publicKey
        M = info.m.toInt()
    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(ProgramFromParams(sigData))
        if (pubKeys.size == 1) {
            writer.writeVarBytes(ProgramFromPubKey(pubKeys[0]))
        } else if (pubKeys.size > 1) {
            writer.writeVarBytes(ProgramFromMultiPubKey(M, *pubKeys))
        }
    }

    fun json(): Any {
        val json = HashMap<Any, Any>()
        json["M"] = M
        val list = mutableListOf<String>()
        for (i in pubKeys.indices) {
            list.add(Helper.toHexString(pubKeys[i]))
        }
        val list2 = mutableListOf<String>()
        for (i in sigData.indices) {
            list2.add(Helper.toHexString(sigData[i]))
        }
        json["PubKeys"] = list
        json["SigData"] = list2
        return json
    }
}
