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

internal class ByteArrayWriter(private val bytes: ByteArray) {
    private var idx = 0

    fun concat(bytesSource: ByteArray, length: Int = bytesSource.size) {
        System.arraycopy(bytesSource, 0, bytes, idx, length)
        idx += length
    }

    /**
     * ser32(i): serialize a 32-bit unsigned integer i as a 4-byte sequence, most significant byte first.
     *
     * @param i a 32-bit unsigned integer
     */
    fun concatSer32(i: Int) {
        concat((i shr 24).toByte())
        concat((i shr 16).toByte())
        concat((i shr 8).toByte())
        concat(i.toByte())
    }

    fun concat(b: Byte) {
        bytes[idx++] = b
    }

    companion object {
        fun tail32(bytes64: ByteArray): ByteArray {
            val ir = ByteArray(bytes64.size - 32)
            System.arraycopy(bytes64, 32, ir, 0, ir.size)
            return ir
        }

        fun head32(bytes64: ByteArray): ByteArray {
            return Arrays.copyOf(bytes64, 32)
        }
    }
}