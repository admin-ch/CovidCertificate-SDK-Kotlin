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

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CovidCertificate
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.ModeValidity
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.*
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicPayload
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

internal class ModeRulesVerifier {

	fun verify(
		certificate: CovidCertificate,
		ruleSet: RuleSet,
		headers: CertLogicHeaders?,
		mode: String,
		clock: Clock = Clock.systemUTC()
	): ModeValidity {
		val ruleSetData = getCertlogicData(certificate, ruleSet.valueSets, headers, clock)
		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		val ruleLogic = jacksonMapper.readTree(ruleSet.modeRules.logic)
		val isSuccessful = isTruthy(evaluate(ruleLogic, data))

		return ModeValidity(mode, isSuccessful)
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

}