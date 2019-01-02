/**
 This class actually manages the corpus processing and uses the various departments to perform the following actions (in chronological order):
 1. Reading and parsing the document repository (The parse output will be written into temporary files called segment files).
 2. Creating the inverted indexes
 */

package Engine.Model;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CorpusProcessingManager {
    public static final boolean testMode = false;

    private static double MILLION = Math.pow(10, 6);
    private ReadFile reader; // The reader will be responsible for reading the files and breaking them into parts that will be passed to the parser
    private String corpusPath; // The document repository path to perform the processing operation
    private String postingPath ; // A path for the posting files depending on the user's selection
    private Indexer inverter; // The inverter will be responsible for constructing the inverter indexes
    private ArrayList<String> filesPathsList; // A data structure that will hold all file paths in the corpus
    private HashMap<String,String > inverted_city; // < State , City >
    private static ExecutorService parseExecutor; // Thread pool for the parsers run
    public static ConcurrentHashMap<String, City> cities ; // < City , City_obj >  cities after parsing


    public CorpusProcessingManager(String corpusPath, String postingPath  , boolean stemming) {
        this.corpusPath = corpusPath;
        this.postingPath =  postingPath + "\\Postings" + ifStemming(stemming);

        this.reader = new ReadFile(this.postingPath , stemming , corpusPath);
        createDirs(this.postingPath);
        Posting.initPosting(this.postingPath + "\\Docs");
        inverter = new Indexer(new Posting(this.postingPath));

        filesPathsList = new ArrayList<>();
        int NUM_OF_PARSERS = 2;
        parseExecutor = Executors.newFixedThreadPool(NUM_OF_PARSERS);
        cities = new ConcurrentHashMap<>();
        inverted_city = new HashMap<>() ;
    }

    public static String ifStemming(boolean stemming) {
        if (stemming)
            return "withStemming";
        return "";
    }

    /**
     * Creates the required folder tree to hold the final output files that will be generated from the processing process
     * @param postingPath The desired posting path given by the user
     */
    private void createDirs(String postingPath) {
        new File(postingPath + "\\Terms").mkdirs();
        new File(postingPath + "\\Docs").mkdirs();
        new File(postingPath + "\\Segment Files").mkdirs();
    }



    /**
     * The method that manages the entire corpus processing process.
     * The method calls for auxiliary methods in order to obtain the final outputs required
     */
    public void StartCorpusProcessing() {
        initFilesPathList(corpusPath);
        Indexer.initIndexer(postingPath);

        try {
            if (testMode) {
                String startParseTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                System.out.println("Starting parsing: " + startParseTimeStamp);
            }
            readAndParse();

            if (testMode) {
                String finishParseTimeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
                System.out.println("Finish parsing: " + finishParseTimeStamp);
            }


        }
        catch (Exception e ){
        }
        //end of parse
        cities = reader.cities;
        //reader.CleanAll();
        if (testMode){
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            System.out.println("Starting building Inverted Index: " + timeStamp);
        }

        try {
            buildInvertedIndex();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (testMode){
            String timeStamp1 = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            System.out.println("Finished building Inverted Index: " + timeStamp1);
        }
        //closeAllSegmentFiles();
        try {
            FileUtils.deleteDirectory(new File(postingPath + "//Segment Files"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        AddInfoToCities();
    }




    private void buildInvertedIndex() throws FileNotFoundException {
        inverter.startIndexerOperations();
        System.out.println("done");
    }

    /**
     * This method manages the parallel run of the parsers.
     * The method is part of a mechanism that takes care of parallelism in performing the reading and parsing operation of multiple files simultaneously.
     * @throws InterruptedException
     */
    private void readAndParse() throws InterruptedException {
        for (int i = 0; i < filesPathsList.size(); i++) {
            int j = i ;
            ArrayList<String> temp_list= new ArrayList<>() ;
            int u = 0 ;
            while ( j < filesPathsList.size() && u < 20 ){
                temp_list.add( filesPathsList.get(j));
                j++ ;
                u++ ;
            }
            i = j -1  ;
            int finalJ = j;
            Thread readNParseThread = new Thread(() ->reader.readAndParseLineByLine(temp_list , finalJ));
            parseExecutor.execute(readNParseThread);

        }
        parseExecutor.shutdown();
        while (!parseExecutor.isTerminated()) {
        }
        System.out.println("done files");
        writeLangToDisc();
    }

    private void writeLangToDisc() {

        String segmantPartitionFilePath = postingPath +"\\Languages"+  ".txt";
        File newFile = new File(segmantPartitionFilePath );
        try {
            newFile.createNewFile();
        }
        catch (Exception e ){}

        try {
            BufferedWriter file_buffer_writer = new BufferedWriter(new FileWriter(segmantPartitionFilePath));
            int counter = 0 ;
            for (String s:reader.languages
                    ) {
                file_buffer_writer.append(s+ "\n");
                counter++;
                if (counter > 400) {
                    file_buffer_writer.flush();
                }
            }

            file_buffer_writer.flush();
            file_buffer_writer.close();

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }




    }


    public void AddInfoToCities(){
        cities = reader.getCities() ;
        getCitiesInfo () ;
        //end of parse
    }


    /**
     * This method, along with the following method, is responsible for adding the paths of all corpus files into the filePathsList data structure.
     * @param curposPath The path of the corpus given by the user.
     */
    private void initFilesPathList(String curposPath) {
        final File folder = new File(curposPath);
        listFilesOfFolder(folder);
    }

    public void listFilesOfFolder(final File folder) {
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesOfFolder(fileEntry);
            } else {
                String t =  fileEntry.getName() ;
                if ( t.equals("stop_words.txt"))
                    continue;
                filesPathsList.add(fileEntry.getPath());
            }
        }
    }


    /**
     * save city in a global hash map
     * @param
     */
    public void getCitiesInfo (){
//        for (Map.Entry<String, City> entry : cities.entrySet())
//        {
        //System.out.println(entry.getKey() + "/" + entry.getValue());
        try {
            // System.out.println(getText(entry.getKey()));
            //get hash maps
            getCitiesState();
            getCitiesPopulation() ;
            getCitiesCurrencies() ;
        }
        catch (Exception e ){
            System.out.println(e.getCause());
        }
//        }



    }

    /**
     * get cities info about states and insert to citias hashmap
     * @return
     * @throws Exception
     */
    public  String getCitiesState() throws Exception {

        //URL website = new URL("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + city_name);
        URL url = new URL("http://restcountries.eu/rest/v2/all?fields=name;capital;");


        //URLConnection connection = website.openConnection();
        HttpURLConnection con  = ( HttpURLConnection)  url.openConnection();
        con.setRequestMethod("GET");


        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        con.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;
        inputLine = in.readLine() ;
        //while (() != null) {
        //response.append(inputLine);
        String[] splited = StringUtils.split(inputLine,"[]}{,:\"") ;
        for ( int i= 0 ; i < splited.length; ) {
            String s = splited[i];
            String state = "";
            String city = "";
            if (!splited[i].equals("name")){
                i++;
                continue;
            }
            if ( splited[i].equals("name")) {
                i++;
                state = splited[i].toLowerCase();
                i++;
            }
            if ( splited[i].equals("capital")) {
                i++;
                if ( Character.isUpperCase(splited[i].charAt(0)))
                    city = splited[i].toLowerCase();
                else city = "none" ;

            }
            String first_part = "" ;
            if ( city.contains(" ") ) // 2 word city
                first_part = city.split(" ")[0].toLowerCase();
            if (cities.containsKey(city) || cities.containsKey(first_part)) {
                City city_obj = cities.get(city);
                if (city_obj == null )
                    city_obj = cities.get(first_part) ;
                if ( city_obj == null ) {
                    continue;
                }
                try {
                    city_obj.setState_name(state);
                    inverted_city.put(state, city);
                    cities.put(city, city_obj);
                }
                catch (Exception e ){
                    System.out.println(city + " : " + state);
                }

            }

        }

        // }

        in.close();

        return null ;
    }

    /**
     * get info of cities pop from api
     * @return
     * @throws Exception
     */
    public  String getCitiesPopulation() throws Exception {
        //URL website = new URL("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + city_name);
        URL url = new URL("http://restcountries.eu/rest/v2/all?fields=name;population");
        //URLConnection connection = website.openConnection();
        HttpURLConnection con  = ( HttpURLConnection)  url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        con.getInputStream()));
        //StringBuilder response = new StringBuilder();
        String inputLine;
        inputLine = in.readLine() ;
        String[] splited = StringUtils.split(inputLine,"[]}{:\"") ;
        for ( int i= 0 ; i < splited.length-3; ) {
            String s =  splited[i] ;
            if (s.equals("[") || s.equals(",") || s.equals("]") || s.equals("name") || s.equals("population")) {
                i++;
                continue;
            }
            //String[] splited_split = StringUtils.split(inputLine,"") ;
            String state = splited[i].toLowerCase();
            String population = splited[i+3];
            String first_part = "" ;

            if (inverted_city.containsKey(state) ) {
                String city = inverted_city.get(state);

                City city_obj = cities.get(city); // try 1 word city first
                if (city_obj == null) {
                    first_part = city.split(" ")[0];// try 2 words city
                    city_obj = cities.get(first_part);
                }
                //round num
                int num = Integer.parseInt(population);
                try {
                    if ( Parse.isNumber( population ) && num > MILLION ){

                        double num_d = round_num ( num ) ;
                        population =  "M" + Double.toString(num_d) ;
                    }
                    city_obj.setPopulation(population);
                    cities.put(city, city_obj);
                }
                catch (Exception e ){
                    System.out.println(state + " " + city_obj.toString());
                }
            }
            i=i+4;
        }
        in.close();

        return null ;
    }

    /**
     * round a num over 1 m
     * @param num
     * @return
     */
    private double round_num(int num) {
        double rounded = num / MILLION;
        rounded = round( rounded , 2 ) ;
        return rounded ;
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * get cities info about currencies and insert to citias hashmap
     * @return
     * @throws Exception
     */
    public  String getCitiesCurrencies() throws Exception {


        //URL website = new URL("http://getcitydetails.geobytes.com/GetCityDetails?fqcn=" + city_name);
        URL url = new URL("http://restcountries.eu/rest/v2/all?fields=name;currencies;");


        //URLConnection connection = website.openConnection();
        HttpURLConnection con  = ( HttpURLConnection)  url.openConnection();
        con.setRequestMethod("GET");


        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        con.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;
        inputLine = in.readLine() ;
        //while (() != null) {
        //response.append(inputLine);
        String[] splited = StringUtils.split(inputLine,"[]}{,:\"") ;
        int jump = 7 ;
        for ( int i= 0 ; i < splited.length-3; ) {
            String s =  splited[i] ;
            if (!s.equals("code")) {
                i++;
                continue;
            }
            //String[] splited_split = StringUtils.split(inputLine,"") ;
            String currency = "" ;
            if ( s.equals("code"))
                currency= splited[i+1] ; // got cuurency
            i++ ;
            //now find state
            String state = "" ;
            int count_name = 0 ;
            while( i < splited.length-3 &&  !inverted_city.containsKey(state)  ){
                state = splited[i].toLowerCase() ;
                if (state.equals("name")){
                    count_name++;
                }
                if ( count_name == 2) {// counted 2 names , should stop
                    state = splited[i + 1].toLowerCase();
                    break;
                }
                i++ ;
            }

            String first_part = "" ;
            if (inverted_city.containsKey(state) ) {
                String city = inverted_city.get(state);

                City city_obj = cities.get(city); // try 1 word city first
                if (city_obj == null) {
                    first_part = city.split(" ")[0];// try 2 words city
                    city_obj = cities.get(first_part);
                }
                try {
                    city_obj.setCurrency(currency);
                    cities.put(city, city_obj);
                }
                catch (Exception e ){
                    System.out.println(state + " : " + state);
                }

            }
            i++;
        }

        // }

        in.close();

        return null ;
    }

    public String[] getDocLang ()
    {
        return reader.getLanguagesList();
    }
    //URL url = new URL("http://restcountries.eu/rest/v2/all?fields=name;currencies;");
    //URL url = new URL("http://restcountries.eu/rest/v2/all?fields=name;population");

}
