package test;

import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import model.Category;
import model.Clerk;
import model.DateRange;
import model.Inventory;
import model.InventoryItem;
import model.Manager;
import model.Product;
import model.SalesTransaction;
import persistence.InventoryFileManager;
import persistence.TransactionFileManager;
import service.InventoryService;
import service.TransactionService;

/*
 * TransactionCheck class
 * 	- console test for sales (checkout), voiding, searching and saving
 * 	- run it as a normal Java application and look for "FAIL" lines
 * 	- saves into a temporary folder, so it never touches the real "data" folder
 */

public class TransactionCheck {
	
	interface Action {void run();}
	
	static int passed = 0;
	static int failed = 0;
	
	static void check(String name, boolean condition) {
		if (condition) {
			passed++;
			System.out.println("PASS  " + name);
		} else {
			failed++;
			System.out.println("FAIL  " + name);
		} // end of if else statement
	} // end of check
	
	static void expectThrows(String name, Class<? extends RuntimeException> type, Action action) {
		try {
			action.run();
			failed++;
			System.out.println("FAIL  " + name + " (no exception thrown)");
		} catch (RuntimeException e) {
			check(name, type.isInstance(e));
		} // end of try catch
	} // end of expectThrows
	
	static Map<Integer, Integer> cart(int... idAndQuantity) {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		for (int i = 0; i < idAndQuantity.length; i += 2) {
			map.put(idAndQuantity[i], idAndQuantity[i + 1]);
		} // end of for loop
		return map;
	} // end of cart
	
	public static void main(String[] args) throws Exception {
		
		File folder = Files.createTempDirectory("agri_tx").toFile();
		InventoryFileManager inventoryFiles = new InventoryFileManager(folder.getPath());
		TransactionFileManager transactionFiles = new TransactionFileManager(folder.getPath());
		
		Inventory inventory = new Inventory();
		inventory.addItem(new InventoryItem(new Product(1, "Tomato Seeds", "d", 35.0, Category.SEEDS, true, "Packet"), 10, 5));
		inventory.addItem(new InventoryItem(new Product(2, "Complete Fertilizer", "d", 180.0, Category.FERTILIZERS_SOIL, true, "Bag"), 5, 5));
		inventory.addItem(new InventoryItem(new Product(3, "Old Item", "d", 10.0, Category.SEEDS, false, "Piece"), 9, 5));
		inventoryFiles.saveItems(inventory.getItems());
		
		InventoryService inv = new InventoryService(inventory, inventoryFiles);
		TransactionService tx = new TransactionService(inv, transactionFiles);
		
		Manager manager = new Manager("manager", "manager123", "Mgr");
		Clerk juan = new Clerk("juan", "pw12345", "Juan");
		Clerk ana = new Clerk("ana", "pw12345", "Ana");
		
		// ----- the proposal's worked example: 2 Tomato Seeds (35) + 1 Complete Fertilizer (180) = 250, pays 300, change 50 -----
		SalesTransaction t1 = tx.checkout(juan, cart(1, 2, 2, 1), 300, "customer1");
		check("checkout returns a transaction", t1 != null);
		check("total is 250 and change is 50", t1.getTotalAmount() == 250.0 && t1.getChangeGiven() == 50.0);
		check("stock goes down after the sale", inventory.findByProductId(1).getCurrentStock() == 8 && inventory.findByProductId(2).getCurrentStock() == 4);
		check("transaction is valid and has the clerk", !t1.isVoided() && t1.getClerkUsername().equals("juan"));
		check("customer name is saved", t1.getCustomerName().equals("customer1"));
		check("price snapshot kept in the items", t1.getItems().get(0).getUnitPrice() == 35.0 && t1.getTotalQuantity() == 3);
		
		// ----- sale rules -----
		check("manager cannot process a sale", tx.checkout(manager, cart(1, 1), 100, "customer2") == null);
		check("empty cart is rejected", tx.checkout(juan, cart(), 100, "customer3") == null);
		check("cash below the total is rejected", tx.checkout(juan, cart(2, 1), 100, "customer4") == null);
		check("failed sales did not change the stock", inventory.findByProductId(2).getCurrentStock() == 4);
		check("unknown product is rejected", tx.checkout(juan, cart(99, 1), 100, "customer5") == null);
		check("quantity 0 is rejected", tx.checkout(juan, cart(1, 0), 100, "customer6") == null);
		expectThrows("not enough stock throws", IllegalStateException.class, () -> tx.checkout(juan, cart(1, 3, 2, 99), 100000, "customer7"));
		check("nothing changed after the stock error (all or nothing)", inventory.findByProductId(1).getCurrentStock() == 8 && tx.getAll().size() == 1);
		expectThrows("inactive product cannot be sold", IllegalArgumentException.class, () -> tx.checkout(juan, cart(3, 1), 100, "customer8"));
		
		// ----- search and totals -----
		SalesTransaction t2 = tx.checkout(ana, cart(1, 1), 50, "customer9");
		check("ids count up", t2.getTransactionId() == t1.getTransactionId() + 1);
		check("getAll is newest first", tx.getAll().get(0).getTransactionId() == t2.getTransactionId());
		check("getByClerk only returns that clerk", tx.getByClerk("juan").size() == 1 && tx.getByClerk("ana").size() == 1);
		check("search by clerk name or username", tx.search("ju", DateRange.ANY_TIME, null).size() == 1 && tx.search("ANA", DateRange.ANY_TIME, null).size() == 1);
		check("total sales of a clerk", tx.totalSales("juan", DateRange.ANY_TIME) == 250.0);
		check("items sold by a clerk", tx.totalItemsSold("juan", DateRange.ANY_TIME) == 3);
		check("search for today finds today's sales", tx.search(null, DateRange.TODAY, false).size() == 2);
		
		// ----- void -----
		int stock1 = inventory.findByProductId(1).getCurrentStock();   // 7 (8 - 1 from Ana's sale)
		int stock2 = inventory.findByProductId(2).getCurrentStock();   // 4
		tx.voidTransaction(ana, t1.getTransactionId());                // Ana is not allowed to void Juan's sale
		check("a clerk cannot void another clerk's sale", !t1.isVoided() && inventory.findByProductId(1).getCurrentStock() == stock1);
		tx.voidTransaction(juan, t1.getTransactionId());
		check("clerk can void their own sale", t1.isVoided() && t1.getVoidedBy().equals("juan") && t1.getVoidedAt() != null);
		check("void puts the stock back", inventory.findByProductId(1).getCurrentStock() == stock1 + 2 && inventory.findByProductId(2).getCurrentStock() == stock2 + 1);
		
		int afterVoid1 = inventory.findByProductId(1).getCurrentStock();
		int afterVoid2 = inventory.findByProductId(2).getCurrentStock();
		tx.voidTransaction(manager, t1.getTransactionId());            // already voided: must change NOTHING
		check("voiding twice does not restore the stock twice", inventory.findByProductId(1).getCurrentStock() == afterVoid1 && inventory.findByProductId(2).getCurrentStock() == afterVoid2);
		check("voiding twice keeps the first voider", t1.getVoidedBy().equals("juan"));
		
		tx.voidTransaction(manager, t2.getTransactionId());
		check("manager can void any sale", t2.isVoided() && t2.getVoidedBy().equals("manager"));
		expectThrows("unknown transaction id", IllegalArgumentException.class, () -> tx.voidTransaction(manager, 999));
		check("voided sales are not counted in totals", tx.totalSales("juan", DateRange.ANY_TIME) == 0.0 && tx.countTransactions("juan", true, DateRange.ANY_TIME) == 1);
		check("voided sales stay in the records", tx.getAll().size() == 2 && tx.search(null, DateRange.ANY_TIME, true).size() == 2);
		
		// ----- persistence: a new TransactionService reading the same file sees everything -----
		TransactionService reloaded = new TransactionService(inv, new TransactionFileManager(folder.getPath()));
		SalesTransaction r1 = reloaded.findById(t1.getTransactionId());
		check("transactions survive a restart", reloaded.getAll().size() == 2 && r1 != null);
		check("voided state survives a restart", r1.isVoided() && r1.getVoidedBy().equals("juan") && r1.getTotalAmount() == 250.0 && r1.getItems().size() == 2);
		
		System.out.println();
		System.out.println("Passed: " + passed + "   Failed: " + failed);
		if (failed > 0) {System.exit(1);}
		
	} // end of main
	
} // end of TransactionCheck class