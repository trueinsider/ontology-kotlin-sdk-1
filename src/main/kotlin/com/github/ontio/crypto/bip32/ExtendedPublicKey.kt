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
import com.github.ontio.crypto.bip32.ByteArrayWriter.Companion.head32
import com.github.ontio.crypto.bip32.ByteArrayWriter.Companion.tail32
import com.github.ontio.crypto.bip32.HmacSha512.hmacSha512
import com.github.ontio.crypto.bip32.Secp256r1SC.gMultiplyAndAddPoint
import com.github.ontio.crypto.bip32.Secp256r1SC.n
import com.github.ontio.crypto.bip32.Secp256r1SC.pointSerP
import io.github.novacrypto.base58.Base58.base58Encode
import io.github.novacrypto.bip32.Index.isHardened
import io.github.novacrypto.bip32.Network
import io.github.novacrypto.bip32.Networks
import io.github.novacrypto.bip32.derivation.CkdFunction
import io.github.novacrypto.bip32.derivation.CkdFunctionDerive
import io.github.novacrypto.bip32.derivation.CkdFunctionResultCacheDecorator.newCacheOf
import io.github.novacrypto.bip32.derivation.Derivation
import io.github.novacrypto.bip32.derivation.Derive
import io.github.novacrypto.hashing.Hash160.hash160into
import io.github.novacrypto.hashing.Sha256.sha256Twice

/**
 * A BIP32 public key
 */
class ExtendedPublicKey internal constructor(private val hdKey: HdKey) : Derive<ExtendedPublicKey>, CKDpub, ExtendedKey {
    override fun cKDpub(index: Int): ExtendedPublicKey? {
        if (isHardened(index)) {
            return null
        }

        val parent = this.hdKey
        val kPar = parent.key

        val data = ByteArray(37)
        val writer = ByteArrayWriter(data)
        writer.concat(kPar, 33)
        writer.concatSer32(index)

        val I = hmacSha512(parent.chainCode, data)
        val Il = head32(I)
        val Ir = tail32(I)

        val parse256_Il = parse256(Il)
        val ki = gMultiplyAndAddPoint(parse256_Il, kPar)

        if (parse256_Il >= n() || ki.isInfinity) {
            return cKDpub(index + 1)
        }

        val key = pointSerP(ki)

        return ExtendedPublicKey(HdKey.Builder()
                .network(parent.network)
                .neutered(true)
                .depth(parent.depth() + 1)
                .parentFingerprint(parent.calculateFingerPrint())
                .key(key)
                .chainCode(Ir)
                .childNumber(index)
                .build())
    }

    override fun extendedKeyByteArray(): ByteArray {
        return hdKey.serialize()
    }

    override fun toNetwork(otherNetwork: Network): ExtendedPublicKey {
        return if (otherNetwork === network()) {
            this
        } else ExtendedPublicKey(
                hdKey.toBuilder()
                        .network(otherNetwork)
                        .build())
    }

    override fun extendedBase58(): String {
        return base58Encode(extendedKeyByteArray())
    }

    fun p2pkhAddress(): String {
        return encodeAddress(hdKey.network.p2pkhVersion(), hdKey.key)
    }

    fun p2shAddress(): String {
        val script = ByteArray(22)
        script[1] = 20.toByte()
        hash160into(script, 2, hdKey.key)
        return encodeAddress(hdKey.network.p2shVersion(), script)
    }

    fun derive(): Derive<ExtendedPublicKey> {
        return derive(CKD_FUNCTION)
    }

    fun deriveWithCache(): Derive<ExtendedPublicKey> {
        return derive(newCacheOf(CKD_FUNCTION))
    }

    override fun derive(derivationPath: CharSequence): ExtendedPublicKey {
        return derive().derive(derivationPath)
    }

    override fun <Path> derive(derivationPath: Path, derivation: Derivation<Path>): ExtendedPublicKey {
        return derive().derive(derivationPath, derivation)
    }

    private fun derive(ckdFunction: CkdFunction<ExtendedPublicKey>): Derive<ExtendedPublicKey> {
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
        fun deserializer(): Deserializer<ExtendedPublicKey> {
            return ExtendedPublicKeyDeserializer.DEFAULT
        }

        fun deserializer(networks: Networks): Deserializer<ExtendedPublicKey> {
            return ExtendedPublicKeyDeserializer(networks)
        }

        private val CKD_FUNCTION = CkdFunction<ExtendedPublicKey> { parent, childIndex -> parent.cKDpub(childIndex) }

        internal fun from(hdKey: HdKey): ExtendedPublicKey {
            return ExtendedPublicKey(HdKey.Builder()
                    .network(hdKey.network)
                    .neutered(true)
                    .key(hdKey.point)
                    .parentFingerprint(hdKey.parentFingerprint)
                    .depth(hdKey.depth())
                    .childNumber(hdKey.childNumber)
                    .chainCode(hdKey.chainCode)
                    .build())
        }

        private fun encodeAddress(version: Byte, data: ByteArray?): String {
            val address = ByteArray(25)
            address[0] = version
            hash160into(address, 1, data!!)
            System.arraycopy(sha256Twice(address, 0, 21), 0, address, 21, 4)
            return base58Encode(address)
        }
    }
}