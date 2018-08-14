package com.github.neo.core


import com.alibaba.fastjson.JSON
import com.github.neo.core.transaction.InvocationTransaction
import com.github.neo.core.transaction.PublishTransaction
import com.github.ontio.common.Fixed8
import com.github.ontio.common.Helper
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.smartcontract.neovm.abi.AbiFunction
import java.math.BigInteger
import java.util.*

/**
 *
 */
class SmartContract {
    companion object {
        fun makeInvocationTransaction(contractAddress: String, addr: ByteArray, abiFunction: AbiFunction): InvocationTransaction {
            val contractAddress = contractAddress.replace("0x", "")
            var params = serializeAbiFunction(abiFunction)
            params = Helper.addBytes(params, byteArrayOf(0x67))
            params = Helper.addBytes(params, Helper.hexToBytes(contractAddress))

            return makeInvocationTransaction(params, addr)
        }

        private fun serializeAbiFunction(abiFunction: AbiFunction): ByteArray {
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
                    else -> throw Exception("type error")
                }
            }
            if (list.size > 0) {
                list.add(tmp)
            }
            return createCodeParamsScript(list)
        }

        private fun createCodeParamsScript(builder: ScriptBuilder, list: List<*>): ByteArray {
            for (i in list.indices.reversed()) {
                val `val` = list[i]
                when (`val`) {
                    is ByteArray -> builder.emitPushByteArray(`val`)
                    is Boolean -> builder.emitPushBool(`val`)
                    is Long -> builder.emitPushInteger(BigInteger.valueOf(`val`))
                    is List<*> -> {
                        createCodeParamsScript(builder, `val`)
                        builder.emitPushInteger(BigInteger(`val`.size.toString()))
                        builder.pushPack()
                    }
                    else -> {}
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
            for (i in list.indices.reversed()) {
                val `val` = list[i]
                when (`val`) {
                    is ByteArray -> sb.emitPushByteArray(`val`)
                    is Boolean -> sb.emitPushBool(`val`)
                    is Long -> sb.emitPushInteger(BigInteger.valueOf(`val`))
                    is List<*> -> {
                        createCodeParamsScript(sb, `val`)
                        sb.emitPushInteger(BigInteger(`val`.size.toString()))
                        sb.pushPack()
                    }
                    else -> {}
                }
            }

            return sb.toArray()
        }

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
