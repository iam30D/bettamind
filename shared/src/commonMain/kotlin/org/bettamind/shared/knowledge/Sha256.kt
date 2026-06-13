package org.bettamind.shared.knowledge

internal fun ByteArray.sha256Hex(): String {
    val hash = sha256()
    val hex = CharArray(hash.size * 2)
    val alphabet = "0123456789abcdef"

    hash.forEachIndexed { index, byte ->
        val value = byte.toInt() and 0xff
        hex[index * 2] = alphabet[value ushr 4]
        hex[index * 2 + 1] = alphabet[value and 0x0f]
    }

    return hex.concatToString()
}

private fun ByteArray.sha256(): ByteArray {
    var h0 = 0x6a09e667.toInt()
    var h1 = 0xbb67ae85.toInt()
    var h2 = 0x3c6ef372.toInt()
    var h3 = 0xa54ff53a.toInt()
    var h4 = 0x510e527f.toInt()
    var h5 = 0x9b05688c.toInt()
    var h6 = 0x1f83d9ab.toInt()
    var h7 = 0x5be0cd19.toInt()

    val padded = sha256PaddedMessage()
    val words = IntArray(64)

    for (chunkStart in padded.indices step 64) {
        for (index in 0 until 16) {
            val offset = chunkStart + index * 4
            words[index] =
                ((padded[offset].toInt() and 0xff) shl 24) or
                    ((padded[offset + 1].toInt() and 0xff) shl 16) or
                    ((padded[offset + 2].toInt() and 0xff) shl 8) or
                    (padded[offset + 3].toInt() and 0xff)
        }

        for (index in 16 until 64) {
            words[index] =
                sha256SmallSigma1(words[index - 2]) +
                    words[index - 7] +
                    sha256SmallSigma0(words[index - 15]) +
                    words[index - 16]
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4
        var f = h5
        var g = h6
        var h = h7

        for (index in 0 until 64) {
            val temp1 = h +
                sha256BigSigma1(e) +
                sha256Choose(e, f, g) +
                Sha256RoundConstants[index] +
                words[index]
            val temp2 = sha256BigSigma0(a) + sha256Majority(a, b, c)

            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
        h5 += f
        h6 += g
        h7 += h
    }

    return intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7).toBigEndianBytes()
}

private fun ByteArray.sha256PaddedMessage(): ByteArray {
    val bitLength = size.toLong() * 8L
    var paddedLength = size + 1 + 8
    while (paddedLength % 64 != 0) {
        paddedLength += 1
    }

    val padded = ByteArray(paddedLength)
    copyInto(padded)
    padded[size] = 0x80.toByte()

    for (index in 0 until 8) {
        padded[padded.size - 1 - index] = (bitLength ushr (index * 8)).toByte()
    }

    return padded
}

private fun IntArray.toBigEndianBytes(): ByteArray {
    val bytes = ByteArray(size * 4)
    forEachIndexed { index, value ->
        val offset = index * 4
        bytes[offset] = (value ushr 24).toByte()
        bytes[offset + 1] = (value ushr 16).toByte()
        bytes[offset + 2] = (value ushr 8).toByte()
        bytes[offset + 3] = value.toByte()
    }
    return bytes
}

private fun sha256Choose(x: Int, y: Int, z: Int): Int =
    (x and y) xor (x.inv() and z)

private fun sha256Majority(x: Int, y: Int, z: Int): Int =
    (x and y) xor (x and z) xor (y and z)

private fun sha256BigSigma0(value: Int): Int =
    value.rotateRight32(2) xor value.rotateRight32(13) xor value.rotateRight32(22)

private fun sha256BigSigma1(value: Int): Int =
    value.rotateRight32(6) xor value.rotateRight32(11) xor value.rotateRight32(25)

private fun sha256SmallSigma0(value: Int): Int =
    value.rotateRight32(7) xor value.rotateRight32(18) xor (value ushr 3)

private fun sha256SmallSigma1(value: Int): Int =
    value.rotateRight32(17) xor value.rotateRight32(19) xor (value ushr 10)

private fun Int.rotateRight32(bits: Int): Int =
    (this ushr bits) or (this shl (32 - bits))

private val Sha256RoundConstants = intArrayOf(
    0x428a2f98.toInt(),
    0x71374491.toInt(),
    0xb5c0fbcf.toInt(),
    0xe9b5dba5.toInt(),
    0x3956c25b.toInt(),
    0x59f111f1.toInt(),
    0x923f82a4.toInt(),
    0xab1c5ed5.toInt(),
    0xd807aa98.toInt(),
    0x12835b01.toInt(),
    0x243185be.toInt(),
    0x550c7dc3.toInt(),
    0x72be5d74.toInt(),
    0x80deb1fe.toInt(),
    0x9bdc06a7.toInt(),
    0xc19bf174.toInt(),
    0xe49b69c1.toInt(),
    0xefbe4786.toInt(),
    0x0fc19dc6.toInt(),
    0x240ca1cc.toInt(),
    0x2de92c6f.toInt(),
    0x4a7484aa.toInt(),
    0x5cb0a9dc.toInt(),
    0x76f988da.toInt(),
    0x983e5152.toInt(),
    0xa831c66d.toInt(),
    0xb00327c8.toInt(),
    0xbf597fc7.toInt(),
    0xc6e00bf3.toInt(),
    0xd5a79147.toInt(),
    0x06ca6351.toInt(),
    0x14292967.toInt(),
    0x27b70a85.toInt(),
    0x2e1b2138.toInt(),
    0x4d2c6dfc.toInt(),
    0x53380d13.toInt(),
    0x650a7354.toInt(),
    0x766a0abb.toInt(),
    0x81c2c92e.toInt(),
    0x92722c85.toInt(),
    0xa2bfe8a1.toInt(),
    0xa81a664b.toInt(),
    0xc24b8b70.toInt(),
    0xc76c51a3.toInt(),
    0xd192e819.toInt(),
    0xd6990624.toInt(),
    0xf40e3585.toInt(),
    0x106aa070.toInt(),
    0x19a4c116.toInt(),
    0x1e376c08.toInt(),
    0x2748774c.toInt(),
    0x34b0bcb5.toInt(),
    0x391c0cb3.toInt(),
    0x4ed8aa4a.toInt(),
    0x5b9cca4f.toInt(),
    0x682e6ff3.toInt(),
    0x748f82ee.toInt(),
    0x78a5636f.toInt(),
    0x84c87814.toInt(),
    0x8cc70208.toInt(),
    0x90befffa.toInt(),
    0xa4506ceb.toInt(),
    0xbef9a3f7.toInt(),
    0xc67178f2.toInt(),
)
