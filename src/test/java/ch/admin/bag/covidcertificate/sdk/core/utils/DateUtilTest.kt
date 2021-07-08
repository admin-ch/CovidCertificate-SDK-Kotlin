package ch.admin.bag.covidcertificate.sdk.core.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DateUtilTest {

	@Test
	fun testParseDateOnly() {
		val input = "1990-12-31"
		val expected = LocalDate.of(1990, 12, 31)
		val actual = DateUtil.parseDate(input)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

	@Test
	fun testParseDateWithTime() {
		val input = "1990-12-31T00:00:00"
		val zoneOffset = ZoneOffset.UTC
		val expected = OffsetDateTime.of(1990, 12, 31, 0, 0, 0, 0, zoneOffset).toLocalDate()
		val actual = DateUtil.parseDate(input, zoneOffset)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

	@Test
	fun testParseDateWithTimeAndOffset() {
		val input = "1990-12-31T12:00:00+02:00"
		val zoneOffset = ZoneOffset.ofHours(2)
		val expected = OffsetDateTime.of(1990, 12, 31, 12, 0, 0, 0, zoneOffset).toLocalDate()
		val actual = DateUtil.parseDate(input, zoneOffset)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

	@Test
	fun testParseInvalidDateFormat() {
		val input = "31.12.1990"
		val expected = null
		val actual = DateUtil.parseDate(input)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

	@Test
	fun testParseDateTime() {
		val input = "1990-12-31T12:00:00"
		val zoneOffset = ZoneOffset.UTC
		val expected = OffsetDateTime.of(1990, 12, 31, 12, 0, 0, 0, zoneOffset).toLocalDateTime()
		val actual = DateUtil.parseDateTime(input, zoneOffset)
		assertEquals(expected, actual, "Parsed LocalDateTime does not match the expected output")
	}

	@Test
	fun testParseDateTimeWithoutZone() {
		val input = "1990-12-31T12:00:00Z"
		val zoneOffset = ZoneOffset.UTC
		val expected = OffsetDateTime.of(1990, 12, 31, 12, 0, 0, 0, zoneOffset).toLocalDateTime()
		val actual = DateUtil.parseDateTime(input, zoneOffset)
		assertEquals(expected, actual, "Parsed LocalDateTime does not match the expected output")
	}

	@Test
	fun testParseDateTimeWithZone() {
		val input = "1990-12-31T12:00:00+02:00"
		val zoneOffset = ZoneOffset.ofHours(2)
		val expected = OffsetDateTime.of(1990, 12, 31, 12, 0, 0, 0, zoneOffset).toLocalDateTime()
		val actual = DateUtil.parseDateTime(input, zoneOffset)
		assertEquals(expected, actual, "Parsed LocalDateTime does not match the expected output")
	}

	@Test
	fun testParseInvalidDateTimeFormat() {
		val input = "31.12.1990 12:00:00"
		val expected = null
		val actual = DateUtil.parseDateTime(input)
		assertEquals(expected, actual, "Parsed LocalDateTime does not match the expected output")
	}

	@Test
	fun testDefaultDisplayDateFormat() {
		val input = LocalDate.of(1990, 12, 31)
		val expected = "31.12.1990"
		val actual = DateUtil.formatDate(input)
		assertEquals(expected, actual, "Formatted Date does not match the expected output")
	}

	@Test
	fun testCustomDisplayDateFormat() {
		val input = LocalDate.of(1990, 12, 31)
		val expected = "1990-12-31"
		val actual = DateUtil.formatDate(input, DateTimeFormatter.ISO_DATE)
		assertEquals(expected, actual, "Formatted Date does not match the expected output")
	}

}