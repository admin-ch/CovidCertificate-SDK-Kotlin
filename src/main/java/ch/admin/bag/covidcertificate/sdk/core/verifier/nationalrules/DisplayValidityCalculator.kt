/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.DisplayRule
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.JsonDateTime
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.isTruthy
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/*
* This class is responsible to return a Validity Range to display in a view. The LocalDateTime is calculated for
* the timezone of the device
* */
internal class DisplayValidityCalculator {

	private val jacksonMapper = ObjectMapper().apply {
		setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
	}

	fun getDisplayValidityRangeForSystemTimeZone(
		displayRules: List<DisplayRule>,
		data: JsonNode,
		certType: CertType
	): ValidityRange? {
		val resultFromDate = evalRule(displayRules, "display-from-date", data) ?: return null
		val resultUntilDate = evalRule(displayRules, "display-until-date", data) ?: return null
		val dateFromString = getDateTime(resultFromDate, data, certType)
		val dateUntilString = getDateTime(resultUntilDate, data, certType)
		return ValidityRange(dateFromString, dateUntilString)
	}

	fun isOnlyValidInSwitzerland(
		displayRules: List<DisplayRule>,
		data: JsonNode
	): Boolean {
		val result = evalRule(displayRules, "is-only-valid-in-ch", data) ?: return false
		return isTruthy(result)
	}

	private fun getDateTime(resultFromDisplayRule: JsonNode, data: JsonNode, certType: CertType): LocalDateTime? {
		if (resultFromDisplayRule is JsonDateTime) {
			return getLocalDateTime(certType, resultFromDisplayRule)
		}
		return null
	}

	private fun getLocalDateTime(certType: CertType, data: JsonDateTime): LocalDateTime {
		if (certType == CertType.TEST) {
			//test
			return data.temporalValue().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
		} else {
			//is vaccine or recovery entry
			return data.temporalValue().toLocalDate().atStartOfDay()
		}
	}

	private fun evalRule(displayRules: List<DisplayRule>, ruleName: String, data: JsonNode): JsonNode? {
		val displayRule: String = displayRules.find { it.id == ruleName }?.logic ?: return null
		val displayLogic = jacksonMapper.readTree(displayRule)
		return evaluate(displayLogic, data)
	}

}