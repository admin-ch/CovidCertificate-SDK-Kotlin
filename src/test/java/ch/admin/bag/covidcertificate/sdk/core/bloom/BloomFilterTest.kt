package ch.admin.bag.covidcertificate.sdk.core.bloom

import ch.admin.bag.covidcertificate.sdk.core.data.base64.Base64Impl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.SecureRandom


class BloomFilterTest {
    @Test
    fun testBloomFilter() {
        val spec = BloomFilterSpec("5lPRc7MdD6uLy2RF/bIDowyz61BomW+9Tu0MyNqd0E/KdWYKJ/QhvAcuaOK9JO3S5qiRLeCrjJeyk5FzEhebqzIV6SscCx186hEUxdAiqgkQgS1z6kl8LPWVNWGRUbr3xXGVvI3m4tH6Y/uwIDWxwe04qITiQCf/yTab12BInvnYwmBjeZG4lpjsznX7WWsE2NS3wA==",
            1179, 20, 1646298029533)
        val hash = "n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg="
        val bloom = BloomFilter(spec)
        assert(bloom.contains(hash))
    }
    @Test
    fun testRatioCorrect() {
        val hashes: MutableList<String> = mutableListOf()
        val rand = SecureRandom.getInstanceStrong()
        val numHashes = 400_000
        for (i in 0 until numHashes) {
            val bytes = ByteArray(32)
            rand.nextBytes(bytes)
            val hashString: String = Base64Impl.encode(bytes)

            hashes.add(hashString)
        }

        val bloom = BloomFilter(hashes, 1e-3f)
        for (h in hashes) {
            assertTrue(bloom.contains(h))
        }

        var contained = 0
        val samples = 100000

        for (i in 0 until samples) {
            val bytes = ByteArray(32)
            rand.nextBytes(bytes)
            val hashString: String = Base64Impl.encode(bytes)

            if (bloom.contains(hashString) && !hashes.contains(hashString)) {
                contained += 1
            }
        }
        val ratio = contained.toFloat() / samples.toFloat()
        assertTrue(ratio - 5e-4f < 1e-3f && ratio + 5e-4f > 1e-3f)
    }
}