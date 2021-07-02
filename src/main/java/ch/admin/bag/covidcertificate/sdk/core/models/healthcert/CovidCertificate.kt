package ch.admin.bag.covidcertificate.sdk.core.models.healthcert

import java.time.LocalDate

interface CovidCertificate {

	fun getPersonName(): PersonName
	fun getDateOfBirth(): LocalDate

}