package com.github.ontio.common


import com.alibaba.fastjson.JSON
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.wallet.*

import java.security.NoSuchAlgorithmException
import java.util.Base64
import java.util.HashMap

object WalletQR {
    @Throws(Exception::class)
    fun exportIdentityQRCode(walletFile: Wallet, identity: Identity): Map<*, *> {
        val control = identity.controls[0]
        val address = identity.ontid.substring(8)
        val map = HashMap()
        map.put("type", "I")
        map.put("label", identity.label)
        map.put("key", control.key)
        map.put("parameters", control.parameters)
        map.put("algorithm", "ECDSA")
        map.put("scrypt", walletFile.scrypt)
        map.put("address", address)
        map.put("salt", control.salt)
        return map
    }

    @Throws(Exception::class)
    fun exportIdentityQRCode(scrypt: Scrypt, identity: Identity): Map<*, *> {
        val control = identity.controls[0]
        val address = identity.ontid.substring(8)
        val map = HashMap()
        map.put("type", "I")
        map.put("label", identity.label)
        map.put("key", control.key)
        map.put("parameters", control.parameters)
        map.put("algorithm", "ECDSA")
        map.put("scrypt", scrypt)
        map.put("address", address)
        map.put("salt", control.salt)
        return map
    }

    @Throws(Exception::class)
    fun exportAccountQRCode(walletFile: Wallet, account: Account): Map<*, *> {
        val map = HashMap()
        map.put("type", "A")
        map.put("label", account.label)
        map.put("key", account.key)
        map.put("parameters", account.parameters)
        map.put("algorithm", "ECDSA")
        map.put("scrypt", walletFile.scrypt)
        map.put("address", account.address)
        map.put("salt", account.salt)
        return map
    }

    @Throws(Exception::class)
    fun exportAccountQRCode(scrypt: Scrypt, account: Account): Map<*, *> {
        val map = HashMap()
        map.put("type", "A")
        map.put("label", account.label)
        map.put("key", account.key)
        map.put("parameters", account.parameters)
        map.put("algorithm", "ECDSA")
        map.put("scrypt", scrypt)
        map.put("address", account.address)
        map.put("salt", account.salt)
        return map
    }

    fun getPriKeyFromQrCode(qrcode: String, password: String): String? {
        val map = JSON.parseObject(qrcode, Map<*, *>::class.java)
        val key = map["key"] as String
        val address = map["address"] as String
        val salt = map["salt"] as String
        val n = (map["scrypt"] as Map<*, *>)["n"] as Int
        try {
            return com.github.ontio.account.Account.getGcmDecodedPrivateKey(key, password, address, Base64.getDecoder().decode(salt), n, SignatureScheme.SHA256WITHECDSA)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}
