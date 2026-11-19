package persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import model.Clerk;
import model.Manager;
import model.Role;
import model.User;

/*
 * UserFileManager class
 * 	- manages users.txt file
 */
public class UserFileManager {
	
	// ATTRIBUTES
    private final File usersFile;
    
    // METHODS
    /*
     * UserFileManager(String)
     * 	- constructor method
     */
    public UserFileManager(String folderName) {
    	
        File folder = new File(folderName);
        folder.mkdirs(); // creates the folder if it doesnt exist yet
        usersFile = new File(folder, "users.txt");
    	
    } // end of constructor method
    
    /*
     * loadUsers()
     * 	- loads from users.txt back into the users list
     */
    public List<User> loadUsers() {
    	
        List<User> users = new ArrayList<>();
        
        // error: items file is empty/never saved before
        if (!usersFile.exists()) {
            return users;
        } // end of if statement

        try {
        	
        	BufferedReader reader = new BufferedReader(new FileReader(usersFile));
            String line;
            while ((line = reader.readLine()) != null) {
            	
            	// skip whitespace
                if (line.trim().isEmpty()) {continue;}
                
                // divide each line into 4 parts
                String[] parts = line.split("\\|", -1);
                
                // error: not divided properly
                if (parts.length != 4) {
                    continue;
                } // end of if statement
                
                try {
                	
                	// get all attributes
                	Role role = Role.valueOf(parts[0]);
                    String username = parts[1].trim();
                    String displayName = parts[2].trim();
                    String password = parts[3].trim();
                    
                    // create new product and inventory item using those attributes
                    if (role.equals(Role.MANAGER)) {
                    	users.add(new Manager(username, password, displayName));
                    } else {
                    	users.add(new Clerk(username, password, displayName));
                    }
                                        
                } catch (RuntimeException e) {
                } // end of try catch
                                
            } // end of while loop
            reader.close();
            
        } catch (IOException e) {
            System.out.println("Could not read users: " + e.getMessage());
        } // end of try catch
        
        return users;
        
    } // end of loadUsers

} // end of UserFileManager
