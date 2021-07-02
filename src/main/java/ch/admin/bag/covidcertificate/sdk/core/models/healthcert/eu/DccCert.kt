package ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CovidCertificate
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.PersonName
import com.fasterxml.jackson.annotation.JsonProperty
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.time.LocalDate

@JsonClass(generateAdapter = true)
data class DccCert(
	@Json(name = "ver") @get:JsonProperty("ver") val version: String,
	@Json(name = "nam") @get:JsonProperty("nam") val person: PersonName,
	@Json(name = "dob") @get:JsonProperty("dob") val dateOfBirth: String,
	@Json(name = "v") @get:JsonProperty("v") val vaccinations: List<VaccinationEntry>?,
	@Json(name = "t") @get:JsonProperty("t") val tests: List<TestEntry>?,
	@Json(name = "r") @get:JsonProperty("r") val pastInfections: List<RecoveryEntry>?,
) : CovidCertificate {
	override fun getPersonName() = person

	override fun getDateOfBirth(): LocalDate = LocalDate.parse(dateOfBirth)
}
