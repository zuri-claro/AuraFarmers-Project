package ui;

import java.io.InputStream;
import java.net.URL;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.util.Callback;
import model.InventoryItem;

/*
 * Theme class
 * 	- one place for the look of the app
 * 	- the colors, fonts, and button styles are all written in resources/aura.css, this class only attaches that stylesheet to windows and has a few small helpers
 * 	- utility class, so nobody should create a Theme object
 */

public final class Theme {
	
	// ATTRIBUTES
	private static final String STYLESHEET = "/resources/aura.css";
	
	// METHODS
	/*
	 * Theme()
	 * 	- private constructor method
	 */
	private Theme() {}
	
	/*
	 * apply(Scene)
	 * 	- attaches aura.css to a scene (does nothing if it is already attached or the file is missing)
	 * 	- every new window (Scene) needs this once
	 */
	public static void apply(Scene scene) {
		
		URL url = Theme.class.getResource(STYLESHEET);
		if (url != null && !scene.getStylesheets().contains(url.toExternalForm())) {
			scene.getStylesheets().add(url.toExternalForm());
		} // end of if statement
		
	} // end of apply
	
	/*
	 * apply(DialogPane)
	 * 	- same as apply(Scene), but for pop-ups made with Alert or Dialog
	 */
	public static void apply(DialogPane pane) {
		
		URL url = Theme.class.getResource(STYLESHEET);
		if (url != null && !pane.getStylesheets().contains(url.toExternalForm())) {
			pane.getStylesheets().add(url.toExternalForm());
		} // end of if statement
		
	} // end of apply
	
	/*
	 * addStyle(Node, String)
	 * 	- adds a style class from aura.css to a node, only once
	 */
	public static void addStyle(Node node, String styleClass) {
		
		if (!node.getStyleClass().contains(styleClass)) {
			node.getStyleClass().add(styleClass);
		} // end of if statement
		
	} // end of addStyle
	
	/*
	 * sectionTitle(String)
	 * 	- returns a label styled like the green section headings (e.g. "Inventory Overview")
	 */
	public static Label sectionTitle(String text) {
		
		Label label = new Label(text);
		label.getStyleClass().add("section-title");
		return label;
		
	} // end of sectionTitle
	
	/*
	 * loadImage(String)
	 * 	- loads an image from the resources folder, e.g. "/resources/logo_name.png"
	 * 	- returns null if the file is missing, so the app still runs without it
	 */
	public static Image loadImage(String path) {
		
		InputStream stream = Theme.class.getResourceAsStream(path);
		return stream == null ? null : new Image(stream);
		
	} // end of loadImage
	
	/*
	 * logo(double)
	 * 	- the Aura logo scaled to the given height (null if the image is missing)
	 */
	public static ImageView logo(double height) {
		
		Image image = loadImage("/resources/logo_name.png");
		if (image == null) {
			return null;
		} // end of if statement
		
		ImageView view = new ImageView(image);
		view.setFitHeight(height);
		view.setPreserveRatio(true);
		return view;
		
	} // end of logo
	
	/*
	 * avatar(double)
	 * 	- the profile picture cropped to a circle of the given size (null if the image is missing)
	 */
	public static ImageView avatar(double size) {
		
		Image image = loadImage("/resources/profile_picture.jpg");
		if (image == null) {
			return null;
		} // end of if statement
		
		// crop the middle square so the picture is not stretched
		double side = Math.min(image.getWidth(), image.getHeight());
		ImageView view = new ImageView(image);
		view.setViewport(new Rectangle2D((image.getWidth() - side) / 2, (image.getHeight() - side) / 2, side, side));
		view.setFitWidth(size);
		view.setFitHeight(size);
		view.setClip(new Circle(size / 2, size / 2, size / 2));
		return view;
		
	} // end of avatar
	
	/*
	 * stockRowFactory()
	 * 	- colors the rows of an inventory table like the Figma design:
	 * 		red for out of stock, amber for low stock, no color for products that are fine (or inactive)
	 * 	- use it with table.setRowFactory(Theme.stockRowFactory())
	 */
	public static Callback<TableView<InventoryItem>, TableRow<InventoryItem>> stockRowFactory() {
		
		return table -> new TableRow<InventoryItem>() {
			
			@Override
			protected void updateItem(InventoryItem item, boolean empty) {
				
				super.updateItem(item, empty);
				getStyleClass().removeAll("row-out", "row-low");
				
				if (!empty && item != null && item.getProduct().isActive()) {
					if (item.getCurrentStock() <= 0) {
						getStyleClass().add("row-out");
					} else if (item.isLowStock()) {
						getStyleClass().add("row-low");
					} // end of if else statement
				} // end of if statement
				
			} // end of updateItem
			
		};
		
	} // end of stockRowFactory
	
} // end of Theme class
