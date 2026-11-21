package dashboard;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.AgriSystem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ui.LoginScreen;
import ui.Theme;

/*
 * Dashboard class
 * - Parent class for ManagerDashboard and ClerkDashboard
 * - Contains reusable dashboard components
 * - top part is the navigation bar (buttons on top), center part is whichever screen is open
 */

public class Dashboard extends BorderPane {
	
	// COLORS
	public static final String DARKGREEN = "#2E8B57";
	public static final String GREEN = "#66BB6A";
	public static final String LIGHTGREEN = "#C8E6C9";
	public static final String ORANGE = "#FF8A65";
	public static final String PEACH = "#FFCCBC";
	
	public static final String GRAY = "#D9D9D9";
	
	public static final String NAV_IDLE = "#2DA562"; // header button, not selected
	public static final String NAV_ACTIVE = "#66BB6A"; // header button / tab, selected
	public static final String NAV_TEXT = "#7DE3A3"; 
	
	// ATTRIBUTES
	private final List<Button> navButtons = new ArrayList<>(); // buttons on top, used to show which screen is open
	
	// runs for every dashboard: gives it the green background from aura.css
	{
		getStyleClass().add("dashboard");
	}
    
	/*
	 * createHeader(String, String)
	 * 	- creates the title and subtitle used by the dashboards
	 */
    protected VBox createHeader(String title, String subtitle) {

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("welcome-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("welcome-subtitle");

        VBox header = new VBox(5, titleLabel, subtitleLabel);
        header.setPadding(new Insets(10));
        return header;
        
    } // end of createHeader

    /*
     * createStatCard(String, Label)
     * 	- creates a card for displaying one dashboard statistic (title on the left, value on the right)
     */
    protected VBox createStatCard(String title, Label value) {

        Label titleLabel = new Label(title);
        
        Region space = new Region();
        HBox.setHgrow(space, Priority.ALWAYS);
        
        HBox line = new HBox(10, titleLabel, space, value);
        line.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(line);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(260);
        return card;
        
    } // end of createStatCard

    /*
     * createStatRow(VBox...)
     * 	- places statistic cards in one row
     */
    protected HBox createStatRow(VBox... cards) {

        HBox row = new HBox(15, cards);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
        
    } // end of createStatRow
    
    /*
     * createStatColumn(VBox...)
     * 	- places statistic cards in one column (used by the Overview and Summary boxes)
     */
    protected VBox createStatColumn(VBox... cards) {
    	
    	VBox column = new VBox(5, cards);
    	return column;
    	
    } // end of createStatColumn
    
    /*
     * createNavBar(String...)
     * 	- creates the buttons on top (e.g. Dashboard, Inventory, Transactions) and a Log Out button
     * 	- returns the buttons in the same order as the names, so the subclass can set what each one does
     */
    protected List<Button> createNavBar(String... names) {
    	
    	HBox bar = new HBox(12);
    	bar.getStyleClass().add("nav-bar");
    	bar.setPadding(new Insets(10, 24, 10, 24));
    	bar.setAlignment(Pos.CENTER_LEFT);
    	
    	// the Aura logo on the left (skipped if the image file is missing)
    	ImageView logo = Theme.logo(46);
    	if (logo != null) {
    		bar.getChildren().add(logo);
    	} // end of if statement
    	
    	navButtons.clear();
    	for (String name : names) {
    		Button button = new Button(name);
    		button.getStyleClass().add("nav-button");
    		navButtons.add(button);
    		bar.getChildren().add(button);
    	} // end of for loop
    	
    	// pushes the Log Out button to the right
    	Region space = new Region();
    	HBox.setHgrow(space, Priority.ALWAYS);
    	
    	Button logoutButton = new Button("Log Out");
    	logoutButton.getStyleClass().addAll("nav-button", "nav-logout");
    	logoutButton.setOnAction(e -> logout());
    	
    	bar.getChildren().addAll(space, logoutButton);
    	
    	// the profile picture on the far right (skipped if the image file is missing)
    	ImageView avatar = Theme.avatar(46);
    	if (avatar != null) {
    		bar.getChildren().add(avatar);
    	} // end of if statement
    	
    	setTop(bar);
    	
    	return new ArrayList<>(navButtons);
    	
    } // end of createNavBar
    
    /*
     * showScreen(Button, Node)
     * 	- puts the screen in the center of the dashboard
     * 	- disables the button of the open screen so the user can see which one is selected
     */
    protected void showScreen(Button selected, Node screen) {
    	
    	for (Button button : navButtons) {
    		button.setDisable(button == selected);
    	} // end of for loop
    	
    	// most screens sit on a grey card like in the Figma design;
    	// a screen that builds its own cards (e.g. the dashboards' home pages) adds the style class "no-card" to skip this
    	Node content = screen;
    	if (!screen.getStyleClass().contains("no-card")) {
    		VBox card = new VBox(screen);
    		VBox.setVgrow(screen, Priority.ALWAYS);
    		card.getStyleClass().add("content-card");
    		content = card;
    	} // end of if statement
    	
    	BorderPane.setMargin(content, new Insets(0, 24, 24, 24));
    	setCenter(content);
    	
    } // end of showScreen
    
    /*
     * logout()
     * 	- goes back to the login screen (same window, new root)
     */
    protected void logout() {
    	
    	Stage stage = (Stage) getScene().getWindow();
    	stage.getScene().setRoot(new LoginScreen(AgriSystem.getInstance(), stage));
    	stage.setTitle("Aura Farm");
    	
    } // end of logout
    
    /*
     * formatPeso(double)
     * 	- returns the amount as "₱100.00"
     * 	- static so the screens in the ui package can use it too
     */
    public static String formatPeso(double amount) {
    	return String.format("₱%.2f", amount);
    } // end of formatPeso
    
    /*
     * showError(String)
     * 	- pop up for error messages
     */
    public static void showError(String message) {
    	
    	Alert alert = new Alert(Alert.AlertType.ERROR);
    	alert.setHeaderText(null);
    	alert.setContentText(message);
    	Theme.apply(alert.getDialogPane());
    	alert.showAndWait();
    	
    } // end of showError
    
    /*
     * showInfo(String)
     * 	- pop up for success messages
     */
    public static void showInfo(String message) {
    	
    	Alert alert = new Alert(Alert.AlertType.INFORMATION);
    	alert.setHeaderText(null);
    	alert.setContentText(message);
    	Theme.apply(alert.getDialogPane());
    	alert.showAndWait();
    	
    } // end of showInfo
    
    /*
     * confirm(String)
     * 	- yes/no pop up, returns true if the user clicked OK
     */
    public static boolean confirm(String message) {
    	
    	Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    	alert.setHeaderText(null);
    	alert.setContentText(message);
    	Theme.apply(alert.getDialogPane());
    	Optional<ButtonType> result = alert.showAndWait();
    	return result.isPresent() && result.get() == ButtonType.OK;
    	
    } // end of confirm
    
    /*
     * ask(String, String, String, String)
     * 	- gets user input from a text pop up
     * 	- returns null if the user cancels
     */
    public static String ask(String title, String header, String label, String defaultValue) {
    	
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText(label);
        Theme.apply(dialog.getDialogPane());
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            return result.get();
        } // end of if statement
        return null;
        
    } // end of ask

} // end of Dashboard class