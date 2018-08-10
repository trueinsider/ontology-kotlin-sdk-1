package com.github.ontio.core.program

class ProgramInfo {
    var publicKey: Array<ByteArray>
    var m: Short = 0

    constructor() {}
    constructor(publicKey: Array<ByteArray>, m: Short) {
        this.publicKey = publicKey
        this.m = m
    }
}
