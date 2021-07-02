package ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CovidCertificate
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.PersonName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class ChLightCert(
	@Json(name = "ver") val version: String,
	@Json(name = "nam") val person: PersonName,
	@Json(name = "dob") val dateOfBirth: String
) : CovidCertificate {
	override fun getPersonName() = person

	override fun getDateOfBirth(): LocalDate = LocalDate.parse(dateOfBirth)
}
