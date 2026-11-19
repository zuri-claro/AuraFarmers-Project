package ui;

import dashboard.Dashboard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.PurchasedProduct;
import model.SalesTransaction;

/*
 * TransactionDetailsWindow class
 * 	- extends Stage (its own pop up window)
 * 	- opened by "View Details" in the Transactions screen
 * 	- top part: transaction id, date and time, quantities, total cost, cash received, change
 * 	- bottom part: table of the products bought
 */
public class TransactionDetailsWindow extends Stage {
	
	// METHODS
	/*
	 * TransactionDetailsWindow(SalesTransaction)
	 * 	- constructor method
	 */
	public TransactionDetailsWindow(SalesTransaction transaction) {
		
		VBox root = new VBox(15, createSummary(transaction), createItemTable(transaction));
		root.setPadding(new Insets(20));
		
		Button closeButton = new Button("Close");
		closeButton.setOnAction(e -> close());
		root.getChildren().add(closeButton);
		
		initModality(Modality.APPLICATION_MODAL);
		setTitle("Transaction Details");
		setScene(new Scene(root, 520, 520));
		
	} // end of constructor method
	
	/*
	 * createSummary(SalesTransaction)
	 * 	- label on the left, value on the right
	 */
	private GridPane createSummary(SalesTransaction t) {
		
		GridPane grid = new GridPane();
		grid.setHgap(20);
		grid.setVgap(5);
		
		grid.addRow(0, new Label("Transaction ID:"), new Label(t.getDisplayId()));
		grid.addRow(1, new Label("Date and Time:"), new Label(t.getTimestampSecondsText()));
		grid.addRow(2, new Label("Processed By:"), new Label(t.getClerkName()));
		grid.addRow(3, new Label("Quantities:"), new Label(String.valueOf(t.getTotalQuantity())));
		grid.addRow(4, new Label("Total Cost:"), new Label(Dashboard.formatPeso(t.getTotalAmount())));
		grid.addRow(5, new Label("Cash Received:"), new Label(Dashboard.formatPeso(t.getCashReceived())));
		grid.addRow(6, new Label("Change:"), new Label(Dashboard.formatPeso(t.getChangeGiven())));
		grid.addRow(7, new Label("Status:"), new Label(t.isVoided() ? "VOIDED by " + t.getVoidedBy() : "VALID"));
		
		return grid;
		
	} // end of createSummary
	
	/*
	 * createItemTable(SalesTransaction)
	 * 	- Product Name, Individual Price, Quantity, Total
	 */
	private TableView<PurchasedProduct> createItemTable(SalesTransaction t) {
		
		TableView<PurchasedProduct> table = new TableView<>(FXCollections.observableArrayList(t.getItems()));
		
		TableColumn<PurchasedProduct, String> nameColumn = new TableColumn<>("Product Name");
		nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));
		nameColumn.setPrefWidth(170);
		
		TableColumn<PurchasedProduct, String> priceColumn = new TableColumn<>("Individual Price");
		priceColumn.setCellValueFactory(c -> new SimpleStringProperty(Dashboard.formatPeso(c.getValue().getUnitPrice())));
		priceColumn.setPrefWidth(110);
		
		TableColumn<PurchasedProduct, String> quantityColumn = new TableColumn<>("Quantity");
		quantityColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getQuantity())));
		quantityColumn.setPrefWidth(80);
		
		TableColumn<PurchasedProduct, String> totalColumn = new TableColumn<>("Total");
		totalColumn.setCellValueFactory(c -> new SimpleStringProperty(Dashboard.formatPeso(c.getValue().getTotal())));
		totalColumn.setPrefWidth(100);
		
		table.getColumns().add(nameColumn);
		table.getColumns().add(priceColumn);
		table.getColumns().add(quantityColumn);
		table.getColumns().add(totalColumn);
		VBox.setVgrow(table, Priority.ALWAYS);
		
		return table;
		
	} // end of createItemTable
	
} // end of TransactionDetailsWindow class
