package Engine.Model;

import javafx.collections.ObservableList;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;


import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Part of MVC Design pattern  , get called from controller after View events
 */
public class Model extends Observable {
    public static boolean debug = false;
    public static boolean status = true;
    private String corpusPath; //saved corpus path
    private String postingPath;// saved outpot posting path
    private boolean is_stemming; // using a stemmer on terms ot not
    private boolean useSemantics;
    public String[] list_lang; //list of lang returns from parsing the docs
    // will allow to load the term dic to prog memory -
    // will be used in project part 2
    TreeMap<String, String> termDictionary = new TreeMap<>();
    TreeMap<String, String> termDictionaryToShow = new TreeMap<>();
    TreeMap<String, Pair> citiesDictionary = new TreeMap<>();
    TreeMap<String, String> docsDictionary = new TreeMap<>();
    HashMap<String, String> headersDictionary = new HashMap<>();
    HashMap<String, String> docEntities = new HashMap<>();
    private ArrayList<String> citiesView;
    private double  AVL;



    /**
     * run corpus processing manager , get back info from posting process and
     * display it at the end ,
     * get lang lind and set it in gui
     *
     * @param corpusPath
     * @param postingPath
     * @param stemming
     */
    public void run(String corpusPath, String postingPath, boolean stemming) {
        long startTime = System.currentTimeMillis();
        this.corpusPath = corpusPath;
        //this.citiesView = new ArrayList();
        this.postingPath = postingPath;
        this.is_stemming = stemming;
        CorpusProcessingManager corpusProcessingManager = new CorpusProcessingManager(corpusPath, postingPath, stemming);
        corpusProcessingManager.StartCorpusProcessing();
        int uniqueTerms = Indexer.terms_dictionary.size();
        int docsGenerate = Indexer.docs_dictionary.size();
        Indexer.writeDictionariesToDisc();
        long estimatedTime = System.currentTimeMillis() - startTime;
        String summery = getSummary(estimatedTime, uniqueTerms, docsGenerate);
        list_lang = corpusProcessingManager.getDocLang();
        setChanged();
        notifyObservers("finished");
        JOptionPane.showMessageDialog(null, summery, "Build Info", JOptionPane.INFORMATION_MESSAGE);
        loadDicToMemory(stemming,postingPath);
    }

    /**
     * help to present a summary of run info in the end of posting
     *
     * @param estimatedTime - runtime
     * @param uniqueTerms   - num of unique terms in the corpus
     * @param docsGenerate  - how many docs in the corpus
     * @return
     */
    private String getSummary(long estimatedTime, int uniqueTerms, int docsGenerate) {
        long runTime = TimeUnit.MILLISECONDS.toSeconds(estimatedTime);
        String ans = uniqueTerms + " Unique Terms" + "\n";
        ans += docsGenerate + " Docs Indexed" + "\n";
        ans += "Total Runtime: " + runTime + "\n";
        return ans;
    }


    public void showDic() {
    }

    /**
     * load the dic file from disk to memory - insert to termDictionary
     *
     * @param postingPath
     * @param stemming
     */
    public boolean loadDicToMemory(boolean stemming, String postingPath) {
        //update
        this.postingPath = postingPath ;
        this.is_stemming = stemming ;
        String informationMsg = "";
        boolean ans = true;
        citiesView = new ArrayList<>();


        File dir = new File(postingPath + "\\Postings" + ifStemming());



        if (dir != null && dir.exists()) {
            ArrayList<String> list = new ArrayList<>();
            list_lang = new String[0];
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() +  "\\Languages"+  ".txt"));
                String line = "";

                while ((line = br_dic.readLine()) != null) {
                    list.add(line);
                }
                list_lang = list.toArray(new String[list.size()]);
                br_dic.close();
            } catch (Exception e) {
            }

           // JOptionPane.showMessageDialog(null, "Langs dictionary loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
        } else {
            ans = false ;
            JOptionPane.showMessageDialog(null, "Langs Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
        }
        if (dir != null && dir.exists()) {
            StringBuilder sb = new StringBuilder();
            this.cleanAllDictionaries(); // reset all
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\termDictionary.txt"));
                String line = "";
                while ((line = br_dic.readLine()) != null) {

                    String term = "";
                    String tf = "";
                    int indexOfFirstComma = StringUtils.indexOf(line, ",");
                    term = StringUtils.substring(line, 0, indexOfFirstComma);
                    String details = StringUtils.substring(line, indexOfFirstComma);
                    termDictionary.put(term, details);
                    String[] splitedDetails = StringUtils.split(details, ",");
                    tf = splitedDetails[2];
                    termDictionaryToShow.put(term, tf);
                }
                br_dic.close();
            } catch (Exception e) {
            }

            String line = null;

            informationMsg += "Term dictionary, ";
//            JOptionPane.showMessageDialog(null, "Term dictionary loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
            ans = false ;
            return false;
        }

        File cityDir = new File(postingPath + "\\Postings" + ifStemming());
        if (dir != null && cityDir.exists()) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\citiesDictionary.txt"));
                String line = "";
                while ((line = br_dic.readLine()) != null) {

                    String city = "";
                    String docsList = "";
                    String info = "";
                    int firstIndexOfComma = StringUtils.indexOf(line, ",");
                    int lastIndexOfComma = StringUtils.lastIndexOf(line, ",");
                    city = StringUtils.substring(line, 0, firstIndexOfComma); // city with details
                    info = StringUtils.substring(line, firstIndexOfComma + 1, lastIndexOfComma);
                    if (info.equals(","))
                        info = "NULL";
                    docsList = StringUtils.substring(line, lastIndexOfComma + 1);
                    citiesView.add(city) ;
                    citiesDictionary.put(city, new Pair<String, String>(info, docsList));
                }
                br_dic.close();
            } catch (Exception e) {
            }

            String line = null;


//            JOptionPane.showMessageDialog(null, "Cities dictionary loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
            informationMsg += "Cities dictionary, ";
        } else {
            ans = false ;
            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
            return false;

        }

        File docsDir = new File(postingPath + "\\Postings" + ifStemming());
        if (dir != null && docsDir.exists()) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\docDictionary.txt"));
                String line = "";
                /** AVL **/
                line = br_dic.readLine();
                String[] split = line.split("##");
                AVL = Double.parseDouble(split[0]);
                while ((line = br_dic.readLine()) != null) {

                    String docNumber = "";
                    String pointer = "";
                    int firstIndexOfComma = StringUtils.indexOf(line, ",");
                    docNumber = StringUtils.substring(line, 0, firstIndexOfComma);
                    pointer = StringUtils.substring(line, firstIndexOfComma + 1);

                    docsDictionary.put(docNumber, pointer);
                }
                br_dic.close();
            } catch (Exception e) {
            }
//            String docPointer = docsDictionary.get("FBIS3-3366");
//            int docPointerInt = Integer.parseInt(docPointer);
//            try {
//                BufferedReader br_docPosting = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\Docs" + "\\docsPosting.txt"));
//                for (int i = 0; i < docPointerInt; i++) {
//                    br_docPosting.readLine();
//                }
//                String docPostingLine = br_docPosting.readLine();
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//
//            String line = null;

//            JOptionPane.showMessageDialog(null, "Docs dictionary loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
            informationMsg += "Docs dictionary, ";
        } else {
            ans = false ;
            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
            return false;

        }

        if (dir != null && dir.exists()) {
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\headersDictionary.txt"));
                String line = "";
                while ((line = br_dic.readLine()) != null) {

                    String headerTerm = "";
                    String listOfDocs = "";
                    int indexOfFirstComma = StringUtils.indexOf(line, ",");
                    headerTerm = StringUtils.substring(line, 0, indexOfFirstComma);
                    listOfDocs = StringUtils.substring(line, indexOfFirstComma + 1);
                    headersDictionary.put(headerTerm, listOfDocs);
                }
                br_dic.close();
            } catch (Exception e) {
            }

//            JOptionPane.showMessageDialog(null, "Headers dictionary loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
            informationMsg += "Headers dictionary, ";
        } else {
            ans = false ;
            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
            return false;

        }

        if (dir != null && dir.exists()) {
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\docsEntities.txt"));
                String line = "";
                while ((line = br_dic.readLine()) != null) {

                    String docNum = "";
                    String listOfEntities = "";
                    int indexOfFirstComma = StringUtils.indexOf(line, ",");
                    docNum = StringUtils.substring(line, 0, indexOfFirstComma);
                    StringBuilder value = new StringBuilder();
                    listOfEntities = StringUtils.substring(line, indexOfFirstComma + 2); // Triming the ", {"
                    listOfEntities = StringUtils.substring(listOfEntities, 0, listOfEntities.length()-1);
                    String[] entities = StringUtils.split(listOfEntities, ",");
                    for (String entity1 : entities) {
                        String entity = entity1;
                        if (entity.charAt(0) == ' ')
                            entity = entity.substring(1);
                        value.append(entity).append(",");
                    }
                    docEntities.put(docNum, value.toString());
                }
                br_dic.close();
            } catch (Exception e) {
            }

//            JOptionPane.showMessageDialog(null, "Docs Entities loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
            informationMsg += "Docs entities ";
        } else {
            ans = false ;
            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        JOptionPane.showMessageDialog(null, informationMsg + " loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);




        return ans;
    }

    /**
     * clear all dic if loading again with stemming or not
     */
    private void cleanAllDictionaries() {
       termDictionary.clear();
        termDictionaryToShow.clear();
        citiesDictionary.clear();
        docsDictionary.clear();
         headersDictionary.clear();
        docEntities.clear();
         citiesView.clear();
    }


    /**
     * delete all files & folder created after posting process
     *
     * @return
     */
    public boolean resetAll() {
        File dir = new File(postingPath + "\\Postings" + ifStemming());
        System.out.println("Deletes: " + postingPath + "\\Postings" + ifStemming());
        if (dir.exists()) {

            try {
                return deleteDirectory(dir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    /**
     * Force deletion of directory
     *
     * @param path
     * @return
     */
    static public boolean deleteDirectory(File path) {
        if (path.exists()) {
            try {
                Posting.closeIO();
                FileUtils.deleteDirectory(path);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void pathUpdate(String corpusPath, String postingPath, boolean stemming) {
        this.corpusPath = corpusPath;
        this.postingPath = postingPath;
        this.is_stemming = stemming;
    }

    private String ifStemming() {
        if (is_stemming)
            return "withStemming";
        return "";
    }

    public void printTests() {
//        analyzeQrles("C:\\Ofek\\QueriesTests\\results\\qrels.txt");
        //  loadDicToMemory(ifStemming());
//        Searcher searcher = new Searcher(termDictionary, citiesDictionary, docsDictionary, postingPath, is_stemming);
//        try {
//            ArrayList<String> ans = searcher.getTermDocs("people");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        readQueryFromFile("C:\\Ofek\\QueriesTests\\queries-test.txt", null, false);
        //Searcher searcher = new Searcher(postingPath, is_stemming, null, termDictionary, docsDictionary, citiesDictionary);
        // searcher.handleQuery(query_id, sb_query.toString(), sb_desc.toString(), "British Chunnel impact");

//        printAnswer5();
//        printAnswer6();
//        try {
//            printAnswer7();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void analyzeQrles(String s) {

        try {
            BufferedReader br = new BufferedReader(new FileReader(s));
            BufferedWriter bw2 = new BufferedWriter(new FileWriter("C:\\Ofek\\QueriesTests\\analayze.txt"));
            String query_id = "";
            String line = "";
            while ((line = br.readLine()) != null){
                String[] split = StringUtils.split(line, " ");
                String queryId = split[0];
                if (query_id.equals("") || !query_id.equals(queryId)){
                    query_id = queryId;
//                    bw.append(query_id).append("\n");
                }
                String docNo = split[2];
                String strRelevant = split[3];
                if (strRelevant.equals("1")){
                    Posting.initTermPosting(postingPath, is_stemming);
                    String strPointer = docsDictionary.get(docNo);
                    if (strPointer == null){
                        System.out.println("Not exist: " + docNo);
                        continue;
                    }
                    int intPointer = Integer.parseInt(strPointer);
                    String parentPath = Posting.getDocPostingLineByPointer(intPointer).split(",")[1];
                    bw2.append(parentPath).append(" OR ");
                    bw2.flush();
//                    bw.append(docNo).append(" ").append(parentPath).append("\n");
                }
            }
//            bw.flush();
            bw2.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * read queries from file one by one
     *
     * @param path
     * @param
     */
    public void readQueryFromFile(String path, ObservableList items, boolean useSemantics) {

//        int queriesCounter = 0;
//        int queriesHandles = 0;
//        String line1;
//        try {
//
//            BufferedReader br_queriesCounter = new BufferedReader(new FileReader(path));
//            while ((line1 = br_queriesCounter.readLine()) != null) {
//                if (line1.equals("<top>"))
//                    queriesCounter++;
//            }
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        view.pi_progressIndicator.setProgress((double)queriesCounter/(double)queriesCounter);
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(path));

            StringBuilder sb_narr = new StringBuilder();
            StringBuilder sb_desc = new StringBuilder();
            //loadDicToMemory(ifStemming());

            ArrayList<String> cities = new ArrayList<>();
            if (items != null)
                cities.addAll(items);

//            cities.add("BUENOS");
//            cities.add("MOSCOW");
            //cities = null;

            //public Searcher(String posting, Boolean stemming, ArrayList<String> specificCities, TreeMap<String, String> termsDic, TreeMap<String, String> docsDic, TreeMap<String, Pair> citiesDic, HashMap<String, String> headersDictionary, HashMap<String,String> docEntities, boolean semantic) {

            String line = "" , query_id  ="" ,query = "" ;
            Searcher searcher = new Searcher(postingPath,corpusPath, is_stemming, cities, termDictionary, docsDictionary, citiesDictionary, headersDictionary, docEntities, useSemantics , AVL);
            while ((line = br.readLine()) != null) {
                while ((line = br.readLine()) != null) {
                    if (line.equals("<top>")) { // start of query
                        continue ;
                    }
                    if (line.equals("</top>")) { // start of query
                        break ;
                    }

                    if (line.startsWith("<num>")) {
                        String[] temp = line.split(" ");
                        query_id = temp[2];
                    }
                    if ( line.startsWith("<title>")){
                        query = line.split("> ")[1];
                    }
                    if ( line.startsWith("<desc>")){
                        line = br.readLine();
                        while (line != null && !line.equals("")&&!line.startsWith("<")) {
                            sb_desc.append(" " + line);
                            line = br.readLine();
                        }
                    }
                    if (line != null && line.startsWith("<narr>")){
                        line = br.readLine();
                        while (line != null && !line.equals("")&& !line.startsWith("<")&&!line.equals(" ")) {
                            sb_narr.append(" " + line);
                            line = br.readLine();
                        }
                    }

                }
                searcher.handleQuery(query_id, query, sb_desc.toString(), sb_narr.toString() , useSemantics);
//                queriesHandles++;
//                view.pi_progressIndicator.setProgress((double)queriesHandles/(double)queriesCounter);
//                view.indicateQueryHandled(query_id);
                sb_desc.delete(0, sb_desc.length());
                sb_desc.setLength(0);
                sb_narr.delete(0, sb_narr.length());
                sb_narr.setLength(0);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void printAnswer5() {
        int noneCapitalCity = 0;
        int totalNumOfCities = citiesDictionary.size();
        for (Map.Entry<String, Pair> entry : citiesDictionary.entrySet()) {
            String currCity = entry.getKey();
            String[] cityAndDetails = StringUtils.split(currCity, ",");
            if (cityAndDetails.length < 2 || cityAndDetails[1].equals("")) {
                noneCapitalCity++;
            }
        }
        System.out.println("---THE ANSWER FOR QUESTION 5---");
        System.out.println("Number of unique cities: " + totalNumOfCities);
        System.out.println("Number of none capital cities: " + noneCapitalCity);
    }

    private void printAnswer6() {
        String docNum = "";
        String city = "";
        String locs = "";

        String maxCitySoFar = "";
        String maxDocNumSoFar = "";
        int maxTfSoFar = 0;

        for (Map.Entry<String, Pair> entry : citiesDictionary.entrySet()) {
            String currCity = entry.getKey();
            String docsList = (String) entry.getValue().getValue();
            String[] docs = StringUtils.split(docsList, "#");
            for (int i = 0; i < docs.length; i++) {
                String[] docNumAndTf = StringUtils.split(docs[i], "|");
                if ((Integer.parseInt(docNumAndTf[1]) > maxTfSoFar)) {
                    maxCitySoFar = currCity;
                    maxDocNumSoFar = docNumAndTf[0];
                    maxTfSoFar = Integer.parseInt(docNumAndTf[1]);
                }
            }
        }
        System.out.println("---THE ANSWER FOR QUESTION 6---");


        System.out.println(maxDocNumSoFar + " " + maxCitySoFar + " " + maxTfSoFar);
    }

    private void printAnswer7() throws IOException {
        BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings\\termDictionary.txt"));
        PriorityQueue<String> maxQueue = new PriorityQueue<>(new TermMaxTFComparator());
        PriorityQueue<String> minQueue = new PriorityQueue<>(new TermMinTFComparator());
        String line = null;
        while ((line = br_dic.readLine()) != null) {
            String term = "";
            String tf = "";
            String[] splited = StringUtils.split(line, ",");
            String[] termSplited = StringUtils.split(splited[0], "<D>");
            if (termSplited.length < 1)
                continue;
            term = termSplited[0];
            if (splited.length > 4) {
                tf = splited[splited.length - 3];
            }
            if (!tf.equals("")) {
                maxQueue.add(tf + "," + term);
                minQueue.add(tf + "," + term);
            }
        }
        System.out.println("---THE ANSWER FOR QUESTION 7---");
        System.out.println("The less frequency terms: ");
        for (int i = 0; i < 10; i++) {
            String ans = maxQueue.poll();
            System.out.println(ans);
        }
        System.out.println("The most frequency terms: ");
        for (int i = 0; i < 10; i++) {
            String ans = minQueue.poll();
            System.out.println(ans);
        }

    }

    public String getQueriesResults() {
        return Ranker.getQueriesResults();
    }

    public String getEntities(String selectedDocNo) {
        StringBuilder sb = new StringBuilder();
        String entities = docEntities.get(selectedDocNo);
        String[] split = StringUtils.split(entities, ",");
        for (int i = 0; i < split.length; i++) {
            String[] split1 = StringUtils.split(split[i], "=");
            String entity = split1[1];
            String value = split1[0];
            sb.append(entity).append("=").append(value).append("\n");
        }
        return sb.toString();
    }

    public String getResultsTrecFormat() {
        return Ranker.getTrecFormatResults();
    }

    public void handleSingleQuery(String query, ObservableList items, boolean useSemantics) {
        ArrayList<String> cities = new ArrayList<>();
        cities.addAll(items);
        Searcher searcher = new Searcher(postingPath,corpusPath, is_stemming, cities, termDictionary, docsDictionary, citiesDictionary, headersDictionary, docEntities, useSemantics , AVL);
        searcher.handleQuery( "single query" , query, "null", "null", useSemantics);

    }


    public static class TermMaxTFComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            String s1 = ((String) (o1));
            String s2 = ((String) (o2));
            if (s1.charAt(0) == '*')
                s1 = s1.substring(1);
            if (s2.charAt(0) == '*')
                s2 = s2.substring(1);
            s1 = StringUtils.split(s1, ",")[0];
            s2 = StringUtils.split(s2, ",")[0];
            int intS1 = Integer.parseInt(s1);
            int intS2 = Integer.parseInt(s2);
            if (intS1 > intS2)
                return 1;
            if (intS1 == intS2)
                return 0;
            return -1;
        }
    }

    public static class TermMinTFComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            String s1 = ((String) (o1));
            String s2 = ((String) (o2));
            if (s1.charAt(0) == '*')
                s1 = s1.substring(1);
            if (s2.charAt(0) == '*')
                s2 = s2.substring(1);
            s1 = StringUtils.split(s1, ",")[0];
            s2 = StringUtils.split(s2, ",")[0];
            int intS1 = Integer.parseInt(s1);
            int intS2 = Integer.parseInt(s2);
            if (intS1 > intS2)
                return -1;
            if (intS1 == intS2)
                return 0;
            return 1;
        }
    }
    public ArrayList<String> getCitiesView (){
        return citiesView;
    }
}
