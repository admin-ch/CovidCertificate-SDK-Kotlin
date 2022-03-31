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
import ch.admin.bag.covidcertificate.sdk.core.extensions.isTargetDiseaseCorrect
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicPayload
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.utils.DEFAULT_DISPLAY_RULES_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.utils.DateUtil
import ch.admin.bag.covidcertificate.sdk.core.utils.prettyPrint
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.JsonDateTime
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class NationalRulesVerifierTest {

	private lateinit var nationalRulesVerifier: NationalRulesVerifier
	private lateinit var nationalRuleSet: RuleSet
	private lateinit var foreignRuleSet: RuleSet
	private lateinit var utcClock: Clock

	@BeforeEach
	fun setup() {
		val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		val ruleSetAdapter = moshi.adapter(RuleSet::class.java)

		val nationalRulesContent = this::class.java.classLoader.getResource("nationalrules.json")!!.readText()
		nationalRuleSet = requireNotNull(ruleSetAdapter.fromJson(nationalRulesContent))

		val foreignRulesContent = this::class.java.classLoader.getResource("foreignRules_de.json")!!.readText()
		foreignRuleSet = requireNotNull(ruleSetAdapter.fromJson(foreignRulesContent))

		nationalRulesVerifier = NationalRulesVerifier()
		utcClock = Clock.systemUTC()
	}

	/// VACCINE TESTS

	@Test
	fun testVaccineDiseaseTargetedHasToBeSarsCoV2() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).minusDays(10).atStartOfDay()

		val cert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)

		val result = nationalRulesVerifier.verify(
			cert,
			nationalRuleSet,
			CertType.VACCINATION,
			null,
			clock = clock
		)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testCOVAXIN_TouristenZertifikate() {
		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val clock = Clock.fixed(iat, ZoneId.systemDefault())
		val iatDate = LocalDate.now(clock).atStartOfDay()
		val vaccinationDate = iatDate.minusDays(180)
		val vaccine = Vaccine.TOURIST_COVAXIN_T
		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		val result = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				false,
				null
			),
			clock = clock
		)
		assert(result is CheckNationalRulesState.SUCCESS)

		val inValidVaccinationDate = iatDate.minusDays(365)
		val inValidCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			inValidVaccinationDate,
		)

		val invalidResult = nationalRulesVerifier.verify(
			inValidCert,
			nationalRuleSet,
			CertType.VACCINATION,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				false,
				null
			),
			clock = clock
		)
		assert(invalidResult is CheckNationalRulesState.NOT_VALID_ANYMORE)
		val inValidFutureVaccinationDate = iatDate.plusDays(1)
		val inValidFutureCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			inValidFutureVaccinationDate,
		)

		val invalidFutureResult = nationalRulesVerifier.verify(
			inValidFutureCert,
			nationalRuleSet,
			CertType.VACCINATION,
			CertLogicHeaders(
				iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
				false,
				null
			),
			clock = clock
		)
		assert(invalidFutureResult is CheckNationalRulesState.NOT_YET_VALID)
	}


	@Test
	fun testVaccineDiseaseTargetedIsNotSarsCoV2() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).minusDays(10).atStartOfDay()

		val cert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			"840539009",
			"J07BX03",
			vaccinationDate,
		)

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.INVALID)
	}

	@Test
	fun testVaccineMustBeInWhitelist() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).minusDays(10).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"1119349007",
			vaccinationDate,
		)
		val invalidCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529/INVALID",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"1119349007",
			vaccinationDate,
		)

		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.INVALID && result.nationalRulesError == NationalRulesError.NO_VALID_PRODUCT)
	}

	@Test
	fun test2of2VaccineIsValidToday() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).atStartOfDay()

		val cert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccine1of1WithPreviousInfectionsIsValidToday() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clock).atStartOfDay()

		val cert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)
		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testVaccine1of1IsValidAfter21Days() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val nowDate = LocalDate.now(clock).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001417",
			"EU/1/20/1525",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate.minusDays(21),
		)

		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateVaccineCert(
			1,
			1,
			"ORG-100001417",
			"EU/1/20/1525",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate.minusDays(20),
		)
		val expectedValidFrom = LocalDate.now(clock).plusDays(1).atStartOfDay()

		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.NOT_YET_VALID)

		result = result as CheckNationalRulesState.NOT_YET_VALID
		assertEquals(result.validityRange?.validFrom!!, expectedValidFrom)
	}

	@Test
	fun testVaccineUntilDatesSuccess() {
		val validDateFrom = LocalDate.of(2021, 1, 3).atStartOfDay()
		val validDateUntil = validDateFrom.plusDays(269)

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			validDateFrom,
		)

		val payload = CertLogicPayload(validCert.pastInfections, validCert.tests, validCert.vaccinations, null)
		val clock: Clock = Clock.systemUTC()
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(nationalRuleSet.valueSets, validationClock, validationClockAtStartOfDay)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		val displayUntilDate: String? = nationalRuleSet.displayRules?.find { it.id == "display-until-date" }?.logic
		assert(displayUntilDate != null)
		val untilDateLogic = jacksonMapper.readTree(displayUntilDate)
		val untilFrom = evaluate(untilDateLogic, data)
		val untilFromString = DateUtil.parseDate((untilFrom as JsonDateTime).temporalValue().toString())?.atStartOfDay()

		assertTrue(untilFromString == validDateUntil)
		val validResultTodayBeforeUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			null,
			clock = Clock.fixed(Instant.parse("2021-06-30T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayBeforeUntilDate is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			null,
			clock = Clock.fixed(Instant.parse("2021-07-01T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayEqualUntilDate is CheckNationalRulesState.SUCCESS)

		val invalidResultTodayAfterUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			null,
			clock = Clock.fixed(Instant.parse("2022-01-03T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(invalidResultTodayAfterUntilDate is CheckNationalRulesState.NOT_VALID_ANYMORE)

		val invalidResultTodayBeforeFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			null,
			clock = Clock.fixed(Instant.parse("2021-01-02T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(invalidResultTodayBeforeFromDate is CheckNationalRulesState.NOT_YET_VALID)
	}

	@Test
	fun testWeNeedAllShots() {
		val clock = Clock.fixed(Instant.parse("2021-05-25T12:00:00Z"), ZoneId.systemDefault())
		val nowDate = LocalDate.now(clock).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate,
		)
		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateVaccineCert(
			1,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			nowDate,
		)
		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.VACCINATION, null, clock = clock)
		assertTrue(result is CheckNationalRulesState.INVALID)

		result = result as CheckNationalRulesState.INVALID
		assertEquals(result.nationalRulesError, NationalRulesError.NOT_FULLY_PROTECTED)
	}

	/// TEST TESTS

	@Test
	fun testTestDiseaseTargetedHasToBeSarsCoV2() {
		val duration = Duration.ofHours(-10)
		val validCert = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			duration,
			utcClock
		)
		assertTrue(validCert.tests!!.first().isTargetDiseaseCorrect())
		val result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.TEST, null)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidCert = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			"01123",
			duration,
			utcClock
		)

		assertFalse(invalidCert.tests!!.first().isTargetDiseaseCorrect())
		val wrongResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		assertTrue(wrongResult is CheckNationalRulesState.INVALID)
	}

	@Test
	fun testTypeHasToBePcrOrRat() {
		val validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val invalidTest = TestDataGenerator.generateTestCert(
			"INVALID_TEST_TYPE",
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)

		val validRatResult = nationalRulesVerifier.verify(validRat, nationalRuleSet, CertType.TEST, null, clock = utcClock)

		assertTrue(validRatResult is CheckNationalRulesState.SUCCESS)

		val validPcrResult = nationalRulesVerifier.verify(validPcr, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		assertTrue(validPcrResult is CheckNationalRulesState.SUCCESS)

		val invalidTestResult = nationalRulesVerifier.verify(invalidTest, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		if (invalidTestResult is CheckNationalRulesState.INVALID) {
			assertTrue(invalidTestResult.nationalRulesError == NationalRulesError.WRONG_TEST_TYPE)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testPcrTestsAreAlwaysAccepted() {
		val validTest = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1df097",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val result = nationalRulesVerifier.verify(validTest, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testPcrIsValidFor72h() {
		val validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-71),
			utcClock
		)
		val result = nationalRulesVerifier.verify(validPcr, nationalRuleSet, CertType.TEST, null)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-72),
			utcClock
		)
		val invalid = nationalRulesVerifier.verify(invalidPcr, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		if (invalid is CheckNationalRulesState.NOT_VALID_ANYMORE) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testRatIsValidFor48h() {
		val validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-23),
			utcClock
		)
		val result = nationalRulesVerifier.verify(validRat, nationalRuleSet, CertType.TEST, null)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		val invalidPcr = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-24),
			utcClock
		)
		val invalid = nationalRulesVerifier.verify(invalidPcr, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		if (invalid is CheckNationalRulesState.NOT_VALID_ANYMORE) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testTestResultHasToBeNegative() {

		val validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)

		val invalidPcr = nationalRulesVerifier.verify(validPcr, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		if (invalidPcr is CheckNationalRulesState.INVALID) {
			assertTrue(invalidPcr.nationalRulesError == NationalRulesError.POSITIVE_RESULT)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testPositiveRatTestShouldBeValidAfter10Days() {
		val validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofDays(-10),
			utcClock
		)

		val validRatResult = nationalRulesVerifier.verify(validRat, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		assertTrue(validRatResult is CheckNationalRulesState.SUCCESS)


		val invalidRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofDays(-9),
			utcClock
		)

		val invalidRatResult = nationalRulesVerifier.verify(invalidRat, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		assertTrue(invalidRatResult is CheckNationalRulesState.NOT_YET_VALID)
	}


	@Test
	fun testSeroPositiv() {
		val validSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val inValidSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)

		val inValidBeforeSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(24),
			utcClock
		)

		val inValidAfterSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-24 * 90L),
			utcClock
		)


		val valid = nationalRulesVerifier.verify(validSeroPostiv, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		val invalid = nationalRulesVerifier.verify(inValidSeroPostiv, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		val invalidBefore = nationalRulesVerifier.verify(inValidBeforeSeroPostiv, nationalRuleSet, CertType.TEST, null, clock = utcClock)
		val invalidAfter = nationalRulesVerifier.verify(inValidAfterSeroPostiv, nationalRuleSet, CertType.TEST, null, clock = utcClock)

		if (valid is CheckNationalRulesState.SUCCESS
			&& (invalid is CheckNationalRulesState.INVALID && invalid.nationalRulesError == NationalRulesError.NEGATIVE_RESULT)
			&& invalidBefore is CheckNationalRulesState.NOT_YET_VALID
			&& invalidAfter is CheckNationalRulesState.NOT_VALID_ANYMORE
		) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	/// RECOVERY TESTS

	@Test
	fun testRecoveryDiseaseTargetedHasToBeSarsCoV2() {
		val validRecovery = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)

		val validRecoveryResult =
			nationalRulesVerifier.verify(validRecovery, nationalRuleSet, CertType.RECOVERY, null, clock = utcClock)
		assertTrue(validRecoveryResult is CheckNationalRulesState.SUCCESS)

		val invalidRecovery = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			"INVALID DISEASE",
			utcClock
		)

		val invalidRecoveryResult =
			nationalRulesVerifier.verify(invalidRecovery, nationalRuleSet, CertType.RECOVERY, null, clock = utcClock)
		if (invalidRecoveryResult is CheckNationalRulesState.INVALID) {
			assertTrue(invalidRecoveryResult.nationalRulesError == NationalRulesError.WRONG_DISEASE_TARGET)
		}
	}

	@Test
	fun testRecoveryUntilDatesSuccess() {
		val firstTestResult = LocalDate.of(2021, 5, 8).atStartOfDay()
		val validDateFrom = firstTestResult.plusDays(10)
		val validDateUntil = firstTestResult.plusDays(179)

		val validCert = TestDataGenerator.generateRecoveryCertFromDate(
			validDateFrom = validDateFrom,
			validDateUntil = validDateUntil,
			firstTestResult = firstTestResult,
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val payload = CertLogicPayload(validCert.pastInfections, validCert.tests, validCert.vaccinations, null)
		val clock: Clock = Clock.systemUTC()
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(nationalRuleSet.valueSets, validationClock, validationClockAtStartOfDay)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		val displayFromDate: String? = nationalRuleSet.displayRules?.find { it.id == "display-from-date" }?.logic
		assert(displayFromDate != null)
		val fromDateLogic = jacksonMapper.readTree(displayFromDate)
		val dateFrom = evaluate(fromDateLogic, data)
		val dateFromString =
			DateUtil.parseDate((dateFrom as JsonDateTime).temporalValue().toString(), ZoneId.of("UTC"))?.atStartOfDay()
		assertTrue(dateFromString == validDateFrom)

		val validResultBefore = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			null,
			clock = Clock.fixed(validDateUntil.minusDays(1).toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
		)
		assertTrue(validResultBefore is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualToUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			null,
			clock = Clock.fixed(validDateUntil.toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayEqualToUntilDate is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualToFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			null,
			clock = Clock.fixed(validDateFrom.toInstant(ZoneOffset.UTC).plusMillis(1), ZoneId.of("UTC"))
		)
		assertTrue(validResultTodayEqualToFromDate is CheckNationalRulesState.SUCCESS)

		val invalidResultTodayIsAfterUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			null,
			clock = Clock.fixed(validDateUntil.plusDays(1).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))
		)
		assertTrue(invalidResultTodayIsAfterUntilDate is CheckNationalRulesState.NOT_VALID_ANYMORE)

		val invalidResultTodayIsBeforeFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			null,
			clock = Clock.fixed(validDateFrom.minusDays(1).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))
		)
		assertTrue(invalidResultTodayIsBeforeFromDate is CheckNationalRulesState.NOT_YET_VALID)
	}

	@Test
	fun testCertificateIsValidFor365DaysAfter() {
		val validCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-179),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)
		val invalidCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-365),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)

		val validResult = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.RECOVERY, null, clock = utcClock)
		val invalidResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.RECOVERY, null, clock = utcClock)

		assertTrue(validResult is CheckNationalRulesState.SUCCESS)
		assertTrue(invalidResult is CheckNationalRulesState.NOT_VALID_ANYMORE)
	}

	@Test
	fun testTestIsOnlyValid10DaysAfterTestResult() {
		val validCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-10),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)
		val invalidCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-9),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)

		val validResult = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.RECOVERY, null, clock = utcClock)
		val invalidResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.RECOVERY, null, clock = utcClock)

		assertTrue(validResult is CheckNationalRulesState.SUCCESS)
		assertTrue(invalidResult is CheckNationalRulesState.NOT_YET_VALID)
	}
	
	@Test
	fun testNationalRulesAgainstDateTooFarInTheFuture() {
		val validCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(0),
			Duration.ofDays(-10),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)
		
		val checkDate = LocalDateTime.of(3000, 12, 31, 12, 34, 56)

		val result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.RECOVERY, null, checkDate, clock = utcClock)
		assertTrue(result is CheckNationalRulesState.INVALID)
		assertTrue((result as CheckNationalRulesState.INVALID).nationalRulesError == NationalRulesError.NO_VALID_RULES_FOR_SPECIFIC_DATE)
	}

	@Test
	fun testNationalRulesAgainstSpecificDate() {
		val checkDate = LocalDateTime.now()

		// Vaccination
		val vaccinationDate = LocalDate.now(utcClock).atStartOfDay()

		val vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			vaccinationDate,
		)

		val vaccinationResult = nationalRulesVerifier.verify(vaccination, nationalRuleSet, CertType.VACCINATION, null, checkDate, false, utcClock)
		assertTrue(vaccinationResult is CheckNationalRulesState.SUCCESS)

		// Tests
		val validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-71),
			utcClock
		)
		val pcrResult = nationalRulesVerifier.verify(validPcr, nationalRuleSet, CertType.TEST, null, checkDate, false, utcClock)
		assertTrue(pcrResult is CheckNationalRulesState.SUCCESS)

		val validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-23),
			utcClock
		)
		val ratResult = nationalRulesVerifier.verify(validRat, nationalRuleSet, CertType.TEST, null, checkDate, false, utcClock)
		assertTrue(ratResult is CheckNationalRulesState.SUCCESS)

		// Recovery
		val validRecovery = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)

		val validRecoveryResult =
			nationalRulesVerifier.verify(validRecovery, nationalRuleSet, CertType.RECOVERY, null, checkDate, clock = utcClock)
		assertTrue(validRecoveryResult is CheckNationalRulesState.SUCCESS)
	}

	/// FOREIGN RULES TESTS

	@Test
	fun testPositiveRatShouldNotBeValidInGermany() {
		val positiveRatTest = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofDays(-10),
			utcClock
		)

		val resultSwitzerland = nationalRulesVerifier.verify(
			positiveRatTest,
			nationalRuleSet,
			CertType.TEST,
			null,
			isForeignRulesCheck = false,
			clock = utcClock
		)
		assertTrue(resultSwitzerland is CheckNationalRulesState.SUCCESS)

		val resultGermany = nationalRulesVerifier.verify(
			positiveRatTest,
			foreignRuleSet,
			CertType.TEST,
			null,
			isForeignRulesCheck = true,
			clock = utcClock
		)
		assertTrue(resultGermany is CheckNationalRulesState.INVALID)
	}


}