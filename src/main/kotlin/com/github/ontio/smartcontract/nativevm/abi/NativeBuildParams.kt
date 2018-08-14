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
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.core.ontid.Attribute
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.io.BinaryWriter
import com.github.ontio.sdk.exception.SDKException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.util.*

object NativeBuildParams {
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
                } else if (param is Array<*> && param.isArrayOf<Attribute>()) {
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
        for (i in list.indices.reversed()) {
            val `val` = list[i]
            when (`val`) {
                is ByteArray -> builder.emitPushByteArray(`val`)
                is Boolean -> builder.emitPushBool(`val`)
                is Int -> builder.emitPushInteger(BigInteger.valueOf(`val`.toLong()))
                is Long -> builder.emitPushInteger(BigInteger.valueOf(`val`))
                is Address -> builder.emitPushByteArray(`val`.toArray())
                is String -> builder.emitPushByteArray(`val`.toByteArray())
                is Struct -> {
                    builder.emitPushInteger(BigInteger.valueOf(0))
                    builder.add(ScriptOp.OP_NEWSTRUCT)
                    builder.add(ScriptOp.OP_TOALTSTACK)
                    for (k in `val`.list.indices) {
                        val o = `val`.list[k]
                        val tmpList = mutableListOf<Any>()
                        tmpList.add(o)
                        createCodeParamsScript(builder, tmpList)
                        builder.add(ScriptOp.OP_DUPFROMALTSTACK)
                        builder.add(ScriptOp.OP_SWAP)
                        builder.add(ScriptOp.OP_APPEND)
                    }
                    builder.add(ScriptOp.OP_FROMALTSTACK)
                }
                is List<*> -> {
                    for (k in `val`.indices.reversed()) {
                        val tmpList = mutableListOf<Any>()
                        tmpList.add(`val`[k]!!)
                        createCodeParamsScript(builder, tmpList)
                    }
                    builder.emitPushInteger(BigInteger(`val`.size.toString()))
                    builder.pushPack()
                }
                else -> throw SDKException(ErrorCode.OtherError("not this type"))
            }
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

    fun serializeAbiFunction(abiFunction: AbiFunction): ByteArray {
        val list = ArrayList<Any>()
        list.add(abiFunction.name!!.toByteArray())
        val tmp = ArrayList<Any>()
        for (obj in abiFunction.parameters!!) {
            when {
                "Byte" == obj.type -> tmp.add(JSON.parseObject(obj.value, Byte::class.javaPrimitiveType))
                "ByteArray" == obj.type -> tmp.add(JSON.parseObject(obj.value, ByteArray::class.java))
                "String" == obj.type -> tmp.add(obj.value!!)
                "Bool" == obj.type -> tmp.add(JSON.parseObject(obj.value, Boolean::class.javaPrimitiveType))
                "Int" == obj.type -> tmp.add(JSON.parseObject(obj.value, Long::class.java))
                "Array" == obj.type -> tmp.add(JSON.parseObject(obj.value, java.lang.reflect.Array::class.java))
                "Struct" == obj.type -> {}
                "Uint256" == obj.type -> {}
                "Address" == obj.type -> {}
                else -> throw SDKException(ErrorCode.TypeError)
            }
        }
        if (list.size > 0) {
            list.add(tmp)
        }
        return createCodeParamsScript(list)
    }
}