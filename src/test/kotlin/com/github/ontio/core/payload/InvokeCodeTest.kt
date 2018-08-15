package com.github.ontio.core.payload

import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class InvokeCodeTest {

    @Test
    fun serializeExclusiveData() {
        val invokeCode = InvokeCode("test".toByteArray())

        val byteArrayOutputStream = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(byteArrayOutputStream)
        invokeCode.serializeExclusiveData(binaryWriter)

        assertNotNull(byteArrayOutputStream)

        InvokeCode.deserializeFrom(BinaryReader(ByteArrayInputStream(byteArrayOutputStream.toByteArray())))
    }
}