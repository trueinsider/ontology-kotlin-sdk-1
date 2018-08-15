package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk.DEFAULT_GAS_LIMIT
import com.github.ontio.OntSdk.addSign
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.openWalletFile
import com.github.ontio.OntSdk.restful
import com.github.ontio.OntSdk.setDefaultConnect
import com.github.ontio.OntSdk.setRestful
import com.github.ontio.OntSdk.signTx
import com.github.ontio.OntSdk.walletMgr
import com.github.ontio.OntSdkTest
import com.github.ontio.common.Common
import com.github.ontio.core.ontid.Attribute
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Identity
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

class NativeOntIdTxTest {
    lateinit var payer: Account
    lateinit var payerAcct: com.github.ontio.account.Account
    lateinit var identity: Identity

    val password = "111111"
    val walletFile = "NativeOntIdTxTest.json"

    @Before
    fun setUp() {
        setRestful(OntSdkTest.URL)
        setDefaultConnect(restful)
        openWalletFile(walletFile)
        payer = walletMgr.createAccount(password)
        payerAcct = walletMgr.getAccount(payer.address, password, payer.getSalt())
        identity = walletMgr.createIdentity(password)
        OntId.sendRegister(identity, password, payerAcct, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
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
    fun sendRegister() {
        val tx = OntId.makeRegister(identity.ontid, password, identity.controls[0].getSalt(), payer.address, DEFAULT_GAS_LIMIT, 0)
        signTx(tx, identity.ontid, password, identity.controls[0].getSalt())
        addSign(tx, payerAcct)
        connect!!.sendRawTransaction(tx)

        val identity2 = walletMgr.createIdentity(password)
        OntId.sendRegister(identity2, password, payerAcct, DEFAULT_GAS_LIMIT, 0)

        val identity3 = walletMgr.createIdentity(password)
        val attributes = arrayOf(Attribute("key2".toByteArray(), "value2".toByteArray(), "type2".toByteArray()))
        OntId.sendRegisterWithAttrs(identity3, password, attributes, payerAcct, DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = OntId.sendGetDDO(identity.ontid)
        Assert.assertTrue(ddo.contains(identity.ontid))

        val dd02 = OntId.sendGetDDO(identity3.ontid)
        Assert.assertTrue(dd02.contains("key2"))

        val keystate = OntId.sendGetKeyState(identity.ontid, 1)
        Assert.assertNotNull(keystate)

        //merkleproof
        val merkleproof = OntId.getMerkleProof(tx.hash().toHexString())
        val b = OntId.verifyMerkleProof(JSONObject.toJSONString(merkleproof))
        Assert.assertTrue(b)

        //claim
        val map = mutableMapOf<String, String>()
        map["Issuer"] = identity.ontid
        map["Subject"] = identity2.ontid

        val clmRevMap = mutableMapOf<String, String>()
        clmRevMap["typ"] = "AttestContract"
        clmRevMap["addr"] = identity.ontid.replace(Common.didont, "")

        val claim = OntId.createOntIdClaim(identity.ontid, password, identity.controls[0].getSalt(), "claim:context", map, map, clmRevMap, System.currentTimeMillis() / 1000 + 100000)
        val b2 = OntId.verifyOntIdClaim(claim)
        Assert.assertTrue(b2)
    }

    @Test
    fun sendAddPubkey() {
        val info = walletMgr.createIdentityInfo(password)
        val info2 = walletMgr.createIdentityInfo(password)
        val tx = OntId.makeAddPubKey(identity.ontid, password, identity.controls[0].getSalt(), info.pubkey, payer.address, DEFAULT_GAS_LIMIT, 0)
        signTx(tx, identity.ontid, password, identity.controls[0].getSalt())
        addSign(tx, payerAcct)
        connect!!.sendRawTransaction(tx)

        OntId.sendAddPubKey(identity.ontid, password, identity.controls[0].getSalt(), info2.pubkey, payerAcct, DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = OntId.sendGetDDO(identity.ontid)
        Assert.assertTrue(ddo.contains(info.pubkey))
        Assert.assertTrue(ddo.contains(info2.pubkey))

        val publikeys = OntId.sendGetPublicKeys(identity.ontid)
        Assert.assertNotNull(publikeys)

        val tx2 = OntId.makeRemovePubKey(identity.ontid, password, identity.controls[0].getSalt(), info.pubkey, payer.address, DEFAULT_GAS_LIMIT, 0)
        signTx(tx2, identity.ontid, password, identity.controls[0].getSalt())
        addSign(tx2, payerAcct)
        connect!!.sendRawTransaction(tx2)

        OntId.sendRemovePubKey(identity.ontid, password, identity.controls[0].getSalt(), info2.pubkey, payerAcct, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val ddo3 = OntId.sendGetDDO(identity.ontid)
        Assert.assertFalse(ddo3.contains(info.pubkey))
        Assert.assertFalse(ddo3.contains(info2.pubkey))
    }

    @Test
    fun sendAddAttributes() {
        val attributes = arrayOf(Attribute("key1".toByteArray(), "value1".toByteArray(), "String".toByteArray()))
        val tx = OntId.makeAddAttributes(identity.ontid, password, identity.controls[0].getSalt(), attributes, payer.address, DEFAULT_GAS_LIMIT, 0)
        signTx(tx, identity.ontid, password, identity.controls[0].getSalt())
        addSign(tx, payerAcct)
        connect!!.sendRawTransaction(tx)

        val attributes2 = arrayOf(Attribute("key99".toByteArray(), "value99".toByteArray(), "String".toByteArray()))
        OntId.sendAddAttributes(identity.ontid, password, identity.controls[0].getSalt(), attributes2, payerAcct, DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = OntId.sendGetDDO(identity.ontid)
        Assert.assertTrue(ddo.contains("key1"))
        Assert.assertTrue(ddo.contains("key99"))

        val attribute = OntId.sendGetAttributes(identity.ontid)
        Assert.assertTrue(attribute.contains("key1"))

        val tx2 = OntId.makeRemoveAttribute(identity.ontid, password, identity.controls[0].getSalt(), "key1", payer.address, DEFAULT_GAS_LIMIT, 0)
        signTx(tx2, identity.ontid, password, identity.controls[0].getSalt())
        addSign(tx2, payerAcct)
        connect!!.sendRawTransaction(tx2)

        OntId.sendRemoveAttribute(identity.ontid, password, identity.controls[0].getSalt(), "key99", payerAcct, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)

        val ddo2 = OntId.sendGetDDO(identity.ontid)
        Assert.assertFalse(ddo2.contains("key1"))
        Assert.assertFalse(ddo2.contains("key99"))

    }

    @Test
    fun sendAddRecovery() {
        val identity = walletMgr.createIdentity(password)
        OntId.sendRegister(identity, password, payerAcct, DEFAULT_GAS_LIMIT, 0)

        val identity2 = walletMgr.createIdentity(password)
        OntId.sendRegister(identity2, password, payerAcct, DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)

        val account = walletMgr.createAccount(password)

        val tx = OntId.makeAddRecovery(identity.ontid, password, identity.controls[0].getSalt(), account.address, payer.address, DEFAULT_GAS_LIMIT, 0)
        signTx(tx, identity.ontid, password, identity.controls[0].getSalt())
        addSign(tx, payerAcct)
        connect!!.sendRawTransaction(tx)

        OntId.sendAddRecovery(identity2.ontid, password, identity2.controls[0].getSalt(), account.address, payerAcct, DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = OntId.sendGetDDO(identity.ontid)
        Assert.assertTrue(ddo.contains(account.address))
        val ddo2 = OntId.sendGetDDO(identity2.ontid)
        Assert.assertTrue(ddo2.contains(account.address))

        val info2 = walletMgr.createAccountInfo(password)

        val tx2 = OntId.makeChangeRecovery(identity.ontid, info2.addressBase58, account.address, password, payerAcct.addressU160.toBase58(), DEFAULT_GAS_LIMIT, 0)
        signTx(tx2, account.address, password, account.getSalt())

        OntId.sendChangeRecovery(identity2.ontid, info2.addressBase58, account.address, password, account.getSalt(), payerAcct, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)

        val ddo3 = OntId.sendGetDDO(identity.ontid)
        Assert.assertTrue(ddo3.contains(account.address))
        val ddo4 = OntId.sendGetDDO(identity2.ontid)
        Assert.assertTrue(ddo4.contains(info2.addressBase58))
    }
}