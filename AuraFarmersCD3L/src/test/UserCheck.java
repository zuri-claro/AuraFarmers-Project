package test;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;

import model.Clerk;
import model.Manager;
import model.PasswordHasher;
import model.User;
import persistence.UserFileManager;

/*
 * UserCheck class
 * 	- console test for passwords: hashing, login check, and the automatic hashing of users.txt
 * 	- run it as a normal Java application and look for "FAIL" lines
 * 	- uses a temporary folder, so it never touches the real "data" folder
 */

public class UserCheck {
	
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
	
	static String read(File file) throws Exception {
		return new String(Files.readAllBytes(file.toPath()), "UTF-8");
	} // end of read
	
	static User find(List<User> users, String username) {
		for (User user : users) {
			if (user.getUsername().equals(username)) {return user;}
		} // end of for loop
		return null;
	} // end of find
	
	public static void main(String[] args) throws Exception {
		
		// ----- the hasher on its own -----
		String hash1 = PasswordHasher.hash("manager123");
		String hash2 = PasswordHasher.hash("manager123");
		check("a hash starts with sha256$ and does not contain the password", hash1.startsWith("sha256$") && !hash1.contains("manager123"));
		check("same password gives a different hash each time (random salt)", !hash1.equals(hash2));
		check("both hashes accept the right password", PasswordHasher.matches("manager123", hash1) && PasswordHasher.matches("manager123", hash2));
		check("wrong password is rejected", !PasswordHasher.matches("manager124", hash1) && !PasswordHasher.matches("", hash1));
		check("a plain stored password still works (legacy)", PasswordHasher.matches("abc", "abc") && !PasswordHasher.matches("abd", "abc"));
		check("null and damaged values are rejected, not crashes", !PasswordHasher.matches(null, hash1) && !PasswordHasher.matches("x", null)
				&& !PasswordHasher.matches("x", "sha256$!!notbase64!!$abc") && !PasswordHasher.matches("x", "sha256$onlytwo"));
		check("the hash has no | character (it is the file separator)", !hash1.contains("|"));
		
		// ----- accounts made in code still work (the other tests do this) -----
		check("a Manager created with a plain password can log in", new Manager("m", "manager123", "M").checkPassword("manager123"));
		check("... and rejects a wrong one", !new Manager("m", "manager123", "M").checkPassword("nope"));
		
		// ----- users.txt with plain passwords is hashed on the first load -----
		File folder = Files.createTempDirectory("agri_users").toFile();
		File usersFile = new File(folder, "users.txt");
		try (PrintWriter writer = new PrintWriter(usersFile)) {
			writer.println("MANAGER|manager|Mr. Manager|manager123");
			writer.println("CLERK|clerk|Sir Clerk|clerk123");
			writer.println("CLERK|allen|Allen|password");
			writer.println("CLERK|caleb|Caleb|password");
		} // end of try
		
		List<User> users = new UserFileManager(folder.getPath()).loadUsers();
		check("all 4 users loaded", users.size() == 4);
		check("login works with the original passwords", find(users, "manager").checkPassword("manager123")
				&& find(users, "clerk").checkPassword("clerk123") && find(users, "allen").checkPassword("password"));
		check("login rejects wrong passwords", !find(users, "manager").checkPassword("clerk123") && !find(users, "clerk").checkPassword("manager123"));
		check("roles are kept (Manager and Clerk)", find(users, "manager") instanceof Manager && find(users, "clerk") instanceof Clerk);
		
		String afterFirstLoad = read(usersFile);
		check("the file no longer contains any readable password", !afterFirstLoad.contains("manager123")
				&& !afterFirstLoad.contains("clerk123") && !afterFirstLoad.contains("|password"));
		check("the file still has 4 lines with the right format (role|username|name|hash)", afterFirstLoad.trim().split("\n").length == 4
				&& afterFirstLoad.contains("MANAGER|manager|Mr. Manager|sha256$"));
		check("two users with the same password have different hashes", !afterFirstLoad.split("\n")[2].split("\\|")[3].equals(afterFirstLoad.split("\n")[3].split("\\|")[3]));
		
		// ----- a second start changes nothing, and the passwords still work -----
		List<User> again = new UserFileManager(folder.getPath()).loadUsers();
		check("loading again does not rewrite the file (hashes stay the same)", read(usersFile).equals(afterFirstLoad));
		check("login still works after the restart", find(again, "manager").checkPassword("manager123") && find(again, "caleb").checkPassword("password"));
		
		// ----- a new account typed by hand is hashed next time, existing hashes are untouched -----
		try (java.io.FileWriter appender = new java.io.FileWriter(usersFile, true)) {
			appender.write("CLERK|newclerk|New Clerk|fresh123\n");
		} // end of try
		List<User> third = new UserFileManager(folder.getPath()).loadUsers();
		String afterThird = read(usersFile);
		check("the new account was added and can log in", third.size() == 5 && find(third, "newclerk").checkPassword("fresh123"));
		check("the new password was hashed too", !afterThird.contains("fresh123"));
		check("old hashes did not change", afterThird.startsWith(afterFirstLoad.trim()));
		
		// ----- a missing or empty file gives an empty list (no crash) -----
		check("missing file gives an empty list", new UserFileManager(Files.createTempDirectory("agri_none").toFile().getPath()).loadUsers().isEmpty());
		
		System.out.println();
		System.out.println("Passed: " + passed + "   Failed: " + failed);
		if (failed > 0) {System.exit(1);}
		
	} // end of main
	
} // end of UserCheck class