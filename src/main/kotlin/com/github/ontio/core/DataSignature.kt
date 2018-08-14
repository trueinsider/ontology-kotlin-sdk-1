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

package com.github.ontio.core

import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter

class DataSignature : Signable {
    private lateinit var scheme: SignatureScheme
    private lateinit var account: Account
    lateinit var data: ByteArray
        private set

    override val addressU160ForVerifying: Array<Address>?
        get() = null

    constructor()

    constructor(scheme: SignatureScheme, account: Account, data: ByteArray) {
        this.scheme = scheme
        this.account = account
        this.data = data
    }

    constructor(scheme: SignatureScheme, account: Account, data: String) : this(scheme, account, data.toByteArray())

    fun signature(): ByteArray {
        try {
            return sign(account, scheme)
        } catch (e: Exception) {
            throw RuntimeException(ErrorCode.DataSignatureErr)
        }
    }

    override fun sign(account: Account, scheme: SignatureScheme): ByteArray {
        return account.generateSignature(hashData, scheme, null)
    }

    override fun verifySignature(account: Account, data: ByteArray, signature: ByteArray): Boolean {
        return account.verifySignature(data, signature)
    }

    override fun deserialize(reader: BinaryReader) {
    }

    override fun deserializeUnsigned(reader: BinaryReader) {
    }

    override fun serializeUnsigned(writer: BinaryWriter) {
        writer.write(data)
    }

    override fun serialize(writer: BinaryWriter) {
    }
}
