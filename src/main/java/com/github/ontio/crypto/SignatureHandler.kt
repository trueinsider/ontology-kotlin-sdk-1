package com.github.ontio.crypto

import com.github.ontio.common.ErrorCode
import com.github.ontio.sdk.exception.SDKException
import org.bouncycastle.asn1.*
import java.io.IOException
import java.math.BigInteger
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.AlgorithmParameterSpec
import java.util.*

class SignatureHandler constructor(private val type: KeyType, private val scheme: SignatureScheme) {
    private var ctx = when (this.type) {
        KeyType.ECDSA -> when (scheme) {
            SignatureScheme.SHA224WITHECDSA, SignatureScheme.SHA256WITHECDSA, SignatureScheme.SHA384WITHECDSA, SignatureScheme.SHA512WITHECDSA -> java.security.Signature.getInstance(scheme.toString(), "BC")
            else -> throw Exception(ErrorCode.UnsupportedSignatureScheme + scheme.toString())
        }
        KeyType.SM2 -> {
            if (scheme.compareTo(SignatureScheme.SM3WITHSM2) != 0) {
                throw SDKException(ErrorCode.UnsupportedSignatureScheme)
            }
            java.security.Signature.getInstance(scheme.toString(), "BC")
        }
        else -> throw SDKException(ErrorCode.UnknownKeyType)
    }

    fun generateSignature(priKey: PrivateKey, msg: ByteArray, param: AlgorithmParameterSpec?): ByteArray {
        if (param != null) {
            ctx.setParameter(param)
        }
        ctx.initSign(priKey)
        ctx.update(msg)
        var sig = ctx.sign()
        when (type) {
            KeyType.ECDSA, KeyType.SM2 -> sig = DSADERtoPlain(sig)
            else -> throw SDKException(ErrorCode.UnknownKeyType)
        }

        return sig
    }

    fun verifySignature(pubKey: PublicKey, msg: ByteArray, sig: ByteArray): Boolean {
        ctx.initVerify(pubKey)
        ctx.update(msg)
        val v = when (type) {
            KeyType.ECDSA, KeyType.SM2 -> DSAPlaintoDER(sig)
            else -> sig
        }
        return ctx!!.verify(v)
    }

    private fun DSADERtoPlain(sig: ByteArray): ByteArray {
        val seq = ASN1Primitive.fromByteArray(sig) as ASN1Sequence
        if (seq.size() != 2) {
            throw IOException(ErrorCode.MalformedSignature)
        } else if (!Arrays.equals(sig, seq.getEncoded("DER"))) {
            throw IOException(ErrorCode.MalformedSignature)
        }

        val r = ASN1Integer.getInstance(seq.getObjectAt(0)).value.toByteArray()
        val s = ASN1Integer.getInstance(seq.getObjectAt(1)).value.toByteArray()
        val ri = if (r[0].toInt() == 0) 1 else 0
        val rl = r.size - ri
        val si = if (s[0].toInt() == 0) 1 else 0
        val sl = s.size - si
        val res: ByteArray
        res = if (rl > sl) {
            ByteArray(rl * 2)
        } else {
            ByteArray(sl * 2)
        }
        System.arraycopy(r, ri, res, res.size / 2 - rl, rl)
        System.arraycopy(s, si, res, res.size - sl, sl)
        return res
    }

    private fun DSAPlaintoDER(sig: ByteArray): ByteArray {
        val r = BigInteger(1, Arrays.copyOfRange(sig, 0, sig.size / 2))
        val s = BigInteger(1, Arrays.copyOfRange(sig, sig.size / 2, sig.size))

        val var3 = ASN1EncodableVector()
        var3.add(ASN1Integer(r))
        var3.add(ASN1Integer(s))
        return DERSequence(var3).getEncoded("DER")
    }
}
