package ui;

import java.io.InputStream;

import app.AgriSystem;
import dashboard.ClerkDashboard;
import dashboard.Dashboard;
import dashboard.ManagerDashboard;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.Clerk;
import model.Manager;
import model.Role;
import model.User;

/*
 * LoginScreen class
 * 	- first screen to pop up
 * 	- "Forgot Password?" opens a pop up listing every current user's username and password
 * 	- a successful login swaps the scene's root to the Dashboard
 */

public class LoginScreen extends StackPane {
	
	// COLORS - to move to dashboard
	public static final String DARKGREEN = "#2E8B57";
	public static final String GREEN = "66BB6A";
	public static final String LIGHTGREEN = "C8E6C9";
	public static final String ORANGE = "#FF8A65";
	public static final String PEACH = "#FFCCBC";
	
	public static final String GRAY = "#D9D9D9";
	
	// ATTRIBUTES
	private final AgriSystem system;
	private final Stage stage;
	
	private final TextField usernameField = new TextField();
	private final PasswordField passwordField = new PasswordField();
	private final Label errorLabel = new Label();
	
	private static final String FIELD_STYLE =
			"-fx-background-color: transparent;" +
			"-fx-background-radius: 30;" +
			"-fx-border-color: #A9A9A9;" +
			"-fx-border-width: 2;" +
			"-fx-border-radius: 30;" +
			"-fx-padding: 10 20;" +
			"-fx-font-size: 16px;" +
			"-fx-prompt-text-fill: #9A9A9A;" +
			"-fx-focus-color: transparent;" +
			"-fx-faint-focus-color: transparent;";
	
	// METHODS
	/*
	 * LoginScreen(AgriSystem, Stage)
	 * 	- constructor method
	 */
	public LoginScreen(AgriSystem system, Stage stage) {
		
		this.system = system;
		this.stage = stage;
		
		setStyle("-fx-background-color: " + DARKGREEN + ";");
		setPadding(new Insets(40));
		getChildren().add(buildCard());
		
	} // end of constructor method
	
	/*
	 * buildCard()
	 * 	- the gray rounded card: logo on the left, form on the right
	 */
	private HBox buildCard() {
		
		StackPane left = new StackPane(createLogo(250));
		left.setPrefWidth(400);
		
		VBox right = buildForm();
		right.setPrefWidth(340);
		
		HBox card = new HBox(20, left, right);
		card.setAlignment(Pos.CENTER);
		card.setPadding(new Insets(50));
		card.setMaxSize(860, 460);
		card.setStyle("-fx-background-color: " + GRAY + ";" + "-fx-background-radius: 20;");
		card.setEffect(new DropShadow(25, Color.rgb(0, 0, 0, 0.3)));
		return card;
		
	} // end of buildCard
	
	/*
	 * buildForm()
	 * 	- welcome text, username and password fields, forgot password, sign in button
	 */
	private VBox buildForm() {
		
		Label welcome = new Label("Welcome!");
		welcome.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: " + DARKGREEN + ";");
		
		Label subtitle = new Label("Sign in to your Account");
		subtitle.setStyle("-fx-font-size: 18px; -fx-text-fill: #4CAF50;");
		
		usernameField.setPromptText("Username");
		usernameField.setStyle(FIELD_STYLE);
		usernameField.setMaxWidth(320);
		usernameField.setOnAction(e -> passwordField.requestFocus()); // enter moves to password
		
		passwordField.setPromptText("Password");
		passwordField.setStyle(FIELD_STYLE);
		passwordField.setMaxWidth(320);
		passwordField.setOnAction(e -> onLogin()); // enter signs in
		
		Label forgot = new Label("Forgot Password?");
		forgot.setStyle("-fx-font-size: 17px; -fx-text-fill: " + DARKGREEN + "; -fx-cursor: hand;");
		forgot.setOnMouseClicked(e -> showUsersPopup());
		
		Button signIn = new Button("SIGN IN");
		signIn.setStyle(
				"-fx-background-color: " + ORANGE + ";" +
				"-fx-text-fill: white;" +
				"-fx-font-size: 17px;" +
				"-fx-font-weight: bold;" +
				"-fx-background-radius: 30;" +
				"-fx-padding: 10 55;" +
				"-fx-cursor: hand;");
		signIn.setOnAction(e -> onLogin());
		
		errorLabel.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 14px;");
		errorLabel.setWrapText(true);
		
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
		}
		
		stage.setTitle("Aura Farm - Dashboard");
		
	} // end of onLogin
	
	/*
	 * showUsersPopup()
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
		root.setStyle("-fx-background-color: " + GRAY + ";");
		
		popup.initOwner(stage);
		popup.initModality(Modality.APPLICATION_MODAL);
		popup.setTitle("Current Users");
		popup.setScene(new Scene(root, 420, 380));
		popup.showAndWait();
		
	} // end of showUsersPopup
	
	/*
	 * createLogo(double)
	 * 	- loads /resources/logo_name.png
	 * 	- size is the height of the image; the width follows the image's proportions
	 */
	public static Node createLogo(double size) {
		
		Image image = null;
		
		try {
			InputStream stream = Dashboard.class.getResourceAsStream("/resources/logo_name.png");
			if (stream != null) {
				image = new Image(stream);
			} // end of if statement
		} catch (Exception e) {
			image = null;
		} // end of try catch
		
		// image found: just show it
		if (image != null && !image.isError()) {
			ImageView view = new ImageView(image);
			view.setFitHeight(size);
			view.setPreserveRatio(true);
			view.setSmooth(true);
			return view;
		} // end of if statement
		
		return null;
		
	} // end of createLogo
	
} // end of LoginScreen class