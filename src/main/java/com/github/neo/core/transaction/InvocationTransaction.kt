package com.github.neo.core.transaction


import com.github.neo.core.TransactionAttribute
import com.github.neo.core.TransactionInput
import com.github.neo.core.TransactionOutput
import com.github.ontio.common.Address
import com.github.ontio.common.Fixed8
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter

class InvocationTransaction(
        version: Byte,
        attributes: Array<TransactionAttribute>,
        inputs: Array<TransactionInput>,
        outputs: Array<TransactionOutput>,
        var script: ByteArray,
        var gas: Fixed8
) : TransactionNeo(TransactionType.InvokeCode, version, attributes, inputs, outputs) {
    override val addressU160ForVerifying: Array<Address>?
        get() = null

    override fun deserializeExclusiveData(reader: BinaryReader) {
        script = reader.readVarBytes()
        gas = reader.readSerializable(Fixed8::class.java)
    }

    override fun serializeExclusiveData(writer: BinaryWriter) {
        writer.writeVarBytes(script)
        writer.writeSerializable(gas)
    }
}
