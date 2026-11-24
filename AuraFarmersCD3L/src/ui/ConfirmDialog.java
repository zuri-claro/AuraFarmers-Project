package ui;

/*
 * ConfirmDialog class
 * 	- the Yes/No confirmation pop-ups from the Figma design (Void Transaction, Deactivate Product)
 * 	- same look as the other dialogs (it is a FormDialog with a message instead of input fields)
 * 	- every method returns true if the user clicked the confirm button, false if they cancelled
 * 	- utility class, so nobody should create a ConfirmDialog object
 */

public final class ConfirmDialog {
	
	// METHODS
	/*
	 * ConfirmDialog()
	 * 	- private constructor method
	 */
	private ConfirmDialog() {}
	
	/*
	 * show(String, String, String, boolean)
	 * 	- general version: title, message, text of the confirm button,
	 * 		and whether the action is destructive (red button) or normal (orange button)
	 */
	public static boolean show(String title, String message, String confirmText, boolean destructive) {
		
		FormDialog dialog = new FormDialog(title, confirmText);
		dialog.addMessage(message);
		if (destructive) {
			dialog.markDestructive();
		} // end of if statement
		
		return dialog.showAndWait(() -> null); // nothing to check, so confirming always works
		
	} // end of show
	
	/*
	 * confirmVoid(String)
	 * 	- "Void Transaction" pop-up, e.g. confirmVoid("TS001")
	 */
	public static boolean confirmVoid(String transactionId) {
		
		return show("Void Transaction",
				"Void transaction " + transactionId + "?\n\n"
				+ "The items will be put back into the inventory, and the transaction stays in the records as VOIDED. "
				+ "This cannot be undone.",
				"Void", true);
		
	} // end of confirmVoid
	
	/*
	 * confirmDeactivate(String)
	 * 	- "Deactivate Product" pop-up
	 */
	public static boolean confirmDeactivate(String productName) {
		
		return show("Deactivate Product",
				"Deactivate " + productName + "?\n\n"
				+ "It will no longer be available for sale. You can activate it again anytime.",
				"Deactivate", true);
		
	} // end of confirmDeactivate
	
	/*
	 * confirmActivate(String)
	 * 	- "Activate Product" pop-up (same style, not a red button because nothing is lost)
	 */
	public static boolean confirmActivate(String productName) {
		
		return show("Activate Product",
				"Activate " + productName + "?\n\n"
				+ "It will be available for sale again.",
				"Activate", false);
		
	} // end of confirmActivate
	
} // end of ConfirmDialog class
