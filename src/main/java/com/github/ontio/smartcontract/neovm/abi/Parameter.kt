/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.smartcontract.neovm.abi

import com.alibaba.fastjson.JSON
import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException

/**
 *
 */
class Parameter {
    var name: String? = null
    var type: String? = null
    var value: String? = null

    fun setValue(value: Any?): Boolean {
        when {
            value == null -> this.value = null
            "ByteArray" == type -> {
                val tmp = value as ByteArray?
                this.value = JSON.toJSONString(tmp)
            }
            "String" == type -> this.value = value as String?
            "Boolean" == type -> {
                val tmp = value as Boolean
                this.value = JSON.toJSONString(tmp)
            }
            "Integer" == type -> {
                val tmp = value as Long
                this.value = JSON.toJSONString(tmp)
            }
            "Array" == type -> {
                val tmp = value as List<*>?
                this.value = JSON.toJSONString(tmp)
            }
            "InteropInterface" == type -> {
                val tmp = value as Any?
                this.value = JSON.toJSONString(tmp)
            }
            "Void" == type -> {
            }
            "Map" == type -> {
                val tmp = value as Map<*, *>?
                this.value = JSON.toJSONString(tmp)
            }
            "Struct" == type -> {
                val tmp = value as Struct?
                this.value = JSON.toJSONString(tmp)
            }
            else -> throw SDKException(ErrorCode.TypeError)
        }
        return true
    }

    override fun toString(): String {
        return JSON.toJSONString(this)
    }
}
