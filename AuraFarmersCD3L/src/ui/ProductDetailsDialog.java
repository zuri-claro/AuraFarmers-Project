package ui;

import dashboard.Dashboard;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.InventoryItem;
import model.Product;

/*
 * ProductDetailsDialog class
 * 	- the pop-up opened by the "View" button of a product card
 * 	- big photo, name, product id, category, price with unit, stock badge, and the description
 * 	- read only: it only shows information
 * 	- utility class, so nobody should create a ProductDetailsDialog object
 */

public final class ProductDetailsDialog {
	
	// METHODS
	/*
	 * ProductDetailsDialog()
	 * 	- private constructor method
	 */
	private ProductDetailsDialog() {}
	
	/*
	 * show(InventoryItem)
	 * 	- opens the details of the product and waits until it is closed
	 */
	public static void show(InventoryItem item) {
		
		Product product = item.getProduct();
		
		Label nameLabel = new Label(product.getName());
		nameLabel.getStyleClass().add("form-title");
		nameLabel.setWrapText(true);
		
		Label idLabel = new Label(product.getDisplayId() + "  -  " + product.getCategory());
		idLabel.getStyleClass().add("muted-text");
		
		Label priceLabel = new Label(Dashboard.formatPeso(product.getPrice()) + " /" + product.getUnit());
		priceLabel.getStyleClass().add("card-price");
		
		HBox priceRow = new HBox(12, priceLabel, ProductCard.createStockBadge(item));
		priceRow.setAlignment(Pos.CENTER_LEFT);
		
		Label descriptionLabel = new Label(product.getDescription().isEmpty() ? "No description." : product.getDescription());
		descriptionLabel.setWrapText(true);
		
		Stage stage = new Stage();
		Button closeButton = new Button("Close");
		closeButton.setOnAction(e -> stage.close());
		HBox buttons = new HBox(closeButton);
		buttons.setAlignment(Pos.CENTER);
		
		VBox card = new VBox(12, ProductCard.createPhoto(product, 420, 240), nameLabel, idLabel, priceRow, descriptionLabel, buttons);
		card.getStyleClass().add("white-card");
		card.setPadding(new Insets(24));
		
		VBox root = new VBox(card);
		root.getStyleClass().add("form-dialog");
		root.setPadding(new Insets(18));
		
		Scene scene = new Scene(root);
		Theme.apply(scene);
		
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle("Product Details");
		stage.setResizable(false);
		stage.setScene(scene);
		stage.showAndWait();
		
	} // end of show
	
} // end of ProductDetailsDialog class