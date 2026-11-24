package ui;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import model.Category;
import model.Product;

/*
 * ProductImages class
 * 	- finds the photo of a product in the resources folder
 * 	- photos are saved as  src/resources/products/<category folder>/<product name>.jpg  (or .jpeg / .png)
 * 		category folder = the category's constant in lower case, e.g. seeds, fertilizers_soil, pesticides,
 * 		hand_tools, watering, planting_materials
 * 		product name = the name in lower case with spaces turned into hyphens,
 * 		e.g. "Tomato Seeds" becomes tomato-seeds.jpg
 * 	- search order: the product's own category folder, then the other category folders (in case the Manager
 * 		moved the product to another category), then the category's placeholder
 * 		(resources/products/placeholders/<category folder>.png), then nothing (the card draws a letter instead)
 * 	- found images are kept in a cache, so each file is only read once
 * 	- utility class, so nobody should create a ProductImages object
 */

public final class ProductImages {
	
	// ATTRIBUTES
	private static final String ROOT = "/resources/products/";
	private static final String[] EXTENSIONS = {".jpg", ".jpeg", ".png"};
	private static final Map<String, Image> CACHE = new HashMap<>(); // path -> image (null = no such file)
	
	// METHODS
	/*
	 * ProductImages()
	 * 	- private constructor method
	 */
	private ProductImages() {}
	
	/*
	 * fileName(String)
	 * 	- turns a product name into its photo file name (without the extension)
	 * 	- "Tomato Seeds" -> "tomato-seeds", "General-Purpose Insecticide" -> "general-purpose-insecticide"
	 */
	public static String fileName(String productName) {
		
		return productName.trim().toLowerCase()
				.replaceAll("[^a-z0-9]+", "-")   // anything that is not a letter or number becomes a hyphen
				.replaceAll("^-+|-+$", "");      // no hyphen at the start or end
		
	} // end of fileName
	
	/*
	 * folderName(Category)
	 * 	- the folder of a category, e.g. HAND_TOOLS -> "hand_tools"
	 */
	public static String folderName(Category category) {
		return category.name().toLowerCase();
	} // end of folderName
	
	/*
	 * forProduct(Product)
	 * 	- returns the photo of the product, or the placeholder of its category, or null if there is neither
	 */
	public static Image forProduct(Product product) {
		
		String name = fileName(product.getName());
		
		// 1) the product's own category folder
		Image image = find(folderName(product.getCategory()) + "/" + name);
		if (image != null) {
			return image;
		} // end of if statement
		
		// 2) the other category folders, and the products folder itself
		for (Category other : Category.values()) {
			if (other != product.getCategory()) {
				image = find(folderName(other) + "/" + name);
				if (image != null) {
					return image;
				} // end of if statement
			} // end of if statement
		} // end of for loop
		image = find(name);
		if (image != null) {
			return image;
		} // end of if statement
		
		// 3) the placeholder of the category
		return find("placeholders/" + folderName(product.getCategory()));
		
	} // end of forProduct
	
	/*
	 * find(String)
	 * 	- tries the .jpg, .jpeg, and .png versions of a path (relative to resources/products/)
	 * 	- returns null if none of them exists
	 */
	private static Image find(String relativePath) {
		
		for (String extension : EXTENSIONS) {
			String path = ROOT + relativePath + extension;
			
			if (!CACHE.containsKey(path)) {
				CACHE.put(path, load(path));
			} // end of if statement
			
			if (CACHE.get(path) != null) {
				return CACHE.get(path);
			} // end of if statement
		} // end of for loop
		return null;
		
	} // end of find
	
	/*
	 * load(String)
	 * 	- reads one image file, or returns null if it does not exist or cannot be read
	 */
	private static Image load(String path) {
		
		try (InputStream stream = ProductImages.class.getResourceAsStream(path)) {
			if (stream == null) {
				return null;
			} // end of if statement
			Image image = new Image(stream);
			return image.isError() ? null : image;
		} catch (java.io.IOException e) {
			return null;
		} // end of try catch
		
	} // end of load
	
} // end of ProductImages class