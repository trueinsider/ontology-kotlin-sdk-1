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

import com.github.ontio.common.Address
import com.github.ontio.common.Common
import com.github.ontio.common.Helper
import com.github.ontio.core.payload.DeployCode
import com.github.ontio.core.payload.InvokeCode
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.sdk.exception.SDKException
import java.math.BigInteger
import java.util.*

/**
 *
 */
object Vm {
    private const val NATIVE_INVOKE_NAME = "Ontology.Native.Invoke"

    /**
     *
     * @param codeStr
     * @param needStorage
     * @param name
     * @param codeVersion
     * @param author
     * @param email
     * @param desp
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeDeployCodeTransaction(codeStr: String, needStorage: Boolean, name: String, codeVersion: String, author: String, email: String, desp: String, payer: String?, gaslimit: Long, gasprice: Long): DeployCode {
        val tx = DeployCode(
                Helper.hexToBytes(codeStr),
                needStorage,
                name,
                codeVersion,
                author,
                email,
                desp
        )
        if (payer != null) {
            tx.payer = Address.decodeBase58(payer.replace(Common.didont, ""))
        }
        tx.attributes = emptyArray()
        tx.nonce = Random().nextInt()
        tx.gasLimit = gaslimit
        tx.gasPrice = gasprice
        return tx
    }

    //NEO makeInvokeCodeTransaction
    fun makeInvokeCodeTransaction(codeAddr: String, method: String?, params: ByteArray, payer: String?, gaslimit: Long, gasprice: Long): InvokeCode {
        var params = params
        params = Helper.addBytes(params, byteArrayOf(0x67))
        params = Helper.addBytes(params, Address.parse(codeAddr).toArray())
        val tx = InvokeCode(params)
        tx.attributes = emptyArray()
        tx.nonce = Random().nextInt()
        tx.gasLimit = gaslimit
        tx.gasPrice = gasprice
        if (payer != null) {
            tx.payer = Address.decodeBase58(payer.replace(Common.didont, ""))
        }
        return tx
    }

    /**
     * Native makeInvokeCodeTransaction
     * @param params
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeInvokeCodeTransaction(params: ByteArray, payer: String?, gaslimit: Long, gasprice: Long): InvokeCode {
        val tx = InvokeCode(params)
        tx.attributes = emptyArray()
        tx.nonce = Random().nextInt()
        tx.gasLimit = gaslimit
        tx.gasPrice = gasprice
        if (payer != null) {
            tx.payer = Address.decodeBase58(payer.replace(Common.didont, ""))
        }
        return tx
    }

    fun buildNativeParams(codeAddr: Address, initMethod: String, args: ByteArray, payer: String?, gaslimit: Long, gasprice: Long): Transaction {
        val sb = ScriptBuilder()
        if (args.isNotEmpty()) {
            sb.add(args)
        }
        sb.emitPushByteArray(initMethod.toByteArray())
        sb.emitPushByteArray(codeAddr.toArray())
        sb.emitPushInteger(BigInteger.valueOf(0))
        sb.emit(ScriptOp.OP_SYSCALL)
        sb.emitPushByteArray(NATIVE_INVOKE_NAME.toByteArray())
        return makeInvokeCodeTransaction(sb.toArray(), payer, gaslimit, gasprice)
    }
}
