package com.leelo.service;

import com.leelo.model.AiDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaServiceTest {

    private final OllamaService service = new OllamaService();

    @Test
    void formatsAiDefinitionProperly() {
        AiDefinition def = new AiDefinition(
                "manzana",
                "A round fruit with red, yellow, or green skin and firm white flesh.",
                "Fruta redonda con piel roja, amarilla o verde y pulpa blanca y firme.",
                "She ate a crisp apple for breakfast."
        );

        String formatted = def.formatForTranslationArea();
        assertTrue(formatted.startsWith("manzana"));
        assertTrue(formatted.contains("📖 EN: A round fruit with red, yellow, or green skin and firm white flesh."));
        assertTrue(formatted.contains("📖 ES: Fruta redonda con piel roja, amarilla o verde y pulpa blanca y firme."));
        assertTrue(formatted.contains("💬 Ejemplo: She ate a crisp apple for breakfast."));
    }

    @Test
    void parsesDefinitionJsonCleanly() {
        String json = """
                {
                  "translation": "libro",
                  "english_definition": "A written or printed work consisting of pages glued or sewn together.",
                  "spanish_definition": "Una obra escrita o impresa que consta de páginas unidas.",
                  "english_example": "He read an interesting book about history."
                }
                """;

        AiDefinition def = service.parseDefinitionJson(json);
        assertNotNull(def);
        assertEquals("libro", def.translation());
        assertEquals("A written or printed work consisting of pages glued or sewn together.", def.englishDefinition());
        assertEquals("Una obra escrita o impresa que consta de páginas unidas.", def.spanishDefinition());
        assertEquals("He read an interesting book about history.", def.englishExample());
    }

    @Test
    void parsesDefinitionJsonWithMarkdownTicks() {
        String jsonWithTicks = """
                ```json
                {
                  "translation": "perro",
                  "english_definition": "A domesticated carnivorous mammal.",
                  "spanish_definition": "Un mamífero carnívoro domesticado.",
                  "english_example": "The dog barked happily."
                }
                ```
                """;

        AiDefinition def = service.parseDefinitionJson(jsonWithTicks);
        assertNotNull(def);
        assertEquals("perro", def.translation());
        assertEquals("The dog barked happily.", def.englishExample());
    }

    @Test
    void parsesFullOllamaChatResponse() {
        String chatResponse = """
                {
                  "model": "llama3.2",
                  "created_at": "2026-11-23T16:00:00Z",
                  "message": {
                    "role": "assistant",
                    "content": "{\\"translation\\": \\"casa\\", \\"english_definition\\": \\"A building for human habitation.\\", \\"spanish_definition\\": \\"Un edificio para la habitación humana.\\", \\"english_example\\": \\"They bought a new house.\\"}"
                  },
                  "done": true
                }
                """;

        AiDefinition def = service.parseChatResponse(chatResponse);
        assertNotNull(def);
        assertEquals("casa", def.translation());
        assertEquals("A building for human habitation.", def.englishDefinition());
        assertEquals("Un edificio para la habitación humana.", def.spanishDefinition());
        assertEquals("They bought a new house.", def.englishExample());
    }

    @Test
    void parsesTruncatedJsonGracefully() {
        // Simulates the exact user error: string was cut off mid-sentence without closing quote
        String truncatedJson = """
                {
                  "translation": "manzana",
                  "english_definition": "A round fruit with red or green skin.",
                  "spanish_definition": "Fruta redonda con piel roja o verde.",
                  "english_example": "She took a bite of the crisp
                """;

        AiDefinition def = service.parseDefinitionJson(truncatedJson);
        assertNotNull(def);
        assertEquals("manzana", def.translation());
        assertEquals("A round fruit with red or green skin.", def.englishDefinition());
        assertEquals("Fruta redonda con piel roja o verde.", def.spanishDefinition());
        assertTrue(def.englishExample().contains("She took a bite of the crisp"));
    }
}
