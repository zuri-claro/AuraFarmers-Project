package ui;

import app.AgriSystem;
import dashboard.Dashboard;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import model.InventoryItem;
import model.User;
import service.ProductService;

/*
 * ProductManagementScreen class
 * 	- extends BorderPane
 * 	- only opened by the Manager
 * 	- the products are shown as photo cards (ProductGrid): category chips, search bar, and status dropdown on top
 * 	- click a card to choose it, double-click (or the Edit button on the card) to edit it
 * 	- Add Product, Edit Product, and Deactivate/Activate buttons at the bottom
 */
public class ProductManagementScreen extends BorderPane {
	
	// ATTRIBUTES
	private final ProductService service;
	private final User user;
	
	private final ProductGrid grid;
	private final Button statusButton = new Button("Deactivate");
	
	// METHODS
	/*
	 * ProductManagementScreen(AgriSystem)
	 * 	- constructor method
	 */
	public ProductManagementScreen(AgriSystem system) {
		
		service = system.getProductService();
		user = system.getCurrentUser();
		
		// the card button says "Edit"; double-clicking a card does the same
		grid = new ProductGrid("All Products", true, true, "Edit", item -> onEdit(item));
		grid.setOnItemDoubleClicked(item -> onEdit(item));
		
		// the third button says "Activate" if the chosen product is inactive
		grid.setOnSelectionChanged(item -> {
			if (item != null && !item.getProduct().isActive()) {
				statusButton.setText("Activate");
			} else {
				statusButton.setText("Deactivate");
			} // end of if else statement
		});
		
		grid.setPadding(new Insets(10));
		setCenter(grid);
		setBottom(createBottomPart());
		
	} // end of constructor method
	
	/*
	 * createBottomPart()
	 * 	- Add Product, Edit Product, Deactivate buttons
	 */
	private HBox createBottomPart() {
		
		Button addButton = new Button("Add Product");
		Button editButton = new Button("Edit Product");
		
		addButton.setOnAction(e -> onAdd());
		editButton.setOnAction(e -> {
			InventoryItem item = getSelectedItem();
			if (item != null) {
				onEdit(item);
			} // end of if statement
		});
		statusButton.setOnAction(e -> onToggleStatus());
		
		HBox bottom = new HBox(10, addButton, editButton, statusButton);
		bottom.setPadding(new Insets(10));
		return bottom;
		
	} // end of createBottomPart
	
	/*
	 * onAdd()
	 * 	- opens the Add Product form, then redraws the cards
	 */
	private void onAdd() {
		
		ProductFormDialog.showAdd(AgriSystem.getInstance(), user);
		grid.refresh();
		
	} // end of onAdd
	
	/*
	 * onEdit(InventoryItem)
	 * 	- opens the Edit Product form for the product, then redraws the cards
	 */
	private void onEdit(InventoryItem item) {
		
		ProductFormDialog.showEdit(AgriSystem.getInstance(), user, item);
		grid.refresh();
		
	} // end of onEdit
	
	/*
	 * onToggleStatus()
	 * 	- deactivates an active product, activates an inactive one (after asking to confirm)
	 */
	private void onToggleStatus() {
		
		InventoryItem item = getSelectedItem();
		if (item == null) {
			return;
		} // end of if statement
		
		boolean makeActive = !item.getProduct().isActive();
		String name = item.getProduct().getName();
		boolean confirmed = makeActive ? ConfirmDialog.confirmActivate(name) : ConfirmDialog.confirmDeactivate(name);
		if (!confirmed) {
			return;
		} // end of if statement
		
		try {
			service.setProductActive(user, item.getProductId(), makeActive);
		} catch (RuntimeException e) {
			Dashboard.showError(e.getMessage());
		} // end of try catch
		grid.refresh();
		
	} // end of onToggleStatus
	
	/*
	 * getSelectedItem()
	 * 	- returns the chosen product, or shows an error and returns null if no card is chosen
	 */
	private InventoryItem getSelectedItem() {
		
		InventoryItem item = grid.getSelectedItem();
		if (item == null) {
			Dashboard.showError("Click a product card first.");
		} // end of if statement
		return item;
		
	} // end of getSelectedItem
	
} // end of ProductManagementScreen class