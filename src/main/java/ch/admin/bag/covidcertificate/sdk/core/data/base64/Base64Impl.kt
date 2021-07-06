package ch.admin.bag.covidcertificate.sdk.core.data.base64

import java.util.*

object Base64Impl {

	@JvmField
	val JAVA_UTIL_BASE64 = JavaUtilBase64()

	private var base64Provider: Base64Provider = JAVA_UTIL_BASE64

	@JvmStatic
	fun setBase64Provider(provider: Base64Provider) {
		this.base64Provider = provider
	}

	@JvmStatic
	fun encode(src: ByteArray) = base64Provider.encode(src)

	@JvmStatic
	fun decode(src: String) = base64Provider.decode(src)

	class JavaUtilBase64 : Base64Provider {
		override fun encode(src: ByteArray): String = Base64.getEncoder().encodeToString(src).trim()
		override fun decode(src: String): ByteArray = Base64.getDecoder().decode(src)
	}
}

interface Base64Provider {
	fun encode(src: ByteArray): String
	fun decode(src: String): ByteArray
}