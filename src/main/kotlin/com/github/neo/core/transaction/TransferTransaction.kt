package com.github.neo.core.transaction

import com.github.neo.core.TransactionAttribute
import com.github.neo.core.TransactionInput
import com.github.neo.core.TransactionOutput
import com.github.ontio.common.Address
import com.github.ontio.core.transaction.TransactionType

class TransferTransaction(
        version: Byte,
        attributes: Array<TransactionAttribute>,
        inputs: Array<TransactionInput>,
        outputs: Array<TransactionOutput>
) : TransactionNeo(TransactionType.TransferTransaction, version, attributes, inputs, outputs) {
    override val addressU160ForVerifying: Array<Address>?
        get() = null
}
