package model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/*
 * PasswordHasher class
 * 	- stores passwords as a salted SHA-256 hash, so users.txt never contains a readable password
 * 	- a stored password looks like  sha256$<salt>$<hash>   (the salt is random, so two users with the
 * 		same password get different hashes)
 * 	- a stored value that does NOT start with "sha256$" is treated as a plain password typed by hand
 * 		into users.txt; UserFileManager turns it into a hash the next time the app starts
 * 	- utility class, so nobody should create a PasswordHasher object
 */

public final class PasswordHasher {
	
	// ATTRIBUTES
	private static final String PREFIX = "sha256$";
	private static final SecureRandom RANDOM = new SecureRandom();
	
	// METHODS
	/*
	 * PasswordHasher()
	 * 	- private constructor method
	 */
	private PasswordHasher() {}
	
	/*
	 * isHashed(String)
	 * 	- true if the stored value is already a hash made by this class
	 */
	public static boolean isHashed(String stored) {
		return stored != null && stored.startsWith(PREFIX);
	} // end of isHashed
	
	/*
	 * hash(String)
	 * 	- returns "sha256$<salt>$<hash>" for the password, using a new random salt each time
	 */
	public static String hash(String password) {
		
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return PREFIX + Base64.getEncoder().encodeToString(salt) + "$" + digest(salt, password);
		
	} // end of hash
	
	/*
	 * matches(String, String)
	 * 	- true if the password typed by the user matches the stored value
	 * 	- works for a hash, and also for a plain password written by hand in users.txt
	 */
	public static boolean matches(String input, String stored) {
		
		if (input == null || stored == null) {
			return false;
		} // end of if statement
		
		// plain password (not hashed yet)
		if (!isHashed(stored)) {
			return input.equals(stored);
		} // end of if statement
		
		String[] parts = stored.split("\\$"); // ["sha256", salt, hash]
		if (parts.length != 3) {
			return false;
		} // end of if statement
		
		try {
			byte[] salt = Base64.getDecoder().decode(parts[1]);
			byte[] expected = parts[2].getBytes(StandardCharsets.UTF_8);
			byte[] actual = digest(salt, input).getBytes(StandardCharsets.UTF_8);
			return MessageDigest.isEqual(actual, expected); // compares in constant time
		} catch (IllegalArgumentException e) { // the salt is not valid Base64, so the stored value is damaged
			return false;
		} // end of try catch
		
	} // end of matches
	
	/*
	 * digest(byte[], String)
	 * 	- SHA-256 of (salt + password), written as Base64 text
	 */
	private static String digest(byte[] salt, String password) {
		
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			return Base64.getEncoder().encodeToString(md.digest(password.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available on this computer.", e);
		} // end of try catch
		
	} // end of digest
	
} // end of PasswordHasher class