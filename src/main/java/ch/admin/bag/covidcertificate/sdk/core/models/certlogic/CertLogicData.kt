package ch.admin.bag.covidcertificate.sdk.core.models.certlogic

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.PersonName
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.RecoveryEntry
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.TestEntry
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.VaccinationEntry

internal data class CertLogicData(
	val payload: CertLogicPayload,
	val external: CertLogicExternalInfo,
)

internal data class CertLogicPayload(
	val nam: PersonName? = null,
	val dob: String? = null,
	val ver: String? = null,
	val r: List<RecoveryEntry>? = null,
	val t: List<TestEntry>? = null,
	val v: List<VaccinationEntry>? = null,
	val h: CertLogicHeaders? = null
)

internal data class CertLogicExternalInfo(
	val valueSets: Map<String, Array<String>>,
	val validationClock: String, // ISO-8601 extended offset date-time format
	val validationClockAtStartOfDay: String, // ISO-8601 date format
)

internal data class CertLogicHeaders(
	val iat: String?,
	val exp: String?,
	val isIsLight: Boolean?, //Must be isIsLight because the Json Serializer strips one "is"
	val mode: String?,
	val iss: String? = null,
	val kid: String? = null,
)