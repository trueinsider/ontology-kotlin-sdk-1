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

package com.github.ontio.core.governance

import com.alibaba.fastjson.JSON
import com.github.ontio.common.Address
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

class VoteInfo(
        var peerPubkey: String,
        var address: Address,
        var consensusPos: Long,
        var freezePos: Long,
        var newPos: Long,
        var withdrawPos: Long,
        var withdrawFreezePos: Long,
        var withdrawUnfreezePos: Long
) : Serializable {
    override fun deserialize(reader: BinaryReader) {
        this.peerPubkey = reader.readVarString()
        this.address = reader.readSerializable(Address::class.java)
        this.consensusPos = reader.readLong()
        this.freezePos = reader.readLong()
        this.newPos = reader.readLong()
        this.withdrawPos = reader.readLong()
        this.withdrawFreezePos = reader.readLong()
        this.withdrawUnfreezePos = reader.readLong()
    }

    override fun serialize(writer: BinaryWriter) {
    }

    fun json(): String {
        val map = mutableMapOf<String, Any>()
        map["peerPubkey"] = peerPubkey
        map["address"] = address.toBase58()
        map["consensusPos"] = consensusPos
        map["freezePos"] = freezePos
        map["newPos"] = newPos
        map["withdrawPos"] = withdrawPos
        map["withdrawFreezePos"] = withdrawFreezePos
        map["withdrawUnfreezePos"] = withdrawUnfreezePos
        return JSON.toJSONString(map)
    }
}
