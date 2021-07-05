/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.PersonName
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class TimestampServiceTest {

	// RECALL: The service returns null if there was NO error
	private val emptyDccCert =
		DccCert("1.0", PersonName(null, "standardizedFamilyName", null, null), "1985-09-21", emptyList(), emptyList(), emptyList())

	@Test
	fun future_expiration() {
		val certificateHolder = CertificateHolder(emptyDccCert, "", expirationTime = Instant.now().plusSeconds(60))
		assertNull(TimestampService.decode(certificateHolder))
	}

	@Test
	fun past_expiration() {
		val certificateHolder = CertificateHolder(emptyDccCert, "", expirationTime = Instant.now().minusSeconds(60))
		assertEquals(TimestampService.decode(certificateHolder), ErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED)
	}

	@Test
	fun no_expiration() {
		val certificateHolder = CertificateHolder(emptyDccCert, "", expirationTime = null)
		assertNull(TimestampService.decode(certificateHolder))
	}

	@Test
	fun future_issuedAt() {
		val certificateHolder = CertificateHolder(emptyDccCert, "", issuedAt = Instant.now().plusSeconds(60))
		assertEquals(TimestampService.decode(certificateHolder), ErrorCodes.SIGNATURE_TIMESTAMP_NOT_YET_VALID)
	}

	@Test
	fun past_issuedAt() {
		val certificateHolder = CertificateHolder(emptyDccCert, "", issuedAt = Instant.now().minusSeconds(60))
		assertNull(TimestampService.decode(certificateHolder))
	}

	@Test
	fun no_issuedAt() {
		val certificateHolder = CertificateHolder(emptyDccCert, "", issuedAt = null)
		assertNull(TimestampService.decode(certificateHolder))
	}

	@Test
	fun combined_invalid() {
		val certificateHolder = CertificateHolder(
			emptyDccCert,
			"",
			expirationTime = Instant.now().minusSeconds(120),
			issuedAt = Instant.now().plusSeconds(60)
		)
		assertEquals(TimestampService.decode(certificateHolder), ErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED)
	}

	@Test
	fun combined_valid() {
		val certificateHolder = CertificateHolder(
			emptyDccCert,
			"",
			expirationTime = Instant.now().plusSeconds(120),
			issuedAt = Instant.now().minusSeconds(60)
		)
		assertNull(TimestampService.decode(certificateHolder))
	}
}