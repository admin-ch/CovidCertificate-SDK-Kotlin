/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.CertType
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.DisplayRule
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.JsonDateTime
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/*
* This class is responsible to return a Validity Range to display in a view. The LocalDateTime is calculated for
* the timezone of the device
* */
internal class DisplayValidityRangeCalculator {

    private val jacksonMapper = ObjectMapper().apply {
        setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
    }

    fun getDisplayValidityRangeForSystemTimeZone(
        displayRules: List<DisplayRule>,
        data: JsonNode,
        certType: CertType
    ): ValidityRange? {
        val displayFromDate: String = displayRules.find { it.id == "display-from-date" }?.logic ?: return null
        val displayUntilDate: String = displayRules.find { it.id == "display-until-date" }?.logic ?: return null
        val dateFromString = getValidity(displayFromDate, data, certType)
        val dateUntilString = getValidity(displayUntilDate, data, certType)
        return ValidityRange(dateFromString, dateUntilString)
    }

    private fun getValidity(displayRule: String, data: JsonNode, certType: CertType): LocalDateTime? {
        val displayLogic = jacksonMapper.readTree(displayRule)
        val date = evaluate(displayLogic, data)
        return getLocalDateTime(certType, date)
    }

    private fun getLocalDateTime(certType: CertType, data: JsonNode): LocalDateTime? {
        if (certType == CertType.TEST) {
            //test
            return (data as JsonDateTime).temporalValue().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        } else {
            //is vaccine or recovery entry
            return (data as JsonDateTime).temporalValue().toLocalDate().atStartOfDay()
        }
    }

}