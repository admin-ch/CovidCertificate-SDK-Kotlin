package ch.admin.bag.covidcertificate.sdk.core.models.healthcert

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.light.ChLightCert
import java.time.Instant

class CertificateHolder(
	val certificate: CovidCertificate,
	val qrCodeData: String,
	val expirationTime: Instant? = null,
	val issuedAt: Instant? = null,
	val issuer: String? = null,
) {

	var certType: CertType? = null
		internal set

	fun containsDccCert() = certificate is DccCert

	fun containsChLightCert() = certificate is ChLightCert

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (javaClass != other?.javaClass) return false

		other as CertificateHolder

		if (certificate != other.certificate) return false
		if (qrCodeData != other.qrCodeData) return false

		return true
	}

	override fun hashCode(): Int {
		var result = qrCodeData.hashCode()
		result = 31 * result + certificate.hashCode()
		return result
	}
}
