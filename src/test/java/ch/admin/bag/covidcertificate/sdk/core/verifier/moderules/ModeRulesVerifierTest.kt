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

import ch.admin.bag.covidcertificate.sdk.core.LT1_A
import ch.admin.bag.covidcertificate.sdk.core.TestDataGenerator
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.sdk.core.getCertificateLightTestKey
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.WalletSuccessState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.*
import ch.admin.bag.covidcertificate.sdk.core.utils.DEFAULT_DISPLAY_RULES_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.utils.prettyPrint
import ch.admin.bag.covidcertificate.sdk.core.verifier.CertificateVerifier
import ch.admin.bag.covidcertificate.sdk.core.verifier.VerificationType
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ModeRulesVerifierTest {

	private lateinit var modeRulesVerifier: ModeRulesVerifier
	private lateinit var ruleSet: RuleSet
	private lateinit var utcClock: Clock

	private lateinit var certificateVerifier: CertificateVerifier

	@BeforeEach
	fun setup() {
		val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		val nationalRulesContent = this::class.java.classLoader.getResource("nationalrules.json")!!.readText()
		ruleSet = moshi.adapter(RuleSet::class.java).fromJson(nationalRulesContent)!!

		modeRulesVerifier = ModeRulesVerifier()
		utcClock = Clock.systemUTC()
		certificateVerifier = CertificateVerifier()
	}

	@Test
	fun testTwoGInvalidForLightCertificate() {

		val mode = "TWO_G"
		val certificateHolder = decodeCertificate(LT1_A)
		val trustList = createTrustList(listOf(getCertificateLightTestKey()))

		runBlocking {
			val verificationState = certificateVerifier.verify(certificateHolder, trustList, setOf(mode), VerificationType.WALLET)
			assertTrue(verificationState is VerificationState.SUCCESS)

			verificationState as VerificationState.SUCCESS
			val walletSuccessState = verificationState.successState as WalletSuccessState
			assertTrue(!walletSuccessState.modeValidity.first().isModeValid)
		}
	}

	@Test
	fun testTwoGInvalidForTests() {

		val mode = "TWO_G"

		val validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val validSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)

		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val headers = CertLogicHeaders(
			iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
			exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
			false,
			mode
		)
		val modeValidityRat = modeRulesVerifier.verify(validRat, ruleSet, headers, mode, utcClock)
		val modeValidityPcr = modeRulesVerifier.verify(validPcr, ruleSet, headers, mode, utcClock)
		val modeValiditySeroPositiv = modeRulesVerifier.verify(validSeroPostiv, ruleSet, headers, mode, utcClock)

		assertFalse(modeValidityRat.isModeValid)
		assertFalse(modeValidityPcr.isModeValid)
		assertFalse(modeValiditySeroPositiv.isModeValid)
	}


	@Test
	fun testThreeGValidForTests() {
		val mode = "THREE_G"
		val validRat = TestDataGenerator.generateTestCert(
			TestType.RAT.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val validPcr = TestDataGenerator.generateTestCert(
			TestType.PCR.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"Nucleic acid amplification with probe detection",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)
		val validSeroPostiv = TestDataGenerator.generateTestCert(
			TestType.SEROLOGICAL.code,
			AcceptanceCriteriasConstants.POSITIVE_CODE,
			"1232",
			AcceptanceCriteriasConstants.TARGET_DISEASE,
			Duration.ofHours(-10),
			utcClock
		)

		val iat = Instant.parse("2021-06-05T12:00:00Z")
		val exp = iat.plusSeconds(30 * 24 * 60 * 60L)
		val headers = CertLogicHeaders(
			iat.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
			exp.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER),
			false,
			mode
		)
		val modeValidityRat = modeRulesVerifier.verify(validRat, ruleSet, headers, mode, utcClock)
		val modeValidityPcr = modeRulesVerifier.verify(validPcr, ruleSet, headers, mode, utcClock)
		val modeValiditySeroPositiv = modeRulesVerifier.verify(validSeroPostiv, ruleSet, headers, mode, utcClock)
		assertTrue(modeValidityRat.isModeValid)
		assertTrue(modeValidityPcr.isModeValid)
		assertTrue(modeValiditySeroPositiv.isModeValid)
	}

	private fun decodeCertificate(qrCodeData: String): CertificateHolder {
		val decodeState = CertificateDecoder.decode(qrCodeData)
		assertTrue(decodeState is DecodeState.SUCCESS)

		return (decodeState as DecodeState.SUCCESS).certificateHolder
	}

	private fun createTrustList(
		signingKeys: List<Jwk> = emptyList(),
		revokedKeyIds: List<String> = emptyList()
	): TrustList {
		return TrustList(
			Jwks(signingKeys),
			RevokedCertificatesInMemoryImpl(revokedKeyIds),
			ruleSet.copy(rules = emptyList()) // Use national ruleset from test resources to access the validity offsets, but not process any rules
		)
	}

}