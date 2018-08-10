package com.github.ontio.io

import com.github.ontio.common.Address
import com.github.ontio.common.Helper
import com.github.ontio.sdk.exception.SDKException
import org.junit.Test

import java.io.ByteArrayOutputStream
import java.io.IOException

import org.junit.Assert.*

class BinaryWriterTest {

    @Test
    @Throws(IOException::class)
    fun writeVarInt() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.writeVarInt(2544.toLong())
        binaryWriter.flush()
        assertNotNull(ms)
    }

    @Test
    @Throws(IOException::class)
    fun write() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.write("test".toByteArray())
        binaryWriter.flush()
        assertNotNull(ms)
    }

    @Test
    @Throws(IOException::class)
    fun writeInt() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.writeInt(1)
        binaryWriter.flush()
        assertNotNull(ms)
    }

    @Test
    @Throws(IOException::class, SDKException::class)
    fun writeSerializable() {
        //        ByteArrayOutputStream ms = new ByteArrayOutputStream();
        //        BinaryWriter binaryWriter = new BinaryWriter(ms);
        //        Address address = Address.decodeBase58("TA6nRD9DqGkE8xRJaB37bW2KQEz59ovKRH");
        //        binaryWriter.writeSerializable(address);
        //        binaryWriter.flush();
        //        assertNotNull(ms);
    }

    @Test
    @Throws(IOException::class)
    fun writeVarBytes() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.writeVarBytes("test".toByteArray())
        binaryWriter.flush()
        assertNotNull(ms)
    }

}