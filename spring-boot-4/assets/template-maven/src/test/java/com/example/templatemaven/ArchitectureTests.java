package com.example.templatemaven;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.enofex.taikai.Taikai;
import com.example.templatemaven.architecturefixture.api.IllegalApiFixture;
import com.example.templatemaven.user.persistence.UserMapper;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

class ArchitectureTests {

    private static final ArchRule API_MUST_NOT_DEPEND_ON_PERSISTENCE = noClasses()
            .that()
            .resideInAPackage("..api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..persistence..");

    private static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_INFRASTRUCTURE = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "com.baomidou..", "org.apache.ibatis..");

    @Test
    void springRulesPassForApplicationPackage() {
        Taikai.builder()
                .namespace("com.example.templatemaven")
                .spring(spring -> spring.noAutowiredFields())
                .build()
                .checkAll();
    }

    @Test
    void featureLayersRespectDependencyDirection() {
        var applicationClasses = new ClassFileImporter()
                .importPackages(
                        "com.example.templatemaven.config",
                        "com.example.templatemaven.user",
                        "com.example.templatemaven.notification");

        API_MUST_NOT_DEPEND_ON_PERSISTENCE.check(applicationClasses);
        DOMAIN_MUST_NOT_DEPEND_ON_INFRASTRUCTURE.check(applicationClasses);
    }

    @Test
    void apiPersistenceRuleRejectsKnownIllegalDependency() {
        var fixtureClasses = new ClassFileImporter().importClasses(IllegalApiFixture.class, UserMapper.class);

        assertThrows(AssertionError.class, () -> API_MUST_NOT_DEPEND_ON_PERSISTENCE.check(fixtureClasses));
    }
}
