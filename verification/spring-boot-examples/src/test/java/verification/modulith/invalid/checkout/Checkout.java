package verification.modulith.invalid.checkout;

import verification.modulith.invalid.inventory.internal.InventoryRepository;

public class Checkout {
    private final InventoryRepository repository;
    public Checkout(InventoryRepository repository) { this.repository = repository; }
}
