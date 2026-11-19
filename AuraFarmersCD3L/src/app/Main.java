package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ui.LoginScreen;

public class Main extends Application{
	
	@Override
	public void start(Stage stage) {
		
	    AgriSystem system = AgriSystem.getInstance();

	    stage.setTitle("Aura Farm");
	    stage.setScene(new Scene(new LoginScreen(system, stage), 1100, 700));
	    stage.setMinWidth(1000);
	    stage.setMinHeight(650);
	    stage.show();
	    
	} // end of start
	
    public static void main(String[] args) {
    	
        launch(args);
        
    } // end of main
	
} // end of Main class
