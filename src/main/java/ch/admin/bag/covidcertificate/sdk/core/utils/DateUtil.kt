package ch.admin.bag.covidcertificate.sdk.core.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateUtil {

	val DEFAULT_DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

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

	fun parseDateTime(input: String): LocalDateTime? {
		return try {
			OffsetDateTime.parse(input).toLocalDateTime()
		} catch (e: DateTimeParseException) {
			try {
				LocalDateTime.parse(input)
			} catch (e: DateTimeParseException) {
				null
			}
		}
	}

	fun formatDate(date: LocalDate, formatter: DateTimeFormatter = DEFAULT_DISPLAY_DATE_FORMATTER): String {
		return formatter.format(date)
	}

}