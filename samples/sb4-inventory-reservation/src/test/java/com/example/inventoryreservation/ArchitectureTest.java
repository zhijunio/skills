package com.example.inventoryreservation;

import java.util.Set;

import com.example.inventoryreservation.reservation.DirectStockAccessFixture;
import com.example.inventoryreservation.stock.StockService;
import com.example.inventoryreservation.stock.persistence.StockRepository;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.describe;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {
	private static final Set<String> STOCK_API = Set.of(StockService.class.getName(),
			StockService.Allocation.class.getName(), StockService.StockView.class.getName());
	private static final ArchRule ONLY_STOCK_API_CAN_BE_USED_OUTSIDE_STOCK = noClasses()
			.that().resideOutsideOfPackage("..stock..")
			.should().dependOnClassesThat(resideInAPackage("..stock..")
					.and(describe("are outside the stock public API", type -> !STOCK_API.contains(type.getName()))));

	@Test
	void controllersAndFeaturesRespectPersistenceBoundaries() {
		var code = new ClassFileImporter().withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackagesOf(InventoryReservationApplication.class);
		noClasses().that().haveSimpleNameEndingWith("Controller")
				.should().dependOnClassesThat().resideInAPackage("..persistence..").check(code);
		ONLY_STOCK_API_CAN_BE_USED_OUTSIDE_STOCK.check(code);
		noClasses().that().resideOutsideOfPackage("..reservation..")
				.should().dependOnClassesThat().resideInAPackage("..reservation.persistence..").check(code);
		noClasses().that().resideInAPackage("..stock..")
				.should().dependOnClassesThat().resideInAPackage("..reservation..").check(code);
	}

	@Test
	void directRepositoryAccessFromReservationFailsTheRule() {
		var fixture = new ClassFileImporter().importClasses(DirectStockAccessFixture.class, StockRepository.class);
		var result = ONLY_STOCK_API_CAN_BE_USED_OUTSIDE_STOCK.evaluate(fixture);
		assertThat(result.hasViolation()).isTrue();
		assertThat(result.getFailureReport().toString()).contains("DirectStockAccessFixture", "StockRepository");
	}
}
