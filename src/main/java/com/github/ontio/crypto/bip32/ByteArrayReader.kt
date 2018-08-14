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

import java.util.*

internal class ByteArrayReader(private val bytes: ByteArray) {
    private var idx = 0

    fun readRange(length: Int): ByteArray {
        val range = Arrays.copyOfRange(this.bytes, idx, idx + length)
        idx += length
        return range
    }

    /**
     * deserialize a 32-bit unsigned integer i as a 4-byte sequence, most significant byte first.
     */
    fun readSer32(): Int {
        var result = read()
        result = result shl 8
        result = result or read()
        result = result shl 8
        result = result or read()
        result = result shl 8
        result = result or read()
        return result
    }

    fun read(): Int {
        return 0xff and bytes[idx++].toInt()
    }
}