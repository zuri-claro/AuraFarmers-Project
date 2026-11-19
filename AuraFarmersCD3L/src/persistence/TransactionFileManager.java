package persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import model.PurchasedProduct;
import model.SalesTransaction;

/*
 * TransactionFileManager class
 * 	- saves and loads all sales transactions in "transactions.txt", one per line
 * 	- id|timestamp|clerkUsername|clerkName|cashReceived|status|voidedBy|voidedAt|lines
 * 	- items = productId~productName~unitPrice~quantity, separated by ";"
 */
public class TransactionFileManager {
	
	// ATTRIBUTES
	private static final String SEPARATOR = "|";
	private final File transactionsFile;
	
	// METHODS
	/*
	 * TransactionFileManager(String)
	 * 	- constructor method
	 * 	- creates folder and puts transactions.txt in folder
	 */
	public TransactionFileManager(String folderName) {
		File folder = new File(folderName);
		folder.mkdirs();
		transactionsFile = new File(folder, "transactions.txt");
	} // end of constructor method
	
	/*
	 * saveTransaction(List<SalesTransaction>)
	 * 	- saves transactions into transactions.txt
	 */
	public void saveTransactions(List<SalesTransaction> transactions) {
		
		try {
			
			PrintWriter writer = new PrintWriter(new FileWriter(transactionsFile));
			
			// for each transaction t
			for (SalesTransaction t : transactions) {
				
				// create string for line of items
				StringBuilder items = new StringBuilder();
				for (PurchasedProduct item : t.getItems()) {
					
					if (items.length() > 0) {
						items.append(";");
					}
					
					// separate each attribute with "~"
					items.append(item.getProductId()).append("~")
							.append(clean(item.getProductName())).append("~")
							.append(item.getUnitPrice()).append("~")
							.append(item.getQuantity());
				}
				
				// write the transaction details first, then the line of items
				writer.println(t.getTransactionId() + SEPARATOR
						+ t.getTimestamp() + SEPARATOR
						+ clean(t.getClerkUsername()) + SEPARATOR
						+ clean(t.getClerkName()) + SEPARATOR
						+ t.getCashReceived() + SEPARATOR
						+ t.getStatus() + SEPARATOR
						+ clean(t.getVoidedBy()) + SEPARATOR
						+ (t.getVoidedAt() == null ? "" : t.getVoidedAt().toString()) + SEPARATOR
						+ items);
			}
			
			writer.close();
			
		} catch (IOException e) {
			System.out.println("Could not save transactions. " + e.getMessage());
		}
		
	} // end of saveTransactions
	
	/*
	 * loadTransactions()
	 * 	- loads from transactions.txt back into objects
	 */
	public List<SalesTransaction> loadTransactions() {
		
		List<SalesTransaction> transactions = new ArrayList<>();
		if (!transactionsFile.exists()) {
			return transactions;
		}

		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(transactionsFile));
			String line;
			while ((line = reader.readLine()) != null) {
				
				if (line.trim().isEmpty()) {continue;}

				String[] parts = line.split("\\|", -1);
				
				// ERROR: some line was saved wrong
				if (parts.length != 9) {
					System.out.println("Skipped line in transactions file: " + line);
					continue;
				}

				try {
					
					// items purchased
					List<PurchasedProduct> items = new ArrayList<>();
					for (String text : parts[8].split(";")) {
						if (text.trim().isEmpty()) {continue;}
						String[] f = text.split("~", -1);
						items.add(new PurchasedProduct(Integer.parseInt(f[0]), f[1], Double.parseDouble(f[2]), Integer.parseInt(f[3])));
					}
					
					// void details
					LocalDateTime voidedAt = parts[7].trim().isEmpty() ? null : LocalDateTime.parse(parts[7].trim());
					String voidedBy = parts[6].isEmpty() ? null : parts[6];
					
					// add all to transactions
					transactions.add(new SalesTransaction(Integer.parseInt(parts[0]), LocalDateTime.parse(parts[1]), parts[2], parts[3],
							items, Double.parseDouble(parts[4]), Boolean.parseBoolean(parts[5]), voidedBy, voidedAt));
					
				} catch (RuntimeException e) {
					System.out.println("Skipped a bad line in transactions file: " + line);
				}
			}
			
			reader.close();
			
		} catch (IOException e) {
			System.out.println("Could not read transactions. " + e.getMessage());
		}
		
		return transactions;
		
	} // end of loadTransactions

	// removes the separators and line breaks so they cannot break the file format
	private String clean(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("|", "/").replace(";", ",").replace("~", "-").replace("\n", " ").replace("\r", " ");
	}
	
} // end of TransactionFileManager class