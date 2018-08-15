package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk.DEFAULT_GAS_LIMIT
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.openWalletFile
import com.github.ontio.OntSdk.restful
import com.github.ontio.OntSdk.setDefaultConnect
import com.github.ontio.OntSdk.setRestful
import com.github.ontio.OntSdk.signTx
import com.github.ontio.OntSdk.walletMgr
import com.github.ontio.OntSdkTest
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.Helper
import com.github.ontio.core.payload.DeployCode
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.smartcontract.NeoVm
import com.github.ontio.smartcontract.Vm.makeDeployCodeTransaction
import com.github.ontio.smartcontract.neovm.abi.AbiInfo
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class AuthTest {
    lateinit var codeHex: String
    lateinit var codeAddress: String
    lateinit var abi: String
    lateinit var account: Account
    lateinit var password: String
    lateinit var adminIdentity: Identity
    lateinit var identity: Identity
    lateinit var identity2: Identity

    val walletFile = "AuthTest.json"

    @Before
    fun setUp() {
        setRestful(OntSdkTest.URL)
        setDefaultConnect(restful)
        openWalletFile(walletFile)
        codeHex = "57c56b6c766b00527ac46c766b51527ac4616c766b00c304696e6974876c766b52527ac46c766b52c3641100616509016c766b53527ac46294006c766b00c30a717565727961646d696e876c766b54527ac46c766b54c3641100616598006c766b53527ac46266006c766b00c303666f6f876c766b55527ac46c766b55c3644200616c766b00c36c766b51c3617c655d01009c6c766b56527ac46c766b56c3640e00006c766b53527ac46221006c766b51c3616521006c766b53527ac4620e00006c766b53527ac46203006c766b53c3616c756652c56b6c766b00527ac461516c766b51527ac46203006c766b51c3616c756651c56b61612a6469643a6f6e743a41617a457666515063513247454646504c46315a4c7751374b356a446e38316876656c766b00527ac46203006c766b00c3616c756653c56b611400000000000000000000000000000000000000066c766b00527ac4006c766b00c311696e6974436f6e747261637441646d696e612a6469643a6f6e743a41617a457666515063513247454646504c46315a4c7751374b356a446e383168766561537951795572755172755279527954727552727568164f6e746f6c6f67792e4e61746976652e496e766f6b656c766b51527ac46c766b51c300517f519c6c766b52527ac46203006c766b52c3616c756657c56b6c766b00527ac46c766b51527ac461556154c66c766b527a527ac46c766b55c36c766b52527ac46c766b52c361682d53797374656d2e457865637574696f6e456e67696e652e476574457865637574696e6753637269707448617368007cc46c766b52c36c766b00c3527cc46c766b52c36c766b51c300c3517cc46c766b52c36c766b51c351c3537cc41400000000000000000000000000000000000000066c766b53527ac4006c766b53c30b766572696679546f6b656e6c766b52c361537951795572755172755279527954727552727568164f6e746f6c6f67792e4e61746976652e496e766f6b656c766b54527ac46c766b54c300517f519c6c766b56527ac46203006c766b56c3616c7566"
        codeAddress = Address.AddressFromVmCode(codeHex).toHexString()
        abi = "{\"hash\":\"0x3acea0d75537d14762b692dc7e3c62b98975fa50\",\"entrypoint\":\"Main\",\"functions\":[{\"name\":\"Main\",\"parameters\":[{\"name\":\"operation\",\"type\":\"String\"},{\"name\":\"args\",\"type\":\"Array\"}],\"returntype\":\"Any\"},{\"name\":\"foo\",\"parameters\":[{\"name\":\"args\",\"type\":\"Array\"}],\"returntype\":\"Boolean\"},{\"name\":\"queryadmin\",\"parameters\":[],\"returntype\":\"ByteArray\"},{\"name\":\"init\",\"parameters\":[],\"returntype\":\"Boolean\"}],\"events\":[]}"
        account = Account(Helper.hexToBytes(OntSdkTest.PRIVATEKEY), SignatureScheme.SHA256WITHECDSA)
        password = "111111"
        adminIdentity = walletMgr.createIdentityFromPriKey(password, OntSdkTest.PRIVATEKEY)
        identity = walletMgr.createIdentityFromPriKey(password, OntSdkTest.PRIVATEKEY2)
        identity2 = walletMgr.createIdentityFromPriKey(password, OntSdkTest.PRIVATEKEY3)
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
    fun test() {
        OntId.sendRegister(adminIdentity, password, account, DEFAULT_GAS_LIMIT, 0)
        OntId.sendRegister(identity, password, account, DEFAULT_GAS_LIMIT, 0)
        OntId.sendRegister(identity2, password, account, DEFAULT_GAS_LIMIT, 0)
        val tx = makeDeployCodeTransaction(codeHex, true, "name", "v1.0", "author",
                "email", "desp", account.addressU160.toBase58(), 20000000, 0)
        signTx(tx, arrayOf(arrayOf(account)))
        val txHex = Helper.toHexString(tx.toArray())
        connect!!.sendRawTransaction(txHex)
        Thread.sleep(6000)
        val t = connect!!.getTransaction(tx.hash().toHexString()) as DeployCode
        assertNotNull(t)
    }

    @Test
    fun sendTransfer() {
        val txhash = Auth.sendTransfer(identity.ontid, password, identity.controls[0].getSalt(), 1, codeAddress, adminIdentity.ontid, account, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val obj = connect!!.getSmartCodeEvent(txhash)
        assertTrue((obj as JSONObject).getString("State") == "1")
    }

    @Test
    fun initTest() {

        val abiInfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "init"
        val function = abiInfo.getFunction(name)
        function!!.setParamsValue()
        val txhash = NeoVm.sendTransaction(Helper.reverse(codeAddress), account, account, DEFAULT_GAS_LIMIT, 0, function, false) as String
        Thread.sleep(6000)
        val obj = connect!!.getSmartCodeEvent(txhash)
        println(obj)
    }

    @Test
    fun queryAdmin() {
        val abiInfo = JSON.parseObject(abi, AbiInfo::class.java)
        val name = "queryadmin"
        val function = abiInfo.getFunction(name)
        function!!.setParamsValue()
        val obj = NeoVm.sendTransaction(Helper.reverse(codeAddress), account, account, DEFAULT_GAS_LIMIT, 0, function, true)
        val res = (obj as JSONObject).getString("Result")
        val aa = String(Helper.hexToBytes(res))
        assertTrue("did:ont:AazEvfQPcQ2GEFFPLF1ZLwQ7K5jDn81hve" == aa)
    }


    @Test
    fun assignFuncsToRole2() {
        var txhash = Auth.assignFuncsToRole(adminIdentity.ontid, password, adminIdentity.controls[0].getSalt(),
                1, Helper.reverse(codeAddress), "role1", arrayOf("foo1"), account, DEFAULT_GAS_LIMIT, 0)!!

        val txhash2 = Auth.assignFuncsToRole(adminIdentity.ontid, password, adminIdentity.controls[0].getSalt(), 1,
                Helper.reverse(codeAddress), "role2", arrayOf("foo2", "foo3"), account, DEFAULT_GAS_LIMIT, 0)!!
        Thread.sleep(6000)
        var obj = connect!!.getSmartCodeEvent(txhash)
        assertTrue((obj as JSONObject).getString("State") == "1")
        obj = connect!!.getSmartCodeEvent(txhash2)
        assertTrue((obj as JSONObject).getString("State") == "1")
        txhash = Auth.assignOntIdsToRole(adminIdentity.ontid, password, adminIdentity.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "role1", arrayOf(identity2.ontid), account, DEFAULT_GAS_LIMIT, 0)!!
        Thread.sleep(6000)
        obj = connect!!.getSmartCodeEvent(txhash)
        assertTrue((obj as JSONObject).getString("State") == "1")
        val result = Auth.verifyToken(identity2.ontid, password, identity2.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "foo1")
        assertTrue(result == "01")
    }


    @Test
    fun assignFuncsToRole() {
        var txhash = Auth.assignFuncsToRole(adminIdentity.ontid, password, adminIdentity.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "role", arrayOf("foo"), account, DEFAULT_GAS_LIMIT, 0)!!
        Thread.sleep(6000)
        var obj = connect!!.getSmartCodeEvent(txhash)
        assertTrue((obj as JSONObject).getString("State") == "1")
        txhash = Auth.assignOntIdsToRole(adminIdentity.ontid, password, adminIdentity.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "role", arrayOf(identity2.ontid), account, DEFAULT_GAS_LIMIT, 0)!!
        Thread.sleep(6000)
        obj = connect!!.getSmartCodeEvent(txhash)
        assertTrue((obj as JSONObject).getString("State") == "1")
        val result = Auth.verifyToken(identity2.ontid, password, identity2.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "foo")
        assertTrue(result == "01")
    }

    @Test
    fun delegate() {
        Auth.delegate(identity2.ontid, password, identity2.controls[0].getSalt(), 1, Helper.reverse(codeAddress), identity.ontid, "role", (60 * 5).toLong(), 1, account, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        Auth.withdraw(identity2.ontid, password, identity2.controls[0].getSalt(), 1, Helper.reverse(codeAddress), identity.ontid, "role1", account, DEFAULT_GAS_LIMIT, 0)
        Auth.verifyToken(identity2.ontid, password, identity2.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "foo")
        val result2 = Auth.verifyToken(identity.ontid, password, identity.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "foo")
        assertTrue(result2 == "01")
    }

    @Test
    fun verifyToken() {
        val result2 = Auth.verifyToken(identity.ontid, password, identity.controls[0].getSalt(), 1, Helper.reverse(codeAddress), "foo")
        assertTrue(result2 == "01")
    }
}