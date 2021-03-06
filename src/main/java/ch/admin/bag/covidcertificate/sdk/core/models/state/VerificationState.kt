/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.state

import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.ValidityRange

sealed class VerificationState {
	data class SUCCESS(val successState: SuccessState, var isLightCertificate: Boolean) : VerificationState()

	data class INVALID(
		val signatureState: CheckSignatureState?,
		val revocationState: CheckRevocationState?,
		val nationalRulesState: CheckNationalRulesState?,
		val validityRange: ValidityRange?,
		val showRenewBanner: String?,
	) : VerificationState()

	object LOADING : VerificationState()
	data class ERROR(val error: StateError, val validityRange: ValidityRange?) : VerificationState()
}

sealed class SuccessState {
	data class WalletSuccessState(
		val isValidOnlyInSwitzerland: Boolean,
		val validityRange: ValidityRange?,
		val modeValidity: List<ModeValidity>,
		val eolBannerIdentifier: String?,
		val showRenewBanner: String?,
	) : SuccessState()

	data class VerifierSuccessState(
		val modeValidity: ModeValidity
	) : SuccessState()
}

