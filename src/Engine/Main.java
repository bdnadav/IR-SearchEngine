package Engine;
import Engine.Controller.Controller;
import Engine.Model.*;
import Engine.View.View;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;

import javax.print.Doc;
import java.util.HashMap;
import java.util.List;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("View/viewf.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("SearchEngine Builder");
        Scene scene = new Scene(root, 600, 600) ;
        primaryStage.setScene(scene);
        Controller controller = new Controller();
        //view notify controller
        View view = fxmlLoader.getController();
        view.posting_txt_field.appendText( "C:\\Users\\Nadav\\Desktop\\Engine Project\\resources-10files" );
        view.corpus_txt_field.appendText("C:\\Users\\Nadav\\Desktop\\Engine Project\\corpus\\corpus-10files");
        view.setParent(primaryStage) ;
        view.setScene(scene) ;
        view.addObserver(controller);
        Model model = new Model() ;
        model.addObserver(controller);
        controller.setVM(view , model);

        primaryStage.show();
      //  String str = "isn't it great to parse in Java-Parser KOKO - STR U.S.A  , od don't it ? ";
        String str1 = "$450,000 " +
//                " The BLA BLA June 16" +
                " 16 June " +
                "betrothal of her daughter, Ashley Cummins Brittingham," +
                " 0.6 percent " +
                "BLA BLA " +
                "10.6 percentage " +
                "BLA BLA " +
                "10,123 " +
                "55 Million " +
                "55 Billion " +
                "1010.56" +
                " 6%" +
                " 1,000,000 Dollars" +
                " 1.7320 Dollars" +
                " $450,000,000" +
                " 7 Trillion" +
                " 10,123,000,000" +
                " $100 billion" +
                " $100 million" +
                " MAY 1994" +
                " JUNE 4" +
                " 1 trillion U.S. dollars" +
                " 22 3/4 Dollars" +
                " $450,000" +
                " 35 3/4" +
                " 35.66" +
                " 100bn Dollars" +
                " 20.6m Dollars" +
                " $1.732 million"+
                " Between 18 and 24"+
                " 15-15"+
                " pop-pop-pop" +
                " 10-part" +
                " \"one way or another\"" +
                "one.'yesterday" +
                "<P>\"grandmother's" +
                "<>";
        String str2 = "<P>BLA BLA BLA";


//        // move to TExt operationg !!!
//       Parse parser = new Parse(null , "D:\\\\documents\\\\users\\\\harelsa\\\\Downloads\\\\test");
//        Document doc = new Document( "FBIS3-50" , "C:\\Users\\harel_000\\Desktop\\Retrival\\corpus\\corpus\\FB396002\\FB3960020", " JERUSALEM" , "ENGLISH") ;
//       parser.parse(str1 , doc );
//        Term term = new Term(0,0, "Nadav");
//        SegmentFilePartition sfp = new SegmentFilePartition("d:\\documents\\users\\bardanad\\Downloads\\Segment Files", 'a', 'd');
//        sfp.signNewTerm(term, doc);
//        sfp.extractTermFromSegmentFilePartition();


//        Document doc = new Document("NF-70", "d:\\documents\\users\\bardanad\\Downloads\\");
//        HashMap<String, Term> allTerms = new HashMap<>();
//        Term term = new Term(0,0, "Aba");
//        Term term1 = new Term(0,0, "baba");
//        Term term2 = new Term(0,0, "Home");
//        Term term3 = new Term(0,0, "Zzz");
//        Term term4 = new Term(0,0, "Yuli");
//        allTerms.put("Aba", term);
//        allTerms.put("baba", term1);
//        allTerms.put("Home", term2);
//        allTerms.put("Zzz", term3);
//        allTerms.put("Yuli", term4);
//        SegmentFile segmentFile = new SegmentFile();
//        segmentFile.sortDocTermsToSpecificPartition(allTerms, doc);



//        HashMap<String, Term> allTerms, Document currDoc

//        CorpusProcessingManager textOperationsManager = new CorpusProcessingManager("d:\\documents\\users\\bardanad\\Downloads\\corpus-split");
        //CorpusProcessingManager textOperationsManager = new CorpusProcessingManager("C:\\Users\\Nadav\\Desktop\\Engine Project\\corpus\\corpus");
        //CorpusProcessingManager textOperationsManager = new CorpusProcessingManager("D:\\documents\\users\\bardanad\\corpus\\test");
//        CorpusProcessingManager textOperationsManager = new CorpusProcessingManager("d:\\documents\\users\\harelsa\\Downloads\\corpus");
        //textOperationsManager.StartCorpusProcessing();
        //String[] list_lang = textOperationsManager.getDocLang() ;
        //System.out.println(list_lang.toString());
        //System.out.println("lalal");
        //textOperationsManager.p();
        // textOperationsManager.buildCitiesPosting();




        // Print words and Pos Tags
//        for (Tree leaf : leaves) {
//            Tree parent = leaf.parent(tree);
//            System.out.print( "'" + leaf.label().value() + "'");
//        }
//        System.out.println();

    }


    public static void main(String[] args) {
        launch(args);
    }
}
