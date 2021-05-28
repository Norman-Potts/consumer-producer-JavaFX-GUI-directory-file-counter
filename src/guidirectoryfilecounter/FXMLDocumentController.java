 
package guidirectoryfilecounter;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/** Class FXMLDocumentController
 *
 * @author Norman
 */
public class FXMLDocumentController implements Initializable
{
    
    @FXML
    private TextField input_dir;
    @FXML
    private TextField input_file;
    @FXML
    private TextArea txtArea;
    @FXML
    private Label lbl_countOfFilesFound;
    
    
    
    /** Method Search
     *      Runs when the search button gets clicked. Uses the ProducerConsumer
     *      to search the directory for files that have the same name as the 
     *      given file.
     * 
     * @param event
     * @throws InterruptedException 
     */
    @FXML
    private void Search(ActionEvent event) throws InterruptedException 
    {
        String directory ="";///../MyDirectories
        String filename = "";
        directory = input_dir.getText();
        filename = input_file.getText();                            
        ProducerConsumer pc = new ProducerConsumer(directory, filename);        
        lbl_countOfFilesFound.setText(""+pc.getCountofMatches());
        ArrayList<String> list = pc.getListOfMatches();
        String text = "";
        for(int i = 0 ; i < list.size(); i++ )
        {
            text += ""+list.get(i)+"\n";
        }        
        txtArea.setText(text);
        
    }/// End of method Search
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) 
    {
        
    }    
    
}/// End of class FCMLDocumentontroller,
