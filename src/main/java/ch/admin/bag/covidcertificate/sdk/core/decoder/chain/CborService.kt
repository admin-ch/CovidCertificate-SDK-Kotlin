/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import com.upokecenter.cbor.CBORObject
import java.time.Instant
import java.util.*

internal object CborService {

	private val keyDccCertV1 = CBORObject.FromObject(1)
	private val keyChLightCertV1 = CBORObject.FromObject(1)

	// Takes qrCodeData to directly construct a Bagdgc AND keep the field in the DCC a val
	fun decode(input: ByteArray, qrCodeData: String): CertificateHolder? {

		val moshi = Moshi.Builder().add(Date::class.java, Rfc3339DateJsonAdapter()).build()
		val dccCertAdapter = moshi.adapter(DccCert::class.java)
		val chLightCertAdapter = moshi.adapter(ChLightCert::class.java)

		try {
			val map = CBORObject.DecodeFromBytes(input)

			val expirationTime: Instant? = map[CwtHeaderKeys.EXPIRATION.asCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
			val issuedAt: Instant? = map[CwtHeaderKeys.ISSUED_AT.asCBOR()]?.let { Instant.ofEpochSecond(it.AsInt64()) }
			val issuer: String? = map[CwtHeaderKeys.ISSUER.asCBOR()]?.AsString()

			val hcert = map[CwtHeaderKeys.HCERT.asCBOR()]
			val light = map[CwtHeaderKeys.LIGHT.asCBOR()]

			when {
				hcert != null -> {
					hcert[keyDccCertV1]?.let {
						val dccCert = dccCertAdapter.fromJson(it.ToJSONString()) ?: return null
						return CertificateHolder(dccCert, qrCodeData, expirationTime, issuedAt, issuer)
					} ?: return null
				}
				light != null -> {
					light[keyChLightCertV1]?.let {
						val chLightCert = chLightCertAdapter.fromJson(it.ToJSONString()) ?: return null
						return CertificateHolder(chLightCert, qrCodeData, expirationTime, issuedAt, issuer)
					} ?: return null
				}
				else -> return null
			}

		} catch (e: Throwable) {
			return null
		}
	}

}