package ui;

import app.AgriSystem;
import dashboard.ClerkDashboard;
import dashboard.ManagerDashboard;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Clerk;
import model.Manager;
import model.Role;
import model.User;

/*
 * LoginScreen class
 * 	- first screen to pop up
 * 	- "Forgot Password?" is only a label for now, clicking it does nothing
 * 	- a successful login swaps the scene's root to the Dashboard
 */

public class LoginScreen extends StackPane {
	
	// ATTRIBUTES
	private final AgriSystem system;
	private final Stage stage;
	
	private final TextField usernameField = new TextField();
	private final PasswordField passwordField = new PasswordField();
	private final Label errorLabel = new Label();
	
	// METHODS
	/*
	 * LoginScreen(AgriSystem, Stage)
	 * 	- constructor method
	 */
	public LoginScreen(AgriSystem system, Stage stage) {
		
		this.system = system;
		this.stage = stage;
		
		getStyleClass().add("login-root"); // green background
		setPadding(new Insets(40));
		getChildren().add(buildCard());
		
	} // end of constructor method
	
	/*
	 * buildCard()
	 * 	- the card in the middle of the screen that holds the form
	 */
	private HBox buildCard() {
		
		VBox right = buildForm();
		right.setPrefWidth(340);
		
		HBox card = new HBox(40);
		card.getStyleClass().add("content-card");
		card.setAlignment(Pos.CENTER);
		card.setPadding(new Insets(50));
		card.setMaxSize(860, 460);
		
		// the Aura logo on the left, like in the Figma design (skipped if the image file is missing)
		ImageView logo = Theme.logo(180);
		if (logo != null) {
			card.getChildren().add(logo);
		} // end of if statement
		
		card.getChildren().add(right);
		return card;
		
	} // end of buildCard
	
	/*
	 * buildForm()
	 * 	- welcome text, username and password fields, forgot password, sign in button
	 */
	private VBox buildForm() {
		
		Label welcome = new Label("Welcome!");
		Label subtitle = new Label("Sign in to your Account");
		
		usernameField.setPromptText("Username");
		usernameField.setMaxWidth(320);
		usernameField.setOnAction(e -> passwordField.requestFocus()); // enter moves to password
		
		passwordField.setPromptText("Password");
		passwordField.setMaxWidth(320);
		passwordField.setOnAction(e -> onLogin()); // enter signs in
		
		Label forgot = new Label("Forgot Password?"); 
		forgot.setOnMouseClicked(e -> showUsersPopup());
		
		Button signIn = new Button("SIGN IN");
		signIn.setOnAction(e -> onLogin());
		
		errorLabel.setWrapText(true);
		
		welcome.getStyleClass().add("login-welcome");
		subtitle.getStyleClass().add("login-subtitle");
		forgot.getStyleClass().add("login-link");
		errorLabel.getStyleClass().add("form-error");
		
		VBox form = new VBox(14, welcome, subtitle, usernameField, passwordField, forgot, signIn, errorLabel);
		form.setAlignment(Pos.CENTER);
		VBox.setMargin(usernameField, new Insets(12, 0, 0, 0));
		return form;
		
	} // end of buildForm
	
	/*
	 * onLogin()
	 * 	- checks the fields, logs in through AgriSystem, then opens the Dashboard
	 */
	private void onLogin() {
		
		String username = usernameField.getText().trim();
		String password = passwordField.getText();
		
		// ERROR: text fields cannot be empty
		if (username.isEmpty() || password.isEmpty()) {
			errorLabel.setText("Please enter your username and password.");
			return;
		} // end of if statement
		
		User user = system.login(username, password);
		if (user == null) { // if user not found in users list
			errorLabel.setText("Wrong username or password.");
			passwordField.clear();
			return;
		} // end of if statement
		
		// same scene, new root (keeps the window size)
		if (user.getRole().equals(Role.MANAGER)) {
			stage.getScene().setRoot(new ManagerDashboard((Manager) user, system.getInventoryService(), system.getUsers()));
		} else {
			stage.getScene().setRoot(new ClerkDashboard((Clerk) user, system.getInventoryService()));
		} // end of if else statement
		
		stage.setTitle("Aura Farm - Dashboard");
		
	} // end of onLogin
	
	/*
	 * showUsersPopup()
	 * 	- only meant for presentation
	 * 	- pop up with a table of every current user's username and password
	 */
	private void showUsersPopup() {
		
		TableView<User> table = new TableView<>(FXCollections.observableArrayList(system.getUsers()));
		
		TableColumn<User, String> usernameCol = new TableColumn<>("Username");
		usernameCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getUsername()));
		usernameCol.prefWidthProperty().bind(table.widthProperty().multiply(0.48));
		
		TableColumn<User, String> passwordCol = new TableColumn<>("Password");
		passwordCol.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getPassword()));
		passwordCol.prefWidthProperty().bind(table.widthProperty().multiply(0.48));
		
		table.getColumns().add(usernameCol);
		table.getColumns().add(passwordCol);
		
		Stage popup = new Stage();
		
		VBox root = new VBox(12, table);
		root.setPadding(new Insets(20));
		root.setAlignment(Pos.CENTER_LEFT);
		VBox.setVgrow(table, Priority.ALWAYS);
		
		popup.initOwner(stage);
		popup.initModality(Modality.APPLICATION_MODAL);
		popup.setTitle("Current Users");
		Scene popupScene = new Scene(root, 420, 380);
		Theme.apply(popupScene);
		popup.setScene(popupScene);
		popup.showAndWait();
		
	} // end of showUsersPopup
	
} // end of LoginScreen class
