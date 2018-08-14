package com.github.neo.core


import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

/**
 *
 */
class Program(var parameter: ByteArray, var code: ByteArray) : Serializable {
    override fun deserialize(reader: BinaryReader) {
        parameter = reader.readVarBytes()    // sign data
        code = reader.readVarBytes()        // pubkey
    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(parameter)
        writer.writeVarBytes(code)
    }

    companion object {
        fun ProgramFromParams(sigData: Array<ByteArray>): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromParams(sigData)
        }

        fun ProgramFromPubKey(publicKey: ByteArray): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromPubKey(publicKey)
        }

        fun ProgramFromMultiPubKey(m: Int, vararg publicKeys: ByteArray): ByteArray {
            return com.github.ontio.core.program.Program.ProgramFromMultiPubKey(m, *publicKeys)
        }
    }

}
