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
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.OntSdk.walletMgr
import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.crypto.KeyType
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.Vm.makeInvokeCodeTransaction
import com.github.ontio.smartcontract.neovm.abi.BuildParams
import java.util.*

class Record(contractAddress: String) {
    val contractAddress = contractAddress.replace("0x", "")

    fun sendPut(addr: String, password: String, salt: ByteArray, key: String, value: String, gaslimit: Long, gas: Long): String? {
        val addr = addr.replace(Common.didont, "")
        if (key.isEmpty() || value.isEmpty()) {
            throw SDKException(ErrorCode.NullKeyOrValue)
        }
        val info = walletMgr.getAccountInfo(addr, password, salt)
        val list = ArrayList<Any>()
        list.add("Put".toByteArray())
        val tmp = ArrayList<Any>()
        tmp.add(key.toByteArray())
        tmp.add(JSON.toJSONString(constructRecord(value)).toByteArray())
        list.add(tmp)
        val tx = makeInvokeTransaction(list, info.addressBase58, gaslimit, gas)
        signTx(tx, addr, password, salt)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    fun sendGet(addr: String, password: String, salt: ByteArray, key: String): String {
        if (key.isEmpty()) {
            throw SDKException(ErrorCode.NullKey)
        }
        val list = ArrayList<Any>()
        list.add("Get".toByteArray())
        val tmp = ArrayList<Any>()
        tmp.add(key.toByteArray())
        list.add(tmp)
        val tx = makeInvokeTransaction(list, null, 0, 0)
        signTx(tx, addr, password, salt)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        return String(Helper.hexToBytes(obj as String))
    }

    fun makeInvokeTransaction(list: List<Any>, payer: String?, gaslimit: Long, gas: Long): Transaction {
        val params = BuildParams.createCodeParamsScript(list)
        return makeInvokeCodeTransaction(contractAddress, null, params, payer, gaslimit, gas)
    }

    fun constructRecord(text: String): LinkedHashMap<String, Any> {
        val recordData = LinkedHashMap<String, Any>()
        val data = LinkedHashMap<String, Any>()
        data["Algrithem"] = KeyType.SM2.name
        data["Hash"] = ""
        data["Text"] = text
        data["Signature"] = ""

        recordData["Data"] = data
        recordData["CAkey"] = ""
        recordData["SeqNo"] = ""
        recordData["Timestamp"] = 0
        return recordData
    }
}
