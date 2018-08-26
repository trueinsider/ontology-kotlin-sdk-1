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

package com.github.ontio.smartcontract.neovm.abi

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.sdk.exception.SDKException
import java.math.BigInteger
import java.util.*

object BuildParams {
    enum class Type constructor(t: Int) {
        ByteArrayType(0x00),
        BooleanType(0x01),
        IntegerType(0x02),
        InterfaceType(0x40),
        ArrayType(0x80),
        StructType(0x81),
        MapType(0x82);

        val value: Byte = t.toByte()
    }

    /**
     * @param builder
     * @param list
     * @return
     */
    private fun createCodeParamsScript(builder: ScriptBuilder, list: List<Any>): ByteArray {
        for (i in list.indices.reversed()) {
            val `val` = list[i]
            when (`val`) {
                is ByteArray -> builder.emitPushByteArray(`val`)
                is String -> builder.emitPushByteArray(`val`.toByteArray())
                is Boolean -> builder.emitPushBool(`val`)
                is Int -> builder.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf(`val`.toLong())))
                is Long -> builder.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf(`val`)))
                is Map<*, *> -> {
                    val bys = getMapBytes(`val`)
                    println(Helper.toHexString(bys))
                    builder.emitPushByteArray(bys)
                }
                is Struct -> {
                    val bys = getStructBytes(`val`)
                    builder.emitPushByteArray(bys)
                }
                is List<*> -> {
                    createCodeParamsScript(builder, `val` as List<Any>)
                    builder.emitPushInteger(BigInteger(`val`.size.toString()))
                    builder.pushPack()

                }
                else -> {
                }
            }
        }

        return builder.toArray()
    }

    fun getStructBytes(`val`: Any): ByteArray {
        val sb = ScriptBuilder()
        val list = (`val` as Struct).list
        sb.add(Type.StructType.value)
        sb.add(Helper.BigIntToNeoBytes(BigInteger.valueOf(list.size.toLong())))
        for (i in list.indices) {
            when {
                list[i] is ByteArray -> {
                    sb.add(Type.ByteArrayType.value)
                    sb.emitPushByteArray(list[i] as ByteArray)
                }
                list[i] is String -> {
                    sb.add(Type.ByteArrayType.value)
                    sb.emitPushByteArray((list[i] as String).toByteArray())
                }
                list[i] is Int -> {
                    sb.add(Type.ByteArrayType.value)
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((list[i] as Int).toLong())))
                }
                list[i] is Long -> {
                    sb.add(Type.ByteArrayType.value)
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf(list[i] as Long)))
                }
                else -> throw SDKException(ErrorCode.ParamError)
            }
        }

        return sb.toArray()
    }

    fun getMapBytes(`val`: Any): ByteArray {
        val sb = ScriptBuilder()
        val map = `val` as Map<*, *>
        sb.add(Type.MapType.value)
        sb.add(Helper.BigIntToNeoBytes(BigInteger.valueOf(map.size.toLong())))
        for ((key, value) in map) {
            sb.add(Type.ByteArrayType.value)
            sb.emitPushByteArray((key as String).toByteArray())
            when (value) {
                is ByteArray -> {
                    sb.add(Type.ByteArrayType.value)
                    sb.emitPushByteArray(value)
                }
                is String -> {
                    sb.add(Type.ByteArrayType.value)
                    sb.emitPushByteArray(value.toByteArray())
                }
                is Int -> {
                    sb.add(Type.IntegerType.value)
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf(value.toLong())))
                }
                is Long -> {
                    sb.add(Type.IntegerType.value)
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf(value)))
                }
                else -> throw SDKException(ErrorCode.ParamError)
            }
        }

        return sb.toArray()
    }

    /**
     * @param list
     * @return
     */
    fun createCodeParamsScript(list: List<Any>): ByteArray {
        val sb = ScriptBuilder()
        for (i in list.indices.reversed()) {
            val `val` = list[i]
            when (`val`) {
                is ByteArray -> sb.emitPushByteArray(`val`)
                is String -> sb.emitPushByteArray(`val`.toByteArray())
                is Boolean -> sb.emitPushBool(`val`)
                is Int -> sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf(`val`.toLong())))
                is Long -> sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf(`val`)))
                is BigInteger -> sb.emitPushInteger(`val`)
                is Map<*, *> -> {
                    val bys = getMapBytes(`val`)
                    sb.emitPushByteArray(bys)
                }
                is Struct -> {
                    val bys = getStructBytes(`val`)
                    sb.emitPushByteArray(bys)
                }
                is List<*> -> {
                    createCodeParamsScript(sb, `val` as List<Any>)
                    sb.emitPushInteger(BigInteger(`val`.size.toString()))
                    sb.pushPack()
                }
                else -> {
                }
            }
        }

        return sb.toArray()
    }

    /**
     * @param abiFunction
     * @return
     * @throws Exception
     */
    fun serializeAbiFunction(abiFunction: AbiFunction): ByteArray {
        val list = ArrayList<Any>()
        list.add(abiFunction.name!!.toByteArray())
        val tmp = ArrayList<Any>()
        for (obj in abiFunction.parameters!!) {
            when {
                "ByteArray" == obj.type -> tmp.add(JSON.parseObject(obj.value, ByteArray::class.java))
                "String" == obj.type -> tmp.add(obj.value!!)
                "Boolean" == obj.type -> tmp.add(JSON.parseObject(obj.value, Boolean::class.javaPrimitiveType))
                "Integer" == obj.type -> tmp.add(JSON.parseObject(obj.value, Long::class.java))
                "Array" == obj.type -> tmp.add(JSON.parseObject(obj.value, List::class.java))
                "InteropInterface" == obj.type -> tmp.add(JSON.parseObject(obj.value, Any::class.java))
                "Void" == obj.type -> {}
                "Map" == obj.type -> tmp.add(JSON.parseObject(obj.value, Map::class.java))
                "Struct" == obj.type -> tmp.add(JSON.parseObject(obj.value, Struct::class.java))
                else -> throw SDKException(ErrorCode.TypeError)
            }
        }
        if (list.size > 0) {
            list.add(tmp)
        }
        return createCodeParamsScript(list)
    }
}