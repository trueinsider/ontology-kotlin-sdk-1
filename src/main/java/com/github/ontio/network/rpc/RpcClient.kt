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

package com.github.ontio.network.rpc

import com.github.ontio.common.Helper
import com.github.ontio.common.UInt256
import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.Serializable
import com.github.ontio.network.connect.AbstractConnector

class RpcClient(url: String) : AbstractConnector() {
    private var rpc: Interfaces = Interfaces(url)

    override fun getUrl() = rpc.host

    override fun getNodeCount() = rpc.call("getconnectioncount") as Int

    override fun getBlockHeight() = rpc.call("getblockcount") as Int

    override fun getMemPoolTxCount() = rpc.call("getmempooltxcount")

    override fun getVersion(): String = rpc.call("getversion") as String

    override fun getBalance(address: String) = rpc.call("getbalance", address)

    override fun sendRawTransaction(sData: String) = rpc.call("sendrawtransaction", sData) as String

    override fun sendRawTransaction(preExec: Boolean, userid: String?, sData: String) = if (preExec) {
        rpc.call("sendrawtransaction", sData, 1)
    } else {
        rpc.call("sendrawtransaction", sData)
    }

    override fun getRawTransaction(txhash: String): Transaction {
        val result = rpc.call("getrawtransaction", txhash)
        return Transaction.deserializeFrom(Helper.hexToBytes(result as String))
    }

    override fun getRawTransactionJson(txhash: String): Any {
        val result = rpc.call("getrawtransaction", txhash)
        return Transaction.deserializeFrom(Helper.hexToBytes(result as String)).json()
    }

    override fun getBlockJson(height: Int) = rpc.call("getblock", height, 1)

    override fun getBlockJson(hash: String) = rpc.call("getblock", hash, 1)

    override fun getContract(hash: String) = rpc.call("getcontractstate", hash)

    override fun getContractJson(hash: String) = rpc.call("getcontractstate", hash, 1)

    fun getRawTransaction(txhash: UInt256) = rpc.call("getrawtransaction", txhash.toString()) as String

    fun getBlock(hash: UInt256): Block {
        val result = rpc.call("getblock", hash.toString())
        try {
            return Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    override fun getBlock(height: Int): Block {
        val result = rpc.call("getblock", height)
        try {
            return Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    override fun getBlock(hash: String): Block {
        val result = rpc.call("getblock", hash)
        try {
            return Serializable.from(Helper.hexToBytes(result as String), Block::class.java)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
    }

    override fun getSmartCodeEvent(height: Int) = rpc.call("getsmartcodeevent", height)

    override fun getSmartCodeEvent(hash: String) = rpc.call("getsmartcodeevent", hash)

    override fun getBlockHeightByTxHash(hash: String) = rpc.call("getblockheightbytxhash", hash) as Int

    override fun getStorage(codehash: String, key: String) = rpc.call("getstorage", codehash, key) as String

    override fun getMerkleProof(hash: String) = rpc.call("getmerkleproof", hash) as Map<String, Any>

    override fun getAllowance(asset: String, from: String, to: String) = rpc.call("getallowance", asset, from, to) as String

    override fun getMemPoolTxState(hash: String) = rpc.call("getmempooltxstate", hash)
}

