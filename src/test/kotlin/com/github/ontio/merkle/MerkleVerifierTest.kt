package com.github.ontio.merkle

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
import com.github.ontio.common.UInt256
import com.github.ontio.smartcontract.nativevm.OntId.makeRegister
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class MerkleVerifierTest {
    val password = "111111"
    val walletFile = "MerkleVerifierTest.json"

    @Before
    fun setUp() {
        val restUrl = OntSdkTest.URL

        setRestful(restUrl)
        setDefaultConnect(restful)

        openWalletFile(walletFile)
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
    fun getProof() {
        val identity = walletMgr.createIdentity(password)
        val payer = walletMgr.createAccount(password)
        val salt = identity.controls[0].getSalt()
        val tx = makeRegister(identity.ontid, password, salt, payer.address, DEFAULT_GAS_LIMIT, 0)
        signTx(tx, identity.ontid, password, salt)
        addSign(tx, payer.address, password, payer.getSalt())
        connect!!.sendRawTransaction(tx)
        Thread.sleep(6000)

        val hash = tx.hash().toHexString()
        println(hash)
        val proof = mutableMapOf<String, Map<String, Any>>()
        val map = mutableMapOf<String, Any>()
        val height = connect!!.getBlockHeightByTxHash(hash)
        map["Type"] = "MerkleProof"
        map["TxnHash"] = hash
        map["BlockHeight"] = height
        println(hash)
        val tmpProof = connect!!.getMerkleProof(hash) as Map<*, *>
        println(JSONObject.toJSONString(tmpProof))
        val txroot = UInt256.parse(tmpProof["TransactionsRoot"] as String)
        val blockHeight = tmpProof["BlockHeight"] as Int
        val curBlockRoot = UInt256.parse(tmpProof["CurBlockRoot"] as String)
        val curBlockHeight = tmpProof["CurBlockHeight"] as Int
        val hashes = tmpProof["TargetHashes"] as List<*>
        val targetHashes = Array(hashes.size) { i -> UInt256.parse(hashes[i] as String) }
        map["MerkleRoot"] = curBlockRoot.toHexString()
        map["Nodes"] = MerkleVerifier.getProof(blockHeight, targetHashes, curBlockHeight + 1)
        proof["Proof"] = map
        val b = MerkleVerifier.Verify(txroot, MerkleVerifier.getProof(blockHeight, targetHashes, curBlockHeight + 1), curBlockRoot)
        assertTrue(b)
    }
}