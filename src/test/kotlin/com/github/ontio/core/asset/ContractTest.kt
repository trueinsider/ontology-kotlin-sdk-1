package com.github.ontio.core.asset

import com.github.ontio.common.Address
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.sdk.exception.SDKException
import org.junit.Before
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.IOException

import org.junit.Assert.*

class ContractTest {

    internal var address: Address? = null

    @Before
    @Throws(SDKException::class)
    fun setUp() {
        //        address = Address.decodeBase58("TA6nRD9DqGkE8xRJaB37bW2KQEz59ovKRH");
    }

    @Test
    @Throws(IOException::class)
    fun serialize() {
        //        Contract contract = new Contract((byte)1,address,"test","t".getBytes());
        //        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        //        BinaryWriter binaryWriter = new BinaryWriter(bs);
        //        contract.serialize(binaryWriter);
        //        binaryWriter.flush();
        //        byte[] seril = bs.toByteArray();
        //        assertNotNull(seril);
        //
        //        Contract contract1 = new Contract((byte)1,address,"test2","t2".getBytes());
        //        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(seril);
        //        BinaryReader binaryReader = new BinaryReader(byteArrayInputStream);
        //        contract1.deserialize(binaryReader);
        //        assertNotNull(binaryReader);

    }
}