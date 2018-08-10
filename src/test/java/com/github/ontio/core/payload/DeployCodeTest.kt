package com.github.ontio.core.payload

import com.github.ontio.core.VmType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import org.junit.Test

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

import org.junit.Assert.*

class DeployCodeTest {

    @Test
    @Throws(IOException::class)
    fun serializeExclusiveData() {
        val deployCode = DeployCode()
        deployCode.version = "1"
        deployCode.author = "sss"
        deployCode.name = "sss"
        deployCode.code = "test".toByteArray()
        deployCode.description = "test"
        deployCode.email = "test"
        deployCode.needStorage = true

        val byteArrayOutputStream = ByteArrayOutputStream()
        val binaryWriter = BinaryWriter(byteArrayOutputStream)
        deployCode.serializeExclusiveData(binaryWriter)

        val selr = byteArrayOutputStream.toByteArray()

        val deployCode1 = DeployCode()
        deployCode1.deserializeExclusiveData(BinaryReader(ByteArrayInputStream(selr)))
        assertEquals(deployCode.version, deployCode1.version)
    }
}