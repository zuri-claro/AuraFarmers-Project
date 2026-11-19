package model;

/*
 * PurchasedItem class
 * 	- to be used for transactions
 * 	- keeps track of the product the customer bought, its price and quantity
 */
public class PurchasedProduct {
	
	// ATTRIBUTES
	private final int productId;
	private final String productName;
	private final double unitPrice;
	private final int quantity;
	
	// METHODS
	/*
	 * PurchasedItem(int, String, double, int)
	 * 	- constructor method
	 */
	public PurchasedProduct(int productId, String productName, double unitPrice, int quantity) {
		this.productId = productId;
		this.productName = productName;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
	} // end of constructor method

	/*
	 * GETTERS
	 * 	- getProductId()
	 *  - getProductName()
	 *  - getUnitPrice()
	 *  - getQuantity()
	 */
	public int getProductId() {return productId;}
	public String getProductName() {return productName;}
	public double getUnitPrice() {return unitPrice;}
	public int getQuantity() {return quantity;}

	/*
	 * getTotal()
	 * 	- returns the total price rounded to 2 decimal places
	 */
	public double getTotal() {
		return Math.round(unitPrice * quantity * 100.0) / 100.0;
	} // end of getLineTotal
	
} // end of PurchasedItem class