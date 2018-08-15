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

package com.github.ontio

import com.github.ontio.account.Account
import com.github.ontio.common.Common
import com.github.ontio.common.ErrorCode
import com.github.ontio.core.DataSignature
import com.github.ontio.core.asset.Sig
import com.github.ontio.core.program.Program
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.crypto.Digest
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.sdk.manager.ConnectMgr
import com.github.ontio.sdk.manager.SignServer
import com.github.ontio.sdk.manager.WalletMgr
import java.util.*

/**
 * Ont Sdk
 */
object OntSdk {
    /**
     * get Wallet Mgr
     * @return
     */
    lateinit var walletMgr: WalletMgr
        private set
    private lateinit var connRpc: ConnectMgr
    private lateinit var connRestful: ConnectMgr
    private lateinit var connWebSocket: ConnectMgr
    private lateinit var connDefault: ConnectMgr

    private lateinit var signServer: SignServer
    var defaultSignScheme = SignatureScheme.SHA256WITHECDSA
    const val DEFAULT_GAS_LIMIT = 20000L
    const val DEFAULT_DEPLOY_GAS_LIMIT = 20200000L

    val rpc: ConnectMgr
        get() {
            if (!::connRpc.isInitialized) {
                throw SDKException(ErrorCode.ConnRestfulNotInit)
            }
            return connRpc
        }

    val restful: ConnectMgr
        get() {
            if (!::connRestful.isInitialized) {
                throw SDKException(ErrorCode.ConnRestfulNotInit)
            }
            return connRestful
        }

    val connect: ConnectMgr?
        get() {
            if (::connDefault.isInitialized) {
                return connDefault
            }
            if (::connRpc.isInitialized) {
                return connRpc
            }
            if (::connRestful.isInitialized) {
                return connRestful
            }
            if (::connWebSocket.isInitialized) {
                return connWebSocket
            }

            return null
        }

    val webSocket: ConnectMgr
        get() {
            if (!::connWebSocket.isInitialized) {
                throw SDKException(ErrorCode.WebsocketNotInit)
            }
            return connWebSocket
        }

    fun getSignServer(): SignServer {
        if (!::signServer.isInitialized) {
            throw SDKException(ErrorCode.OtherError("signServer null"))
        }
        return signServer
    }

    fun setDefaultConnect(conn: ConnectMgr) {
        connDefault = conn
    }

    fun setConnectTestNet() {
        val rpcUrl = "http://polaris1.ont.io:20336"
        setRpc(rpcUrl)
        connDefault = rpc
    }

    fun setConnectMainNet() {
        val rpcUrl = "http://dappnode1.ont.io:20336"
        setRpc(rpcUrl)
        connDefault = rpc
    }

    /**
     *
     * @param scheme
     */
    fun setSignatureScheme(scheme: SignatureScheme) {
        defaultSignScheme = scheme
        walletMgr.signatureScheme = scheme
    }

    fun setSignServer(url: String) {
        this.signServer = SignServer(url)
    }

    fun setRpc(url: String) {
        this.connRpc = ConnectMgr(url, "rpc")
    }

    fun setRestful(url: String) {
        this.connRestful = ConnectMgr(url, "restful")
    }

    fun setWebsocket(url: String, lock: Object) {
        connWebSocket = ConnectMgr(url, "websocket", lock)
    }

    /**
     *
     * @param path
     */
    fun openWalletFile(path: String) {
        this.walletMgr = WalletMgr(path, defaultSignScheme)
        setSignatureScheme(defaultSignScheme)
    }

    /**
     *
     * @param tx
     * @param addr
     * @param password
     * @return
     * @throws Exception
     */
    fun addSign(tx: Transaction, addr: String, password: String, salt: ByteArray): Transaction {
        return addSign(tx, walletMgr.getAccount(addr, password, salt))
    }

    fun addSign(tx: Transaction, acct: Account): Transaction {
        if (tx.sigs.size >= Common.TX_MAX_SIG_SIZE) {
            throw SDKException(ErrorCode.ParamErr("the number of transaction signatures should not be over 16"))
        }
        val sigs = mutableListOf(*tx.sigs)
        sigs.add(Sig(1, arrayOf(acct.serializePublicKey()), arrayOf(tx.sign(acct, acct.signatureScheme))))
        tx.sigs = sigs.toTypedArray()
        return tx
    }

    /**
     *
     * @param tx
     * @param M
     * @param pubKeys
     * @param acct
     * @return
     * @throws Exception
     */
    fun addMultiSign(tx: Transaction, M: Int, pubKeys: Array<ByteArray>, acct: Account): Transaction {
        addMultiSign(tx, M, pubKeys, tx.sign(acct, acct.signatureScheme))
        return tx
    }

    fun addMultiSign(tx: Transaction, M: Int, pubKeys: Array<ByteArray>, signatureData: ByteArray): Transaction {
        if (tx.sigs.size > Common.TX_MAX_SIG_SIZE || M > pubKeys.size || M <= 0) {
            throw SDKException(ErrorCode.ParamError)
        }
        val pubKeys = Program.sortPublicKeys(*pubKeys)
        for (i in tx.sigs.indices) {
            if (Arrays.deepEquals(tx.sigs[i].pubKeys, pubKeys)) {
                if (tx.sigs[i].sigData.size + 1 > pubKeys.size) {
                    throw SDKException(ErrorCode.ParamErr("too more sigData"))
                }
                if (tx.sigs[i].M != M) {
                    throw SDKException(ErrorCode.ParamErr("M error"))
                }
                val sigData = mutableListOf(*tx.sigs[i].sigData)
                sigData.add(signatureData)
                tx.sigs[i].sigData = sigData.toTypedArray()
                return tx
            }
        }
        val sigs = mutableListOf(*tx.sigs)
        sigs.add(Sig(M, pubKeys, arrayOf(signatureData)))
        tx.sigs = sigs.toTypedArray()
        return tx
    }

    fun signTx(tx: Transaction, address: String, password: String, salt: ByteArray) = signTx(
            tx,
            arrayOf(arrayOf(walletMgr.getAccount(address.replace(Common.didont, ""), password, salt)))
    )

    /**
     * sign tx
     * @param tx
     * @param accounts
     * @return
     */
    fun signTx(tx: Transaction, accounts: Array<Array<Account>>): Transaction {
        if (accounts.size > Common.TX_MAX_SIG_SIZE) {
            throw SDKException(ErrorCode.ParamErr("the number of transaction signatures should not be over 16"))
        }
        val sigs = mutableListOf<Sig>()
        for (i in accounts.indices) {
            val pubKeys = mutableListOf<ByteArray>()
            val sigData = mutableListOf<ByteArray>()
            for (j in 0 until accounts[i].size) {
                val signature = tx.sign(accounts[i][j], accounts[i][j].signatureScheme)
                pubKeys.add(accounts[i][j].serializePublicKey())
                sigData.add(signature)
            }
            sigs.add(Sig(pubKeys.size, pubKeys.toTypedArray(), sigData.toTypedArray()))
        }
        tx.sigs = sigs.toTypedArray()
        return tx
    }

    /**
     * signTx
     * @param tx
     * @param accounts
     * @param M
     * @return
     * @throws SDKException
     */
    fun signTx(tx: Transaction, accounts: Array<Array<Account>>, M: IntArray): Transaction {
        if (accounts.size > Common.TX_MAX_SIG_SIZE) {
            throw SDKException(ErrorCode.ParamErr("the number of transaction signatures should not be over 16"))
        }
        if (M.size != accounts.size) {
            throw SDKException(ErrorCode.ParamError)
        }
        signTx(tx, accounts)
        for (i in tx.sigs.indices) {
            if (M[i] > tx.sigs[i].pubKeys.size || M[i] < 0) {
                throw SDKException(ErrorCode.ParamError)
            }
            tx.sigs[i].M = M[i]
        }
        return tx
    }

    fun signatureData(acct: com.github.ontio.account.Account, data: ByteArray): ByteArray {
        val sign: DataSignature
        try {
            sign = DataSignature(defaultSignScheme, acct, Digest.sha256(Digest.sha256(data)))
            return sign.signature()
        } catch (e: Exception) {
            throw SDKException(e)
        }
    }

    fun verifySignature(pubkey: ByteArray, data: ByteArray, signature: ByteArray): Boolean {
        val sign: DataSignature
        try {
            sign = DataSignature()
            return sign.verifySignature(Account(pubkey), Digest.sha256(Digest.sha256(data)), signature)
        } catch (e: Exception) {
            throw SDKException(e)
        }
    }
}
