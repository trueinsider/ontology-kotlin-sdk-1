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
import com.github.ontio.crypto.bip32.Secp256r1SC.pointSerP_gMultiply
import io.github.novacrypto.bip32.Network
import io.github.novacrypto.hashing.Hash160.hash160

internal class HdKey private constructor(builder: Builder) {
    private val neutered: Boolean
    val network: Network
    val chainCode: ByteArray
    val key: ByteArray
    private val serializer: Serializer
    val parentFingerprint: Int
    val childNumber: Int
    private val depth: Int

    val point: ByteArray
        get() = pointSerP_gMultiply(parse256(key))

    private val publicBuffer: ByteArray?
        get() = if (neutered) key else point

    init {
        neutered = builder.neutered
        network = builder.network
        key = builder.key
        parentFingerprint = builder.parentFingerprint
        childNumber = builder.childNumber
        chainCode = builder.chainCode
        depth = builder.depth
        serializer = Serializer.Builder()
                .network(builder.network)
                .neutered(builder.neutered)
                .depth(builder.depth)
                .childNumber(builder.childNumber)
                .fingerprint(builder.parentFingerprint)
                .build()
    }

    fun serialize(): ByteArray {
        return serializer.serialize(key, chainCode)
    }

    fun calculateFingerPrint(): Int {
        val point = publicBuffer
        val o = hash160(point!!)
        return o[0].toInt() and 0xFF shl 24 or
                (o[1].toInt() and 0xFF shl 16) or
                (o[2].toInt() and 0xFF shl 8) or
                (o[3].toInt() and 0xFF)
    }

    fun depth(): Int {
        return depth
    }

    fun toBuilder(): Builder {
        return Builder()
                .neutered(neutered)
                .chainCode(chainCode)
                .key(key)
                .depth(depth)
                .childNumber(childNumber)
                .parentFingerprint(parentFingerprint)
    }

    internal class Builder {
        internal lateinit var network: Network
            private set
        internal var neutered: Boolean = false
            private set
        internal lateinit var chainCode: ByteArray
            private set
        internal lateinit var key: ByteArray
            private set
        internal var depth: Int = 0
            private set
        internal var childNumber: Int = 0
            private set
        internal var parentFingerprint: Int = 0
            private set

        fun network(network: Network): Builder {
            this.network = network
            return this
        }

        fun neutered(neutered: Boolean): Builder {
            this.neutered = neutered
            return this
        }

        fun key(key: ByteArray): Builder {
            this.key = key
            return this
        }

        fun chainCode(chainCode: ByteArray): Builder {
            this.chainCode = chainCode
            return this
        }

        fun depth(depth: Int): Builder {
            this.depth = depth
            return this
        }

        fun childNumber(childNumber: Int): Builder {
            this.childNumber = childNumber
            return this
        }

        fun parentFingerprint(parentFingerprint: Int): Builder {
            this.parentFingerprint = parentFingerprint
            return this
        }

        fun build(): HdKey {
            return HdKey(this)
        }
    }
}