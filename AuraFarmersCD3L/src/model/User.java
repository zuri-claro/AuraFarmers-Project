package model;

/*
 * User class
 * 	- abstract
 */

public abstract class User {
	
	// ATTRIBUTES
	private final String username; // final
	private String password; // a salted hash when loaded from users.txt (see PasswordHasher); a plain value is also accepted
	private String displayName;
	private final Role role; // final
	
	// METHODS
	/*
	 * User(String, String, String, String)
	 * 	- constructor method
	 * 	- initializes username, password, displayName, role
	 */
	protected User(String username, String password, String displayName, Role role) {
		
		this.username = username;
		this.password = password;
		this.displayName = displayName;
		this.role = role;
		
	} // end of constructor method
	
	/*
	 * GETTERS
	 * 	- getUsername()
	 * 	- getName()
	 * 	- getRole()
	 */
	public String getUsername() {return username;}
	public String getDisplayName() {return displayName;}
	public Role getRole() {return role;}
		
	/*
	 * checkPassword(String)
	 * 	- normal (not abstract) method: true if the typed password matches the stored one
	 * 	- the comparison is done by PasswordHasher, so it works with a hash and with a plain password
	 */
	public boolean checkPassword(String input) {
		return PasswordHasher.matches(input, this.password);
	} // end of checkPassword
	
	/*
	 * getStoredPassword()
	 * 	- the value kept for the password (the hash), only used by UserFileManager when saving users.txt
	 */
	public String getStoredPassword() {return password;}
	
	/*
	 * canModifyInventory()
	 * 	- returns true for manager, false for clerk
	 */
	public abstract boolean canModifyInventory();
	
	/*
	 * canModifyInventory()
	 * 	- returns false for manager, true for clerk
	 */
	public abstract boolean canProcessSale();
	
	/*
	 * toString()
	 * 	- returns a string containing the user's full name and their role
	 */
    @Override
    public String toString() {
        return this.displayName + " (" + role + ")";
    } // end of toString
    	
} // end of User class