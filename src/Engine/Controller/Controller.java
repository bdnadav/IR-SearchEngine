package Engine.Controller;

import Engine.Model.Model;
import Engine.View.View;
import org.apache.commons.lang3.StringUtils;

import java.util.Observable;
import java.util.Observer;

public class Controller extends Observable implements Observer {
    private Model  model;
    public View view ;
    @Override
    public void update(Observable o, Object arg) {

        if ( o == view) {
            switch ((String) arg) {
                case "run":
                    /* The next two lines in comment only for test 8/12/18 10:45*/
                    model.run(view.corpus_txt_field.getText(), view.posting_txt_field.getText(), view.check_stemming.isSelected());
                    break;
                case "show_dic":
                    model.showDic();
                    break;
                case "load_to_memory":
                    String stemming = "";
                    if ( view.check_stemming.isSelected())
                        stemming = "Stemming" ;
                    boolean success = model.loadDicToMemory(stemming);
                    if (success)
                    view.enableSearchBtns();
                    view.setCitiesView(model.getCitiesView());
                    break;
                case "reset":
                    model.resetAll();
                    break;
                case "update_path":
                    model.pathUpdate(view.corpus_txt_field.getText(), view.posting_txt_field.getText(), view.check_stemming.isSelected());
                    break;
                case "showTests":
                    model.printTests();
                    String queriesResults = model.getQueriesResults();
                    view.showQueriesResults(queriesResults);
                    break;
                case "showEntities":
                    String docNo = view.selectedDocNo;
                    if (docNo.charAt(0) == ' ')
                        docNo = StringUtils.substring(docNo, 1);
                    String entities = model.getEntities(docNo);
                    view.showEntities(entities);
                    break;
                case "saveResultsBtnPushed":
                    String results = model.getResultsTrecFormat();
                    view.saveResults(results);
                    break;
                case "file_search_query":
                    String file_path_query = view.query_file_path.getText().toString();
                    model.readQueryFromFile(file_path_query  ,view.filter_city_view.getItems() , view.check_semmantics.isSelected());
                    view.showQueriesResults(model.getQueriesResults());
                    break;
                case "search_query":
                    String query = view.query_path_txtfield.getText().toString();
                    model.handleSingleQuery(query , view.filter_city_view.getItems() , view.check_semmantics.isSelected()) ;
                    view.showQueriesResults(model.getQueriesResults());
                    break;
                default:
                    System.out.println("no match");
            }
        }
        if ( o == model) {
            switch ((String) arg) {
                case "finished":
                    view.updateLangLIst(model.list_lang);
                    break;
            }
        }
    }




    public void setVM(View view, Model model) {
        this.model = model;
        this.view = view ;
    }
}
