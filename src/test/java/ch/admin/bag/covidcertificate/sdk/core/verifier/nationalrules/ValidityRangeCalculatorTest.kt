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

import ch.admin.bag.covidcertificate.sdk.core.TestDataGenerator
import ch.admin.bag.covidcertificate.sdk.core.Vaccine
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.*
import java.time.temporal.ChronoUnit
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidityRangeCalculatorTest {

	private lateinit var validityRangeCalculator: DisplayValidityRangeCalculator
	private lateinit var nationalRulesVerifier: NationalRulesVerifier
	private lateinit var nationalRuleSet: RuleSet
	private lateinit var utcClock: Clock
	private val jacksonMapper = ObjectMapper().apply {
		setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
	}

	@BeforeAll
	fun setup() {
		val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		val nationalRulesString = this::class.java.classLoader.getResource("nationalrules.json")!!.readText()
		nationalRuleSet = moshi.adapter(RuleSet::class.java).fromJson(nationalRulesString)!!

		validityRangeCalculator = DisplayValidityRangeCalculator()
		nationalRulesVerifier = NationalRulesVerifier()
		utcClock = Clock.systemUTC()
	}

	@Test
	fun testOneDoseVaccinationValidityRange() {
		val clock = Clock.fixed(Instant.parse("2021-06-05T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).atStartOfDay()

		val vaccine = Vaccine.JANSSEN
		val vaccination = TestDataGenerator.generateVaccineCert(
			1,
			1,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		val data = getJsonNodeData(vaccination, clock)
		val validityRange = validityRangeCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)
		assertEquals(LocalDate.of(2021, 6, 26), validityRange?.validFrom?.toLocalDate())
		assertEquals(LocalDate.of(2022, 6, 4), validityRange?.validUntil?.toLocalDate())
	}

	@Test
	fun testTwoDoseVaccinationValidityRange() {
		val clock = Clock.fixed(Instant.parse("2021-06-05T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).atStartOfDay()

		val vaccine = Vaccine.BIONTECH
		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		val data = getJsonNodeData(vaccination, clock)
		val validityRange = validityRangeCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)
		assertEquals(LocalDate.of(2021, 6, 5), validityRange?.validFrom?.toLocalDate())
		assertEquals(LocalDate.of(2022, 6, 4), validityRange?.validUntil?.toLocalDate())
	}

	@Test
	fun testRatTestValidityRange() {
		val now = OffsetDateTime.now(utcClock)
		val duration = Duration.ofHours(10)
		val sampleCollectionTime = now.minus(duration)
		val test = TestDataGenerator.generateTestCertFromDate(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)

		val data = getJsonNodeData(test, utcClock)
		val validityRange =
			validityRangeCalculator.getDisplayValidityRangeForSystemTimeZone(nationalRuleSet.displayRules, data, CertType.TEST)
		assertNotNull(validityRange)

		// The expected values need to be truncated to milliseconds because the JsonDateTime class reformats the timestamp string
		// and strips away micro- and nanoseconds
		val expectedValidFrom =
			sampleCollectionTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS)
		assertEquals(expectedValidFrom, validityRange?.validFrom)

		val expectedValidUntil = sampleCollectionTime.plusHours(48).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
			.truncatedTo(ChronoUnit.MILLIS)
		assertEquals(expectedValidUntil, validityRange?.validUntil)
	}

	@Test
	fun testPcrTestValidityRange() {
		val now = OffsetDateTime.now(utcClock)
		val duration = Duration.ofHours(10)
		val sampleCollectionTime = now.minus(duration)
		val test = TestDataGenerator.generateTestCertFromDate(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)
		val data = getJsonNodeData(test, utcClock)
		val validityRange =
			validityRangeCalculator.getDisplayValidityRangeForSystemTimeZone(nationalRuleSet.displayRules, data, CertType.TEST)
		assertNotNull(validityRange)

		// The expected values need to be truncated to milliseconds because the JsonDateTime class reformats the timestamp string
		// and strips away micro- and nanoseconds.atZoneSameInstant(ZoneId.systemDefault())
		val expectedValidFrom =
			sampleCollectionTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS)
		assertEquals(expectedValidFrom, validityRange?.validFrom)

		val expectedValidUntil = sampleCollectionTime.plusHours(72).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
			.truncatedTo(ChronoUnit.MILLIS)
		assertEquals(expectedValidUntil, validityRange?.validUntil)
	}

	@Test
	fun testRecoveryValidityRange() {
		val firstTestResult = LocalDate.now(utcClock).minusDays(20)
		val validFrom = firstTestResult.plusDays(10)
		val validUntil = firstTestResult.plusDays(179)
		val recovery = TestDataGenerator.generateRecoveryCertFromDate(
			validFrom.atStartOfDay(),
			validUntil.atStartOfDay(),
			firstTestResult.atStartOfDay(),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)
		val data = getJsonNodeData(recovery, utcClock)
		val validityRange =
			validityRangeCalculator.getDisplayValidityRangeForSystemTimeZone(nationalRuleSet.displayRules, data, CertType.RECOVERY)
		assertNotNull(validityRange)
		assertEquals(validFrom, validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil, validityRange?.validUntil?.toLocalDate())
	}

	private fun getJsonNodeData(
		vaccination: DccCert,
		clock: Clock = Clock.systemUTC()
	): JsonNode {
		val ruleSetData = nationalRulesVerifier.getCertlogicData(vaccination, nationalRuleSet.valueSets, clock)
		return jacksonMapper.valueToTree<JsonNode>(ruleSetData)
	}

}