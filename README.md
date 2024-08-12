# Standalone Peppol phase4

This an example standalone implementation of [phase4](https://github.com/phax/phase4) for the Peppol Network.

This is a demo application and NOT ready for production use.
Use it as a template to add your own code.

**Note:** because it is demo code, no releases are created - you have to modify it anyway.

This project is part of my Peppol solution stack. See https://github.com/phax/peppol for other components and libraries in that area.

# Functionality

## Functionality Receiving

Based on the Servlet technology, the application takes AS4 messages via HTTP POST to `/as4`.

By default, all valid incoming messages are handled by class `CustomPeppolIncomingSBDHandlerSPI`.
This class contains a `TODO` where you need to implement the stuff you want to do with incoming messages.

## Functionality Sending

Sending is triggered via an HTTP POST request.

To send to a production endpoint (using SML) use this URL:
```
/sendprod/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}
```

To send to a test endpoint (using SMK) use this URL:
```
/sendtest/{senderId}/{receiverId}/{docTypeId}/{processId}/{countryC1}
```

In both cases, the payload to send must be the XML business document (like the UBL Invoice).
The outcome is a simple JSON document that contains most of the relevant details on sending.

Test call using the file `src\test\resources\external\example-invoice.xml` as the request body (note the URL escaping of special chars via the `%` sign):
`http://localhost:8080/sendtest/9915:phase4-test-sender/9915:helger/urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice%23%23urn:cen.eu:en16931:2017%23compliant%23urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1/urn:fdc:peppol.eu:2017:poacc:billing:01:1.0/GB`

**Note:** Documents are NOT validated internally. They need to be validated externally. See https://github.com/phax/phive and https://github.com/phax/phive-rules for this.

## What is not included

The following list contains the elements not considered for this demo application:

* You need your own Peppol certificate to make it work - the contained keystore is a dummy one only
* Document validation is not included
* Peppol Reporting is not included, as no backend connection is available

# Get it up and running

## Tasks

1. Prepare your Peppol Access Point Key Store according to the rules described at https://github.com/phax/phoss-smp/wiki/Certificate-setup
1. Configure your Key Store in the `application.properties` file - don't touch the Trust Store - it is part of the deployment.
1. Set the correct value of `peppol.seatid` in the `application.properties` file
1. Once the Peppol Certificate is configured, change the code snippet with `TODO` in file `ServletConfig` according to the comment (approx. line 192)
1. Note that incoming Peppol messages are only logged and discarded. Edit the code in class `CustomPeppolIncomingSBDHandlerSPI` to fix it.
1. Build and start the application (see below)  

## Building

This application is based on Spring Boot 3.x and uses Apache 3.x and Java 17 to build.

```
mvn clean install
```

The resulting Spring Boot application is afterwards available as `target/phase4-peppol-standalone-x.y.z.jar` (`x.y.z` is the version number).

An example Docker file is also present - see `docker-build.cmd` and `docker-run.cmd` for details.

## Configuration

The main configuration is done via the file `src/main/resources/application.properties`.
You may need to rebuild the application to have an effect.

## Running

If you run it with `java -jar target/phase4-peppol-standalone-x.y.z.jar` it will spawn a local Tomcat at port `8080` and you can access it via `http://localhost:8080`.
It should show a small introduction page. The `/as4` servlet itself has no user interface.

In case you run the application behind an HTTP proxy, modify the settings in the configuration file (`http.proxy.*`).

In case you don't like port 8080, also check the configuration file.

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
