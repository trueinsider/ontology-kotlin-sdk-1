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

package com.github.ontio.smartcontract.nativevm.abi

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.ontid.Attribute
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryWriter
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams.createCodeParamsScript

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.reflect.Array
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList

object NativeBuildParams {
    @Throws(SDKException::class)
    fun buildParams(vararg params: Any): ByteArray {
        val baos = ByteArrayOutputStream()
        val bw = BinaryWriter(baos)
        try {
            for (param in params) {
                if (param is Int) {
                    bw.writeInt(param.toInt())
                } else if (param is ByteArray) {
                    bw.writeVarBytes(param)
                } else if (param is String) {
                    bw.writeVarString(param)
                } else if (param is Array<Attribute>) {
                    bw.writeSerializableArray(param as Array<Attribute>)
                } else if (param is Attribute) {
                    bw.writeSerializable(param)
                } else if (param is Address) {
                    bw.writeSerializable(param)
                } else {
                    throw SDKException(ErrorCode.WriteVarBytesError)
                }
            }
        } catch (e: IOException) {
            throw SDKException(ErrorCode.WriteVarBytesError)
        }

        return baos.toByteArray()
    }

    private fun createCodeParamsScript(builder: ScriptBuilder, list: List<Any>): ByteArray {
        try {
            for (i in list.indices.reversed()) {
                val `val` = list[i]
                if (`val` is ByteArray) {
                    builder.emitPushByteArray(`val`)
                } else if (`val` is Boolean) {
                    builder.emitPushBool(`val`)
                } else if (`val` is Int) {
                    builder.emitPushInteger(BigInteger.valueOf(`val`.toLong()))
                } else if (`val` is Long) {
                    builder.emitPushInteger(BigInteger.valueOf(`val`))
                } else if (`val` is Address) {
                    builder.emitPushByteArray(`val`.toArray())
                } else if (`val` is String) {
                    builder.emitPushByteArray(`val`.toByteArray())
                } else if (`val` is Struct) {
                    builder.emitPushInteger(BigInteger.valueOf(0))
                    builder.add(ScriptOp.OP_NEWSTRUCT)
                    builder.add(ScriptOp.OP_TOALTSTACK)
                    for (k in `val`.list.indices) {
                        val o = `val`.list[k]
                        val tmpList = ArrayList()
                        tmpList.add(o)
                        createCodeParamsScript(builder, tmpList)
                        builder.add(ScriptOp.OP_DUPFROMALTSTACK)
                        builder.add(ScriptOp.OP_SWAP)
                        builder.add(ScriptOp.OP_APPEND)
                    }
                    builder.add(ScriptOp.OP_FROMALTSTACK)
                } else if (`val` is List<*>) {
                    for (k in `val`.indices.reversed()) {
                        val tmpList = ArrayList()
                        tmpList.add(`val`[k])
                        createCodeParamsScript(builder, tmpList)
                    }
                    builder.emitPushInteger(BigInteger(`val`.size.toString()))
                    builder.pushPack()
                } else {
                    throw SDKException(ErrorCode.OtherError("not this type"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return builder.toArray()
    }

    /**
     * @param list
     * @return
     */
    fun createCodeParamsScript(list: List<Any>): ByteArray {
        val sb = ScriptBuilder()
        return createCodeParamsScript(sb, list)
    }

    @Throws(Exception::class)
    fun serializeAbiFunction(abiFunction: AbiFunction): ByteArray {
        val list = ArrayList<Any>()
        list.add(abiFunction.name!!.toByteArray())
        val tmp = ArrayList<Any>()
        for (obj in abiFunction.parameters!!) {
            if ("Byte" == obj.type) {
                tmp.add(JSON.parseObject(obj.value, Byte::class.javaPrimitiveType))
            } else if ("ByteArray" == obj.type) {
                tmp.add(JSON.parseObject(obj.value, ByteArray::class.java))
            } else if ("String" == obj.type) {
                tmp.add(obj.value)
            } else if ("Bool" == obj.type) {
                tmp.add(JSON.parseObject(obj.value, Boolean::class.javaPrimitiveType))
            } else if ("Int" == obj.type) {
                tmp.add(JSON.parseObject(obj.value, Long::class.java))
            } else if ("Array" == obj.type) {
                tmp.add(JSON.parseObject(obj.value, Array::class.java))
            } else if ("Struct" == obj.type) {
                //tmp.add(JSON.parseObject(obj.getValue(), Object.class));
            } else if ("Uint256" == obj.type) {

            } else if ("Address" == obj.type) {

            } else {
                throw SDKException(ErrorCode.TypeError)
            }
        }
        if (list.size > 0) {
            list.add(tmp)
        }
        return createCodeParamsScript(list)
    }
}