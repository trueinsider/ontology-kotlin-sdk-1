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

import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.SecureRandom
import java.util.Arrays
import java.util.HashMap
import kotlin.collections.Map.Entry

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager

import com.alibaba.fastjson.JSON

object http {
    private val DEFAULT_CHARSET = "UTF-8"

    @Throws(IOException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class, KeyManagementException::class)
    fun post(url: String, header: Map<String, String>?, body: String, https: Boolean): String {
        val u = URL(url)
        val http = u.openConnection() as HttpURLConnection
        http.connectTimeout = 10000
        http.readTimeout = 10000
        http.requestMethod = "POST"
        http.setRequestProperty("Content-Type", "application/json")
        if (header != null) {
            for ((key, value) in header) {
                http.setRequestProperty(key, value)
            }
        }
        if (https) {
            val sslContext = SSLContext.getInstance("SSL", "SunJSSE")
            sslContext.init(null, arrayOf<TrustManager>(X509()), SecureRandom())
            val ssf = sslContext.socketFactory
            (http as HttpsURLConnection).sslSocketFactory = ssf
        }
        http.doOutput = true
        http.doInput = true
        http.connect()
        http.outputStream.use { out ->
            out.write(body.toByteArray(charset(DEFAULT_CHARSET)))
            out.flush()
        }
        var str = ""
        http.inputStream.use { `is` ->
            BufferedReader(InputStreamReader(`is`, DEFAULT_CHARSET)).use { reader ->
                str = reader.readText()
            }
        }
        http.disconnect()
        return str
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class, KeyManagementException::class)
    fun post(url: String, body: String, https: Boolean): String {
        return post(url, null, body, https)
    }

    @Throws(IOException::class, NoSuchAlgorithmException::class, NoSuchProviderException::class, KeyManagementException::class)
    fun delete(url: String, body: String, https: Boolean): String {
        val u = URL(url)
        val http = u.openConnection() as HttpURLConnection
        http.connectTimeout = 10000
        http.readTimeout = 10000
        http.requestMethod = "DELETE"
        http.setRequestProperty("Content-Type", "application/json")
        if (https) {
            val sslContext = SSLContext.getInstance("SSL", "SunJSSE")
            sslContext.init(null, arrayOf<TrustManager>(X509()), SecureRandom())
            val ssf = sslContext.socketFactory
            (http as HttpsURLConnection).sslSocketFactory = ssf
        }
        http.doOutput = true
        http.doInput = true
        http.connect()
        http.outputStream.use { out ->
            out.write(body.toByteArray(charset(DEFAULT_CHARSET)))
            out.flush()
        }
        var str = ""
        http.inputStream.use { `is` ->
            BufferedReader(InputStreamReader(`is`, DEFAULT_CHARSET)).use { reader ->
                str = reader.readText()
            }
        }
        http.disconnect()
        return str
    }

    @Throws(Exception::class)
    fun delete(url: String, params: Map<String, String>, body: Map<String, Any>): String {
        return if (url.startsWith("https")) {
            delete(url + cvtParams(params), JSON.toJSONString(body), true)
        } else {
            delete(url + cvtParams(params), JSON.toJSONString(body), false)
        }
    }

    @Throws(Exception::class)
    fun post(url: String, params: Map<String, String>, body: Map<String, Any>): String {
        println(String.format("POST url=%s,%s,%s", url, JSON.toJSONString(params), JSON.toJSONString(body)))
        return if (url.startsWith("https")) {
            post(url + cvtParams(params), JSON.toJSONString(body), true)
        } else {
            post(url + cvtParams(params), JSON.toJSONString(body), false)
        }
    }

    @Throws(Exception::class)
    private fun get(url: String, https: Boolean): String {
        val u = URL(url)
        val http = u.openConnection() as HttpURLConnection
        http.connectTimeout = 50000
        http.readTimeout = 50000
        http.requestMethod = "GET"
        http.setRequestProperty("Content-Type", "application/json")
        if (https) {
            val sslContext = SSLContext.getInstance("SSL", "SunJSSE")
            sslContext.init(null, arrayOf<TrustManager>(X509()), SecureRandom())
            val ssf = sslContext.socketFactory
            (http as HttpsURLConnection).sslSocketFactory = ssf
        }
        http.doOutput = true
        http.doInput = true
        http.connect()
        var str = ""
        http.inputStream.use { `is` ->
            BufferedReader(InputStreamReader(`is`, DEFAULT_CHARSET)).use { reader ->
                str = reader.readText()
            }
        }
        http.disconnect()
        return str
    }

    @Throws(Exception::class)
    private fun get(url: String): String {
        println(String.format(" GET url=%s, params=%s", url, null))
        return if (url.startsWith("https")) {
            get(url, true)
        } else {
            get(url, false)
        }
    }

    @Throws(Exception::class)
    fun get(url: String, params: Map<String, String>): String {
        return if (url.startsWith("https")) {
            get(url + cvtParams(params), true)
        } else {
            get(url + cvtParams(params), false)
        }
    }


    private fun cvtParams(params: Map<String, String>?): String {
        if (params == null || params.isEmpty()) {
            return ""
        }
        val sb = StringBuilder()
        for (entry in params.entries) {
            val key = entry.key
            val value = URLEncoder.encode(entry.value, DEFAULT_CHARSET)
            sb.append("&").append(key).append("=").append(value)
        }
        return "?" + sb.toString().substring(1)
    }

    /**
     *
     * @param objs
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun close(vararg objs: Closeable) {
        if (objs.isNotEmpty()) {
            Arrays.stream(objs).forEach { p ->
                try {
                    p.close()
                } catch (e: Exception) {
                }
            }
        }
    }
}