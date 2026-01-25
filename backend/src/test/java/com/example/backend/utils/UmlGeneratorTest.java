package com.example.backend.utils;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class UmlGeneratorTest {

    @Test
    void main_writesUmlFile() throws Exception {
        Path temp = Files.createTempFile("uml_test", ".puml");
        try {
            UmlGenerator.main(new String[]{temp.toString()});
            String content = Files.readString(temp);
            assertThat(content).contains("@startuml");
            assertThat(content).contains("@enduml");
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
