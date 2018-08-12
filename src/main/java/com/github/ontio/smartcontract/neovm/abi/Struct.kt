package com.github.ontio.smartcontract.neovm.abi

class Struct {
    val list = mutableListOf<Any>()

    fun add(vararg objs: Any): Struct {
        for (i in objs.indices) {
            list.add(objs[i])
        }
        return this
    }
}