package com.github.ontio.sdk.wallet

import com.github.ontio.OntSdk
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File

import org.junit.Assert.*

class WalletTest {

    internal var ontSdk: OntSdk
    internal var id1: Identity? = null
    internal var id2: Identity? = null
    internal var acct1: Account? = null
    internal var acct2: Account? = null

    internal var walletFile = "WalletTest.json"

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ontSdk = OntSdk.getInstance()
        ontSdk.openWalletFile(walletFile)


        id1 = ontSdk.walletMgr!!.createIdentity("passwordtest")
        id2 = ontSdk.walletMgr!!.createIdentity("passwordtest")

        acct1 = ontSdk.walletMgr!!.createAccount("passwordtest")
        acct2 = ontSdk.walletMgr!!.createAccount("passwordtest")
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
    @Throws(Exception::class)
    fun getAccount() {
        val acct = ontSdk.walletMgr!!.wallet!!.getAccount(acct1!!.address)
        Assert.assertNotNull(acct)

        ontSdk.walletMgr!!.wallet!!.setDefaultIdentity(id1!!.ontid)
        ontSdk.walletMgr!!.wallet!!.setDefaultIdentity(1)
        ontSdk.walletMgr!!.wallet!!.setDefaultAccount(acct1!!.address)
        ontSdk.walletMgr!!.wallet!!.setDefaultAccount(1)
        val did = ontSdk.walletMgr!!.wallet!!.getIdentity(id1!!.ontid)
        Assert.assertNotNull(did)
        val b = ontSdk.walletMgr!!.wallet!!.removeIdentity(id1!!.ontid)
        Assert.assertTrue(b)

        val b2 = ontSdk.walletMgr!!.wallet!!.removeAccount(acct1!!.address)
        Assert.assertTrue(b2)


    }


}