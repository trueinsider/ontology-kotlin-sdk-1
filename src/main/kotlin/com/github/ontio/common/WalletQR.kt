package com.github.ontio.common

import com.alibaba.fastjson.JSON
import com.github.ontio.account.Account.Companion.getGcmDecodedPrivateKey
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.sdk.wallet.Scrypt
import com.github.ontio.sdk.wallet.Wallet
import java.util.*

object WalletQR {
    fun exportIdentityQRCode(walletFile: Wallet, identity: Identity): Map<*, *> {
        val control = identity.controls[0]
        val address = identity.ontid.substring(8)
        val map = mutableMapOf<String, Any>()
        map["type"] = "I"
        map["label"] = identity.label
        map["key"] = control.key
        map["parameters"] = control.parameters
        map["algorithm"] = "ECDSA"
        map["scrypt"] = walletFile.scrypt
        map["address"] = address
        map["salt"] = control.salt
        return map
    }

    fun exportIdentityQRCode(scrypt: Scrypt, identity: Identity): Map<*, *> {
        val control = identity.controls[0]
        val address = identity.ontid.substring(8)
        val map = mutableMapOf<String, Any>()
        map["type"] = "I"
        map["label"] = identity.label
        map["key"] = control.key
        map["parameters"] = control.parameters
        map["algorithm"] = "ECDSA"
        map["scrypt"] = scrypt
        map["address"] = address
        map["salt"] = control.salt
        return map
    }

    fun exportAccountQRCode(walletFile: Wallet, account: Account): Map<*, *> {
        val map = mutableMapOf<String, Any>()
        map["type"] = "A"
        map["label"] = account.label
        map["key"] = account.key
        map["parameters"] = account.parameters
        map["algorithm"] = "ECDSA"
        map["scrypt"] = walletFile.scrypt
        map["address"] = account.address
        map["salt"] = account.salt
        return map
    }

    fun exportAccountQRCode(scrypt: Scrypt, account: Account): Map<*, *> {
        val map = mutableMapOf<String, Any>()
        map["type"] = "A"
        map["label"] = account.label
        map["key"] = account.key
        map["parameters"] = account.parameters
        map["algorithm"] = "ECDSA"
        map["scrypt"] = scrypt
        map["address"] = account.address
        map["salt"] = account.salt
        return map
    }

    fun getPriKeyFromQrCode(qrcode: String, password: String): String {
        val map = JSON.parseObject(qrcode, Map::class.java)
        val key = map["key"] as String
        val address = map["address"] as String
        val salt = map["salt"] as String
        val n = (map["scrypt"] as Map<*, *>)["n"] as Int
        return getGcmDecodedPrivateKey(key, password, address, Base64.getDecoder().decode(salt), n, SignatureScheme.SHA256WITHECDSA)
    }
}
