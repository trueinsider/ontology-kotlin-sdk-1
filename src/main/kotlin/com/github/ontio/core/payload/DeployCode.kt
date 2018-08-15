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

package com.github.ontio.core.payload

import com.github.ontio.common.Address
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter


class DeployCode : Transaction {
    lateinit var code: ByteArray
        private set
    var needStorage: Boolean = false
        private set
    lateinit var name: String
        private set
    lateinit var versionString: String
        private set
    lateinit var author: String
        private set
    lateinit var email: String
        private set
    lateinit var description: String
        private set

    private constructor() : super(TransactionType.DeployCode)

    constructor(code: ByteArray, needStorage: Boolean, name: String, versionString: String, author: String, email: String, description: String) : super(TransactionType.DeployCode) {
        this.code = code
        this.needStorage = needStorage
        this.name = name
        this.versionString = versionString
        this.author = author
        this.email = email
        this.description = description
    }

    override val addressU160ForVerifying: Array<Address>?
        get() = null

    public override fun deserializeExclusiveData(reader: BinaryReader) {
        code = reader.readVarBytes()
        needStorage = reader.readBoolean()
        name = reader.readVarString()
        versionString = reader.readVarString()
        author = reader.readVarString()
        email = reader.readVarString()
        description = reader.readVarString()
    }

    public override fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(code)
        writer.writeBoolean(needStorage)
        writer.writeVarString(name)
        writer.writeVarString(versionString)
        writer.writeVarString(author)
        writer.writeVarString(email)
        writer.writeVarString(description)
    }

    companion object {
        fun deserializeFrom(reader: BinaryReader): DeployCode {
            val deployCode = DeployCode()
            deployCode.deserializeExclusiveData(reader)
            return deployCode
        }
    }
}
