package com.github.neo.core


import com.github.ontio.common.UInt256
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

/**
 *
 */
class TransactionInput : Serializable {
    /**
     *
     */
    lateinit var prevHash: UInt256
    /**
     *
     */
    var prevIndex: Short = 0

    constructor()

    constructor(prevHash: UInt256, prevIndex: Int) {
        this.prevHash = prevHash
        this.prevIndex = prevIndex.toShort()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (null == other) {
            return false
        }
        if (other !is TransactionInput) {
            return false
        }
        return prevHash == other.prevHash && prevIndex == other.prevIndex
    }

    override fun hashCode(): Int {
        return prevHash.hashCode() + prevIndex
    }

    /**
     *
     */
    override fun deserialize(reader: BinaryReader) {
        try {
            prevHash = reader.readSerializable(UInt256::class.java)
            prevIndex = reader.readShort()
        } catch (e: InstantiationException) {
        } catch (e: IllegalAccessException) {
        }

    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeSerializable(prevHash)
        writer.writeShort(prevIndex)
    }


    override fun toString(): String {
        return ("TransactionInput [prevHash=" + prevHash + ", prevIndex="
                + prevIndex + "]")
    }
}
