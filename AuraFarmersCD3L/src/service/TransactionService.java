package service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.DateRange;
import model.InventoryItem;
import model.PurchasedProduct;
import model.Role;
import model.SalesTransaction;
import model.User;
import persistence.TransactionFileManager;

/*
 * TransactionService class
 */
public class TransactionService {
	
	// ATTRIBUTES
	private final InventoryService inventoryService;
	private final TransactionFileManager fileManager;
	private final List<SalesTransaction> transactions;
	
	// METHODS
	/*
	 * TransactionService(InventoryService, TransactionFileManager)
	 * 	- constructor method
	 */
	public TransactionService(InventoryService inventoryService, TransactionFileManager fileManager) {
		this.inventoryService = inventoryService;
		this.fileManager = fileManager;
		this.transactions = fileManager.loadTransactions();
	} // end of TransactionService

	/*
	 * checkout(User, Map<Integer, Integer>, double)
	 * 	- only clerk can use this
	 *  - if anything is wrong, nothing is sold and no stock changes.
	 * 	- returns the saved transaction (with id, total, and change)
	 */
	public SalesTransaction checkout(User user, Map<Integer, Integer> cart, double cashReceived, String customerName) {
		
		// ERROR: manager tries to checkout
		if (user == null || !user.canProcessSale()) {
			System.out.println("Only a Clerk can process sales.");
			return null;
		}
		// ERROR: cart is empty
		if (cart == null || cart.isEmpty()) {
			System.out.println("The cart is empty.");
			return null;
		}
		// ERROR: invalid cash amount received
		if (Double.isNaN(cashReceived) || Double.isInfinite(cashReceived) || cashReceived < 0) {
			System.out.println("Cash received is not a valid amount.");
			return null;
		}
		
		// keep track of purchased products
		List<PurchasedProduct> purchasedProducts = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : cart.entrySet()) { // maps product id to quantity

			InventoryItem item = inventoryService.getInventory().findByProductId(entry.getKey());
			
			// ERROR: invalid product id
			if (item == null) {
				System.out.println("Product not found: " + entry.getKey());
				return null;
			}
			
			// ERROR: invalid quantity
			if (entry.getValue() == null || entry.getValue() <= 0) {
				System.out.println("Quantity must be greater than zero.");
				return null;
			}

			purchasedProducts.add(new PurchasedProduct(item.getProductId(), item.getProduct().getName(), item.getProduct().getPrice(), entry.getValue()));
		
		}
		
		// create sales transaction
		int id = nextTransactionId();
		SalesTransaction transaction = new SalesTransaction(customerName, id, LocalDateTime.now(), user.getUsername(),
				user.getDisplayName(), purchasedProducts, cashReceived, false, null, null);
		
		// ERROR: insufficient cash
		if (cashReceived < transaction.getTotalAmount()) {
			System.out.println("Insufficient cash. The total is " + String.format("%.2f", transaction.getTotalAmount()) + ".");
			return null;
		}

		// checks everything (active, enough stock) before changing anything, then lowers the stock
		inventoryService.deductForSale(user, new LinkedHashMap<>(cart), "Sale " + id);

		transactions.add(transaction);
		fileManager.saveTransactions(transactions);
		return transaction;
		
	} // end of checkout

	/*
	 * voidTransaction(User, int)
	 * 	- manager can void any transaction, clerk only their own
	 */
	public void voidTransaction(User user, int transactionId) {
		
		// ERROR
		if (user == null) {
			return;
		}

		SalesTransaction transaction = findById(transactionId);
		if (transaction == null) {
			throw new IllegalArgumentException("Transaction not found: " + transactionId);
		}

		boolean isManager = user.getRole() == Role.MANAGER;
		
		// ERROR
		if (!isManager && !transaction.getClerkUsername().equals(user.getUsername())) {
			System.out.println("A Clerk can only void their own transactions.");
			return;
		}
		
		// ERROR
		if (transaction.isVoided()) {
			System.out.println("Transaction " + transactionId + " is already voided.");
			return;
		}
		
		// get total sum of quantities of items from voided transaction
		Map<Integer, Integer> quantities = new LinkedHashMap<>();
		for (PurchasedProduct item : transaction.getItems()) {
			quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
		}

		inventoryService.restoreForVoid(user, quantities, "Void " + transactionId);

		transaction.markVoided(user.getUsername(), LocalDateTime.now());
		fileManager.saveTransactions(transactions);
		
	} // end of voidTransaction

	/*
	 * findById(int)
	 * 	- returns salestransaction based on provided id
	 */
	public SalesTransaction findById(int transactionId) {
		for (SalesTransaction t : transactions) {
			if (t.getTransactionId() == transactionId) {
				return t;
			}
		}
		return null;
	} // end of findById

	// every transaction, newest first
	/*
	 * getAll()
	 */
	public List<SalesTransaction> getAll() {
		return search(null, DateRange.ANY_TIME, null);
	} // end of getAll

	/*
	 * getByClerk(String)
	 * 	- returns a list of the valid transactions of clerk by username
	 */
	public List<SalesTransaction> getByClerk(String clerkUsername) {
		return searchByClerk(clerkUsername, DateRange.ANY_TIME, false);
	} // end of getByClerk

	/*
	 * search(String, DateRange, boolean)
	 * 	- manager can search for either clerk username or name
	 * 	- returns that clerk's sales transactions
	 */
	public List<SalesTransaction> search(String clerkText, DateRange range, Boolean voided) {

		String query = clerkText == null ? "" : clerkText.trim().toLowerCase();
		LocalDateTime now = LocalDateTime.now();
		List<SalesTransaction> results = new ArrayList<>();

		for (SalesTransaction t : transactions) {

			if (voided != null && t.getStatus() != voided) {continue;}
			if (range != null && !range.contains(t.getTimestamp(), now)) {continue;}
			if (!query.isEmpty()
					&& !t.getClerkUsername().toLowerCase().contains(query)
					&& !t.getClerkName().toLowerCase().contains(query)) {continue;}

			results.add(t);
		}

		Collections.reverse(results); // stored oldest first
		return results;
		
	} // end of search

	/*
	 * searchByClerk(String, DateRange, boolean)
	 * 	- for gui manager screen to search by clerk username
	 */
	public List<SalesTransaction> searchByClerk(String clerkUsername, DateRange range, boolean status) {
		List<SalesTransaction> results = new ArrayList<>();
		for (SalesTransaction t : search(null, range, status)) {
			if (t.getClerkUsername().equals(clerkUsername)) {
				results.add(t);
			}
		}
		return results;
	} // end of searchByClerk

	/*
	 * countTransactions(String, boolean, DateRange)
	 * 	- counts how many transactions were made by that clerk in that date range
	 */
	public int countTransactions(String clerkUsername, boolean status, DateRange range) {
		int count = 0;
		LocalDateTime now = LocalDateTime.now();
		for (SalesTransaction t : transactions) {
			if (matches(t, clerkUsername, status, range, now)) {
				count++;
			}
		}
		return count;
	} // end of countTransactions

	/*
	 * totalSales(String, DateRange)
	 * 	- gets the total money from valid transactions by a certain clerk
	 */
	public double totalSales(String clerkUsername, DateRange range) {
		double total = 0;
		LocalDateTime now = LocalDateTime.now();
		for (SalesTransaction t : transactions) {
			if (!t.isVoided() && matches(t, clerkUsername, false, range, now)) {
				total += t.getTotalAmount();
			}
		}
		return Math.round(total * 100.0) / 100.0;
	} // end of totalSales

	/*
	 * totalItemsSold(String, DateRange)
	 * 	- returns how many total items were sold in valid transactions by a given clerk
	 */
	public int totalItemsSold(String clerkUsername, DateRange range) {
		int total = 0;
		LocalDateTime now = LocalDateTime.now();
		for (SalesTransaction t : transactions) {
			if (matches(t, clerkUsername, false, range, now)) {
				total += t.getTotalQuantity();
			}
		}
		return total;
	} // end of totalItemsSold

	/*
	 * matches(SalesTransaction, String, boolean, DateRange, LocalDateTime)
	 *  - checks if given sales transaction matches other given data
	 */
	private boolean matches(SalesTransaction t, String clerkUsername, boolean status, DateRange range, LocalDateTime now) {
		return (clerkUsername == null || t.getClerkUsername().equals(clerkUsername))
				&& (t.getStatus() == status)
				&& (range == null || range.contains(t.getTimestamp(), now));
	} // end of matches

	/*
	 * nextTransactionId()
	 * 	- returns the next available transaction id
	 */
	private int nextTransactionId() {
		int highest = 0;
		for (SalesTransaction t : transactions) {
			try {
				highest = Math.max(highest, t.getTransactionId());
			} catch (RuntimeException e) {
			}
		}
		return highest + 1;
	} // end of nextTransactionId
	
} // end of TransactionService class
