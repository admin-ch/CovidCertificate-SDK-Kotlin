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
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.utils.DEFAULT_DISPLAY_RULES_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.utils.prettyPrint
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
class DisplayValidityCalculatorTest {

	private lateinit var displayValidityCalculator: DisplayValidityCalculator
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

		displayValidityCalculator = DisplayValidityCalculator()
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
		val data = getJsonNodeData(vaccination, null, clock)
		val validityRange = displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)
		val validFrom = vaccinationDate.plusDays(21)
		val validUntil = vaccinationDate.plusDays(365 + 21)
		assertEquals(validFrom.toLocalDate(), validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil.toLocalDate(), validityRange?.validUntil?.toLocalDate())
	}

	@Test
	fun testOneDoseWith2InjectionsVaccinationValidityRange() {
		val clock = Clock.fixed(Instant.parse("2021-06-05T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).atStartOfDay()

		val vaccine = Vaccine.JANSSEN
		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			1,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		val data = getJsonNodeData(vaccination, null, clock)
		val validityRange = displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)
		val validFrom = vaccinationDate
		val validUntil = vaccinationDate.plusDays(364)
		assertEquals(validFrom.toLocalDate(), validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil.toLocalDate(), validityRange?.validUntil?.toLocalDate())
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
		val data = getJsonNodeData(vaccination, null, clock)
		val validityRange = displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)


		val validFrom = vaccinationDate
		val validUntil = vaccinationDate.plusDays(364)
		assertEquals(validFrom.toLocalDate(), validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil.toLocalDate(), validityRange?.validUntil?.toLocalDate())
	}

	@Test
	fun testBBIBP_CORV_TOURIST_ZertifikateValidityRange() {
		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val clock = Clock.fixed(iat, ZoneId.systemDefault())
		val iatDate = LocalDate.now(clock).atStartOfDay()
		val vaccinationDate = iatDate.minusDays(180)
		val vaccine = Vaccine.TOURIST_BBIBP_CORV_T
		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)

		val data = getJsonNodeData(
			vaccination,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
			),
			clock
		)
		val validityRange = displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)

		val validFrom = iatDate
		val validUntil = iatDate.plusDays(29)
		assertEquals(validFrom.toLocalDate(), validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil.toLocalDate(), validityRange?.validUntil?.toLocalDate())
	}
	@Test
	fun testCORONAVAC_TouristenZertifikateValidityRange() {
		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val clock = Clock.fixed(iat, ZoneId.systemDefault())
		val iatDate = LocalDate.now(clock).atStartOfDay()
		val vaccinationDate = iatDate.minusDays(180)
		val vaccine = Vaccine.TOURIST_CORONAVAC_T
		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)

		val data = getJsonNodeData(
			vaccination,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
			),
			clock
		)
		val validityRange = displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)

		val validFrom = iatDate
		val validUntil = iatDate.plusDays(29)
		assertEquals(validFrom.toLocalDate(), validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil.toLocalDate(), validityRange?.validUntil?.toLocalDate())
	}

	@Test
	fun testCOVAXIN_TouristenZertifikateValidityRange() {
		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val clock = Clock.fixed(iat, ZoneId.systemDefault())
		val iatDate = LocalDate.now(clock).atStartOfDay()
		val vaccinationDate = iatDate.minusDays(180)
		val vaccine = Vaccine.TOURIST_COVAXIN_T
		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)

		val data = getJsonNodeData(
			vaccination,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
			),
			clock
		)
		val validityRange = displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(
			nationalRuleSet.displayRules,
			data,
			CertType.VACCINATION
		)
		assertNotNull(validityRange)

		val validFrom = iatDate
		val validUntil = iatDate.plusDays(29)
		assertEquals(validFrom.toLocalDate(), validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil.toLocalDate(), validityRange?.validUntil?.toLocalDate())
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

		val data = getJsonNodeData(test, null, utcClock)
		val validityRange =
			displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(nationalRuleSet.displayRules, data, CertType.TEST)
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
		val data = getJsonNodeData(test, null, utcClock)
		val validityRange =
			displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(nationalRuleSet.displayRules, data, CertType.TEST)
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
		val validUntil = firstTestResult.plusDays(364)
		val recovery = TestDataGenerator.generateRecoveryCertFromDate(
			validFrom.atStartOfDay(),
			validUntil.atStartOfDay(),
			firstTestResult.atStartOfDay(),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)
		val data = getJsonNodeData(recovery, null, utcClock)
		val validityRange =
			displayValidityCalculator.getDisplayValidityRangeForSystemTimeZone(nationalRuleSet.displayRules, data, CertType.RECOVERY)
		assertNotNull(validityRange)
		assertEquals(validFrom, validityRange?.validFrom?.toLocalDate())
		assertEquals(validUntil, validityRange?.validUntil?.toLocalDate())
	}

	private fun getJsonNodeData(
		vaccination: DccCert,
		headers: CertLogicHeaders?,
		clock: Clock = Clock.systemUTC()
	): JsonNode {
		val ruleSetData = nationalRulesVerifier.getCertlogicData(vaccination, nationalRuleSet.valueSets, headers, clock)
		return jacksonMapper.valueToTree<JsonNode>(ruleSetData)
	}



	@Test
	fun testVaccineonlyVaildinCH() {
		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val clock = Clock.fixed(iat, ZoneId.systemDefault())
		val iatDate = LocalDate.now(clock).atStartOfDay()
		val vaccinationDate = iatDate.minusDays(180)
		val vaccine = Vaccine.TOURIST_BBIBP_CORV_T
		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)

		val data = getJsonNodeData(
			vaccination,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
			),
			clock
		)
		val isValidInSwizterland = displayValidityCalculator.isOnlyValidInSwitzerland(
			nationalRuleSet.displayRules,
			data
		)
		assertTrue(isValidInSwizterland)

	}

	@Test
	fun testVaccineIsValidEveryWhere() {
		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val clock = Clock.fixed(iat, ZoneId.systemDefault())
		val iatDate = LocalDate.now(clock).atStartOfDay()
		val vaccinationDate = iatDate.minusDays(180)
		val vaccine = Vaccine.JANSSEN
		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)

		val data = getJsonNodeData(
			vaccination,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
			),
			clock
		)
		val isValidInSwizterland = displayValidityCalculator.isOnlyValidInSwitzerland(
			nationalRuleSet.displayRules,
			data
		)
		assertFalse(isValidInSwizterland)

	}

}