package ui;

import java.util.ArrayList;
import java.util.List;

import app.AgriSystem;
import dashboard.Dashboard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import model.DateRange;
import model.Role;
import model.SalesTransaction;
import model.User;
import service.TransactionService;

/*
 * TransactionScreen class
 * 	- extends BorderPane
 * 	- can be opened by all users
 * 	- Manager: sees every clerk's transactions, can search by clerk username or name
 * 	- Clerk: only sees their own transactions, no search bar and no "Processed By" column
 * 	- View Details opens a pop up of the transaction, Void Transaction voids the selected transaction
 */
public class TransactionScreen extends BorderPane {
	
	// ATTRIBUTES
	private final TransactionService service;
	private final User user;
	private final boolean isManager;
	
	private final TableView<SalesTransaction> table = new TableView<>();
	private final TextField searchField = new TextField();
	private final ComboBox<DateRange> dateBox = new ComboBox<>();
	private final ComboBox<String> statusBox = new ComboBox<>();
	
	private static final String ALL = "All";
	private static final String VALID = "Valid";
	private static final String VOIDED = "Voided";
	
	// METHODS
	/*
	 * TransactionScreen(AgriSystem)
	 * 	- constructor method
	 */
	public TransactionScreen(AgriSystem system) {
		
		service = system.getTransactionService();
		user = system.getCurrentUser();
		isManager = user.getRole() == Role.MANAGER;
		
		setupTable();
		setTop(createTopPart());
		setCenter(table);
		setBottom(createBottomPart());
		refresh();
		
	} // end of constructor method
	
	/*
	 * createTopPart()
	 * 	- search bar (manager only), date range dropdown, status dropdown
	 */
	private HBox createTopPart() {
		
		HBox top = new HBox(10);
		top.setPadding(new Insets(10));
		
		if (isManager) {
			searchField.setPromptText("Search by Clerk username or name...");
			searchField.setPrefWidth(300);
			searchField.setOnKeyReleased(e -> refresh());
			top.getChildren().add(searchField);
		} // end of if statement
		
		dateBox.getItems().addAll(DateRange.values());
		dateBox.setValue(DateRange.ANY_TIME);
		dateBox.setOnAction(e -> refresh());
		
		statusBox.getItems().addAll(ALL, VALID, VOIDED);
		statusBox.setValue(VALID);
		statusBox.setOnAction(e -> refresh());
		
		top.getChildren().addAll(dateBox, statusBox);
		return top;
		
	} // end of createTopPart
	
	/*
	 * createBottomPart()
	 * 	- View Details and Void Transaction buttons
	 */
	private HBox createBottomPart() {
		
		Button detailsButton = new Button("View Details");
		Button voidButton = new Button("Void Transaction");
		
		detailsButton.setOnAction(e -> onViewDetails());
		voidButton.setOnAction(e -> onVoid());
		
		HBox bottom = new HBox(10, detailsButton, voidButton);
		bottom.setPadding(new Insets(10));
		return bottom;
		
	} // end of createBottomPart
	
	/*
	 * setupTable()
	 * 	- Transaction ID, Date and Time, (Processed By), Total Cost, Cash Received, Change, Status
	 */
	private void setupTable() {
		
		table.getColumns().add(makeColumn("Transaction ID", 110, t -> t.getDisplayId()));
		table.getColumns().add(makeColumn("Date and Time", 130, t -> t.getTimestampText()));
		if (isManager) {
			table.getColumns().add(makeColumn("Processed By", 130, t -> t.getClerkName()));
		} // end of if statement
		table.getColumns().add(makeColumn("Total Cost", 100, t -> Dashboard.formatPeso(t.getTotalAmount())));
		table.getColumns().add(makeColumn("Cash Received", 110, t -> Dashboard.formatPeso(t.getCashReceived())));
		table.getColumns().add(makeColumn("Change", 100, t -> Dashboard.formatPeso(t.getChangeGiven())));
		table.getColumns().add(makeColumn("Status", 80, t -> t.isVoided() ? "VOIDED" : "VALID"));
		
	} // end of setupTable
	
	/*
	 * makeColumn(String, int, java.util.function.Function)
	 * 	- helper for making a text column
	 */
	private TableColumn<SalesTransaction, String> makeColumn(String title, int width,
			java.util.function.Function<SalesTransaction, String> getter) {
		
		TableColumn<SalesTransaction, String> column = new TableColumn<>(title);
		column.setPrefWidth(width);
		column.setCellValueFactory(cell -> new SimpleStringProperty(getter.apply(cell.getValue())));
		return column;
		
	} // end of makeColumn
	
	/*
	 * refresh()
	 * 	- searches again with the current filters (newest first)
	 * 	- a clerk's list is narrowed down to their own transactions
	 */
	private void refresh() {
		
		Boolean voided = null; // null means valid and voided
		if (VALID.equals(statusBox.getValue())) {
			voided = false;
		} else if (VOIDED.equals(statusBox.getValue())) {
			voided = true;
		} // end of if else statement
		
		String clerkText = isManager ? searchField.getText() : null;
		List<SalesTransaction> results = service.search(clerkText, dateBox.getValue(), voided);
		
		if (!isManager) {
			List<SalesTransaction> own = new ArrayList<>();
			for (SalesTransaction t : results) {
				if (t.getClerkUsername().equals(user.getUsername())) {
					own.add(t);
				} // end of if statement
			} // end of for loop
			results = own;
		} // end of if statement
		
		table.setItems(FXCollections.observableArrayList(results));
		table.refresh();
		
	} // end of refresh
	
	/*
	 * onViewDetails()
	 * 	- opens the details pop up of the selected transaction
	 */
	private void onViewDetails() {
		
		SalesTransaction transaction = getSelectedTransaction();
		if (transaction == null) {
			return;
		} // end of if statement
		new TransactionDetailsWindow(transaction).showAndWait();
		
	} // end of onViewDetails
	
	/*
	 * onVoid()
	 * 	- voids the selected transaction (puts the stock back)
	 * 	- possible errors: already voided, clerk voiding another clerk's transaction
	 */
	private void onVoid() {
		
		SalesTransaction transaction = getSelectedTransaction();
		if (transaction == null) {
			return;
		} // end of if statement
		
		// ERROR: already voided
		if (transaction.isVoided()) {
			Dashboard.showError(transaction.getDisplayId() + " is already voided.");
			return;
		} // end of if statement
		
		// ERROR: clerk can only void their own
		if (!isManager && !transaction.getClerkUsername().equals(user.getUsername())) {
			Dashboard.showError("A Clerk can only void their own transactions.");
			return;
		} // end of if statement
		
		if (!Dashboard.confirm("Void " + transaction.getDisplayId() + "? The stock will be put back.")) {
			return;
		} // end of if statement
		
		try {
			service.voidTransaction(user, transaction.getTransactionId());
		} catch (RuntimeException e) {
			Dashboard.showError(e.getMessage());
		} // end of try catch
		refresh();
		
	} // end of onVoid
	
	/*
	 * getSelectedTransaction()
	 * 	- returns either the selected row or null
	 */
	private SalesTransaction getSelectedTransaction() {
		
		SalesTransaction transaction = table.getSelectionModel().getSelectedItem();
		if (transaction == null) {
			Dashboard.showError("Select a transaction in the table first.");
		} // end of if statement
		return transaction;
		
	} // end of getSelectedTransaction
	
} // end of TransactionScreen class
