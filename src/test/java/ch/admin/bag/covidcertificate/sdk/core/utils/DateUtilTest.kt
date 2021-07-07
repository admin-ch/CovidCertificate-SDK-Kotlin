package ch.admin.bag.covidcertificate.sdk.core.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DateUtilTest {

	@Test
	fun testParseDate() {
		val input = "1990-12-31"
		val expected = LocalDate.of(1990, 12, 31)
		val actual = DateUtil.parseDate(input)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

	@Test
	fun testParseDateTime() {
		val input = "1990-12-31T00:00:00"
		val expected = LocalDate.of(1990, 12, 31)
		val actual = DateUtil.parseDate(input)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

	@Test
	fun testParseDateTimeWithOffset() {
		val input = "1990-12-31T12:00:00+02:00"
		val expected = LocalDate.of(1990, 12, 31)
		val actual = DateUtil.parseDate(input)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

	@Test
	fun testParseInvalidDateFormat() {
		val input = "31.12.1990"
		val expected = null
		val actual = DateUtil.parseDate(input)
		assertEquals(expected, actual, "Parsed LocalDate does not match the expected output")
	}

}