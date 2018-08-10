package com.github.neo.core


import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException

/**
 *
 */
class Program(var parameter: ByteArray, var code: ByteArray) : Serializable {
    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {
        parameter = reader.readVarBytes()    // sign data
        code = reader.readVarBytes()        // pubkey
    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(parameter)
        writer.writeVarBytes(code)
    }

    companion object {
        @Throws(IOException::class)
        fun ProgramFromParams(sigData: Array<ByteArray>): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromParams(sigData)
        }

        @Throws(Exception::class)
        fun ProgramFromPubKey(publicKey: ByteArray): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromPubKey(publicKey)
        }

        @Throws(Exception::class)
        fun ProgramFromMultiPubKey(m: Int, vararg publicKeys: ByteArray): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromMultiPubKey(m, *publicKeys)
        }
    }

}
