package verification.modulith;

import org.junit.jupiter.api.Test;
import com.tngtech.archunit.core.importer.ImportOption;
import org.springframework.modulith.core.ApplicationModules;
import static org.junit.jupiter.api.Assertions.*;

class ModuleStructureTest {
    // Synthetic module fixtures are intentionally located in test sources.
    private static final ImportOption FIXTURE_CLASSES = location -> true;

    @Test
    void allowsPublicApiDependency() {
        var modules = ApplicationModules.of("verification.modulith.valid", FIXTURE_CLASSES);
        assertEquals(2L, modules.stream().count(), "Both fixture modules must be discovered");
        assertDoesNotThrow(() -> modules.verify());
    }

    @Test
    void rejectsInternalDependency() {
        var violations = ApplicationModules.of("verification.modulith.invalid", FIXTURE_CLASSES).detectViolations();
        assertTrue(violations.hasViolations(), "Expected an internal package violation");
        String details = violations.toString();
        assertTrue(details.contains("InventoryRepository"), details);
    }

    @Test
    void rejectsCycle() {
        var violations = ApplicationModules.of("verification.modulith.cyclic", FIXTURE_CLASSES).detectViolations();
        assertTrue(violations.hasViolations(), "Expected a module cycle");
        assertTrue(violations.toString().toLowerCase(java.util.Locale.ROOT).contains("cycle"), violations.toString());
    }
}
