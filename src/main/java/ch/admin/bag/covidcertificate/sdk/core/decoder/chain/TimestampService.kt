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
import java.time.Instant
import java.time.temporal.ChronoUnit

internal object TimestampService {

	private const val ISSUED_AT_MINUTE_OFFSET = 5L

	fun decode(certificateHolder: CertificateHolder, now: Instant = Instant.now()): String? {
		// Check that the CWT expiration time is after the current timestamp
		certificateHolder.expirationTime?.also { et ->
			if (et.isBefore(now)) {
				return ErrorCodes.SIGNATURE_TIMESTAMP_EXPIRED
			}
		}

		// Check that the CWT issuedAt time is at most 5 minutes in the future to allow minor time-deviations between devices
		certificateHolder.issuedAt?.also { ia ->
			if (ia.isAfter(now.plus(ISSUED_AT_MINUTE_OFFSET, ChronoUnit.MINUTES))) {
				return ErrorCodes.SIGNATURE_TIMESTAMP_NOT_YET_VALID
			}
		}

		return null
	}

}