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


internal fun isFalsy(value: JsonNode): Boolean = when (value) {
	is BooleanNode -> value == BooleanNode.FALSE
	is NullNode -> true
	is TextNode -> value.textValue().isEmpty()
	is IntNode -> value.intValue() == 0
	is ArrayNode -> value.size() == 0
	is ObjectNode -> value.size() == 0
	else -> false
}

internal fun isTruthy(value: JsonNode): Boolean = when (value) {
	is BooleanNode -> value == BooleanNode.TRUE
	is TextNode -> value.textValue().isNotEmpty()
	is IntNode -> value.intValue() != 0
	is ArrayNode -> value.size() > 0
	is ObjectNode -> value.size() > 0
	else -> false
}


internal fun intCompare(operator: String, l: Int, r: Int): Boolean =
	when (operator) {
		"<" -> l < r
		">" -> l > r
		"<=" -> l <= r
		">=" -> l >= r
		else -> throw RuntimeException("unhandled comparison operator \"$operator\"")
	}

internal fun <T : Comparable<T>> compare(operator: String, args: List<T>): Boolean =
	when (args.size) {
		2 -> intCompare(operator, args[0].compareTo(args[1]), 0)
		3 -> intCompare(operator, args[0].compareTo(args[1]), 0) && intCompare(operator, args[1].compareTo(args[2]), 0)
		else -> throw RuntimeException("invalid number of operands to a \"$operator\" operation")
	}

internal fun comparisonOperatorForDateTimeComparison(operator: String): String =
	when (operator) {
		"after" -> ">"
		"before" -> "<"
		"not-after" -> "<="
		"not-before" -> ">="
		else -> throw RuntimeException("unhandled date-time comparison operator \"$operator\"")
	}


internal val optionalPrefix = "URN:UVCI:"
internal fun extractFromUVCI(uvci: String?, index: Int): String? {
	if (uvci == null || index < 0) {
		return null
	}
	val prefixlessUvci = if (uvci.startsWith(optionalPrefix)) uvci.substring(optionalPrefix.length) else uvci
	val fragments = prefixlessUvci.split(Regex("[/#:]"))
	return if (index < fragments.size) fragments[index] else null
}