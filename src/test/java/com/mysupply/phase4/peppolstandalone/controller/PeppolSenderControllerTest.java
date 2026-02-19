package com.mysupply.phase4.peppolstandalone.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysupply.phase4.peppolstandalone.APConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

class PeppolSenderControllerTest {

    //@Test
    void tesSendPeppolSbdhMessage() throws IOException {
        // Load test XML file from resources
        String testFilePath = "src/test/resources/Sbd_Oioubl_Invoice_v2.1.xml";
        byte[] xmlContent = Files.readAllBytes(Paths.get(testFilePath));

        // Create controller instance
        PeppolSenderController controller = new PeppolSenderController();

        // Mock APConfig to return test values
        try (MockedStatic<APConfig> mockedAPConfig = mockStatic(APConfig.class)) {
            mockedAPConfig.when(APConfig::getPhase4ApiRequiredToken).thenReturn("test-token");
            mockedAPConfig.when(APConfig::getPeppolStage)
                    .thenReturn(com.helger.peppol.servicedomain.EPeppolNetwork.TEST);

            // Call the canSend method directly
            // Dette vil fejle. Den vil forsøge at indlæse peppol.seatid, men det er NULL, så den korrekte App.config er ikke opsat.
            ResponseEntity<String> response = controller.sendPeppolSbdhMessage("test-token", xmlContent);
            String result = response.getBody();

            // Verify the result is valid JSON
            assertNotNull(result);
            assertFalse(result.isEmpty());

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(result);

            // Verify JSON structure
            assertTrue(jsonNode.has("lookupCompleted"), "Response should contain 'lookupCompleted' field");
            assertTrue(jsonNode.has("receiverExist"), "Response should contain 'receiverExist' field");

            // The values can be true or false depending on SMP lookup, but fields should exist
            boolean lookupCompleted = jsonNode.get("lookupCompleted").asBoolean();
            boolean receiverExist = jsonNode.get("receiverExist").asBoolean();

            System.out.println("Lookup completed: " + lookupCompleted);
            System.out.println("Receiver exists: " + receiverExist);
            System.out.println("Full response: " + result);

            // Since this is a real SMP/SML lookup, we just verify the structure is correct
            // The actual values depend on whether the receiver exists in the test environment
        }
    }

    @Test
    void testCanSendPeppolSbdhMessage() throws IOException {
        // Load test XML file from resources
        String testFilePath = "src/test/resources/Sbd_Oioubl_Invoice_v2.1.xml";
        byte[] xmlContent = Files.readAllBytes(Paths.get(testFilePath));

        // Create controller instance
        PeppolSenderController controller = new PeppolSenderController();

        // Mock APConfig to return test values
        try (MockedStatic<APConfig> mockedAPConfig = mockStatic(APConfig.class)) {
            mockedAPConfig.when(APConfig::getPhase4ApiRequiredToken).thenReturn("test-token");
            mockedAPConfig.when(APConfig::getPeppolStage)
                    .thenReturn(com.helger.peppol.servicedomain.EPeppolNetwork.TEST);

            // Call the canSend method directly
            ResponseEntity<String> response = controller.canSendPeppolSbdhMessage("test-token", xmlContent);
            String result = response.getBody();

            // Verify the result is valid JSON
            assertNotNull(result);
            assertFalse(result.isEmpty());

            // Parse JSON response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(result);

            // Verify JSON structure
            assertTrue(jsonNode.has("lookupCompleted"), "Response should contain 'lookupCompleted' field");
            assertTrue(jsonNode.has("receiverExist"), "Response should contain 'receiverExist' field");

            // The values can be true or false depending on SMP lookup, but fields should exist
            boolean lookupCompleted = jsonNode.get("lookupCompleted").asBoolean();
            boolean receiverExist = jsonNode.get("receiverExist").asBoolean();

            System.out.println("Lookup completed: " + lookupCompleted);
            System.out.println("Receiver exists: " + receiverExist);
            System.out.println("Full response: " + result);

            // Since this is a real SMP/SML lookup, we just verify the structure is correct
            // The actual values depend on whether the receiver exists in the test environment
        }
    }



    @Test
    void testCannotSendWithInvalidToken() {
        // Load test XML file
        try {
            String testFilePath = "src/test/resources/Sbd_Oioubl_Invoice_v2.0.xml";
            byte[] xmlContent = Files.readAllBytes(Paths.get(testFilePath));

            PeppolSenderController controller = new PeppolSenderController();

            try (MockedStatic<APConfig> mockedAPConfig = mockStatic(APConfig.class)) {
                mockedAPConfig.when(APConfig::getPhase4ApiRequiredToken).thenReturn("test-token");

                // Test with invalid token - should throw HttpForbiddenException
                assertThrows(HttpForbiddenException.class, () -> {
                    controller.canSendPeppolSbdhMessage("invalid-token", xmlContent);
                });
            }
        } catch (IOException e) {
            fail("Failed to load test file: " + e.getMessage());
        }
    }

    @Test
    void testCanSendWithInvalidXml() {
        byte[] invalidXml = "invalid xml content".getBytes();

        PeppolSenderController controller = new PeppolSenderController();

        try (MockedStatic<APConfig> mockedAPConfig = mockStatic(APConfig.class)) {
            mockedAPConfig.when(APConfig::getPhase4ApiRequiredToken).thenReturn("test-token");
            mockedAPConfig.when(APConfig::getPeppolStage)
                    .thenReturn(com.helger.peppol.servicedomain.EPeppolNetwork.TEST);

            // Call with invalid XML
            ResponseEntity<String> response = controller.canSendPeppolSbdhMessage("test-token", invalidXml);
            String result = response.getBody();

            // Should return JSON with failed lookup
            assertNotNull(result);
            assertTrue(result.contains("\"lookupCompleted\":false"));
            assertTrue(result.contains("\"receiverExist\":false"));

            System.out.println("Invalid XML response: " + result);
        }
    }
}
