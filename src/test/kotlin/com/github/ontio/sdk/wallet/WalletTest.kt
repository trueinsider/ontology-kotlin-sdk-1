package com.github.ontio.sdk.wallet

import com.github.ontio.OntSdk.openWalletFile
import com.github.ontio.OntSdk.walletMgr
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File

class WalletTest {
    lateinit var id1: Identity
    lateinit var id2: Identity
    lateinit var acct1: Account
    lateinit var acct2: Account

    val walletFile = "WalletTest.json"

    @Before
    fun setUp() {
        openWalletFile(walletFile)

        id1 = walletMgr.createIdentity("passwordtest")
        id2 = walletMgr.createIdentity("passwordtest")

        acct1 = walletMgr.createAccount("passwordtest")
        acct2 = walletMgr.createAccount("passwordtest")
    }

    @After
    fun removeWallet() {
        val file = File(walletFile)
        if (file.exists()) {
            if (file.delete()) {
                println("delete wallet file success")
            }
        }
    }

    @Test
    fun getAccount() {
        val acct = walletMgr.wallet!!.getAccount(acct1.address)
        Assert.assertNotNull(acct)

        walletMgr.wallet!!.setDefaultIdentity(id1.ontid)
        walletMgr.wallet!!.setDefaultIdentity(1)
        walletMgr.wallet!!.setDefaultAccount(acct1.address)
        walletMgr.wallet!!.setDefaultAccount(1)
        val did = walletMgr.wallet!!.getIdentity(id1.ontid)
        Assert.assertNotNull(did)
        val b = walletMgr.wallet!!.removeIdentity(id1.ontid)
        Assert.assertTrue(b)

        val b2 = walletMgr.wallet!!.removeAccount(acct1.address)
        Assert.assertTrue(b2)
    }
}