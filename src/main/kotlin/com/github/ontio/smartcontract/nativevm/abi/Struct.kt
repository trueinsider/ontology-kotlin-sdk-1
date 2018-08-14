package com.github.ontio.smartcontract.nativevm.abi

class Struct {
    var list: MutableList<Any> = mutableListOf()
    fun add(vararg objs: Any): Struct {
        for (i in objs.indices) {
            list.add(objs[i])
        }
        return this
    }
}
