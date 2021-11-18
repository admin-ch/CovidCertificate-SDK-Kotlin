package ch.admin.bag.covidcertificate.sdk.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val DEFAULT_DISPLAY_RULES_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Instant.prettyPrint(dateFormatter: DateTimeFormatter): String {
	return try {
		this.atZone(ZoneId.systemDefault()).format(dateFormatter)
	} catch (e: Throwable) {
		this.toString()
	}
}