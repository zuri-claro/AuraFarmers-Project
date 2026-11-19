package dashboard;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import app.AgriSystem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import model.Role;
import model.User;
import ui.LoginScreen;

/*
 * Dashboard class
 * - Parent class for ManagerDashboard and ClerkDashboard
 * - Contains reusable dashboard components
 */

public class Dashboard extends BorderPane {
	
	// COLORS
	public static final String DARKGREEN = "#2E8B57";
	public static final String GREEN = "66BB6A";
	public static final String LIGHTGREEN = "C8E6C9";
	public static final String ORANGE = "#FF8A65";
	public static final String PEACH = "#FFCCBC";
	
	public static final String GRAY = "#D9D9D9";
	
	public static final String NAV_IDLE = "#2DA562"; // header button, not selected
	public static final String NAV_ACTIVE = "#66BB6A"; // header button / tab, selected
	public static final String NAV_TEXT = "#7DE3A3"; 
	
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
    
    // Creates the header used by the dashboards
    protected VBox createHeader(String title, String subtitle) {

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 26px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: white;"
        );

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #eeeeee;"
        );

        VBox header = new VBox(5, titleLabel, subtitleLabel);
        header.setPadding(new Insets(20));
        header.setStyle("-fx-background-color: #542020;");

        return header;
    }

    // Creates a card for displaying dashboard statistics
    protected VBox createStatCard(String title, Label value) {

        Label titleLabel = new Label(title);
        titleLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #666666;"
        );

        value.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #542020;"
        );

        VBox card = new VBox(10, titleLabel, value);
        card.setPadding(new Insets(15));
        card.setPrefWidth(190);
        card.setAlignment(Pos.CENTER_LEFT);

        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #dddddd;" +
            "-fx-border-radius: 6px;" +
            "-fx-background-radius: 6px;"
        );

        return card;
    }

    // Places statistic cards in one row
    protected HBox createStatRow(VBox... cards) {

        HBox row = new HBox(15, cards);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

} // end of Dashboard class
