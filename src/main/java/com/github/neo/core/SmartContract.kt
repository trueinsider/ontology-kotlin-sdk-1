package com.github.neo.core


import com.alibaba.fastjson.JSON
import com.github.neo.core.transaction.InvocationTransaction
import com.github.ontio.common.Fixed8
import com.github.ontio.common.Helper
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.neo.core.transaction.PublishTransaction
import com.github.ontio.smartcontract.neovm.abi.AbiFunction

import java.math.BigInteger
import java.util.ArrayList
import java.util.UUID

/**
 *
 */
class SmartContract {
    companion object {
        @Throws(Exception::class)
        fun makeInvocationTransaction(contractAddress: String?, addr: ByteArray, abiFunction: AbiFunction): InvocationTransaction {
            var contractAddress: String? = contractAddress ?: throw Exception("null contractHash")
            contractAddress = contractAddress!!.replace("0x", "")
            var params = serializeAbiFunction(abiFunction)
            params = Helper.addBytes(params, byteArrayOf(0x67))
            params = Helper.addBytes(params, Helper.hexToBytes(contractAddress))

            return makeInvocationTransaction(params, addr)
        }

        @Throws(Exception::class)
        private fun serializeAbiFunction(abiFunction: AbiFunction): ByteArray {
            val list = ArrayList<Any>()
            list.add(abiFunction.name!!.toByteArray())
            val tmp = ArrayList<Any>()
            for (obj in abiFunction.parameters!!) {
                if ("ByteArray" == obj.type) {
                    tmp.add(JSON.parseObject(obj.value, ByteArray::class.java))
                } else if ("String" == obj.type) {
                    tmp.add(obj.value!!)
                } else if ("Boolean" == obj.type) {
                    tmp.add(JSON.parseObject(obj.value, Boolean::class.javaPrimitiveType))
                } else if ("Integer" == obj.type) {
                    tmp.add(JSON.parseObject(obj.value, Long::class.java))
                } else if ("Array" == obj.type) {
                    tmp.add(JSON.parseObject(obj.value, List::class.java))
                } else if ("InteropInterface" == obj.type) {
                    tmp.add(JSON.parseObject(obj.value, Any::class.java))
                } else if ("Void" == obj.type) {

                } else {
                    throw Exception("type error")
                }
            }
            if (list.size > 0) {
                list.add(tmp)
            }
            return createCodeParamsScript(list)
        }

        private fun createCodeParamsScript(builder: ScriptBuilder, list: List<*>): ByteArray {
            try {
                for (i in list.indices.reversed()) {
                    val `val` = list[i]
                    if (`val` is ByteArray) {
                        builder.emitPushByteArray(`val`)
                    } else if (`val` is Boolean) {
                        builder.emitPushBool(`val`)
                    } else if (`val` is Long) {
                        builder.emitPushInteger(BigInteger.valueOf(`val`))
                    } else if (`val` is List<*>) {
                        createCodeParamsScript(builder, `val`)
                        builder.emitPushInteger(BigInteger(`val`.size.toString()))
                        builder.pushPack()

                    } else {
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
            try {
                for (i in list.indices.reversed()) {
                    val `val` = list[i]
                    if (`val` is ByteArray) {
                        sb.emitPushByteArray(`val`)
                    } else if (`val` is Boolean) {
                        sb.emitPushBool(`val`)
                    } else if (`val` is Long) {
                        sb.emitPushInteger(BigInteger.valueOf(`val`))
                    } else if (`val` is List<*>) {
                        createCodeParamsScript(sb, `val`)
                        sb.emitPushInteger(BigInteger(`val`.size.toString()))
                        sb.pushPack()
                    } else {
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return sb.toArray()
        }

        @Throws(Exception::class)
        fun makePublishTransaction(
                codeStr: String,
                needStorage: Boolean,
                name: String,
                codeVersion: String,
                author: String,
                email: String,
                desp: String,
                returnType: ContractParameterType
        ) = PublishTransaction(
                1,
                arrayOf(TransactionAttribute(TransactionAttributeUsage.DescriptionUrl, UUID.randomUUID().toString().toByteArray())),
                emptyArray(),
                emptyArray(),
                Helper.hexToBytes(codeStr),
                arrayOf(ContractParameterType.ByteArray, ContractParameterType.Array),
                returnType,
                needStorage,
                name,
                codeVersion,
                author,
                email,
                desp
        )

        @Throws(Exception::class)
        fun makeInvocationTransaction(paramsHexStr: ByteArray, addr: ByteArray) = InvocationTransaction(
                1,
                arrayOf(
                        TransactionAttribute(TransactionAttributeUsage.Script, addr),
                        TransactionAttribute(
                                TransactionAttributeUsage.DescriptionUrl,
                                UUID.randomUUID().toString().toByteArray()
                        )
                ),
                emptyArray(),
                emptyArray(),
                paramsHexStr,
                Fixed8(0)
        )
    }
}
