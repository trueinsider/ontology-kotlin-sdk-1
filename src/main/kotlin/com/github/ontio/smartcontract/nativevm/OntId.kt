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

package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk.addSign
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.OntSdk.walletMgr
import com.github.ontio.account.Account
import com.github.ontio.common.*
import com.github.ontio.core.DataSignature
import com.github.ontio.core.ontid.Attribute
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.crypto.Curve
import com.github.ontio.crypto.KeyType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.merkle.MerkleVerifier
import com.github.ontio.sdk.claim.Claim
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.smartcontract.Vm.buildNativeParams
import com.github.ontio.smartcontract.Vm.makeInvokeCodeTransaction
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

object OntId {
    val contractAddress = "0000000000000000000000000000000000000003"

    /**
     * @param ident
     * @param password
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @param isPreExec
     * @return
     * @throws Exception
     */
    fun sendRegister(ident: Identity, password: String, payerAcct: Account, gaslimit: Long, gasprice: Long, isPreExec: Boolean = false): String {
        if (password.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val tx = makeRegister(ident.ontid, password, ident.controls[0].getSalt(), payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        walletMgr.writeWallet()
        signTx(tx, ident.ontid, password, ident.controls[0].getSalt())
        addSign(tx, payerAcct)
        if (isPreExec) {
            val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
            val result = (obj as JSONObject).getString("Result")
            if (Integer.parseInt(result) == 0) {
                throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: $obj"))
            }
        } else {
            val b = connect!!.sendRawTransaction(tx.toHexString())
            if (!b) {
                throw SDKException(ErrorCode.SendRawTxError)
            }
        }
        return tx.hash().toHexString()
    }

    fun sendRegisterPreExec(ident: Identity, password: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        return sendRegister(ident, password, payerAcct, gaslimit, gasprice, true)
    }


    /**
     * @param ontid
     * @param password
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeRegister(ontid: String, password: String, salt: ByteArray, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (password.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val info = walletMgr.getIdentityInfo(ontid, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = mutableListOf<Struct>()
        list.add(Struct().add(info.ontid, pk))
        val args = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "regIDWithPublicKey", args, payer, gaslimit, gasprice)
    }


    /**
     * @param ident
     * @param password
     * @param attributes
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */

    fun sendRegisterWithAttrs(ident: Identity, password: String, attributes: Array<Attribute>, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        if (password.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val info = walletMgr.getIdentityInfo(ident.ontid, password, ident.controls[0].getSalt())
        val ontid = info.ontid
        val tx = makeRegisterWithAttrs(ontid, password, ident.controls[0].getSalt(), attributes, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, ontid, password, ident.controls[0].getSalt())
        addSign(tx, payerAcct)
        walletMgr.wallet!!.addOntIdController(ontid, info.encryptedPrikey, info.ontid, info.pubkey)
        walletMgr.writeWallet()
        connect!!.sendRawTransaction(tx.toHexString())
        return tx.hash().toHexString()
    }

    /**
     * @param ontid
     * @param password
     * @param attributes
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeRegisterWithAttrs(ontid: String, password: String, salt: ByteArray, attributes: Array<Attribute>, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (password.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val info = walletMgr.getIdentityInfo(ontid, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = mutableListOf<Struct>()
        val struct = Struct().add(ontid.toByteArray(), pk)
        struct.add(attributes.size)
        for (i in attributes.indices) {
            struct.add(attributes[i].key, attributes[i].valueType, attributes[i].value)
        }
        list.add(struct)
        val args = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "regIDWithAttributes", args, payer, gaslimit, gasprice)
    }

    private fun serializeAttributes(attributes: Array<Attribute>): ByteArray {
        val baos = ByteArrayOutputStream()
        val bw = BinaryWriter(baos)
        bw.writeSerializableArray(attributes)
        return baos.toByteArray()
    }

    /**
     * @param ontid
     * @return
     * @throws Exception
     */
    fun sendGetPublicKeys(ontid: String): String {
        if (ontid.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("ontid should not be null"))
        }

        val list = mutableListOf<ByteArray>()
        list.add(ontid.toByteArray())
        val arg = NativeBuildParams.createCodeParamsScript(list)
        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getPublicKeys", arg, null, 0, 0)

        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        if (res.isEmpty()) {
            return res
        }
        val bais = ByteArrayInputStream(Helper.hexToBytes(res))
        val br = BinaryReader(bais)
        val pubKeyList = mutableListOf<Map<String, Any>>()
        while (true) {
            try {
                val publicKeyMap = mutableMapOf<String, Any>()
                publicKeyMap["PubKeyId"] = ontid + "#keys-" + br.readInt().toString()
                val pubKey = br.readVarBytes()
                publicKeyMap["Type"] = KeyType.fromLabel(pubKey[0])
                publicKeyMap["Curve"] = Curve.fromLabel(pubKey[1].toInt())
                publicKeyMap["Value"] = Helper.toHexString(pubKey)
                pubKeyList.add(publicKeyMap)
            } catch (e: Exception) {
                break
            }

        }
        return JSON.toJSONString(pubKeyList)
    }

    /**
     * @param ontid
     * @return
     * @throws Exception
     */
    fun sendGetKeyState(ontid: String, index: Int): String {
        if (ontid.isEmpty() || index < 0) {
            throw SDKException(ErrorCode.ParamErr("parameter is wrong"))
        }

        val list = mutableListOf<Struct>()
        list.add(Struct().add(ontid.toByteArray(), index))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getKeyState", arg, null, 0, 0)

        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        return if (res.isEmpty()) {
            res
        } else String(Helper.hexToBytes(res))
    }

    fun sendGetAttributes(ontid: String): String {
        if (ontid.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("ontid should not be null"))
        }

        val list = mutableListOf<ByteArray>()
        list.add(ontid.toByteArray())
        val arg = NativeBuildParams.createCodeParamsScript(list)
        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getAttributes", arg, null, 0, 0)

        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        if (res.isEmpty()) {
            return res
        }

        val bais = ByteArrayInputStream(Helper.hexToBytes(res))
        val br = BinaryReader(bais)
        val attrsList = mutableListOf<Map<String, String>>()
        while (true) {
            try {
                val attributeMap = mutableMapOf<String, String>()
                attributeMap["Key"] = String(br.readVarBytes())
                attributeMap["Type"] = String(br.readVarBytes())
                attributeMap["Value"] = String(br.readVarBytes())
                attrsList.add(attributeMap)
            } catch (e: Exception) {
                break
            }
        }

        return JSON.toJSONString(attrsList)
    }

    /**
     * @param ontid
     * @param password
     * @param newpubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendAddPubKey(ontid: String, password: String, salt: ByteArray, newpubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        return sendAddPubKey(ontid, null, password, salt, newpubkey, payerAcct, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryOntid
     * @param password
     * @param newpubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendAddPubKey(ontid: String, recoveryOntid: String?, password: String, salt: ByteArray, newpubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || password.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val tx = makeAddPubKey(ontid, recoveryOntid, password, salt, newpubkey, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        val addr = recoveryOntid?.replace(Common.didont, "") ?: ontid.replace(Common.didont, "")
        signTx(tx, addr, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param newpubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeAddPubKey(ontid: String, password: String, salt: ByteArray, newpubkey: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        return makeAddPubKey(ontid, null, password, salt, newpubkey, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryOntid
     * @param password
     * @param newpubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeAddPubKey(ontid: String, recoveryOntid: String?, password: String, salt: ByteArray, newpubkey: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || payer.isEmpty() || newpubkey.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val arg = if (recoveryOntid == null) {
            val info = walletMgr.getAccountInfo(ontid, password, salt)
            val pk = Helper.hexToBytes(info.pubkey)

            val list = mutableListOf<Struct>()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(newpubkey), pk))
            NativeBuildParams.createCodeParamsScript(list)
        } else {
            val list = mutableListOf<Struct>()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(newpubkey), Address.decodeBase58(recoveryOntid.replace(Common.didont, "")).toArray()))
            NativeBuildParams.createCodeParamsScript(list)
        }

        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "addKey", arg, payer, gaslimit, gasprice)
    }


    /**
     * @param ontid
     * @param password
     * @param removePubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendRemovePubKey(ontid: String, password: String, salt: ByteArray, removePubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        return sendRemovePubKey(ontid, null, password, salt, removePubkey, payerAcct, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryOntid
     * @param password
     * @param removePubkey
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendRemovePubKey(ontid: String, recoveryOntid: String?, password: String, salt: ByteArray, removePubkey: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || password.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val tx = makeRemovePubKey(ontid, recoveryOntid, password, salt, removePubkey, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        val addr = recoveryOntid?.replace(Common.didont, "") ?: ontid.replace(Common.didont, "")
        signTx(tx, addr, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param removePubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeRemovePubKey(ontid: String, password: String, salt: ByteArray, removePubkey: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        return makeRemovePubKey(ontid, null, password, salt, removePubkey, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param recoveryAddr
     * @param password
     * @param removePubkey
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeRemovePubKey(ontid: String, recoveryAddr: String?, password: String, salt: ByteArray, removePubkey: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || password.isEmpty() || payer.isEmpty() || removePubkey.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val arg = if (recoveryAddr == null) {
            val info = walletMgr.getAccountInfo(ontid.replace(Common.didont, ""), password, salt)
            val pk = Helper.hexToBytes(info.pubkey)
            val list = mutableListOf<Struct>()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(removePubkey), pk))
            NativeBuildParams.createCodeParamsScript(list)
        } else {
            val list = mutableListOf<Struct>()
            list.add(Struct().add(ontid.toByteArray(), Helper.hexToBytes(removePubkey), Address.decodeBase58(recoveryAddr.replace(Common.didont, "")).toArray()))
            NativeBuildParams.createCodeParamsScript(list)
        }

        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "removeKey", arg, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param password
     * @param recoveryAddr
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */

    fun sendAddRecovery(ontid: String, password: String, salt: ByteArray, recoveryAddr: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || password.isEmpty() || recoveryAddr.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val addr = ontid.replace(Common.didont, "")
        val tx = makeAddRecovery(ontid, password, salt, recoveryAddr, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, addr, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param recoveryAddr
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeAddRecovery(ontid: String, password: String, salt: ByteArray, recoveryAddr: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || password.isEmpty() || payer.isEmpty() || recoveryAddr.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val addr = ontid.replace(Common.didont, "")
        val info = walletMgr.getAccountInfo(addr, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = mutableListOf<Struct>()
        list.add(Struct().add(ontid, Address.decodeBase58(recoveryAddr), pk))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "addRecovery", arg, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param password
     * @param newRecovery
     * @param oldRecovery
     * @return
     * @throws Exception
     */
    fun sendChangeRecovery(ontid: String, newRecovery: String, oldRecovery: String, password: String, salt: ByteArray, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || password.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val tx = makeChangeRecovery(ontid, newRecovery, oldRecovery, password, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, oldRecovery, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param newRecoveryOntId
     * @param oldRecoveryOntId
     * @param password
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeChangeRecovery(ontid: String, newRecoveryOntId: String, oldRecoveryOntId: String, password: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || password.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val newAddr = Address.decodeBase58(newRecoveryOntId.replace(Common.didont, ""))
        val oldAddr = Address.decodeBase58(oldRecoveryOntId.replace(Common.didont, ""))

        val list = mutableListOf<Struct>()
        list.add(Struct().add(ontid, newAddr, oldAddr))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "changeRecovery", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param ontid
     * @param newRecoveryOntId
     * @param oldRecoveryOntId
     * @param accounts
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    private fun sendChangeRecovery(ontid: String, newRecoveryOntId: String, oldRecoveryOntId: String, accounts: Array<Account>, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || accounts.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val newAddr = Address.decodeBase58(newRecoveryOntId.replace(Common.didont, ""))
        val oldAddr = Address.decodeBase58(oldRecoveryOntId.replace(Common.didont, ""))
        val parabytes = NativeBuildParams.buildParams(ontid.toByteArray(), newAddr.toArray(), oldAddr.toArray())
        val tx = makeInvokeCodeTransaction(contractAddress, "changeRecovery", parabytes, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, arrayOf(accounts))
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param attributes
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendAddAttributes(ontid: String, password: String, salt: ByteArray, attributes: Array<Attribute>, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || attributes.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val addr = ontid.replace(Common.didont, "")
        val tx = makeAddAttributes(ontid, password, salt, attributes, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, addr, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param attributes
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeAddAttributes(ontid: String, password: String, salt: ByteArray, attributes: Array<Attribute>, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || attributes.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val addr = ontid.replace(Common.didont, "")
        val info = walletMgr.getAccountInfo(addr, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)
        val list = mutableListOf<Struct>()
        val struct = Struct().add(ontid.toByteArray())
        struct.add(attributes.size)
        for (i in attributes.indices) {
            struct.add(attributes[i].key, attributes[i].valueType, attributes[i].value)
        }
        struct.add(pk)
        list.add(struct)
        val args = NativeBuildParams.createCodeParamsScript(list)

        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "addAttributes", args, payer, gaslimit, gasprice)
    }

    /**
     * @param ontid
     * @param password
     * @param path
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendRemoveAttribute(ontid: String, password: String, salt: ByteArray, path: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || path.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val addr = ontid.replace(Common.didont, "")
        val tx = makeRemoveAttribute(ontid, password, salt, path, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, addr, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toString()
        } else null
    }

    /**
     * @param ontid
     * @param password
     * @param path
     * @param payer
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun makeRemoveAttribute(ontid: String, password: String, salt: ByteArray, path: String, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || payer.isEmpty() || path.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (gasprice < 0 || gaslimit < 0) {
            throw SDKException(ErrorCode.ParamErr("gas or gaslimit should not be less than 0"))
        }
        val addr = ontid.replace(Common.didont, "")
        val info = walletMgr.getAccountInfo(addr, password, salt)
        val pk = Helper.hexToBytes(info.pubkey)

        val list = mutableListOf<Struct>()
        list.add(Struct().add(ontid.toByteArray(), path.toByteArray(), pk))
        val arg = NativeBuildParams.createCodeParamsScript(list)
        return buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "removeAttribute", arg, payer, gaslimit, gasprice)
    }

    /**
     * @param txhash
     * @return
     * @throws Exception
     */
    fun getMerkleProof(txhash: String): Any {
        if (txhash.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("txhash should not be null"))
        }
        val proof = mutableMapOf<String, Map<String, Any>>()
        val map = mutableMapOf<String, Any>()
        val height = connect!!.getBlockHeightByTxHash(txhash)
        map["Type"] = "MerkleProof"
        map["TxnHash"] = txhash
        map["BlockHeight"] = height

        val tmpProof = connect!!.getMerkleProof(txhash)
        val blockHeight = tmpProof["BlockHeight"] as Int
        val curBlockRoot = UInt256.parse(tmpProof["CurBlockRoot"] as String)
        val curBlockHeight = tmpProof["CurBlockHeight"] as Int
        val hashes = tmpProof["TargetHashes"] as List<String>
        val targetHashes = Array(hashes.size) { i -> UInt256.parse(hashes[i]) }
        map["MerkleRoot"] = curBlockRoot.toHexString()
        map["Nodes"] = MerkleVerifier.getProof(blockHeight, targetHashes, curBlockHeight + 1)
        proof["Proof"] = map
        return proof
    }

    /**
     * @param merkleProof
     * @return
     * @throws Exception
     */
    fun verifyMerkleProof(merkleProof: String): Boolean {
        if (merkleProof.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("claim should not be null"))
        }
        try {
            val obj = JSON.parseObject(merkleProof)
            val proof = obj.getJSONObject("Proof") as Map<*, *>
            val txhash = proof["TxnHash"] as String
            val blockHeight = proof["BlockHeight"] as Int
            val merkleRoot = UInt256.parse(proof["MerkleRoot"] as String)
            val block = connect!!.getBlock(blockHeight)
            if (block.height != blockHeight) {
                throw SDKException("blockHeight not match")
            }
            var containTx = false
            for (i in block.transactions.indices) {
                if (block.transactions[i].hash().toHexString() == txhash) {
                    containTx = true
                }
            }
            if (!containTx) {
                throw SDKException(ErrorCode.OtherError("not contain this tx"))
            }
            val txsroot = block.transactionsRoot

            val nodes = proof["Nodes"] as List<*>
            return MerkleVerifier.Verify(txsroot, nodes, merkleRoot)
        } catch (e: Exception) {
            throw SDKException(e)
        }

    }

    /**
     * @param signerOntid
     * @param password
     * @param context
     * @param claimMap
     * @param metaData
     * @param clmRevMap
     * @param expire
     * @return
     * @throws Exception
     */
    fun createOntIdClaim(signerOntid: String, password: String, salt: ByteArray, context: String, claimMap: Map<String, Any>, metaData: Map<String, String>, clmRevMap: Map<String, Any>, expire: Long): String {
        if (signerOntid.isEmpty() || password.isEmpty() || context.isEmpty() || expire < 0) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (expire < System.currentTimeMillis() / 1000) {
            throw SDKException(ErrorCode.ExpireErr)
        }
        val claim: Claim?
        try {
            val sendDid = metaData["Issuer"]
            val receiverDid = metaData["Subject"]
            if (sendDid == null || receiverDid == null) {
                throw SDKException(ErrorCode.DidNull)
            }
            val issuerDdo = sendGetDDO(sendDid)
            val owners = JSON.parseObject(issuerDdo).getJSONArray("Owners")
                    ?: throw SDKException(ErrorCode.NotExistCliamIssuer)
            var pubkeyId: String? = null
            val acct = walletMgr.getAccount(signerOntid, password, salt)
            val pk = Helper.toHexString(acct.serializePublicKey())
            for (i in owners.indices) {
                val obj = owners.getJSONObject(i)
                if (obj.getString("Value") == pk) {
                    pubkeyId = obj.getString("PubKeyId")
                    break
                }
            }
            if (pubkeyId == null) {
                throw SDKException(ErrorCode.NotFoundPublicKeyId)
            }
            val receiverDidStr = receiverDid.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (receiverDidStr.size != 3) {
                throw SDKException(ErrorCode.DidError)
            }
            claim = Claim(walletMgr.signatureScheme, acct, context, claimMap, metaData, clmRevMap, pubkeyId, expire)
            return claim.claimStr
        } catch (e: SDKException) {
            throw SDKException(ErrorCode.CreateOntIdClaimErr)
        }
    }

    /**
     * @param claim
     * @return
     * @throws Exception
     */
    fun verifyOntIdClaim(claim: String): Boolean {
        val sign: DataSignature?
        try {

            val obj = claim.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (obj.size != 3) {
                throw SDKException(ErrorCode.ParamError)
            }
            val payloadBytes = Base64.getDecoder().decode(obj[1].toByteArray())
            val payloadObj = JSON.parseObject(String(payloadBytes))
            val issuerDid = payloadObj.getString("iss")
            val str = issuerDid.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (str.size != 3) {
                throw SDKException(ErrorCode.DidError)
            }
            val issuerDdo = sendGetDDO(issuerDid)
            val owners = JSON.parseObject(issuerDdo).getJSONArray("Owners")
                    ?: throw SDKException(ErrorCode.NotExistCliamIssuer)
            val signatureBytes = Base64.getDecoder().decode(obj[2])
            val headerBytes = Base64.getDecoder().decode(obj[0].toByteArray())
            val header = JSON.parseObject(String(headerBytes))
            val kid = header.getString("kid")
            val id = kid.split("#keys-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            val pubkeyStr = owners.getJSONObject(Integer.parseInt(id) - 1).getString("Value")
            sign = DataSignature()
            val data = (obj[0] + "." + obj[1]).toByteArray()
            return sign.verifySignature(Account(Helper.hexToBytes(pubkeyStr)), data, signatureBytes)
        } catch (e: Exception) {
            throw SDKException(ErrorCode.VerifyOntIdClaimErr)
        }

    }


    /**
     * @param ontid
     * @return
     * @throws Exception
     */
    fun sendGetDDO(ontid: String): String {
        val list = mutableListOf<ByteArray>()
        list.add(ontid.toByteArray())
        val arg = NativeBuildParams.createCodeParamsScript(list)

        val tx = buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "getDDO", arg, null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        val res = (obj as JSONObject).getString("Result")
        if (res.isEmpty()) {
            return res
        }
        val map = parseDdoData(ontid, res)
        return if (map.isEmpty()) {
            ""
        } else JSON.toJSONString(map)
    }

    private fun parseDdoData(ontid: String, obj: String): Map<String, Any> {
        val bys = Helper.hexToBytes(obj)

        val bais = ByteArrayInputStream(bys)
        val br = BinaryReader(bais)

        val publickeyBytes = try {
            br.readVarBytes()
        } catch (e: Exception) {
            byteArrayOf()
        }

        val attributeBytes = try {
            br.readVarBytes()
        } catch (e: Exception) {
            e.printStackTrace()
            byteArrayOf()
        }

        val recoveryBytes = try {
            br.readVarBytes()
        } catch (e: Exception) {
            byteArrayOf()
        }

        val pubKeyList = mutableListOf<Map<String, Any>>()
        if (publickeyBytes.isNotEmpty()) {
            val bais1 = ByteArrayInputStream(publickeyBytes)
            val br1 = BinaryReader(bais1)
            while (true) {
                try {
                    val publicKeyMap = mutableMapOf<String, Any>()
                    publicKeyMap["PubKeyId"] = ontid + "#keys-" + br1.readInt().toString()
                    val pubKey = br1.readVarBytes()
                    if (pubKey.size == 33) {
                        publicKeyMap["Type"] = KeyType.ECDSA.name
                        publicKeyMap["Curve"] = Curve.P256
                        publicKeyMap["Value"] = Helper.toHexString(pubKey)
                    } else {
                        publicKeyMap["Type"] = KeyType.fromLabel(pubKey[0])
                        publicKeyMap["Curve"] = Curve.fromLabel(pubKey[1].toInt())
                        publicKeyMap["Value"] = Helper.toHexString(pubKey)
                    }

                    pubKeyList.add(publicKeyMap)
                } catch (e: Exception) {
                    break
                }

            }
        }

        val attrsList = mutableListOf<Map<String, String>>()
        if (attributeBytes.isNotEmpty()) {
            val bais2 = ByteArrayInputStream(attributeBytes)
            val br2 = BinaryReader(bais2)
            while (true) {
                try {
                    val attributeMap = mutableMapOf<String, String>()
                    attributeMap["Key"] = String(br2.readVarBytes())
                    attributeMap["Type"] = String(br2.readVarBytes())
                    attributeMap["Value"] = String(br2.readVarBytes())
                    attrsList.add(attributeMap)
                } catch (e: Exception) {
                    break
                }

            }
        }

        val map = mutableMapOf<String, Any>()
        map["Owners"] = pubKeyList
        map["Attributes"] = attrsList
        if (recoveryBytes.isNotEmpty()) {
            map["Recovery"] = Address.parse(Helper.toHexString(recoveryBytes)).toBase58()
        }
        map["OntId"] = ontid
        return map
    }
}
