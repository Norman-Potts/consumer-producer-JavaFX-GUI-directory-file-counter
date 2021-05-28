 
package guidirectoryfilecounter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Class Assignment5
 *
 *       Creates a GuI and looks threw a specifed directory for a given file.
 * 
 * @author Norman
 */
public class Guidirectoryfilecounter extends Application 
{    
    @Override
    public void start(Stage stage) throws Exception 
    {
        Parent root = FXMLLoader.load(getClass().getResource("FXMLDocument.fxml"));        
        Scene scene = new Scene(root);        
        stage.setScene(scene);
        stage.sizeToScene();
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());                
    }/// End of method start.       
    /** Method main
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }    
}/// End of class Guidirectoryfilecounter
