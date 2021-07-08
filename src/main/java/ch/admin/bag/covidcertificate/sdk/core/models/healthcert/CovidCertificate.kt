/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.models.healthcert

import java.io.Serializable

interface CovidCertificate : Serializable {

	/**
	 * Returns the object that contains the personal information of the certificate, including given and family name and their
	 * standardized values
	 */
	fun getPersonName(): PersonName

	/**
	 * Returns the formatted date of birth or the original date of birth string if the datetime format is unknown
	 */
	fun getFormattedDateOfBirth(): String

}