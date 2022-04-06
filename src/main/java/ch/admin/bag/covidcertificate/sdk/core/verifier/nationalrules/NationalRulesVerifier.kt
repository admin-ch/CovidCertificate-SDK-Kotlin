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

import ch.admin.bag.covidcertificate.sdk.core.extensions.isPositiveRatTest
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicPayload
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState.*
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.DisplayRule
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Rule
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.utils.DateUtil
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.NationalRulesError.*
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.isTruthy
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

internal class NationalRulesVerifier {

	private val displayValidityCalculator = DisplayValidityCalculator()

	fun verify(
		dccCert: DccCert,
		ruleSet: RuleSet,
		certType: CertType,
		headers: CertLogicHeaders?,
		nationalRulesCheckDate: LocalDateTime? = null,
		isForeignRulesCheck: Boolean = false,
		clock: Clock = Clock.systemUTC()
	): CheckNationalRulesState {
		// Filter the rules with the specified date
		val rules = getValidRulesForDate(ruleSet, nationalRulesCheckDate)
		if (rules.isEmpty()) {
			return INVALID(NO_VALID_RULES_FOR_SPECIFIC_DATE)
		}

		val ruleSetData = getCertlogicData(dccCert, ruleSet.valueSets, headers, clock, nationalRulesCheckDate)
		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)

		rules.forEach { rule ->
			val ruleLogic = jacksonMapper.readTree(rule.logic)
			val isSuccessful = isTruthy(evaluate(ruleLogic, data))

			if (!isSuccessful) {
				return getErrorStateForRule(rule, dccCert, ruleSet.displayRules, ruleSetData.external.valueSets, certType, headers)
			}
		}

		return when {
			ruleSet.displayRules != null -> {
				// If the ruleset contains display rules, get the certificate validity range and additional display flags
				val validityRange = getValidityRange(ruleSet.displayRules, data, certType)
				val isOnlyValidInSwitzerland = displayValidityCalculator.isOnlyValidInSwitzerland(ruleSet.displayRules, data)
				val eolBannerIdentifier = displayValidityCalculator.getEolBannerIdentifier(ruleSet.displayRules, data)
				SUCCESS(validityRange, isOnlyValidInSwitzerland, eolBannerIdentifier)
			}
			isForeignRulesCheck -> {
				// If the ruleset contains no display rules but this is a foreign rules check, consider it a success but without any additional information
				SUCCESS(null, false)
			}
			else -> {
				// In all other cases, consider it invalid without a validity range
				INVALID(VALIDITY_RANGE_NOT_FOUND)
			}
		}
	}

	fun getCertlogicData(
		dccCert: DccCert,
		valueSets: Map<String, Array<String>>,
		headers: CertLogicHeaders?,
		clock: Clock = Clock.systemUTC(),
		checkDate: LocalDateTime? = null,
	): CertLogicData {
		val tests = dccCert.tests?.map {
			if (it.isPositiveRatTest()) {
				val offsetTime = OffsetDateTime.parse(it.timestampSample).toOffsetTime()
				val timestampSample = DateUtil.parseDateTime(it.timestampSample, offsetTime.offset)
					?.withHour(0)
					?.withMinute(0)
					?.withSecond(0)
					?.atZone(ZoneId.systemDefault())
					?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
					?: it.timestampSample
				it.copy(timestampSample = timestampSample)
			} else {
				it
			}
		}

		val payload = CertLogicPayload(
			dccCert.person,
			dccCert.dateOfBirth,
			dccCert.version,
			dccCert.pastInfections,
			tests,
			dccCert.vaccinations,
			headers
		)

		val validationClock = checkDate
			?.atZone(ZoneId.systemDefault())
			?.withZoneSameInstant(ZoneOffset.UTC)
			?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
			?: ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

		val validationClockAtStartOfDay = checkDate
			?.atZone(ZoneId.systemDefault())
			?.withZoneSameInstant(ZoneOffset.UTC)
			?.with(LocalTime.MIN)
			?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
			?: LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

		val externalInfo = CertLogicExternalInfo(valueSets, validationClock, validationClockAtStartOfDay)
		return CertLogicData(payload, externalInfo)
	}

	private fun getValidRulesForDate(ruleSet: RuleSet, checkDate: LocalDateTime?): List<Rule> {
		if (checkDate == null) {
			return ruleSet.rules
		}

		return ruleSet.rules
			.groupBy { it.identifier } // The same rule can occur multiple times with different validity ranges
			.mapNotNull { (_, rules) ->
				// Filter the grouped rules by their validity range for the specified checkDate, then take the one with the latest validFrom field
				rules.filter {
					val validFromDate = DateUtil.parseDateTime(it.validFrom)
					val validToDate = DateUtil.parseDateTime(it.validTo)

					if (validFromDate != null && validToDate != null) {
						checkDate in validFromDate..validToDate
					} else {
						false
					}
				}.maxByOrNull { it.validFrom }
			}
	}

	private fun getErrorStateForRule(
		rule: Rule,
		dccCert: DccCert,
		displayRules: List<DisplayRule>?,
		valueSets: Map<String, Array<String>>,
		certType: CertType,
		headers: CertLogicHeaders?,
		clock: Clock = Clock.systemUTC()
	): CheckNationalRulesState {
		val ruleSetData = getCertlogicData(dccCert, valueSets, headers, clock)
		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		return when (rule.identifier) {
			"GR-CH-0001" -> INVALID(WRONG_DISEASE_TARGET, rule.identifier)
			"VR-CH-0000" -> INVALID(TOO_MANY_VACCINE_ENTRIES, rule.identifier)
			"VR-CH-0001" -> INVALID(NOT_FULLY_PROTECTED, rule.identifier)
			"VR-CH-0002" -> INVALID(NO_VALID_PRODUCT, rule.identifier)
			"VR-CH-0003" -> INVALID(NO_VALID_DATE, rule.identifier)
			"VR-CH-0004" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"VR-CH-0005" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"VR-CH-0006" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"VR-CH-0007" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"VR-CH-0008" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0000" -> INVALID(TOO_MANY_TEST_ENTRIES, rule.identifier)
			"TR-CH-0001" -> INVALID(POSITIVE_RESULT, rule.identifier)
			"TR-CH-0002" -> INVALID(WRONG_TEST_TYPE, rule.identifier)
			"TR-CH-0003" -> INVALID(NO_VALID_PRODUCT, rule.identifier)
			"TR-CH-0004" -> INVALID(NO_VALID_DATE, rule.identifier)
			"TR-CH-0005" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0006" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0007" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0008" -> INVALID(NEGATIVE_RESULT, rule.identifier)
			"TR-CH-0009" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0010" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0011" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"TR-CH-0012" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"RR-CH-0000" -> INVALID(TOO_MANY_RECOVERY_ENTRIES, rule.identifier)
			"RR-CH-0001" -> INVALID(NO_VALID_DATE, rule.identifier)
			"RR-CH-0002" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_YET_VALID(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			"RR-CH-0003" -> getValidityRange(displayRules, data, certType)?.let {
				NOT_VALID_ANYMORE(it, rule.identifier)
			} ?: INVALID(VALIDITY_RANGE_NOT_FOUND, rule.identifier)
			else -> INVALID(UNKNOWN_RULE_FAILED, rule.identifier)
		}
	}

	private fun getValidityRange(
		displayRules: List<DisplayRule>?,
		data: JsonNode,
		certType: CertType
	): ValidityRange? {
		return displayRules?.let {
			displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(it, data, certType)
		}
	}


}

