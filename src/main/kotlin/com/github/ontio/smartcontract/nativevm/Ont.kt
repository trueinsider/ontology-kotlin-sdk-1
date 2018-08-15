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

package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk.addMultiSign
import com.github.ontio.OntSdk.addSign
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.asset.State
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.Vm.buildNativeParams
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct

/**
 *
 */
object Ont {
    const val contractAddress = "0000000000000000000000000000000000000001"

    /**
     *
     * @param sendAcct
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendTransfer(sendAcct: Account, recvAddr: String, amount: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }

        val tx = makeTransfer(sendAcct.addressU160.toBase58(), recvAddr, amount, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(sendAcct)))
        if (sendAcct != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     *
     * @param M
     * @param pubKeys
     * @param sendAccts
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendTransferFromMultiSignAddr(M: Int, pubKeys: Array<ByteArray>, sendAccts: Array<Account>, recvAddr: String, amount: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (sendAccts.size <= 1) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }

        val multiAddr = Address.addressFromMultiPubKeys(sendAccts.size, *pubKeys)
        val tx = makeTransfer(multiAddr.toBase58(), recvAddr, amount, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        for (i in sendAccts.indices) {
            addMultiSign(tx, M, pubKeys, sendAccts[i])
        }
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param sender
     * @param recvAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeTransfer(sender: String, recvAddr: String, amount: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (recvAddr.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }

        val list = mutableListOf<Any>()
        val listStruct = mutableListOf<Struct>()
        listStruct.add(Struct().add(Address.decodeBase58(sender), Address.decodeBase58(recvAddr), amount))
        list.add(listStruct)
        val args = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transfer", args, payer, gaslimit, gasprice)
    }

    fun makeTransfer(states: Array<State>, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamError)
        }
        val list = mutableListOf<Any>()
        val listStruct = mutableListOf<Struct>()
        for (i in states.indices) {
            listStruct.add(Struct().add(states[i].from, states[i].to, states[i].value))
        }
        list.add(listStruct)
        val args = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transfer", args, payer, gaslimit, gasprice)
    }

    /**
     * @param address
     * @return
     * @throws Exception
     */
    fun queryBalanceOf(address: String): Long {
        if (address.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("address should not be null"))
        }
        val list = mutableListOf<Any>()
        list.add(Address.decodeBase58(address))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "balanceOf", arg, null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == null || res.isEmpty()) {
            0
        } else Helper.reverse(res).toLong(16)
    }

    /**
     * @param fromAddr
     * @param toAddr
     * @return
     * @throws Exception
     */
    fun queryAllowance(fromAddr: String, toAddr: String): Long {
        if (fromAddr.isEmpty() || toAddr.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        val list = mutableListOf<Any>()
        list.add(Struct().add(Address.decodeBase58(fromAddr), Address.decodeBase58(toAddr)))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "allowance", arg, null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == null || res.isEmpty()) {
            0
        } else Helper.reverse(res).toLong(16)
    }

    /**
     *
     * @param sendAcct
     * @param recvAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendApprove(sendAcct: Account, recvAddr: String, amount: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val tx = makeApprove(sendAcct.addressU160.toBase58(), recvAddr, amount, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(sendAcct)))
        if (sendAcct != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     * @param sender
     * @param recvAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeApprove(sender: String, recvAddr: String, amount: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (recvAddr.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val list = mutableListOf<Any>()
        list.add(Struct().add(Address.decodeBase58(sender), Address.decodeBase58(recvAddr), amount))
        val args = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "approve", args, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param sendAcct
     * @param fromAddr
     * @param toAddr
     * @param amount
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendTransferFrom(sendAcct: Account, fromAddr: String, toAddr: String, amount: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val tx = makeTransferFrom(sendAcct.addressU160.toBase58(), fromAddr, toAddr, amount, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(sendAcct)))
        if (sendAcct != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     * @param sender
     * @param fromAddr
     * @param toAddr
     * @param amount
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeTransferFrom(sender: String, fromAddr: String, toAddr: String, amount: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (fromAddr.isEmpty() || toAddr.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameters should not be null"))
        }
        if (amount <= 0 || gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("amount or gasprice or gaslimit should not be less than 0"))
        }
        val list = mutableListOf<Any>()
        list.add(Struct().add(Address.decodeBase58(sender), Address.decodeBase58(fromAddr), Address.decodeBase58(toAddr), amount))
        val args = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transferFrom", args, payer, gaslimit, gasprice)
    }

    /**
     * @return
     * @throws Exception
     */
    fun queryName(): String {
        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "name", byteArrayOf(0), null, 0, 0)

        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return String(Helper.hexToBytes(res))
    }

    /**
     * @return
     * @throws Exception
     */
    fun querySymbol(): String {
        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "symbol", byteArrayOf(0), null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return String(Helper.hexToBytes(res))
    }

    /**
     * @return
     * @throws Exception
     */
    fun queryDecimals(): Long {
        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "decimals", byteArrayOf(0), null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == null || res.isEmpty()) {
            0
        } else Helper.reverse(res).toLong(16)
    }

    /**
     * @return
     * @throws Exception
     */
    fun queryTotalSupply(): Long {
        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "totalSupply", byteArrayOf(0), null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res == null || res.isEmpty()) {
            0
        } else Helper.reverse(res).toLong(16)
    }
}
