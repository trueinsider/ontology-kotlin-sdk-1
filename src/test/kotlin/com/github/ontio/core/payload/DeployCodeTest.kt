package com.github.ontio.core.payload

import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class DeployCodeTest {

    @Test
    fun serializeExclusiveData() {
        val deployCode = DeployCode(
                "test".toByteArray(),
                true,
                "sss",
                "1",
                "sss",
                "test",
                "test"
        )

        val byteArrayOutputStream = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(byteArrayOutputStream)
        deployCode.serializeExclusiveData(binaryWriter)

        val selr = byteArrayOutputStream.toByteArray()

        val deployCode1 = DeployCode.deserializeFrom(BinaryReader(ByteArrayInputStream(selr)))
        assertEquals(deployCode.versionString, deployCode1.versionString)
    }
}