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

import ch.admin.bag.covidcertificate.sdk.core.data.AcceptedVaccineProvider
import ch.admin.bag.covidcertificate.sdk.core.extensions.validFromDate
import ch.admin.bag.covidcertificate.sdk.core.extensions.validUntilDate
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.AcceptanceCriterias
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicPayload
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Rule
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.isTruthy
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class NationalRulesVerifier(private val acceptedVaccineProvider: AcceptedVaccineProvider) {

	fun verify(dccCert: DccCert, ruleSet: RuleSet, clock: Clock = Clock.systemDefaultZone()): CheckNationalRulesState {
		val payload = CertLogicPayload(dccCert.pastInfections, dccCert.tests, dccCert.vaccinations)
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(ruleSet.valueSets, validationClock, validationClockAtStartOfDay)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getDefault())
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)

		for (rule in ruleSet.rules) {
			val ruleLogic = jacksonMapper.readTree(rule.logic)
			val isSuccessful = isTruthy(evaluate(ruleLogic, data))

			if (!isSuccessful) {
				return getErrorStateForRule(rule, dccCert, ruleSetData.external.valueSets.acceptanceCriteria)
			}
		}

		val validityRange = getValidityRange(dccCert, ruleSetData.external.valueSets.acceptanceCriteria)
		return if (validityRange != null) {
			CheckNationalRulesState.SUCCESS(validityRange)
		} else {
			CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE)
		}
	}

	private fun getErrorStateForRule(
		rule: Rule,
		dccCert: DccCert,
		acceptanceCriterias: AcceptanceCriterias
	): CheckNationalRulesState {
		return when (rule.id) {
			"GR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_DISEASE_TARGET, rule.id)
			"VR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_VACCINE_ENTRIES, rule.id)
			"VR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NOT_FULLY_PROTECTED, rule.id)
			"VR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT, rule.id)
			"VR-CH-0003" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0004" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0005" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"VR-CH-0006" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_TEST_ENTRIES, rule.id)
			"TR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.POSITIVE_RESULT, rule.id)
			"TR-CH-0002" -> CheckNationalRulesState.INVALID(NationalRulesError.WRONG_TEST_TYPE, rule.id)
			"TR-CH-0003" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_PRODUCT, rule.id)
			"TR-CH-0004" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0005" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0006" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"TR-CH-0007" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0000" -> CheckNationalRulesState.INVALID(NationalRulesError.TOO_MANY_RECOVERY_ENTRIES, rule.id)
			"RR-CH-0001" -> CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0002" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_YET_VALID(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			"RR-CH-0003" -> getValidityRange(dccCert, acceptanceCriterias)?.let {
				CheckNationalRulesState.NOT_VALID_ANYMORE(it, rule.id)
			} ?: CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_DATE, rule.id)
			else -> CheckNationalRulesState.INVALID(NationalRulesError.UNKNOWN_RULE_FAILED, rule.id)
		}
	}

	private fun getValidityRange(dccCert: DccCert, acceptanceCriterias: AcceptanceCriterias): ValidityRange? {
		return when {
			!dccCert.vaccinations.isNullOrEmpty() -> {
				val vaccination = dccCert.vaccinations.first()
				val usedVaccine = acceptedVaccineProvider.getVaccineDataFromList(vaccination)
				usedVaccine?.let {
					ValidityRange(
						vaccination.validFromDate(it, acceptanceCriterias),
						vaccination.validUntilDate(acceptanceCriterias)
					)
				}
			}
			!dccCert.tests.isNullOrEmpty() -> {
				val test = dccCert.tests.first()
				ValidityRange(test.validFromDate(), test.validUntilDate(acceptanceCriterias))
			}
			!dccCert.pastInfections.isNullOrEmpty() -> {
				val recovery = dccCert.pastInfections.first()
				ValidityRange(recovery.validFromDate(acceptanceCriterias), recovery.validUntilDate(acceptanceCriterias))
			}
			else -> null
		}
	}
}

