package dashboard;

import model.Clerk;
import model.DateFormats;
import model.DateRange;
import model.Inventory;
import model.InventoryItem;
import model.Manager;
import model.SalesTransaction;
import model.StockMovement;
import model.User;
import service.InventoryService;
import service.TransactionService;
import ui.InventoryScreen;
import ui.ProductManagementScreen;
import ui.Theme;
import ui.TransactionScreen;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.ArrayList;

import app.AgriSystem;

/*
 * ManagerDashboard class
 * - Extends Dashboard
 * - 4 buttons on top: Dashboard, Inventory, Product Management, Transactions
 * - the Dashboard screen has 3 tabs on the left: Overview, Clerk Monitoring, Low-Stock Alerts
 * - Displays inventory information and management overview
 * - Allows the Manager to monitor Clerks
 */

public class ManagerDashboard extends Dashboard {

    // ATTRIBUTES
    private final Manager manager;
    private final InventoryService inventoryService;
    private final Inventory inventory;

    private final ObservableList<Clerk> clerks =
            FXCollections.observableArrayList();

    // Inventory statistics
    private final Label activeProductsValue = new Label("0");
    private final Label lowStockValue = new Label("0");
    private final Label outOfStockValue = new Label("0");

    // Sales statistics
    private final Label totalSalesValue = new Label("0");
    private final Label completedTransactionsValue = new Label("0");
    private final Label voidedTransactionsValue = new Label("0");
    
    private final Label asOfLabel = new Label();

    // Actions for opening other screens
    private Consumer<Clerk> clerkDashboardAction;
    private Runnable inventoryAction;
    
    // Buttons on top and tabs on the left
    private Button dashboardButton;
    private Button inventoryButton;
    private Button productButton;
    private Button transactionsButton;
    
    private final Button overviewTab = new Button("Overview");
    private final Button clerkTab = new Button("Clerk Monitoring");
    private final Button lowStockTab = new Button("Low-Stock Alerts");
    
    private final BorderPane tabContent = new BorderPane(); // right side of the Dashboard screen

    /*
     * ManagerDashboard(Manager, InventoryService, List<User>)
     * - Constructor method
     * - Receives the Manager, inventory service, and registered users
     */
    public ManagerDashboard(Manager manager,
                            InventoryService inventoryService,
                            List<User> users) {

        this.manager = manager;
        this.inventoryService = inventoryService;

        // Gets the same inventory used by InventoryService
        this.inventory = inventoryService.getInventory();

        // Gets only the users who are Clerks
        for (User user : users) {

            if (user instanceof Clerk) {
                clerks.add((Clerk) user);
            }

        }
        
        // default actions (can still be replaced with the setters below)
        clerkDashboardAction = this::openClerkDashboard;
        inventoryAction = () -> inventoryButton.fire();

        buildDashboard();
        updateStatistics();

    } // end of constructor method
    
    /*
     * buildDashboard()
     * - Creates the buttons on top and opens the Dashboard screen
     */
    private void buildDashboard() {

        List<Button> nav = createNavBar("Dashboard", "Inventory", "Product Management", "Transactions");
        dashboardButton = nav.get(0);
        inventoryButton = nav.get(1);
        productButton = nav.get(2);
        transactionsButton = nav.get(3);
        
        // a new screen is made every time a button is clicked, so the data is always up to date
        dashboardButton.setOnAction(e -> showDashboardHome());
        inventoryButton.setOnAction(e -> showScreen(inventoryButton, new InventoryScreen(AgriSystem.getInstance())));
        productButton.setOnAction(e -> showScreen(productButton, new ProductManagementScreen(AgriSystem.getInstance())));
        transactionsButton.setOnAction(e -> showScreen(transactionsButton, new TransactionScreen(AgriSystem.getInstance())));
        
        // tabs on the left
        overviewTab.setOnAction(e -> showTab(overviewTab, createOverviewTab()));
        clerkTab.setOnAction(e -> showTab(clerkTab, createClerkMonitoringTab()));
        lowStockTab.setOnAction(e -> showTab(lowStockTab, createLowStockTab()));
        
        showDashboardHome();

    } // end of buildDashboard
    
    /*
     * showDashboardHome()
     * - "Welcome, displayname!" on top, tabs on the left, tab content on the right
     * - always opens on the Overview tab
     */
    private void showDashboardHome() {
    	
    	Label welcome = new Label("Welcome, " + manager.getDisplayName() + "!");
    	welcome.getStyleClass().add("welcome-title");
    	
    	Label tabsTitle = new Label("TABS");
    	tabsTitle.getStyleClass().add("tabs-title");
    	
    	// the side tabs look like the Figma menu; the content area sits on a grey card
    	Theme.addStyle(overviewTab, "tab-button");
    	Theme.addStyle(clerkTab, "tab-button");
    	Theme.addStyle(lowStockTab, "tab-button");
    	Theme.addStyle(tabContent, "content-card");
    	tabContent.setPadding(new Insets(10));
    	
    	overviewTab.setMaxWidth(Double.MAX_VALUE);
    	clerkTab.setMaxWidth(Double.MAX_VALUE);
    	lowStockTab.setMaxWidth(Double.MAX_VALUE);
    	
    	VBox tabs = new VBox(10, tabsTitle, overviewTab, clerkTab, lowStockTab);
    	tabs.setPrefWidth(190);
    	tabs.setAlignment(Pos.TOP_CENTER);
    	
    	HBox body = new HBox(15, tabs, tabContent);
    	HBox.setHgrow(tabContent, Priority.ALWAYS);
    	
    	VBox home = new VBox(10, welcome, body);
    	home.setPadding(new Insets(10, 24, 24, 24));
    	home.getStyleClass().add("no-card"); // this page builds its own card, so showScreen must not add another
    	VBox.setVgrow(body, Priority.ALWAYS);
    	
    	showScreen(dashboardButton, home);
    	overviewTab.fire();
    	
    } // end of showDashboardHome
    
    /*
     * showTab(Button, Node)
     * - puts the tab's content on the right, disables the selected tab
     */
    private void showTab(Button selected, Node content) {
    	
    	overviewTab.setDisable(selected == overviewTab);
    	clerkTab.setDisable(selected == clerkTab);
    	lowStockTab.setDisable(selected == lowStockTab);
    	tabContent.setCenter(content);
    	
    } // end of showTab
    
    /*
     * createOverviewTab()
     * - "As of" date, Inventory Overview, Sales Overview, Current Inventory, Recent Stock Movements
     */
    private Node createOverviewTab() {
    	
    	updateStatistics();
    	
    	VBox inventoryOverview = new VBox(5,
    			Theme.sectionTitle("Inventory Overview"),
    			createStatColumn(
    					createStatCard("Active Products", activeProductsValue),
    					createStatCard("Low Stock Products", lowStockValue),
    					createStatCard("Out of Stock Products", outOfStockValue)));
    	
    	VBox salesOverview = new VBox(5,
    			Theme.sectionTitle("Sales Overview"),
    			createStatColumn(
    					createStatCard("Total Sales", totalSalesValue),
    					createStatCard("Completed Transactions", completedTransactionsValue),
    					createStatCard("Voided Transactions", voidedTransactionsValue)));
    	
    	HBox overviews = new HBox(30, inventoryOverview, salesOverview);
    	
    	VBox content = new VBox(15,
    			asOfLabel,
    			overviews,
    			Theme.sectionTitle("Current Inventory"),
    			createInventoryTable(),
    			Theme.sectionTitle("Recent Stock Movements"),
    			createMovementTable());
    	content.setPadding(new Insets(10));
    	
        // Allows scrolling when the dashboard content is too long
    	ScrollPane scrollPane = new ScrollPane(content);
    	scrollPane.setFitToWidth(true);
    	return scrollPane;
    	
    } // end of createOverviewTab
    
    /*
     * createClerkMonitoringTab()
     * - Clerk Sales History on top, All Clerks below
     * - selecting a clerk shows only that clerk's sales; nothing selected shows every clerk's sales
     */
    private Node createClerkMonitoringTab() {
    	
    	TableView<SalesTransaction> historyTable = createSalesHistoryTable();
    	TableView<Clerk> clerkTable = createClerkTable();
    	
    	TransactionService transactions = AgriSystem.getInstance().getTransactionService();
    	historyTable.setItems(FXCollections.observableArrayList(
    			transactions.search(null, DateRange.ANY_TIME, false)));
    	
    	clerkTable.getSelectionModel().selectedItemProperty().addListener((obs, oldClerk, newClerk) -> {
    		if (newClerk == null) {
    			historyTable.setItems(FXCollections.observableArrayList(
    					transactions.search(null, DateRange.ANY_TIME, false)));
    		} else {
    			historyTable.setItems(FXCollections.observableArrayList(
    					transactions.getByClerk(newClerk.getUsername())));
    		} // end of if else statement
    	}); // end of addListener
    	
    	// Open Dashboard button (opens the selected clerk's dashboard)
    	Button openButton = new Button("Open Dashboard");
    	openButton.setOnAction(event -> {
    		
    		Clerk clerk = clerkTable.getSelectionModel().getSelectedItem();
    		if (clerk == null) {
    			showError("Select a clerk in the table first.");
    			return;
    		}
    		if (clerkDashboardAction != null) {
    			clerkDashboardAction.accept(clerk);
    		}
    		
    	});
    	
    	Region space = new Region();
    	HBox.setHgrow(space, Priority.ALWAYS);
    	HBox clerkHeader = new HBox(10, Theme.sectionTitle("All Clerks"), space, openButton);
    	clerkHeader.setAlignment(Pos.CENTER_LEFT);
    	
    	VBox content = new VBox(15,
    			Theme.sectionTitle("Clerk Sales History"),
    			historyTable,
    			clerkHeader,
    			clerkTable);
    	content.setPadding(new Insets(10));
    	
    	ScrollPane scrollPane = new ScrollPane(content);
    	scrollPane.setFitToWidth(true);
    	return scrollPane;
    	
    } // end of createClerkMonitoringTab
    
    /*
     * createLowStockTab()
     * - Low-Stock Products table and a Restock Product button
     */
    private Node createLowStockTab() {
    	
    	TableView<InventoryItem> table = createLowStockTable();
    	
    	Button restockButton = new Button("Restock Product +");
    	restockButton.setOnAction(event -> {
    		
    		InventoryItem item = table.getSelectionModel().getSelectedItem();
    		if (item == null) {
    			showError("Select a product in the table first.");
    			return;
    		}
    		
    		String quantityText = ask("Restock Product", item.getProduct().getName(), "Quantity received:", "");
    		if (quantityText == null) {
    			return;
    		}
    		String note = ask("Restock Product", item.getProduct().getName(), "Note (optional):", "");
    		if (note == null) {
    			return;
    		}
    		
    		try {
    			int quantity = Integer.parseInt(quantityText.trim());
    			String error = inventoryService.restock(manager, item.getProductId(), quantity, note);
    			if (error != null) {
    				showError(error);
    			}
    		} catch (NumberFormatException e) {
    			showError("Please enter a whole number.");
    		} // end of try catch
    		
    		// refresh the tab (a restocked product may no longer be low)
    		showTab(lowStockTab, createLowStockTab());
    		
    	});
    	
    	Region space = new Region();
    	HBox.setHgrow(space, Priority.ALWAYS);
    	HBox header = new HBox(10, Theme.sectionTitle("Low-Stock Products"), space, restockButton);
    	header.setAlignment(Pos.CENTER_LEFT);
    	
    	VBox content = new VBox(15, header, table);
    	content.setPadding(new Insets(10));
    	VBox.setVgrow(table, Priority.ALWAYS);
    	return content;
    	
    } // end of createLowStockTab


    /*
     * createInventoryTable()
     * - Displays products and their current stock information
     */
    private TableView<InventoryItem> createInventoryTable() {

        TableView<InventoryItem> table = new TableView<>();
    	table.setRowFactory(Theme.stockRowFactory());

        TableColumn<InventoryItem, String> nameColumn =
                new TableColumn<>("Product Name");

        nameColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getProduct().getName()
            )
        );

        TableColumn<InventoryItem, String> categoryColumn =
                new TableColumn<>("Category");

        categoryColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getProduct().getCategory().toString()
            )
        );

        TableColumn<InventoryItem, Integer> stockColumn =
                new TableColumn<>("Current Stock");

        stockColumn.setCellValueFactory(cell ->
            new SimpleIntegerProperty(
                cell.getValue().getCurrentStock()
            ).asObject()
        );

        TableColumn<InventoryItem, Integer> thresholdColumn =
                new TableColumn<>("Low Stock Threshold");

        thresholdColumn.setCellValueFactory(cell ->
            new SimpleIntegerProperty(
                cell.getValue().getLowStockThreshold()
            ).asObject()
        );

        table.getColumns().add(nameColumn);
        table.getColumns().add(categoryColumn);
        table.getColumns().add(stockColumn);
        table.getColumns().add(thresholdColumn);

        // Uses the same inventory data as InventoryService
        table.setItems(inventory.getItems());
        table.setPrefHeight(250);
        
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        return table;

    } // end of createInventoryTable


    /*
     * createMovementTable()
     * - Displays the recorded changes in inventory stock (newest first)
     */
    private TableView<StockMovement> createMovementTable() {

        TableView<StockMovement> table = new TableView<>();

        TableColumn<StockMovement, String> productIdColumn =
                new TableColumn<>("Product ID");

        // shown as AG001; if the product is somehow gone, just the number
        productIdColumn.setCellValueFactory(cell -> {
        	InventoryItem item = inventory.findByProductId(cell.getValue().getProductId());
        	String id = (item == null) ? String.valueOf(cell.getValue().getProductId())
        			: item.getProduct().getDisplayId();
        	return new SimpleStringProperty(id);
        });

        TableColumn<StockMovement, Integer> quantityColumn =
                new TableColumn<>("Quantity");

        quantityColumn.setCellValueFactory(cell ->
            new SimpleIntegerProperty(
                cell.getValue().getQuantity()
            ).asObject()
        );
        quantityColumn.setPrefWidth(50);

        TableColumn<StockMovement, Integer> stockColumn =
                new TableColumn<>("New Stock");

        stockColumn.setCellValueFactory(cell ->
            new SimpleIntegerProperty(
                cell.getValue().getStockAfter()
            ).asObject()
        );
        stockColumn.setPrefWidth(60);

        TableColumn<StockMovement, String> clerkColumn =
                new TableColumn<>("Performed By");

        clerkColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getPerformedBy()
            )
        );
        
        TableColumn<StockMovement, String> reasonColumn = new TableColumn<>("Reason");
        reasonColumn.setCellValueFactory(cell -> 
        	new SimpleStringProperty(
        			cell.getValue().getReason()
        	)
        );
        
        TableColumn<StockMovement, String> timestampColumn = new TableColumn<>("Timestamp");
        timestampColumn.setCellValueFactory(cell -> 
        	new SimpleStringProperty(
        			cell.getValue().getTimestampText()
        	)
        );

        table.getColumns().add(productIdColumn);
        table.getColumns().add(quantityColumn);
        table.getColumns().add(stockColumn);
        table.getColumns().add(clerkColumn);
        table.getColumns().add(reasonColumn);
        table.getColumns().add(timestampColumn);
        
        // Uses the stock movement records from InventoryService, newest first
        table.setItems(FXCollections.observableArrayList(
        		inventoryService.getRecentMovements(inventoryService.getMovements().size())));

        table.setPrefHeight(300);
        
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        return table;

    } // end of createMovementTable


    /*
     * createClerkTable()
     * - Displays the registered Clerks
     */
    private TableView<Clerk> createClerkTable() {

        TableView<Clerk> table = new TableView<>();

        TableColumn<Clerk, String> nameColumn =
                new TableColumn<>("Clerk Name");

        nameColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getDisplayName()
            )
        );

        TableColumn<Clerk, String> usernameColumn =
                new TableColumn<>("Username");

        usernameColumn.setCellValueFactory(cell ->
            new SimpleStringProperty(
                cell.getValue().getUsername()
            )
        );

        TableColumn<Clerk, Integer> salesMadeColumn =
                new TableColumn<>("Sales Made");

        salesMadeColumn.setCellValueFactory(cell ->
            new SimpleIntegerProperty(
                AgriSystem.getInstance().getTransactionService()
                    .countTransactions(cell.getValue().getUsername(),
                        false, DateRange.ANY_TIME)
            ).asObject()
        );

        table.getColumns().add(nameColumn);
        table.getColumns().add(usernameColumn);
        table.getColumns().add(salesMadeColumn);

        table.setItems(clerks);
        table.setPrefHeight(300);
        
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        return table;

    } // end of createClerkTable
    
    /*
     * createSalesHistoryTable()
     * - Date and Time, Clerk, Quantity of Items, Total
     * - items are set by createClerkMonitoringTab
     */
    private TableView<SalesTransaction> createSalesHistoryTable() {
    	
    	TableView<SalesTransaction> table = new TableView<>();
    	
    	TableColumn<SalesTransaction, String> dateColumn = new TableColumn<>("Date and Time");
    	dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTimestampSecondsText()));
    	dateColumn.setPrefWidth(150);
    	
    	TableColumn<SalesTransaction, String> clerkColumn = new TableColumn<>("Clerk");
    	clerkColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClerkName()));
    	clerkColumn.setPrefWidth(120);
    	
    	TableColumn<SalesTransaction, Integer> quantityColumn = new TableColumn<>("Quantity of Items");
    	quantityColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getTotalQuantity()).asObject());
    	quantityColumn.setPrefWidth(130);
    	
    	TableColumn<SalesTransaction, String> totalColumn = new TableColumn<>("Total");
    	totalColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatPeso(cell.getValue().getTotalAmount())));
    	totalColumn.setPrefWidth(100);
    	
    	table.getColumns().add(dateColumn);
    	table.getColumns().add(clerkColumn);
    	table.getColumns().add(quantityColumn);
    	table.getColumns().add(totalColumn);
    	table.setPrefHeight(300);
    	
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    	
    	return table;
    	
    } // end of createSalesHistoryTable
    
    /*
     * createLowStockTable()
     * - Product Name, Current Stock, Low Stock Threshold of active products at or below their threshold
     */
    private TableView<InventoryItem> createLowStockTable() {
    	
    	TableView<InventoryItem> table = new TableView<>();
    	table.setRowFactory(Theme.stockRowFactory());
    	
    	TableColumn<InventoryItem, String> nameColumn = new TableColumn<>("Product Name");
    	nameColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getProduct().getName()));
    	nameColumn.setPrefWidth(200);
    	
    	TableColumn<InventoryItem, Integer> stockColumn = new TableColumn<>("Current Stock");
    	stockColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getCurrentStock()).asObject());
    	stockColumn.setPrefWidth(120);
    	
    	TableColumn<InventoryItem, Integer> thresholdColumn = new TableColumn<>("Low Stock Threshold");
    	thresholdColumn.setCellValueFactory(cell -> new SimpleIntegerProperty(cell.getValue().getLowStockThreshold()).asObject());
    	thresholdColumn.setPrefWidth(150);
    	
    	table.getColumns().add(nameColumn);
    	table.getColumns().add(stockColumn);
    	table.getColumns().add(thresholdColumn);
    	table.setItems(FXCollections.observableArrayList(inventory.getLowStockItems()));
    	
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    	
    	return table;
    	
    } // end of createLowStockTable


    /*
     * updateStatistics()
     * - Updates the inventory and sales statistics shown on the dashboard
     */
    public void updateStatistics() {

        activeProductsValue.setText(
            String.valueOf(inventory.getActiveProductCount())
        );

        // running low only (0 stock is counted by Out of Stock), so a product is never counted twice
        lowStockValue.setText(
            String.valueOf(inventory.getRunningLowCount())
        );

        outOfStockValue.setText(
            String.valueOf(inventory.getOutOfStockCount())
        );
        
        TransactionService transactions = AgriSystem.getInstance().getTransactionService();

        totalSalesValue.setText(formatPeso(transactions.totalSales(null, DateRange.ANY_TIME)));
        completedTransactionsValue.setText(String.valueOf(
            transactions.countTransactions(null, false, DateRange.ANY_TIME)));
        voidedTransactionsValue.setText(String.valueOf(
            transactions.countTransactions(null, true, DateRange.ANY_TIME)));
        
        asOfLabel.setText("As of " + LocalDateTime.now().format(DateFormats.AS_OF));

    } // end of updateStatistics
    
    /*
     * openClerkDashboard(Clerk)
     * - default action of the Open Dashboard button
     * - pop up of the clerk's summary and transactions (view only, the manager cannot make sales)
     */
    private void openClerkDashboard(Clerk clerk) {
    	
    	TransactionService transactions = AgriSystem.getInstance().getTransactionService();
    	String username = clerk.getUsername();
    	
    	Label salesToday = new Label(formatPeso(transactions.totalSales(username, DateRange.TODAY)));
    	Label transactionsToday = new Label(String.valueOf(transactions.countTransactions(username, false, DateRange.TODAY)));
    	Label voidedToday = new Label(String.valueOf(transactions.countTransactions(username, true, DateRange.TODAY)));
    	Label itemsSold = new Label(String.valueOf(transactions.totalItemsSold(username, DateRange.ANY_TIME)));
    	
    	VBox summary = createStatColumn(
    			createStatCard("Today's Sales", salesToday),
    			createStatCard("Transactions Today", transactionsToday),
    			createStatCard("Voided Today", voidedToday),
    			createStatCard("Total Items Sold", itemsSold));
    	
    	// let the summary cards stretch to the full width of the pop-up, like in the Figma design
    	for (Node card : summary.getChildren()) {
    		((Region) card).setMaxWidth(Double.MAX_VALUE);
    	}
    	
    	// every transaction of the clerk (valid and voided), newest first
    	List<SalesTransaction> clerkTransactions = new ArrayList<>();
    	for (SalesTransaction t : transactions.getAll()) {
    		if (t.getClerkUsername().equals(username)) {
    			clerkTransactions.add(t);
    		}
    	}
    	
    	TableView<SalesTransaction> table = new TableView<>(FXCollections.observableArrayList(clerkTransactions));
    	
    	TableColumn<SalesTransaction, String> idColumn = new TableColumn<>("Transaction ID");
    	idColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getDisplayId()));
    	TableColumn<SalesTransaction, String> dateColumn = new TableColumn<>("Date and Time");
    	dateColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getTimestampText()));
    	TableColumn<SalesTransaction, String> totalColumn = new TableColumn<>("Total");
    	totalColumn.setCellValueFactory(cell -> new SimpleStringProperty(formatPeso(cell.getValue().getTotalAmount())));
    	TableColumn<SalesTransaction, String> statusColumn = new TableColumn<>("Status");
    	statusColumn.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isVoided() ? "VOIDED" : "VALID"));
    	
    	// VOIDED is shown in red, like in the Figma design
    	statusColumn.setCellFactory(column -> new TableCell<SalesTransaction, String>() {
    		@Override
    		protected void updateItem(String status, boolean empty) {
    			super.updateItem(status, empty);
    			setText(empty ? null : status);
    			setStyle(!empty && "VOIDED".equals(status) ? "-fx-text-fill: #c62828; -fx-font-weight: bold;" : "");
    		}
    	});
    	
    	table.getColumns().add(idColumn);
    	table.getColumns().add(dateColumn);
    	table.getColumns().add(totalColumn);
    	table.getColumns().add(statusColumn);
    	table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    	table.setPlaceholder(new Label("No transactions yet."));
    	
    	// the table sits on a white card
    	VBox tableCard = new VBox(table);
    	tableCard.getStyleClass().add("white-card");
    	tableCard.setPadding(new Insets(12));
    	VBox.setVgrow(table, Priority.ALWAYS);
    	
    	Label title = new Label(clerk.getDisplayName());
    	title.getStyleClass().add("form-title");
    	Label subtitle = new Label(username + "  -  viewing as Manager");
    	subtitle.getStyleClass().add("muted-text");
    	
    	Stage popup = new Stage();
    	Button closeButton = new Button("Close");
    	closeButton.setOnAction(e -> popup.close());
    	HBox buttons = new HBox(closeButton);
    	buttons.setAlignment(Pos.CENTER);
    	
    	VBox root = new VBox(12,
    			title, subtitle,
    			Theme.sectionTitle("SUMMARY"), summary,
    			Theme.sectionTitle("TRANSACTIONS"), tableCard,
    			buttons);
    	root.getStyleClass().add("form-dialog");
    	root.setPadding(new Insets(24));
    	VBox.setVgrow(tableCard, Priority.ALWAYS);
    	
    	Scene popupScene = new Scene(root, 640, 760);
    	Theme.apply(popupScene);
    	
    	popup.initOwner(getScene().getWindow());
    	popup.initModality(Modality.APPLICATION_MODAL);
    	popup.setTitle("Clerk Dashboard - " + clerk.getDisplayName());
    	popup.setScene(popupScene);
    	popup.showAndWait();
    	
    } // end of openClerkDashboard


    /*
     * setClerkDashboardAction(Consumer<Clerk>)
     * - Sets the action for opening a selected Clerk's dashboard
     */
    public void setClerkDashboardAction(Consumer<Clerk> action) {

        this.clerkDashboardAction = action;

    } // end of setClerkDashboardAction


    /*
     * setInventoryAction(Runnable)
     * - Sets the action for opening the Inventory Management screen
     */
    public void setInventoryAction(Runnable action) {

        this.inventoryAction = action;

    } // end of setInventoryAction

} // end of ManagerDashboard
