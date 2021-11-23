/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.healthcert

import ch.admin.bag.covidcertificate.sdk.core.extensions.isSerologicalTest
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import java.io.Serializable
import java.time.Instant

class CertificateHolder(
	val certificate: CovidCertificate,
	val qrCodeData: String,
	val expirationTime: Instant? = null,
	val issuedAt: Instant? = null,
	val issuer: String? = null,
) : Serializable {

	var certType: CertType? = null
		internal set

	fun containsDccCert() = certificate is DccCert

	fun containsChLightCert() = certificate is ChLightCert

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as CertificateHolder

		if (certificate != other.certificate) return false
		if (qrCodeData != other.qrCodeData) return false

		return true
	}

	override fun hashCode(): Int {
		var result = qrCodeData.hashCode()
		result = 31 * result + certificate.hashCode()
		return result
	}
}
