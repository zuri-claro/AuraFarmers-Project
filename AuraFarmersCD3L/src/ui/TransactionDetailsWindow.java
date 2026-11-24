package ui;

import dashboard.Dashboard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.DateFormats;
import model.PurchasedProduct;
import model.SalesTransaction;

/*
 * TransactionDetailsWindow class
 * 	- extends Stage (its own pop up window), styled like the "View Details" dialog
 * 	- opened by "View Details" in the Transactions screen
 * 	- top card: transaction id, date and time, processed by, quantities, total cost, cash received, change, status (for a voided transaction it also shows who voided it and when)
 * 	- bottom card: table of the products bought
 */
public class TransactionDetailsWindow extends Stage {
	
	// METHODS
	/*
	 * TransactionDetailsWindow(SalesTransaction)
	 * 	- constructor method
	 */
	public TransactionDetailsWindow(SalesTransaction transaction) {
		
		// two white cards on a grey background, like the Figma dialog
		VBox summaryCard = new VBox(createSummary(transaction));
		summaryCard.getStyleClass().add("white-card");
		summaryCard.setPadding(new Insets(20, 24, 20, 24));
		
		VBox itemsCard = new VBox(createItemTable(transaction));
		itemsCard.getStyleClass().add("white-card");
		itemsCard.setPadding(new Insets(12));
		VBox.setVgrow(itemsCard, Priority.ALWAYS);
		
		Button closeButton = new Button("Close");
		closeButton.setOnAction(e -> close());
		HBox buttons = new HBox(closeButton);
		buttons.setAlignment(Pos.CENTER);
		
		VBox root = new VBox(15, summaryCard, itemsCard, buttons);
		root.getStyleClass().add("form-dialog");
		root.setPadding(new Insets(20));
		
		Scene scene = new Scene(root, 580, 640);
		Theme.apply(scene);
		
		initModality(Modality.APPLICATION_MODAL);
		setTitle("Transaction Details");
		setScene(scene);
		
	} // end of constructor method
	
	/*
	 * createSummary(SalesTransaction)
	 * 	- label on the left (bold), value on the right
	 */
	private GridPane createSummary(SalesTransaction t) {
		
		GridPane grid = new GridPane();
		grid.setHgap(24);
		grid.setVgap(6);
		
		int row = 0;
		addRow(grid, row++, "Transaction ID:", t.getDisplayId());
		addRow(grid, row++, "Date and Time:", t.getTimestampSecondsText());
		addRow(grid, row++, "Processed By:", t.getClerkName());
		addRow(grid, row++, "Customer Name:", t.getCustomerName());
		addRow(grid, row++, "Quantities:", String.valueOf(t.getTotalQuantity()));
		addRow(grid, row++, "Total Cost:", Dashboard.formatPeso(t.getTotalAmount()));
		addRow(grid, row++, "Cash Received:", Dashboard.formatPeso(t.getCashReceived()));
		addRow(grid, row++, "Change:", Dashboard.formatPeso(t.getChangeGiven()));
		addRow(grid, row++, "Status:", t.isVoided() ? "VOIDED" : "VALID");
		
		// only a voided transaction has these
		if (t.isVoided()) {
			addRow(grid, row++, "Voided By:", t.getVoidedBy());
			if (t.getVoidedAt() != null) {
				addRow(grid, row++, "Voided At:", t.getVoidedAt().format(DateFormats.DATE_TIME_SECONDS));
			} // end of if statement
		} // end of if statement
		
		return grid;
		
	} // end of createSummary
	
	/*
	 * addRow(GridPane, int, String, String)
	 * 	- helper: one bold label and its value
	 */
	private void addRow(GridPane grid, int row, String label, String value) {
		
		Label labelNode = new Label(label);
		labelNode.getStyleClass().add("detail-label");
		grid.addRow(row, labelNode, new Label(value));
		
	} // end of addRow
	
	/*
	 * createItemTable(SalesTransaction)
	 * 	- Product Name, Individual Price, Quantity, Total
	 */
	private TableView<PurchasedProduct> createItemTable(SalesTransaction t) {
		
		TableView<PurchasedProduct> table = new TableView<>(FXCollections.observableArrayList(t.getItems()));
		
		TableColumn<PurchasedProduct, String> nameColumn = new TableColumn<>("Product Name");
		nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getProductName()));
		nameColumn.setPrefWidth(190);
		
		TableColumn<PurchasedProduct, String> priceColumn = new TableColumn<>("Individual Price");
		priceColumn.setCellValueFactory(c -> new SimpleStringProperty(Dashboard.formatPeso(c.getValue().getUnitPrice())));
		priceColumn.setPrefWidth(130);
		
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
		
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
		
		return table;
		
	} // end of createItemTable
	
} // end of TransactionDetailsWindow class