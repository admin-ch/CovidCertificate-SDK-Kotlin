package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import ch.admin.bag.covidcertificate.sdk.core.TestDataGenerator
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.PersonName
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

class CertTypeServiceTest {

	@Test
	fun testDccHolderIsCertificateLight() {
		val person = PersonName("FamilyName", "StandardizedFamilyName", "GivenName", "StandardizedGivenName")
		val dccHolder = CertificateHolder(ChLightCert("1.0.0", person, "1990-12-31"), "", null, null, null)
		assertTrue(dccHolder.containsChLightCert(), "DccHolder should contain a certificate light")

		val certificateType = CertTypeService.decode(dccHolder)
		assertEquals(CertType.LIGHT, certificateType)
	}

	@Test
	fun testDccHolderIsVaccination() {
		val dccCert = TestDataGenerator.generateVaccineCert(
			2,
			2,
			"ORG-100001699",
			"EU/1/21/1529",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			"J07BX03",
			LocalDate.now().minusDays(10).atStartOfDay(),
		)

		val certificateHolder = CertificateHolder(dccCert, "", null, null, null)
		assertTrue(certificateHolder.containsDccCert(), "CertificateHolder should contain a DccCert")

		val certificateType = CertTypeService.decode(certificateHolder)
		assertEquals(CertType.VACCINATION, certificateType)
	}

	@Test
	fun testDccHolderIsTest() {
		val dccCert = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofDays(-1)
		)

		val certificateHolder = CertificateHolder(dccCert, "", null, null, null)
		assertTrue(certificateHolder.containsDccCert(), "CertificateHolder should contain a DccCert")

		val certificateType = CertTypeService.decode(certificateHolder)
		assertEquals(CertType.TEST, certificateType)
	}

	@Test
	fun testDccHolderIsRecovery() {
		val dccCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val certificateHolder = CertificateHolder(dccCert, "", null, null, null)
		assertTrue(certificateHolder.containsDccCert(), "CertificateHolder should contain a DccCert")

		val certificateType = CertTypeService.decode(certificateHolder)
		assertEquals(CertType.RECOVERY, certificateType)
	}

	@Test
	fun testDccHolderIsOnlyOneType() {
		val testDccCert = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.NEGATIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofDays(-1)
		)

		val recoveryDccCert = TestDataGenerator.generateRecoveryCert(
			Duration.ofDays(-10),
			Duration.ofDays(180),
			Duration.ofDays(-20),
			AcceptanceCriteriasConstants.TARGET_DISEASE
		)

		val combinedDccCert = testDccCert.copy(pastInfections = recoveryDccCert.pastInfections)

		val certificateHolder = CertificateHolder(combinedDccCert, "", null, null, null)
		assertTrue(certificateHolder.containsDccCert(), "CertificateHolder should contain a DccCert")

		val certificateType = CertTypeService.decode(certificateHolder)
		assertNotNull(certificateType, "Certificate can  contain one or more of the three data sets (v, t or r)")
	}

}