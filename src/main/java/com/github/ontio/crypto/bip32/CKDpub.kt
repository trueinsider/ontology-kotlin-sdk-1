package com.github.ontio.crypto.bip32

interface CKDpub {
    fun cKDpub(index: Int): ExtendedPublicKey?
}