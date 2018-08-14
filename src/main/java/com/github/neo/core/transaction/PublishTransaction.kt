package com.github.neo.core.transaction


import com.github.neo.core.ContractParameterType
import com.github.neo.core.TransactionAttribute
import com.github.neo.core.TransactionInput
import com.github.neo.core.TransactionOutput
import com.github.ontio.common.Address
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import java.util.*

class PublishTransaction(
        version: Byte,
        attributes: Array<TransactionAttribute>,
        inputs: Array<TransactionInput>,
        outputs: Array<TransactionOutput>,
        var script: ByteArray,
        var parameterList: Array<ContractParameterType>,
        var returnType: ContractParameterType,
        var needStorage: Boolean,
        var name: String,
        var codeVersion: String,
        var author: String,
        var email: String,
        var description: String
) : TransactionNeo(TransactionType.DeployCode, version, attributes, inputs, outputs) {
    override val addressU160ForVerifying: Array<Address>?
        get() = null

    override fun deserializeExclusiveData(reader: BinaryReader) {
        script = reader.readVarBytes()
        val param = reader.readVarBytes()
        parameterList = toEnum(param)
        returnType = toEnum(reader.readByte())
        needStorage = reader.readBoolean()
        name = String(reader.readVarBytes(252))
        codeVersion = String(reader.readVarBytes(252))
        author = String(reader.readVarBytes(252))
        email = String(reader.readVarBytes(252))
        description = String(reader.readVarBytes(65535))
    }

    override fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(script)
        writer.writeVarBytes(toByte(parameterList))
        writer.writeByte(returnType.ordinal.toByte())
        writer.writeBoolean(needStorage)
        writer.writeVarString(name)
        writer.writeVarString(codeVersion)
        writer.writeVarString(author)
        writer.writeVarString(email)
        writer.writeVarString(description)
    }

    private fun toEnum(bt: Byte): ContractParameterType {
        return Arrays.stream(ContractParameterType.values()).filter { p -> p.ordinal == bt.toInt() }.findAny().get()
    }

    private fun toEnum(bt: ByteArray) = bt.map { toEnum(it) }.toTypedArray()

    private fun toByte(types: Array<ContractParameterType>?): ByteArray {
        if (types == null) {
            return ByteArray(0)
        }
        val len = types.size
        val bt = ByteArray(len)
        for (i in 0 until len) {
            bt[i] = types[i].ordinal.toByte()
        }
        return bt
    }
}
