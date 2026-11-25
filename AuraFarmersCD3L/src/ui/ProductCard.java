package ui;

import java.util.function.Consumer;

import dashboard.Dashboard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import model.InventoryItem;
import model.Product;

/*
 * ProductCard class
 * 	- one product shown as a card: photo on top with the stock badge in the top-right corner,
 * 		then the name, the category, and at the bottom the price (with unit) and an action button
 * 	- stock badge: green "20 in stock", amber "3 left" (low stock), red "Out of stock", grey "Inactive"
 * 	- used inside ProductGrid (Product Management and the Clerk Dashboard catalog)
 */

public class ProductCard extends VBox {
	
	// ATTRIBUTES
	public static final double WIDTH = 240;
	public static final double PHOTO_HEIGHT = 150;
	
	private final InventoryItem item;
	
	// METHODS
	/*
	 * ProductCard(InventoryItem, String, Consumer<InventoryItem>)
	 * 	- constructor method
	 * 	- actionText is the text of the button at the bottom right (null = no button),
	 * 		onAction is what happens when that button is clicked
	 */
	public ProductCard(InventoryItem item, String actionText, Consumer<InventoryItem> onAction) {
		
		this.item = item;
		Product product = item.getProduct();
		
		getStyleClass().add("product-card");
		setPrefWidth(WIDTH);
		setMinWidth(WIDTH);
		setMaxWidth(WIDTH);
		
		// photo with the stock badge in the top-right corner
		StackPane photo = createPhoto(product, WIDTH, PHOTO_HEIGHT);
		Label badge = createStockBadge(item);
		StackPane.setAlignment(badge, Pos.TOP_RIGHT);
		StackPane.setMargin(badge, new Insets(8));
		photo.getChildren().add(badge);
		
		// out of stock: fade the photo
		if (product.isActive() && item.getCurrentStock() <= 0) {
			photo.getChildren().get(0).setOpacity(0.45);
		} // end of if statement
		
		Label nameLabel = new Label(product.getName());
		nameLabel.getStyleClass().add("card-name");
		nameLabel.setMaxWidth(WIDTH - 28);
		
		Label categoryLabel = new Label(product.getCategory().toString());
		categoryLabel.getStyleClass().add("card-category");
		categoryLabel.setMaxWidth(WIDTH - 28);
		
		// short description (2 lines at most, so every card has the same height)
		Label descriptionLabel = new Label(product.getDescription() == null ? "" : product.getDescription());
		descriptionLabel.getStyleClass().add("card-description");
		descriptionLabel.setWrapText(true);
		descriptionLabel.setMaxWidth(WIDTH - 28);
		descriptionLabel.setMinHeight(36);
		descriptionLabel.setPrefHeight(36);
		descriptionLabel.setMaxHeight(36);
		
		// bottom line: price on the left, button on the right
		Label priceLabel = new Label(Dashboard.formatPeso(product.getPrice()) + " /" + product.getUnit());
		priceLabel.getStyleClass().add("card-price");
		
		Region space = new Region();
		HBox.setHgrow(space, Priority.ALWAYS);
		
		HBox bottom = new HBox(8, priceLabel, space);
		bottom.setAlignment(Pos.CENTER_LEFT);
		
		if (actionText != null) {
			Button actionButton = new Button(actionText);
			actionButton.getStyleClass().add("card-button");
			actionButton.setOnAction(e -> onAction.accept(item));
			bottom.getChildren().add(actionButton);
		} // end of if statement
		
		VBox info = new VBox(4, nameLabel, categoryLabel, descriptionLabel, new Separator(), bottom);		info.setPadding(new Insets(12));
		
		getChildren().addAll(photo, info);
		
	} // end of constructor method
	
	/*
	 * getItem()
	 * 	- returns the product (with its stock) shown by this card
	 */
	public InventoryItem getItem() {
		return item;
	} // end of getItem
	
	/*
	 * setSelected(boolean)
	 * 	- highlights the card with a green border (used by Product Management to show the chosen product)
	 */
	public void setSelected(boolean selected) {
		
		getStyleClass().remove("card-selected");
		if (selected) {
			getStyleClass().add("card-selected");
		} // end of if statement
		
	} // end of setSelected
	
	/*
	 * createStockBadge(InventoryItem)
	 * 	- the small colored label for the stock of a product
	 * 	- "Inactive" (grey), "Out of stock" (red), "N left" (amber, low stock), or "N in stock" (green)
	 */
	public static Label createStockBadge(InventoryItem item) {
		
		Label badge = new Label();
		badge.getStyleClass().add("badge");
		
		if (!item.getProduct().isActive()) {
			badge.setText("Inactive");
			badge.getStyleClass().add("badge-inactive");
		} else if (item.getCurrentStock() <= 0) {
			badge.setText("Out of stock");
			badge.getStyleClass().add("badge-out");
		} else if (item.isLowStock()) {
			badge.setText(item.getCurrentStock() + " left");
			badge.getStyleClass().add("badge-low");
		} else {
			badge.setText(item.getCurrentStock() + " in stock");
			badge.getStyleClass().add("badge-in");
		} // end of if else statement
		
		return badge;
		
	} // end of createStockBadge
	
	/*
	 * createPhoto(Product, double, double)
	 * 	- the product's photo cropped from the middle to exactly width x height, with rounded top corners
	 * 	- if the product has no photo (and no category placeholder), a grey-green box with the first letter is drawn
	 */
	public static StackPane createPhoto(Product product, double width, double height) {
		
		StackPane box = new StackPane();
		box.setMinSize(width, height);
		box.setPrefSize(width, height);
		box.setMaxSize(width, height);
		
		Image image = ProductImages.forProduct(product);
		
		if (image != null) {
			// crop the middle part that has the same shape as the box, so the photo is not stretched
			double boxRatio = width / height;
			double imageWidth = image.getWidth();
			double imageHeight = image.getHeight();
			boolean wider = (imageWidth / imageHeight) > boxRatio;
			double cropWidth = wider ? imageHeight * boxRatio : imageWidth;
			double cropHeight = wider ? imageHeight : imageWidth / boxRatio;
			
			ImageView view = new ImageView(image);
			view.setViewport(new Rectangle2D((imageWidth - cropWidth) / 2, (imageHeight - cropHeight) / 2, cropWidth, cropHeight));
			view.setFitWidth(width);
			view.setFitHeight(height);
			box.getChildren().add(view);
			
		} else {
			Label missing = new Label(product.getName().substring(0, 1).toUpperCase());
			missing.getStyleClass().add("photo-missing");
			missing.setMaxWidth(Double.MAX_VALUE);
			missing.setMaxHeight(Double.MAX_VALUE);
			missing.setAlignment(Pos.CENTER);
			box.getChildren().add(missing);
		} // end of if else statement
		
		// round the top corners only (the clip is taller than the box, so the bottom corners are cut off)
		Rectangle clip = new Rectangle(width, height + 20);
		clip.setArcWidth(24);
		clip.setArcHeight(24);
		box.setClip(clip);
		
		return box;
		
	} // end of createPhoto
	
} // end of ProductCard class