package dashboard;

/*
 * ClerkDashboard
 * 	- extends Dashboard
 * 	- 3 buttons on top: Dashboard, Inventory, Transactions
 * 	- the Dashboard screen has the "+ New Sale" button, Summary, Recent Transactions, and Product Catalog
 */

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.List;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;

import app.AgriSystem;
import model.*;
import service.InventoryService;
import service.ProductService;
import service.TransactionService;
import ui.InventoryScreen;
import ui.NewSaleScreen;
import ui.TransactionScreen;

public class ClerkDashboard extends Dashboard {
	// attributes
    private final Clerk clerk;
    private final Inventory inventory;
    private final InventoryService inventoryService;
    private final Label salesTodayValue = new Label("0");
    private final Label transactionsValue = new Label("0");
    private final Label voidedValue = new Label("0");
    private final Label asOfLabel = new Label();
    
    // product catalog and its filters
    private TableView<InventoryItem> productCatalog;
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final ComboBox<StatusFilter> statusBox = new ComboBox<>();
    private static final String ALL_CATEGORIES = "All Categories";
    private static final int RECENT_COUNT = 10; // how many rows the Recent Transactions table shows
    
    private final ObservableList<SalesTransaction> transactionHistory = FXCollections.observableArrayList();
    
    // buttons on top
    private Button dashboardButton;
    private Button inventoryButton;
    private Button transactionsButton;
    
    //constructor
    public ClerkDashboard(Clerk clerk, InventoryService inventoryService) {
			this.clerk = clerk;
			this.inventoryService = inventoryService;
			this.inventory = inventoryService.getInventory();
			
			// filters of the product catalog (made once so they keep their values)
	        searchField.setPromptText("Search by Product ID or name...");
	        searchField.setPrefWidth(250);
	        categoryBox.getItems().add(ALL_CATEGORIES);
	        for (Category c : Category.values()) {
	            categoryBox.getItems().add(c.toString());
	        }
	        categoryBox.setValue(ALL_CATEGORIES);
	        statusBox.getItems().addAll(StatusFilter.values());
	        statusBox.setValue(StatusFilter.ACTIVE);
	        
	        searchField.setOnKeyReleased(e -> refreshCatalog());
	        categoryBox.setOnAction(e -> refreshCatalog());
	        statusBox.setOnAction(e -> refreshCatalog());
			
			createButtons();
			buildDashboard();
    }
    
    // methods
    
    // creates the 3 buttons on top and what each one opens
    private void createButtons() {
    	List<Button> nav = createNavBar("Dashboard", "Inventory", "Transactions");
    	dashboardButton = nav.get(0);
    	inventoryButton = nav.get(1);
    	transactionsButton = nav.get(2);
    	
    	// a new screen is made every time a button is clicked, so the data is always up to date
    	dashboardButton.setOnAction(e -> buildDashboard());
    	inventoryButton.setOnAction(e -> showScreen(inventoryButton, new InventoryScreen(AgriSystem.getInstance())));
    	transactionsButton.setOnAction(e -> showScreen(transactionsButton, new TransactionScreen(AgriSystem.getInstance())));
    }
    
    // builds the Dashboard screen: welcome, + New Sale, as of, summary, recent transactions, product catalog
    private void buildDashboard() {
        transactionHistory.setAll(getRecentTransactions());
        refreshStats();        
        
        Label welcome = new Label("Welcome, " + clerk.getDisplayName() + "!");
        
        // + New Sale opens the cart screen
        Button newSaleBtn = new Button("+ New Sale");
        newSaleBtn.setOnAction(event -> showScreen(null, createPurchaseEntry()));

        // keeping track of today's sales, number of transactions, and number of voided transactions
        VBox summary = new VBox(5,
        		new Label("SUMMARY"),
        		createStatColumn(
        				createStatCard("Today's Sales", salesTodayValue),
        				createStatCard("Transactions Today", transactionsValue),
        				createStatCard("Voided Today", voidedValue)));

        // creates a table for viewing this clerk's most recent transactions
        TableView<SalesTransaction> historyTable = createTransactionTable();
        VBox recent = new VBox(5, new Label("RECENT TRANSACTIONS"), historyTable);
        
        HBox topRow = new HBox(30, summary, recent);
        
        // creates a product catalog table with its search bar and dropdowns
        productCatalog = createProductCatalog();
        refreshCatalog();
        HBox filters = new HBox(10, searchField, categoryBox, statusBox);
        
        VBox mainContent = new VBox(
        		15,
        		welcome,
        		newSaleBtn,
        		asOfLabel,
                topRow,
                new Label("PRODUCT CATALOG"),
                filters,
                productCatalog
        );
        
        mainContent.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        showScreen(dashboardButton, scrollPane);
    }
    
    // creates a product catalog showing the product id, name, category, unit, price, stock, and status
    private TableView<InventoryItem> createProductCatalog() {

        TableView<InventoryItem> table = new TableView<>();
        
        TableColumn<InventoryItem, String> idColumn =
            new TableColumn<>("Product ID");

        idColumn.setCellValueFactory(cell ->
    		new SimpleStringProperty(
                cell.getValue().getProduct().getDisplayId()));

        TableColumn<InventoryItem, String> productColumn =
            new TableColumn<>("Name");

        productColumn.setCellValueFactory(cell ->
    		new SimpleStringProperty(
                cell.getValue().getProduct().getName()));

        TableColumn<InventoryItem, String> categoryColumn =
            new TableColumn<>("Category");

        categoryColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getProduct().getCategory().toString()));
        
        TableColumn<InventoryItem, String> unitColumn =
            new TableColumn<>("Unit");

        unitColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getProduct().getUnit()));

        TableColumn<InventoryItem, String> priceColumn =
            new TableColumn<>("Price");

        priceColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                formatPeso(cell.getValue().getProduct().getPrice())));

        TableColumn<InventoryItem, Integer> stockColumn =
            new TableColumn<>("Stock");

        stockColumn.setCellValueFactory(cell ->
            new SimpleIntegerProperty(
                cell.getValue().getCurrentStock()).asObject());

        TableColumn<InventoryItem, String> statusColumn =
            new TableColumn<>("Status");

        // getStatus() returns Inactive, Out of Stock, Low Stock, or Available
        statusColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getStatus()));

        table.getColumns().add(idColumn);
        table.getColumns().add(productColumn);
        table.getColumns().add(categoryColumn);
        table.getColumns().add(unitColumn);
        table.getColumns().add(priceColumn);
        table.getColumns().add(stockColumn);
        table.getColumns().add(statusColumn);

        table.setMinHeight(250);
        table.setPrefHeight(250);
        
        return table;
    }
    
    // searches the product catalog again using the search bar and dropdowns
    private void refreshCatalog() {
    	if (productCatalog == null) return;
    	
    	Category category = null; // null means all categories
    	for (Category c : Category.values()) {
    		if (c.toString().equals(categoryBox.getValue())) {
    			category = c;
    		}
    	}
    	
    	ProductService products = AgriSystem.getInstance().getProductService();
    	productCatalog.setItems(FXCollections.observableArrayList(
    			products.search(searchField.getText(), category, statusBox.getValue())));
    	productCatalog.refresh();
    }
    
    // acts as an online cash register/cart for processing purchases (the New Sale screen)
    private Node createPurchaseEntry() {
    	return new NewSaleScreen(AgriSystem.getInstance());
    }
    
    // creates the Recent Transactions table (Transaction ID, Time, Total, Status)
    private TableView<SalesTransaction> createTransactionTable() {
    	
        TableColumn<SalesTransaction, String> idColumn =
            new TableColumn<>("Transaction ID");

        idColumn.setCellValueFactory(cell ->
        	new SimpleStringProperty(
            		cell.getValue().getDisplayId()));
        
        TableColumn<SalesTransaction, String> timeColumn =
            new TableColumn<>("Time");

        timeColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
            		cell.getValue().getTimestamp().format(DateFormats.TIME)));
        
        TableColumn<SalesTransaction, String> totalColumn =
            new TableColumn<>("Total");

        totalColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                formatPeso(cell.getValue().getTotalAmount())
            ));
        
        TableColumn<SalesTransaction, String> statusColumn =
            new TableColumn<>("Status");

        statusColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().isVoided() ? "VOIDED" : "VALID"
            ));
        
        TableView<SalesTransaction> table = new TableView<>();
        
        idColumn.setPrefWidth(110);
        timeColumn.setPrefWidth(80);
        totalColumn.setPrefWidth(100);
        statusColumn.setPrefWidth(80);
        
        table.getColumns().add(idColumn);
        table.getColumns().add(timeColumn);
        table.getColumns().add(totalColumn);
        table.getColumns().add(statusColumn);

        table.setItems(transactionHistory);
        table.setPrefHeight(180);

        return table;
    }
    
    // this clerk's latest transactions (valid and voided), newest first
    private List<SalesTransaction> getRecentTransactions() {
    	List<SalesTransaction> recent = new java.util.ArrayList<>();
    	for (SalesTransaction t : transactions().getAll()) {
    		if (t.getClerkUsername().equals(clerk.getUsername()) && recent.size() < RECENT_COUNT) {
    			recent.add(t);
    		}
    	}
    	return recent;
    }
    
    // shared transaction service
    private TransactionService transactions() {
        return AgriSystem.getInstance().getTransactionService();
    }

    // refreshes Today's Sales / Transactions Today / Voided Today from this clerk's saved transactions
    private void refreshStats() {
        salesTodayValue.setText(formatPeso(transactions().totalSales(clerk.getUsername(), DateRange.TODAY)));
        transactionsValue.setText(String.valueOf(transactions().countTransactions(clerk.getUsername(), false, DateRange.TODAY)));
        voidedValue.setText(String.valueOf(transactions().countTransactions(clerk.getUsername(), true, DateRange.TODAY)));
        asOfLabel.setText("As of " + LocalDateTime.now().format(DateFormats.AS_OF));
    }
  
} // end of ClerkDashboard class
