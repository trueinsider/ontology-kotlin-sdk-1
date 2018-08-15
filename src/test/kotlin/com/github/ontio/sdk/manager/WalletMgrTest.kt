package com.github.ontio.sdk.manager

import com.github.ontio.OntSdk.openWalletFile
import com.github.ontio.OntSdk.setRestful
import com.github.ontio.OntSdk.walletMgr
import com.github.ontio.OntSdkTest
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Wallet
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class WalletMgrTest {
    lateinit var wallet: Wallet
    lateinit var payer: Account

    val password = "111111"
    val walletFile = "wallet.json"

    @Before
    fun setUp() {
        setRestful(OntSdkTest.URL)
        openWalletFile(walletFile)
        wallet = walletMgr.wallet!!
        payer = walletMgr.createAccount(password)

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
    fun openWallet() {
        openWalletFile("wallet.json")
    }

    @Test
    fun getWallet() {
    }

    @Test
    fun writeWallet() {
        walletMgr.writeWallet()
        val f = File("wallet.json")
        val isExist = f.exists() && !f.isDirectory
        assertTrue(isExist)
    }

    @Test
    fun createIdentity() {
        val identity = walletMgr.createIdentity(password)
        val account = walletMgr.getAccount(identity.ontid, password, identity.controls[0].getSalt())
        assertNotNull(account)
        assertNotNull(identity)
        assertNotNull(identity.ontid)
        assertNotEquals(identity.ontid, "")
    }

    @Test
    fun importIdentity() {
        val identities = wallet.identities
        identities.clear()
        walletMgr.writeWallet()
        assertEquals(identities.size.toLong(), 0)

        val identity = walletMgr.createIdentity(password)
        val account = walletMgr.getAccount(identity.ontid, password, identity.controls[0].getSalt())
        val prikeyStr = account.exportGcmEncryptedPrikey(password, identity.controls[0].getSalt(), 16384)
        assertTrue(identities.size == 1)
        identities.clear()
        walletMgr.writeWallet()
        assertTrue(identities.size == 0)

        val addr = identity.ontid.substring(8)
        walletMgr.importIdentity(prikeyStr, password, identity.controls[0].getSalt(), addr)
        assertTrue(identities.size == 1)
        val identity1 = identities[0]
        assertEquals(identity.ontid, identity1.ontid)
    }

    @Test
    fun importAccount() {
        val accounts = walletMgr.wallet!!.accounts
        accounts.clear()
        assertEquals(accounts.size.toLong(), 0)
        walletMgr.writeWallet()
        val account = walletMgr.createAccount(password)
        val accountDiff = walletMgr.getAccount(account.address, password, account.getSalt())
        val prikeyStr = accountDiff.exportGcmEncryptedPrikey(password, account.getSalt(), 16384)
        assertTrue(accounts.size == 1)
        accounts.clear()
        assertTrue(accounts.size == 0)
        walletMgr.writeWallet()

        val account1 = walletMgr.importAccount(prikeyStr, password, account.address, account.getSalt())
        assertTrue(accounts.size == 1)
        assertEquals(account.address, account1!!.address)
    }
}