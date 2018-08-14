package com.github.ontio.common

import com.github.ontio.account.Account
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.sdk.exception.SDKException
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class AddressTest {
    internal var account: Account
    @Before
    @Throws(Exception::class)
    fun setUp() {
        account = Account(SignatureScheme.SHA256WITHECDSA)
    }

    @Test
    @Throws(Exception::class)
    fun compareTo() {
        val account2 = Account(SignatureScheme.SHA256WITHECDSA)
        val res = account2.addressU160!!.compareTo(account.addressU160!!)
        assertNotNull(res)
    }

    @Test
    fun parse() {
        //        Address address = Address.parse(account.getAddressU160().toHexString());
        //        assertEquals(address,account.getAddressU160());
    }

    @Test
    fun addressFromPubKey() {
        val address = Address.addressFromPubKey(account.serializePublicKey())
        assertEquals(address, account.addressU160)
    }

    @Test
    fun addressFromPubKey1() {
        val address = Address.addressFromPubKey(Helper.toHexString(account.serializePublicKey()!!))
        assertEquals(address, account.addressU160)
    }

    @Test
    @Throws(Exception::class)
    fun addressFromMultiPubKeys() {
        val account2 = Account(SignatureScheme.SHA256WITHECDSA)
        val res = Address.addressFromMultiPubKeys(2, account.serializePublicKey(), account2.serializePublicKey())
        assertNotNull(res)
    }

    @Test
    @Throws(SDKException::class)
    fun toBase58() {
        val res = account.addressU160!!.toBase58()
        val addr = Address.decodeBase58(res)
        assertEquals(addr, account.addressU160)
    }

    @Test
    fun toScriptHash() {
        val addr = Address.toScriptHash(Helper.hexToBytes("12a67b"))
        assertNotNull(addr)
    }

}