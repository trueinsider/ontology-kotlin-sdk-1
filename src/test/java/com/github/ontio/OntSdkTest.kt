package com.github.ontio

import org.junit.After
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class OntSdkTest {
    private var ontSdk: OntSdk? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        ontSdk = OntSdk.getInstance()
    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    fun getInstance() {
        val ontSdk = OntSdk.getInstance()
        assertNotNull(ontSdk)
        assertSame(ontSdk, this.ontSdk)
    }

    companion object {
        //    public static String URL = "http://polaris1.ont.io:20334";
        //    public static String URL = "http://139.219.129.26:20334";
        var URL = "http://127.0.0.1:20334"

        var PRIVATEKEY3 = "c19f16785b8f3543bbaf5e1dbb5d398dfa6c85aaad54fc9d71203ce83e505c07"//有钱的账号的私钥
        var PRIVATEKEY2 = "f1442d5e7f4e2061ff9a6884d6d05212e2aa0f6a6284f0a28ae82a29cdb3d656"//有钱的账号的私钥
        var PRIVATEKEY = "75de8489fcb2dcaf2ef3cd607feffde18789de7da129b5e97c81e001793cb7cf"

        var PASSWORD = "111111"//有钱账号的密码
    }
}