package com.github.ontio.core.scripts

import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigInteger

class ScriptBuilderTest {
    lateinit var scriptBuilder: ScriptBuilder

    @Before
    fun setUp() {
        scriptBuilder = ScriptBuilder()
    }

    @Test
    fun add() {
        val sb = scriptBuilder.add("test".toByteArray())
        assertNotNull(sb)

    }

    @Test
    fun push() {
        val sb = scriptBuilder.emitPushBool(true)
        assertNotNull(sb)
        assertNotNull(scriptBuilder.emitPushByteArray("test".toByteArray()))
        assertNotNull(scriptBuilder.emitPushInteger(BigInteger("11")))
    }

    @Test
    fun pushPack() {
        assertNotNull(scriptBuilder.pushPack())

    }
}