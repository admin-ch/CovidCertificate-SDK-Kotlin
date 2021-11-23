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

class RevokedCertificatesInMemoryImpl(private var revokedCertificates: List<String>) : RevokedCertificatesStore {
	override fun containsCertificate(certificate: String) = revokedCertificates.contains(certificate)

	override fun addCertificates(certificates: List<String>) {
		revokedCertificates = revokedCertificates.toMutableList().apply {
			addAll(certificates)
			distinct()
		}
	}

}