# Standalone Peppol phase4

This an example standalone implementation of [phase4](https://github.com/phax/phase4) for the Peppol Network.

This is a demo application and NOT ready for production use.
Use it as a template to add your own code.

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

**Note:** Documents are NOT validated internally. They need to be validated externally.

## Building

This application is based on Spring Boot 3.x and uses Apache 3.x and Java 17 to build.

```
mvn clean install
```

The resulting Spring Boot application is afterwards available as `target/phase4-peppol-standalone-x.y.z.jar` (`x.y.z` is the version number).

## Configuration

The main configuration is done via the file `src/main/resources/application.properties`.
You may need to rebuild the application to have an effect. 

## Running

If you run it with `java -jar target/phase4-peppol-standalone-x.y.z.jar` it will spawn a local Tomcat at port `8080` and you can access it via `http://localhost:8080`.
It should show a small introduction page. The `/as4` servlet itself has no user interface.

In case you run the application behind an HTTP proxy, modify the settings in the configuration file (`http.proxy.*`).

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
