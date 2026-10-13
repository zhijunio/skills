package com.example.sb4jpatask;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.example.sb4jpatask", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule controllersDoNotDependOnRepositories = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .because("controllers access persistence through the task service");

    @ArchTest
    static final ArchRule featurePackagesHaveNoCycles = slices()
            .matching("com.example.sb4jpatask.(*)..")
            .should().beFreeOfCycles();
}
