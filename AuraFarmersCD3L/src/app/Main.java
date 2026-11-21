package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.LoginScreen;
import ui.Theme;

public class Main extends Application{
	
	@Override
	public void start(Stage stage) {
		
	    AgriSystem system = AgriSystem.getInstance();

	    Scene scene = new Scene(new LoginScreen(system, stage), 1100, 700);
	    Theme.apply(scene); // the Aura Farm look (aura.css), used by every screen in this window

	    stage.setTitle("Aura Farm");
	    stage.setScene(scene);
	    stage.setMinWidth(1000);
	    stage.setMinHeight(650);
	    stage.show();
	    
	} // end of start
	
    public static void main(String[] args) {
    	
        launch(args);
        
    } // end of main
	
} // end of Main class