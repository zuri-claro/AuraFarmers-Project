package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * SalesTransaction class
 * 	- contains all details related a customer transaction
 */
public class SalesTransaction {

	// ATTRIBUTES
	// 	transaction details
	private final int transactionId;
	private final LocalDateTime timestamp;
    private final double totalAmount;
    private final double cashReceived;
    private final double changeGiven;
	
	// 	clerk who processed transaction
	private final String clerkUsername;
    private final String clerkName;
    
    //	for voided
    private boolean voided;
	private String voidedBy;
	private LocalDateTime voidedAt;
	
	//	for items
    private List<PurchasedProduct> itemsPurchased;
    
    // METHODS
    /*
     * SalesTransaction(int, LocalDateTime, String, String,
     * 						List<SaleLine>, double, boolean, String, LocalDateTime)
     * 	- constructor method
     */
    public SalesTransaction(int transactionId, LocalDateTime timestamp, String clerkUsername, String clerkName,
			List<PurchasedProduct> itemsPurchased, double cashReceived, boolean status, String voidedBy, LocalDateTime voidedAt) {
    	
    	// initialize attributes
		this.transactionId = transactionId;
		this.timestamp = timestamp.withNano(0);
		this.clerkUsername = clerkUsername;
		this.clerkName = clerkName;
		this.itemsPurchased = Collections.unmodifiableList(new ArrayList<>(itemsPurchased));
		
		// gets price total of ALL items and quantities purchased
		double sum = 0;
		for (PurchasedProduct item : itemsPurchased) {
			sum += item.getTotal();
		}
		
		// calculates other attributes
		this.totalAmount = Math.round(sum * 100.0) / 100.0;
		this.cashReceived = cashReceived;
		this.changeGiven = Math.round((cashReceived - this.totalAmount) * 100.0) / 100.0;
		
		// for voided
		this.voided = status;
		this.voidedBy = voidedBy;
		this.voidedAt = voidedAt;
		
	} // end of constructor method
    
    /*
     * GETTERS
     */
	public int getTransactionId() {return transactionId;}
	public LocalDateTime getTimestamp() {return timestamp;}
	public String getClerkUsername() {return clerkUsername;}
	public String getClerkName() {return clerkName;}
	public List<PurchasedProduct> getItems() {return itemsPurchased;}
	public double getTotalAmount() {return totalAmount;}
	public double getCashReceived() {return cashReceived;}
	public double getChangeGiven() {return changeGiven;}
	public boolean getStatus() {return voided;}
	public String getVoidedBy() {return voidedBy;}
	public LocalDateTime getVoidedAt() {return voidedAt;}

	public LocalDate getTransactionDate() {return timestamp.toLocalDate();}
	public LocalTime getTransactionTime() {return timestamp.toLocalTime();}
	
	public String getTimestampText() {return timestamp.format(DateFormats.DATE_TIME);}                 // 11/14/26 21:04
	public String getTimestampSecondsText() {return timestamp.format(DateFormats.DATE_TIME_SECONDS);}  // 11/14/26 21:04:00

	/*
	 * isVoided()
	 * 	- returns if transaction is voided
	 */
	public boolean isVoided() {return voided;}
	
	/*
	 * getTotalQuantity()
	 * 	- returns total number of items bought in the transaction
	 */
	public int getTotalQuantity() {
		int total = 0;
		for (PurchasedProduct item : itemsPurchased) {
			total += item.getQuantity();
		}
		return total;
	} // end of getTotalQuantity
	
	/*
	 * markVoided(String, LocalDateTime)
	 * 	- marks transaction as voided
	 */
	public void markVoided(String byUsername, LocalDateTime when) {
		this.voided = true;
		this.voidedBy = byUsername;
		this.voidedAt = when.withNano(0);
	} // end of markVoided
	
} // end of SalesTransaction class