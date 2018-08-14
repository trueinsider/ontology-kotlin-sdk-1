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

import org.spongycastle.crypto.ec.CustomNamedCurves
import org.spongycastle.math.ec.ECPoint
import java.math.BigInteger

internal object Secp256r1SC {

    val CURVE = CustomNamedCurves.getByName("secp256r1")

    fun n(): BigInteger {
        return CURVE.n
    }

    fun pointSerP(point: ECPoint): ByteArray {
        return point.getEncoded(true)
    }

    fun pointSerP_gMultiply(p: BigInteger): ByteArray {
        return pointSerP(gMultiply(p))
    }

    fun gMultiplyAndAddPoint(p: BigInteger, toAdd: ByteArray): ECPoint {
        return gMultiply(p).add(decode(toAdd))
    }

    private fun decode(toAdd: ByteArray): ECPoint {
        return CURVE.curve.decodePoint(toAdd)
    }

    private fun gMultiply(p: BigInteger): ECPoint {
        return CURVE.g
                .multiply(p)
    }
}