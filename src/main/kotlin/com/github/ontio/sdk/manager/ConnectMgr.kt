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

package com.github.ontio.sdk.manager

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.network.connect.IConnector
import com.github.ontio.network.exception.ConnectorException
import com.github.ontio.network.rest.RestClient
import com.github.ontio.network.rest.Result
import com.github.ontio.network.rpc.RpcClient
import com.github.ontio.network.websocket.WebsocketClient
import com.github.ontio.sdk.exception.SDKException

/**
 *
 */
class ConnectMgr {
    private var connector: IConnector? = null

    private fun getUrl() = connector!!.getUrl()

    fun getNodeCount(): Int = connector!!.getNodeCount()

    fun getBlockHeight(): Int = connector!!.getBlockHeight()

    fun getMemPoolTxCount(): Any = connector!!.getMemPoolTxCount()

    fun getVersion(): String = connector!!.getVersion()

    constructor(url: String, type: String, lock: Object) {
        if (type == "websocket") {
            setConnector(WebsocketClient(url, lock))
        }
    }

    constructor(url: String, type: String) {
        if (type == "rpc") {
            setConnector(RpcClient(url))
        } else if (type == "restful") {
            setConnector(RestClient(url))
        }
    }

    fun startWebsocketThread(log: Boolean) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).startWebsocketThread(log)
        }
    }

    fun setReqId(n: Long) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).setReqId(n)
        }
    }

    fun send(map: Map<*, *>) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).send(map)
        }
    }

    fun sendHeartBeat() {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).sendHeartBeat()
        }
    }

    fun sendSubscribe(map: MutableMap<String, Any>) {
        if (connector is WebsocketClient) {
            (connector as WebsocketClient).sendSubscribe(map)
        }
    }

    constructor(connector: IConnector) {
        setConnector(connector)
    }

    fun setConnector(connector: IConnector) {
        this.connector = connector
    }

    fun sendRawTransaction(tx: Transaction): Boolean {
        val rs = connector!!.sendRawTransaction(Helper.toHexString(tx.toArray())) as String
        if (connector is RpcClient) {
            return true
        }
        if (connector is WebsocketClient) {
            return true
        }
        val rr = JSON.parseObject(rs, Result::class.java)
        return rr.Error == 0L
    }

    fun sendRawTransaction(hexData: String): Boolean {
        val rs = connector!!.sendRawTransaction(hexData) as String
        if (connector is RpcClient) {
            return true
        }
        if (connector is WebsocketClient) {
            return true
        }
        val rr = JSON.parseObject(rs, Result::class.java)
        return rr.Error == 0L
    }

    fun trySendRawTransaction(hexData: String, reSendTime: Int): Boolean {
        var b = false
        for (i in 0 until reSendTime) {
            try {
                b = sendRawTransaction(hexData)
            } catch (e: Exception) {
                if (e.message!!.contains("\"Error\":58403") && i < reSendTime - 1) {
                    continue
                } else {
                    e.printStackTrace()
                    break
                }
            }
        }
        return b
    }

    /**
     * wait result after send
     * @param hexData
     * @return
     * @throws ConnectorException
     * @throws Exception
     */
    fun sendRawTransactionSync(hexData: String): Any {
        return syncSendRawTransaction(hexData)
    }

    /**
     * wait result after send
     * @param hexData
     * @return
     * @throws ConnectorException
     * @throws Exception
     */
    fun syncSendRawTransaction(hexData: String): Any {
        connector!!.sendRawTransaction(hexData)
        val hash = Transaction.deserializeFrom(Helper.hexToBytes(hexData)).hash().toString()
        println("Transaction hash is: $hash, Please waitting result... ")
        return waitResult(hash)
    }

    fun sendRawTransactionPreExec(hexData: String): Any? {
        val rs = connector!!.sendRawTransaction(true, null, hexData)
        if (connector is RpcClient) {
            return rs
        }
        if (connector is WebsocketClient) {
            return rs
        }
        val rr = JSON.parseObject(rs as String, Result::class.java)
        return if (rr.Error == 0L) {
            rr.Result
        } else null
    }

    fun getTransaction(txhash: String): Transaction {
        var txhash = txhash
        txhash = txhash.replace("0x", "")
        return connector!!.getRawTransaction(txhash)
    }

    fun getTransactionJson(txhash: String): Any {
        var txhash = txhash
        txhash = txhash.replace("0x", "")
        return connector!!.getRawTransactionJson(txhash)
    }

    fun getBlock(height: Int): Block {
        if (height < 0) {
            throw SDKException(ErrorCode.ParamError)
        }
        return connector!!.getBlock(height)
    }

    fun getBlock(hash: String): Block {
        return connector!!.getBlock(hash)

    }

    fun getBalance(address: String): Any {
        return connector!!.getBalance(address)
    }

    fun getBlockJson(height: Int): Any {
        return connector!!.getBlockJson(height)
    }

    fun getBlockJson(hash: String): Any {
        return connector!!.getBlockJson(hash)
    }

    fun getContract(hash: String): Any {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getContractJson(hash)
    }

    fun getContractJson(hash: String): Any {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getContractJson(hash)
    }

    fun getSmartCodeEvent(height: Int): Any {
        return connector!!.getSmartCodeEvent(height)
    }

    fun getSmartCodeEvent(hash: String): Any {
        return connector!!.getSmartCodeEvent(hash)
    }

    fun getBlockHeightByTxHash(hash: String): Int {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getBlockHeightByTxHash(hash)
    }

    fun getStorage(codehash: String, key: String): String {
        var codehash = codehash
        codehash = codehash.replace("0x", "")
        return connector!!.getStorage(codehash, key)
    }

    fun getMerkleProof(hash: String): Map<String, Any> {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getMerkleProof(hash)
    }

    fun getAllowance(asset: String, from: String, to: String): String {
        return connector!!.getAllowance(asset, from, to)
    }

    fun getMemPoolTxState(hash: String): Any {
        var hash = hash
        hash = hash.replace("0x", "")
        return connector!!.getMemPoolTxState(hash)
    }

    fun waitResult(hash: String): Any {
        var objEvent: Any? = null
        var notInpool = 0
        for (i in 0..19) {
            try {
                Thread.sleep(3000)
                objEvent = connector!!.getSmartCodeEvent(hash)
                if (objEvent == "") {
                    Thread.sleep(1000)
                    connector!!.getMemPoolTxState(hash)
                    continue
                }
                if ((objEvent as Map<*, *>)["Notify"] != null) {
                    return objEvent
                }
            } catch (e: Exception) {
                if (e.message!!.contains("UNKNOWN TRANSACTION") && e.message!!.contains("getmempooltxstate")) {
                    notInpool++
                    if ((objEvent == "" || objEvent == null) && notInpool > 1) {
                        throw SDKException(e.message!!)
                    }
                } else {
                    continue
                }
            }

        }
        throw SDKException(ErrorCode.OtherError("time out"))
    }
}


