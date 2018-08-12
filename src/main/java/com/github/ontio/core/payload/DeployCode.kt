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

import java.io.IOException

import com.github.ontio.common.Address
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter


class DeployCode(
        code: ByteArray,
        needStorage: Boolean,
        name: String,
        versionString: String,
        author: String,
        email: String,
        description: String
) : Transaction(TransactionType.DeployCode) {
    var code: ByteArray = code
        private set
    var needStorage: Boolean = needStorage
        private set
    var name: String = name
        private set
    var versionString: String = versionString
        private set
    var author: String = author
        private set
    var email: String = email
        private set
    var description: String = description
        private set

    override val addressU160ForVerifying: Array<Address>?
        get() = null

    @Throws(IOException::class)
    public override fun deserializeExclusiveData(reader: BinaryReader) {
        try {
            code = reader.readVarBytes()
            needStorage = reader.readBoolean()
            name = reader.readVarString()
            versionString = reader.readVarString()
            author = reader.readVarString()
            email = reader.readVarString()
            description = reader.readVarString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    @Throws(IOException::class)
    public override fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(code)
        writer.writeBoolean(needStorage)
        writer.writeVarString(name)
        writer.writeVarString(versionString)
        writer.writeVarString(author)
        writer.writeVarString(email)
        writer.writeVarString(description)
    }
}
