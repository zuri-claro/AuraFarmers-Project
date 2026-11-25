package ui;

import app.AgriSystem;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.Category;
import model.InventoryItem;
import model.Product;
import model.User;
import service.InventoryService;
import service.ProductService;

/*
 * ProductFormDialog class
 * 	- the Add Product and Edit Product pop-ups
 * 	- showAdd() opens an empty form (with starting stock), showEdit() opens the form filled with a product
 * 	- when editing, the stock is shown but locked (stock is changed in the Inventory screen)
 * 	- the form only collects the input; ProductService and InventoryService do the real checking and saving
 * 	- utility class, so nobody should create a ProductFormDialog object
 */

public final class ProductFormDialog {
	
	// METHODS
	/*
	 * ProductFormDialog()
	 * 	- private constructor method
	 */
	private ProductFormDialog() {}
	
	/*
	 * showAdd(AgriSystem, User)
	 * 	- opens the Add Product form
	 * 	- returns true if a product was added, false if the user cancelled
	 */
	public static boolean showAdd(AgriSystem system, User user) {
		return show(system, user, null);
	} // end of showAdd
	
	/*
	 * showEdit(AgriSystem, User, InventoryItem)
	 * 	- opens the Edit Product form filled with the item's details
	 * 	- returns true if the product was saved, false if the user cancelled
	 */
	public static boolean showEdit(AgriSystem system, User user, InventoryItem item) {
		return show(system, user, item);
	} // end of showEdit
	
	/*
	 * show(AgriSystem, User, InventoryItem)
	 * 	- builds the form; a null item means adding a new product
	 */
	private static boolean show(AgriSystem system, User user, InventoryItem item) {
		
		boolean adding = (item == null);
		ProductService productService = system.getProductService();
		InventoryService inventoryService = system.getInventoryService();
		
		// the inputs
		TextField nameField = new TextField();
		nameField.setPromptText("e.g. Tomato Seeds");
		
		ComboBox<Category> categoryBox = new ComboBox<>();
		categoryBox.getItems().addAll(Category.values());
		categoryBox.setPromptText("Choose a category");
		
		TextField unitField = new TextField();
		unitField.setPromptText("e.g. Packet, Bag, Piece");
		
		TextField priceField = new TextField();
		priceField.setPromptText("0.00");
		
		TextField stockField = new TextField(adding ? "0" : String.valueOf(item.getCurrentStock()));
		TextField thresholdField = new TextField("5");
		
		TextArea descriptionArea = new TextArea();
		descriptionArea.setPromptText("Short description of the product");
		descriptionArea.setPrefRowCount(3);
		descriptionArea.setWrapText(true);
		
		// when editing: fill in the current details
		if (!adding) {
			Product product = item.getProduct();
			nameField.setText(product.getName());
			categoryBox.setValue(product.getCategory());
			unitField.setText(product.getUnit());
			priceField.setText(String.format("%.2f", product.getPrice()));
			thresholdField.setText(String.valueOf(item.getLowStockThreshold()));
			descriptionArea.setText(product.getDescription());
			stockField.setDisable(true); // stock is changed in the Inventory screen
		} // end of if statement
		
		// the layout (same as the Figma dialog)
		FormDialog dialog = new FormDialog(adding ? "Add Product" : "Edit Product", "Save");
		dialog.addField("Product Name", nameField);
		dialog.addRow(FormDialog.labeled("Category", categoryBox, null), FormDialog.labeled("Unit", unitField, null));
		dialog.addRow(FormDialog.labeled("Price (\u20B1)", priceField, null),
				adding ? FormDialog.labeled("Starting Stock", stockField, null)
						: FormDialog.labeled("Current Stock", stockField, "Change stock in Inventory."));
		
		Label thresholdHint = new Label("A Low Stock alert shows when stock reaches this number.");
		thresholdHint.getStyleClass().add("form-hint");
		thresholdHint.setWrapText(true);
		VBox hintBox = new VBox(thresholdHint);
		hintBox.setAlignment(Pos.BOTTOM_LEFT);
		dialog.addRow(FormDialog.labeled("Low-Stock Threshold", thresholdField, null), hintBox);
		
		dialog.addField("Description", descriptionArea);
		
		// what happens on Save: returns null if it worked, or the error message to show in the dialog
		return dialog.showAndWait(() -> {
			
			try {
				// the required text fields are checked first, so the first problem shown is the first one in the form
				productService.checkRequiredFields(nameField.getText(), categoryBox.getValue(), unitField.getText());
				double price = parsePrice(priceField.getText());
				int threshold = parseWholeNumber(thresholdField.getText(), "Low-Stock Threshold");
				if (threshold < 0) {
					return "Low-stock threshold cannot be negative.";
				} // end of if statement
				
				if (adding) {
					int startingStock = parseWholeNumber(stockField.getText(), "Starting Stock");
					productService.addProduct(user, nameField.getText(), descriptionArea.getText(), price,
							categoryBox.getValue(), unitField.getText(), startingStock, threshold);
					
				} else {
					productService.updateProduct(user, item.getProductId(), nameField.getText(), descriptionArea.getText(),
							price, categoryBox.getValue(), unitField.getText());
					
					// the threshold belongs to the inventory, so it is saved separately
					if (threshold != item.getLowStockThreshold()) {
						String error = inventoryService.setThreshold(user, item.getProductId(), threshold);
						if (error != null) {
							return error;
						} // end of if statement
					} // end of if statement
				} // end of if else statement
				
				return null; // everything worked
				
			} catch (RuntimeException e) { // SecurityException or IllegalArgumentException from the services
				return e.getMessage();
			} // end of try catch
			
		});
		
	} // end of show
	
	/*
	 * parseWholeNumber(String, String)
	 * 	- turns text into an int, or throws an IllegalArgumentException with a friendly message
	 */
	private static int parseWholeNumber(String text, String fieldName) {
		
		try {
			return Integer.parseInt(text.trim());
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(fieldName + " must be a whole number.");
		} // end of try catch
		
	} // end of parseWholeNumber
	
	/*
	 * parsePrice(String)
	 * 	- turns text like "35", "35.50", or "₱1,250" into a double, or throws an IllegalArgumentException
	 */
	private static double parsePrice(String text) {
		
		String cleaned = text.replace("\u20B1", "").replace(",", "").trim();
		try {
			return Double.parseDouble(cleaned);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Price must be a number, e.g. 35 or 35.50.");
		} // end of try catch
		
	} // end of parsePrice
	
} // end of ProductFormDialog class