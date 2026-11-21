package ui;



import app.AgriSystem;
import dashboard.Dashboard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import model.Category;
import model.InventoryItem;
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
			
			ProductFormDialog.showAdd(AgriSystem.getInstance(), user);
			refresh();
			
		} // end of onAdd
	
	/*
	 * onEdit()
	 * 	- opens the Edit Product form for the selected product
	 */
	private void onEdit() {
		
		InventoryItem item = getSelectedItem();
		if (item == null) {
			return;
		} // end of if statement
		
		ProductFormDialog.showEdit(AgriSystem.getInstance(), user, item);
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
	
	
	
} // end of ProductManagementScreen class
