package com.github.ontio.io

import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayOutputStream

class BinaryWriterTest {

    @Test
    fun writeVarInt() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.writeVarInt(2544.toLong())
        binaryWriter.flush()
        assertNotNull(ms)
    }

    @Test
    fun write() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.write("test".toByteArray())
        binaryWriter.flush()
        assertNotNull(ms)
    }

    @Test
    fun writeInt() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.writeInt(1)
        binaryWriter.flush()
        assertNotNull(ms)
    }

    @Test
    fun writeVarBytes() {
        val ms = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(ms)
        binaryWriter.writeVarBytes("test".toByteArray())
        binaryWriter.flush()
        assertNotNull(ms)
    }

}