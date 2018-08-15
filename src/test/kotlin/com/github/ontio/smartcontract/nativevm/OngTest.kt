package com.github.ontio.smartcontract.nativevm

import com.github.ontio.OntSdk.DEFAULT_GAS_LIMIT
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.setRestful
import com.github.ontio.OntSdkTest
import com.github.ontio.account.Account
import com.github.ontio.common.Helper
import com.github.ontio.crypto.SignatureScheme
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class OngTest {
    lateinit var account: Account
    lateinit var receiveAcc: Account

    @Before
    fun setUp() {
        setRestful(OntSdkTest.URL)
        account = Account(Helper.hexToBytes(OntSdkTest.PRIVATEKEY), SignatureScheme.SHA256WITHECDSA)
        receiveAcc = Account(SignatureScheme.SHA256WITHECDSA)
        Ong.sendTransfer(account, receiveAcc.addressU160.toBase58(), 10L, account, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)

        Ong.unboundOng(account.addressU160.toBase58())
        Ong.withdrawOng(account, account.addressU160.toBase58(), 1000, account, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val obj = connect!!.getBalance(account.addressU160.toBase58())
        println(obj)
    }

    @Test
    fun sendTransfer() {
        val accountOng = Ong.queryBalanceOf(account.addressU160.toBase58())
        val receiveAccOng = Ong.queryBalanceOf(receiveAcc.addressU160.toBase58())
        Ong.sendTransfer(account, receiveAcc.addressU160.toBase58(), 10L, account, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val accountOng2 = Ong.queryBalanceOf(account.addressU160.toBase58())
        val receiveAccOng2 = Ong.queryBalanceOf(receiveAcc.addressU160.toBase58())
        Assert.assertTrue(accountOng - accountOng2 == 10L)
        Assert.assertTrue(receiveAccOng2 - receiveAccOng == 10L)
    }


    @Test
    fun sendApprove() {
        val allowance = Ong.queryAllowance(account.addressU160.toBase58(), receiveAcc.addressU160.toBase58())
        Ong.sendApprove(account, receiveAcc.addressU160.toBase58(), 10, account, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val allowance2 = Ong.queryAllowance(account.addressU160.toBase58(), receiveAcc.addressU160.toBase58())
        Assert.assertTrue(allowance2 - allowance == 10L)

        val acctbalance = Ong.queryBalanceOf(account.addressU160.toBase58())
        val reciebalance = Ong.queryBalanceOf(receiveAcc.addressU160.toBase58())
        Ong.sendTransferFrom(receiveAcc, account.addressU160.toBase58(), receiveAcc.addressU160.toBase58(), 10, receiveAcc, DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val acctbalance2 = Ong.queryBalanceOf(account.addressU160.toBase58())
        val reciebalance2 = Ong.queryBalanceOf(receiveAcc.addressU160.toBase58())
        Assert.assertTrue(acctbalance - acctbalance2 == 10L)
        Assert.assertTrue(reciebalance2 - reciebalance == 10L)
        val allowance3 = Ong.queryAllowance(account.addressU160.toBase58(), receiveAcc.addressU160.toBase58())
        Assert.assertTrue(allowance3 == allowance)
    }

    @Test
    fun queryName() {
        val name = Ong.queryName()
        Assert.assertTrue(name.contains("ONG"))
        val symbol = Ong.querySymbol()
        Assert.assertTrue(symbol.contains("ONG"))
        val decimals = Ong.queryDecimals()
        Assert.assertTrue(decimals == 9L)
        val total = Ong.queryTotalSupply()
        Assert.assertFalse(total < 0)
    }
}