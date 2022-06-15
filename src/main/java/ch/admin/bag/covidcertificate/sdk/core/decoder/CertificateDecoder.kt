/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.decoder

import ch.admin.bag.covidcertificate.sdk.core.data.ErrorCodes
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.Base45Service
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.CborService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.CertTypeService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.DecompressionService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.NoopVerificationCoseService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.PrefixIdentifierService
import ch.admin.bag.covidcertificate.sdk.core.decoder.chain.VerificationCoseService
import ch.admin.bag.covidcertificate.sdk.core.models.state.DecodeState
import ch.admin.bag.covidcertificate.sdk.core.models.state.StateError

object CertificateDecoder {

	/**
	 * Decodes the string from a QR code into a DCC.
	 *
	 * Does not do any validity checks. Simply checks whether the data is decodable.
	 *
	 * @param qrCodeData content of the scanned qr code, of the format "HC1:base45(...)" or "LT1:base45(...)"
	 */
	@JvmStatic
	fun decode(qrCodeData: String): DecodeState {
		// Remove the prefix to retrieve the encoded data
		val encoded = PrefixIdentifierService.decode(qrCodeData) ?: return DecodeState.ERROR(StateError(ErrorCodes.DECODE_PREFIX))

		// Base45 decode to retrieve the compressed data
		val compressed = Base45Service.decode(encoded) ?: return DecodeState.ERROR(StateError(ErrorCodes.DECODE_BASE_45))

		// ZLIB decompress to retrieve the COSE data
		val cose = DecompressionService.decode(compressed) ?: return DecodeState.ERROR(StateError(ErrorCodes.DECODE_Z_LIB))

		// COSE decode to retrieve the CBOR data
		val cbor = NoopVerificationCoseService.decode(cose) ?: return DecodeState.ERROR(StateError(ErrorCodes.DECODE_COSE))

		// Read the key ID from the signature. This is non-critical for the certificate decoding, so it does not return an error state if it is missing
		val signature = VerificationCoseService.getSignature(cose)
		val kid = signature?.let { VerificationCoseService.getKeyId(it) }

		// Decode the CBOR data into a certificate holder
		val certificateHolder = CborService.decode(cbor, qrCodeData, kid) ?: return DecodeState.ERROR(StateError(ErrorCodes.DECODE_CBOR))

		// Determine the certificate type the certificate holder contains
		certificateHolder.certType = CertTypeService.decode(certificateHolder)

		return DecodeState.SUCCESS(certificateHolder)
	}

}