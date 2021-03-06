/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core.extensions

import ch.admin.bag.covidcertificate.sdk.core.data.base64.Base64Impl

fun ByteArray.toBase64(): String = Base64Impl.encode(this)
fun String.fromBase64(): ByteArray = Base64Impl.decode(this)
