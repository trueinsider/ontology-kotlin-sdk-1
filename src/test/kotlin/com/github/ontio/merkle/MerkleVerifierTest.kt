package com.github.ontio.merkle

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk
import com.github.ontio.OntSdkTest
import com.github.ontio.common.UInt256
import com.github.ontio.core.block.Block
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.network.exception.ConnectorException
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.sdk.wallet.Account
import com.github.ontio.sdk.wallet.Identity
import org.junit.After
import org.junit.Before
import org.junit.Test

import java.io.File
import java.io.IOException
import java.util.HashMap

import org.junit.Assert.*

class MerkleVerifierTest {
    internal var ontSdk: OntSdk
    internal var password = "111111"
    internal var walletFile = "MerkleVerifierTest.json"

    @Before
    @Throws(SDKException::class)
    fun setUp() {
        val restUrl = OntSdkTest.URL

        ontSdk = OntSdk.getInstance()
        ontSdk.setRestful(restUrl)
        ontSdk.setDefaultConnect(ontSdk.restful)

        ontSdk.openWalletFile(walletFile)
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
    fun verifyLeafHashInclusion() {
        val txroot = UInt256.parse("f332c8ede11799137f28b10e40200063353dfc3233da6cea689e0637231ad1a7")
        val curBlkRoot = UInt256.parse("ba64746f650b7be0ac89fbf8defeceeb63821272d8096d83d3764b7ae9eb4a21")
        val targetHashes = arrayOf(UInt256.parse("0000000000000000000000000000000000000000000000000000000000000000"), UInt256.parse("e14172c8a6e193943465648e1c586a9186a3784ee7ee29db9edbf6afe04f5390"), UInt256.parse("f440531999c547db08f516677c152215475a69dccb82176e4bca1b726261a1be"),

                UInt256.parse("ef4d3c0debb66bb15af8b82e1b9463d3039f6a95bf91c349e7df1b34ef5f7630"), UInt256.parse("d6dd5266af7407b89d00b1a11044cd4eb30f94dabbdf440b03f9173384b16d67"), UInt256.parse("17100c09ef2d19689c85cc7038b6654037045fc83b3951439634e5d2074998c6"),

                UInt256.parse("584aa3a421020a07710a2bc16e7865ea9ee0860692ad509515fb8caa837d27df"), UInt256.parse("192eed54084fbe94a8c34c1274168f38ac02abe01c94f9b425e58f26fe93d598"))
        val blockHeight = 1277
        val curBlockHeight = 1277
        val b = MerkleVerifier.VerifyLeafHashInclusion(txroot, blockHeight, targetHashes, curBlkRoot, curBlockHeight + 1)
        assertTrue(b)
    }

    @Test
    @Throws(Exception::class)
    fun getProof() {
        val identity = ontSdk.walletMgr!!.createIdentity(password)
        val payer = ontSdk.walletMgr!!.createAccount(password)
        val salt = identity!!.controls[0].getSalt()
        val tx = ontSdk.nativevm().ontId().makeRegister(identity.ontid, password, salt, payer!!.address, ontSdk.DEFAULT_GAS_LIMIT, 0)
        ontSdk.signTx(tx, identity.ontid, password, salt)
        ontSdk.addSign(tx, payer.address, password, payer.getSalt())
        ontSdk.connect!!.sendRawTransaction(tx)
        Thread.sleep(6000)

        val hash = tx.hash().toHexString()
        println(hash)
        val proof = HashMap()
        val map = HashMap()
        val height = ontSdk.connect!!.getBlockHeightByTxHash(hash)
        map.put("Type", "MerkleProof")
        map.put("TxnHash", hash)
        map.put("BlockHeight", height)
        println(hash)
        val tmpProof = ontSdk.connect!!.getMerkleProof(hash) as Map<*, *>
        println(JSONObject.toJSONString(tmpProof))
        val txroot = UInt256.parse(tmpProof["TransactionsRoot"] as String)
        val blockHeight = tmpProof["BlockHeight"] as Int
        val curBlockRoot = UInt256.parse(tmpProof["CurBlockRoot"] as String)
        val curBlockHeight = tmpProof["CurBlockHeight"] as Int
        val hashes = tmpProof["TargetHashes"] as List<*>
        val targetHashes = arrayOfNulls<UInt256>(hashes.size)
        for (i in hashes.indices) {
            targetHashes[i] = UInt256.parse(hashes[i] as String)
        }
        map.put("MerkleRoot", curBlockRoot.toHexString())
        map.put("Nodes", MerkleVerifier.getProof(txroot, blockHeight, targetHashes, curBlockHeight + 1))
        proof.put("Proof", map)
        val b = MerkleVerifier.Verify(txroot, MerkleVerifier.getProof(txroot, blockHeight, targetHashes, curBlockHeight + 1), curBlockRoot)
        assertTrue(b)
    }
}