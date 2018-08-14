package com.github.ontio.smartcontract

import com.github.ontio.OntSdk
import com.github.ontio.common.Address
import com.github.ontio.sdk.exception.SDKException
import org.junit.Before
import org.junit.Test

import org.junit.Assert.*

class VmTest {

    internal var ontSdk: OntSdk
    internal var vm: Vm
    @Before
    fun setUp() {
        ontSdk = OntSdk.getInstance()
        vm = Vm(ontSdk)

    }

    @Test
    @Throws(SDKException::class)
    fun buildNativeParams() {
        //        Address addr = Address.decodeBase58("TA9MXtwAcXkUMuujJh2iNRaWoXrvzfrmZb");
        //        vm.buildNativeParams(addr,"init","1".getBytes(),null,0,0);
    }
}