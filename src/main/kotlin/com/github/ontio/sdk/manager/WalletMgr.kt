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

import com.alibaba.fastjson.JSON
import com.github.ontio.common.Address
import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.crypto.Curve
import com.github.ontio.crypto.ECC
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.sdk.info.AccountInfo
import com.github.ontio.sdk.info.IdentityInfo
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Control
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.sdk.wallet.Wallet
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.PrintWriter
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
    var signatureScheme: SignatureScheme
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

    constructor(wallet: Wallet, scheme: SignatureScheme) {
        this.signatureScheme = scheme
        this.wallet = wallet
        this.walletFile = wallet
    }

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
        writeWallet()
    }

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
        if (wallet!!.identities.isEmpty()) {
            createIdentity(label, password)
            writeWallet()
        }
    }

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

    fun saveWallet(): Wallet? {
        return writeWallet()
    }

    fun writeWallet(): Wallet? {
        writeFile(filePath, JSON.toJSONString(wallet))
        walletFile = wallet!!.clone()
        return walletFile
    }

    fun importIdentity(encryptedPrikey: String, password: String, salt: ByteArray, address: String): Identity? {
        return importIdentity("", encryptedPrikey, password, salt, address)
    }

    fun importIdentity(label: String, encryptedPrikey: String, password: String, salt: ByteArray, address: String): Identity? {
        val prikey: String? = com.github.ontio.account.Account.getGcmDecodedPrivateKey(encryptedPrikey, password, address, salt, walletFile!!.scrypt.n, signatureScheme)
        val info = createIdentity(label, password, salt, Helper.hexToBytes(prikey))
        return wallet!!.getIdentity(info.ontid)
    }


    fun createIdentity(password: String): Identity {
        return createIdentity("", password)
    }

    fun createIdentity(label: String, password: String): Identity {
        val info = createIdentity(label, password, ECC.generateKey())
        return wallet!!.getIdentity(info.ontid)!!
    }

    fun createIdentityFromPriKey(label: String, password: String, prikey: String): Identity? {
        val info = createIdentity(label, password, Helper.hexToBytes(prikey))
        return wallet!!.getIdentity(info.ontid)
    }

    fun createIdentityFromPriKey(password: String, prikey: String?): Identity {
        val info = createIdentity("", password, Helper.hexToBytes(prikey))
        return wallet!!.getIdentity(info.ontid)!!
    }

    fun createIdentityInfo(password: String): IdentityInfo {
        return createIdentityInfo("", password)
    }

    fun createIdentityInfo(label: String, password: String): IdentityInfo {
        return createIdentity(label, password, ECC.generateKey())
    }


    fun getIdentityInfo(ontid: String, password: String, salt: ByteArray): IdentityInfo {
        val acct = getAccountByAddress(Address.decodeBase58(ontid.replace(Common.didont, "")), password, salt)
        return IdentityInfo(
            Common.didont + Address.addressFromPubKey(acct.serializePublicKey()).toBase58(),
            Helper.toHexString(acct.serializePublicKey()),
            acct.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n),
            acct.addressU160.toString()
        )
    }

    private fun createIdentity(label: String, password: String, prikey: ByteArray): IdentityInfo {
        val salt = ECC.generateKey(16)
        return createIdentity(label, password, salt, prikey)
    }

    private fun createIdentity(label: String, password: String, salt: ByteArray, prikey: ByteArray): IdentityInfo {
        val acct = createAccount(label, password, salt, prikey, false)
        return IdentityInfo(
            Common.didont + Address.addressFromPubKey(acct.serializePublicKey()).toBase58(),
            Helper.toHexString(acct.serializePublicKey()),
            acct.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n),
            acct.addressU160.toHexString()
        )
    }

    fun importAccount(encryptedPrikey: String, password: String, address: String, salt: ByteArray): Account? {
        return importAccount("", encryptedPrikey, password, address, salt)
    }

    fun importAccount(label: String, encryptedPrikey: String, password: String, address: String, salt: ByteArray): Account? {
        val prikey = com.github.ontio.account.Account.getGcmDecodedPrivateKey(encryptedPrikey, password, address, salt, walletFile!!.scrypt.n, signatureScheme)
        val info = createAccountInfo(label, password, salt, Helper.hexToBytes(prikey))
        return wallet!!.getAccount(info.addressBase58)
    }

    fun createAccounts(count: Int, password: String) {
        for (i in 0 until count) {
            createAccount("", password)
        }
    }

    fun createAccount(password: String): Account {
        return createAccount("", password)
    }

    fun createAccount(label: String, password: String): Account {
        val info = createAccountInfo(label, password, ECC.generateKey())
        return wallet!!.getAccount(info.addressBase58)!!
    }

    private fun createAccountInfo(label: String, password: String, prikey: ByteArray): AccountInfo {
        val salt = ECC.generateKey(16)
        return createAccountInfo(label, password, salt, prikey)
    }

    private fun createAccountInfo(label: String, password: String, salt: ByteArray, prikey: ByteArray): AccountInfo {
        val acct = createAccount(label, password, salt, prikey, true)
        SecureRandom().nextBytes(prikey)
        return AccountInfo(
            Address.addressFromPubKey(acct.serializePublicKey()).toBase58(),
            Helper.toHexString(acct.serializePublicKey()),
            acct.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n),
            acct.addressU160.toHexString()
        )
    }

    fun createAccountFromPriKey(password: String, prikey: String): Account {
        val info = createAccountInfo("", password, Helper.hexToBytes(prikey))
        return wallet!!.getAccount(info.addressBase58)!!
    }

    fun createAccountFromPriKey(label: String, password: String, prikey: String): Account? {
        val info = createAccountInfo(label, password, Helper.hexToBytes(prikey))
        return wallet!!.getAccount(info.addressBase58)
    }

    fun createAccountInfo(password: String): AccountInfo {
        return createAccountInfo("", password)
    }

    fun createAccountInfo(label: String, password: String): AccountInfo {
        return createAccountInfo(label, password, ECC.generateKey())
    }

    fun createAccountInfoFromPriKey(password: String, prikey: String): AccountInfo {
        return createAccountInfo("", password, Helper.hexToBytes(prikey))
    }

    fun createAccountInfoFromPriKey(label: String, password: String, prikey: String): AccountInfo {
        return createAccountInfo(label, password, Helper.hexToBytes(prikey))
    }

    fun createIdentityInfoFromPriKey(label: String, password: String, prikey: String): IdentityInfo {
        return createIdentity(label, password, Helper.hexToBytes(prikey))
    }

    fun privateKeyToWif(privateKey: String): String {
        val act = com.github.ontio.account.Account(Helper.hexToBytes(privateKey), signatureScheme)
        return act.exportWif()
    }

    fun getAccount(address: String, password: String, salt: ByteArray = wallet!!.getAccount(address)!!.getSalt()): com.github.ontio.account.Account {
        val address = address.replace(Common.didont, "")
        return getAccountByAddress(Address.decodeBase58(address), password, salt)
    }

    fun getAccountInfo(address: String, password: String, salt: ByteArray): AccountInfo {
        val address = address.replace(Common.didont, "")
        val acc = getAccountByAddress(Address.decodeBase58(address), password, salt)
        return AccountInfo(
            address,
            Helper.toHexString(acc.serializePublicKey()),
            acc.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n),
            acc.addressU160.toString()
        )
    }


    private fun createAccount(label: String?, password: String?, salt: ByteArray, privateKey: ByteArray, accountFlag: Boolean): com.github.ontio.account.Account {
        val account = com.github.ontio.account.Account(privateKey, signatureScheme)
        val acct: Account = when (signatureScheme) {
            SignatureScheme.SHA256WITHECDSA -> Account("ECDSA", arrayOf(Curve.P256.toString()), "aes-256-gcm", "SHA256withECDSA", "sha256")
            SignatureScheme.SM3WITHSM2 -> Account("SM2", arrayOf(Curve.SM2P256V1.toString()), "aes-256-gcm", "SM3withSM2", "sha256")
            else -> throw SDKException(ErrorCode.OtherError("scheme type error"))
        }
        if (password != null) {
            acct.key = account.exportGcmEncryptedPrikey(password, salt, walletFile!!.scrypt.n)
        } else {
            acct.key = Helper.toHexString(account.serializePrivateKey())
        }
        acct.address = Address.addressFromPubKey(account.serializePublicKey()).toBase58()
        var label = label
        if (label == null || label.isEmpty()) {
            val uuidStr = UUID.randomUUID().toString()
            label = uuidStr.substring(0, 8)
        }
        if (accountFlag) {
            for (e in wallet!!.accounts) {
                if (e.address == acct.address) {
                    throw SDKException(ErrorCode.ParamErr("wallet account exist"))
                }
            }
            if (wallet!!.accounts.isEmpty()) {
                acct.isDefault = true
                wallet!!.defaultAccountAddress = acct.address
            }
            acct.label = label
            acct.setSalt(salt)
            acct.publicKey = Helper.toHexString(account.serializePublicKey())
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
            if (wallet!!.identities.isEmpty()) {
                idt.isDefault = true
                wallet!!.defaultOntid = idt.ontid
            }
            idt.controls = ArrayList()
            val ctl = Control(acct.key, "keys-1", Helper.toHexString(account.serializePublicKey()))
            ctl.setSalt(salt)
            ctl.address = acct.address
            idt.controls.add(ctl)
            wallet!!.identities.add(idt)
        }
        return account
    }

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
