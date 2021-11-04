/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.extensions

import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.data.TestType
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.TestEntry
import ch.admin.bag.covidcertificate.sdk.core.utils.DateUtil
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


fun TestEntry.isNegative(): Boolean {
	return this.result == AcceptanceCriteriasConstants.NEGATIVE_CODE
}

fun TestEntry.isTargetDiseaseCorrect(): Boolean {
	return this.disease == AcceptanceCriteriasConstants.TARGET_DISEASE
}

fun TestEntry.getFormattedSampleDate(dateTimeFormatter: DateTimeFormatter): String? {
	return DateUtil.parseDateTime(this.timestampSample)?.format(dateTimeFormatter)
}

fun TestEntry.getFormattedResultDate(dateTimeFormatter: DateTimeFormatter): String? {
	return this.timestampResult?.let {
		DateUtil.parseDateTime(it)?.format(dateTimeFormatter)
	}
}

fun TestEntry.getTestCenter(): String? {
	if (!this.testCenter.isNullOrBlank()) {
		return this.testCenter
	}
	return null
}

fun TestEntry.getTestCountry(showEnglishVersionForLabels: Boolean): String {
	return try {
		val loc = Locale("", this.country)
		var countryString = loc.displayCountry
		if (showEnglishVersionForLabels) {
			countryString = "$countryString / ${loc.getDisplayCountry(Locale.ENGLISH)}"
		}
		return countryString
	} catch (e: Exception) {
		this.country
	}
}

fun TestEntry.getIssuer(): String {
	return this.certificateIssuer
}

fun TestEntry.getCertificateIdentifier(): String {
	return this.certificateIdentifier
}

fun TestEntry.validFromDate(): LocalDateTime? {
	return DateUtil.parseDateTime(this.timestampSample)
}

fun  TestEntry.isSeroPositiv(): Boolean {
	return this.type == TestType.SERO_POSITIV.code
}

