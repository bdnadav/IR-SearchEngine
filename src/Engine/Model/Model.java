package Engine.Model;

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
    private String corpusPath; //saved corpus path
    private String postingPath ;// saved outpot posting path
    private boolean is_stemming ; // using a stemmer on terms ot not
    public String [] list_lang ; //list of lang returns from parsing the docs
    // will allow to load the term dic to prog memory -
    // will be used in project part 2
    HashMap < String , String[] > termDictionary = new HashMap<>();
    TreeMap < String , String > citiesDictionary = new TreeMap<>();
    TreeMap < String , String > docsDictionary = new TreeMap<>();

    /**
     * run corpus processing manager , get back info from posting process and
     * display it at the end ,
     * get lang lind and set it in gui
     * @param corpusPath
     * @param postingPath
     * @param stemming
     */
    public void run(String corpusPath, String postingPath, boolean stemming) {
        long startTime = System.currentTimeMillis();
        this.corpusPath = corpusPath;
        this.postingPath = postingPath;
        this.is_stemming = stemming ;
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

    }

    /**
     * help to present a summary of run info in the end of posting
     * @param estimatedTime - runtime
     * @param uniqueTerms - num of unique terms in the corpus
     * @param docsGenerate - how many docs in the corpus
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
     * @param stemming
     */
    public void loadDicToMemory(String stemming) {
        File dir = new File(postingPath + "\\Postings" + ifStemming());
        if (dir != null && dir.exists()) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\termDictionary.txt"));
                String line = "";
                while ((line = br_dic.readLine()) != null) {

                    String term = "";
                    String tf = "";
                    String[] splited = StringUtils.split(line, "<D>");
                    String[] termSplited = StringUtils.split(splited[1], ",");
                    term = splited[0];
                    termDictionary.put(term, termSplited);
                }
            } catch (Exception e) {
            }

            String line = null;

            JOptionPane.showMessageDialog(null, "Directoy Loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
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
                    int firstIndexOfComma = StringUtils.indexOf(line, ",");
                    int lastIndexOfComma = StringUtils.lastIndexOf(line, ",");
                    city = StringUtils.substring(line, 0, lastIndexOfComma); // city with details
                    docsList = StringUtils.substring(line, lastIndexOfComma + 1);

                    citiesDictionary.put(city, docsList);
                }
            } catch (Exception e) {
            }

            String line = null;


            JOptionPane.showMessageDialog(null, "Directoy Loaded to Memory", "Load", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(null, "Posting Directory does not Exists", "Error", JOptionPane.ERROR_MESSAGE);
        }

        File docsDir = new File(postingPath + "\\Postings" + ifStemming());
        if (dir != null && docsDir.exists()) {
            StringBuilder sb = new StringBuilder();
            try {
                BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings" + ifStemming() + "\\docsDictionary.txt"));
                String line = "";
                while ((line = br_dic.readLine()) != null) {

                    String docNumber = "";
                    String pointer = "";
                    int firstIndexOfComma = StringUtils.indexOf(line, ",");
                    docNumber = StringUtils.substring(line, 0, firstIndexOfComma);
                    pointer = StringUtils.substring(line,  firstIndexOfComma + 1);

                    docsDictionary.put(docNumber, pointer);
                }
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


        }
    }

    /**
     * delete all files & folder created after posting process
     * @return
     */
    public boolean resetAll() {
        File dir = new File(postingPath + "\\Postings" + ifStemming());
        System.out.println("Deletes: " + postingPath + "\\Postings"+ifStemming());
        if (dir.exists()) {

            try {
                return deleteDirectory(dir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else {

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
    public void pathUpdate ( String corpusPath , String postingPath , boolean stemming ) {
        this.corpusPath = corpusPath;
        this.postingPath = postingPath;
        this.is_stemming = stemming ;
    }

    private String ifStemming() {
        if (is_stemming)
            return "withStemming";
        return "";
    }

    public void printTests() {

        printAnswer5();
        printAnswer6();
        try {
            printAnswer7();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printAnswer5(){
        int noneCapitalCity = 0;
        int totalNumOfCities = citiesDictionary.size();
        for(Map.Entry<String,String> entry : citiesDictionary.entrySet()) {
            String currCity = entry.getKey();
            String[] cityAndDetails = StringUtils.split(currCity, ",");
            if (cityAndDetails.length < 2 || cityAndDetails[1].equals("")){
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

        for(Map.Entry<String,String> entry : citiesDictionary.entrySet()) {
            String currCity = entry.getKey();
            String docsList = entry.getValue();
            String[] docs = StringUtils.split(docsList, "#");
            for (int i = 0; i < docs.length; i++) {
                String[] docNumAndTf = StringUtils.split(docs[i], "|");
                if ((Integer.parseInt(docNumAndTf[1]) > maxTfSoFar)){
                    maxCitySoFar = currCity;
                    maxDocNumSoFar = docNumAndTf[0];
                    maxTfSoFar = Integer.parseInt(docNumAndTf[1]);
                }
            }
        }
        System.out.println("---THE ANSWER FOR QUESTION 6---");


        System.out.println(maxDocNumSoFar + " " +  maxCitySoFar + " " +  maxTfSoFar);
    }

    private void printAnswer7() throws IOException {
        BufferedReader br_dic = new BufferedReader(new FileReader(postingPath + "\\Postings\\termDictionary.txt"));
        PriorityQueue<String> maxQueue = new PriorityQueue<>(new TermMaxTFComparator());
        PriorityQueue<String> minQueue = new PriorityQueue<>(new TermMinTFComparator());
        String line = null;
        while ((line = br_dic.readLine()) != null){
            String term = "";
            String tf = "";
            String[] splited = StringUtils.split(line,",");
            String[] termSplited = StringUtils.split(splited[0], "<D>");
            if (termSplited.length < 1)
                continue;
            term = termSplited[0];
            if (splited.length > 4){
                tf = splited[splited.length-3];
            }
            if (!tf.equals("")){
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
}
