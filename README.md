# Standalone Peppol phase4

This an example standalone implementation of [phase4](https://github.com/phax/phase4) for the Peppol Network.

This is a template application and NOT ready for production use, because you need to take decisions and add some code.
Of course phase4 itself is ready for production use - see a list of [known phase4 users](https://github.com/phax/phase4/wiki/Known-Users) that have agreed to be publicly listed.

**Note:** because it is a template application, no releases are created - you have to modify it anyway.

This project is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.

# Functionality

## Functionality Receiving

Based on the Servlet technology, the application takes AS4 messages via HTTP POST to `/as4`.

By default, all valid incoming messages are handled by class `com.helger.phase4.peppolstandalone.spi.CustomPeppolIncomingSBDHandlerSPI`.
This class contains a `TODO` where you need to implement the stuff you want to do with incoming messages.
It also contains a lot of boilerplate code to show how certain things can be achieved (e.g. intergration with `peppol-reporting`).

## Functionality Sending

Sending is triggered via an HTTP POST request.

All the sending APIs mentioned below also require the HTTP Header `X-Token` to be present and have a specific value.
What value that is, depends on the configuration property `phase4.api.requiredtoken`.
The pre-configured value is `NjIh9tIx3Rgzme19mGIy` and should be changed in your own setup.

The actual Peppol Network choice (test or production network) is done based on the `peppol.stage` configuration parameter.

To send to an AS4 endpoint use this URL (the SBDH is built inside):
```
/sendas4/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}
```

To send to an AS4 endpoint use this URL when the SBDH is already available (especially for Peppol Testbed):
```
/sendsbdh
```

In both cases, the payload to send must be the XML business document (like the UBL Invoice).
The outcome is a JSON document that contains most of the relevant details on sending.

Test call using the file `src\test\resources\external\example-invoice.xml` as the request body (note the URL escaping of special chars via the `%` sign):
`http://localhost:8080/sendas4/9915:phase4-test-sender/9915:helger/urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice%23%23urn:cen.eu:en16931:2017%23compliant%23urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1/urn:fdc:peppol.eu:2017:poacc:billing:01:1.0/GB`

**Note:** Documents are NOT validated internally. They need to be validated externally. See https://github.com/phax/phive and https://github.com/phax/phive-rules for this.

## Peppol Reporting

Was added on 2025-02-16 as an example. On 2025-04-12 extended with the `do-peppol-reporting` API and the automatic scheduling.

By default every 2nd of the month, at 5:00am the scheduled job to create, validate, store and send the Peppol Reports is executed. The 2nd was chosen to definitively not run in timezone issues. 

Via `GET` on `/create-tsr/{year}/{month}` a Peppol Reporting Transaction Statistics Report (TSR) will be created. This does not validate or send the report.
The `year` parameter must be &ge; 2024 and the `month` parameter must be between `1` and `12`.
The response is a TSR XML in UTF-8 encoding. 

Via `GET` on `/create-eusr/{year}/{month}` a Peppol Reporting End User Statistics Report (EUSR) will be created. This does not validate or send the report.
The `year` parameter must be &ge; 2024 and the `month` parameter must be between `1` and `12`.
The response is an EUSR XML in UTF-8 encoding. 

Via `GET` on `/do-peppol-reporting/{year}/{month}` it will create TSR and EUSR reports, validate them, store them, send them to OpenPeppol and stores the sending reports of those.
The `year` parameter must be &ge; 2024 and the `month` parameter must be between `1` and `12`.
The response is a constant text showing that it was done.


## What is not included

The following list contains the elements not considered for this demo application:

* You need your own Peppol certificate to make it work - the contained keystore is a dummy one only
* Document validation is not included
    * See https://github.com/phax/phive and https://github.com/phax/phive-rules for this.
* Peppol Reporting is not included, as no reporting backend is present.
    * You can pick one from https://github.com/phax/peppol-reporting to add to your `pom.xml`
    * The calls for storing Peppol Reporting information is part of the code, but disabled by default, as relevant parameters cannot automatically be determined
    * The default storage of Peppol Reports is the file system - you may choose something else here as well (SQL, MongoDB etc.)

# Get it up and running

## Tasks

1. Prepare your Peppol Access Point Key Store according to the rules described at https://github.com/phax/phoss-smp/wiki/Certificate-setup
1. Set the correct value of `peppol.stage` in the `application.properties` file
1. Configure your Key Store in the `application.properties` file
1. Choose the correct Trust Store based on the Peppol Network stage (see above). Don't touch the Trust Store contents - they are part of the deployment.
1. Set the correct value of `peppol.seatid` in the `application.properties` file
1. Once the Peppol Certificate is configured, change the code snippet with `TODO` in file `ServletConfig` according to the comment (approx. line 215)
1. Note that incoming Peppol messages are only logged and discarded. Edit the code in class `CustomPeppolIncomingSBDHandlerSPI` to fix it.
1. Build and start the application (see below)

## Building

This application is based on Spring Boot 3.x and uses Apache 3.x and Java 17 (or higher) to build.

```
mvn clean install
```

The resulting Spring Boot application is afterwards available as `target/phase4-peppol-standalone-x.y.z.jar` (`x.y.z` is the version number).

An example Docker file is also present - see `docker-build.cmd` and `docker-run.cmd` for details.

## Configuration

The main configuration is done via the file `src/main/resources/application.properties`.
You may need to rebuild the application to have an effect.

The following configuration properties are contained by default:
* **`peppol.stage`** - defines the stage of the Peppol Network that should be used. Allowed values are `test` 
   (for the test/pilot Peppol Network) and `prod` (for the production Peppol Network). It defines e.g.
   the SML to be used and the CAs against which checks are performed
* **`peppol.seatid`** - defines your Peppol Seat ID. It could be taken from your AP certificate as well,
   but this way it is a bit easier.
* **`peppol.owner.countrycode`** - defines the country code of you as a Peppol Service Provider. Use the
   2-letter country code (as in `AT` for Austria). This is required to send the Peppol Reports to
   OpenPeppol.
* **`peppol.reporting.senderid`** - the sending Peppol Participant ID. For now, this can be e.g. the VAT
   number or organisational number of you as a Service Provider. In **the future** this will most likely need
   to be an SPID (using the `0242` participant scheme ID). Example value: `9915:TestReportSender`. This will be used
   as the sending Participant ID for sending Peppol Reports to OpenPeppol.
* **`peppol.reporting.scheduled`** - a boolean value to indicate, if the Peppol TSR and EUSR reports should
   automatically sent be towards OpenPeppol on a monthly basis. The cron rule is place is `0 0 5 2 * *`.

## Running

If you run it with `java -jar target/phase4-peppol-standalone-x.y.z.jar` it will spawn a local Tomcat at port `8080` and you can access it via `http://localhost:8080`.
It should show a small introduction page. The `/as4` servlet itself has no user interface.

In case you run the application behind an HTTP proxy, modify the settings in the configuration file (`http.proxy.*`).

In case you don't like port 8080, also change it in the configuration file.

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
