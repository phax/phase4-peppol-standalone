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

To send to a test endpoint (using SMK) use this URL when the SBDH is already available (especially for Peppol Testbed):
```
/send
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

# API Key Authentication and Rotation Documentation (mySupply custom logic)

## Overview
This document outlines the modifications made to implement API key authentication and rotation for the `/send` endpoint. These changes enhance security by requiring valid API keys for access and providing a mechanism to rotate keys if compromised.

## Key Features
1. **API Key Authentication**: All requests to `/send` must include a valid API key.
2. **Key Rotation**: Admins can generate new API keys via `/api-keys/rotate` to revoke compromised keys.
3. **Secure Key Generation**: Keys are cryptographically secure and stored hashed (assumed; code shows plain storage).

## Authentication Header Configuration
* **Header Name**: Configurable via `peppol.api.key.header` (default: `X-API-Key`).
* **Example Request**:
```http
POST /send HTTP/1.1
X-API-Key: YOUR_API_KEY_HERE
Content-Type: application/json

[Binary Payload]
```

## Endpoints

### 1. Send Message (`POST /send`)
**Authentication**: Requires valid API key in header.
**Response**:
* `401 Unauthorized` if key is invalid/missing.
* `200 OK` with success/failure details.

**Example Error**:
```json
{
  "success": false,
  "error": "Authentication failed. Invalid or missing API key."
}
```

### 2. Rotate API Key (`POST /api-keys/rotate`) *Admin Only*
Generates a new API key, invalidating the old one.

**Parameters**:
* `keyName`: Name of the key to rotate (e.g., `peppol.api.key`).
* Requires valid API key in header for authorization.

**Example Request**:
```http
POST /api-keys/rotate?keyName=peppol.api.key HTTP/1.1
X-API-Key: CURRENT_ADMIN_KEY
```

**Success Response**:
```json
{
  "success": true,
  "message": "API key rotated successfully",
  "newKey": "NEW_SECURE_KEY_VALUE"
}
```

**Failure Cases**:
* `401 Unauthorized`: Invalid/missing API key.
* `400 Bad Request`: Key not found or rotation error.

## Security Details

### Key Generation
* **Method**: Uses `SecureRandom` with 512-bit entropy, Base64URL-encoded.
* **Storage**: Keys are stored in the `api_keys` table with timestamps (`created_at`, `last_used`).

### Validation Flow
1. Extract API key from the header.
2. Check against the database; update `last_used` on success.
3. Reject requests with invalid/missing keys.

## Database Schema (`api_keys` Table)

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGINT (PK) | Auto-incremented ID |
| `key_name` | VARCHAR | Key identifier (e.g., `peppol.api.key`) |
| `key_value` | VARCHAR | API key value |
| `created_at` | DATETIME | Creation timestamp |
| `last_used` | DATETIME | Last successful usage timestamp |