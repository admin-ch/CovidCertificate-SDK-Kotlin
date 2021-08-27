/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
/**
 * Adapted from https://github.com/ehn-dcc-development/dgc-business-rules/tree/main/certlogic/certlogic-kotlin
 * published under Apache-2.0 License.
 */
package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.*
import kotlin.math.min


data class ValidationError(val expr: JsonNode, val message: String) {}


internal fun validateVar(expr: JsonNode, values: JsonNode): List<ValidationError> {
	if (values !is TextNode) {
		return listOf(ValidationError(expr, "not of the form { \"var\": \"<path>\" }"))
	}
	val path = values.asText()
	return if (path == "" || Regex("^([^\\.]+?)(\\.[^\\.]+?)*\$").matches(path))
		emptyList()
	else
		listOf(ValidationError(expr, "data access path doesn't have a valid format: $path"))
}


internal fun validateIf(expr: JsonNode, values: ArrayNode): List<ValidationError> =
	(
			if (values.size() == 3)
				emptyList()
			else
				listOf(
					ValidationError(
						expr,
						"an \"if\"-operation must have exactly 3 values/operands, but it has ${values.size()}"
					)
				)
			) + values.toList().subList(0, 3).flatMap { validate(it) }


internal fun validateInfix(expr: JsonNode, values: ArrayNode, operator: String): List<ValidationError> {
	val nOperands = values.size()
	val maxOperands = when (operator) {
		"and" -> nOperands
		"<", ">", "<=", ">=", "after", "before", "not-after", "not-before" -> 3
		else -> 2
	}
	return when (operator) {
		"and" -> if (nOperands < 2) listOf(
			ValidationError(
				expr,
				"an \"and\" operation must have at least 2 operands, but it has ${values.size()}"
			)
		) else emptyList()
		"<", ">", "<=", ">=", "after", "before", "not-after", "not-before" -> if (nOperands < 2 || nOperands > 3) listOf(
			ValidationError(
				expr,
				"an operation with operator \"${operator}\" must have 2 or 3 operands, but it has ${values.size()}"
			)
		) else emptyList()
		else -> if (nOperands != 2) listOf(
			ValidationError(
				expr,
				"an operation with operator \"${operator}\" must have 2 operands, but it has ${values.size()}"
			)
		) else emptyList()
	} + values.take(min(maxOperands, nOperands)).flatMap { validate(it) }
}


internal fun validateNot(expr: JsonNode, values: ArrayNode): List<ValidationError> =
	if (values.size() == 1)
		validate(values[0])
	else
		listOf(
			ValidationError(
				expr,
				"a !-operation (logical not/negation) must have exactly 1 operand, but it has ${values.size()}"
			)
		)


internal fun validatePlusTime(expr: JsonNode, values: ArrayNode): List<ValidationError> =
	(
			if (values.size() == 3)
				emptyList()
			else
				listOf(
					ValidationError(
						expr,
						"a \"plusTime\"-operation must have exactly 3 values/operands, but it has ${values.size()}"
					)
				)
			) +
			(
					if (values.has(0))
						validate(values[0])
					else
						emptyList()
					) +
			(
					if (values.has(1) && !values[1].isInt)
						listOf(
							ValidationError(
								expr,
								"\"amount\" argument (#2) of \"plusTime\" must be an integer, but it is: ${values[1]}"
							)
						)
					else
						emptyList()
					) +
			(
					if (values.has(2) && !TimeUnit.isTimeUnitName(values[2].asText()))
						listOf(
							ValidationError(
								expr,
								"\"unit\" argument (#3) of \"plusTime\" must be a string equal to one of ${
									TimeUnit.values().map { it.name }.joinToString(", ")
								}, but it is: ${values[2]}"
							)
						)
					else
						emptyList()
					)


internal fun validateReduce(expr: JsonNode, values: ArrayNode): List<ValidationError> =
	(
			if (values.size() == 3)
				emptyList()
			else
				listOf(
					ValidationError(
						expr,
						"an \"reduce\"-operation must have exactly 3 values/operands, but it has ${values.size()}"
					)
				)
			) + values.toList().subList(0, 3).flatMap { validate(it) }


internal fun validateExtractFromUVCI(expr: JsonNode, values: ArrayNode): List<ValidationError> =
	(
			if (values.size() == 2)
				emptyList()
			else
				listOf(
					ValidationError(
						expr,
						"an \"extractFromUVCI\"-operation must have exactly 2 values/operands, but it has ${values.size()}"
					)
				)
			) +
			(
					if (values.has(0))
						validate(values[0])
					else
						emptyList()
					) +
			(
					if (values.has(1) && values[1] !is IntNode)
						listOf(
							ValidationError(
								expr,
								"\"index\" argument (#2) of \"extractFromUVCI\" must be an integer, but it is: ${values[1]}"
							)
						)
					else
						emptyList()
					)


fun validate(expr: JsonNode): List<ValidationError> {
	fun withError(message: String) = listOf(ValidationError(expr, message))
	return when (expr) {
		is TextNode, is IntNode, is BooleanNode -> emptyList()
		is NumericNode -> withError("$expr is a non-integer number")
		is NullNode -> withError("invalid CertLogic expression")
		is ArrayNode -> expr.flatMap { validate(it) }
		is ObjectNode -> {
			if (expr.size() != 1) {
				return withError("expression object must have exactly one key, but it has ${expr.size()}")
			}
			val (operator, args) = expr.fields().next()
			if (operator == "var") {
				return validateVar(expr, args)
			}
			if (!(args is ArrayNode && args.size() > 0)) {
				return withError("operation not of the form { \"<operator>\": [ <values...> ] }")
			}
			return when (operator) {
				"if" -> validateIf(expr, args)
				"===", "and", ">", "<", ">=", "<=", "in", "+", "after", "before", "not-after", "not-before" -> validateInfix(
					expr,
					args,
					operator
				)
				"!" -> validateNot(expr, args)
				"plusTime" -> validatePlusTime(expr, args)
				"reduce" -> validateReduce(expr, args)
				"extractFromUVCI" -> validateExtractFromUVCI(expr, args)
				else -> withError("unrecognised operator: \"$operator\"")
			}
		}
		else -> withError("invalid CertLogic expression")
	}
}