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
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/*
* This class is responsible to return a Validity Range to display in a view. The LocalDateTime is calculated for
* the timezone of the device
* */
internal class DisplayValidityCalculator {

	companion object {
		private const val RULE_DISPLAY_DATE_FROM = "display-from-date"
		private const val RULE_DISPLAY_DATE_UNTIL = "display-until-date"
		private const val RULE_CH_ONLY = "is-only-valid-in-ch"
		private const val RULE_EOL_BANNER = "eol-banner"
		private const val RULE_RENEW_BANNER = "renew-banner"
	}

	private val jacksonMapper = ObjectMapper().apply {
		setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
	}

	fun getDisplayValidityRangeForSystemTimeZone(
		displayRules: List<DisplayRule>?,
		data: JsonNode,
		certType: CertType
	): ValidityRange? {
		if (displayRules == null) return null

		val resultFromDate = evalRule(displayRules, RULE_DISPLAY_DATE_FROM, data) ?: return null
		val resultUntilDate = evalRule(displayRules, RULE_DISPLAY_DATE_UNTIL, data) ?: return null
		val dateFromString = getDateTime(resultFromDate, certType, true)
		val dateUntilString = getDateTime(resultUntilDate, certType, false)
		return ValidityRange(dateFromString, dateUntilString)
	}

	fun isOnlyValidInSwitzerland(
		displayRules: List<DisplayRule>?,
		data: JsonNode
	): Boolean {
		val result = displayRules?.let { evalRule(it, RULE_CH_ONLY, data) } ?: return false
		return isTruthy(result)
	}

	fun getEolBannerIdentifier(
		displayRules: List<DisplayRule>?,
		data: JsonNode
	): String? {
		return displayRules?.let { evalRule(it, RULE_EOL_BANNER, data)?.asText(null) }
	}

	fun getShowRenewBanner(
		displayRules: List<DisplayRule>?,
		data: JsonNode
	): String? {
		return displayRules?.let { evalRule(it, RULE_RENEW_BANNER, data)?.asText(null) }
	}

	private fun getDateTime(
		resultFromDisplayRule: JsonNode,
		certType: CertType,
		atStartOfDay: Boolean
	): LocalDateTime? {
		if (resultFromDisplayRule is JsonDateTime) {
			return getLocalDateTime(certType, resultFromDisplayRule, atStartOfDay)
		}
		return null
	}

	private fun getLocalDateTime(certType: CertType, data: JsonDateTime, atStartOfDay: Boolean): LocalDateTime {
		return if (certType == CertType.TEST) {
			//test
			data.temporalValue().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
		} else {
			//is vaccine or recovery entry
			if (atStartOfDay) {
				data.temporalValue().toLocalDate().atStartOfDay()
			} else {
				data.temporalValue().toLocalDate().atTime(LocalTime.MAX)
			}
		}
	}

	private fun evalRule(displayRules: List<DisplayRule>, ruleName: String, data: JsonNode): JsonNode? {
		val displayRule: String = displayRules.find { it.id == ruleName }?.logic ?: return null
		val displayLogic = jacksonMapper.readTree(displayRule)
		return evaluate(displayLogic, data)
	}

}