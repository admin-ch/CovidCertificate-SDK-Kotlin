package ch.admin.bag.covidcertificate.sdk.core.models.trustlist

interface RevokedCertificatesStore {

	fun containsCertificate(certificate: String): Boolean

	fun addCertificates(certificates: List<String>)
}