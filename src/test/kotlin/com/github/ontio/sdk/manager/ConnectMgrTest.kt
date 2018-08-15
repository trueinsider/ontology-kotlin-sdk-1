package com.github.ontio.sdk.manager

import com.github.ontio.OntSdk.DEFAULT_DEPLOY_GAS_LIMIT
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
import com.github.ontio.common.Common
import com.github.ontio.common.Helper
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.wallet.Identity
import com.github.ontio.smartcontract.Vm.buildNativeParams
import com.github.ontio.smartcontract.Vm.makeDeployCodeTransaction
import com.github.ontio.smartcontract.Vm.makeInvokeCodeTransaction
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.neovm.abi.BuildParams
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import java.io.File
import java.util.*

class ConnectMgrTest {
    lateinit var codeAddress: String
    lateinit var blockHash: String

    val password = "111111"
    var identity: Identity? = null
    val ontContract = "0000000000000000000000000000000000000001"

    val wallet = "ConnectMgrTest.json"

    @Before
    fun setUp() {
        val restUrl = OntSdkTest.URL
        setRestful(restUrl)
        setDefaultConnect(restful)
        openWalletFile(wallet)

        identity = if (walletMgr.wallet!!.identities.size < 1) {
            walletMgr.createIdentity(password)
        } else {
            walletMgr.wallet!!.identities[0]
        }
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
    fun sendRawTransaction() {
        //部署交易
        val codeHex = "5ec56b6c766b00527ac46c766b51527ac4616c766b00c306436f6d6d6974876c766b52527ac46c766b52c3645d00616c766b51c3c0529c009c6c766b55527ac46c766b55c3640e00006c766b56527ac4621e016c766b51c300c36c766b53527ac46c766b51c351c36c766b54527ac46c766b53c36c766b54c3617c65fc006c766b56527ac462e9006c766b00c3065265766f6b65876c766b57527ac46c766b57c3645d00616c766b51c3c0529c009c6c766b5a527ac46c766b5ac3640e00006c766b56527ac462a8006c766b51c300c36c766b58527ac46c766b51c351c36c766b59527ac46c766b58c36c766b59c3617c65d7016c766b56527ac46273006c766b00c309476574537461747573876c766b5b527ac46c766b5bc3644900616c766b51c3c0519c009c6c766b5d527ac46c766b5dc3640e00006c766b56527ac4622f006c766b51c300c36c766b5c527ac46c766b5cc36165b8036c766b56527ac4620e00006c766b56527ac46203006c766b56c3616c756656c56b6c766b00527ac46c766b51527ac4616168164e656f2e53746f726167652e476574436f6e746578746c766b00c3617c680f4e656f2e53746f726167652e4765746c766b52527ac46c766b52c3640e006c766b52c3c000a0620400006c766b54527ac46c766b54c364410061616c766b00c309206578697374656421617c084572726f724d736753c168124e656f2e52756e74696d652e4e6f7469667961006c766b55527ac462a0000231236c766b53527ac46c766b53c36c766b51c37e6c766b53527ac46168164e656f2e53746f726167652e476574436f6e746578746c766b00c36c766b53c3615272680f4e656f2e53746f726167652e50757461616c766b51c31320637265617465206e657720636c61696e3a206c766b00c3615272045075736854c168124e656f2e52756e74696d652e4e6f7469667961516c766b55527ac46203006c766b55c3616c756658c56b6c766b00527ac46c766b51527ac4616168164e656f2e53746f726167652e476574436f6e746578746c766b00c3617c680f4e656f2e53746f726167652e4765746c766b52527ac46168164e656f2e53746f726167652e476574436f6e746578746c766b00c3617c680f4e656f2e53746f726167652e476574756c766b52c3630e006c766b52c3c0009c620400006c766b54527ac46c766b54c364450061616c766b00c30d206e6f74206578697374656421617c084572726f724d736753c168124e656f2e52756e74696d652e4e6f7469667961006c766b55527ac4625f016c766b52c300517f01309c6c766b56527ac46c766b56c3644a0061616c766b00c312206861732062656564207265766f6b65642e617c084572726f724d736753c168124e656f2e52756e74696d652e4e6f7469667961006c766b55527ac462fe006c766b52c300517f01319c009c6c766b57527ac46c766b57c364490061616c766b00c3112076616c756520696e76616c696465642e617c084572726f724d736753c168124e656f2e52756e74696d652e4e6f7469667961006c766b55527ac4629c000230236c766b53527ac46c766b53c36c766b51c37e6c766b53527ac46168164e656f2e53746f726167652e476574436f6e746578746c766b00c36c766b53c3615272680f4e656f2e53746f726167652e50757461616c766b51c30f207265766f6b6520636c61696d3a206c766b00c3615272045075736854c168124e656f2e52756e74696d652e4e6f7469667961516c766b55527ac46203006c766b55c3616c756653c56b6c766b00527ac4616168164e656f2e53746f726167652e476574436f6e746578746c766b00c3617c680f4e656f2e53746f726167652e4765746c766b51527ac4616c766b00c309207374617475733a206c766b51c3615272045075736854c168124e656f2e52756e74696d652e4e6f74696679616c766b51c36c766b52527ac46203006c766b52c3616c7566"
        codeAddress = Address.AddressFromVmCode(codeHex).toHexString()
        val tx = makeDeployCodeTransaction(codeHex, true, "name", "1.0", "1", "1", "1", identity!!.ontid, DEFAULT_DEPLOY_GAS_LIMIT, 0)
        signTx(tx, identity!!.ontid, password, identity!!.controls[0].getSalt())

        val txHex = Helper.toHexString(tx.toArray())
        val b = connect!!.sendRawTransaction(txHex)
        Thread.sleep(6000)

        walletMgr.getAccountInfo(identity!!.ontid.replace(Common.didont, ""), password, identity!!.controls[0].getSalt())
        val list = ArrayList<Any>()
        list.add("Commit".toByteArray())
        val tmp = ArrayList<Any>()
        tmp.add(Helper.hexToBytes("dde1a09571bf98e04e62b1a8778b2d413747408f4594c946577965fa571de8e5"))
        tmp.add(identity!!.ontid.toByteArray())
        list.add(tmp)
        val params = BuildParams.createCodeParamsScript(list)
        val tx2 = makeInvokeCodeTransaction(codeAddress, null, params, identity!!.ontid, DEFAULT_GAS_LIMIT, 0)
        signTx(tx2, identity!!.ontid, password, identity!!.controls[0].getSalt())
        connect!!.sendRawTransaction(tx2.toHexString())
        Assert.assertEquals(true, b)

        Thread.sleep(6000)

        val txres = connect!!.getTransaction(tx2.hash().toHexString())
        Assert.assertNotNull(txres)
        val obj = connect!!.getTransactionJson(tx2.hash().toHexString())
        Assert.assertNotNull(obj)

        val obj2 = connect!!.getSmartCodeEvent(tx2.hash().toHexString())
        Assert.assertNotNull(obj2)

        val blockheight = connect!!.getBlockHeightByTxHash(tx2.hash().toHexString())
        Assert.assertNotNull(blockheight)
    }

    @Test
    fun sendRawTransactionPreExec() {
        val account = Account(Helper.hexToBytes(OntSdkTest.PRIVATEKEY), SignatureScheme.SHA256WITHECDSA)
        val list = mutableListOf<Address>()
        list.add(Address.decodeBase58(account.addressU160.toBase58()))
        val parabytes = NativeBuildParams.createCodeParamsScript(list)
        val tx = buildNativeParams(Address(Helper.hexToBytes(ontContract)), "balanceOf", parabytes, null, 0, 0)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
        Assert.assertNotEquals(null, obj)
    }

    @Test
    fun getBlockHeight() {

        val res = connect!!.getBlockHeight()
        Assert.assertTrue(res > 0)
    }

    @Test
    fun getBlock() {
        val blockHeight = connect!!.getBlockHeight()
        val b = connect!!.getBlock(blockHeight)
        Assert.assertNotNull(b)

    }

    @Test
    fun getBlockByBlockhash() {
        val blockHeight = connect!!.getBlockHeight()
        val b2 = connect!!.getBlock(blockHeight)
        blockHash = b2.hash().toString()
        val b = connect!!.getBlock(blockHash)
        Assert.assertNotNull(b)
    }

    @Test
    fun getBalance() {
        val account = Account(Helper.hexToBytes(OntSdkTest.PRIVATEKEY), SignatureScheme.SHA256WITHECDSA)
        val obj = connect!!.getBalance(account.addressU160.toBase58())
        Assert.assertNotNull(obj)
    }

    @Test
    fun getBlockJson() {
        val blockHeight = connect!!.getBlockHeight()
        val obj = connect!!.getBlockJson(blockHeight)
        Assert.assertNotNull(obj)
    }

}