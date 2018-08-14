package com.github.neo.core.transaction


import com.github.neo.core.Program
import com.github.neo.core.TransactionAttribute
import com.github.neo.core.TransactionInput
import com.github.neo.core.TransactionOutput
import com.github.ontio.account.Account
import com.github.ontio.core.Inventory
import com.github.ontio.core.InventoryType
import com.github.ontio.core.transaction.TransactionType
import com.github.ontio.crypto.SignatureScheme
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.*
import java.util.stream.Stream
import kotlin.streams.toList

/**
 *
 */
abstract class TransactionNeo protected constructor(
        val type: TransactionType,
        var version: Byte,
        var attributes: Array<TransactionAttribute>,
        var inputs: Array<TransactionInput>,
        var outputs: Array<TransactionOutput>
) : Inventory() {
    /**
     *
     */
    var nonce: Long = 0
    /**
     *
     */
    var scripts = emptyArray<Program>()

    val allInputs: Stream<TransactionInput>
        get() = Arrays.stream(inputs)

    val allOutputs: Stream<TransactionOutput>
        get() = Arrays.stream(outputs)

    //[NonSerialized]
    private val _references: Map<TransactionInput, TransactionOutput>? = null

    override fun deserialize(reader: BinaryReader) {
        deserializeUnsigned(reader)
        try {
            scripts = reader.readSerializableArray(Program::class.java)
        } catch (ex: InstantiationException) {
            throw RuntimeException(ex)
        } catch (ex: IllegalAccessException) {
            throw RuntimeException(ex)
        }

        onDeserialized()
    }

    override fun deserializeUnsigned(reader: BinaryReader) {
        if (type.value() != reader.readByte()) { // type
            throw IOException()
        }
        deserializeUnsignedWithoutType(reader)
    }

    private fun deserializeUnsignedWithoutType(reader: BinaryReader) {
        try {
            version = reader.readByte()
            deserializeExclusiveData(reader)
            attributes = reader.readSerializableArray(TransactionAttribute::class.java)
            inputs = reader.readSerializableArray(TransactionInput::class.java)
            val inputs_all = allInputs.toList()
            for (i in 1 until inputs_all.size) {
                for (j in 0 until i) {
                    if (inputs_all[i].prevHash === inputs_all[j].prevHash && inputs_all[i].prevIndex == inputs_all[j].prevIndex) {
                        throw IOException()
                    }
                }
            }
            outputs = reader.readSerializableArray(TransactionOutput::class.java)
        } catch (ex: InstantiationException) {
            throw IOException(ex)
        } catch (ex: IllegalAccessException) {
            throw IOException(ex)
        }

    }

    protected open fun deserializeExclusiveData(reader: BinaryReader) {
    }

    override fun serialize(writer: BinaryWriter) {
        serializeUnsigned(writer)
        writer.writeSerializableArray(scripts)
    }

    override fun serializeUnsigned(writer: BinaryWriter) {
        writer.writeByte(type.value())
        writer.writeByte(version)
        serializeExclusiveData(writer)
        writer.writeSerializableArray(attributes)
        writer.writeSerializableArray(inputs)
        writer.writeSerializableArray(outputs)
    }

    protected open fun serializeExclusiveData(writer: BinaryWriter) {
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other == null) {
            return false
        }
        if (other !is TransactionNeo) {
            return false
        }
        return hash() == other.hash()
    }

    override fun hashCode(): Int {
        return hash().hashCode()
    }


    override fun inventoryType(): InventoryType {
        return InventoryType.TX
    }


    protected fun onDeserialized() {
    }


    /**
     *
     */
    override fun verify(): Boolean {
        return true
    }

    override fun sign(account: Account, scheme: SignatureScheme): ByteArray {
        val bys = account.generateSignature(hashData, scheme, null)
        val signature = ByteArray(64)
        System.arraycopy(bys, 1, signature, 0, 64)
        return signature
    }

    companion object {

        fun deserializeFrom(value: ByteArray, offset: Int = 0): TransactionNeo {
            ByteArrayInputStream(value, offset, value.size - offset).use { ms -> BinaryReader(ms).use { reader -> return deserializeFrom(reader) } }
        }

        fun deserializeFrom(reader: BinaryReader): TransactionNeo {
            try {
                val type = TransactionType.valueOf(reader.readByte())
                var typeName = "NEO.Core." + type.toString()
                if (type.toString() == "InvokeCode") {
                    typeName = "com.github.neo.core.transaction.InvocationTransaction"
                }
                val transaction = Class.forName(typeName).newInstance() as TransactionNeo
                transaction.deserializeUnsignedWithoutType(reader)
                transaction.scripts = reader.readSerializableArray(Program::class.java)
                return transaction
            } catch (ex: ClassNotFoundException) {
                throw IOException(ex)
            } catch (ex: InstantiationException) {
                throw IOException(ex)
            } catch (ex: IllegalAccessException) {
                throw IOException(ex)
            }

        }
    }
}
/**
 * 反序列化Transaction(static)
 */
