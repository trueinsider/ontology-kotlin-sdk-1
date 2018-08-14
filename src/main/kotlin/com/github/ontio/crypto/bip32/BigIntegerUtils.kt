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

import java.math.BigInteger
import java.util.*

internal object BigIntegerUtils {

    fun parse256(bytes: ByteArray): BigInteger {
        return BigInteger(1, bytes)
    }

    fun ser256(target: ByteArray, integer: BigInteger) {
        if (integer.bitLength() > target.size * 8)
            throw RuntimeException("ser256 failed, cannot fit integer in buffer")
        val modArr = integer.toByteArray()
        Arrays.fill(target, 0.toByte())
        copyTail(modArr, target)
        Arrays.fill(modArr, 0.toByte())
    }

    private fun copyTail(src: ByteArray, dest: ByteArray) {
        if (src.size < dest.size) {
            System.arraycopy(src, 0, dest, dest.size - src.size, src.size)
        } else {
            System.arraycopy(src, src.size - dest.size, dest, 0, dest.size)
        }
    }
}