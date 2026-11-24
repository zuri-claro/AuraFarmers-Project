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
import javafx.scene.control.Label;


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
import ui.ProductDetailsDialog;
import ui.ProductGrid;
import ui.Theme;
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
    private ProductGrid productCatalog; // the product catalog shown as photo cards
    private static final int RECENT_COUNT = 3; // how many rows the Recent Transactions table shows
    
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
        welcome.getStyleClass().add("welcome-title");
        
        // + New Sale opens the cart screen
        Button newSaleBtn = new Button("+ New Sale");
        newSaleBtn.setOnAction(event -> showScreen(null, createPurchaseEntry()));

        // keeping track of today's sales, number of transactions, and number of voided transactions
        VBox summary = new VBox(5,
        		Theme.sectionTitle("SUMMARY"),
        		createStatColumn(
        				createStatCard("Today's Sales", salesTodayValue),
        				createStatCard("Transactions Today", transactionsValue),
        				createStatCard("Voided Today", voidedValue)));

        // creates a table for viewing this clerk's most recent transactions
        TableView<SalesTransaction> historyTable = createTransactionTable();
        VBox recent = new VBox(5, Theme.sectionTitle("RECENT TRANSACTIONS"), historyTable);
        
        HBox topRow = new HBox(30, summary, recent);
        
        // the product catalog as photo cards (category chips + search bar); "View" opens the product details
        productCatalog = new ProductGrid("Product Catalog", false, false, "View", item -> ProductDetailsDialog.show(item));
        
        // everything below the welcome text sits on one grey card, like in the Figma design
        VBox card = new VBox(15, asOfLabel, topRow, productCatalog);
        card.getStyleClass().add("content-card");
        card.setPadding(new Insets(16));
        
        VBox mainContent = new VBox(15, welcome, newSaleBtn, card);
        mainContent.setPadding(new Insets(10, 24, 24, 24));
        
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("no-card"); // this page builds its own card, so showScreen must not add another
        showScreen(dashboardButton, scrollPane);
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
        
        idColumn.setPrefWidth(120);
        timeColumn.setPrefWidth(90);
        totalColumn.setPrefWidth(110);
        statusColumn.setPrefWidth(90);
        
        table.getColumns().add(idColumn);
        table.getColumns().add(timeColumn);
        table.getColumns().add(totalColumn);
        table.getColumns().add(statusColumn);

        table.setItems(transactionHistory);
        table.setPrefHeight(170);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

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

