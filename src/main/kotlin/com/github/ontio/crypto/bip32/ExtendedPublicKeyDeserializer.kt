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

import com.github.ontio.crypto.bip32.Checksum.confirmExtendedKeyChecksum
import io.github.novacrypto.base58.Base58.base58Decode
import io.github.novacrypto.bip32.Networks
import io.github.novacrypto.bip32.networks.DefaultNetworks
import java.util.*

internal class ExtendedPublicKeyDeserializer(private val networks: Networks) : Deserializer<ExtendedPublicKey> {

    override fun deserialize(extendedBase58Key: CharSequence): ExtendedPublicKey {
        val extendedKeyData = base58Decode(extendedBase58Key)
        try {
            return deserialize(extendedKeyData)
        } finally {
            Arrays.fill(extendedKeyData, 0.toByte())
        }
    }

    override fun deserialize(extendedKeyData: ByteArray): ExtendedPublicKey {
        confirmExtendedKeyChecksum(extendedKeyData)
        val reader = ByteArrayReader(extendedKeyData)
        return ExtendedPublicKey(HdKey.Builder()
                .network(networks.findByPublicVersion(reader.readSer32()))
                .depth(reader.read())
                .parentFingerprint(reader.readSer32())
                .childNumber(reader.readSer32())
                .chainCode(reader.readRange(32))
                .key(reader.readRange(33))
                .neutered(true)
                .build()
        )
    }

    companion object {

        val DEFAULT = ExtendedPublicKeyDeserializer(DefaultNetworks.INSTANCE)
    }
}