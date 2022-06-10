/*
 * Copyright (c) 2021 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package ch.admin.bag.covidcertificate.sdk.core

import ch.admin.bag.covidcertificate.sdk.core.models.trustlist.Jwk

// Note that this String (taken directly from a QR code) has some \ that escape $
// This HC is signed by the hardcoded BAG DEV key
internal const val HC1_A =
	"HC1:NCFB60MG0/3WUWGSLKH47GO0SK7KFDCBOECI9CKW500XK0JCV498F3//CO/74F3*QKODOY50.FK6ZK7:EDOLOPCO8F6%E3.DA%EOPC1G72A6YM87G7N46YX8HS8VF60B8646U*8XA7H*8SG81S81T8UPC0JCZ69FVCBJ0LVC6JD846KF6C464W5B56+EDG8F3I80/D6\$CBECSUER:C2\$NS346\$C2%E9VC- CSUE145GB8JA5B\$D% D3IA4W5646946846.96.JCP9EJY8L/5M/5546.96VF63KC/SC4KCD3DX47B46IL6646H*6Z/E5JD%96IA74R6646407GVC*JC1A6FA74W5KF6AL6TPCBEC7ZKW.CYUC6\$C4WEI3DUOCGB8LPCG/DYUCHY83UAI3D5WE-G8519O/EZKEZ967L6256V50 6MZL8L1SRRDHKRYOV*A97PC32I4CQ9PBL-3.Z05+13GC6995PO JG%FNE:J30H5Z82*20FF2MP:HB4P9.P4Z+RG-392WSTE+8JFU780IY QDVFTD4X RRIP/H0XL4/GN:1CPWIWB1UWVYYLMXLNBD4UHVYUVUC558PIN.OT*+0LWQ+3G-+QGQM:CGT+3ITKZIA2WADIOR*VB 8M795-7%Q5EJT0V8SMJZR02-VKD01KQT3RSTN1OH461YTHP91VST7V6S\$35HLUGQG\$GD3FL676\$UG8ML4U/LVNLR6Y2:GE807\$ADY\$MC5C V412R7K6AVI1%A56KPJF DJX 5.XM3+J. D%HPAAM2RK1URNBVZAQ3%2CGVM29%*DG*BJ6DHPQ2KR"
internal const val LT1_A =
	"LT1:6BFU90V10RDWT 9O60GO0000W50JB06H08CK34C/70YM8N34GB8WY0ABC VI597.FKMTKGVC*JC1A6/Q63W5KF6746TPCBEC7ZKW.CU2DNXO VD5\$C JC3/DMP8\$ILZEDZ CW.C9WE.Y9AY8+S9VIAI3D8WEVM8:S9C+9\$PC5\$CUZCY\$5Y\$527BJZH/HULXS+Q5M8R .S6YE2JCU.OR8ICBM+2QZFLK DHPHCS3Q6EK3A:RFH%HGEV:DE79K/8NM7MY.9VRKV5SP89HN2OED85SW.C8A9"

internal fun getInvalidSigningKeys(): List<Jwk> {
	val n =
		"4uZO4_7tneZ3XD5OAiTyoANOohQZC-DzZ4YC0AoLnEO-Z3PcTialCuRKS1zHfujNPI0GGG09DRVVXdv-tcKNXFDt_nRU1zlWDGFf4_63l5RIjkWFD3JFKqR8IlcJjrYYxstuZs3May3SGQJ-kZaeH5GFZMRvE0waHqMxbfwakvjf8qyBXCrZ1WsK-XJf7iYbJS2dO1a5HnegxPuRA7Zz8ikO7QRzmSongqOlkejEaIkFjx7gLGTUsOrBPYa5sdZqinDwmnjtKi52HLWarMXs-t1MN4etIp7GE7_zarjBNxk1Efiiwl-RdcwJ2uVwfrgzxfv3_TekZF8IUykV2Geu3Q"
	val e = "AQAB"

	return listOf(
		Jwk.fromNE("", n, e, use = "")
	)
}

internal fun getCertificateLightTestKey() = Jwk.fromXY(LIGHT_TEST_KID, LIGHT_TEST_X, LIGHT_TEST_Y, use = "l")

internal fun getHardcodedSigningKeys(flavor: String): List<Jwk> {
	val jwks = mutableListOf<Jwk>()
	when (flavor) {
		"dev" -> {
			jwks.add(Jwk.fromNE(CH_DEV_KID, CH_DEV_N, CH_DEV_E, use = "sig"))

			jwks.add(Jwk.fromXY(LI_DEV_ABN_VACCINATION_KID, LI_DEV_ABN_VACCINATION_X, LI_DEV_ABN_VACCINATION_Y, use = "v"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_TEST_KID, LI_DEV_ABN_TEST_X, LI_DEV_ABN_TEST_Y, use = "t"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_RECOVERY_KID, LI_DEV_ABN_RECOVERY_X, LI_DEV_ABN_RECOVERY_Y, use = "r"))
		}
		"abn" -> {
			jwks.add(Jwk.fromNE(CH_ABN_KID, CH_ABN_N, CH_ABN_E, use = "sig"))

			jwks.add(Jwk.fromXY(LI_DEV_ABN_VACCINATION_KID, LI_DEV_ABN_VACCINATION_X, LI_DEV_ABN_VACCINATION_Y, use = "v"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_TEST_KID, LI_DEV_ABN_TEST_X, LI_DEV_ABN_TEST_Y, use = "t"))
			jwks.add(Jwk.fromXY(LI_DEV_ABN_RECOVERY_KID, LI_DEV_ABN_RECOVERY_X, LI_DEV_ABN_RECOVERY_Y, use = "r"))
		}
		else -> {
			jwks.add(Jwk.fromNE(CH_PROD_KID, CH_PROD_N, CH_PROD_E, use = "sig"))
		}
	}
	return jwks
}

internal enum class Vaccine(val identifier: String, val manufacturer: String, val prophylaxis: String) {
	BIONTECH("EU/1/20/1528", "ORG-100030215", "1119349007"),
	MODERNA("EU/1/20/1507", "ORG-100031184", "1119349007"),
	JANSSEN("EU/1/20/1525", "ORG-100001417", "J07BX03"),
	ASTRA_ZENECA("EU/1/21/1529", "ORG-100001699", "J07BX03"),
	TOURIST_BBIBP_CORV_T("BBIBP-CorV_T", "ORG-100020693", "J07BX03"),
	TOURIST_CORONAVAC_T("CoronaVac_T", "Sinovac-Biotech", "J07BX03"),
	TOURIST_COVAXIN_T("Covaxin_T", "Bharat-Biotech", "J07BX03")
}

/* Certificate Light Test Key */
private const val LIGHT_TEST_KID = "AAABAQICAwM="
private const val LIGHT_TEST_X = "ceBrQgj3RwWzoxkv8/vApqkB7yJGfpBC9TjeIiXUR0U="
private const val LIGHT_TEST_Y = "g9ufnhfjFLVIiQYeQWmQATN/CMiVbfAgFp/08+Qqv2s="

/* Switzerland's public keys */
private const val CH_DEV_KID = "Ov9pjL/TkIw="
private const val CH_DEV_N =
	"AMDOmdgTEhMBAcf1VoLqI3yDmh4fZtinoXHFz+M0UuDto8C0eLZwuP76Cmd4zevoe4h7Kvx/h3RtEsoJKliYfa7dqkDJtLprv+h1VxKDJj6/51W+gjmnt2qO6ShWHyR2Gd+ta+pL4G20fMCs7FqFUl55CmXTRu8NP9dE38D/m33FWbEgROHIdcE7ceb2ysHQzwyCChN1Z9rsiCGj/X6RKcbDSODTc89QrMXEsNqYzV5aikhLr8hBdwGVJt//j1j0sI3kn0hCKnPz6AVufSbPKMoD7qhLFmzIPQOOH+85hHp/uIte/UqfIOwsQDCBrf8Fs4u/kXWJ9sNOcvWQQOPAJpc="
private const val CH_DEV_E = "AQAB"

private const val CH_ABN_KID = "JLxre3vSwyg="
private const val CH_ABN_N =
	"ANG1XnHVRFARGgelLvFbV67VZzdBWvfoQHDtF3Iy4C1QwfPWOPobhjveGPd02ON8fXl0UVnDZXmnAUdDncw6QFDn3VG768NpzUm+ToYShvph27gWiJliqb4pmtAXitBondNSBvLvN0igTmm1N+FlJ+Zt+5j49GKJ6hTso58ghNcK52nhveZYdGQuVglAdgajSOGWUF8AwgguUk5Gt5dNmTQCBzKBy5oKgKlm110ua+NZbbpm0UWlRruO6UlEac8/8AmXqeh55oTbzhP0+ZTc5aJcYHJbSnO1WbXKGZyvSRZE+7ZOBkdh+JpwNZcQBzpCTmhJGcU+ja5ua/DrwNMm7jE="
private const val CH_ABN_E = "AQAB"

private const val CH_PROD_KID = "Ll3NP03zOxY="
private const val CH_PROD_N =
	"ALZP+dbLSV1OPEag9pYeCHbMRa45SX5kwqy693EDRF5KxKCNzhFfDZ6LRNUY1ZkK6i009OKMaVpXGzKJV7SQbbt6zoizcEL8lRG4/8UnOik/OE6exgaNT/5JLp2PlZmm+h1Alf6BmWJrHYlD/zp0z1+lsunXpQ4Z64ByA7Yu9/00rBu2ZdVepJu/iiJIwJFQhA5JFA+7n33eBvhgWdAfRdSjk9CHBUDbw5tM5UTlaBhZZj0vA1payx7iHTGwdvNbog43DfpDVLe61Mso+kxYF/VgoBAf+ZkATEWmlytc3g02jZJgtkuyFsYTELDAVycgHWw/QJ0DmXOl0YwWrju4M9M="
private const val CH_PROD_E = "AQAB"

/* Liechtenstein's public keys */
private const val LI_DEV_ABN_VACCINATION_KID = "pXjP4Y6sns4="
private const val LI_DEV_ABN_VACCINATION_X = "iO9c7u35s7GF1I6gTyy7W3l4WkEil7N6s/Zbs613fvo="
private const val LI_DEV_ABN_VACCINATION_Y = "ITx2eL6yzmysHC2jVab+YVoxiwKyZ9X3vAn56zyxCTU="

private const val LI_DEV_ABN_TEST_KID = "7/MOPvQI+WY="
private const val LI_DEV_ABN_TEST_X = "IjUSzW6EsS4U+yWuH9asbOHSH+KUeAVHcQ0xCIMOY5E="
private const val LI_DEV_ABN_TEST_Y = "37bBS8tAw4WoQQrehHpj/bjMbuDL4piC/loUgMaA8zY="

private const val LI_DEV_ABN_RECOVERY_KID = "dAacIEGMNcE="
private const val LI_DEV_ABN_RECOVERY_X = "dRONZMFNTpyg8cRP8uVmscHjdfKotSCTIfnZQb9NWuA="
private const val LI_DEV_ABN_RECOVERY_Y = "T8mYbbPQoU9PFssKpqiENWd7sZl4EMwH9hUVkr/bcyE="