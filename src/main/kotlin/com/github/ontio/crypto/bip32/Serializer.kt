/*
 *  BIP32 library, a Java implementation of BIP32
 *  Copyright (C) 2017 Alan Evans, NovaCrypto
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

import com.github.ontio.crypto.bip32.Checksum.checksum
import io.github.novacrypto.bip32.Network

internal class Serializer private constructor(builder: Builder) {
    private val network: Network
    private val neutered: Boolean
    private val depth: Int
    private val childNumber: Int
    private val fingerprint: Int

    private val version: Int
        get() = if (neutered) network.publicVersion else network.privateVersion

    init {
        network = builder.network
        neutered = builder.neutered
        depth = builder.depth
        childNumber = builder.childNumber
        fingerprint = builder.fingerprint
    }

    fun serialize(key: ByteArray, chainCode: ByteArray): ByteArray {
        if (chainCode.size != 32) {
            throw IllegalArgumentException("Chain code must be 32 bytes")
        }
        if (neutered) {
            if (key.size != 33) {
                throw IllegalArgumentException("Key must be 33 bytes for neutered serialization")
            }
        } else {
            if (key.size != 32) {
                throw IllegalArgumentException("Key must be 32 bytes for non neutered serialization")
            }
        }

        val privateKey = ByteArray(82)
        val writer = ByteArrayWriter(privateKey)
        writer.concatSer32(version)
        writer.concat(depth.toByte())
        writer.concatSer32(fingerprint)
        writer.concatSer32(childNumber)
        writer.concat(chainCode)
        if (!neutered) {
            writer.concat(0.toByte())
            writer.concat(key)
        } else {
            writer.concat(key)
        }
        writer.concat(checksum(privateKey), 4)
        return privateKey
    }

    internal class Builder {
        internal lateinit var network: Network
            private set
        internal var neutered: Boolean = false
            private set
        internal var depth: Int = 0
            private set
        internal var childNumber: Int = 0
            private set
        internal var fingerprint: Int = 0
            private set

        fun network(network: Network): Builder {
            this.network = network
            return this
        }

        fun neutered(neutered: Boolean): Builder {
            this.neutered = neutered
            return this
        }

        fun depth(depth: Int): Builder {
            if (depth < 0 || depth > 255) {
                throw IllegalArgumentException("Depth must be [0..255]")
            }
            this.depth = depth
            return this
        }

        fun childNumber(childNumber: Int): Builder {
            this.childNumber = childNumber
            return this
        }

        fun fingerprint(fingerprint: Int): Builder {
            this.fingerprint = fingerprint
            return this
        }

        fun build(): Serializer {
            return Serializer(this)
        }
    }
}