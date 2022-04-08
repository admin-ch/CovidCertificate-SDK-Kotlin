/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.verifier

import ch.admin.bag.covidcertificate.sdk.core.HC1_A
import ch.admin.bag.covidcertificate.sdk.core.LT1_A
import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.data.moshi.RawJsonStringAdapter
import ch.admin.bag.covidcertificate.sdk.core.decoder.CertificateDecoder
import ch.admin.bag.covidcertificate.sdk.core.getCertificateLightTestKey
import ch.admin.bag.covidcertificate.sdk.core.getHardcodedSigningKeys
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckRevocationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckSignatureState
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.SuccessState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwk
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificatesInMemoryImpl
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.NationalRulesError
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CertificateVerifierTest {

	private lateinit var nationalRuleSet: RuleSet
	private lateinit var certificateVerifier: CertificateVerifier

	@BeforeEach
	fun setup() {
		val moshi = Moshi.Builder().add(RawJsonStringAdapter()).build()
		val nationalRulesContent = this::class.java.classLoader.getResource("nationalrules.json")!!.readText()
		nationalRuleSet = moshi.adapter(RuleSet::class.java).fromJson(nationalRulesContent)!!

		certificateVerifier = CertificateVerifier()
	}

	@Test
	fun testFullCertificateInvalidSignature() {
		val certificateHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList()

		runBlocking {
			val verificationState = certificateVerifier.verify(certificateHolder, trustList, setOf("THREE_G"))
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SUCCESS)

			assertEquals(
				CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_RULES_FOR_SPECIFIC_DATE, false),
				invalidState.nationalRulesState
			)
		}
	}

	@Test
	fun testFullCertificateWrongSignature() {
		val certificateHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList(getHardcodedSigningKeys("abn"))

		runBlocking {
			val verificationState = certificateVerifier.verify(certificateHolder, trustList, setOf("THREE_G"))
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SUCCESS)

			assertEquals(
				CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_RULES_FOR_SPECIFIC_DATE, false),
				invalidState.nationalRulesState
			)
		}
	}

	@Test
	fun testFullCertificateRevocation() {
		val certificateHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList(
			signingKeys = getHardcodedSigningKeys("dev"),
			revokedKeyIds = listOf("01:CH:42A272C9E1CAA43D934142C9")
		)

		runBlocking {
			val verificationState = certificateVerifier.verify(certificateHolder, trustList, setOf("THREE_G"))
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.SUCCESS)

			assertTrue(invalidState.revocationState is CheckRevocationState.INVALID)
			assertEquals(
				ErrorCodes.REVOCATION_REVOKED,
				(invalidState.revocationState as CheckRevocationState.INVALID).revocationErrorCode
			)

			assertEquals(
				CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_RULES_FOR_SPECIFIC_DATE, false),
				invalidState.nationalRulesState
			)
		}
	}

	@Test
	fun testFullCertificateValid() {
		val certificateHolder = decodeCertificate(HC1_A)
		val trustList = createTrustList(getHardcodedSigningKeys("dev"))

		runBlocking {
			// This certificate should be valid, but since the custom trustlist we pass in has an empty ruleset, it will now return NO_VALID_RULES_FOR_SPECIFIC_DATE instead of SUCCESS
			val verificationState = certificateVerifier.verify(certificateHolder, trustList, setOf("THREE_G"))
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.SUCCESS)
			assertTrue(invalidState.revocationState is CheckRevocationState.SUCCESS)

			assertEquals(
				CheckNationalRulesState.INVALID(NationalRulesError.NO_VALID_RULES_FOR_SPECIFIC_DATE, false),
				invalidState.nationalRulesState
			)
		}
	}

	@Test
	fun testCertificateLightInvalidSignature() {
		val certificateHolder = decodeCertificate(LT1_A)
		val trustList = createTrustList()

		runBlocking {
			val verificationState = certificateVerifier.verify(certificateHolder, trustList, setOf("THREE_G"))
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SKIPPED)
			assertTrue(invalidState.nationalRulesState is CheckNationalRulesState.SUCCESS)
		}
	}

	@Test
	fun testCertificateLightWrongSignature() {
		val certificateHolder = decodeCertificate(LT1_A)
		val trustList = createTrustList(getHardcodedSigningKeys("dev"))

		runBlocking {
			val verificationState = certificateVerifier.verify(certificateHolder, trustList, setOf("THREE_G"))
			assertTrue(verificationState is VerificationState.INVALID)

			val invalidState = verificationState as VerificationState.INVALID
			assertTrue(invalidState.signatureState is CheckSignatureState.INVALID)
			assertEquals(
				ErrorCodes.SIGNATURE_COSE_INVALID,
				(invalidState.signatureState as CheckSignatureState.INVALID).signatureErrorCode
			)

			assertTrue(invalidState.revocationState is CheckRevocationState.SKIPPED)
			assertTrue(invalidState.nationalRulesState is CheckNationalRulesState.SUCCESS)
		}
	}

	@Test
	fun testCertificateLightValid() {
		val certificateHolder = decodeCertificate(LT1_A)
		val trustList = createTrustList(listOf(getCertificateLightTestKey()))

		runBlocking {
			val verificationState =
				certificateVerifier.verify(certificateHolder, trustList, setOf("THREE_G"), VerificationType.WALLET)
			assertTrue(verificationState is VerificationState.SUCCESS)

			val successState = verificationState as VerificationState.SUCCESS
			val walletSuccessState = successState.successState as SuccessState.WalletSuccessState
			certificateHolder.issuedAt?.let {
				val expectedValidFrom = LocalDateTime.ofInstant(it, ZoneId.systemDefault())
				assertEquals(expectedValidFrom, walletSuccessState.validityRange?.validFrom)
			}

			certificateHolder.expirationTime?.let {
				val expectedValidUntil = LocalDateTime.ofInstant(it, ZoneId.systemDefault())
				assertEquals(expectedValidUntil, walletSuccessState.validityRange?.validUntil)
			}
		}
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
			nationalRuleSet.copy(rules = emptyList()) // Use national ruleset from test resources to access the validity offsets, but not process any rules
		)
	}

}