package com.github.ontio.core.globalparams

import com.github.ontio.common.Helper
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable

import java.io.IOException
import java.math.BigInteger

class Params(var params: Array<Param>) : Serializable {

    @Throws(IOException::class)
    override fun deserialize(reader: BinaryReader) {

    }

    @Throws(IOException::class)
    override fun serialize(writer: BinaryWriter) {
        val l = params.size.toLong()
        val aa = Helper.BigIntToNeoBytes(BigInteger.valueOf(l))
        val bb = Helper.toHexString(aa)
        writer.writeVarBytes(aa)
        for (i in params.indices) {
            writer.writeVarString(params[i].key)
            writer.writeVarString(params[i].value)
        }
    }
}
