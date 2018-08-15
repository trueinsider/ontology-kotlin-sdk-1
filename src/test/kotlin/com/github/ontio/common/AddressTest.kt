package com.github.ontio.common

import com.github.ontio.account.Account
import com.github.ontio.crypto.SignatureScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AddressTest {
    lateinit var account: Account

    @Before
    fun setUp() {
        account = Account(SignatureScheme.SHA256WITHECDSA)
    }

    @Test
    fun compareTo() {
        val account2 = Account(SignatureScheme.SHA256WITHECDSA)
        val res = account2.addressU160.compareTo(account.addressU160)
        assertNotNull(res)
    }

    @Test
    fun addressFromPubKey() {
        val address = Address.addressFromPubKey(account.serializePublicKey())
        assertEquals(address, account.addressU160)
    }

    @Test
    fun addressFromPubKey1() {
        val address = Address.addressFromPubKey(Helper.toHexString(account.serializePublicKey()))
        assertEquals(address, account.addressU160)
    }

    @Test
    fun addressFromMultiPubKeys() {
        val account2 = Account(SignatureScheme.SHA256WITHECDSA)
        val res = Address.addressFromMultiPubKeys(2, account.serializePublicKey(), account2.serializePublicKey())
        assertNotNull(res)
    }

    @Test
    fun toBase58() {
        val res = account.addressU160.toBase58()
        val addr = Address.decodeBase58(res)
        assertEquals(addr, account.addressU160)
    }

    @Test
    fun toScriptHash() {
        val addr = Address.toScriptHash(Helper.hexToBytes("12a67b"))
        assertNotNull(addr)
    }

}