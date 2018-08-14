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

package com.github.ontio.network.connect

import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction

interface IConnector {
    fun getUrl(): String

    fun sendRawTransaction(preExec: Boolean, userid: String?, sData: String): Any

    fun sendRawTransaction(sData: String): Any

    fun getRawTransaction(txhash: String): Transaction

    fun getRawTransactionJson(txhash: String): Any

    fun getNodeCount(): Int

    fun getBlockHeight(): Int

    fun getBlock(height: Int): Block

    fun getBlock(hash: String): Block

    fun getBlockJson(height: Int): Any

    fun getBlockJson(hash: String): Any

    fun getBalance(address: String): Any

    fun getContract(hash: String): Any

    fun getContractJson(hash: String): Any

    fun getSmartCodeEvent(height: Int): Any

    fun getSmartCodeEvent(hash: String): Any

    fun getBlockHeightByTxHash(hash: String): Int

    fun getStorage(codehash: String, key: String): String

    fun getMerkleProof(hash: String): Map<String, Any>

    fun getAllowance(asset: String, from: String, to: String): String

    fun getMemPoolTxCount(): Any

    fun getMemPoolTxState(hash: String): Any

    fun getVersion(): String
}