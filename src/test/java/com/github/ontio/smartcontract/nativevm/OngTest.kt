package com.github.ontio.smartcontract.nativevm

import com.github.ontio.OntSdk
import com.github.ontio.OntSdkTest
import com.github.ontio.account.Account
import com.github.ontio.common.Helper
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.exception.SDKException
import org.junit.Assert
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class OngTest {

    var password = "111111"
    internal var ontSdk: OntSdk
    internal var account: Account
    internal var receiveAcc: Account

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ontSdk = OntSdk.getInstance()
        ontSdk.setRestful(OntSdkTest.URL)
        account = Account(Helper.hexToBytes(OntSdkTest.PRIVATEKEY), SignatureScheme.SHA256WITHECDSA)
        receiveAcc = Account(SignatureScheme.SHA256WITHECDSA)
        ontSdk.nativevm().ont().sendTransfer(account, receiveAcc.addressU160!!.toBase58(), 10L, account, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)

        val accountOng = ontSdk.nativevm().ong().unboundOng(account.addressU160!!.toBase58())
        ontSdk.nativevm().ong().withdrawOng(account, account.addressU160!!.toBase58(), 1000, account, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val obj = ontSdk.connect!!.getBalance(account.addressU160!!.toBase58())
        println(obj)
    }

    @Test
    @Throws(Exception::class)
    fun sendTransfer() {
        val accountOng = ontSdk.nativevm().ong().queryBalanceOf(account.addressU160!!.toBase58())
        val receiveAccOng = ontSdk.nativevm().ong().queryBalanceOf(receiveAcc.addressU160!!.toBase58())
        ontSdk.nativevm().ong().sendTransfer(account, receiveAcc.addressU160!!.toBase58(), 10L, account, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val accountOng2 = ontSdk.nativevm().ong().queryBalanceOf(account.addressU160!!.toBase58())
        val receiveAccOng2 = ontSdk.nativevm().ong().queryBalanceOf(receiveAcc.addressU160!!.toBase58())
        Assert.assertTrue(accountOng - accountOng2 == 10L)
        Assert.assertTrue(receiveAccOng2 - receiveAccOng == 10L)
    }


    @Test
    @Throws(Exception::class)
    fun sendApprove() {
        val allowance = ontSdk.nativevm().ong().queryAllowance(account.addressU160!!.toBase58(), receiveAcc.addressU160!!.toBase58())
        ontSdk.nativevm().ong().sendApprove(account, receiveAcc.addressU160!!.toBase58(), 10, account, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val allowance2 = ontSdk.nativevm().ong().queryAllowance(account.addressU160!!.toBase58(), receiveAcc.addressU160!!.toBase58())
        Assert.assertTrue(allowance2 - allowance == 10L)

        val acctbalance = ontSdk.nativevm().ong().queryBalanceOf(account.addressU160!!.toBase58())
        val reciebalance = ontSdk.nativevm().ong().queryBalanceOf(receiveAcc.addressU160!!.toBase58())
        ontSdk.nativevm().ong().sendTransferFrom(receiveAcc, account.addressU160!!.toBase58(), receiveAcc.addressU160!!.toBase58(), 10, receiveAcc, ontSdk.DEFAULT_GAS_LIMIT, 0)
        Thread.sleep(6000)
        val acctbalance2 = ontSdk.nativevm().ong().queryBalanceOf(account.addressU160!!.toBase58())
        val reciebalance2 = ontSdk.nativevm().ong().queryBalanceOf(receiveAcc.addressU160!!.toBase58())
        Assert.assertTrue(acctbalance - acctbalance2 == 10L)
        Assert.assertTrue(reciebalance2 - reciebalance == 10L)
        val allowance3 = ontSdk.nativevm().ong().queryAllowance(account.addressU160!!.toBase58(), receiveAcc.addressU160!!.toBase58())
        Assert.assertTrue(allowance3 == allowance)
    }

    @Test
    @Throws(Exception::class)
    fun queryName() {
        val name = ontSdk.nativevm().ong().queryName()
        Assert.assertTrue(name.contains("ONG"))
        val symbol = ontSdk.nativevm().ong().querySymbol()
        Assert.assertTrue(symbol.contains("ONG"))
        val decimals = ontSdk.nativevm().ong().queryDecimals()
        Assert.assertTrue(decimals == 9L)
        val total = ontSdk.nativevm().ong().queryTotalSupply()
        Assert.assertFalse(total < 0)
    }
}