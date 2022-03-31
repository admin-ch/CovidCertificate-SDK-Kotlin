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
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RuleSet(
	val rules: List<Rule>,
	val valueSets: Map<String, Array<String>>,
	val validDuration: Long,
	val displayRules: List<DisplayRule>? = null,
	val modeRules: ModeRules? = null,
)

@JsonClass(generateAdapter = true)
data class ModeRules(
	val activeModes: List<ActiveModes>, // Legacy property when there was only one list for both apps
	val walletActiveModes: List<ActiveModes>?,
	val verifierActiveModes: List<ActiveModes>?,
	@RawJsonString
	val logic: String,
)

@JsonClass(generateAdapter = true)
data class ActiveModes(
	val id: String,
	val displayName: String
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