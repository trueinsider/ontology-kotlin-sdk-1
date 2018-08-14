package com.github.neo.core

import com.alibaba.fastjson.JSON
import com.github.neo.core.NeoRpc.call
import com.github.ontio.common.ErrorCode
import com.github.ontio.network.exception.RpcException

import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.HashMap

object NeoRpc {

    @Throws(Exception::class)
    fun sendRawTransaction(url: String, sData: String): Any {
        return call(url, "sendrawtransaction", sData)
    }

    @Throws(Exception::class)
    fun getBalance(url: String, contractAddr: String, addr: String): Any {
        return call(url, "getstorage", contractAddr, addr)
    }

    @Throws(RpcException::class, IOException::class)
    fun call(url: String, method: String, vararg params: Any): Any {
        val req = makeRequest(method, params)
        val response = send(url, req)
        return when {
            response["result"] != null -> response["result"]!!
            response["Result"] != null -> response["Result"]!!
            response["error"] != null -> throw RpcException(0, JSON.toJSONString(response))
            else -> throw RpcException(0, JSON.toJSONString(response))
        }
    }

    private fun makeRequest(method: String, params: Array<out Any>): Map<*, *> {
        val request = HashMap<Any, Any>()
        request["jsonrpc"] = "2.0"
        request["method"] = method
        request["params"] = params
        request["id"] = 1
        println(String.format("POST %s", JSON.toJSONString(request)))
        return request
    }


    @Throws(IOException::class)
    fun send(url: String, request: Any): Map<*, *> {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream).use { w -> w.write(JSON.toJSONString(request)) }
        InputStreamReader(connection.inputStream).use { r ->
            return JSON.parseObject(r.readText(), Map::class.java)
        }
    }
}
