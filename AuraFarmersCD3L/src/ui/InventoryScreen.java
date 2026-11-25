package ui;

import app.AgriSystem;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.SelectionMode;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import model.Category;
import model.InventoryItem;
import model.User;
import service.InventoryService;


/*
 * InventoryScreen
 * 	- extends BorderPane
 * 	- can be viewed by all users but only managers can use buttons
 */
public class InventoryScreen extends BorderPane {
	
	// ATTRIBUTES
    private final InventoryService service;
    private final User user;

    private final TableView<InventoryItem> table = new TableView<>();
    private final FilteredList<InventoryItem> filteredItems;
    private final TextField searchField = new TextField();
    private final ComboBox<String> categoryBox = new ComboBox<>();
    private final CheckBox lowOnlyBox = new CheckBox("Low Stock Only");
    private final Label banner = new Label();

    private static final String ALL_CATEGORIES = "All Categories";
    
    // METHODS
    /*
     * InventoryScreen(AgriSystem)
     * 	- constructor method
     */
    public InventoryScreen(AgriSystem system) {
    	
        service = system.getInventoryService();
        user = system.getCurrentUser();
        filteredItems = new FilteredList<>(service.getInventory().getItems());

        setupTable();
        setTop(createTopPart());
        setCenter(table);
        setPadding(new Insets(10));
        setBottom(createBottomPart());
        refresh();
        
    } // end of InventoryScreen

    /*
     * createTopPart()
     */
    private VBox createTopPart() {
    	
        banner.setMaxWidth(Double.MAX_VALUE);

        searchField.setPromptText("Search by Product ID or name...");
        searchField.setPrefWidth(240);

        categoryBox.getItems().add(ALL_CATEGORIES);
        for (Category c : Category.values()) {
            categoryBox.getItems().add(c.toString());
        } // end of for loop
        categoryBox.setValue(ALL_CATEGORIES);

        // reapply the filter whenever one of the three controls changes.
        searchField.setOnKeyReleased(e -> applyFilter());
        categoryBox.setOnAction(e -> applyFilter());
        lowOnlyBox.setOnAction(e -> applyFilter());

        HBox controls = new HBox(10, searchField, categoryBox, lowOnlyBox);
        VBox top = new VBox(8, banner, controls);
        top.setPadding(new Insets(10));
        return top;
        
    } // end of createTopPart
    
    /*
     * createBottomPart()
     */
    private HBox createBottomPart() {
    	
        HBox bottom = new HBox(10);
        bottom.setPadding(new Insets(10));

        if (user.canModifyInventory()) {
            Button restockButton = new Button("Restock Item");
            Button adjustButton = new Button("Stock Adjustment");
            Button thresholdButton = new Button("Set Low-Stock Threshold");

            restockButton.setOnAction(e -> onRestock());
            adjustButton.setOnAction(e -> onAdjust());
            thresholdButton.setOnAction(e -> onThreshold());

            bottom.getChildren().addAll(restockButton, adjustButton, thresholdButton);
        } // end of if statement (clerk: read-only, no buttons)
        
        return bottom;
        
    } // end of createBottomPart
    
    /*
     * setupTable()
     */
    private void setupTable() {
    	
    	// lets the user select multiple products via SHIFT + CLICK or CTRL + CLICK
    	table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        // product id is shown as AG001 (not just 1)
        TableColumn<InventoryItem, Object> idColumn = new TableColumn<>("Product ID");
        idColumn.setPrefWidth(90);
        idColumn.setCellValueFactory(cell -> new SimpleObjectProperty<>(cell.getValue().getProduct().getDisplayId()));
        table.getColumns().add(idColumn);
        table.getColumns().add(makeColumn("Product Name", 200, "name"));
        table.getColumns().add(makeColumn("Category", 190, "category"));
        table.getColumns().add(makeColumn("Current Stock", 100, "currentStock"));
        table.getColumns().add(makeColumn("Low-Stock Threshold", 130, "lowStockThreshold"));
        table.getColumns().add(makeColumn("Stock Status", 100, "status"));

        // SortedList lets user click a column header to sort
        SortedList<InventoryItem> sortedItems = new SortedList<>(filteredItems);
        sortedItems.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedItems);
        table.setRowFactory(Theme.stockRowFactory()); // red = out of stock, amber = low stock
        table.setPlaceholder(new javafx.scene.control.Label("No products match your search."));
        
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        
    } // end of setupTable
    
    /*
     * makeColumn(String, int, String)	
     */
    private TableColumn<InventoryItem, Object> makeColumn(String title, int width, String getterName) {
        
    	TableColumn<InventoryItem, Object> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(new PropertyValueFactory<>(getterName));
        return column;
        
    } // end of makeColumn

    /*
     * applyFilter()
     */
    private void applyFilter() {
        String text = searchField.getText().trim().toLowerCase();
        String category = categoryBox.getValue();
        boolean lowOnly = lowOnlyBox.isSelected();

        filteredItems.setPredicate(item -> {
            boolean matchesText = text.isEmpty()
                    || item.getProduct().getName().toLowerCase().contains(text)
                    || item.getProduct().getDisplayId().toLowerCase().contains(text);
            boolean matchesCategory = category == null
                    || category.equals(ALL_CATEGORIES)
                    || item.getProduct().getCategory().toString().equals(category);
            boolean matchesLow = !lowOnly || item.isLowStock();
            return matchesText && matchesCategory && matchesLow;
        }); // end of .setPredicate
    } // end of applyFilter

    /*
     * refresh()
     */
    private void refresh() {
        applyFilter();
        table.refresh();

        int lowCount = service.getInventory().getLowStockCount();
        if (lowCount > 0) {
            banner.setText(lowCount + " item(s) at or below the low-stock threshold - consider restocking.");
        } else {
            banner.setText("All stock levels are good.");
        } // end of if else statement
        
    } // end of refresh

    /*
     * onRestock()
     */
    private void onRestock() {
        List<InventoryItem> items = getSelectedItems();
        if (items.isEmpty()) {
            return;
        }
        StockDialogs.showRestock(service, user, items);
        refresh();
        table.getSelectionModel().clearSelection();
    } // end of onRestock
    
    /*
     * onAdjust()
     */
    private void onAdjust() {
        List<InventoryItem> items = getSelectedItems();
        if (items.isEmpty()) {
            return;
        }
        StockDialogs.showAdjust(service, user, items);
        refresh();
        table.getSelectionModel().clearSelection();
    } // end of onAdjust
    
    /*
     * onThreshold()
     */
    private void onThreshold() {
        List<InventoryItem> items = getSelectedItems();
        if (items.isEmpty()) {
            return;
        }
        StockDialogs.showThreshold(service, user, items);
        refresh();
        table.getSelectionModel().clearSelection();
    } // end of onThreshold

    /*
     * getSelectedItem()
     * 	- returns either the selected row or null
     */
    private List<InventoryItem> getSelectedItems() {
        List<InventoryItem> items = table.getSelectionModel().getSelectedItems();

        if (items.isEmpty()) {
            showError("Select at least one product in the table first.");
        }

        return new ArrayList<>(items);
    } // end of getSelectedItems
    
    
    /*
     * showError(String)
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    } // end of showError
} // end of InventoryScreen class
