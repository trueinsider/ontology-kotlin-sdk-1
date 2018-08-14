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

import io.github.novacrypto.toruntime.CheckedExceptionToRuntime
import io.github.novacrypto.toruntime.CheckedExceptionToRuntime.toRuntime
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object HmacSha512 {
    private val HMAC_SHA512 = "HmacSHA512"

    fun hmacSha512(byteKey: ByteArray, seed: ByteArray): ByteArray {
        return initialize(byteKey)
                .doFinal(seed)
    }

    private fun initialize(byteKey: ByteArray): Mac {
        val hmacSha512 = getInstance(HMAC_SHA512)
        val keySpec = SecretKeySpec(byteKey, HMAC_SHA512)
        toRuntime { hmacSha512.init(keySpec) }
        return hmacSha512
    }

    private fun getInstance(HMAC_SHA256: String): Mac {
        return toRuntime(CheckedExceptionToRuntime.Func { Mac.getInstance(HMAC_SHA256) })
    }
}