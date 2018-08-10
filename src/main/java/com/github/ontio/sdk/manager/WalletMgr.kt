/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.sdk.manager

import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.common.Address
import com.github.ontio.crypto.*
import com.github.ontio.sdk.exception.*
import com.github.ontio.sdk.info.AccountInfo
import com.github.ontio.sdk.info.IdentityInfo
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Control
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.sdk.wallet.Wallet
import com.github.ontio.common.Common
import com.github.ontio.core.DataSignature
import com.alibaba.fastjson.JSON

import java.io.*
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 */
class WalletMgr {
    /**
     *
     * @return wallet in memory
     */
    var wallet: Wallet? = null
        private set
    /**
     *
     * @return wallet file data
     */
    var walletFile: Wallet? = null
        private set
    var signatureScheme: SignatureScheme? = null
    private var filePath: String? = null


    val defaultIdentity: Identity?
        get() {
            for (e in wallet!!.identities) {
                if (e.isDefault) {
                    return e
                }
            }
            return null
        }
    val defaultAccount: Account?
        get() {
            for (e in wallet!!.accounts) {
                if (e.isDefault) {
                    return e
                }
            }
            return null
        }

    @Throws(Exception::class)
    constructor(wallet: Wallet, scheme: SignatureScheme) {
        this.signatureScheme = scheme
        this.wallet = wallet
        this.walletFile = wallet
    }

    @Throws(Exception::class)
    constructor(path: String, scheme: SignatureScheme) {
        this.signatureScheme = scheme
        this.filePath = path
        val file = File(filePath!!)
        if (!file.exists()) {
            wallet = Wallet()
            wallet!!.createTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date())
            walletFile = Wallet()
            file.createNewFile()
            writeWallet()
        }
        val inputStream = FileInputStream(filePath!!)
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        val text = String(bytes)
        wallet = JSON.parseObject(text, Wallet::class.java)
        walletFile = JSON.parseObject(text, Wallet::class.java)
        if (wallet!!.identities == null) {
            wallet!!.identities = ArrayList()
        }
        if (wallet!!.accounts == null) {
            wallet!!.accounts = ArrayList()
        }
        writeWallet()
    }

    @Throws(Exception::class)
    private constructor(path: String, label: String, password: String, scheme: SignatureScheme) {
        this.signatureScheme = scheme
        this.filePath = path
        val file = File(filePath!!)
        if (!file.exists()) {
            wallet = Wallet()
            wallet!!.createTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Date())
            walletFile = Wallet()
            file.createNewFile()
            createIdentity(label, password)
            writeWallet()
        }
        val inputStream = FileInputStream(filePath!!)
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        val text = String(bytes)
        wallet = JSON.parseObject(text, Wallet::class.java)
        walletFile = JSON.parseObject(text, Wallet::class.java)
        if (wallet!!.identities == null) {
            wallet!!.identities = ArrayList()
        }
        if (wallet!!.accounts == null) {
            wallet!!.accounts = ArrayList()
        }
        if (wallet!!.identities.size == 0) {
            createIdentity(label, password)
            writeWallet()
        }
    }

    @Throws(IOException::class)
    private fun writeFile(filePath: String?, sets: String) {
        val fw = FileWriter(filePath!!)
        val out = PrintWriter(fw)
        out.write(sets)
        out.println()
        fw.close()
        out.close()
    }

    /**
     * wallet in memory = wallet file data
     * @return
     */
    fun resetWallet(): Wallet? {
        wallet = walletFile!!.clone()
        return wallet
    }

    @Throws(Exception::class)
    fun saveWallet(): Wallet? {
        return writeWallet()
    }

    @Throws(Exception::class)
    fun writeWallet(): Wallet? {
        writeFile(filePath, JSON.toJSONString(wallet))
        walletFile = wallet!!.clone()
        return walletFile
    }

    @Throws(Exception::class)
    fun importIdentity(encryptedPrikey: String, password: String, salt: ByteArray, address: String): Identity? {
        return importIdentity("", encryptedPrikey, password, salt, address)
    }

    @Throws(Exception::class)
    fun importIdentity(label: String, encryptedPrikey: String, password: String, salt: ByteArray, address: String): Identity? {
        var prikey: String? = com.github.ontio.account.Account.getGcmDecodedPrivateKey(encryptedPrikey, password, address, salt, walletFile!!.scrypt.n, signatureScheme)
        val info = createIdentity(label, password, salt, Helper.hexToBytes(prikey))
        prikey = null
        return wallet!!.getIdentity(info.ontid)
    }


    @Throws(Exception::class)
    fun createIdentity(password: String): Identity? {
        return createIdentity("", password)
    }

    @Throws(Exception::class)
    fun createIdentity(label: String, password: String): Identity? {
        val info = createIdentity(label, password, ECC.generateKey())
        return wallet!!.getIdentity(info.ontid)
    }

    @Throws(Exception::class)
    fun createIdentityFromPriKey(label: String, password: String, prikey: String): Identity? {
        val info = createIdentity(label, password, Helper.hexToBytes(prikey))
        return wallet!!.getIdentity(info.ontid)
    }

    @Throws(Exception::class)
    fun createIdentityFromPriKey(password: String, prikey: String?): Identity? {
        var prikey = prikey
        val info = createIdentity("", password, Helper.hexToBytes(prikey))
        prikey = null
        return wallet!!.getIdentity(info.ontid)
    }

    @Throws(Exception::class)
    fun createIdentityInfo(password: String): IdentityInfo {
        return createIdentityInfo("", password)
    }

    @Throws(Exception::class)
    fun createIdentityInfo(label: String, password: String): IdentityInfo {
        return createIdentity(label, password, ECC.generateKey())
    }


    @Throws(Exception::class)
    fun getIdentityInfo(ontid: String, password: String, salt: ByteArray): IdentityInfo {
        val acct = getAccountByAddress(Address.decodeBase58(ontid.replace(Common.didont, "")), password, salt)
        val info = IdentityInfo()
        info.ontid = Common.didont + Address.addressFromPubKey(acct.serializePublicKey()).toBase58()
        info.pubkey = Helper.toHexString(acct.serializePublicKey()!!)
        info.setPrikey(Helper.toHexString(acct.serializePrivateKey()))
        info.setPriwif(acct.exportWif())
        info.encryptedPrikey = acct.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n)
        info.addressU160 = acct.addressU160!!.toString()
        return info
    }

    @Throws(Exception::class)
    private fun createIdentity(label: String, password: String, prikey: ByteArray): IdentityInfo {
        val salt = ECC.generateKey(16)
        return createIdentity(label, password, salt, prikey)
    }

    @Throws(Exception::class)
    private fun createIdentity(label: String, password: String, salt: ByteArray, prikey: ByteArray): IdentityInfo {
        val acct = createAccount(label, password, salt, prikey, false)
        val info = IdentityInfo()
        info.ontid = Common.didont + Address.addressFromPubKey(acct.serializePublicKey()).toBase58()
        info.pubkey = Helper.toHexString(acct.serializePublicKey()!!)
        info.setPrikey(Helper.toHexString(acct.serializePrivateKey()))
        info.setPriwif(acct.exportWif())
        info.encryptedPrikey = acct.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n)
        info.addressU160 = acct.addressU160!!.toHexString()
        return info
    }

    @Throws(Exception::class)
    fun importAccount(encryptedPrikey: String, password: String, address: String, salt: ByteArray): Account? {
        return importAccount("", encryptedPrikey, password, address, salt)
    }

    @Throws(Exception::class)
    fun importAccount(label: String, encryptedPrikey: String, password: String?, address: String, salt: ByteArray): Account? {
        var password = password
        var prikey: String? = com.github.ontio.account.Account.getGcmDecodedPrivateKey(encryptedPrikey, password, address, salt, walletFile!!.scrypt.n, signatureScheme)
        val info = createAccountInfo(label, password, salt, Helper.hexToBytes(prikey))
        prikey = null
        password = null
        return wallet!!.getAccount(info.addressBase58)
    }

    @Throws(Exception::class)
    fun createAccounts(count: Int, password: String) {
        for (i in 0 until count) {
            createAccount("", password)
        }
    }

    @Throws(Exception::class)
    fun createAccount(password: String): Account? {
        return createAccount("", password)
    }

    @Throws(Exception::class)
    fun createAccount(label: String, password: String): Account? {
        val info = createAccountInfo(label, password, ECC.generateKey())
        return wallet!!.getAccount(info.addressBase58)
    }

    @Throws(Exception::class)
    private fun createAccountInfo(label: String, password: String, prikey: ByteArray): AccountInfo {
        val salt = ECC.generateKey(16)
        return createAccountInfo(label, password, salt, prikey)
    }

    @Throws(Exception::class)
    private fun createAccountInfo(label: String, password: String?, salt: ByteArray, prikey: ByteArray): AccountInfo {
        val acct = createAccount(label, password, salt, prikey, true)
        SecureRandom().nextBytes(prikey)
        val info = AccountInfo()
        info.addressBase58 = Address.addressFromPubKey(acct.serializePublicKey()).toBase58()
        info.pubkey = Helper.toHexString(acct.serializePublicKey()!!)
        info.setPrikey(Helper.toHexString(acct.serializePrivateKey()))
        info.setPriwif(acct.exportWif())
        info.encryptedPrikey = acct.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n)
        info.addressU160 = acct.addressU160!!.toHexString()
        return info
    }

    @Throws(Exception::class)
    fun createAccountFromPriKey(password: String, prikey: String): Account? {
        val info = createAccountInfo("", password, Helper.hexToBytes(prikey))
        return wallet!!.getAccount(info.addressBase58)
    }

    @Throws(Exception::class)
    fun createAccountFromPriKey(label: String, password: String, prikey: String): Account? {
        val info = createAccountInfo(label, password, Helper.hexToBytes(prikey))
        return wallet!!.getAccount(info.addressBase58)
    }

    @Throws(Exception::class)
    fun createAccountInfo(password: String): AccountInfo {
        return createAccountInfo("", password)
    }

    @Throws(Exception::class)
    fun createAccountInfo(label: String, password: String): AccountInfo {
        return createAccountInfo(label, password, ECC.generateKey())
    }

    @Throws(Exception::class)
    fun createAccountInfoFromPriKey(password: String, prikey: String): AccountInfo {
        return createAccountInfo("", password, Helper.hexToBytes(prikey))
    }

    @Throws(Exception::class)
    fun createAccountInfoFromPriKey(label: String, password: String, prikey: String): AccountInfo {
        return createAccountInfo(label, password, Helper.hexToBytes(prikey))
    }

    @Throws(Exception::class)
    fun createIdentityInfoFromPriKey(label: String, password: String, prikey: String): IdentityInfo {
        return createIdentity(label, password, Helper.hexToBytes(prikey))
    }

    @Throws(Exception::class)
    fun privateKeyToWif(privateKey: String): String {
        val act = com.github.ontio.account.Account(Helper.hexToBytes(privateKey), signatureScheme)
        return act.exportWif()
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun getAccount(address: String, password: String, salt: ByteArray = wallet!!.getAccount(address)!!.getSalt()): com.github.ontio.account.Account {
        var address = address
        address = address.replace(Common.didont, "")
        return getAccountByAddress(Address.decodeBase58(address), password, salt)
    }

    @Throws(Exception::class)
    fun getAccountInfo(address: String, password: String, salt: ByteArray): AccountInfo {
        var address = address
        address = address.replace(Common.didont, "")
        val info = AccountInfo()
        val acc = getAccountByAddress(Address.decodeBase58(address), password, salt)
        info.addressBase58 = address
        info.pubkey = Helper.toHexString(acc.serializePublicKey()!!)
        info.setPrikey(Helper.toHexString(acc.serializePrivateKey()))
        info.encryptedPrikey = acc.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n)
        info.setPriwif(acc.exportWif())
        info.addressU160 = acc.addressU160!!.toString()
        return info
    }


    @Throws(Exception::class)
    private fun createAccount(label: String?, password: String?, salt: ByteArray, privateKey: ByteArray, accountFlag: Boolean): com.github.ontio.account.Account {
        var label = label
        var password = password
        val account = com.github.ontio.account.Account(privateKey, signatureScheme)
        val acct: Account
        when (signatureScheme) {
            SignatureScheme.SHA256WITHECDSA -> acct = Account("ECDSA", arrayOf<Any>(Curve.P256.toString()), "aes-256-gcm", "SHA256withECDSA", "sha256")
            SignatureScheme.SM3WITHSM2 -> acct = Account("SM2", arrayOf<Any>(Curve.SM2P256V1.toString()), "aes-256-gcm", "SM3withSM2", "sha256")
            else -> throw SDKException(ErrorCode.OtherError("scheme type error"))
        }
        if (password != null) {
            acct.key = account.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n)
            password = null
        } else {
            acct.key = Helper.toHexString(account.serializePrivateKey())
        }
        acct.address = Address.addressFromPubKey(account.serializePublicKey()).toBase58()
        if (label == null || label == "") {
            val uuidStr = UUID.randomUUID().toString()
            label = uuidStr.substring(0, 8)
        }
        if (accountFlag) {
            for (e in wallet!!.accounts) {
                if (e.address == acct.address) {
                    throw SDKException(ErrorCode.ParamErr("wallet account exist"))
                }
            }
            if (wallet!!.accounts.size == 0) {
                acct.isDefault = true
                wallet!!.defaultAccountAddress = acct.address
            }
            acct.label = label
            acct.setSalt(salt)
            acct.publicKey = Helper.toHexString(account.serializePublicKey()!!)
            wallet!!.accounts.add(acct)
        } else {
            for (e in wallet!!.identities) {
                if (e.ontid == Common.didont + acct.address) {
                    throw SDKException(ErrorCode.ParamErr("wallet Identity exist"))
                }
            }
            val idt = Identity()
            idt.ontid = Common.didont + acct.address
            idt.label = label
            if (wallet!!.identities.size == 0) {
                idt.isDefault = true
                wallet!!.defaultOntid = idt.ontid
            }
            idt.controls = ArrayList()
            val ctl = Control(acct.key, "keys-1", Helper.toHexString(account.serializePublicKey()!!))
            ctl.setSalt(salt)
            ctl.address = acct.address
            idt.controls.add(ctl)
            wallet!!.identities.add(idt)
        }
        return account
    }

    @Throws(Exception::class)
    private fun getAccountByAddress(address: Address, password: String, salt: ByteArray): com.github.ontio.account.Account {
        try {
            for (e in wallet!!.accounts) {
                if (e.address == address.toBase58()) {
                    val prikey = com.github.ontio.account.Account.getGcmDecodedPrivateKey(e.key, password, e.address, salt, walletFile!!.scrypt.n, signatureScheme)
                    return com.github.ontio.account.Account(Helper.hexToBytes(prikey), signatureScheme)
                }
            }

            for (e in wallet!!.identities) {
                if (e.ontid == Common.didont + address.toBase58()) {
                    val addr = e.ontid.replace(Common.didont, "")
                    val prikey = com.github.ontio.account.Account.getGcmDecodedPrivateKey(e.controls[0].key, password, addr, salt, walletFile!!.scrypt.n, signatureScheme)
                    return com.github.ontio.account.Account(Helper.hexToBytes(prikey), signatureScheme)
                }
            }
        } catch (e: Exception) {
            throw SDKException(ErrorCode.GetAccountByAddressErr)
        }

        throw SDKException(ErrorCode.OtherError("Account null"))
    }
}
