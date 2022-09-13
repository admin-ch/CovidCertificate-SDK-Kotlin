package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules

import ch.admin.bag.covidcertificate.sdk.core.TestDataGenerator
import ch.admin.bag.covidcertificate.sdk.core.Vaccine
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.squareup.moshi.Moshi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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
	fun testNotInvalid() {
		val clockFirstFeb = Clock.fixed(Instant.parse("2022-02-01T12:00:00Z"), ZoneId.systemDefault())
		val vaccinationDate = LocalDate.now(clockFirstFeb).atStartOfDay().minusDays(270).minusDays(1)

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
		var data = getJsonNodeData(vaccination, null, clockFirstFeb)
		var bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, null)

		//SERO
		var now = OffsetDateTime.now(clockFirstFeb).minusDays(90).minusDays(1)
		var sampleCollectionTime = now
		var test = TestDataGenerator.generateTestCertFromDate(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Sero Positiv",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)

		data = getJsonNodeData(test, null, clockFirstFeb)
		bannerID = displayValidityCalculator.getEolBannerIdentifier(nationalRuleSet.displayRules, data)
		Assertions.assertEquals(bannerID, null)


		now = OffsetDateTime.now(clockFirstFeb).minusDays(365).minusDays(1)
		sampleCollectionTime = now
		test = TestDataGenerator.generateTestCertFromDate(
			TestType.MEDICAL_EXEMPTION.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Ausnahme",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			sampleCollectionTime
		)

		data = getJsonNodeData(test, null, clockFirstFeb)
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