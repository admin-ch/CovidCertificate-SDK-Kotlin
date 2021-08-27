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
package ch.admin.bag.covidcertificate.sdk.core.verifier.nationalrules.certlogic;

public enum TimeUnit {

	year, month, day, hour;

	public static boolean isTimeUnitName(String name) {
		try {
			valueOf(name);
			return true;
		} catch (IllegalArgumentException iae) {
			return false;
		}
	}

}
