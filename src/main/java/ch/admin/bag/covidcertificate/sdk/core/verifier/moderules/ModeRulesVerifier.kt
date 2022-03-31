/*
* Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
*
* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at https://mozilla.org/MPL/2.0/.
*
* SPDX-License-Identifier: MPL-2.0
*/

package ch.admin.bag.covidcertificate.sdk.core.verifier.moderules

import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicPayload
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CovidCertificate
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.ModeValidity
import ch.admin.bag.covidcertificate.sdk.core.models.state.ModeValidityState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.TextNode
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

internal class ModeRulesVerifier {

	fun verify(
		certificate: CovidCertificate,
		ruleSet: RuleSet,
		headers: CertLogicHeaders?,
		mode: String,
		clock: Clock = Clock.systemUTC()
	): ModeValidity {
		val modeRules = ruleSet.modeRules ?: return ModeValidity(mode, ModeValidityState.UNKNOWN)

		val ruleSetData = getCertlogicData(certificate, ruleSet.valueSets, headers, clock)
		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		val ruleLogic = jacksonMapper.readTree(modeRules.logic)
		val resultFromModeRule = evaluate(ruleLogic, data)
		val state = getValidityState(resultFromModeRule)
		return ModeValidity(mode, state)
	}

	private fun getCertlogicData(
		certificate: CovidCertificate,
		valueSets: Map<String, Array<String>>,
		headers: CertLogicHeaders?,
		clock: Clock = Clock.systemUTC()
	): CertLogicData {
		val payload = when (certificate) {
			is ChLightCert -> CertLogicPayload(null, null, null, headers)
			is DccCert -> CertLogicPayload(certificate.pastInfections, certificate.tests, certificate.vaccinations, headers)
			else -> CertLogicPayload(null, null, null, null)
		}
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(valueSets, validationClock, validationClockAtStartOfDay)
		return CertLogicData(payload, externalInfo)
	}

	private fun getValidityState(resultFromModeRule: JsonNode): ModeValidityState {
		if (resultFromModeRule is TextNode) {
			val modeValidityState = resultFromModeRule.textValue()
			return when {
				ModeValidityState.SUCCESS.name.equals(modeValidityState, true) -> ModeValidityState.SUCCESS
				ModeValidityState.INVALID.name.equals(modeValidityState, true) -> ModeValidityState.INVALID
				ModeValidityState.IS_LIGHT.name.equals(modeValidityState, true) -> ModeValidityState.IS_LIGHT
				ModeValidityState.SUCCESS_2G.name.equals(modeValidityState, true) -> ModeValidityState.SUCCESS_2G
				ModeValidityState.SUCCESS_2G_PLUS.name.equals(modeValidityState, true) -> ModeValidityState.SUCCESS_2G_PLUS
				ModeValidityState.UNKNOWN_MODE.name.equals(modeValidityState, true) -> ModeValidityState.UNKNOWN_MODE
				else -> ModeValidityState.UNKNOWN
			}
		}
		return ModeValidityState.UNKNOWN

	}

}