package com.github.ontio.sdk.manager

import com.github.ontio.OntSdk
import com.github.ontio.OntSdkTest
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.sdk.wallet.Wallet
import com.github.ontio.smartcontract.nativevm.OntId
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.io.File

import org.junit.Assert.*

class WalletMgrTest {
    private var ontSdk: OntSdk? = null
    private var walletMgr: WalletMgr? = null
    private var wallet: Wallet? = null
    private var ontIdTx: OntId? = null

    internal var password = "111111"
    internal var salt = byteArrayOf()
    internal var payer: Account? = null

    internal var walletFile = "wallet.json"

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ontSdk = OntSdk.getInstance()
        ontSdk!!.setRestful(OntSdkTest.URL)
        ontSdk!!.openWalletFile(walletFile)
        walletMgr = ontSdk!!.walletMgr
        wallet = walletMgr!!.wallet
        ontIdTx = ontSdk!!.nativevm().ontId()
        payer = ontSdk!!.walletMgr!!.createAccount(password)

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
        ontSdk!!.openWalletFile("wallet.json")
        walletMgr = ontSdk!!.walletMgr
        assertNotNull(walletMgr)
    }

    @Test
    fun getWallet() {
    }

    @Test
    @Throws(Exception::class)
    fun writeWallet() {
        walletMgr!!.writeWallet()
        val f = File("wallet.json")
        val isExist = f.exists() && !f.isDirectory
        assertTrue(isExist)
    }

    @Test
    @Throws(Exception::class)
    fun createIdentity() {
        val identity = walletMgr!!.createIdentity(password)
        val account = walletMgr!!.getAccount(identity!!.ontid, password, identity.controls[0].getSalt())
        assertNotNull(account)
        assertNotNull(identity)
        assertNotNull(identity.ontid)
        assertNotEquals(identity.ontid, "")
    }

    @Test
    @Throws(Exception::class)
    fun importIdentity() {
        val identities = wallet!!.identities
        identities.clear()
        walletMgr!!.writeWallet()
        assertEquals(identities.size.toLong(), 0)

        val identity = walletMgr!!.createIdentity(password)
        val account = walletMgr!!.getAccount(identity!!.ontid, password, identity.controls[0].getSalt())
        val prikeyStr = account.exportGcmEncryptedPrikey(password, identity.controls[0].getSalt(), 16384)
        assertTrue(identities.size == 1)
        identities.clear()
        walletMgr!!.writeWallet()
        assertTrue(identities.size == 0)

        val addr = identity.ontid.substring(8)
        walletMgr!!.importIdentity(prikeyStr, password, identity.controls[0].getSalt(), addr)
        assertTrue(identities.size == 1)
        val identity1 = identities[0]
        assertEquals(identity.ontid, identity1.ontid)
    }

    @Test
    @Throws(Exception::class)
    fun importAccount() {
        val accounts = walletMgr!!.wallet!!.accounts
        accounts.clear()
        assertEquals(accounts.size.toLong(), 0)
        walletMgr!!.writeWallet()
        val account = walletMgr!!.createAccount(password)
        val accountDiff = walletMgr!!.getAccount(account!!.address, password, account.getSalt())
        val prikeyStr = accountDiff.exportGcmEncryptedPrikey(password, account.getSalt(), 16384)
        assertTrue(accounts.size == 1)
        accounts.clear()
        assertTrue(accounts.size == 0)
        walletMgr!!.writeWallet()

        val account1 = walletMgr!!.importAccount(prikeyStr, password, account.address, account.getSalt())
        assertTrue(accounts.size == 1)
        assertEquals(account.address, account1!!.address)

    }
}