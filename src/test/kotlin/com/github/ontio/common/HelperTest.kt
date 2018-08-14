package com.github.ontio.common

import org.junit.Test

import java.math.BigInteger

import org.junit.Assert.*

class HelperTest {

    @Test
    fun bigInt2Bytes() {
        val bigInteger = BigInteger.valueOf(1000000000000L)
        val aa = Helper.toHexString(Helper.BigIntToNeoBytes(bigInteger))
        println(aa)
        val bb = Helper.BigIntFromNeoBytes(Helper.hexToBytes(aa))
        assertTrue(bigInteger == bb)

    }
}