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

import ch.admin.bag.covidcertificate.sdk.core.models.products.Vaccine
import ch.admin.bag.covidcertificate.sdk.core.data.AcceptanceCriteriasConstants
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.AcceptanceCriterias
import ch.admin.bag.covidcertificate.sdk.core.utils.DateUtil
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


fun VaccinationEntry.doseNumber(): Int = this.doseNumber

fun VaccinationEntry.totalDoses(): Int = this.totalDoses

fun VaccinationEntry.hadPastInfection(vaccine: Vaccine): Boolean {
	//if the total Doses of the vaccine is bigger then the total doses in the certificate, the patient had a past infection
	return vaccine.total_dosis_number > this.totalDoses()
}

fun VaccinationEntry.getNumberOverTotalDose(): String {
	return " ${this.doseNumber()}/${this.totalDoses()}"
}

fun VaccinationEntry.isNotFullyProtected(): Boolean {
	return this.doseNumber < this.totalDoses
}

fun VaccinationEntry.isTargetDiseaseCorrect(): Boolean {
	return this.disease == AcceptanceCriteriasConstants.TARGET_DISEASE
}

fun VaccinationEntry.validFromDate(offsetInDays: Long): LocalDateTime? {
	val vaccineDate = this.vaccineDate() ?: return null
	return vaccineDate.plusDays(offsetInDays)
}

fun VaccinationEntry.validUntilDate(acceptanceCriterias: AcceptanceCriterias): LocalDateTime? {
	val vaccinationImmunityEndDate = this.vaccineDate() ?: return null
	return vaccinationImmunityEndDate.plusDays(acceptanceCriterias.vaccineImmunity.toLong())
}

fun VaccinationEntry.vaccineDate(): LocalDateTime? {
	if (this.vaccinationDate.isEmpty()) {
		return null
	}

	return DateUtil.parseDate(this.vaccinationDate)?.atStartOfDay()
}

fun VaccinationEntry.getVaccinationCountry(showEnglishVersionForLabels: Boolean): String {
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

fun VaccinationEntry.getIssuer(): String {
	return this.certificateIssuer
}

fun VaccinationEntry.getCertificateIdentifier(): String {
	return this.certificateIdentifier
}
