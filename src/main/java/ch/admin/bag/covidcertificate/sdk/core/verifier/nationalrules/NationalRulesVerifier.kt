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

import ch.admin.bag.covidcertificate.sdk.core.extensions.validFromDate
import ch.admin.bag.covidcertificate.sdk.core.extensions.validUntilDate
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicPayload
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Rule
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleValueSets
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.isTruthy
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

internal class NationalRulesVerifier {

	fun verify(dccCert: DccCert, ruleSet: RuleSet, clock: Clock = Clock.systemUTC()): CheckNationalRulesState {
		val payload = CertLogicPayload(dccCert.pastInfections, dccCert.tests, dccCert.vaccinations)
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(ruleSet.valueSets, validationClock, validationClockAtStartOfDay)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)

		for (rule in ruleSet.rules) {
			val ruleLogic = jacksonMapper.readTree(rule.logic)
			val isSuccessful = isTruthy(evaluate(ruleLogic, data))

			if (!isSuccessful) {
				return getErrorStateForRule(rule, dccCert, ruleSetData.external.valueSets)
			}
		}

		val validityRange = getValidityRange(dccCert, ruleSetData.external.valueSets)
		return if (validityRange != null) {
			CheckNationalRulesState.SUCCESS(validityRange)
		} else {
			CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND)
		}
	}

	private fun getErrorStateForRule(
		rule: Rule,
		dccCert: DccCert,
		ruleValueSets: RuleValueSets
	): CheckNationalRulesState {
		return when (rule.id) {
			"GR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET, rule.id)
			"VR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_VACCINE_ENTRIES, rule.id)
			"VR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NOT_FULLY_PROTECTED, rule.id)
			"VR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT, rule.id)
			"VR-CH-0003" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0004" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"VR-CH-0005" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"VR-CH-0006" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"VR-CH-0007" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"TR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_TEST_ENTRIES, rule.id)
			"TR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.POSITIVE_RESULT, rule.id)
			"TR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_TEST_TYPE, rule.id)
			"TR-CH-0004" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0005" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"TR-CH-0006" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"TR-CH-0007" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"RR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_RECOVERY_ENTRIES, rule.id)
			"RR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0002" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			"RR-CH-0003" -> getValidityRange(dccCert, ruleValueSets)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.VALIDITY_RANGE_NOT_FOUND, rule.id)
			else -> CheckNationalRulesState.INVALID(NationalRulesError.UNKNOWN_RULE_FAILED, rule.id)
		}
	}

	private fun getValidityRange(dccCert: DccCert, ruleValueSets: RuleValueSets): ValidityRange? {
		return when {
			!dccCert.vaccinations.isNullOrEmpty() -> {
				val vaccination = dccCert.vaccinations.first()
				val offset = when {
					// Use the offset value from the acceptance criteria for single-dose vaccines
					ruleValueSets.oneDoseVaccinesWithOffset?.contains(vaccination.medicinialProduct) == true -> {
						ruleValueSets.acceptanceCriteria.singleVaccineValidityOffset
					}
					// Two-dose vaccines don't have an offset
					ruleValueSets.twoDoseVaccines?.contains(vaccination.medicinialProduct) == true -> 0
					else -> null
				}

				offset?.let {
					ValidityRange(
						vaccination.validFromDate(it.toLong()),
						vaccination.validUntilDate(ruleValueSets.acceptanceCriteria)
					)
				}
			}
			!dccCert.tests.isNullOrEmpty() -> {
				val test = dccCert.tests.first()
				ValidityRange(test.validFromDate(), test.validUntilDate(ruleValueSets.acceptanceCriteria))
			}
			!dccCert.pastInfections.isNullOrEmpty() -> {
				val recovery = dccCert.pastInfections.first()
				ValidityRange(
					recovery.validFromDate(ruleValueSets.acceptanceCriteria),
					recovery.validUntilDate(ruleValueSets.acceptanceCriteria)
				)
			}
			else -> null
		}
	}
}

