package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/*
 * StockMovement class
 * 	- this will track who changed which stock, by how much, why, etc.
 */

public class StockMovement {
	
	// ATTRIBUTES
	public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss");
	public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
	
    private final int productId;
    private final int quantity;
    private final int newStock;
    private final String performedBy;
    private final String reason;
    private final LocalDateTime timestamp;

    // METHODS
    /*
     * StockMovement(int, int, int, String, String, LocalDateTime)
     * 	- constructor method
     * 	- initializes all attributes
     * 	- used for loading a saved movement
     */
    public StockMovement(int productId, int quantity, int stockAfter, String performedBy, String reason, LocalDateTime timestamp) {
        
    	this.productId = productId;
        this.quantity = quantity;
        this.newStock = stockAfter;
        this.performedBy = performedBy;
        this.reason = reason;
        this.timestamp = (timestamp == null) ? null : timestamp.withNano(0);
        
    } // end of constructor method
    
    /*
     * StockMovement(int, int, int, String, String)
     * 	- constructor method
     * 	- used for creating a new movement
     */
    public StockMovement(int productId, int quantity, int stockAfter, String performedBy, String reason) {
        this(productId, quantity, stockAfter, performedBy, reason, LocalDateTime.now());
    } // end of constructor method
    
    /*
     * GETTERS
     * 	- getProductId()
     * 	- getQuantity()
     * 	- getStockAfter()
     * 	- getPerformedBy()
     * 	- getReason()
     * 	- getTimestamp()
     *  - getDate()
     *  - getTime()
     * 
     *  - getTimestampText()
     *  - getTimestampText()
     */
    public int getProductId() {return productId;}
    public int getQuantity() {return quantity;}
    public int getStockAfter() {return newStock;}
    public String getPerformedBy() {return performedBy;}	
    public String getReason() {return reason;}
    public LocalDateTime getTimestamp() {return timestamp;}
    public LocalDate getDate() {return timestamp == null ? null : timestamp.toLocalDate();}
    public LocalTime getTime() {return timestamp == null ? null : timestamp.toLocalTime();}
    
    public String getTimestampText() {return timestamp == null ? "N/A" : timestamp.format(DATE_TIME_FORMAT);}
    public String getTimeText() {return timestamp == null ? "N/A" : timestamp.format(TIME_FORMAT);}
	
} // end of StockMovement class
