package ch.admin.bag.covidcertificate.sdk.core.utils

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

val DEFAULT_DISPLAY_RULES_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun Instant.prettyPrint(dateFormatter: DateTimeFormatter): String {
	return try {
		this.atZone(TimeZone.getTimeZone(ZoneOffset.UTC).toZoneId()).format(dateFormatter)
	} catch (e: Throwable) {
		this.toString()
	}
}