package com.github.ontio.account

import com.github.ontio.common.Helper
import com.github.ontio.crypto.SignatureScheme
import org.junit.Test

import org.junit.Assert.*

class AccountTest {


    @Test
    @Throws(Exception::class)
    fun generateSignature() {
        val account = Account(SignatureScheme.SHA256WITHECDSA)
        val signature = account.generateSignature("hello".toByteArray(), SignatureScheme.SHA256WITHECDSA, null)
        val b = account.verifySignature("hello".toByteArray(), signature)
        assertTrue(b)
    }

    @Test
    @Throws(Exception::class)
    fun serializePublicKey() {
        val account = Account(SignatureScheme.SHA256WITHECDSA)
        val publickey = account.serializePublicKey()
        assertNotNull(publickey)
    }

    @Test
    @Throws(Exception::class)
    fun serializePrivateKey() {
        val account = Account(SignatureScheme.SHA256WITHECDSA)
        val privateKey = account.serializePrivateKey()
        assertNotNull(privateKey)
    }

    @Test
    @Throws(Exception::class)
    fun compareTo() {
        val account1 = Account(SignatureScheme.SHA256WITHECDSA)
        val account2 = Account(SignatureScheme.SHA256WITHECDSA)
        val res = account1.compareTo(account2)
        assertNotNull(res)
    }

    @Test
    @Throws(Exception::class)
    fun exportCtrEncryptedPrikey1() {
        val account = Account(SignatureScheme.SHA256WITHECDSA)
        val encruPri = account.exportCtrEncryptedPrikey("111111", 16384)
        val privateKey = Account.getCtrDecodedPrivateKey(encruPri, "111111", account.addressU160!!.toBase58(), 16384, SignatureScheme.SHA256WITHECDSA)
        assertEquals(privateKey, Helper.toHexString(account.serializePrivateKey()))
    }

}