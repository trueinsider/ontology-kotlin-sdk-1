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

package com.github.ontio.smartcontract

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.Vm.makeInvokeCodeTransaction
import com.github.ontio.smartcontract.neovm.abi.AbiFunction
import com.github.ontio.smartcontract.neovm.abi.BuildParams

object WasmVm {
    fun sendTransaction(contractAddr: String, payer: String, password: String, salt: ByteArray, gaslimit: Long, gas: Long, func: AbiFunction, preExec: Boolean): String {
        val params = BuildParams.serializeAbiFunction(func)
        if (preExec) {
            val tx = makeInvokeCodeTransaction(contractAddr, null, params, payer, gaslimit, gas)
            val obj = connect!!.sendRawTransactionPreExec(tx.toHexString()) as JSONObject
            val result = obj.getString("Result")
            if (Integer.parseInt(result) == 0) {
                throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: $obj"))
            }
            return result
        } else {
            val tx = makeInvokeCodeTransaction(contractAddr, null, params, payer, gaslimit, gas)
            signTx(tx, payer, password, salt)
            val b = connect!!.sendRawTransaction(tx.toHexString())
            if (!b) {
                throw SDKException(ErrorCode.SendRawTxError)
            }
            return tx.hash().toString()
        }
    }

    fun buildWasmContractJsonParam(objs: Array<Any>): String {
        val params = mutableListOf<Map<String, Any>>()
        for (i in objs.indices) {
            val `val` = objs[i]
            if (`val` is String) {
                val map = mutableMapOf<String, String>()
                map["type"] = "string"
                map["value"] = `val`
                params.add(map)
            } else if (`val` is Int) {
                val map = mutableMapOf<String, String>()
                map["type"] = "int"
                map["value"] = `val`.toString()
                params.add(map)
            } else if (`val` is Long) {
                val map = mutableMapOf<String, String>()
                map["type"] = "int64"
                map["value"] = `val`.toString()
                params.add(map)
            } else if (`val` is IntArray) {
                val map = mutableMapOf<String, Any>()
                map["type"] = "int_array"
                map["value"] = `val`
                params.add(map)
            } else if (`val` is LongArray) {
                val map = mutableMapOf<String, Any>()
                map["type"] = "int_array"
                map["value"] = `val`
                params.add(map)
            } else {
                continue
            }
        }
        val result = mutableMapOf<String, List<Map<String, Any>>>()
        result["Params"] = params
        return JSON.toJSONString(result)
    }
}
