package Engine.View;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.io.*;
import java.util.*;

public class View extends Observable {
    @FXML
    public javafx.scene.control.TextField corpus_txt_field ;
    public javafx.scene.control.TextField posting_txt_field;
    public javafx.scene.control.CheckBox check_stemming;
    public javafx.scene.control.ChoiceBox lang_list;
    public javafx.scene.control.Button show_dic_btn;
    public javafx.scene.control.Button load_dic_btn;
    public javafx.scene.control.Button reset_btn;
    public javafx.scene.control.TextArea txtArea_dictionary;
    public javafx.scene.control.Button btn_test;
    public javafx.scene.control.ScrollPane pane_for_cities;

    public javafx.scene.control.cell.CheckBoxListCell<String> cell_choice;
    @FXML

    private Scene scene;
    private Stage parent;



    public void browseCorpus() {

        JFileChooser fc = new JFileChooser() ;
        fc.setCurrentDirectory(new File(System.getProperty("user.home")));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false );
        int result = fc.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            corpus_txt_field.clear();
            File selectedFile = fc.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            corpus_txt_field.appendText(selectedFile.getAbsolutePath());
        }
    }

    public void browsePosting() {
        JFileChooser fc = new JFileChooser() ;
        fc.setCurrentDirectory(new File(System.getProperty("user.home")));
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setAcceptAllFileFilterUsed(false );
        int result = fc.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            posting_txt_field.clear();
            File selectedFile = fc.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
            posting_txt_field.appendText(selectedFile.getAbsolutePath());
        }
    }

    public void  run_btn_pressed () {
        //System.out.println("pressed");
        if ( corpus_txt_field.getText().isEmpty() || posting_txt_field.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "One or  more Paths is missing", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if ( !checkPaths() ) { // paths are not valid
            JOptionPane.showMessageDialog(null, "Paths are Invalid", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

            setChanged();
            notifyObservers("run");
            load_dic_btn.setDisable(false );
            show_dic_btn.setDisable(false );
            reset_btn.setDisable(false);
            lang_list.setDisable(false);


    }

    public void setScene(Scene scene) {
        this.scene = scene ;
    }

    public void setParent(Stage primaryStage) {
        this.parent = primaryStage ;
    }

    public void updateLangLIst(String[] list_lang) {
        ArrayList<String> lang = new ArrayList<String>(Arrays.asList(list_lang));
        ObservableList<String> list = FXCollections.observableArrayList(lang);
        lang_list.setItems(list);
    }


    public  void show_dic_pressed() throws Exception{
        String postingPath = posting_txt_field.getText();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("dic_view.fxml"));
        Parent root = fxmlLoader.load();
        Stage secondryStage = new Stage();
        secondryStage.setTitle("Dic View");
        Scene scene = new Scene(root, 600, 600) ;
        secondryStage.setScene(scene);
        txtArea_dictionary = (javafx.scene.control.TextArea) scene.lookup("#txtArea_dictionary");
        try {
            txtArea_dictionary.setText(getDicDisplay(postingPath));
        }
        catch(IOException ioe){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("File not found");
            alert.setContentText("Please check your posting path and try again");
            alert.showAndWait();
            return;
        }
        //txtArea_dictionary.setText(getDicDisplay("C:\\Users\\Nadav\\Desktop\\Engine Project\\resources"));
        secondryStage.show();
    }

    private String getDicDisplay(String text) throws IOException {
        StringBuilder sb = new StringBuilder();
            BufferedReader br_dic = new BufferedReader(new FileReader(text + "\\Postings\\termDictionary.txt"));
            String line = null;
            while ((line = br_dic.readLine()) != null){
                String term = "";
                String tf = "";
                String[] splited = StringUtils.split(line,",");
               // String[] termSplited = StringUtils.split(splited[0], "<D>");

                if (splited.length < 1)
                    continue;
                term =splited[0];
                if (splited.length > 4){
                    tf = splited[splited.length-3];
                }
                sb.append(term).append(",").append(tf).append("\n");
            }
        return sb.toString();
    }

    public void reset_btn_pressed() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset");
        alert.setHeaderText("Are you sure ? ");
        alert.setContentText (" This will reset all the posting files and Dictionaries that saved . ");
        Optional<ButtonType> option = alert.showAndWait();
        if ( ButtonType.OK.equals(option.get())){
            setChanged();
            notifyObservers("reset");
            load_dic_btn.setDisable(true );
            show_dic_btn.setDisable(true );
            reset_btn.setDisable (true);
            lang_list.setDisable(true);
        }else {

        }


    }

    public void updatePaths(){
        setChanged();
        notifyObservers("update_path");
        if ( posting_txt_field.getText().length() > 0  ) {
            load_dic_btn.setDisable(false );
            show_dic_btn.setDisable(false );
            reset_btn.setDisable (false);
            lang_list.setDisable(false);
        }else {
            load_dic_btn.setDisable(true );
            show_dic_btn.setDisable(true );
            reset_btn.setDisable (true);
            lang_list.setDisable(true);
        }


    }

    public void load_dic_mem(){
        setChanged();
        notifyObservers("load_to_memory");
    }

    public boolean checkPaths ( ) {
        String corpus = corpus_txt_field.getText();
        String posting = posting_txt_field.getText();
        File dir = new File(corpus);
        File dir2 = new File(posting);
        if (dir != null && dir.exists() && dir2 != null && dir2.exists()) {
            return true;
        } else return false;
    }

    public void printTests(ActionEvent actionEvent) {
        setChanged();
        notifyObservers("showTests");
    }

    public void setCitiesFilter() {
       //pane_for_cities.se


    }
}
