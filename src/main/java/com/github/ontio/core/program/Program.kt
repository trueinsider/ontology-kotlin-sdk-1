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

package com.github.ontio.core.program

import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.scripts.ScriptBuilder
import com.github.ontio.core.scripts.ScriptOp
import com.github.ontio.crypto.ECC
import com.github.ontio.crypto.KeyType
import com.github.ontio.io.BinaryReader
import com.github.ontio.sdk.exception.SDKException
import java.io.ByteArrayInputStream
import java.io.IOException
import java.math.BigInteger
import java.util.*

/**
 *
 */
object Program {
    fun ProgramFromParams(sigData: Array<ByteArray>): ByteArray {
        sigData.sortBy(Helper::toHexString)
        val sb = ScriptBuilder()
        for (sig in sigData) {
            sb.emitPushByteArray(sig)
        }
        return sb.toArray()
    }

    fun ProgramFromPubKey(publicKey: ByteArray): ByteArray {
        val sb = ScriptBuilder()
        sb.emitPushByteArray(publicKey)
        sb.add(ScriptOp.OP_CHECKSIG)
        return sb.toArray()
    }

    fun ProgramFromMultiPubKey(m: Int, vararg publicKeys: ByteArray): ByteArray {
        val n = publicKeys.size

        if (m <= 0 || m > n || n > Common.MULTI_SIG_MAX_PUBKEY_SIZE) {
            throw SDKException(ErrorCode.ParamError)
        }
        ScriptBuilder().use { sb ->
            sb.emitPushInteger(BigInteger.valueOf(m.toLong()))
            val publicKeys = sortPublicKeys(*publicKeys)
            for (publicKey in publicKeys) {
                sb.emitPushByteArray(publicKey)
            }
            sb.emitPushInteger(BigInteger.valueOf(publicKeys.size.toLong()))
            sb.add(ScriptOp.OP_CHECKMULTISIG)
            return sb.toArray()
        }
    }

    fun sortPublicKeys(vararg publicKeys: ByteArray): Array<ByteArray> {
        return publicKeys.copyOf().apply { sortWith(Comparator { o1, o2 ->
            if (KeyType.fromPubkey(o1).label != KeyType.fromPubkey(o2).label) {
                return@Comparator if (KeyType.fromPubkey(o1).label >= KeyType.fromPubkey(o2).label) 1 else -1
            }
            when (KeyType.fromPubkey(o1)) {
                KeyType.SM2 -> {
                    val p = ByteArray(33)
                    System.arraycopy(o1, 2, p, 0, p.size)
                    val p2 = ByteArray(33)
                    System.arraycopy(o2, 2, p2, 0, p2.size)
                    val smPk1 = ECC.sm2p256v1.curve.decodePoint(p)
                    val smPk2 = ECC.sm2p256v1.curve.decodePoint(p2)
                    return@Comparator ECC.compare(smPk1, smPk2)
                }
                KeyType.ECDSA -> {
                    val pk1 = ECC.secp256r1.curve.decodePoint(o1)
                    val pk2 = ECC.secp256r1.curve.decodePoint(o2)
                    return@Comparator ECC.compare(pk1, pk2)
                }
                KeyType.EDDSA ->
                    //TODO
                    return@Comparator Helper.toHexString(o1).compareTo(Helper.toHexString(o1))
                else -> return@Comparator Helper.toHexString(o1).compareTo(Helper.toHexString(o1))
            }
        }) } as Array<ByteArray>
    }

    fun getParamInfo(program: ByteArray): Array<ByteArray> {
        val bais = ByteArrayInputStream(program)
        val br = BinaryReader(bais)
        val list = mutableListOf<ByteArray>()
        while (true) {
            try {
                list.add(readBytes(br))
            } catch (e: IOException) {
                break
            }
        }
        return list.toTypedArray()
    }

    fun readBytes(br: BinaryReader): ByteArray {
        val code = br.readByte()
        val keyLen: Long
        keyLen = if (code == ScriptOp.OP_PUSHDATA4.byte) {
            val temp: Int = br.readInt()
            temp.toLong()
        } else if (code == ScriptOp.OP_PUSHDATA2.byte) {
            val temp: Int = br.readShort().toInt()
            temp.toLong()
        } else if (code == ScriptOp.OP_PUSHDATA1.byte) {
            val temp: Int = br.readByte().toInt()
            temp.toLong()
        } else if (code <= ScriptOp.OP_PUSHBYTES75.byte && code >= ScriptOp.OP_PUSHBYTES1.byte) {
            code.toLong() - ScriptOp.OP_PUSHBYTES1.byte.toLong() + 1
        } else {
            0
        }
        return br.readBytes(keyLen.toInt())
    }

    fun getProgramInfo(program: ByteArray): ProgramInfo? {
        if (program.size <= 2) {
        }
        val end = program[program.size - 1]
        val temp = ByteArray(program.size - 1)
        System.arraycopy(program, 0, temp, 0, program.size - 1)
        val bais = ByteArrayInputStream(temp)
        val reader = BinaryReader(bais)
        if (end == ScriptOp.OP_CHECKSIG.byte) {
            val publicKey = readBytes(reader)
            return ProgramInfo(arrayOf(publicKey), 1.toShort())
        } else if (end == ScriptOp.OP_CHECKMULTISIG.byte) {
            val m = (reader.readByte() - ScriptOp.OP_PUSH1.byte + 1).toByte().toShort()
            val len = program[program.size - 2] - ScriptOp.OP_PUSH1.byte + 1
            val pub = Array(len) { reader.readVarBytes() }
            return ProgramInfo(pub, m)
        }
        return null
    }

    fun readNum(reader: BinaryReader): Short {
        val code = readOpCode(reader)
        if (code == ScriptOp.OP_PUSH0) {
            readOpCode(reader)
            return 0
        } else {
            val num = code!!.byte.toInt() - ScriptOp.OP_PUSH1.byte.toInt() + 1
            if (num in 1..16) {
                readOpCode(reader)
                return num.toShort()
            }
        }
        val buff = readBytes(reader)
        val bint = Helper.BigIntFromNeoBytes(buff)
        val num = bint.toLong()
        if (num > Short.MAX_VALUE || num < 16) {
            throw SDKException(ErrorCode.ParamErr("num is wrong"))
        }
        return num.toShort()
    }

    fun readOpCode(reader: BinaryReader): ScriptOp? {
        return ScriptOp.valueOf(reader.readByte().toInt())
    }

    fun programFromParams(sigs: Array<ByteArray>): ByteArray {
        val builder = ScriptBuilder()
        for (sigdata in sigs) {
            builder.emitPushByteArray(sigdata)
        }
        return builder.toArray()
    }

    fun programFromPubKey(publicKey: ByteArray): ByteArray {
        val builder = ScriptBuilder()
        builder.emitPushByteArray(publicKey)
        builder.emit(ScriptOp.OP_CHECKSIG)
        return builder.toArray()
    }

    fun programFromMultiPubKey(publicKey: Array<ByteArray>, m: Short): ByteArray {
        val n = publicKey.size
        if (m in 1..n && n <= 1024) {
            throw SDKException(ErrorCode.ParamErr("m is wrong"))
        }
        val builder = ScriptBuilder()
        builder.pushNum(m)
        builder.add(ScriptOp.OP_CHECKSIG)
        return builder.toArray()
    }
}

