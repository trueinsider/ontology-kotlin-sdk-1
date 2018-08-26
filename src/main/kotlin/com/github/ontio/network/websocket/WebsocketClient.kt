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

package com.github.ontio.network.websocket

import com.alibaba.fastjson.JSON
import com.github.ontio.core.block.Block
import com.github.ontio.core.payload.Bookkeeping
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.network.connect.AbstractConnector
import okhttp3.*
import java.util.*

/**
 *
 */
class WebsocketClient(private val wsUrl: String, private val lock: Object) : AbstractConnector() {
    private var mWebSocket: WebSocket? = null
    private var logFlag: Boolean = false
    private var reqId: Long = 0

    override fun getUrl() = wsUrl

    override fun getNodeCount(): Int {
        val map = HashMap<Any, Any>()
        map["Action"] = "getconnectioncount"
        map["Version"] = "1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return 0
    }

    override fun getBlockHeight(): Int {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockheight"
        map["Version"] = "1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return 0
    }

    override fun getMemPoolTxCount(): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getmempooltxcount"
        map["Version"] = "1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    override fun getVersion(): String {
        val map = HashMap<Any, Any>()
        map["Action"] = "getversion"
        map["Version"] = "1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    fun setLog(b: Boolean) {
        logFlag = b
    }

    fun startWebsocketThread(log: Boolean) {
        this.logFlag = log
        val thread = Thread(Runnable { wsStart() })
        thread.start()
    }

    fun sendHeartBeat() {
        val map = HashMap<Any, Any>()
        map["Action"] = "heartbeat"
        map["Version"] = "V1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
    }

    fun sendSubscribe(map: MutableMap<String, Any>) {
        map["Action"] = "subscribe"
        map["Version"] = "V1.0.0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
    }

    fun send(map: Map<*, *>) {
        mWebSocket!!.send(JSON.toJSONString(map))
    }

    fun setReqId(reqId: Long) {
        this.reqId = reqId
    }

    private fun generateReqId(): Long {
        return if (reqId == 0L) {
            (Random().nextInt() and Integer.MAX_VALUE).toLong()
        } else reqId
    }

    override fun sendRawTransaction(preExec: Boolean, userid: String?, sData: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "sendrawtransaction"
        map["Version"] = "1.0.0"
        map["Data"] = sData
        map["Id"] = generateReqId()
        if (preExec) {
            map["PreExec"] = "1"
        }
        mWebSocket!!.send(JSON.toJSONString(map))
        return if (preExec) {
            "0"
        } else ""
    }

    override fun sendRawTransaction(sData: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "sendrawtransaction"
        map["Version"] = "1.0.0"
        map["Data"] = sData
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    override fun getRawTransaction(txhash: String): Transaction {
        val map = HashMap<Any, Any>()
        map["Action"] = "gettransaction"
        map["Version"] = "1.0.0"
        map["Hash"] = txhash
        map["Raw"] = "1"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return Bookkeeping()
    }

    override fun getRawTransactionJson(txhash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "gettransaction"
        map["Version"] = "1.0.0"
        map["Hash"] = txhash
        map["Raw"] = "0"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    override fun getBlock(height: Int): Block {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyheight"
        map["Version"] = "1.0.0"
        map["Height"] = height
        map["Raw"] = "1"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return Block()
    }

    override fun getBlock(hash: String): Block {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyhash"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Raw"] = "1"
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return Block()
    }

    override fun getBlockJson(height: Int): Block {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyheight"
        map["Version"] = "1.0.0"
        map["Height"] = height
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return Block()
    }

    override fun getBlockJson(hash: String): Block {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockbyhash"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return Block()
    }

    override fun getBalance(address: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getbalance"
        map["Version"] = "1.0.0"
        map["Addr"] = address
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    override fun getContract(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getcontract"
        map["Version"] = "1.0.0"
        map["Raw"] = "1"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    override fun getContractJson(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getcontract"
        map["Version"] = "1.0.0"
        map["Raw"] = "0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    override fun getSmartCodeEvent(height: Int): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getsmartcodeeventbyheight"
        map["Version"] = "1.0.0"
        map["Height"] = height
        map["Id"] = generateReqId()
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    override fun getSmartCodeEvent(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getsmartcodeeventbyhash"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        return mWebSocket!!.send(JSON.toJSONString(map))
    }

    override fun getBlockHeightByTxHash(hash: String): Int {
        val map = HashMap<Any, Any>()
        map["Action"] = "getblockheightbytxhash"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return 0
    }

    override fun getStorage(codehash: String, key: String): String {
        val map = HashMap<Any, Any>()
        map["Action"] = "getstorage"
        map["Version"] = "1.0.0"
        map["Hash"] = codehash
        map["Key"] = key
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    override fun getMerkleProof(hash: String): Map<String, Any> {
        val map = HashMap<Any, Any>()
        map["Action"] = "getmerkleproof"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return emptyMap()
    }

    override fun getAllowance(asset: String, from: String, to: String): String {
        val map = HashMap<Any, Any>()
        map["Action"] = "getallowance"
        map["Version"] = "1.0.0"
        map["Asset"] = asset
        map["From"] = from
        map["To"] = to
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    override fun getMemPoolTxState(hash: String): Any {
        val map = HashMap<Any, Any>()
        map["Action"] = "getmempooltxstate"
        map["Version"] = "1.0.0"
        map["Hash"] = hash
        map["Id"] = generateReqId()
        mWebSocket!!.send(JSON.toJSONString(map))
        return ""
    }

    fun wsStart() {
        val httpUrl: String = if (wsUrl.contains("wss")) {
            "https://" + wsUrl.split("://".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        } else {
            "http://" + wsUrl.split("://".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        }
        val request = Request.Builder().url(wsUrl).addHeader("Origin", httpUrl).build()
        val mClient = OkHttpClient.Builder().build()
        mWebSocket = mClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket?, response: Response?) {
                println("opened websocket connection")
                sendHeartBeat()
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        sendHeartBeat()
                    }
                }, 1000, 30000)
            }

            override fun onMessage(webSocket: WebSocket?, s: String?) {
                if (logFlag) {
                    println("websoket onMessage:" + s!!)
                }
                val result = JSON.parseObject(s, Result::class.java)
                synchronized(lock) {
                    MsgQueue.addResult(result)
                    lock.notify()
                }
            }

            override fun onClosing(webSocket: WebSocket?, code: Int, reason: String?) {
                println(reason)
            }

            override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
                println("close:" + reason!!)
            }

            override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
                println("onFailure:" + response!!)
                wsStart()
            }
        })

    }
}

