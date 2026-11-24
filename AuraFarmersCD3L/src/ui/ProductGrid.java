package ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import app.AgriSystem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import model.Category;
import model.InventoryItem;
import model.StatusFilter;
import service.ProductService;

/*
 * ProductGrid class
 * 	- the product catalog shown as photo cards, like the reference design:
 * 		title and "N items found" on top, category chips (All + the 6 categories), a search box,
 * 		and the cards below, wrapping to as many columns as fit
 * 	- reusable: Product Management uses it for the Manager, the Clerk Dashboard uses it for the catalog
 * 	- the products come from ProductService.search, so search, category, and status work like before
 * 	- manager = true adds the Active/Inactive/All dropdown; manager = false shows only active products
 * 	- scrollable = true puts the cards in their own scroll area (for a screen that fills the window),
 * 		false lets the cards just grow (for a page that already scrolls)
 */

public class ProductGrid extends VBox {
	
	// ATTRIBUTES
	private final boolean manager;
	private final String actionText;
	private final Consumer<InventoryItem> onAction;
	
	private final FlowPane cards = new FlowPane(16, 16);
	private final Label countLabel = new Label();
	private final TextField searchField = new TextField();
	private final ComboBox<StatusFilter> statusBox = new ComboBox<>();
	private final ToggleGroup chipGroup = new ToggleGroup();
	
	private final Map<Integer, ProductCard> cardById = new HashMap<>(); // product id -> its card
	private Category selectedCategory = null; // null means all categories
	private InventoryItem selected = null;    // the chosen card (Product Management)
	
	private Consumer<InventoryItem> selectionListener;
	private Consumer<InventoryItem> doubleClickListener;
	
	// METHODS
	/*
	 * ProductGrid(String, boolean, boolean, String, Consumer<InventoryItem>)
	 * 	- constructor method
	 * 	- title: heading on top (e.g. "All Products"), actionText: text of the button on each card
	 * 		(null = no button), onAction: what the card button does
	 */
	public ProductGrid(String title, boolean manager, boolean scrollable,
			String actionText, Consumer<InventoryItem> onAction) {
		
		this.manager = manager;
		this.actionText = actionText;
		this.onAction = onAction;
		
		setSpacing(12);
		
		// title on the left, "N items found" on the right
		Label titleLabel = new Label(title);
		titleLabel.getStyleClass().add("product-grid-title");
		countLabel.getStyleClass().add("muted-text");
		Region space = new Region();
		HBox.setHgrow(space, Priority.ALWAYS);
		HBox header = new HBox(10, titleLabel, space, countLabel);
		header.setAlignment(Pos.CENTER_LEFT);
		
		// category chips: All + one per category (wraps onto a second line if the window is narrow)
		FlowPane chips = new FlowPane(8, 8);
		chips.getChildren().add(createChip("All", null));
		for (Category category : Category.values()) {
			chips.getChildren().add(createChip(category.toString(), category));
		} // end of for loop
		
		// one chip is always selected (clicking the selected chip does nothing)
		chipGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
			if (newToggle == null) {
				oldToggle.setSelected(true);
			} // end of if statement
		});
		
		// search box (and the status dropdown for the Manager)
		searchField.setPromptText("Search by name, ID, or description...");
		searchField.setPrefWidth(320);
		searchField.textProperty().addListener((obs, oldText, newText) -> refresh());
		
		HBox searchRow = new HBox(10, searchField);
		if (manager) {
			statusBox.getItems().addAll(StatusFilter.values());
			statusBox.setValue(StatusFilter.ACTIVE);
			statusBox.setOnAction(e -> refresh());
			searchRow.getChildren().add(statusBox);
		} // end of if statement
		
		cards.setPadding(new Insets(4));
		getChildren().addAll(header, chips, searchRow);
		
		if (scrollable) {
			ScrollPane scrollPane = new ScrollPane(cards);
			scrollPane.setFitToWidth(true);
			VBox.setVgrow(scrollPane, Priority.ALWAYS);
			getChildren().add(scrollPane);
		} else {
			getChildren().add(cards);
		} // end of if else statement
		
		refresh();
		
	} // end of constructor method
	
	/*
	 * createChip(String, Category)
	 * 	- one category button; clicking it filters the cards (a null category means All)
	 */
	private ToggleButton createChip(String text, Category category) {
		
		ToggleButton chip = new ToggleButton(text);
		chip.getStyleClass().add("chip");
		chip.setToggleGroup(chipGroup);
		chip.setSelected(category == null); // "All" starts selected
		chip.setOnAction(e -> {
			selectedCategory = category;
			refresh();
		});
		return chip;
		
	} // end of createChip
	
	/*
	 * refresh()
	 * 	- searches again (text, category, status) and redraws the cards
	 * 	- call it after a product is added, edited, activated, or deactivated
	 */
	public void refresh() {
		
		ProductService service = AgriSystem.getInstance().getProductService();
		StatusFilter status = manager ? statusBox.getValue() : StatusFilter.ACTIVE;
		List<InventoryItem> items = service.search(searchField.getText(), selectedCategory, status);
		
		cards.getChildren().clear();
		cardById.clear();
		
		for (InventoryItem item : items) {
			ProductCard card = new ProductCard(item, actionText, onAction);
			card.setOnMouseClicked(e -> {
				select(item);
				if (e.getClickCount() == 2 && doubleClickListener != null) {
					doubleClickListener.accept(item);
				} // end of if statement
			});
			cards.getChildren().add(card);
			cardById.put(item.getProductId(), card);
		} // end of for loop
		
		// keep the selection if that product is still shown, otherwise clear it
		if (selected != null && cardById.containsKey(selected.getProductId())) {
			cardById.get(selected.getProductId()).setSelected(true);
		} else if (selected != null) {
			selected = null;
			if (selectionListener != null) {
				selectionListener.accept(null);
			} // end of if statement
		} // end of if else statement
		
		countLabel.setText(items.size() == 1 ? "1 item found" : items.size() + " items found");
		
	} // end of refresh
	
	/*
	 * select(InventoryItem)
	 * 	- makes a card the chosen one (green border) and tells the listener
	 */
	private void select(InventoryItem item) {
		
		if (selected != null && cardById.containsKey(selected.getProductId())) {
			cardById.get(selected.getProductId()).setSelected(false);
		} // end of if statement
		
		selected = item;
		cardById.get(item.getProductId()).setSelected(true);
		
		if (selectionListener != null) {
			selectionListener.accept(item);
		} // end of if statement
		
	} // end of select
	
	/*
	 * getSelectedItem()
	 * 	- the chosen product, or null if no card is chosen
	 */
	public InventoryItem getSelectedItem() {
		return selected;
	} // end of getSelectedItem
	
	/*
	 * setOnSelectionChanged(Consumer<InventoryItem>)
	 * 	- called when a card is chosen (or with null when the chosen product is no longer shown)
	 */
	public void setOnSelectionChanged(Consumer<InventoryItem> listener) {
		this.selectionListener = listener;
	} // end of setOnSelectionChanged
	
	/*
	 * setOnItemDoubleClicked(Consumer<InventoryItem>)
	 * 	- called when a card is double-clicked
	 */
	public void setOnItemDoubleClicked(Consumer<InventoryItem> listener) {
		this.doubleClickListener = listener;
	} // end of setOnItemDoubleClicked
	
} // end of ProductGrid class
