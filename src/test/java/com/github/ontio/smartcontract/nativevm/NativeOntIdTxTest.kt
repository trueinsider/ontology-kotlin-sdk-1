package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk
import com.github.ontio.OntSdkTest
import com.github.ontio.common.Address
import com.github.ontio.common.Common
import com.github.ontio.common.Helper
import com.github.ontio.core.ontid.Attribute
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.info.AccountInfo
import com.github.ontio.sdk.info.IdentityInfo
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Identity
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.util.HashMap

class NativeOntIdTxTest {
    internal var ontSdk: OntSdk
    internal var password = "111111"
    internal var payer: Account? = null
    internal var payerAcct: com.github.ontio.account.Account
    internal var identity: Identity? = null
    internal var walletFile = "NativeOntIdTxTest.json"
    @Before
    @Throws(Exception::class)
    fun setUp() {
        ontSdk = OntSdk.getInstance()
        ontSdk.setRestful(OntSdkTest.URL)
        ontSdk.setDefaultConnect(ontSdk.restful)
        ontSdk.openWalletFile(walletFile)
        //        ontSdk.setSignatureScheme(SignatureScheme.SHA256WITHECDSA);
        payer = ontSdk.walletMgr!!.createAccount(password)
        payerAcct = ontSdk.walletMgr!!.getAccount(payer!!.address, password, payer!!.getSalt())
        identity = ontSdk.walletMgr!!.createIdentity(password)
        ontSdk.nativevm().ontId().sendRegister(identity, password, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)
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
    @Throws(Exception::class)
    fun sendRegister() {
        val tx = ontSdk.nativevm().ontId().makeRegister(identity!!.ontid, password, identity!!.controls[0].getSalt(), payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx, identity!!.ontid, password, identity!!.controls[0].getSalt())
        ontSdk.addSign(tx, payerAcct)
        ontSdk.connect!!.sendRawTransaction(tx)

        val identity2 = ontSdk.walletMgr!!.createIdentity(password)
        ontSdk.nativevm().ontId().sendRegister(identity2, password, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)

        val identity3 = ontSdk.walletMgr!!.createIdentity(password)
        val attributes = arrayOfNulls<Attribute>(1)
        attributes[0] = Attribute("key2".toByteArray(), "value2".toByteArray(), "type2".toByteArray())
        ontSdk.nativevm().ontId().sendRegisterWithAttrs(identity3, password, attributes, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = ontSdk.nativevm().ontId().sendGetDDO(identity!!.ontid)
        Assert.assertTrue(ddo.contains(identity!!.ontid))

        val dd02 = ontSdk.nativevm().ontId().sendGetDDO(identity3!!.ontid)
        Assert.assertTrue(dd02.contains("key2"))

        val keystate = ontSdk.nativevm().ontId().sendGetKeyState(identity!!.ontid, 1)
        Assert.assertNotNull(keystate)

        //merkleproof
        val merkleproof = ontSdk.nativevm().ontId().getMerkleProof(tx.hash().toHexString())
        val b = ontSdk.nativevm().ontId().verifyMerkleProof(JSONObject.toJSONString(merkleproof))
        Assert.assertTrue(b)

        //claim
        val map = HashMap<String, Any>()
        map["Issuer"] = identity!!.ontid
        map["Subject"] = identity2!!.ontid

        val clmRevMap = HashMap()
        clmRevMap.put("typ", "AttestContract")
        clmRevMap.put("addr", identity!!.ontid.replace(Common.didont, ""))

        val claim = ontSdk.nativevm().ontId().createOntIdClaim(identity!!.ontid, password, identity!!.controls[0].getSalt(), "claim:context", map, map, clmRevMap, System.currentTimeMillis() / 1000 + 100000)
        val b2 = ontSdk.nativevm().ontId().verifyOntIdClaim(claim)
        Assert.assertTrue(b2)
    }

    @Test
    @Throws(Exception::class)
    fun sendAddPubkey() {
        val info = ontSdk.walletMgr!!.createIdentityInfo(password)
        val info2 = ontSdk.walletMgr!!.createIdentityInfo(password)
        val tx = ontSdk.nativevm().ontId().makeAddPubKey(identity!!.ontid, password, identity!!.controls[0].getSalt(), info.pubkey, payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx, identity!!.ontid, password, identity!!.controls[0].getSalt())
        ontSdk.addSign(tx, payerAcct)
        ontSdk.connect!!.sendRawTransaction(tx)

        ontSdk.nativevm().ontId().sendAddPubKey(identity!!.ontid, password, identity!!.controls[0].getSalt(), info2.pubkey, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = ontSdk.nativevm().ontId().sendGetDDO(identity!!.ontid)
        Assert.assertTrue(ddo.contains(info.pubkey!!))
        Assert.assertTrue(ddo.contains(info2.pubkey!!))

        val publikeys = ontSdk.nativevm().ontId().sendGetPublicKeys(identity!!.ontid)
        Assert.assertNotNull(publikeys)

        val tx2 = ontSdk.nativevm().ontId().makeRemovePubKey(identity!!.ontid, password, identity!!.controls[0].getSalt(), info.pubkey, payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx2, identity!!.ontid, password, identity!!.controls[0].getSalt())
        ontSdk.addSign(tx2, payerAcct)
        ontSdk.connect!!.sendRawTransaction(tx2)

        ontSdk.nativevm().ontId().sendRemovePubKey(identity!!.ontid, password, identity!!.controls[0].getSalt(), info2.pubkey, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val ddo3 = ontSdk.nativevm().ontId().sendGetDDO(identity!!.ontid)
        Assert.assertFalse(ddo3.contains(info.pubkey!!))
        Assert.assertFalse(ddo3.contains(info2.pubkey!!))
    }

    @Test
    @Throws(Exception::class)
    fun sendAddAttributes() {
        val attributes = arrayOfNulls<Attribute>(1)
        attributes[0] = Attribute("key1".toByteArray(), "value1".toByteArray(), "String".toByteArray())
        val tx = ontSdk.nativevm().ontId().makeAddAttributes(identity!!.ontid, password, identity!!.controls[0].getSalt(), attributes, payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx, identity!!.ontid, password, identity!!.controls[0].getSalt())
        ontSdk.addSign(tx, payerAcct)
        ontSdk.connect!!.sendRawTransaction(tx)

        val attributes2 = arrayOfNulls<Attribute>(1)
        attributes2[0] = Attribute("key99".toByteArray(), "value99".toByteArray(), "String".toByteArray())
        ontSdk.nativevm().ontId().sendAddAttributes(identity!!.ontid, password, identity!!.controls[0].getSalt(), attributes2, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = ontSdk.nativevm().ontId().sendGetDDO(identity!!.ontid)
        Assert.assertTrue(ddo.contains("key1"))
        Assert.assertTrue(ddo.contains("key99"))

        val attribute = ontSdk.nativevm().ontId().sendGetAttributes(identity!!.ontid)
        Assert.assertTrue(attribute.contains("key1"))

        val tx2 = ontSdk.nativevm().ontId().makeRemoveAttribute(identity!!.ontid, password, identity!!.controls[0].getSalt(), "key1", payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx2, identity!!.ontid, password, identity!!.controls[0].getSalt())
        ontSdk.addSign(tx2, payerAcct)
        ontSdk.connect!!.sendRawTransaction(tx2)

        ontSdk.nativevm().ontId().sendRemoveAttribute(identity!!.ontid, password, identity!!.controls[0].getSalt(), "key99", payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)

        val ddo2 = ontSdk.nativevm().ontId().sendGetDDO(identity!!.ontid)
        Assert.assertFalse(ddo2.contains("key1"))
        Assert.assertFalse(ddo2.contains("key99"))

    }

    @Test
    @Throws(Exception::class)
    fun sendAddRecovery() {
        val identity = ontSdk.walletMgr!!.createIdentity(password)
        ontSdk.nativevm().ontId().sendRegister(identity, password, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)

        val identity2 = ontSdk.walletMgr!!.createIdentity(password)
        ontSdk.nativevm().ontId().sendRegister(identity2, password, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)

        val account = ontSdk.walletMgr!!.createAccount(password)

        val tx = ontSdk.nativevm().ontId().makeAddRecovery(identity!!.ontid, password, identity.controls[0].getSalt(), account!!.address, payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx, identity.ontid, password, identity.controls[0].getSalt())
        ontSdk.addSign(tx, payerAcct)
        ontSdk.connect!!.sendRawTransaction(tx)

        ontSdk.nativevm().ontId().sendAddRecovery(identity2!!.ontid, password, identity2.controls[0].getSalt(), account.address, payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)

        Thread.sleep(6000)
        val ddo = ontSdk.nativevm().ontId().sendGetDDO(identity.ontid)
        Assert.assertTrue(ddo.contains(account.address))
        val ddo2 = ontSdk.nativevm().ontId().sendGetDDO(identity2.ontid)
        Assert.assertTrue(ddo2.contains(account.address))

        val info2 = ontSdk.walletMgr!!.createAccountInfo(password)

        val tx2 = ontSdk.nativevm().ontId().makeChangeRecovery(identity.ontid, info2.addressBase58, account.address, password, payerAcct.addressU160!!.toBase58(), ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx2, account.address, password, account.getSalt())

        ontSdk.nativevm().ontId().sendChangeRecovery(identity2.ontid, info2.addressBase58, account.address, password, account.getSalt(), payerAcct, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)

        val ddo3 = ontSdk.nativevm().ontId().sendGetDDO(identity.ontid)
        Assert.assertTrue(ddo3.contains(account.address))
        val ddo4 = ontSdk.nativevm().ontId().sendGetDDO(identity2.ontid)
        Assert.assertTrue(ddo4.contains(info2.addressBase58!!))
    }
}