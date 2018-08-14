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

package com.github.ontio.network.rest

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.Serializable
import com.github.ontio.network.connect.AbstractConnector
import com.github.ontio.network.exception.RestfulException
import java.io.IOException

class RestClient(restUrl: String) : AbstractConnector() {
    private val api: Interfaces = Interfaces(restUrl)
    private val version = "v1.0.0"
    private val action = "sendrawtransaction"

    override fun getUrl() = api.url

    override fun getNodeCount(): Int {
        val rs = api.getNodeCount()
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result as Int
        }
        throw RestfulException(to(rr))
    }

    override fun getBlockHeight(): Int {
        val rs = api.getBlockHeight()
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result as Int
        }
        throw RestfulException(to(rr))
    }

    override fun getMemPoolTxCount(): Any {
        val rs = api.getMemPoolTxCount()
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))
    }

    override fun sendRawTransaction(sData: String): String {
        val rs = api.sendTransaction(false, null, action, version, sData)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rs
        }
        throw RestfulException(to(rr))
    }

    override fun sendRawTransaction(preExec: Boolean, userid: String?, sData: String): String {
        val rs = api.sendTransaction(preExec, userid, action, version, sData)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rs
        }
        throw RestfulException(to(rr))
    }

    override fun getRawTransaction(txhash: String): Transaction {
        val rs = api.getTransaction(txhash, true)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            try {
                return Transaction.deserializeFrom(Helper.hexToBytes(rr.Result as String))
            } catch (e: IOException) {
                throw RestfulException(ErrorCode.TxDeserializeError, e)
            }

        }
        throw RestfulException(to(rr))
    }

    override fun getBlock(height: Int): Block {
        val rs = api.getBlock(height, "1")
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            try {
                return Serializable.from(Helper.hexToBytes(rr.Result as String), Block::class.java)
            } catch (e: InstantiationException) {
                throw RestfulException(ErrorCode.BlockDeserializeError, e)
            } catch (e: IllegalAccessException) {
                throw RestfulException(ErrorCode.BlockDeserializeError, e)
            }

        }
        throw RestfulException(to(rr))
    }


    override fun getBlock(hash: String): Block {
        val rs = api.getBlock(hash, "1")
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error != 0L) {
            throw RestfulException(to(rr))
        }
        try {
            return Serializable.from(Helper.hexToBytes(rr.Result as String), Block::class.java)
        } catch (e: InstantiationException) {
            throw RestfulException(ErrorCode.BlockDeserializeError, e)
        } catch (e: IllegalAccessException) {
            throw RestfulException(ErrorCode.BlockDeserializeError, e)
        }

    }

    override fun getBalance(address: String): Any {
        val rs = api.getBalance(address)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))
    }

    override fun getRawTransactionJson(txhash: String): Any {
        val rs = api.getTransaction(txhash, true)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return JSON.toJSONString(Transaction.deserializeFrom(Helper.hexToBytes(rr.Result as String)).json())
        }
        throw RestfulException(to(rr))
    }

    override fun getBlockJson(height: Int): Any {
        val rs = api.getBlock(height, "0")
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))
    }

    override fun getBlockJson(hash: String): Any {
        val rs = api.getBlock(hash, "0")
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))

    }

    override fun getContract(hash: String): Any {
        val rs = api.getContract(hash)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))
    }

    override fun getContractJson(hash: String): Any {
        val rs = api.getContractJson(hash)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))
    }

    override fun getSmartCodeEvent(height: Int): Any {
        val rs = api.getSmartCodeEvent(height)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))

    }

    override fun getSmartCodeEvent(hash: String): Any {
        val rs = api.getSmartCodeEvent(hash)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))
    }

    override fun getBlockHeightByTxHash(hash: String): Int {
        val rs = api.getBlockHeightByTxHash(hash)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result as Int
        }
        throw RestfulException(to(rr))
    }

    override fun getStorage(codehash: String, key: String): String {
        val rs = api.getStorage(codehash, key)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result as String
        }
        throw RestfulException(to(rr))
    }

    override fun getMerkleProof(hash: String): Map<String, Any> {
        val rs = api.getMerkleProof(hash)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result as Map<String, Any>
        }
        throw RestfulException(to(rr))
    }

    override fun getAllowance(asset: String, from: String, to: String): String {
        val rs = api.getAllowance(asset, from, to)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result as String
        }
        throw RestfulException(to(rr))
    }

    override fun getMemPoolTxState(hash: String): Any {
        val rs = api.getMemPoolTxState(hash)
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result!!
        }
        throw RestfulException(to(rr))
    }

    override fun getVersion(): String {
        val rs = api.getVersion()
        val rr = JSON.parseObject(rs, Result::class.java)
        if (rr.Error == 0L) {
            return rr.Result as String
        }
        throw RestfulException(to(rr))
    }

    private fun to(rr: Result): String {
        return JSON.toJSONString(rr)
    }
}




