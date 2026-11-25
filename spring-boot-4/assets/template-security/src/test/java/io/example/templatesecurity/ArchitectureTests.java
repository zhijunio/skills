package io.example.templatesecurity;

import com.enofex.taikai.Taikai;
import org.junit.jupiter.api.Test;

class ArchitectureTests {

    @Test
    void springArchitectureRulesPass() {
        Taikai.builder()
                .namespace("io.example.templatesecurity")
                .spring(spring -> spring.noAutowiredFields())
                .build()
                .checkAll();
    }
}
