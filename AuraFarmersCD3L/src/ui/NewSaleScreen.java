package ui;

import java.util.LinkedHashMap;
import java.util.Map;

import app.AgriSystem;
import dashboard.Dashboard;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Category;
import model.InventoryItem;
import model.SalesTransaction;
import model.User;
import service.InventoryService;
import service.ProductService;
import service.TransactionService;

/*
 * NewSaleScreen class
 * 	- extends BorderPane
 * 	- opened by the "+ New Sale" button of the Clerk Dashboard
 * 	- left part: search bar, category dropdown, and the active products with - quantity + buttons
 * 	- right part: the cart (each product with - quantity + and Remove), quantities, total,
 * 		cash received, change, and the Complete button
 * 	- nothing is sold until Complete is clicked (TransactionService.checkout)
 */
public class NewSaleScreen extends BorderPane {
	
	// ATTRIBUTES
	private final User user;
	private final ProductService productService;
	private final InventoryService inventoryService;
	private final TransactionService transactionService;
	
	private final Map<Integer, Integer> cart = new LinkedHashMap<>(); // product id -> quantity
	
	private final TableView<InventoryItem> table = new TableView<>();
	private final TextField searchField = new TextField();
	private final ComboBox<String> categoryBox = new ComboBox<>();
	
	private final VBox cartItems = new VBox(10);
	private final Label quantitiesValue = new Label("0");
	private final Label totalValue = new Label(Dashboard.formatPeso(0));
	private final TextField cashField = new TextField();
	private final Label changeValue = new Label(Dashboard.formatPeso(0));
	private final Label messageLabel = new Label();
	
	private static final String ALL_CATEGORIES = "All Categories";
	
	// METHODS
	/*
	 * NewSaleScreen(AgriSystem)
	 * 	- constructor method
	 */
	public NewSaleScreen(AgriSystem system) {
		
		user = system.getCurrentUser();
		productService = system.getProductService();
		inventoryService = system.getInventoryService();
		transactionService = system.getTransactionService();
		
		setupTable();
		setCenter(createProductPart());
		setRight(createCartPart());
		refreshProducts();
		refreshCart();
		
	} // end of constructor method
	
	/*
	 * createProductPart()
	 * 	- search bar, category dropdown, product table
	 */
	private VBox createProductPart() {
		
		searchField.setPromptText("Search by ID or name...");
		searchField.setPrefWidth(250);
		
		categoryBox.getItems().add(ALL_CATEGORIES);
		for (Category c : Category.values()) {
			categoryBox.getItems().add(c.toString());
		} // end of for loop
		categoryBox.setValue(ALL_CATEGORIES);
		
		searchField.setOnKeyReleased(e -> refreshProducts());
		categoryBox.setOnAction(e -> refreshProducts());
		
		HBox filters = new HBox(10, searchField, categoryBox);
		
		VBox left = new VBox(10, filters, table, messageLabel);
		left.getStyleClass().add("white-card");
		left.setPadding(new Insets(10));
		VBox.setVgrow(table, Priority.ALWAYS);
		return left;
		
	} // end of createProductPart
	
	/*
	 * createCartPart()
	 * 	- cart list on top, totals and Complete button below
	 */
	private VBox createCartPart() {
		
		Label cartTitle = new Label("Cart");
		
		cartTitle.getStyleClass().add("cart-title");
		ScrollPane cartScroll = new ScrollPane(cartItems);
		cartScroll.setFitToWidth(true);
		cartScroll.setPrefHeight(300);
		
		cashField.setPromptText("₱");
		cashField.setOnKeyReleased(e -> refreshChange()); // change updates while typing
		
		Button completeButton = new Button("Complete");
		completeButton.setOnAction(e -> onComplete());
		
		GridPane totals = new GridPane();
		totals.setHgap(20);
		totals.setVgap(8);
		totals.addRow(0, new Label("Quantities:"), quantitiesValue);
		totals.addRow(1, new Label("Total:"), totalValue);
		totals.addRow(2, new Label("Cash Received:"), cashField);
		totals.addRow(3, new Label("Change:"), changeValue);
		
		VBox right = new VBox(10, cartTitle, cartScroll, totals, completeButton);
		right.getStyleClass().add("white-card");
		right.setPadding(new Insets(10));
		right.setPrefWidth(340);
		VBox.setVgrow(cartScroll, Priority.ALWAYS);
		return right;
		
	} // end of createCartPart
	
	/*
	 * setupTable()
	 * 	- Product Name, Price, Available Stock, Quantity (- quantity + buttons)
	 */
	private void setupTable() {
		
		TableColumn<InventoryItem, String> nameColumn = new TableColumn<>("Product Name");
		nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProduct().getName()));
		nameColumn.setPrefWidth(180);
		
		TableColumn<InventoryItem, String> priceColumn = new TableColumn<>("Price");
		priceColumn.setCellValueFactory(c -> new SimpleStringProperty(Dashboard.formatPeso(c.getValue().getProduct().getPrice())));
		priceColumn.setPrefWidth(90);
		
		TableColumn<InventoryItem, String> stockColumn = new TableColumn<>("Available Stock");
		stockColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCurrentStock())));
		stockColumn.setPrefWidth(110);
		
		// quantity column: each row has its own - quantity + buttons
		TableColumn<InventoryItem, InventoryItem> quantityColumn = new TableColumn<>("Quantity");
		quantityColumn.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
		quantityColumn.setPrefWidth(130);
		quantityColumn.setCellFactory(column -> new TableCell<InventoryItem, InventoryItem>() {
			@Override
			protected void updateItem(InventoryItem item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
				} else {
					setGraphic(createQuantityControl(item));
				} // end of if else statement
			} // end of updateItem
		}); // end of setCellFactory
		
		table.getColumns().add(nameColumn);
		table.getColumns().add(priceColumn);
		table.getColumns().add(stockColumn);
		table.getColumns().add(quantityColumn);
		
	} // end of setupTable
	
	/*
	 * createQuantityControl(InventoryItem)
	 * 	- the "- 2 +" buttons used in both the product table and the cart
	 */
	private HBox createQuantityControl(InventoryItem item) {
		
		Button minusButton = new Button("-");
		Button plusButton = new Button("+");
		minusButton.getStyleClass().add("stepper-button");
		plusButton.getStyleClass().add("stepper-button");
		Label quantityLabel = new Label(String.valueOf(getCartQuantity(item)));
		
		minusButton.setOnAction(e -> changeQuantity(item, -1));
		plusButton.setOnAction(e -> changeQuantity(item, 1));
		
		HBox control = new HBox(8, minusButton, quantityLabel, plusButton);
		control.setAlignment(Pos.CENTER);
		return control;
		
	} // end of createQuantityControl
	
	/*
	 * getCartQuantity(InventoryItem)
	 * 	- returns how many of that product are in the cart (0 if none)
	 */
	private int getCartQuantity(InventoryItem item) {
		return cart.getOrDefault(item.getProductId(), 0);
	} // end of getCartQuantity
	
	/*
	 * changeQuantity(InventoryItem, int)
	 * 	- adds (1) or removes (-1) one unit of the product from the cart
	 * 	- cannot go above the available stock; at 0 the product is removed from the cart
	 */
	private void changeQuantity(InventoryItem item, int change) {
		
		int newQuantity = getCartQuantity(item) + change;
		
		// ERROR: not enough stock
		if (change > 0 && !inventoryService.hasEnoughStock(item.getProductId(), newQuantity)) {
			messageLabel.setText("Not enough stock for " + item.getProduct().getName()
					+ " (available: " + item.getCurrentStock() + ").");
			return;
		} // end of if statement
		
		messageLabel.setText("");
		if (newQuantity <= 0) {
			cart.remove(item.getProductId());
		} else {
			cart.put(item.getProductId(), newQuantity);
		} // end of if else statement
		
		table.refresh();
		refreshCart();
		
	} // end of changeQuantity
	
	/*
	 * refreshProducts()
	 * 	- searches again (active products only, since inactive ones cannot be sold)
	 */
	private void refreshProducts() {
		
		Category category = null; // null means all categories
		for (Category c : Category.values()) {
			if (c.toString().equals(categoryBox.getValue())) {
				category = c;
			} // end of if statement
		} // end of for loop
		
		table.setItems(FXCollections.observableArrayList(
				productService.search(searchField.getText(), category, false)));
		table.refresh();
		
	} // end of refreshProducts
	
	/*
	 * refreshCart()
	 * 	- rebuilds the cart list, the quantities, and the total
	 */
	private void refreshCart() {
		
		cartItems.getChildren().clear();
		
		for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
			
			InventoryItem item = productService.findItem(entry.getKey());
			int quantity = entry.getValue();
			double price = item.getProduct().getPrice();
			
			Label nameLabel = new Label(item.getProduct().getName());
			Label priceLabel = new Label(Dashboard.formatPeso(price) + " x " + quantity);
			Label lineTotalLabel = new Label(Dashboard.formatPeso(price * quantity));
			
			Button removeButton = new Button("Remove");
			removeButton.getStyleClass().add("danger-button");
			removeButton.setOnAction(e -> {
				cart.remove(item.getProductId());
				table.refresh();
				refreshCart();
			}); // end of setOnAction
			
			VBox details = new VBox(3, nameLabel, priceLabel, lineTotalLabel);
			VBox buttons = new VBox(3, createQuantityControl(item), removeButton);
			buttons.setAlignment(Pos.CENTER);
			HBox.setHgrow(details, Priority.ALWAYS);
			
			cartItems.getChildren().add(new HBox(10, details, buttons));
			
		} // end of for loop
		
		quantitiesValue.setText(String.valueOf(getTotalQuantity()));
		totalValue.setText(Dashboard.formatPeso(getTotal()));
		refreshChange();
		
	} // end of refreshCart
	
	/*
	 * refreshChange()
	 * 	- change = cash received - total (blank if the cash is not a number yet)
	 */
	private void refreshChange() {
		
		try {
			double cash = Double.parseDouble(cashField.getText().trim());
			changeValue.setText(Dashboard.formatPeso(cash - getTotal()));
		} catch (NumberFormatException e) {
			changeValue.setText(Dashboard.formatPeso(0));
		} // end of try catch
		
	} // end of refreshChange
	
	/*
	 * getTotal()
	 * 	- total price of everything in the cart
	 */
	private double getTotal() {
		
		double total = 0;
		for (Map.Entry<Integer, Integer> entry : cart.entrySet()) {
			total += productService.findItem(entry.getKey()).getProduct().getPrice() * entry.getValue();
		} // end of for loop
		return Math.round(total * 100.0) / 100.0;
		
	} // end of getTotal
	
	/*
	 * getTotalQuantity()
	 * 	- total number of units in the cart
	 */
	private int getTotalQuantity() {
		
		int total = 0;
		for (int quantity : cart.values()) {
			total += quantity;
		} // end of for loop
		return total;
		
	} // end of getTotalQuantity
	
	/*
	 * onComplete()
	 * 	- checks the cart and cash, then completes the sale through TransactionService.checkout
	 * 	- possible errors: empty cart, invalid cash, not enough cash, stock changed
	 */
	private void onComplete() {
		
		// ERROR: empty cart
		if (cart.isEmpty()) {
			Dashboard.showError("The cart is empty.");
			return;
		} // end of if statement
		
		// ERROR: cash is not a number
		double cash;
		try {
			cash = Double.parseDouble(cashField.getText().trim());
		} catch (NumberFormatException e) {
			Dashboard.showError("Please enter a valid cash amount.");
			return;
		} // end of try catch
		
		// ERROR: not enough cash
		if (cash < getTotal()) {
			Dashboard.showError("Insufficient cash. The total is " + Dashboard.formatPeso(getTotal()) + ".");
			return;
		} // end of if statement
		
		SalesTransaction transaction;
		try {
			transaction = transactionService.checkout(user, new LinkedHashMap<>(cart), cash);
		} catch (RuntimeException e) { // e.g. not enough stock, inactive product
			Dashboard.showError(e.getMessage());
			return;
		} // end of try catch
		
		// checkout returns null if something was wrong
		if (transaction == null) {
			Dashboard.showError("The transaction could not be completed.");
			return;
		} // end of if statement
		
		Dashboard.showInfo("Transaction " + transaction.getDisplayId() + " completed.\nChange: "
				+ Dashboard.formatPeso(transaction.getChangeGiven()));
		
		// start a new cart
		cart.clear();
		cashField.clear();
		refreshProducts();
		refreshCart();
		
	} // end of onComplete
	
} // end of ProductManagementScreen class
