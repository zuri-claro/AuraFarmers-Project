package ui;

import java.util.function.Supplier;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/*
 * FormDialog class
 * 	- reusable pop-up form that looks like the dialogs in the Figma design
 * 		(grey background, white card, green title, rounded inputs, Cancel + Save buttons)
 * 	- used by Add Product, Edit Product, Restock, Stock Adjustment, and Set Low-Stock Threshold
 * 	- if saving fails, the error message is shown INSIDE the dialog and the dialog stays open,
 * 		so the user does not lose what they typed
 */

public class FormDialog {
	
	// ATTRIBUTES
	private final Stage stage = new Stage();
	private final VBox fields = new VBox(12);
	private final Label errorLabel = new Label();
	private final Button saveButton; // the orange button on the right
	private boolean saved = false;
	private Supplier<String> onSave;
	
	// METHODS
	/*
	 * FormDialog(String, String)
	 * 	- constructor method
	 * 	- title is shown at the top of the card, saveText is the text of the orange button
	 */
	public FormDialog(String title, String saveText) {
		
		Label titleLabel = new Label(title);
		titleLabel.getStyleClass().add("form-title");
		titleLabel.setMaxWidth(Double.MAX_VALUE);
		titleLabel.setAlignment(Pos.CENTER);
		
		// hidden until there is something to say
		errorLabel.getStyleClass().add("form-error");
		errorLabel.setWrapText(true);
		errorLabel.setVisible(false);
		errorLabel.setManaged(false);
		
		
		Button cancelButton = new Button("Cancel");
		cancelButton.getStyleClass().add("secondary-button");
		cancelButton.setCancelButton(true); // Esc closes the dialog
		cancelButton.setOnAction(e -> stage.close());
		
		saveButton = new Button(saveText);
		saveButton.setDefaultButton(true); // Enter saves
		saveButton.setOnAction(e -> save());
		
		HBox buttons = new HBox(14, cancelButton, saveButton);
		buttons.setAlignment(Pos.CENTER);
		
		VBox card = new VBox(14, titleLabel, fields, errorLabel, buttons);
		card.getStyleClass().add("white-card");
		card.setPadding(new Insets(26, 30, 26, 30));
		
		VBox root = new VBox(card);
		root.getStyleClass().add("form-dialog");
		root.setPadding(new Insets(18));
		root.setPrefWidth(620);
		
		Scene scene = new Scene(root);
		Theme.apply(scene);
		
		stage.setScene(scene);
		stage.setTitle(title);
		stage.initModality(Modality.APPLICATION_MODAL); // the window behind cannot be used until this closes
		stage.setResizable(false);
		
	} // end of constructor method
	
	/*
	 * addField(String, Node)
	 * 	- adds one full-width field: label on top, input below
	 */
	public FormDialog addField(String label, Node input) {
		
		fields.getChildren().add(labeled(label, input, null));
		return this;
		
	} // end of addField
	
	/*
	 * addRow(VBox, VBox)
	 * 	- adds two fields side by side with equal widths (build each with labeled(...))
	 */
	public FormDialog addRow(VBox left, VBox right) {
		
		left.setPrefWidth(10);
		right.setPrefWidth(10);
		HBox row = new HBox(16, left, right);
		HBox.setHgrow(left, Priority.ALWAYS);
		HBox.setHgrow(right, Priority.ALWAYS);
		fields.getChildren().add(row);
		return this;
		
	} // end of addRow
	
	/*
	 * addNote(String)
	 * 	- adds a small grey line of text (e.g. which product is being restocked)
	 */
	public FormDialog addNote(String text) {
		
		Label note = new Label(text);
		note.getStyleClass().add("form-note");
		note.setWrapText(true);
		fields.getChildren().add(note);
		return this;
		
	} // end of addNote
	
	/*
	 * addMessage(String)
	 * 	- adds a normal line of text (used by the Yes/No confirmation pop-ups, see ConfirmDialog)
	 */
	public FormDialog addMessage(String text) {
		
		Label message = new Label(text);
		message.getStyleClass().add("form-message");
		message.setWrapText(true);
		fields.getChildren().add(message);
		return this;
		
	} // end of addMessage
	
	/*
	 * markDestructive()
	 * 	- turns the main button red, for actions that cannot be undone (e.g. Void, Deactivate)
	 */
	public FormDialog markDestructive() {
		
		saveButton.getStyleClass().add("destructive-button");
		return this;
		
	} // end of markDestructive
	
	/*
	 * labeled(String, Node, String)
	 * 	- label on top, input below, and an optional small hint under the input (pass null for no hint)
	 */
	public static VBox labeled(String label, Node input, String hint) {
		
		Label labelNode = new Label(label);
		labelNode.getStyleClass().add("form-label");
		
		if (input instanceof Region) {
			((Region) input).setMaxWidth(Double.MAX_VALUE); // fill the whole column
		} // end of if statement
		
		VBox box = new VBox(5, labelNode, input);
		
		if (hint != null) {
			Label hintLabel = new Label(hint);
			hintLabel.getStyleClass().add("form-hint");
			hintLabel.setWrapText(true);
			box.getChildren().add(hintLabel);
		} // end of if statement
		
		return box;
		
	} // end of labeled
	
	/*
	 * showAndWait(Supplier<String>)
	 * 	- opens the dialog and waits until it is closed
	 * 	- onSave runs when Save is clicked: return null if everything worked (the dialog closes),
	 * 		or the error message to show inside the dialog (it stays open)
	 * 	- returns true if the form was saved, false if it was cancelled
	 */
	public boolean showAndWait(Supplier<String> onSave) {
		
		this.onSave = onSave;
		this.saved = false;
		stage.showAndWait();
		return saved;
		
	} // end of showAndWait
	
	/*
	 * save()
	 * 	- runs the save action and either closes the dialog or shows the error
	 */
	private void save() {
		
		String error = onSave == null ? null : onSave.get();
		
		if (error == null) {
			saved = true;
			stage.close();
			return;
		} // end of if statement
		
		errorLabel.setText(error);
		errorLabel.setVisible(true);
		errorLabel.setManaged(true);
		stage.sizeToScene(); // grow a little so the message fits
		
	} // end of save
	
} // end of FormDialog class
