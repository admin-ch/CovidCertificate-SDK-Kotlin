package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules

import ch.admin.bag.covidcertificate.sdk.core.models.healthcert.eu.DccCert
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicData
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicExternalInfo
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.CertLogicPayload
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Rule
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.RuleValueSets
import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.ValidityRules
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.JsonDateTime
import ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic.evaluate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

internal class ValidityRangeCalculator {

	private val jacksonMapper = ObjectMapper().apply {
		setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC))
	}

	fun getValidityRange(
		dccCert: DccCert,
		validityRules: ValidityRules,
		valueSets: RuleValueSets
	): ValidityRange? {
		val payload = CertLogicPayload(dccCert.pastInfections, dccCert.tests, dccCert.vaccinations)
		val externalInfo = CertLogicExternalInfo(valueSets)
		val ruleSetData = CertLogicData(payload, externalInfo)
		val data = jacksonMapper.valueToTree<JsonNode>(ruleSetData)

		val validFrom = getValidity(validityRules.validFrom, data)
		val validUntil = getValidity(validityRules.validUntil, data)

		return if (validFrom != null && validUntil != null) {
			ValidityRange(validFrom, validUntil)
		} else {
			null
		}
	}

	private fun getValidity(rules: List<Rule>, data: JsonNode): LocalDateTime? {
		for (rule in rules) {
			val ruleLogic = jacksonMapper.readTree(rule.logic)
			val evaluatedNode = evaluate(ruleLogic, data)
			if (evaluatedNode is JsonDateTime) {
				return evaluatedNode.temporalValue().toLocalDateTime()
			}
		}

		return null
	}
}