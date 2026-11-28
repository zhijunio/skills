package verification.modulith;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import static org.junit.jupiter.api.Assertions.*;

class ModuleStructureTest {
    @Test
    void allowsPublicApiDependency() {
        assertDoesNotThrow(() -> ApplicationModules.of("verification.modulith.valid").verify());
    }

    @Test
    void rejectsInternalDependency() {
        var violations = ApplicationModules.of("verification.modulith.invalid").detectViolations();
        assertTrue(violations.hasViolations(), "Expected an internal package violation");
        String details = violations.toString();
        assertTrue(details.contains("InventoryRepository"), details);
    }

    @Test
    void rejectsCycle() {
        var violations = ApplicationModules.of("verification.modulith.cyclic").detectViolations();
        assertTrue(violations.hasViolations(), "Expected a module cycle");
        assertTrue(violations.toString().toLowerCase(java.util.Locale.ROOT).contains("cycle"), violations.toString());
    }
}
