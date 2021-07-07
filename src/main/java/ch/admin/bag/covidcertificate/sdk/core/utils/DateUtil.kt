package ch.admin.bag.covidcertificate.sdk.core.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

internal object DateUtil {

	fun parseDate(input: String): LocalDate? {
		return try {
			OffsetDateTime.parse(input).toLocalDate()
		} catch (e: DateTimeParseException) {
			try {
				LocalDateTime.parse(input).toLocalDate()
			} catch (e: DateTimeParseException) {
				try {
					LocalDate.parse(input)
				} catch (e: DateTimeParseException) {
					null
				}
			}
		}
	}

}