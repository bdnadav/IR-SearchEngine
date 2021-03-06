package Engine.Model;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.jws.WebParam;

/**
 * Returns the most relavent docs to a Query
 */
public class Searcher {
    private static final int MAX_TRG_TERMS_FROM_API = 1; // determine the number of terms strongly connected   with saved from the api
    private final int MAX_SYN_TERMS_FROM_API = 2 ; // determine the number of terms with meaning like  saved from the api
    private final boolean stemming;
    private final boolean useSemantic;
    private final String corpusPath;
    private ArrayList<String> speceficCities = null;
    private Parse queryParse;
    private Parse descParse;
    private TreeMap<String, String> terms_dictionary;
    private TreeMap<String, Pair> cities_dictionary;
    private TreeMap<String, String> docs_dictionary;
    private ArrayList<String> synonymous_terms;
    public static HashMap<String, String> headers_dictionary;
    public static HashMap<String, String> docs_entities;
    private Ranker ranker;
    private HashSet<String> legalDocs; // If cities constraint, this data structure will hold all the docs whose can be return.
    private boolean citiesConstraint;
    private String posting;
    private double AVL; // use in the BM25 formula 
    private ArrayList<String> trigers_terms;

    /**
     * Gets a query and handle is by sending it to the ranker
     * @param posting
     * @param corpusPath
     * @param stemming
     * @param specificCities
     * @param termsDic
     * @param docsDic
     * @param citiesDic
     * @param headersDictionary
     * @param docEntities
     * @param semantic
     * @param AVL
     */
    public Searcher(String posting, String corpusPath, boolean stemming, ArrayList<String> specificCities, TreeMap<String, String> termsDic, TreeMap<String, String> docsDic, TreeMap<String, Pair> citiesDic, HashMap<String, String> headersDictionary, HashMap<String, String> docEntities, boolean semantic, double AVL) {
        this.terms_dictionary = termsDic;
        this.AVL = AVL ;
        this.synonymous_terms = new ArrayList<>();
        this.trigers_terms = new ArrayList<>();
        this.stemming = stemming;
        Parse.stemming = stemming;
        this.cities_dictionary = citiesDic;
        this.docs_dictionary = docsDic;
        this.headers_dictionary = headersDictionary;
        this.docs_entities = docEntities;
        this.AVL = AVL;
        this.corpusPath = corpusPath;
        this.posting = posting;
        this.useSemantic = semantic;
        Posting.initTermPosting(posting, stemming);
        if (specificCities != null && !specificCities.isEmpty()) {
            citiesConstraint = true;
            this.speceficCities = specificCities;
            legalDocs = getLegalDocs(specificCities);
        }
    }

    /**
     * get the docs fitted to certain cities
     * @param specificCities
     * @return
     */
    private HashSet<String> getLegalDocs(ArrayList<String> specificCities) {
        HashSet<String> ans = new HashSet<>();
        for (String currCity : specificCities) {
            String citiesDicValue = (String) cities_dictionary.get(currCity).getValue();
            String[] splited = StringUtils.split(citiesDicValue, "#");
            for (int j = 1; j < splited.length; j++) {
                int indexOfSeparator = StringUtils.indexOf(splited[j], "|");
                String docNo = StringUtils.substring(splited[j], 0, indexOfSeparator);
                ans.add(docNo);
            }
        }
        return ans;
    }


    /**
     * get a query , use api if needed and find the right docs by sending it to the ranker
     * @param query_id
     * @param queryTitle
     * @param queryDescription
     * @param queryNarrative
     * @param useSemantic
     * @return
     */
    public ArrayList<String> handleQuery(String query_id, String queryTitle, String queryDescription, String queryNarrative, boolean useSemantic) {
        resetAllDataStructures() ; //a new query has arrived - cleanall
        queryParse.parseQuery(queryTitle);
        ArrayList<String> queryTitleTerms = queryParse.getQueryTerms();
        ArrayList<String> originalTitleTerms = new ArrayList<>(queryTitleTerms);
        Set<String> set = new HashSet<>(queryTitleTerms);
        queryTitleTerms.clear();
        queryTitleTerms.addAll(set);
        ArrayList<String> queryDescTerms  ;
        if ( !queryDescription.equals("null")) {
            descParse.parseQuery(queryDescription);
            queryDescTerms = descParse.getQueryTerms();
        }else   queryDescTerms = new ArrayList<>(); // there is no DEsc to single query
        if (Model.debug){
            System.out.println("Query id: " + query_id + "\n" + "Query title: " + queryTitle + "\n" + "Query title terms (may after stemming): " + queryTitleTerms.toString() + "\n");
            System.out.println("Query description: " + queryDescription + "\n" + "Query description terms (may after stemming): " + queryDescTerms.toString() + "\n");
        }
        HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsByQueryTitleTerms; // <QueryTerm, <DocNo|tf, [DocDetails, DocHeaders]>>
        /* DocDetails = mostFreqTerm, mostFreqTermAppearanceNum, uniqueTermsNum, fullDocLength
           DocHeaders = [headerTerm, headerTerm, ... ] */

        /** Handle Semantic **/
        if ( useSemantic) {
            if(stemming) {
                ArrayList<String> noStemmingQueryTitleTerms;
                Parse.stemming = false;
                Parse noStemmingParse = new Parse(posting, false, corpusPath);
                noStemmingParse.parseQuery(queryTitle);
                noStemmingQueryTitleTerms = noStemmingParse.getQueryTerms();
                Parse.stemming = true;
                getSemanticTerms(queryTitleTerms , noStemmingQueryTitleTerms, true );
            }
            else getSemanticTerms(queryTitleTerms ,originalTitleTerms, false );

            if ( !synonymous_terms.isEmpty()){
                for (String s :synonymous_terms
                        ) {
                    if (!queryTitleTerms.contains(s)) {// join syn and title
                        queryTitleTerms.add(s);
                        if (Model.debug)
                            System.out.println("The synonymous term: " + s + " been added to queryTitleTerms");
                    }
                }
            }
//            if ( !trigers_terms.isEmpty()){
//                for (String s :trigers_terms
//                        ) {
//                    if (!queryTitleTerms.contains(s)) {// join syn and title
//                        queryTitleTerms.add(s);
//                        System.out.println("The trigered term: " + s + " been added to queryTitleTerms");
//                    }
//                }
//            }
        }
        if (extraTermsMayHelp(originalTitleTerms, queryDescTerms)){
            ArrayList<String> queryDescTermsToAdd = getExtraTerms(queryDescTerms, queryTitleTerms);
            if (queryTitleTerms.size() < 6){
                if (Model.debug)
                    System.out.println("queryTitleTerms.size() < originalTitleTerms.size()*2");
                if (queryDescTermsToAdd.size() > 2 && useSemantic){
                    ArrayList<String> queryDescTermsToSendSemantic = new ArrayList<>(queryDescTermsToAdd.subList(queryDescTermsToAdd.size()-3, queryDescTermsToAdd.size()));
                    if (Model.debug)
                        System.out.println("queryDescTermsToSendSemantic: " + queryDescTermsToSendSemantic.toString());
                    getSemanticTerms(queryDescTermsToSendSemantic, queryDescTermsToSendSemantic, true);
                }

            }

            queryTitleTerms.addAll(queryDescTermsToAdd);
        }
        if (Model.debug) {
            System.out.println("Original title terms: " + originalTitleTerms.toString());
            System.out.println("desc terms: " + queryDescTerms.toString());
            System.out.println("Synonymous terms: " + synonymous_terms.toString());
            System.out.println("All terms to get docs by: " + queryTitleTerms.toString());
        }
        relevantDocsByQueryTitleTerms = getRelevantDocs(queryTitleTerms);

        ArrayList<String> rankedDocs = ranker.getRankDocs(query_id, relevantDocsByQueryTitleTerms, queryDescTerms, originalTitleTerms, queryDescTerms);
        return rankedDocs;
    }

    /**
     * help to clean all the data structures from query to query
     */
    protected void resetAllDataStructures() {
        this.ranker = new Ranker(docs_dictionary.size(), AVL);
        this.synonymous_terms.clear();
        this.synonymous_terms = new ArrayList<>();
        this.trigers_terms.clear();
        this.trigers_terms = new ArrayList<>();
        this.queryParse = new Parse(posting, stemming , corpusPath);
        this.descParse = new Parse(posting, stemming , corpusPath);
        Posting.initTermPosting(posting, stemming);
    }

    /**
     * get more terms from the Desc part in query file
     * @param queryDescTerms
     * @param queryTitleTerms
     * @return
     */
    private ArrayList<String> getExtraTerms(ArrayList<String> queryDescTerms, ArrayList<String> queryTitleTerms) {
        ArrayList<String> ans = new ArrayList<>();
        queryDescTerms.removeAll(queryTitleTerms);
        queryDescTerms.remove("*document");
        queryDescTerms.remove("document");
        int termsExtra = (int)(queryDescTerms.size()/1.00) ;
        TreeMap<Integer, String> dfOfTerms = new TreeMap<>();
        for (int i = 0; i < queryDescTerms.size(); i++) {
            String term = queryDescTerms.get(i);
            int df = getTermDf(term);
            dfOfTerms.put(df, term);
        }
        for (int i = 0; i < termsExtra; i += 2) {
            if (dfOfTerms.size() > 1) {
                ans.add(dfOfTerms.pollFirstEntry().getValue()); // add the terms with the lower df
                ans.add(dfOfTerms.pollLastEntry().getValue()); // add the terms with the higher df
            }
        }
        return ans;
    }

    /**
     * get a term Df from the term'sa data in  terms_dictionary
     * @param term
     * @return
     */
    private int getTermDf(String term) {
        if (term.charAt(0) == '*')
            term = StringUtils.substring(term, 1);
        String dicTermLine = terms_dictionary.get(term);
        if (dicTermLine == null)
            dicTermLine = terms_dictionary.get(term.toUpperCase());
        if (dicTermLine == null)
            return -1;
        String[] split = StringUtils.split(dicTermLine, ",");
        if (split.length < 5)
            return -1;
        String strDf = split[3];
        try{
            int ans = Integer.parseInt(strDf);
            return ans;
        }catch (Exception e){
            System.out.println(split[4] + ", " + split[5]);
        }
        return 1;
    }

    private boolean extraTermsMayHelp(ArrayList<String> queryTitleOriginalTerms, ArrayList<String> queryDescTerms) {
        if (queryDescTerms.size() >= 2 * queryTitleOriginalTerms.size())
            return true;
        return false;
    }

    /**
     * send each term from query terms to UseUrlSemantic and insert the result from the api to synonymous_terms
     * @param queryTerms
     * @param originalQueryTerms
     * @param stemming_semantic
     * @return
     */
    private Map<String, List<Pair<String,String>>> getSemanticTerms(ArrayList<String> queryTerms, ArrayList<String> originalQueryTerms, boolean stemming_semantic) {
        int termsFromApi = 0;
        for (int i = 0; i < queryTerms.size(); i++) {
            String term = queryTerms.get(i);
            try {
                term = Parse.cleanToken(term); // clean *
                if (term.charAt(0) == '*')
                    term = StringUtils.substring(term, 1);
                useUrlSemantic(term, stemming_semantic); // insert to  synonymous map all the terms and their docs
                if (termsFromApi == synonymous_terms.size() && i < originalQueryTerms.size()){
                    String originalTerm = originalQueryTerms.get(i);
                    originalTerm = Parse.cleanToken(originalTerm); // clean *
                    if (originalTerm.charAt(0) == '*')
                        originalTerm = StringUtils.substring(term, 1);
                    useUrlSemantic(originalTerm, stemming_semantic); // insert to  synonymous map all the terms and their docs
                }
                termsFromApi = synonymous_terms.size();



                /** do something **/

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return  null ;
    }

    /**
     * check for each term in query if the is synonymouse terms in the api , check if any of them
     * in the dic terms , if they are in the func will save the pointer to the term's posting row
     * @param term
     * @param stemming_semantic
     * @throws Exception
     */
    private void useUrlSemantic(String term, boolean stemming_semantic) throws Exception {
        String[] splitedTerm = StringUtils.split(term, " -");
        if (splitedTerm != null && splitedTerm.length > 1){
            term = splitedTerm[0];
            for (int i = 1; i < splitedTerm.length-1; i++) {
                term += "+";
                term += splitedTerm[i];
            }
            term += "+" + splitedTerm[splitedTerm.length-1];
        }

        URL url = new URL("https://api.datamuse.com/words?ml=" + term);
        HttpURLConnection con  = ( HttpURLConnection)  url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();

        String json_str = "";
        String line ="";
        while ((line = in.readLine()) != null) {
            json_str = json_str + line;
        }
        in.close();
        JSONArray jsonArray = new JSONArray(json_str);
        int count_legit_terms = 0 ;
        //if (jsonArray)
        for (int k = 0 ; k < jsonArray.length() ; k++) {
            JSONObject obj = (JSONObject) jsonArray.get(k);
            String synonymous_term= (String) obj.get("word");
            //String synonymous_score= (String) obj.get("score");
            String termData ;
            if (stemming_semantic) synonymous_term = stem(synonymous_term); //
            termData = terms_dictionary.get(synonymous_term);
            if ( termData == null )  // try capital term
                termData = terms_dictionary.get(synonymous_term.toUpperCase());
            if ( termData == null )// the term isnt in the corpus
                continue;
            /** set a threshhold for term relavence by score !!! ***/
            //synonymous_terms.put(synonymous_term, synonymous_score);
            if ( !synonymous_terms.contains(synonymous_term)) {
                synonymous_terms.add(synonymous_term);
                count_legit_terms++;
            }
            else
                continue;

            if (count_legit_terms == MAX_SYN_TERMS_FROM_API ) //save only the MAX_SYN_TERMS top terms
                break;
        }
        url = new URL("https://api.datamuse.com/words?rel_trg=" + term);
        //URLConnection connection = website.openConnection();
        con  = ( HttpURLConnection)  url.openConnection();
        con.setRequestMethod("GET");
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        response = new StringBuilder();

        json_str = "";
        line ="";
        while ((line = in.readLine()) != null) {
            json_str = json_str + line;
        }
        in.close();
        jsonArray = new JSONArray(json_str);
        count_legit_terms = 0 ;
        for (int k = 0 ; k < jsonArray.length() ; k++) {
            JSONObject obj = (JSONObject) jsonArray.get(k);
            String trg_term = (String) obj.get("word");
            //String synonymous_score= (String) obj.get("score");
            String termData;
            if ( stemming_semantic ) trg_term = stem(trg_term);
            termData = terms_dictionary.get(trg_term);
            if (termData == null)  // try capital term
                termData = terms_dictionary.get(trg_term.toUpperCase());
            if (termData == null)// the term isnt in the corpus
                continue;
            /** set a threshhold for term relavence by score !!! ***/
            //synonymous_terms.put(synonymous_term, synonymous_score);
            if (!synonymous_terms.contains(trg_term) && !trigers_terms.contains(trg_term)){

                trigers_terms.add(trg_term);
                count_legit_terms++;
            } else continue;
            if (count_legit_terms == MAX_TRG_TERMS_FROM_API ) //save only the MAX_SYN_TERMS top terms
                break;
        }
    }

    /**
     * stem only one word and return the stem word
     * @param synonymous_term
     * @return
     */
    private String stem(String synonymous_term) {

        String final_term = "";
        String[] split = synonymous_term.split(" ");
        for ( String s :split
                ) {
            Stemmer stemmer = new Stemmer();
            stemmer.add(s.toCharArray(), s.length());
            stemmer.stem();
            final_term+= stemmer.toString() +" ";
        }
        return final_term.substring(0 , final_term.length()-1) ;

    }

    /**
     * return only the relevant docs to the given query terms
     * @param queryTerms all the query terms including api results
     * @return
     */
    private HashMap<String, HashMap<String, ArrayList<String>>> getRelevantDocs(ArrayList<String> queryTerms) {
        HashMap<String, HashMap<String, ArrayList<String>>> queryTermsToDocsWithDetails = new HashMap<>();
        for (int i = 0; i < queryTerms.size(); i++) {
            Posting.initTermPosting(posting, stemming);
            String queryTerm = queryTerms.get(i);
            HashMap<String, ArrayList<String>> specificQueryTermDocs = new HashMap<>();
            try {
                ArrayList<String> listTermDocs = getTermDocs(queryTerm);
                TreeMap<Integer, String> linesInPostingFile = new TreeMap<>();
                for (int j = 0; j < listTermDocs.size(); j++) {
                    String currDocWithTf = listTermDocs.get(j);
                    int indexOfSeparator = StringUtils.indexOf(currDocWithTf, "|");
                    String docNo = StringUtils.substring(listTermDocs.get(j), 0, indexOfSeparator);
                    if (citiesConstraint) {
                        if (!legalDocs.contains(docNo))
                            continue;
                    }
                    if (!docs_dictionary.containsKey(docNo))
                        continue;
                    String strDocPointer = docs_dictionary.get(docNo);
                    int intDocPointer = Integer.parseInt(strDocPointer);
                    linesInPostingFile.put(intDocPointer, currDocWithTf);
                }
                int firstPointer = 0;
                for (Map.Entry<Integer, String> entry : linesInPostingFile.entrySet()) {
                    int pointer = entry.getKey();
                    String currDocWithTf = entry.getValue();
                    String docPostingLine = Posting.getDocPostingLineByPointer(pointer - firstPointer);
                    firstPointer = pointer;
                    String[] splited = StringUtils.split(docPostingLine, "[");
                    String docHeaders = "[" + splited[1];
                    String[] firstPartSplited = StringUtils.split(splited[0], ",");
                    /* docDetails = mostFreqTerm, mostFreqTermAppearanceNum, uniqueTermsNum, fullDocLength */
                    if (firstPartSplited.length < 7 )
                        continue;
                    String length = firstPartSplited[6];
                    if (length.equals("NULL"))
                        length = firstPartSplited[7];
                    String docDetails = firstPartSplited[2] + "," + firstPartSplited[3] + "," + firstPartSplited[4] + "," + length;
                    ArrayList<String> currDocDetails = new ArrayList<>();
                    currDocDetails.add(docDetails);
                    currDocDetails.add(docHeaders);
                    specificQueryTermDocs.put(currDocWithTf, currDocDetails);
                }
                queryTermsToDocsWithDetails.put(queryTerm, specificQueryTermDocs);
            } catch (Exception e) {
                System.out.println("Problematic term: " + queryTerm);
                e.printStackTrace();
            }
        }
        return queryTermsToDocsWithDetails;
    }

    /**
     * The method returns the names of the documents that contain the term.
     * It also returns the number of occurrences of the term in the document.
     * @param term
     * @return [FT932-8691|6, LA122589-0084|1, ....]
     * @throws IOException
     */
    public ArrayList<String> getTermDocs (String term) throws IOException {
        ArrayList<String> listTermDocs = new ArrayList<>();
        if (term.charAt(0) == '*')
            term = term.substring(1);
        String dictionaryTermLine = terms_dictionary.get(term); // ancestor,FBIS3-40057,1,2,2,6376427
        if (dictionaryTermLine == null)
            dictionaryTermLine = terms_dictionary.get(term.toUpperCase());
        String[] splitLine = StringUtils.split(dictionaryTermLine, ",");
        if (splitLine == null)
            return listTermDocs;
        String strPointer = splitLine[splitLine.length - 1];
        int intPointer = Integer.parseInt(strPointer);
        String termPostingLine = Posting.getTermPostingLineByPointer(intPointer);
        String[] docsWithTf = StringUtils.split(termPostingLine, "#"); // =
        listTermDocs.addAll(Arrays.asList(docsWithTf));
        return listTermDocs;
    }
}
