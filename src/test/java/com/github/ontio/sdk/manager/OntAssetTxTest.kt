package com.github.ontio.sdk.manager

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.util.IOUtils
import com.github.ontio.OntSdk
import com.github.ontio.OntSdkTest
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.sdk.wallet.Account
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.FileInputStream
import java.io.InputStream

import org.junit.Assert.*

class OntAssetTxTest {

    internal var ontSdk: OntSdk
    internal var info1: Account? = null
    internal var info2: Account? = null
    internal var info3: Account? = null
    internal var password = "111111"
    internal var wallet = "OntAssetTxTest.json"

    internal var payer: Account? = null
    @Before
    @Throws(Exception::class)
    fun setUp() {
        ontSdk = OntSdk.getInstance()
        val restUrl = OntSdkTest.URL
        ontSdk.setRestful(restUrl)
        ontSdk.setDefaultConnect(ontSdk.restful)
        ontSdk.openWalletFile(wallet)
        info1 = ontSdk.walletMgr!!.createAccountFromPriKey(OntSdkTest.PASSWORD, OntSdkTest.PRIVATEKEY)
        info2 = ontSdk.walletMgr!!.createAccount(password)
        info3 = ontSdk.walletMgr!!.createAccount(password)

        payer = ontSdk.walletMgr!!.createAccount(password)
    }

    @After
    fun removeWallet() {
        val file = File(wallet)
        if (file.exists()) {
            if (file.delete()) {
                println("delete wallet file success")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun sendTransfer() {
        val sendAcct = ontSdk.walletMgr!!.getAccount(info1!!.address, password, info1!!.getSalt())
        val payerAcct = ontSdk.walletMgr!!.getAccount(payer!!.address, password, payer!!.getSalt())
        val res = ontSdk.nativevm().ont().sendTransfer(sendAcct, info2!!.address, 100L, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)


        Assert.assertNotNull(res)
    }

    @Test
    @Throws(Exception::class)
    fun makeTransfer() {

        val tx = ontSdk.nativevm().ont().makeTransfer(info1!!.address, info2!!.address, 100L, payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Assert.assertNotNull(tx)
    }

    @Test
    @Throws(Exception::class)
    fun sendApprove() {
        val sendAcct1 = ontSdk.walletMgr!!.getAccount(info1!!.address, password, info1!!.getSalt())
        val sendAcct2 = ontSdk.walletMgr!!.getAccount(info2!!.address, password, info2!!.getSalt())
        val payerAcct = ontSdk.walletMgr!!.getAccount(payer!!.address, password, payer!!.getSalt())
        ontSdk.nativevm().ont().sendApprove(sendAcct1, sendAcct2.addressU160!!.toBase58(), 10L, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)
        val info1balance = ontSdk.nativevm().ont().queryBalanceOf(sendAcct1.addressU160!!.toBase58())
        val info2balance = ontSdk.nativevm().ont().queryBalanceOf(sendAcct2.addressU160!!.toBase58())
        Thread.sleep(6000)

        val allo = ontSdk.nativevm().ont().queryAllowance(sendAcct1.addressU160!!.toBase58(), sendAcct2.addressU160!!.toBase58())
        Assert.assertTrue(allo == 10L)
        ontSdk.nativevm().ont().sendTransferFrom(sendAcct2, info1!!.address, sendAcct2.addressU160!!.toBase58(), 10L, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val info1balance2 = ontSdk.nativevm().ont().queryBalanceOf(info1!!.address)
        val info2balance2 = ontSdk.nativevm().ont().queryBalanceOf(info2!!.address)

        Assert.assertTrue(info1balance - info1balance2 == 10L)
        Assert.assertTrue(info2balance2 - info2balance == 10L)


    }

    @Test
    @Throws(Exception::class)
    fun sendOngTransferFrom() {
        val unboundOngStr = ontSdk.nativevm().ong().unboundOng(info1!!.address)
        val unboundOng = java.lang.Long.parseLong(unboundOngStr)
        val res = ontSdk.nativevm().ong().withdrawOng(ontSdk.walletMgr!!.getAccount(info1!!.address, password, info1!!.getSalt()), info2!!.address, unboundOng / 100, ontSdk.walletMgr!!.getAccount(payer!!.address, password, payer!!.getSalt()), ontSdk.DEFAULT_GAS_LIMIT, 0)
        Assert.assertNotNull(res)
    }

    @Test
    @Throws(Exception::class)
    fun queryTest() {

        val decimal = ontSdk.nativevm().ont().queryDecimals()
        val decimal2 = ontSdk.nativevm().ong().queryDecimals()
        Assert.assertNotNull(decimal)
        Assert.assertNotNull(decimal2)

        val ontname = ontSdk.nativevm().ont().queryName()
        val ongname = ontSdk.nativevm().ong().queryName()
        Assert.assertNotNull(ontname)
        Assert.assertNotNull(ongname)

        val ontsym = ontSdk.nativevm().ont().querySymbol()
        val ongsym = ontSdk.nativevm().ong().querySymbol()
        Assert.assertNotNull(ontsym)
        Assert.assertNotNull(ongsym)

        val onttotal = ontSdk.nativevm().ont().queryTotalSupply()
        val ongtotal = ontSdk.nativevm().ong().queryTotalSupply()
        Assert.assertNotNull(onttotal)
        Assert.assertNotNull(ongtotal)
    }
}