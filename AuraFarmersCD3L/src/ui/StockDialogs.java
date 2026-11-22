package ui;

import javafx.scene.control.TextField;
import model.InventoryItem;
import model.User;
import service.InventoryService;

/*
 * StockDialogs class
 * 	- the Restock, Stock Adjustment, and Set Low-Stock Threshold pop-ups of the Inventory screen
 * 	- they use the same look as the Add/Edit Product dialogs (FormDialog)
 * 	- InventoryService does the real checking; if it returns an error message, the message is shown inside the dialog and the dialog stays open
 * 	- each method returns true if the change was saved, false if the user cancelled
 * 	- utility class, so nobody should create a StockDialogs object
 */

public final class StockDialogs {
	
	// METHODS
	/*
	 * StockDialogs()
	 * 	- private constructor method
	 */
	private StockDialogs() {}
	
	/*
	 * showRestock(InventoryService, User, InventoryItem)
	 * 	- asks how many units arrived and an optional note, then adds them to the stock
	 */
	public static boolean showRestock(InventoryService service, User user, InventoryItem item) {
		
		TextField quantityField = new TextField();
		quantityField.setPromptText("e.g. 50");
		TextField noteField = new TextField();
		noteField.setPromptText("e.g. Delivery from supplier");
		
		FormDialog dialog = new FormDialog("Restock Item", "Restock");
		dialog.addNote(item.getProduct().getName() + " - current stock: " + item.getCurrentStock());
		dialog.addField("Quantity Received", quantityField);
		dialog.addField("Note (optional)", noteField);
		
		return dialog.showAndWait(() -> {
			try {
				int quantity = Integer.parseInt(quantityField.getText().trim());
				return service.restock(user, item.getProductId(), quantity, noteField.getText()); // null if it worked
			} catch (NumberFormatException e) {
				return "Quantity must be a whole number.";
			} // end of try catch
		});
		
	} // end of showRestock
	
	/*
	 * showAdjust(InventoryService, User, InventoryItem)
	 * 	- sets the stock to a new number (e.g. after counting); a reason is required
	 */
	public static boolean showAdjust(InventoryService service, User user, InventoryItem item) {
		
		TextField quantityField = new TextField(String.valueOf(item.getCurrentStock()));
		TextField reasonField = new TextField();
		reasonField.setPromptText("e.g. Damaged goods, counting error");
		
		FormDialog dialog = new FormDialog("Stock Adjustment", "Save");
		dialog.addNote(item.getProduct().getName() + " - current stock: " + item.getCurrentStock());
		dialog.addField("New Stock Quantity", quantityField);
		dialog.addField("Reason (required)", reasonField);
		
		return dialog.showAndWait(() -> {
			try {
				int newQuantity = Integer.parseInt(quantityField.getText().trim());
				return service.adjust(user, item.getProductId(), newQuantity, reasonField.getText());
			} catch (NumberFormatException e) {
				return "New stock quantity must be a whole number.";
			} // end of try catch
		});
		
	} // end of showAdjust
	
	/*
	 * showThreshold(InventoryService, User, InventoryItem)
	 * 	- changes the number at which the product counts as low stock
	 */
	public static boolean showThreshold(InventoryService service, User user, InventoryItem item) {
		
		TextField thresholdField = new TextField(String.valueOf(item.getLowStockThreshold()));
		
		FormDialog dialog = new FormDialog("Low-Stock Threshold", "Save");
		dialog.addNote(item.getProduct().getName() + " - current stock: " + item.getCurrentStock());
		dialog.addField("Low-Stock Threshold", thresholdField);
		dialog.addNote("A Low Stock alert shows when stock reaches this number.");
		
		return dialog.showAndWait(() -> {
			try {
				int threshold = Integer.parseInt(thresholdField.getText().trim());
				return service.setThreshold(user, item.getProductId(), threshold);
			} catch (NumberFormatException e) {
				return "Threshold must be a whole number.";
			} // end of try catch
		});
		
	} // end of showThreshold
	
} // end of StockDialogs class