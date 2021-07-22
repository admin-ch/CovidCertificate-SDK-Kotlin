# CovidCertificate-SDK-Kotlin

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL%202.0-brightgreen.svg)](https://github.com/admin-ch/CovidCertificate-SDK-Kotlin/blob/main/LICENSE)
![Build](https://github.com/admin-ch/CovidCertificate-SDK-Kotlin/actions/workflows/build.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/ch.admin.bag.covidcertificate/sdk-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22ch.admin.bag.covidcertificate%22%20AND%20a:%22sdk-core%22)

This is the implementation of the [Electronic Health Certificates (EHN) specification](https://github.com/ehn-digital-green-development/hcert-spec)
used to decode and verify the validity of COVID Certificates [in Switzerland](https://github.com/admin-ch/CovidCertificate-App-Android).

It is partially based on [these](https://github.com/ehn-digital-green-development/hcert-kotlin)
[two](https://github.com/DIGGSweden/dgc-java) implementations.

## How To Use

This repository is used internally in the [CovidCertificate-SDK-Android](https://github.com/admin-ch/CovidCertificate-SDK-Android) as well as the backend services 
that provide the functionality for the Swiss CovidCertificate apps. For usage in Android apps, please refer to the above mentioned SDK for Android.

For a change log, check out the [releases](https://github.com/admin-ch/CovidCertificate-SDK-Kotlin/releases) page.

The latest release is available on [Maven Central](https://search.maven.org/artifact/ch.admin.bag.covidcertificate/sdk-core/).
```groovy
implementation 'ch.admin.bag.covidcertificate:sdk-core:1.1.2'
```

## Repositories

* Android App: [CovidCertificate-App-Android](https://github.com/admin-ch/CovidCertificate-App-Android)
* Android SDK: [CovidCertificate-SDK-Android](https://github.com/admin-ch/CovidCertificate-SDK-Android)
* iOS App: [CovidCertificate-App-iOS](https://github.com/admin-ch/CovidCertificate-App-iOS)
* iOS SDK: [CovidCertificate-SDK-iOS](https://github.com/admin-ch/CovidCertificate-SDK-iOS)
* For all others, see the [Github organisation](https://github.com/admin-ch/)

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file for details.
