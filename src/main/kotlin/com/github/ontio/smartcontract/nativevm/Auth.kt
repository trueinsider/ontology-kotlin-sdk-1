package com.github.ontio.smartcontract.nativevm

import com.alibaba.fastjson.JSONObject
import com.github.ontio.OntSdk.addSign
import com.github.ontio.OntSdk.connect
import com.github.ontio.OntSdk.signTx
import com.github.ontio.account.Account
import com.github.ontio.common.Address
import com.github.ontio.common.ErrorCode
import com.github.ontio.common.Helper
import com.github.ontio.core.transaction.Transaction
import com.github.ontio.io.BinaryReader
import com.github.ontio.io.BinaryWriter
import com.github.ontio.io.Serializable
import com.github.ontio.sdk.exception.SDKException
import com.github.ontio.smartcontract.Vm
import com.github.ontio.smartcontract.nativevm.abi.NativeBuildParams
import com.github.ontio.smartcontract.nativevm.abi.Struct
import java.io.ByteArrayOutputStream

object Auth {
    const val contractAddress = "0000000000000000000000000000000000000006"

    fun sendInit(adminOntId: String, password: String, salt: ByteArray, contractAddr: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        if (adminOntId.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        val bos = ByteArrayOutputStream()
        val bw = BinaryWriter(bos)
        bw.writeVarBytes(adminOntId.toByteArray())
        val tx = Vm.makeInvokeCodeTransaction(contractAddr, "initContractAdmin", byteArrayOf(), payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, adminOntId, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        if (!b) {
            throw SDKException(ErrorCode.SendRawTxError)
        }
        return tx.hash().toHexString()
    }

    /**
     *
     * @param adminOntId
     * @param password
     * @param contractAddr
     * @param newAdminOntID
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun sendTransfer(adminOntId: String, password: String, salt: ByteArray, keyNo: Long, contractAddr: String, newAdminOntID: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String {
        if (adminOntId.isEmpty() || contractAddr.isEmpty() || newAdminOntID.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeTransfer(adminOntId, contractAddr, newAdminOntID, keyNo, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, adminOntId, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        if (!b) {
            throw SDKException(ErrorCode.SendRawTxError)
        }
        return tx.hash().toHexString()
    }

    /**
     *
     * @param adminOntID
     * @param contractAddr
     * @param newAdminOntID
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeTransfer(adminOntID: String, contractAddr: String, newAdminOntID: String, keyNo: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (adminOntID.isEmpty() || contractAddr.isEmpty() || newAdminOntID.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }

        val list = mutableListOf<Any>()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), newAdminOntID.toByteArray(), keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "transfer", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param ontid
     * @param password
     * @param contractAddr
     * @param funcName
     * @param keyNo
     * @return
     * @throws Exception
     */
    fun verifyToken(ontid: String, password: String, salt: ByteArray, keyNo: Long, contractAddr: String, funcName: String): String {
        if (ontid.isEmpty() || password.isEmpty() || contractAddr.isEmpty() || funcName.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0) {
            throw SDKException(ErrorCode.ParamErr("key or gaslimit or gas price should not be less than 0"))
        }
        val tx = makeVerifyToken(ontid, contractAddr, funcName, keyNo)
        signTx(tx, ontid, password, salt)
        val obj = connect!!.sendRawTransactionPreExec(tx.toHexString())
                ?: throw SDKException(ErrorCode.OtherError("sendRawTransaction PreExec error: "))
        return (obj as JSONObject).getString("Result")
    }

    /**
     *
     * @param ontid
     * @param contractAddr
     * @param funcName
     * @param keyNo
     * @return
     * @throws SDKException
     */
    fun makeVerifyToken(ontid: String, contractAddr: String, funcName: String, keyNo: Long): Transaction {
        if (ontid.isEmpty() || contractAddr.isEmpty() || funcName.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0) {
            throw SDKException(ErrorCode.ParamErr("key or gaslimit or gas price should not be less than 0"))
        }
        val list = mutableListOf<Any>()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), ontid.toByteArray(), funcName.toByteArray(), keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "verifyToken", arg, null, 0, 0)
    }

    /**
     *
     * @param adminOntID
     * @param password
     * @param contractAddr
     * @param role
     * @param funcName
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun assignFuncsToRole(adminOntID: String, password: String, salt: ByteArray, keyNo: Long, contractAddr: String, role: String, funcName: Array<String>, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (adminOntID.isEmpty() || contractAddr.isEmpty() || role.isEmpty() || funcName.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gas price should not be less than 0"))
        }
        val tx = makeAssignFuncsToRole(adminOntID, contractAddr, role, funcName, keyNo, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, adminOntID, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param adminOntID
     * @param contractAddr
     * @param role
     * @param funcName
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeAssignFuncsToRole(adminOntID: String, contractAddr: String, role: String, funcName: Array<String>, keyNo: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (adminOntID.isEmpty() || contractAddr.isEmpty() || role.isEmpty() || funcName.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gas price should not be less than 0"))
        }

        val list = mutableListOf<Any>()
        val struct = Struct()
        struct.add(Helper.hexToBytes(contractAddr), adminOntID.toByteArray(), role.toByteArray())
        struct.add(funcName.size)
        for (i in funcName.indices) {
            struct.add(funcName[i])
        }
        struct.add(keyNo)
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "assignFuncsToRole", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param adminOntId
     * @param password
     * @param contractAddr
     * @param role
     * @param ontIDs
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun assignOntIdsToRole(adminOntId: String, password: String, salt: ByteArray, keyNo: Long, contractAddr: String, role: String, ontIDs: Array<String>, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (adminOntId.isEmpty() || password.isEmpty() || contractAddr.isEmpty() || role.isEmpty() || ontIDs.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeAssignOntIDsToRole(adminOntId, contractAddr, role, ontIDs, keyNo, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, adminOntId, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param adminOntId
     * @param contractAddr
     * @param role
     * @param ontIDs
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeAssignOntIDsToRole(adminOntId: String, contractAddr: String, role: String, ontIDs: Array<String>, keyNo: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (adminOntId.isEmpty() || contractAddr.isEmpty() || role.isEmpty() || ontIDs.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val ontId = ontIDs.map { it.toByteArray() }.toTypedArray()
        val list = mutableListOf<Any>()
        val struct = Struct()
        struct.add(Helper.hexToBytes(contractAddr), adminOntId.toByteArray(), role.toByteArray())
        struct.add(ontId.size)
        for (i in ontId.indices) {
            struct.add(ontId[i])
        }
        struct.add(keyNo)
        list.add(struct)
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "assignOntIDsToRole", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param ontid
     * @param password
     * @param contractAddr
     * @param toOntId
     * @param role
     * @param period
     * @param level
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun delegate(ontid: String, password: String, salt: ByteArray, keyNo: Long, contractAddr: String, toOntId: String, role: String, period: Long, level: Long, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (ontid.isEmpty() || password.isEmpty() || contractAddr.isEmpty() || toOntId.isEmpty() || role.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (period < 0 || level < 0 || keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("period level key gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeDelegate(ontid, contractAddr, toOntId, role, period, level, keyNo, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, ontid, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param ontid
     * @param contractAddr
     * @param toAddr
     * @param role
     * @param period
     * @param level
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeDelegate(ontid: String, contractAddr: String, toAddr: String, role: String, period: Long, level: Long, keyNo: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || contractAddr.isEmpty() || toAddr.isEmpty() || role.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (period < 0 || level < 0 || keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("period level keyNo gaslimit or gasprice should not be less than 0"))
        }

        val list = mutableListOf<Any>()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), ontid.toByteArray(), toAddr.toByteArray(), role.toByteArray(), period, level, keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "delegate", arg, payer, gaslimit, gasprice)
    }

    /**
     *
     * @param initiatorOntid
     * @param password
     * @param contractAddr
     * @param delegate
     * @param role
     * @param keyNo
     * @param payerAcct
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws Exception
     */
    fun withdraw(initiatorOntid: String, password: String, salt: ByteArray, keyNo: Long, contractAddr: String, delegate: String, role: String, payerAcct: Account, gaslimit: Long, gasprice: Long): String? {
        if (initiatorOntid.isEmpty() || password.isEmpty() || contractAddr.isEmpty() || role.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("keyNo or gaslimit or gasprice should not be less than 0"))
        }
        val tx = makeWithDraw(initiatorOntid, contractAddr, delegate, role, keyNo, payerAcct.addressU160.toBase58(), gaslimit, gasprice)
        signTx(tx, initiatorOntid, password, salt)
        addSign(tx, payerAcct)
        val b = connect!!.sendRawTransaction(tx.toHexString())
        return if (b) {
            tx.hash().toHexString()
        } else null
    }

    /**
     *
     * @param ontid
     * @param contractAddr
     * @param delegate
     * @param role
     * @param keyNo
     * @param payer
     * @param gaslimit
     * @param gasprice
     * @return
     * @throws SDKException
     */
    fun makeWithDraw(ontid: String, contractAddr: String, delegate: String, role: String, keyNo: Long, payer: String, gaslimit: Long, gasprice: Long): Transaction {
        if (ontid.isEmpty() || contractAddr.isEmpty() || role.isEmpty() || payer.isEmpty()) {
            throw SDKException(ErrorCode.ParamErr("parameter should not be null"))
        }
        if (keyNo < 0 || gaslimit < 0 || gasprice < 0) {
            throw SDKException(ErrorCode.ParamErr("key gaslimit or gasprice should not be less than 0"))
        }
        val list = mutableListOf<Any>()
        list.add(Struct().add(Helper.hexToBytes(contractAddr), ontid.toByteArray(), delegate.toByteArray(), role.toByteArray(), keyNo))
        val arg = NativeBuildParams.createCodeParamsScript(list)

        return Vm.buildNativeParams(Address(Helper.hexToBytes(contractAddress)), "withdraw", arg, payer, gaslimit, gasprice)
    }

    fun queryAuth(contractAddr: String, ontid: String): Any {
        return connect!!.getStorage(contractAddr, contractAddr + Helper.toHexString("role".toByteArray()) + Helper.toHexString(ontid.toByteArray()))
    }
}

internal class TransferParam(var contractAddr: ByteArray, var newAdminOntID: ByteArray, var KeyNo: Long) : Serializable {

    override fun deserialize(reader: BinaryReader) {
        this.contractAddr = reader.readVarBytes()
        this.newAdminOntID = reader.readVarBytes()
        KeyNo = reader.readVarInt()
    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.newAdminOntID)
        writer.writeVarInt(KeyNo)
    }
}

internal class VerifyTokenParam(var contractAddr: ByteArray, var caller: ByteArray, var fn: ByteArray, var keyNo: Long) : Serializable {

    override fun deserialize(reader: BinaryReader) {

    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.caller)
        writer.writeVarBytes(this.fn)
        writer.writeVarInt(keyNo)
    }
}

internal class FuncsToRoleParam(var contractAddr: ByteArray, var adminOntID: ByteArray, var role: ByteArray, var funcNames: Array<String>, var keyNo: Long) : Serializable {

    override fun deserialize(reader: BinaryReader) {
        this.contractAddr = reader.readVarBytes()
        this.adminOntID = reader.readVarBytes()
        this.role = reader.readVarBytes()
        this.funcNames = Array(reader.readVarInt().toInt()) { reader.readVarString() }
        this.keyNo = reader.readVarInt()
    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.adminOntID)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.funcNames.size.toLong())
        for (name in this.funcNames) {
            writer.writeVarString(name)
        }
        writer.writeVarInt(this.keyNo)
    }
}

internal class OntIDsToRoleParam(var contractAddr: ByteArray, var adminOntID: ByteArray, var role: ByteArray, var persons: Array<ByteArray>, var keyNo: Long) : Serializable {

    override fun deserialize(reader: BinaryReader) {
        this.contractAddr = reader.readVarBytes()
        this.adminOntID = reader.readVarBytes()
        this.role = reader.readVarBytes()
        this.persons = Array(reader.readVarInt().toInt()) { reader.readVarBytes() }
        this.keyNo = reader.readVarInt()
    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.adminOntID)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.persons.size.toLong())
        for (p in this.persons) {
            writer.writeVarBytes(p)
        }
        writer.writeVarInt(this.keyNo)
    }
}

internal class DelegateParam(var contractAddr: ByteArray, var from: ByteArray, var to: ByteArray, var role: ByteArray, var period: Long, var level: Long, var keyNo: Long) : Serializable {

    override fun deserialize(reader: BinaryReader) {

    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.from)
        writer.writeVarBytes(this.to)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.period)
        writer.writeVarInt(this.level)
        writer.writeVarInt(this.keyNo)
    }
}

internal class AuthWithdrawParam(var contractAddr: ByteArray, var initiator: ByteArray, var delegate: ByteArray, var role: ByteArray, var keyNo: Long) : Serializable {
    override fun deserialize(reader: BinaryReader) {

    }

    override fun serialize(writer: BinaryWriter) {
        writer.writeVarBytes(this.contractAddr)
        writer.writeVarBytes(this.initiator)
        writer.writeVarBytes(this.delegate)
        writer.writeVarBytes(this.role)
        writer.writeVarInt(this.keyNo)
    }
}


