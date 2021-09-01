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

import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonString
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.RecoveryEntry
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.TestEntry
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RuleSet(
	val displayRules: List<DisplayRule>,
	val rules: List<Rule>,
	val valueSets: Map<String, Array<String>>,
	val validDuration: Long,
)

@JsonClass(generateAdapter = true)
data class DisplayRule(
	val id: String,
	@RawJsonString val logic: String
)

@JsonClass(generateAdapter = true)
data class Rule(
	val affectedFields: List<String>,
	val certificateType: String,
	val country: String,
	val description: List<Description>,
	val engine: String,
	val engineVersion: String,
	val identifier: String,
	@RawJsonString val logic: String,
	val schemaVersion: String,
	val type: String,
	val validFrom: String,
	val validTo: String,
	val version: String

)

@JsonClass(generateAdapter = true)
data class Description(
	val desc: String,
	val lang: String
)

internal data class CertLogicData(
	val payload: CertLogicPayload,
	val external: CertLogicExternalInfo,
)

internal data class CertLogicPayload(
	val r: List<RecoveryEntry>? = null,
	val t: List<TestEntry>? = null,
	val v: List<VaccinationEntry>? = null,
)

internal data class CertLogicExternalInfo(
	val valueSets: Map<String, Array<String>>,
	val validationClock: String, // ISO-8601 extended offset date-time format
	val validationClockAtStartOfDay: String, // ISO-8601 date format
)