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
import com.github.ontio.OntSdk.addSign
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.Helper
import com.github.ontio.core.globalparams.Params
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.Vm
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct

object GlobalParams {
    private const val contractAddress = "0000000000000000000000000000000000000004"

    fun init(): Boolean {
        val tx = Vm.makeInvokeCodeTransaction(contractAddress, "init", byteArrayOf(), null, 0, 0)
        return connect!!.sendRawTransaction(tx.toHexString())
    }

    fun transferAdmin(adminAccount: Account, newAdminAddr: Address, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeTransferAdmin(newAdminAddr, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(adminAccount)))
        if (adminAccount != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun transferAdmin(M: Int, accounts: Array<Account>, newAdminAddr: Address, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (accounts.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (accounts.size < M) {
            throw SDKException("the accounts length should not be less than M")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeTransferAdmin(newAdminAddr, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(accounts), intArrayOf(M))
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun makeTransferAdmin(newAdminAddr: Address, payerAddr: String, gaslimit: Long, gasprice: Long): Transaction {
        if (payerAddr.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val list = mutableListOf<Any>()
        val struct = Struct()
        struct.add(newAdminAddr.toArray())
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transferAdmin", arg, payerAddr, gaslimit, gasprice)
    }

    fun acceptAdmin(account: Account, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeAcceptAdmin(account.addressU160, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(account)))
        if (account != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun acceptAdmin(multiAddr: Address, M: Int, accounts: Array<Account>, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (accounts.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (accounts.size < M) {
            throw SDKException("the accounts length should not be less than M")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeAcceptAdmin(multiAddr, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(accounts), intArrayOf(M))
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun makeAcceptAdmin(multiAddr: Address, payerAddr: String, gaslimit: Long, gasprice: Long): Transaction {
        if (payerAddr.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val list = mutableListOf<Any>()
        val struct = Struct()
        struct.add(multiAddr.toArray())
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transferAdmin", arg, payerAddr, gaslimit, gasprice)
    }


    fun getGlobalParam(paramNameList: Array<String>): String {
        if (paramNameList.isEmpty()) {
            throw SDKException("parameter should not be less than 0")
        }
        val list = mutableListOf<Any>()
        val struct = Struct()
        struct.add(paramNameList.size)
        for (i in paramNameList.indices) {
            struct.add(paramNameList[i])
        }
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)
        val tx = Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getGlobalParam", arg, null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        return (obj as JSONObject).getString("Result")
    }

    fun setGlobalParam(operatorAccount: Account, params: Params, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeSetGlobalParam(params, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(operatorAccount)))
        if (operatorAccount != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun setGlobalParam(M: Int, operatorAccounts: Array<Account>, params: Params, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (operatorAccounts.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (operatorAccounts.size < M) {
            throw SDKException("the operatorAccounts length should not be less than M")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeSetGlobalParam(params, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(operatorAccounts), intArrayOf(M))
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun makeSetGlobalParam(params: Params, payerAddr: String, gaslimit: Long, gasprice: Long): Transaction {
        if (payerAddr.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val list = mutableListOf<Any>()
        val struct = Struct()
        struct.add(params.params.size)
        for (i in params.params.indices) {
            struct.add(params.params[i].key, params.params[i].value)
        }
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "setGlobalParam", arg, payerAddr, gaslimit, gasprice)
    }


    fun setOperator(adminAccount: Account, addr: Address, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeSetOperator(addr, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(adminAccount)))
        if (adminAccount != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun setOperator(M: Int, accounts: Array<Account>, addr: Address, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (accounts.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (accounts.size < M) {
            throw SDKException("accounts length should not be less than M")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeSetOperator(addr, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(accounts), intArrayOf(M))
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun makeSetOperator(addr: Address, payerAddr: String, gaslimit: Long, gasprice: Long): Transaction {
        if (payerAddr.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val list = mutableListOf<Any>()
        val struct = Struct()
        struct.add(addr.toArray())
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transferAdmin", arg, payerAddr, gaslimit, gasprice)
    }

    fun createSnapshot(operatorAccount: Account, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeCreateSnapshot(payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(arrayOf(operatorAccount)))
        if (operatorAccount != payerAcct) {
            addSign(tx, payerAcct)
        }
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun createSnapshot(M: Int, operatorAccounts: Array<Account>, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (operatorAccounts.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (operatorAccounts.size < M) {
            throw SDKException("accounts length should not be less than M")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        val tx = makeCreateSnapshot(payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(operatorAccounts), intArrayOf(M))
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    fun makeCreateSnapshot(payerAddr: String, gaslimit: Long, gasprice: Long): Transaction {
        if (payerAddr.isEmpty()) {
            throw SDKException("parameter should not be null")
        }
        if (gaslimit < 0 || gasprice < 0) {
            throw SDKException("gaslimit or gasprice should not be less than 0")
        }
        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "createSnapshot", byteArrayOf(0), payerAddr, gaslimit, gasprice)
    }
}
