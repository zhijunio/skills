package verification.modulith.valid.checkout;

import verification.modulith.valid.inventory.Inventory;

public class Checkout {
    public boolean allowed(Inventory inventory) { return inventory.available(); }
}
