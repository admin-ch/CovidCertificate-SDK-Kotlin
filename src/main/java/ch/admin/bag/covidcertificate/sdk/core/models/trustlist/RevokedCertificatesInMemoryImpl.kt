package ch.admin.bag.covidcertificate.sdk.core.models.trustlist

class RevokedCertificatesInMemoryImpl(private var revokedCertificates: List<String>) : RevokedCertificatesStore {
	override fun containsCertificate(certificate: String) = revokedCertificates.contains(certificate)

	override fun addCertificates(certificates: List<String>) {
		revokedCertificates = revokedCertificates.toMutableList().apply {
			addAll(certificates)
			distinct()
		}
	}

}