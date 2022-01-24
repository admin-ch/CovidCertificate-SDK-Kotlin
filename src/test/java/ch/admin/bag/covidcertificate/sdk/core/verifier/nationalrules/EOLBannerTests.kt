package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules

import ch.admin.bag.covidcertificate.sdk.core.TestDataGenerator
import ch.admin.bag.covidcertificate.sdk.core.Vaccine
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.*
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EOLBannerTests {

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
	fun testinvalidFromFirstFebruary() {
		val clockToday = Clock.fixed(Instant.parse("2022-01-20T12:00:00Z"), ZoneId.systemDefault())
		val clockFirstFeb = Clock.fixed(Instant.parse("2022-02-01T12:00:00Z"), ZoneId.systemDefault())

		//vaccine case with 2/2
		var vaccinationDate = LocalDate.now(clockFirstFeb).atStartOfDay().minusDays(270)
		var vaccine = Vaccine.BIONTECH
		var vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		var data = getJsonNodeData(vaccination, null, clockToday)
		var bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidFromFirstFebruary")

		//vaccine case with Jansen
		vaccinationDate = LocalDate.now(clockFirstFeb).atStartOfDay().minusDays(270).minusDays(21)
		vaccine = Vaccine.JANSSEN
		vaccination = TestDataGenerator.generateVaccineCert(
			1,
			1,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		data = getJsonNodeData(vaccination, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidFromFirstFebruary")

		//recovery
		val firstTestResult = LocalDate.now(clockFirstFeb).minusDays(270)
		val validFrom = firstTestResult.plusDays(10)
		val validUntil = firstTestResult.plusDays(364)
		val recovery = TestDataGenerator.generateRecoveryCertFromDate(
			validFrom.atStartOfDay(),
			validUntil.atStartOfDay(),
			firstTestResult.atStartOfDay(),
			AcceptanceCriteriasConstants.TARGET_DISEASE

		)

		data = getJsonNodeData(recovery, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidFromFirstFebruary")

		//antigen
		val now = OffsetDateTime.now(clockFirstFeb).minusDays(270)
		val sampleCollectionTime = now
		val test = TestDataGenerator.generateTestCertFromDate(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)

		data = getJsonNodeData(test, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidFromFirstFebruary")

	}

	@Test
	fun testinvalidInThreeWeeks() {
		val clockToday = Clock.fixed(Instant.parse("2022-01-20T12:00:00Z"), ZoneId.systemDefault())
		val clockFirstFeb = Clock.fixed(Instant.parse("2022-02-01T12:00:00Z"), ZoneId.systemDefault())
		var vaccinationDate = LocalDate.now(clockFirstFeb).atStartOfDay().minusDays(270 - 1)

		var vaccine = Vaccine.BIONTECH
		var vaccination = TestDataGenerator.generateVaccineCert(
			2,
			2,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		var data = getJsonNodeData(vaccination, null, clockToday)
		var bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidInThreeWeeks")


		vaccinationDate = LocalDate.now(clockFirstFeb).atStartOfDay().minusDays(270).minusDays(21).plusDays(1)
		vaccine = Vaccine.JANSSEN
		vaccination = TestDataGenerator.generateVaccineCert(
			1,
			1,
			vaccine.manufacturer,
			vaccine.identifier,
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			vaccine.prophylaxis,
			vaccinationDate,
		)
		data = getJsonNodeData(vaccination, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidInThreeWeeks")

		//recovery
		val firstTestResult = LocalDate.now(clockFirstFeb).minusDays(270).plusDays(1)
		val validFrom = firstTestResult.plusDays(10)
		val validUntil = firstTestResult.plusDays(364)
		val recovery = TestDataGenerator.generateRecoveryCertFromDate(
			validFrom.atStartOfDay(),
			validUntil.atStartOfDay(),
			firstTestResult.atStartOfDay(),
			AcceptanceCriteriasConstants.TARGET_DISEASE

		)

		data = getJsonNodeData(recovery, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidInThreeWeeks")

		//antigen
		var now = OffsetDateTime.now(clockFirstFeb).minusDays(270).plusDays(1)
		var sampleCollectionTime = now
		var test = TestDataGenerator.generateTestCertFromDate(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)
		data = getJsonNodeData(test, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidInThreeWeeks")

		now = OffsetDateTime.now(clockFirstFeb).minusDays(365).plusDays(1)
		sampleCollectionTime = now
		test = TestDataGenerator.generateTestCertFromDate(
			TestType.MEDICAL_EXEMPTION.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Ausnahme",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)

		data = getJsonNodeData(test, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, "invalidInThreeWeeks")
	}

	@Test
	fun testNotInvalid() {
		val clockToday = Clock.fixed(Instant.parse("2022-01-20T12:00:00Z"), ZoneId.systemDefault())
		val clockFirstFeb = Clock.fixed(Instant.parse("2022-02-01T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clockFirstFeb).atStartOfDay().minusDays(270 - 15)

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
		var data = getJsonNodeData(vaccination, null, clockToday)
		var bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, null)

		//SERO
		var now = OffsetDateTime.now(clockFirstFeb).minusDays(90)
		var sampleCollectionTime = now
		var test = TestDataGenerator.generateTestCertFromDate(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Sero Positiv",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)

		data = getJsonNodeData(test, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, null)


		now = OffsetDateTime.now(clockFirstFeb).minusDays(365)
		sampleCollectionTime = now
		test = TestDataGenerator.generateTestCertFromDate(
			TestType.MEDICAL_EXEMPTION.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Ausnahme",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)

		data = getJsonNodeData(test, null, clockToday)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, null)
	}

	private fun getJsonNodeData(
		vaccination: DccCert,
		headers: CertLogicHeaders?,
		clock: Clock = Clock.systemUTC()
	): JsonNode {
		val ruleSetData = nationalRulesVerifier.getCertlogicData(vaccination, nationalRuleSet.valueSets, headers, clock)
		return jacksonMapper.valueToTree(ruleSetData)
	}
}