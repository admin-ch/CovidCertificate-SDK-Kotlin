/*
 * Copyright (c) 2022 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.covidcertificate.sdk.core.models.healthcert

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PersonNameTest {

	@Test
	fun testNormal() {
		val person = PersonName(
			familyName = "Müller",
			standardizedFamilyName = "Mueller",
			givenName = "Jörg",
			standardizedGivenName = "Joerg",
		)
		assertEquals(person.prettyFamilyName(), "Müller")
		assertEquals(person.prettyGivenName(), "Jörg")
		assertEquals(person.prettyName(), "Müller Jörg")
		assertEquals(person.prettyStandardizedName(), "Mueller<<Joerg")
	}

	// 1 value null

	@Test
	fun testNullFamilyName() {
		val person = PersonName(
			familyName = null,
			standardizedFamilyName = "Mueller",
			givenName = "Jörg",
			standardizedGivenName = "Joerg",
		)
		assertEquals(person.prettyFamilyName(), "Mueller")
		assertEquals(person.prettyGivenName(), "Jörg")
		assertEquals(person.prettyName(), "Mueller Jörg")
		assertEquals(person.prettyStandardizedName(), "Mueller<<Joerg")
	}

	@Test
	fun testNullGivenName() {
		val person = PersonName(
			familyName = "Müller",
			standardizedFamilyName = "Mueller",
			givenName = null,
			standardizedGivenName = "Joerg",
		)
		assertEquals(person.prettyFamilyName(), "Müller")
		assertEquals(person.prettyGivenName(), "Joerg")
		assertEquals(person.prettyName(), "Müller Joerg")
		assertEquals(person.prettyStandardizedName(), "Mueller<<Joerg")
	}

	@Test
	fun testNullStandardizedGivenName() {
		val person = PersonName(
			familyName = "Müller",
			standardizedFamilyName = "Mueller",
			givenName = "Jörg",
			standardizedGivenName = null,
		)
		assertEquals(person.prettyFamilyName(), "Müller")
		assertEquals(person.prettyGivenName(), "Jörg")
		assertEquals(person.prettyName(), "Müller Jörg")
		assertEquals(person.prettyStandardizedName(), "Mueller<<")
	}

	// 2 values null

	@Test
	fun testNullFamilyNameAndGivenName() {
		val person = PersonName(
			familyName = null,
			standardizedFamilyName = "Mueller",
			givenName = null,
			standardizedGivenName = "Joerg",
		)
		assertEquals(person.prettyFamilyName(), "Mueller")
		assertEquals(person.prettyGivenName(), "Joerg")
		assertEquals(person.prettyName(), "Mueller Joerg")
		assertEquals(person.prettyStandardizedName(), "Mueller<<Joerg")
	}

	@Test
	fun testNullFamilyNameAndStandardizedGivenName() {
		val person = PersonName(
			familyName = null,
			standardizedFamilyName = "Mueller",
			givenName = "Jörg",
			standardizedGivenName = null,
		)
		assertEquals(person.prettyFamilyName(), "Mueller")
		assertEquals(person.prettyGivenName(), "Jörg")
		assertEquals(person.prettyName(), "Mueller Jörg")
		assertEquals(person.prettyStandardizedName(), "Mueller<<")
	}

	@Test
	fun testNullGivenNameAndStandardizedGivenName() {
		val person = PersonName(
			familyName = "Müller",
			standardizedFamilyName = "Mueller",
			givenName = null,
			standardizedGivenName = null,
		)
		assertEquals(person.prettyFamilyName(), "Müller")
		assertEquals(person.prettyGivenName(), "")
		assertEquals(person.prettyName(), "Müller ") // note the trailing space
		assertEquals(person.prettyStandardizedName(), "Mueller<<")
	}

	// 3 values null

	@Test
	fun testAllNull() {
		val person = PersonName(
			familyName = null,
			standardizedFamilyName = "Mueller",
			givenName = null,
			standardizedGivenName = null,
		)
		assertEquals(person.prettyFamilyName(), "Mueller")
		assertEquals(person.prettyGivenName(), "")
		assertEquals(person.prettyName(), "Mueller ") // note the trailing space
		assertEquals(person.prettyStandardizedName(), "Mueller<<")
	}

}
