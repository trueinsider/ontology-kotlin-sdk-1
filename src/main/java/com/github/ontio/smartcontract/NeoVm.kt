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

import com.github.ontio.OntSdk.addSign
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.account.Account
import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.neovm.abi.AbiFunction
import com.github.ontio.smartcontract.neovm.abi.BuildParams

object NeoVm {
    fun sendTransaction(contractAddr: String, acct: Account?, payerAcct: Account?, gaslimit: Long, gasprice: Long, func: AbiFunction?, preExec: Boolean): Any? {
        val params = if (func != null) {
            BuildParams.serializeAbiFunction(func)
        } else {
            byteArrayOf()
        }
        if (preExec) {
            val tx = Vm.makeInvokeCodeTransaction(contractAddr, null, params, null, 0, 0)
            if (acct != null) {
                signTx(tx, arrayOf(arrayOf(acct)))
            }
            return connect!!.sendRawTransactionPreExec(tx.toHexString())
        } else {
            val payer = payerAcct!!.addressU160.toBase58()
            val tx = Vm.makeInvokeCodeTransaction(contractAddr, null, params, payer, gaslimit, gasprice)
            signTx(tx, arrayOf(arrayOf(acct!!)))
            if (acct.addressU160.toBase58() != payerAcct.addressU160.toBase58()) {
                addSign(tx, payerAcct)
            }
            val b = connect!!.sendRawTransaction(tx.toHexString())
            if (!b) {
                throw SDKException(ErrorCode.SendRawTxError)
            }
            return tx.hash().toHexString()
        }
    }
}
