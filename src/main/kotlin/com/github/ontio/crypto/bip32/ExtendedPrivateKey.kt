/*
 *  BIP32 library, a Java implementation of BIP32
 *  Copyright (C) 2017-2018 Alan Evans, NovaCrypto
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  Original source: https://github.com/NovaCrypto/BIP32
 *  You can contact the authors via github issues.
 */

package com.github.ontio.crypto.bip32

import com.github.ontio.crypto.bip32.BigIntegerUtils.parse256
import com.github.ontio.crypto.bip32.BigIntegerUtils.ser256
import com.github.ontio.crypto.bip32.ByteArrayWriter.Companion.head32
import com.github.ontio.crypto.bip32.ByteArrayWriter.Companion.tail32
import com.github.ontio.crypto.bip32.HmacSha512.hmacSha512
import com.github.ontio.crypto.bip32.Secp256r1SC.n
import io.github.novacrypto.base58.Base58.base58Encode
import io.github.novacrypto.bip32.Index.isHardened
import io.github.novacrypto.bip32.Network
import io.github.novacrypto.bip32.Networks
import io.github.novacrypto.bip32.derivation.CkdFunction
import io.github.novacrypto.bip32.derivation.CkdFunctionDerive
import io.github.novacrypto.bip32.derivation.CkdFunctionResultCacheDecorator.newCacheOf
import io.github.novacrypto.bip32.derivation.Derivation
import io.github.novacrypto.bip32.derivation.Derive
import io.github.novacrypto.toruntime.CheckedExceptionToRuntime
import io.github.novacrypto.toruntime.CheckedExceptionToRuntime.toRuntime
import java.math.BigInteger
import java.util.*

/**
 * A BIP32 private key
 */
class ExtendedPrivateKey internal constructor(private val hdKey: HdKey) : Derive<ExtendedPrivateKey>, CKDpriv, CKDpub, ExtendedKey {
    private constructor(network: Network, key: ByteArray, chainCode: ByteArray) : this(HdKey.Builder()
            .network(network)
            .neutered(false)
            .key(key)
            .chainCode(chainCode)
            .depth(0)
            .childNumber(0)
            .parentFingerprint(0)
            .build())

    override fun extendedKeyByteArray(): ByteArray {
        return hdKey.serialize()
    }

    override fun toNetwork(otherNetwork: Network): ExtendedPrivateKey {
        return if (otherNetwork === network()) {
            this
        } else ExtendedPrivateKey(
                hdKey.toBuilder()
                        .network(otherNetwork)
                        .build())
    }

    override fun extendedBase58(): String {
        return base58Encode(extendedKeyByteArray())
    }

    override fun cKDpriv(index: Int): ExtendedPrivateKey {
        val data = ByteArray(37)
        val writer = ByteArrayWriter(data)

        if (isHardened(index)) {
            writer.concat(0.toByte())
            writer.concat(hdKey.key, 32)
        } else {
            writer.concat(hdKey.point)
        }
        writer.concatSer32(index)

        val I = hmacSha512(hdKey.chainCode, data)
        Arrays.fill(data, 0.toByte())

        val Il = head32(I)
        val Ir = tail32(I)

        val key = hdKey.key
        val parse256_Il = parse256(Il)
        val ki = parse256_Il.add(parse256(key)).mod(n())

        if (parse256_Il >= n() || ki == BigInteger.ZERO) {
            return cKDpriv(index + 1)
        }

        ser256(Il, ki)

        return ExtendedPrivateKey(HdKey.Builder()
                .network(hdKey.network)
                .neutered(false)
                .key(Il)
                .chainCode(Ir)
                .depth(hdKey.depth() + 1)
                .childNumber(index)
                .parentFingerprint(hdKey.calculateFingerPrint())
                .build())
    }

    override fun cKDpub(index: Int): ExtendedPublicKey {
        return cKDpriv(index).neuter()
    }

    fun neuter(): ExtendedPublicKey {
        return ExtendedPublicKey.from(hdKey)
    }

    fun derive(): Derive<ExtendedPrivateKey> {
        return derive(CKD_FUNCTION)
    }

    fun deriveWithCache(): Derive<ExtendedPrivateKey> {
        return derive(newCacheOf(CKD_FUNCTION))
    }

    override fun derive(derivationPath: CharSequence): ExtendedPrivateKey {
        return derive().derive(derivationPath)
    }

    override fun <Path> derive(derivationPath: Path, derivation: Derivation<Path>): ExtendedPrivateKey {
        return derive().derive(derivationPath, derivation)
    }

    private fun derive(ckdFunction: CkdFunction<ExtendedPrivateKey>): Derive<ExtendedPrivateKey> {
        return CkdFunctionDerive(ckdFunction, this)
    }

    override fun network(): Network {
        return hdKey.network
    }

    override fun depth(): Int {
        return hdKey.depth()
    }

    override fun childNumber(): Int {
        return hdKey.childNumber
    }

    companion object {

        fun deserializer(): Deserializer<ExtendedPrivateKey> {
            return ExtendedPrivateKeyDeserializer.DEFAULT
        }

        fun deserializer(networks: Networks): Deserializer<ExtendedPrivateKey> {
            return ExtendedPrivateKeyDeserializer(networks)
        }

        private val CKD_FUNCTION = CkdFunction<ExtendedPrivateKey> { parent, childIndex -> parent.cKDpriv(childIndex) }

        private val BITCOIN_SEED = getBytes("Bitcoin seed")

        fun fromSeed(seed: ByteArray, network: Network): ExtendedPrivateKey {
            val I = hmacSha512(BITCOIN_SEED, seed)

            val Il = head32(I)
            val Ir = tail32(I)

            return ExtendedPrivateKey(network, Il, Ir)
        }

        fun fromSeed(seed: ByteArray, byteKey: ByteArray, network: Network): ExtendedPrivateKey {
            val I = hmacSha512(byteKey, seed)

            val Il = head32(I)
            val Ir = tail32(I)

            return ExtendedPrivateKey(network, Il, Ir)
        }

        private fun getBytes(seed: String): ByteArray {
            return toRuntime(CheckedExceptionToRuntime.Func { seed.toByteArray(charset("UTF-8")) })
        }
    }
}