package com.github.ontio.core.payload

import com.github.ontio.core.VmType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

import org.junit.Assert.*

class InvokeCodeTest {

    @Test
    @Throws(IOException::class)
    fun serializeExclusiveData() {
        val invokeCode = InvokeCode()
        invokeCode.code = "test".toByteArray()

        val byteArrayOutputStream = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(byteArrayOutputStream)
        invokeCode.serializeExclusiveData(binaryWriter)

        assertNotNull(byteArrayOutputStream)

        val invokeCode1 = InvokeCode()
        invokeCode1.deserializeExclusiveData(BinaryReader(ByteArrayInputStream(byteArrayOutputStream.toByteArray())))
    }
}