package ui;

import java.util.Optional;

import app.AgriSystem;
import dashboard.Dashboard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import model.Category;
import model.InventoryItem;
import model.Product;
import model.StatusFilter;
import model.User;
import service.ProductService;

/*
 * ProductManagementScreen class
 * 	- extends BorderPane
 * 	- only opened by the Manager
 * 	- search bar, category dropdown, and status dropdown on top
 * 	- product table in the middle
 * 	- Add Product, Edit Product, and Deactivate/Activate buttons at the bottom
 */
public class ProductManagementScreen extends BorderPane {
	
	// ATTRIBUTES
	private final ProductService service;
	private final User user;
	
	private final TableView<InventoryItem> table = new TableView<>();
	private final TextField searchField = new TextField();
	private final ComboBox<String> categoryBox = new ComboBox<>();
	private final ComboBox<StatusFilter> statusBox = new ComboBox<>();
	private final Button statusButton = new Button("Deactivate");
	
	private static final String ALL_CATEGORIES = "All Categories";
	
	// METHODS
	/*
	 * ProductManagementScreen(AgriSystem)
	 * 	- constructor method
	 */
	public ProductManagementScreen(AgriSystem system) {
		
		service = system.getProductService();
		user = system.getCurrentUser();
		
		setupTable();
		setTop(createTopPart());
		setCenter(table);
		setBottom(createBottomPart());
		refresh();
		
	} // end of constructor method
	
	/*
	 * createTopPart()
	 * 	- search bar, category dropdown, status dropdown
	 */
	private HBox createTopPart() {
		
		searchField.setPromptText("Search by Product ID or name...");
		searchField.setPrefWidth(300);
		
		categoryBox.getItems().add(ALL_CATEGORIES);
		for (Category c : Category.values()) {
			categoryBox.getItems().add(c.toString());
		} // end of for loop
		categoryBox.setValue(ALL_CATEGORIES);
		
		statusBox.getItems().addAll(StatusFilter.values());
		statusBox.setValue(StatusFilter.ACTIVE);
		
		// reapply the search whenever one of the three controls changes
		searchField.setOnKeyReleased(e -> refresh());
		categoryBox.setOnAction(e -> refresh());
		statusBox.setOnAction(e -> refresh());
		
		HBox top = new HBox(10, searchField, categoryBox, statusBox);
		top.setPadding(new Insets(10));
		return top;
		
	} // end of createTopPart
	
	/*
	 * createBottomPart()
	 * 	- Add Product, Edit Product, Deactivate buttons
	 */
	private HBox createBottomPart() {
		
		Button addButton = new Button("Add Product");
		Button editButton = new Button("Edit Product");
		
		addButton.setOnAction(e -> onAdd());
		editButton.setOnAction(e -> onEdit());
		statusButton.setOnAction(e -> onToggleStatus());
		
		// the third button says "Activate" if the selected product is inactive
		table.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
			if (newItem != null && !newItem.getProduct().isActive()) {
				statusButton.setText("Activate");
			} else {
				statusButton.setText("Deactivate");
			} // end of if else statement
		}); // end of addListener
		
		HBox bottom = new HBox(10, addButton, editButton, statusButton);
		bottom.setPadding(new Insets(10));
		return bottom;
		
	} // end of createBottomPart
	
	/*
	 * setupTable()
	 * 	- Product ID, Product Name, Category, Description, Unit, Price, Status
	 */
	private void setupTable() {
		
		table.getColumns().add(makeColumn("Product ID", 90, item -> item.getProduct().getDisplayId()));
		table.getColumns().add(makeColumn("Product Name", 180, item -> item.getProduct().getName()));
		table.getColumns().add(makeColumn("Category", 180, item -> item.getProduct().getCategory().toString()));
		table.getColumns().add(makeColumn("Description", 250, item -> item.getProduct().getDescription()));
		table.getColumns().add(makeColumn("Unit", 80, item -> item.getProduct().getUnit()));
		table.getColumns().add(makeColumn("Price", 80, item -> Dashboard.formatPeso(item.getProduct().getPrice())));
		table.getColumns().add(makeColumn("Status", 80, item -> item.getProduct().isActive() ? "Active" : "Inactive"));
		
	} // end of setupTable
	
	/*
	 * makeColumn(String, int, java.util.function.Function)
	 * 	- helper for making a text column
	 */
	private TableColumn<InventoryItem, String> makeColumn(String title, int width,
			java.util.function.Function<InventoryItem, String> getter) {
		
		TableColumn<InventoryItem, String> column = new TableColumn<>(title);
		column.setPrefWidth(width);
		column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
		return column;
		
	} // end of makeColumn
	
	/*
	 * refresh()
	 * 	- searches again using ProductService.search and puts the results in the table
	 */
	private void refresh() {
		
		Category category = null; // null means all categories
		for (Category c : Category.values()) {
			if (c.toString().equals(categoryBox.getValue())) {
				category = c;
			} // end of if statement
		} // end of for loop
		
		table.setItems(FXCollections.observableArrayList(
				service.search(searchField.getText(), category, statusBox.getValue())));
		table.refresh();
		
	} // end of refresh
	
	/*
	 * onAdd()
	 * 	- opens the product form, then adds the product through ProductService
	 */
	private void onAdd() {
		
		ProductForm form = new ProductForm(null);
		if (!form.show()) {
			return;
		} // end of if statement
		
		try {
			Product product = service.addProduct(user, form.getName(), form.getDescription(), form.getPrice(),
					form.getCategory(), form.getUnit(), form.getStock(), form.getThreshold());
			Dashboard.showInfo(product.getName() + " has been added as " + product.getDisplayId() + ".");
		} catch (RuntimeException e) { // SecurityException or IllegalArgumentException
			Dashboard.showError(e.getMessage());
		} // end of try catch
		refresh();
		
	} // end of onAdd
	
	/*
	 * onEdit()
	 * 	- opens the product form with the selected product's details, then saves the changes
	 */
	private void onEdit() {
		
		InventoryItem item = getSelectedItem();
		if (item == null) {
			return;
		} // end of if statement
		
		ProductForm form = new ProductForm(item.getProduct());
		if (!form.show()) {
			return;
		} // end of if statement
		
		try {
			service.updateProduct(user, item.getProductId(), form.getName(), form.getDescription(),
					form.getPrice(), form.getCategory(), form.getUnit());
		} catch (RuntimeException e) {
			Dashboard.showError(e.getMessage());
		} // end of try catch
		refresh();
		
	} // end of onEdit
	
	/*
	 * onToggleStatus()
	 * 	- deactivates an active product, activates an inactive one
	 */
	private void onToggleStatus() {
		
		InventoryItem item = getSelectedItem();
		if (item == null) {
			return;
		} // end of if statement
		
		boolean makeActive = !item.getProduct().isActive();
		String action = makeActive ? "Activate " : "Deactivate ";
		if (!Dashboard.confirm(action + item.getProduct().getName() + "?")) {
			return;
		} // end of if statement
		
		try {
			service.setProductActive(user, item.getProductId(), makeActive);
		} catch (RuntimeException e) {
			Dashboard.showError(e.getMessage());
		} // end of try catch
		refresh();
		
	} // end of onToggleStatus
	
	/*
	 * getSelectedItem()
	 * 	- returns either the selected row or null
	 */
	private InventoryItem getSelectedItem() {
		
		InventoryItem item = table.getSelectionModel().getSelectedItem();
		if (item == null) {
			Dashboard.showError("Select a product in the table first.");
		} // end of if statement
		return item;
		
	} // end of getSelectedItem
	
	/*
	 * ProductForm class
	 * 	- pop up form used by Add Product and Edit Product
	 * 	- when editing, starting stock and low-stock threshold are hidden
	 * 		(those are changed in the Inventory screen)
	 */
	private static class ProductForm {
		
		// ATTRIBUTES
		private final boolean adding;
		private final TextField nameField = new TextField();
		private final TextField descriptionField = new TextField();
		private final TextField priceField = new TextField();
		private final ComboBox<Category> categoryField = new ComboBox<>();
		private final TextField unitField = new TextField();
		private final TextField stockField = new TextField("0");
		private final TextField thresholdField = new TextField("5");
		
		private double price;
		private int stock;
		private int threshold;
		
		/*
		 * ProductForm(Product)
		 * 	- constructor method
		 * 	- null product means adding a new one
		 */
		ProductForm(Product product) {
			
			adding = (product == null);
			categoryField.getItems().addAll(Category.values());
			
			if (!adding) { // fill in the current details
				nameField.setText(product.getName());
				descriptionField.setText(product.getDescription());
				priceField.setText(String.valueOf(product.getPrice()));
				categoryField.setValue(product.getCategory());
				unitField.setText(product.getUnit());
			} // end of if statement
			
		} // end of constructor method
		
		/*
		 * show()
		 * 	- shows the pop up, returns true if the user clicked OK and the numbers are valid
		 * 	- the other rules (empty name, price of 0, etc.) are checked by ProductService
		 */
		boolean show() {
			
			GridPane grid = new GridPane();
			grid.setHgap(10);
			grid.setVgap(10);
			grid.setPadding(new Insets(10));
			
			grid.addRow(0, new Label("Product Name:"), nameField);
			grid.addRow(1, new Label("Description:"), descriptionField);
			grid.addRow(2, new Label("Price:"), priceField);
			grid.addRow(3, new Label("Category:"), categoryField);
			grid.addRow(4, new Label("Unit:"), unitField);
			if (adding) {
				grid.addRow(5, new Label("Starting Stock:"), stockField);
				grid.addRow(6, new Label("Low-Stock Threshold:"), thresholdField);
			} // end of if statement
			
			Dialog<ButtonType> dialog = new Dialog<>();
			dialog.setTitle(adding ? "Add Product" : "Edit Product");
			dialog.getDialogPane().setContent(grid);
			dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
			
			Optional<ButtonType> result = dialog.showAndWait();
			if (!result.isPresent() || result.get() != ButtonType.OK) {
				return false;
			} // end of if statement
			
			// ERROR: numbers must be valid
			try {
				price = Double.parseDouble(priceField.getText().trim());
			} catch (NumberFormatException e) {
				Dashboard.showError("Please enter a valid price.");
				return false;
			} // end of try catch
			
			try {
				stock = Integer.parseInt(stockField.getText().trim());
				threshold = Integer.parseInt(thresholdField.getText().trim());
			} catch (NumberFormatException e) {
				Dashboard.showError("Starting stock and threshold must be whole numbers.");
				return false;
			} // end of try catch
			
			return true;
			
		} // end of show
		
		/*
		 * GETTERS
		 */
		String getName() {return nameField.getText();}
		String getDescription() {return descriptionField.getText();}
		double getPrice() {return price;}
		Category getCategory() {return categoryField.getValue();}
		String getUnit() {return unitField.getText();}
		int getStock() {return stock;}
		int getThreshold() {return threshold;}
		
	} // end of ProductForm class
	
} // end of ProductManagementScreen class
