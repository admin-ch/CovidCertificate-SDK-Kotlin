/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.bloom

import ch.admin.bag.covidcertificate.sdk.core.data.base64.Base64Impl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.SecureRandom
import java.time.Instant
import java.time.OffsetDateTime


class BloomFilterTest {
    @Test
    fun testBloomFilter() {
        val spec = BloomFilterSpec("P7tmqgzoIlhroqWQqyUC24Jo3GZB/C1sBD2ahgMeutcPsGOyEFagdswW9H5rjcPO+7oXYKPHTMbseGl6Of+SkHYQVPP7xJNx8m0I2t3z17TuUvERHj7SH7rOKJc3zKTgpfASEKkBtRaWhnwfiTPI1wiE124tl5SpTrMY3VPyk7wozoZXHPw09Be66KY//mcBOnixQA==",
            1179, 20, 1646302884724)
        val hash = "n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg="
        val bloom = BloomFilter(spec)
        assert(bloom.contains(hash))
    }
    @Test
    fun testRatioCorrect() {
        val start = Instant.now()
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
        val samples = 100_000

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
        val end = Instant.now()
        val duration = end.toEpochMilli() - start.toEpochMilli()
        print("Took: $duration ms\n")
    }
}