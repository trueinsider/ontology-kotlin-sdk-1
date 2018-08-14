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

package com.github.ontio.smartcontract.neovm

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk.addSign
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.account.Account
import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.NeoVm.sendTransaction
import com.github.ontio.smartcontract.Vm.makeInvokeCodeTransaction
import com.github.ontio.smartcontract.neovm.abi.AbiInfo
import com.github.ontio.smartcontract.neovm.abi.BuildParams
import java.io.ByteArrayInputStream

object ClaimRecord {
    var contractAddress: String = "36bb5c053b6b839c8f6b923fe852f91239b9fccc"
        set(codeHash) {
            field = codeHash.replace("0x", "")
        }

    private val abi = "{\"hash\":\"0x36bb5c053b6b839c8f6b923fe852f91239b9fccc\",\"entrypoint\":\"Main\",\"functions\":[{\"name\":\"Main\",\"parameters\":[{\"name\":\"operation\",\"type\":\"String\"},{\"name\":\"args\",\"type\":\"Array\"}],\"returntype\":\"Any\"},{\"name\":\"Commit\",\"parameters\":[{\"name\":\"claimId\",\"type\":\"ByteArray\"},{\"name\":\"commiterId\",\"type\":\"ByteArray\"},{\"name\":\"ownerId\",\"type\":\"ByteArray\"}],\"returntype\":\"Boolean\"},{\"name\":\"Revoke\",\"parameters\":[{\"name\":\"claimId\",\"type\":\"ByteArray\"},{\"name\":\"ontId\",\"type\":\"ByteArray\"}],\"returntype\":\"Boolean\"},{\"name\":\"GetStatus\",\"parameters\":[{\"name\":\"claimId\",\"type\":\"ByteArray\"}],\"returntype\":\"ByteArray\"}],\"events\":[{\"name\":\"ErrorMsg\",\"parameters\":[{\"name\":\"id\",\"type\":\"ByteArray\"},{\"name\":\"error\",\"type\":\"String\"}],\"returntype\":\"Void\"},{\"name\":\"Push\",\"parameters\":[{\"name\":\"id\",\"type\":\"ByteArray\"},{\"name\":\"msg\",\"type\":\"String\"},{\"name\":\"args\",\"type\":\"ByteArray\"}],\"returntype\":\"Void\"}]}"

    /**
     *
     * @param issuerOntid
     * @param password
     * @param subjectOntid
     * @param claimId
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendCommit(issuerOntid: String, password: String, salt: ByteArray, subjectOntid: String, claimId: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (issuerOntid.isEmpty() || password.isEmpty() || subjectOntid.isEmpty() || claimId.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("gaslimit or gasprice is less than 0"))
        }
        val addr = issuerOntid.replace(Common.didont, "")
        val tx = makeCommit(issuerOntid, subjectOntid, claimId, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, addr, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param issuerOntid
     * @param subjectOntid
     * @param claimId
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeCommit(issuerOntid: String, subjectOntid: String, claimId: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (issuerOntid.isEmpty() || subjectOntid.isEmpty() || payer.isEmpty() || claimId.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("gaslimit or gasprice is less than 0"))
        }

        val abiinfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "Commit"
        val func = abiinfo.getFunction(name)
        func!!.name = name
        func.setParamsValue(claimId.toByteArray(), issuerOntid.toByteArray(), subjectOntid.toByteArray())
        val params = BuildParams.serializeAbiFunction(func)
        return makeInvokeCodeTransaction(Helper.reverse(contractAddress), null, params, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param issuerOntid
     * @param password
     * @param claimId
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendRevoke(issuerOntid: String, password: String, salt: ByteArray, claimId: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (issuerOntid.isEmpty() || password.isEmpty() || claimId.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("gaslimit or gasprice is less than 0"))
        }
        val addr = issuerOntid.replace(Common.didont, "")
        val tx = makeRevoke(issuerOntid, claimId, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, addr, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    fun makeRevoke(issuerOntid: String, claimId: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        val abiinfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "Revoke"
        val func = abiinfo.getFunction(name)
        func!!.name = name
        func.setParamsValue(claimId.toByteArray(), issuerOntid.toByteArray())
        val params = BuildParams.serializeAbiFunction(func)
        return makeInvokeCodeTransaction(Helper.reverse(contractAddress), null, params, payer, gaslimit, gasprice)
    }

    fun sendGetStatus(claimId: String): String {
        if (claimId.isEmpty()) {
            throw SDKException(ErrorCode.NullKeyOrValue)
        }
        val abiinfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "GetStatus"
        val func = abiinfo.getFunction(name)
        func!!.name = name
        func.setParamsValue(claimId.toByteArray())
        val obj = sendTransaction(Helper.reverse(this.contractAddress), null, null, 0, 0, func, true)
        val res = (obj as JSONObject).getString("Result")
        if (res.isEmpty()) {
            return res
        }
        val bais = ByteArrayInputStream(Helper.hexToBytes(res))
        val br = BinaryReader(bais)
        val claimTx = ClaimTx.deserializeFrom(br)
        return if (claimTx.status.isEmpty()) {
            String(claimTx.claimId) + "." + "00" + "." + String(claimTx.issuerOntId) + "." + String(claimTx.subjectOntId)
        } else String(claimTx.claimId) + "." + Helper.toHexString(claimTx.status) + "." + String(claimTx.issuerOntId) + "." + String(claimTx.subjectOntId)
    }
}

internal class ClaimTx : Serializable {
    lateinit var claimId: ByteArray
        private set
    lateinit var issuerOntId: ByteArray
        private set
    lateinit var subjectOntId: ByteArray
        private set
    lateinit var status: ByteArray
        private set

    private constructor()

    constructor(claimId: ByteArray, issuerOntId: ByteArray, subjectOntId: ByteArray, status: ByteArray) {
        this.claimId = claimId
        this.issuerOntId = issuerOntId
        this.subjectOntId = subjectOntId
        this.status = status
    }

    override fun deserialize(reader: BinaryReader) {
        reader.readByte()
        reader.readVarInt()
        reader.readByte()
        this.claimId = reader.readVarBytes()
        reader.readByte()
        this.issuerOntId = reader.readVarBytes()
        reader.readByte()
        this.subjectOntId = reader.readVarBytes()
        reader.readByte()
        this.status = reader.readVarBytes()
    }

    override fun serialize(writer: BinaryWriter) {
    }

    companion object {
        fun deserializeFrom(reader: BinaryReader): ClaimTx {
            val claimTx = ClaimTx()
            claimTx.deserialize(reader)
            return claimTx
        }
    }
}
