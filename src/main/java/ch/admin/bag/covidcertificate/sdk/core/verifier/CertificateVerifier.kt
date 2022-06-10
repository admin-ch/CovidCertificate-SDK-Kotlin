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

import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.Base45Service
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.DecompressionService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.PrefixIdentifierService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.RevokedHealthCertService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.TimestampService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.VerificationCoseService
import ch.admin.bag.covidcertificate.sdk.core.models.certlogic.CertLogicHeaders
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertificateHolder
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckModeRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckNationalRulesState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckRevocationState
import ch.admin.bag.covidcertificate.sdk.core.models.state.CheckSignatureState
import ch.admin.bag.covidcertificate.sdk.core.models.state.ModeValidity
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError
import ch.admin.bag.covidcertificate.sdk.core.models.state.SuccessState
import ch.admin.bag.covidcertificate.sdk.core.models.state.VerificationState
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwks
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RevokedCertificatesStore
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleSet
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.TrustList
import ch.admin.bag.covidcertificate.sdk.core.utils.DEFAULT_DISPLAY_RULES_TIME_FORMATTER
import ch.admin.bag.covidcertificate.sdk.core.utils.prettyPrint
import ch.admin.bag.covidcertificate.sdk.core.verifier.moderules.ModeRulesVerifier
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.NationalRulesVerifier
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.ValidityRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

class CertificateVerifier {

	/**
	 * Verify the validity of a certificate. This checks the certificate signature, its revocation status as well as the conformity
	 * with national rules.
	 *
	 * @param certificateHolder The object returned from the decoder
	 * @param trustList The current applicable trust list, containing active public keys for signing, revoked certificate identifiers and the national rule set
	 * @param verificationModes A set of verification mode identifiers to use for the national rules check
	 * @param verificationType The type of verification check, either [VerificationType.WALLET] or [VerificationType.VERIFIER]
	 * @param nationalRulesCheckDate An optional date to verify the certificate against a specific date
	 * @param isForeignRulesCheck An optional flag to indicate this is a check against foreign national rules
	 * @return Outcome state of the verification
	 */
	suspend fun verify(
		certificateHolder: CertificateHolder,
		trustList: TrustList,
		verificationModes: Set<String>,
		verificationType: VerificationType = VerificationType.VERIFIER,
		nationalRulesCheckDate: LocalDateTime? = null,
		isForeignRulesCheck: Boolean = false,
	): VerificationState = withContext(Dispatchers.Default) {
		// Execute all three checks in parallel...
		val checkSignatureStateDeferred = async { checkSignature(certificateHolder, trustList.signatures) }
		val checkRevocationStateDeferred = async { checkRevocationStatus(certificateHolder, trustList.revokedCertificates) }
		val checkNationalRulesStateDeferred = async {
			checkNationalRules(certificateHolder, trustList.ruleSet, nationalRulesCheckDate, isForeignRulesCheck)
		}
		val checkModeRulesStateDeferred = async {
			if (isForeignRulesCheck) {
				// Mode rules don't apply to foreign rules checks
				CheckModeRulesState.SUCCESS(emptyList())
			} else {
				checkModeRules(certificateHolder, verificationModes, trustList.ruleSet)
			}
		}

		// ... but wait for all of them to finish
		val checkSignatureState = checkSignatureStateDeferred.await()
		val checkRevocationState = checkRevocationStateDeferred.await()
		val checkNationalRulesState = checkNationalRulesStateDeferred.await()
		val checkModeRulesState = checkModeRulesStateDeferred.await()

		if (checkSignatureState is CheckSignatureState.ERROR) {
			VerificationState.ERROR(checkSignatureState.error, checkNationalRulesState.validityRange())
		} else if (checkRevocationState is CheckRevocationState.ERROR) {
			VerificationState.ERROR(checkRevocationState.error, checkNationalRulesState.validityRange())
		} else if (checkNationalRulesState is CheckNationalRulesState.ERROR) {
			VerificationState.ERROR(checkNationalRulesState.error, null)
		} else if (checkModeRulesState is CheckModeRulesState.ERROR) {
			VerificationState.ERROR(checkModeRulesState.error, null)
		} else if (
			checkSignatureState == CheckSignatureState.SUCCESS
			&& (checkRevocationState == CheckRevocationState.SUCCESS || checkRevocationState == CheckRevocationState.SKIPPED)
			&& checkNationalRulesState is CheckNationalRulesState.SUCCESS
			&& checkModeRulesState is CheckModeRulesState.SUCCESS
		) {
			val isLightCertificate = certificateHolder.certType == CertType.LIGHT
			if (verificationType == VerificationType.WALLET) {
				val walletSuccessState = SuccessState.WalletSuccessState(
					checkNationalRulesState.isOnlyValidInCH,
					checkNationalRulesState.validityRange,
					checkModeRulesState.modeValidities,
					checkNationalRulesState.eolBannerIdentifier,
				)
				VerificationState.SUCCESS(walletSuccessState, isLightCertificate)
			} else {
				val verificationSuccessState =
					SuccessState.VerifierSuccessState(modeValidity = checkModeRulesState.modeValidities.first())
				VerificationState.SUCCESS(verificationSuccessState, isLightCertificate)
			}
		} else if (
			checkSignatureState is CheckSignatureState.INVALID
			|| checkRevocationState is CheckRevocationState.INVALID
			|| checkNationalRulesState is CheckNationalRulesState.INVALID
			|| checkNationalRulesState is CheckNationalRulesState.NOT_YET_VALID
			|| checkNationalRulesState is CheckNationalRulesState.NOT_VALID_ANYMORE
		) {
			VerificationState.INVALID(
				checkSignatureState, checkRevocationState, checkNationalRulesState,
				checkNationalRulesState.validityRange()
			)
		} else {
			VerificationState.LOADING
		}
	}

	/**
	 * Checks whether a certificate has a valid signature.
	 *
	 * A signature is only valid if it is signed by a trusted key, but also only if other attributes are valid
	 * (e.g. the signature is not expired - which may be different from the legal national rules).
	 *
	 * @param certificateHolder The object returned from the decoder
	 * @param signatures A list of active public keys used for signing certificates
	 * @return Outcome state of the signature check
	 */
	private suspend fun checkSignature(certificateHolder: CertificateHolder, signatures: Jwks) = withContext(Dispatchers.Default) {
		try {
			// Check that the certificate type is valid
			val certType = certificateHolder.certType
				?: return@withContext CheckSignatureState.INVALID(ErrorCodes.SIGNATURE_TYPE_INVALID)

			// Check that the signature timestamps are valid
			val timestampError = TimestampService.decode(certificateHolder)
			if (timestampError != null) {
				return@withContext CheckSignatureState.INVALID(timestampError)
			}

			// Repeat decode chain to get and verify COSE signature
			val encoded = PrefixIdentifierService.decode(certificateHolder.qrCodeData)
				?: return@withContext CheckSignatureState.INVALID(ErrorCodes.DECODE_PREFIX)
			val compressed = Base45Service.decode(encoded)
				?: return@withContext CheckSignatureState.INVALID(ErrorCodes.DECODE_BASE_45)
			val cose = DecompressionService.decode(compressed)
				?: return@withContext CheckSignatureState.INVALID(ErrorCodes.DECODE_Z_LIB)

			val valid = VerificationCoseService.decode(signatures.certs, cose, certType)
			if (valid) {
				CheckSignatureState.SUCCESS
			} else {
				CheckSignatureState.INVALID(ErrorCodes.SIGNATURE_COSE_INVALID)
			}
		} catch (e: Exception) {
			CheckSignatureState.ERROR(StateError(ErrorCodes.SIGNATURE_UNKNOWN, e.message.toString(), certificateHolder))
		}
	}

	/**
	 * Checks whether a certificate has been revoked
	 *
	 * @param certificateHolder The object returned from the decoder
	 * @param revokedCertificates The list of revoked certificate identifiers from the trust list
	 * @return Outcome state of the revocation check
	 */
	private suspend fun checkRevocationStatus(
		certificateHolder: CertificateHolder,
		revokedCertificates: RevokedCertificatesStore
	) = withContext(Dispatchers.Default) {
		// Revocation is not possible for light certificates, so this check returns the SKIPPED state
		if (certificateHolder.containsChLightCert()) return@withContext CheckRevocationState.SKIPPED

		try {
			val revokedCertificateService = RevokedHealthCertService(revokedCertificates)
			val containsRevokedCertificate = revokedCertificateService.isRevoked(certificateHolder.certificate as DccCert)

			if (containsRevokedCertificate) {
				CheckRevocationState.INVALID(ErrorCodes.REVOCATION_REVOKED)
			} else {
				CheckRevocationState.SUCCESS
			}
		} catch (e: Exception) {
			CheckRevocationState.ERROR(StateError(ErrorCodes.REVOCATION_UNKNOWN, e.message.toString(), certificateHolder))
		}
	}

	/**
	 * Checks whether a certificate conforms to a set of national rules
	 *
	 * @param certificateHolder The object returned from the decoder
	 * @param ruleSet The national rule set from the trust list
	 * @param nationalRulesCheckDate An optional date to check verify the certificate against a specific date
	 * @param isForeignRulesCheck An optional flag to indicate that this is a check against foreign national rules
	 * @return Outcome state of the national rules check
	 */
	private suspend fun checkNationalRules(
		certificateHolder: CertificateHolder,
		ruleSet: RuleSet,
		nationalRulesCheckDate: LocalDateTime? = null,
		isForeignRulesCheck: Boolean = false,
	): CheckNationalRulesState = withContext(Dispatchers.Default) {
		try {
			when {
				certificateHolder.containsChLightCert() -> {
					// If the DccHolder contains a light certificate, the national rules don't have to be verified
					// In that case, the validity range is from the issuedAt date until the expiration date from the CWT headers
					val issued = certificateHolder.issuedAt?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
					val expiration = certificateHolder.expirationTime?.let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
					CheckNationalRulesState.SUCCESS(ValidityRange(issued, expiration), true)
				}
				certificateHolder.containsDccCert() -> {
					val nationalRulesVerifier = NationalRulesVerifier()
					val issuedAt = certificateHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
					val expiredAt = certificateHolder.expirationTime?.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
					val issuer = certificateHolder.issuer
					val kid = certificateHolder.kidBase64
					nationalRulesVerifier.verify(
						certificateHolder.certificate as DccCert,
						ruleSet,
						certificateHolder.certType!!,
						CertLogicHeaders(issuedAt, expiredAt, false, null, issuer, kid),
						nationalRulesCheckDate,
						isForeignRulesCheck,
					)
				}
				else -> {
					CheckNationalRulesState.ERROR(StateError(ErrorCodes.RULESET_UNKNOWN, certificateHolder = certificateHolder))
				}
			}
		} catch (e: Exception) {
			CheckNationalRulesState.ERROR(StateError(ErrorCodes.RULESET_UNKNOWN, e.message.toString(), certificateHolder))
		}
	}

	/**
	 * Checks whether a certificate conforms to a set of mode rules
	 *
	 * @param certificateHolder The object returned from the decoder
	 * @param ruleSet The national rule set from the trust list
	 * @return Outcome state of the mode rules check
	 */
	private suspend fun checkModeRules(
		certificateHolder: CertificateHolder,
		verificationIdentifier: Set<String>,
		ruleSet: RuleSet
	): CheckModeRulesState = withContext(Dispatchers.Default) {
		val modeRulesVerifier = ModeRulesVerifier()
		val modeValidities = mutableListOf<ModeValidity>()

		if (!certificateHolder.containsChLightCert() && !certificateHolder.containsDccCert()) {
			return@withContext CheckModeRulesState.ERROR(
				StateError(ErrorCodes.RULESET_UNKNOWN, certificateHolder = certificateHolder)
			)
		}

		verificationIdentifier.forEach { verificationMode ->
			try {
				val issuedAt = certificateHolder.issuedAt?.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
				val expiredAt = certificateHolder.expirationTime?.prettyPrint(DEFAULT_DISPLAY_RULES_TIME_FORMATTER)
				val isLight = certificateHolder.containsChLightCert()
				val issuer = certificateHolder.issuer
				val kid = certificateHolder.kidBase64
				val modeValidity = modeRulesVerifier.verify(
					certificateHolder.certificate,
					ruleSet,
					CertLogicHeaders(issuedAt, expiredAt, isLight, verificationMode, issuer, kid),
					verificationMode
				)
				modeValidities.add(modeValidity)

			} catch (e: Exception) {
				return@withContext CheckModeRulesState.ERROR(
					StateError(ErrorCodes.RULESET_UNKNOWN, e.message.toString(), certificateHolder)
				)
			}
		}
		return@withContext CheckModeRulesState.SUCCESS(modeValidities)
	}


}