package com.github.ontio.crypto

import com.github.ontio.account.Account
import com.github.ontio.common.ErrorCode
import com.github.ontio.crypto.bip32.ExtendedPrivateKey
import com.github.ontio.sdk.exception.SDKException
import io.github.novacrypto.bip32.networks.Bitcoin
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import org.bouncycastle.crypto.generators.SCrypt
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object MnemonicCode {
    fun generateMnemonicCodesStr(): String {
        val sb = StringBuilder()
        val entropy = ByteArray(Words.TWELVE.byteLength())
        SecureRandom().nextBytes(entropy)
        MnemonicGenerator(English.INSTANCE).createMnemonic(entropy) { string -> sb.append(string) }
        SecureRandom().nextBytes(entropy)
        return sb.toString()
    }

    fun getSeedFromMnemonicCodesStr(mnemonicCodesStr: String): ByteArray {
        val mnemonicCodesArray = mnemonicCodesStr.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return SeedCalculator()
                .withWordsFromWordList(English.INSTANCE)
                .calculateSeed(Arrays.asList(*mnemonicCodesArray), "")
    }

    fun getPrikeyFromMnemonicCodesStrBip44(mnemonicCodesStr: String): ByteArray {
        val seed = MnemonicCode.getSeedFromMnemonicCodesStr(mnemonicCodesStr)
        val key = ExtendedPrivateKey.fromSeed(seed, "Nist256p1 seed".toByteArray(charset("UTF-8")), Bitcoin.MAIN_NET)
        val child = key.derive("m/44'/1024'/0'/0/0")
        val p = child.extendedKeyByteArray()
        val tmp = ByteArray(32)
        System.arraycopy(p, 46, tmp, 0, 32)
        return tmp
    }

    fun encryptMnemonicCodesStr(mnemonicCodesStr: String, password: String, address: String): String {
        val N = 4096
        val r = 8
        val p = 8
        val dkLen = 64

        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val salt = Arrays.copyOfRange(addresshashTmp, 0, 4)
        val derivedkey = SCrypt.generate(password.toByteArray(StandardCharsets.UTF_8), salt, N, r, p, dkLen)

        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(derivedkey, 0, iv, 0, 16)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)

        val skeySpec = SecretKeySpec(derivedhalf2, "AES")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, IvParameterSpec(iv))
        val encryptedkey = cipher.doFinal(mnemonicCodesStr.toByteArray())

        return String(Base64.getEncoder().encode(encryptedkey))
    }

    fun decryptMnemonicCodesStr(encryptedMnemonicCodesStr: String, password: String?, address: String): String {
        val encryptedkey = Base64.getDecoder().decode(encryptedMnemonicCodesStr)

        val N = 4096
        val r = 8
        val p = 8
        val dkLen = 64

        val addresshashTmp = Digest.sha256(Digest.sha256(address.toByteArray()))
        val salt = Arrays.copyOfRange(addresshashTmp, 0, 4)

        val derivedkey = SCrypt.generate(password!!.toByteArray(StandardCharsets.UTF_8), salt, N, r, p, dkLen)
        val derivedhalf2 = ByteArray(32)
        val iv = ByteArray(16)
        System.arraycopy(derivedkey, 0, iv, 0, 16)
        System.arraycopy(derivedkey, 32, derivedhalf2, 0, 32)

        val skeySpec = SecretKeySpec(derivedhalf2, "AES")
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, skeySpec, IvParameterSpec(iv))
        val rawMns = cipher.doFinal(encryptedkey)
        val mnemonicCodesStr = String(rawMns)
        println(mnemonicCodesStr)
        val rawkey = MnemonicCode.getPrikeyFromMnemonicCodesStrBip44(mnemonicCodesStr)
        val addressNew = Account(rawkey, SignatureScheme.SHA256WITHECDSA).addressU160.toBase58()
        val addressNewHashTemp = Digest.sha256(Digest.sha256(addressNew.toByteArray()))
        val saltNew = Arrays.copyOfRange(addressNewHashTemp, 0, 4)
        if (!Arrays.equals(saltNew, salt)) {
            throw SDKException(ErrorCode.EncryptedPriKeyError)
        }
        return mnemonicCodesStr
    }

    fun getChars(bytes: ByteArray): CharArray {
        val chars = CharArray(bytes.size)
        for (i in bytes.indices) {
            chars[i] = bytes[i].toChar()
        }
        return chars
    }
}
