package com.github.ontio.core.transaction

import com.github.ontio.OntSdk
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.Helper
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.Vm
import org.junit.Before
import org.junit.Test

import java.io.IOException

import org.junit.Assert.*

class TransactionTest {

    internal var ontSdk: OntSdk
    internal var vm: Vm
    internal var ontContract = "ff00000000000000000000000000000000000001"

    @Before
    fun setUp() {
        ontSdk = OntSdk.getInstance()
        vm = Vm(ontSdk)
    }

    @Test
    @Throws(Exception::class)
    fun serialize() {
        val tx = vm.buildNativeParams(Address.parse(ontContract), "init", "1".toByteArray(), null, 0, 0)
        val account = Account(Helper.hexToBytes("0bc8c1f75a028672cd42c221bf81709dfc7abbbaf0d87cb6fdeaf9a20492c194"), SignatureScheme.SHA256WITHECDSA)
        ontSdk.signTx(tx, arrayOf(arrayOf(account)))

        val t = tx.toHexString()
        println(t)

        val tx2 = Transaction.deserializeFrom(Helper.hexToBytes(t))
        println(tx2.json())


    }
}