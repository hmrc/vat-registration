# vat-registration

[![Build Status](https://travis-ci.org/hmrc/vat-registration.svg)](https://travis-ci.org/hmrc/vat-registration) [ ![Download](https://api.bintray.com/packages/hmrc/releases/vat-registration/images/download.svg) ](https://bintray.com/hmrc/releases/vat-registration/_latestVersion)

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

## Prior to committing
```
sbt clean coverage test it:test scalastyle coverageReport
```
alternatively, create an alias for the above line, and get in the habit of running it before checking in:

```bash
alias precommit="sbt clean coverage test it:test scalastyle coverageReport" 
```

### NOTE: Only commit if test coverage report is above or equal to 95%, scalastyle warnings are corrected and tests green.

## Running locally
User service manager to run all services required by VAT Registration backend:

```bash
sm --start VAT_REG_DEP -f
```
Note this will start the VAT registration backend itself too, as it's included in the profile.

alternatively, create an alias for starting the services required for the backend:

```bash
alias vatreg='docker start graphite && sm --start AUTH AUTH_LOGIN_STUB AUTHENTICATOR BUS_REG CA_FRONTEND GG GG_STUBS USER_DETAILS KEYSTORE SAVE4LATER DATASTREAM ASSETS_FRONTEND -f'```
```

To run the service, just `cd` to cloned directory and execute:

```
sbt run
```
The service will come to life  @
http://localhost:9896/

## Further Documentation

[Documentation of TEST endpoints](test-endpoints.md)

