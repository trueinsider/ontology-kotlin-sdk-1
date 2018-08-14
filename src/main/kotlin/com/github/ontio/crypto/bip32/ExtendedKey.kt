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

import io.github.novacrypto.bip32.Network

interface ExtendedKey {

    /**
     * The network of this extended key
     *
     * @return The network of this extended key
     */
    fun network(): Network

    /**
     * 1 byte: 0 for master nodes, 1 for level-1 derived keys, etc.
     *
     * @return the depth of this key node
     */
    fun depth(): Int

    /**
     * 4 bytes: child number. e.g. 3 for m/3, hard(7) for m/7'
     * 0 if master key
     *
     * @return the child number
     */
    fun childNumber(): Int

    /**
     * Serialized Base58 String of this extended key
     *
     * @return the Base58 String representing this key
     */
    fun extendedBase58(): String

    /**
     * Serialized data of this extended key
     *
     * @return the byte array representing this key
     */
    fun extendedKeyByteArray(): ByteArray

    /**
     * Coerce this key on to another network.
     *
     * @param otherNetwork Network to put key on.
     * @return A new extended key, or this instance if key already on the other Network.
     */
    fun toNetwork(otherNetwork: Network): ExtendedKey
}