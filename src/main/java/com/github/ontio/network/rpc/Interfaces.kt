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

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.network.exception.RpcException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 *
 */
class Interfaces constructor(url: String) {
    private val url: URL = URL(url)

    private val nextId: Double
        get() {
            var d: Double
            do {
                d = Math.random()
            } while (d.toString().indexOf("E") != -1)
            return d
        }

    val host: String
        get() = url.host + " " + url.port

    fun call(method: String, vararg params: Any): Any {
        val req = makeRequest(method, params)
        val response = send(req)
        return when {
            response == null -> throw RpcException(0, ErrorCode.ConnectUrlErr(url.toString() + "response is null. maybe is connect error"))
            response["error"] as Int == 0 -> response["result"]!!
            else -> throw RpcException(0, JSON.toJSONString(response))
        }
    }

    private fun makeRequest(method: String, params: Array<out Any>): Map<*, *> {
        val request = mutableMapOf<String, Any>()
        request["jsonrpc"] = "2.0"
        request["method"] = method
        request["params"] = params
        request["id"] = 1
        println(String.format("POST url=%s,%s", this.url, JSON.toJSONString(request)))
        return request
    }

    fun send(request: Any): Map<String, Any>? {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        OutputStreamWriter(connection.outputStream).use { it.write(JSON.toJSONString(request)) }
        return InputStreamReader(connection.inputStream).use {
            JSON.parseObject<Map<String, Any>?>(it.readText(), Map::class.java)
        }
    }
}
