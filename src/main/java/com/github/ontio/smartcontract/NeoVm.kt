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

import com.github.ontio.OntSdk
import com.github.ontio.account.Account
import com.github.ontio.common.ErrorCode
import com.github.ontio.smartcontract.neovm.abi.AbiFunction
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.neovm.abi.BuildParams
import com.github.ontio.smartcontract.neovm.ClaimRecord
import com.github.ontio.smartcontract.neovm.Nep5
import com.github.ontio.smartcontract.neovm.Record

class NeoVm {
    /**
     * get OntAsset Tx
     * @return instance
     */
    val nep5: Nep5 = Nep5()

    /**
     * RecordTx
     * @return instance
     */
    val record: Record = Record()

    val claimRecord = ClaimRecord()

    @Throws(Exception::class)
    fun sendTransaction(contractAddr: String, acct: Account?, payerAcct: Account, gaslimit: Long, gasprice: Long, func: AbiFunction?, preExec: Boolean): Any? {
        val params = if (func != null) {
            BuildParams.serializeAbiFunction(func)
        } else {
            byteArrayOf()
        }
        if (preExec) {
            val tx = OntSdk.vm().makeInvokeCodeTransaction(contractAddr, null, params, null, 0, 0)
            if (acct != null) {
                OntSdk.signTx(tx, arrayOf(arrayOf(acct)))
            }
            return OntSdk.connect!!.sendRawTransactionPreExec(tx.toHexString())
        } else {
            val payer = payerAcct.addressU160!!.toBase58()
            val tx = OntSdk.vm().makeInvokeCodeTransaction(contractAddr, null, params, payer, gaslimit, gasprice)
            OntSdk.signTx(tx, arrayOf(arrayOf(acct!!)))
            if (acct.addressU160!!.toBase58() != payerAcct.addressU160!!.toBase58()) {
                OntSdk.addSign(tx, payerAcct)
            }
            val b = OntSdk.connect!!.sendRawTransaction(tx.toHexString())
            if (!b) {
                throw SDKException(ErrorCode.SendRawTxError)
            }
            return tx.hash().toHexString()
        }
    }
}
