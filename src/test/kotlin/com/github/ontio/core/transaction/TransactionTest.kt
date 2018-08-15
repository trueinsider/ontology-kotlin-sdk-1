package com.github.ontio.core.transaction

import com.github.ontio.OntSdk.signTx
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.Helper
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.smartcontract.Vm.buildNativeParams
import org.junit.Test

class TransactionTest {
    val ontContract = "ff00000000000000000000000000000000000001"

    @Test
    fun serialize() {
        val tx = buildNativeParams(Address.parse(ontContract), "init", "1".toByteArray(), null, 0, 0)
        val account = Account(Helper.hexToBytes("0bc8c1f75a028672cd42c221bf81709dfc7abbbaf0d87cb6fdeaf9a20492c194"), SignatureScheme.SHA256WITHECDSA)
        signTx(tx, arrayOf(arrayOf(account)))

        val t = tx.toHexString()
        println(t)

        val tx2 = Transaction.deserializeFrom(Helper.hexToBytes(t))
        println(tx2.json())
    }
}