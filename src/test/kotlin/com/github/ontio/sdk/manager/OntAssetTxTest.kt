package com.github.ontio.sdk.manager

import com.github.ontio.OntSdk.DEFAULT_GAS_LIMIT
import com.github.ontio.OntSdk.openWalletFile
import com.github.ontio.OntSdk.restful
import com.github.ontio.OntSdk.setDefaultConnect
import com.github.ontio.OntSdk.setRestful
import com.github.ontio.OntSdk.walletMgr
import com.github.ontio.OntSdkTest
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.smartcontract.nativevm.Ong
import com.github.ontio.smartcontract.nativevm.Ont
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class OntAssetTxTest {
    lateinit var info1: Account
    lateinit var info2: Account
    lateinit var info3: Account

    val password = "111111"
    val wallet = "OntAssetTxTest.json"

    lateinit var payer: Account

    @Before
    fun setUp() {
        val restUrl = OntSdkTest.URL
        setRestful(restUrl)
        setDefaultConnect(restful)
        openWalletFile(wallet)
        info1 = walletMgr.createAccountFromPriKey(OntSdkTest.PASSWORD, OntSdkTest.PRIVATEKEY)
        info2 = walletMgr.createAccount(password)
        info3 = walletMgr.createAccount(password)

        payer = walletMgr.createAccount(password)
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
    fun sendTransfer() {
        val sendAcct = walletMgr.getAccount(info1.address, password, info1.getSalt())
        val payerAcct = walletMgr.getAccount(payer.address, password, payer.getSalt())
        val res = Ont.sendTransfer(sendAcct, info2.address, 100L, payerAcct, DEFAULT_GAS_LIMIT, 0)

        Assert.assertNotNull(res)
    }

    @Test
    fun makeTransfer() {
        val tx = Ont.makeTransfer(info1.address, info2.address, 100L, payer.address, DEFAULT_GAS_LIMIT, 0)
        Assert.assertNotNull(tx)
    }

    @Test
    fun sendApprove() {
        val sendAcct1 = walletMgr.getAccount(info1.address, password, info1.getSalt())
        val sendAcct2 = walletMgr.getAccount(info2.address, password, info2.getSalt())
        val payerAcct = walletMgr.getAccount(payer.address, password, payer.getSalt())
        Ont.sendApprove(sendAcct1, sendAcct2.addressU160.toBase58(), 10L, payerAcct, DEFAULT_GAS_LIMIT, 0)
        val info1balance = Ont.queryBalanceOf(sendAcct1.addressU160.toBase58())
        val info2balance = Ont.queryBalanceOf(sendAcct2.addressU160.toBase58())
        Thread.sleep(6000)

        val allo = Ont.queryAllowance(sendAcct1.addressU160.toBase58(), sendAcct2.addressU160.toBase58())
        Assert.assertTrue(allo == 10L)
        Ont.sendTransferFrom(sendAcct2, info1.address, sendAcct2.addressU160.toBase58(), 10L, payerAcct, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val info1balance2 = Ont.queryBalanceOf(info1.address)
        val info2balance2 = Ont.queryBalanceOf(info2.address)

        Assert.assertTrue(info1balance - info1balance2 == 10L)
        Assert.assertTrue(info2balance2 - info2balance == 10L)
    }

    @Test
    fun sendOngTransferFrom() {
        val unboundOngStr = Ong.unboundOng(info1.address)
        val unboundOng = unboundOngStr.toLong()
        val res = Ong.withdrawOng(walletMgr.getAccount(info1.address, password, info1.getSalt()), info2.address, unboundOng / 100, walletMgr.getAccount(payer.address, password, payer.getSalt()), DEFAULT_GAS_LIMIT, 0)
        Assert.assertNotNull(res)
    }

    @Test
    fun queryTest() {
        val decimal = Ont.queryDecimals()
        val decimal2 = Ong.queryDecimals()
        Assert.assertNotNull(decimal)
        Assert.assertNotNull(decimal2)

        val ontname = Ont.queryName()
        val ongname = Ong.queryName()
        Assert.assertNotNull(ontname)
        Assert.assertNotNull(ongname)

        val ontsym = Ont.querySymbol()
        val ongsym = Ong.querySymbol()
        Assert.assertNotNull(ontsym)
        Assert.assertNotNull(ongsym)

        val onttotal = Ont.queryTotalSupply()
        val ongtotal = Ong.queryTotalSupply()
        Assert.assertNotNull(onttotal)
        Assert.assertNotNull(ongtotal)
    }
}