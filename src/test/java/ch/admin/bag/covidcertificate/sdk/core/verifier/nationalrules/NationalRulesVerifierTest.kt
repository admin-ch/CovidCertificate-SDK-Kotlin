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
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.extensions.isTargetDiseaseCorrect
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicPayload
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.utils.DateUtil
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.JsonDateTime
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

class NationalRulesVerifierTest {

	private lateinit var nationalRulesVerifier: NationalRulesVerifier
	private lateinit var nationalRuleSet: RuleSet
	private lateinit var utcClock: Clock

	@BeforeEach
	fun setup() {
		val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		val nationalRulesContent = this::class.java.classLoader.getResource("nationalrules.json")!!.readText()
		nationalRuleSet = moshi.adapter(RuleSet::class.java).fromJson(nationalRulesContent)!!

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

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, CertType.VACCINATION, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
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

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, CertType.VACCINATION, clock)
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

		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.VACCINATION, clock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.VACCINATION, clock)
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

		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, CertType.VACCINATION, clock)
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
		val result = nationalRulesVerifier.verify(cert, nationalRuleSet, CertType.VACCINATION, clock)
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

		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.VACCINATION, clock)
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

		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.VACCINATION, clock)
		assertTrue(result is CheckNationalRulesState.NOT_YET_VALID)

		result = result as CheckNationalRulesState.NOT_YET_VALID
		assertEquals(result.validityRange?.validFrom!!, expectedValidFrom)
	}

	@Test
	fun testVaccineUntilDatesSuccess() {
		val validDateFrom = LocalDate.of(2021, 1, 3).atStartOfDay()
		val validDateUntil = LocalDate.of(2022, 1, 2).atStartOfDay()

		val validCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			validDateFrom,
		)

		val payload = CertLogicPayload(validCert.pastInfections, validCert.tests, validCert.vaccinations)
		val clock: Clock = Clock.systemUTC()
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(nationalRuleSet.valueSets, validationClock, validationClockAtStartOfDay)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		val displayUntilDate: String? = nationalRuleSet.displayRules.find { it.id == "display-until-date" }?.logic
		assert(displayUntilDate != null)
		val untilDateLogic = jacksonMapper.readTree(displayUntilDate)
		val untilFrom = evaluate(untilDateLogic, data)
		val untilFromString = DateUtil.parseDate((untilFrom as JsonDateTime).temporalValue().toString())?.atStartOfDay()

		assertTrue(untilFromString == validDateUntil)
		val validResultTodayBeforeUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			Clock.fixed(Instant.parse("2021-06-30T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayBeforeUntilDate is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			Clock.fixed(Instant.parse("2021-07-01T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayEqualUntilDate is CheckNationalRulesState.SUCCESS)

		val invalidResultTodayAfterUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			Clock.fixed(Instant.parse("2022-01-03T12:00:00Z"), ZoneId.systemDefault())
		)
		assertTrue(invalidResultTodayAfterUntilDate is CheckNationalRulesState.NOT_VALID_ANYMORE)

		val invalidResultTodayBeforeFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.VACCINATION,
			Clock.fixed(Instant.parse("2021-01-02T12:00:00Z"), ZoneId.systemDefault())
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
		var result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.VACCINATION, clock)
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
		result = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.VACCINATION, clock)
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
		val result = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.TEST)
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
		val wrongResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.TEST, utcClock)
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

		val validRatResult = nationalRulesVerifier.verify(validRat, nationalRuleSet, CertType.TEST, utcClock)

		assertTrue(validRatResult is CheckNationalRulesState.SUCCESS)

		val validPcrResult = nationalRulesVerifier.verify(validPcr, nationalRuleSet, CertType.TEST, utcClock)
		assertTrue(validPcrResult is CheckNationalRulesState.SUCCESS)

		val invalidTestResult = nationalRulesVerifier.verify(invalidTest, nationalRuleSet, CertType.TEST, utcClock)
		if (invalidTestResult is CheckNationalRulesState.INVALID) {
			assertTrue(invalidTestResult.nationalRulesError == NationalRulesError.WRONG_TEST_TYPE)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testPcrTestsAreAlwaysAccepted() {
		var validTest = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1df097",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		var result = nationalRulesVerifier.verify(validTest, nationalRuleSet, CertType.TEST, utcClock)
		assertTrue(result is CheckNationalRulesState.SUCCESS)
	}

	@Test
	fun testPcrIsValidFor72h() {
		var validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-71),
			utcClock
		)
		var result = nationalRulesVerifier.verify(validPcr, nationalRuleSet, CertType.TEST)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		var invalidPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-72),
			utcClock
		)
		var invalid = nationalRulesVerifier.verify(invalidPcr, nationalRuleSet, CertType.TEST, utcClock)
		if (invalid is CheckNationalRulesState.NOT_VALID_ANYMORE) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testRatIsValidFor48h() {
		var validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-47),
			utcClock
		)
		var result = nationalRulesVerifier.verify(validRat, nationalRuleSet, CertType.TEST)
		assertTrue(result is CheckNationalRulesState.SUCCESS)

		var invalidPcr = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-48),
			utcClock
		)
		var invalid = nationalRulesVerifier.verify(invalidPcr, nationalRuleSet, CertType.TEST, utcClock)
		if (invalid is CheckNationalRulesState.NOT_VALID_ANYMORE) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	@Test
	fun testTestResultHasToBeNegative() {
		var validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		var validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)

		var invalidRat = nationalRulesVerifier.verify(validRat, nationalRuleSet, CertType.TEST, utcClock)
		var invalidPcr = nationalRulesVerifier.verify(validPcr, nationalRuleSet, CertType.TEST, utcClock)
		if (invalidRat is CheckNationalRulesState.INVALID &&
			invalidPcr is CheckNationalRulesState.INVALID
		) {
			assertTrue(invalidRat.nationalRulesError == NationalRulesError.POSITIVE_RESULT)
			assertTrue(invalidPcr.nationalRulesError == NationalRulesError.POSITIVE_RESULT)
		} else {
			assertFalse(true)
		}
	}



	@Test
	fun testSeroPositiv() {
		var validSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		var inValidSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)

		var inValidBeforeSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(24),
			utcClock
		)

		var inValidAfterSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-24*90),
			utcClock
		)


		var valid = nationalRulesVerifier.verify(validSeroPostiv, nationalRuleSet, CertType.TEST, utcClock)
		var invalid = nationalRulesVerifier.verify(inValidSeroPostiv, nationalRuleSet, CertType.TEST, utcClock)
		var invalidBefore = nationalRulesVerifier.verify(inValidBeforeSeroPostiv, nationalRuleSet, CertType.TEST, utcClock)
		var invalidAfter = nationalRulesVerifier.verify(inValidAfterSeroPostiv, nationalRuleSet, CertType.TEST, utcClock)

		if (valid is CheckNationalRulesState.SUCCESS
			&& (invalid is CheckNationalRulesState.INVALID && invalid.nationalRulesError == NationalRulesError.NEGATIVE_RESULT)
			&& invalidBefore is CheckNationalRulesState.NOT_YET_VALID
			&& invalidAfter is CheckNationalRulesState.NOT_VALID_ANYMORE) {
			assertTrue(true)
		} else {
			assertFalse(true)
		}
	}

	/// RECOVERY TESTS

	@Test
	fun testRecoveryDiseaseTargetedHasToBeSarsCoV2() {
		var validRecovery = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			utcClock
		)
		var invalidRecovery = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			"INVALID DISEASE",
			utcClock
		)

		var validRecoveryResult = nationalRulesVerifier.verify(validRecovery, nationalRuleSet, CertType.RECOVERY, utcClock)
		var invalidRecoveryResult = nationalRulesVerifier.verify(invalidRecovery, nationalRuleSet, CertType.RECOVERY, utcClock)
		assertTrue(validRecoveryResult is CheckNationalRulesState.SUCCESS)

		if (invalidRecoveryResult is CheckNationalRulesState.INVALID) {
			assertTrue(invalidRecoveryResult.nationalRulesError == NationalRulesError.WRONG_DISEASE_TARGET)
		}
	}

	@Test
	fun testRecoveryUntilDatesSuccess() {
		val firstTestResult = LocalDate.of(2021, 5, 8).atStartOfDay()
		val validDateFrom = firstTestResult.plusDays(10)
		val validDateUntil = firstTestResult.plusDays(364)

		val validCert = TestDataGenerator.generateRecoveryCertFromDate(
			validDateFrom = validDateFrom,
			validDateUntil = validDateUntil,
			firstTestResult = firstTestResult,
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val payload = CertLogicPayload(validCert.pastInfections, validCert.tests, validCert.vaccinations)
		val clock: Clock = Clock.systemUTC()
		val validationClock = ZonedDateTime.now(clock).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val validationClockAtStartOfDay =
			LocalDate.now(clock).atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
		val externalInfo = CertLogicExternalInfo(nationalRuleSet.valueSets, validationClock, validationClockAtStartOfDay)
		val ruleSetData = CertLogicData(payload, externalInfo)

		val jacksonMapper = ObjectMapper()
		jacksonMapper.setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)
		val displayFromDate: String? = nationalRuleSet.displayRules.find { it.id == "display-from-date" }?.logic
		assert(displayFromDate != null)
		val fromDateLogic = jacksonMapper.readTree(displayFromDate)
		val dateFrom = evaluate(fromDateLogic, data)
		val dateFromString = DateUtil.parseDate((dateFrom as JsonDateTime).temporalValue().toString(), ZoneId.of("UTC"))?.atStartOfDay()
		assertTrue(dateFromString == validDateFrom)

		val validResultBefore = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			Clock.fixed( validDateUntil.minusDays(1).toInstant(ZoneOffset.UTC), ZoneId.systemDefault()))
		assertTrue(validResultBefore is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualToUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			Clock.fixed(validDateUntil.toInstant(ZoneOffset.UTC), ZoneId.systemDefault())
		)
		assertTrue(validResultTodayEqualToUntilDate is CheckNationalRulesState.SUCCESS)

		val validResultTodayEqualToFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			Clock.fixed(validDateFrom.toInstant(ZoneOffset.UTC).plusMillis(1), ZoneId.of("UTC"))
		)
		assertTrue(validResultTodayEqualToFromDate is CheckNationalRulesState.SUCCESS)

		val invalidResultTodayIsAfterUntilDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			Clock.fixed(validDateUntil.plusDays(1).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))
		)
		assertTrue(invalidResultTodayIsAfterUntilDate is CheckNationalRulesState.NOT_VALID_ANYMORE)

		val invalidResultTodayIsBeforeFromDate = nationalRulesVerifier.verify(
			validCert,
			nationalRuleSet,
			CertType.RECOVERY,
			Clock.fixed(validDateFrom.minusDays(1).toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))
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

		val validResult = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.RECOVERY, utcClock)
		val invalidResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.RECOVERY, utcClock)

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

		val validResult = nationalRulesVerifier.verify(validCert, nationalRuleSet, CertType.RECOVERY, utcClock)
		val invalidResult = nationalRulesVerifier.verify(invalidCert, nationalRuleSet, CertType.RECOVERY, utcClock)

		assertTrue(validResult is CheckNationalRulesState.SUCCESS)
		assertTrue(invalidResult is CheckNationalRulesState.NOT_YET_VALID)
	}
}