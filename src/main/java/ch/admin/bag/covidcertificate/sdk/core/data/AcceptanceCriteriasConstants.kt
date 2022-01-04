/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.data

object AcceptanceCriteriasConstants {

	const val NEGATIVE_CODE: String = "260415000"
	const val POSITIVE_CODE: String = "260373001"
	const val TARGET_DISEASE = "840539006"
}

enum class TestType(val code: String) {
	RAT("LP217198-3"),
	PCR("LP6464-4"),
	SEROLOGICAL("94504-8"),
	MEDICAL_EXEMPTION("medical-exemption")

}
