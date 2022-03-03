package ch.admin.bag.covidcertificate.sdk.core.bloom

import ch.admin.bag.covidcertificate.sdk.core.data.base64.Base64Impl
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Instant
import java.util.*
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val ln2 = 0.6931471806f
private const val ln2_2 = 0.4804530139f

class BloomFilter(spec: BloomFilterSpec) {
    private var numBits: Int = spec.numBits
    private var numHashes: Int = spec.numHashes
    private lateinit var filter: ByteArray
    private var seed: Long = spec.seed
    private var digest: MessageDigest = MessageDigest.getInstance("SHA-256")

    init {
        filter = if (spec.filterBytes.isEmpty()) {
            ByteArray(numBits / 8 + 1)
        } else {
            Base64Impl.decode(spec.filterBytes)
        }
    }

    constructor(hashes: List<String>, falsePositiveRate: Float) : this(
        BloomFilterSpec(
            "",
            neededBits(hashes.size, falsePositiveRate),
            optimalHashes(hashes.size, neededBits(hashes.size, falsePositiveRate)),
            Instant.now().toEpochMilli()
        )
    ) {
        for (hash in hashes) {
            this.insert(hash)
        }
    }

    fun getFilter(): String {
        return Base64Impl.encode(filter)
    }

    @Throws(NoSuchAlgorithmException::class)
    fun insert(hash: String): Boolean {
        var contained = true
        for (i in 0 until numHashes) {
            digest.update(ByteBuffer.allocate(4).putInt(i).array())
            digest.update(Base64Impl.decode(hash))
            digest.update(ByteBuffer.allocate(8).putLong(seed).array())
            val hashByteBuffer: ByteBuffer = ByteBuffer.wrap(digest.digest())
            var hashedValue: Long = 0
            for (j in 0..7) {
                hashedValue = hashedValue xor hashByteBuffer.int.toUInt().toLong()
            }
            val bitToSet = (hashedValue % numBits).toInt()
            val byteContainingBit = (bitToSet / 8)
            val orMask = (1 shl (7 - bitToSet % 8)).toByte()

            if (orMask != filter[byteContainingBit] and orMask) {
                contained = false
            }
            filter[byteContainingBit] = (filter[byteContainingBit] or orMask) as Byte
        }
        return !contained
    }

    @Throws(NoSuchAlgorithmException::class)
    operator fun contains(hash: String): Boolean {
        for (i in 0 until numHashes) {
            digest.update(ByteBuffer.allocate(4).putInt(i).array())
            digest.update(Base64Impl.decode(hash))
            digest.update(ByteBuffer.allocate(8).putLong(seed).array())
            val hashByteBuffer: ByteBuffer = ByteBuffer.wrap(digest.digest())
            var hashedValue: Long = 0
            for (j in 0..7) {
                hashedValue = hashedValue xor hashByteBuffer.int.toUInt().toLong()
            }
            val bitToSet = hashedValue % numBits
            val byteContainingBit = (bitToSet / 8).toInt()
            val orMask = (1 shl (7 - bitToSet % 8).toInt()).toByte()
            if (orMask != filter[byteContainingBit] and orMask) {
                return false
            }
        }
        return true
    }

    fun getBloomFilterSpec(): BloomFilterSpec? {
        return BloomFilterSpec(getFilter(), numBits, numHashes, seed)
    }

    fun clear() {
        filter = ByteArray(filter.size)
    }
}

private fun neededBits(numHashes: Int, falsePositiveRate: Float): Int {
    val lnRatio = -Math.log(falsePositiveRate.toDouble()) / ln2_2
    return ceil(numHashes * lnRatio).toInt()
}

private fun optimalHashes(numHashes: Int, numBits: Int): Int {
    return if (numHashes <= 0) {
        2
    } else ceil(
        min(max(numBits.toFloat() / numHashes.toFloat() * ln2, 2f), 200f).toDouble()
    )
        .toInt()
}