/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.trustlist

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JwkTest {

	@Test
	fun testNoUseCannotSignAnything() {
		val jwk = Jwk.fromNE("kid", "n", "e", use = "")
		val certTypesAllowedToSign = CertType.values().filter { jwk.isAllowedToSign(it) }
		assertTrue(certTypesAllowedToSign.isEmpty(), "JWK with an empty use flag should not be allowed to sign anything")
	}

	@Test
	fun testWrongUseCannotSignAnything() {
		val jwk = Jwk.fromNE("kid", "n", "e", use = "xyz")
		val certTypesAllowedToSign = CertType.values().filter { jwk.isAllowedToSign(it) }
		assertTrue(certTypesAllowedToSign.isEmpty(), "JWK with a wrong use flag should not be allowed to sign anything")
	}

	@Test
	fun testUseFlags() {
		val combinations = mapOf(
			// Single use flags
			"v" to setOf(CertType.VACCINATION),
			"r" to setOf(CertType.RECOVERY),
			"t" to setOf(CertType.TEST),
			"l" to setOf(CertType.LIGHT),

			// Two use flags
			"vr" to setOf(CertType.VACCINATION, CertType.RECOVERY),
			"vt" to setOf(CertType.VACCINATION, CertType.TEST),
			"vl" to setOf(CertType.VACCINATION, CertType.LIGHT),
			"rt" to setOf(CertType.RECOVERY, CertType.TEST),
			"rl" to setOf(CertType.RECOVERY, CertType.LIGHT),
			"vl" to setOf(CertType.VACCINATION, CertType.LIGHT),

			// Three use flags
			"vrt" to setOf(CertType.VACCINATION, CertType.RECOVERY, CertType.TEST),
			"vrl" to setOf(CertType.VACCINATION, CertType.RECOVERY, CertType.LIGHT),
			"vtl" to setOf(CertType.VACCINATION, CertType.TEST, CertType.LIGHT),
			"rtl" to setOf(CertType.RECOVERY, CertType.TEST, CertType.LIGHT),

			// "Use all" flags
			"sig" to setOf(CertType.VACCINATION, CertType.RECOVERY, CertType.TEST, CertType.LIGHT),
			"vrtl" to setOf(CertType.VACCINATION, CertType.RECOVERY, CertType.TEST, CertType.LIGHT),
		)

		combinations.forEach { (use, expected) ->
			val jwk = Jwk.fromNE("kid", "n", "e", use = use)
			val certTypesAllowedToSign = CertType.values().filter { jwk.isAllowedToSign(it) }.toSet()
			assertEquals(
				expected,
				certTypesAllowedToSign,
				"JWK with '$use' flag(s) should not be allowed to sign certificates of other types"
			)
		}
	}

}