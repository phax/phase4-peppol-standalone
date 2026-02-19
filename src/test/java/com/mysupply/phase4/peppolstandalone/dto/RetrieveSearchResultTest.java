package com.mysupply.phase4.peppolstandalone.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveSearchResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testSerialization_withDocumentIds_shouldReturnValidJson() throws JsonProcessingException {
        // Arrange
        RetrieveSearchResult result = new RetrieveSearchResult();
        UUID id1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID id2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
        result.addDocumentId(id1);
        result.addDocumentId(id2);

        // Act
        String json = objectMapper.writeValueAsString(result);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("documentIds"));
        assertTrue(json.contains("11111111-1111-1111-1111-111111111111"));
        assertTrue(json.contains("22222222-2222-2222-2222-222222222222"));
    }

    @Test
    void testSerialization_emptyList_shouldReturnEmptyArray() throws JsonProcessingException {
        // Arrange
        RetrieveSearchResult result = new RetrieveSearchResult();

        // Act
        String json = objectMapper.writeValueAsString(result);

        // Assert
        assertNotNull(json);
        assertEquals("{\"documentIds\":[]}", json);
    }

    @Test
    void testSerialization_shouldNotThrowStackOverflowError() {
        // Arrange
        RetrieveSearchResult result = new RetrieveSearchResult();
        result.addDocumentId(UUID.randomUUID());
        result.addDocumentId(UUID.randomUUID());
        result.addDocumentId(UUID.randomUUID());

        // Act & Assert - Hvis der er cirkulær reference vil denne test fejle med StackOverflowError
        assertDoesNotThrow(() -> {
            String json = objectMapper.writeValueAsString(result);
            System.out.println("Generated JSON: " + json);
        });
    }
}

