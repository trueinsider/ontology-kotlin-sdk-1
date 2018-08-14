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
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.NeoVm.sendTransaction
import com.github.ontio.smartcontract.Vm.makeInvokeCodeTransaction
import com.github.ontio.smartcontract.neovm.abi.AbiInfo
import com.github.ontio.smartcontract.neovm.abi.BuildParams

/**
 *
 */
class Nep5(contractAddress: String) {
    val contractAddress = contractAddress.replace("0x", "")

    private val nep5abi = "{\"hash\":\"0xd17d91a831c094c1fd8d8634b8cd6fa9fbaedc99\",\"entrypoint\":\"Main\"," +
            "\"functions\":[{\"name\":\"Name\",\"parameters\":[],\"returntype\":\"String\"}," +
            "{\"name\":\"Symbol\",\"parameters\":[],\"returntype\":\"String\"}," +
            "{\"name\":\"Decimals\",\"parameters\":[],\"returntype\":\"Integer\"},{\"name\":\"Main\",\"parameters\":[{\"name\":\"operation\",\"type\":\"String\"}," +
            "{\"name\":\"args\",\"type\":\"Array\"}],\"returntype\":\"Any\"}," +
            "{\"name\":\"Init\",\"parameters\":[],\"returntype\":\"Boolean\"}," +
            "{\"name\":\"TotalSupply\",\"parameters\":[],\"returntype\":\"Integer\"}," +
            "{\"name\":\"Transfer\",\"parameters\":[{\"name\":\"from\",\"type\":\"ByteArray\"},{\"name\":\"to\",\"type\":\"ByteArray\"},{\"name\":\"value\",\"type\":\"Integer\"}],\"returntype\":\"Boolean\"}," +
            "{\"name\":\"BalanceOf\",\"parameters\":[{\"name\":\"address\",\"type\":\"ByteArray\"}],\"returntype\":\"Integer\"}]," +
            "\"events\":[{\"name\":\"transfer\",\"parameters\":[{\"name\":\"arg1\",\"type\":\"ByteArray\"},{\"name\":\"arg2\",\"type\":\"ByteArray\"},{\"name\":\"arg3\",\"type\":\"Integer\"}],\"returntype\":\"Void\"}]}"

    fun sendInit(acct: Account, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        return sendInit(acct, payerAcct, gaslimit, gasprice, false) as String
    }

    fun sendInitPreExec(acct: Account, payerAcct: Account, gaslimit: Long, gasprice: Long): Long {
        return sendInit(acct, payerAcct, gaslimit, gasprice, true) as Long
    }

    private fun sendInit(acct: Account?, payerAcct: Account?, gaslimit: Long, gasprice: Long, preExec: Boolean): Any? {
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("Init")
        func!!.name = "init"
        if (preExec) {
            val params = BuildParams.serializeAbiFunction(func)
            val tx = makeInvokeCodeTransaction(contractAddress, null, params, null, 0, 0)
            if (acct != null) {
                signTx(tx, arrayOf(arrayOf(acct)))
            }
            val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
            if (Integer.parseInt((obj as JSONObject).getString("Result")) != 1) {
                throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: $obj"))
            }
            return obj.getLong("Gas")
        }
        if (acct == null || payerAcct == null) {
            throw SDKException(ErrorCode.ParamError)
        }
        return sendTransaction(contractAddress, acct, payerAcct, gaslimit, gasprice, func, preExec)
    }

    /**
     *
     * @param acct
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendTransfer(acct: Account, recvAddr: String, amount: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        return sendTransfer(acct, recvAddr, amount, payerAcct, gaslimit, gasprice, false) as String
    }

    fun sendTransferPreExec(acct: Account, recvAddr: String, amount: Long): Long {
        return sendTransfer(acct, recvAddr, amount, acct, 0, 0, true) as Long
    }

    private fun sendTransfer(acct: Account, recvAddr: String, amount: Long, payerAcct: Account, gaslimit: Long, gasprice: Long, preExec: Boolean): Any? {
        val sendAddr = acct.addressU160.toBase58()
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("Transfer")
        func!!.name = "transfer"
        func.setParamsValue(Address.decodeBase58(sendAddr).toArray(), Address.decodeBase58(recvAddr).toArray(), amount)
        if (preExec) {
            val params = BuildParams.serializeAbiFunction(func)

            val tx = makeInvokeCodeTransaction(contractAddress, null, params, null, 0, 0)
            signTx(tx, arrayOf(arrayOf(acct)))
            val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
            if (Integer.parseInt((obj as JSONObject).getString("Result")) != 1) {
                throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: $obj"))
            }
            return obj.getLong("Gas")
        }
        return sendTransaction(contractAddress, acct, payerAcct, gaslimit, gasprice, func, preExec)
    }

    fun makeTransfer(sendAddr: String, recvAddr: String, amount: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): Transaction {
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("Transfer")
        func!!.name = "transfer"
        func.setParamsValue(Address.decodeBase58(sendAddr).toArray(), Address.decodeBase58(recvAddr).toArray(), amount)
        val params = BuildParams.serializeAbiFunction(func)
        val payer = payerAcct.addressU160.toBase58()
        return makeInvokeCodeTransaction(contractAddress, null, params, payer, gaslimit, gasprice)
    }

    fun queryBalanceOf(addr: String): String {
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("BalanceOf")
        func!!.name = "balanceOf"
        func.setParamsValue(Address.decodeBase58(addr).toArray())
        val obj = sendTransaction(this.contractAddress, null, null, 0, 0, func, true)
        var balance = (obj as JSONObject).getString("Result")
        if (balance.isEmpty()) {
            balance = "00"
        }
        return balance
    }

    fun queryTotalSupply(): String {
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("TotalSupply")
        func!!.name = "totalSupply"
        func.setParamsValue()
        val obj = sendTransaction(this.contractAddress, null, null, 0, 0, func, true)
        return (obj as JSONObject).getString("Result")
    }

    fun queryName(): String {
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("Name")
        func!!.name = "name"
        func.setParamsValue()
        val obj = sendTransaction(this.contractAddress, null, null, 0, 0, func, true)
        return (obj as JSONObject).getString("Result")
    }

    fun queryDecimals(): String {
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("Decimals")
        func!!.name = "decimals"
        func.setParamsValue()
        val obj = sendTransaction(this.contractAddress, null, null, 0, 0, func, true)
        return (obj as JSONObject).getString("Result")
    }

    fun querySymbol(): String {
        val abiinfo = JSON.parseObject(nep5abi, AbiInfo::class.java)
        val func = abiinfo.getFunction("Symbol")
        func!!.name = "symbol"
        func.setParamsValue()
        val obj = sendTransaction(this.contractAddress, null, null, 0, 0, func, true)
        return (obj as JSONObject).getString("Result")
    }


}
