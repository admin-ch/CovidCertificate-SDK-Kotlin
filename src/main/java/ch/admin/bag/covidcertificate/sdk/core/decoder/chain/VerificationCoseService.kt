/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
/**
 * Adapted from https://github.com/ehn-digital-green-development/hcert-kotlin
 * published under Apache-2.0 License.
 */

package ch.admin.bag.covidcertificate.sdk.core.decoder.chain

import COSE.MessageTag
import COSE.OneKey
import COSE.Sign1Message
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwk
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

internal object VerificationCoseService {

	init {
		Security.removeProvider("BC")
		Security.addProvider(BouncyCastleProvider())
	}

	fun decode(keys: List<Jwk>, input: ByteArray, certType: CertType): Boolean {
		val signature: Sign1Message = try {
			(Sign1Message.DecodeFromBytes(input, MessageTag.Sign1) as Sign1Message)
		} catch (e: Throwable) {
			null
		} ?: return false

		// The keyId is not security-critical and thus may be in either header: https://datatracker.ietf.org/doc/html/rfc8152#section-3.1
		val kid: ByteArray = signature.protectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
			?: signature.unprotectedAttributes[COSE.HeaderKeys.KID.AsCBOR()]?.GetByteString()
			?: return false

		// Return true if any public key of the correct keyId that is allowed to sign this specific certificate type correctly validates the signature
		return keys.filter { it.getKid() contentEquals kid }
			.filter { it.isAllowedToSign(certType) }
			.mapNotNull { it.getPublicKey() }
			.any { pk ->
				try {
					val pubKey = OneKey(pk, null)
					signature.validate(pubKey)
				} catch (t: Throwable) {
					// Key failed to verify the signature, return false
					false
				}
			}
	}

}