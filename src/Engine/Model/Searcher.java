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


public class Searcher {
    private final int MAX_DOCS_TO_RETURN = 50;
    private final boolean useSemantic;

    private Parse queryParse;
    private Parse descParse;
    private TreeMap<String, String> terms_dictionary;
    private TreeMap<String, Pair> cities_dictionary;
    private TreeMap<String, String> docs_dictionary;
    private HashMap<String , String> synonymous_terms;
    public static HashMap<String, String> headers_dictionary;
    private Ranker ranker;
    private ArrayList<String> speceficCities;
    private HashSet<String> legalDocs; // If cities constraint, this data structure will hold all the docs whose can be return.
    private boolean citiesConstraint;
    private String posting;


    public Searcher(String posting, Boolean stemming, ArrayList<String> specificCities, TreeMap<String, String> termsDic, TreeMap<String, String> docsDic, TreeMap<String, Pair> citiesDic, HashMap<String, String> headersDictionary, boolean semantic) {
        this.terms_dictionary = termsDic;
        this.synonymous_terms = new HashMap<>();
        //this.synonymous_and_pointers = new HashMap<>() ;
        this.cities_dictionary = citiesDic;
        this.docs_dictionary = docsDic;
        this.headers_dictionary = headersDictionary;
        this.ranker = new Ranker(docsDic.size(), 250);
        this.queryParse = new Parse(posting, stemming);
        this.descParse = new Parse(posting, stemming);
        this.posting = posting;
        this.useSemantic = semantic ;
        Posting.initTermPosting(posting);
        if (specificCities != null && !specificCities.isEmpty()) {
            citiesConstraint = true;
            this.speceficCities = specificCities;
            legalDocs = getLegalDocs(specificCities);
        }
    }

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



    public ArrayList<String> handleQuery(String query_id, String queryTitle, String queryDescription, String queryNarrative) {
        resetAllDateStructures() ; //a new query has arrived - cleanall
        queryParse.parseQuery(queryTitle);
        ArrayList<String> queryTitleTerms = queryParse.getQueryTerms();
        descParse.parseQuery(queryDescription);
        ArrayList<String> queryDescTerms = descParse.getQueryTerms();

        //queryDescTerms = filterDescTerms(queryDescTerms);
        HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsByQueryTitleTerms; // <QueryTerm, <DocNo|tf, [DocDetails, DocHeaders]>>
        HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsByQueryDescTerm; // <DescTerm, <DocNo|tf, [DocDetails, DocHeaders]>>
        /* DocDetails = mostFreqTerm, mostFreqTermAppearanceNum, uniqueTermsNum, fullDocLength
           DocHeaders = [headerTerm, headerTerm, ... ] */

        /** Handle Semantic **/
        if ( this.useSemantic) {
            //if ( this.useSemantic){
            getSemanticTerms(queryTitleTerms);
        }

        if (extraTermsMayHelp(queryTitleTerms, queryDescTerms)){
            ArrayList<String> queryDescTermsToAdd = getExtraTerms(queryDescTerms, queryTitleTerms);
            queryTitleTerms.addAll(queryDescTermsToAdd);
        }

        relevantDocsByQueryTitleTerms = getRelevantDocs(queryTitleTerms);
        //Posting.initTermPosting(posting);
        //relevantDocsByQueryDescTerm = getRelevantDocs(queryDescTerms);

        String queryId = query_id; // need to change

        //ArrayList<String> rankedDocs = ranker.getRankDocs(relevantDocsByQueryTitleTerm, relevantDocsByQueryDescTerm, queryId);
        //ArrayList<String> rankedDocs = ranker.getRankDocs(query_id, relevantDocsByQueryDescTerm, queryTitleTerms);
        ArrayList<String> rankedDocs = ranker.getRankDocs(query_id, relevantDocsByQueryTitleTerms, queryDescTerms);
        // NEED TO DO: Create SubSet of rankedDocs according to the final integer MAX_DOCS_TO_RETURN
        return rankedDocs;
    }

    private void resetAllDateStructures() {
        this.synonymous_terms.clear();
        this.synonymous_terms = new HashMap<>();

    }

    private ArrayList<String> getExtraTerms(ArrayList<String> queryDescTerms, ArrayList<String> queryTitleTerms) {
        ArrayList<String> ans = new ArrayList<>();
        queryDescTerms.removeAll(queryTitleTerms);
        int termsExtra = queryDescTerms.size()/2;
        TreeMap<Integer, String> dfOfTerms = new TreeMap<>();
        for (int i = 0; i < queryDescTerms.size(); i++) {
            String term = queryDescTerms.get(i);
            int df = getTermDf(term);
            dfOfTerms.put(df, term);
        }
        for (int i = 0; i < termsExtra; i++) {
            ans.add(dfOfTerms.pollFirstEntry().getValue());
        }
        return ans;
    }

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
        int ans = Integer.parseInt(strDf);
        return ans;
    }

    private boolean extraTermsMayHelp(ArrayList<String> queryTitleTerms, ArrayList<String> queryDescTerms) {
        if (queryDescTerms.size() >= 2 * queryTitleTerms.size())
            return true;
        return false;
    }

    private Map<String, List<Pair<String,String>>> getSemanticTerms(ArrayList<String> queryTerms) {
        for (String term :queryTerms
             ) {
            try {
                term = Parse.cleanToken(term) ; // clean *
                useUrlSemantic(term); // insert to  synonymous map all the terms and their docs

                /** do somthing **/

            }
            catch (Exception e ){}
        }
        return  null ;
    }

    /**
     * check for each term in query if the is synonymouse terms in the api , check if any of them
     * in the dic terms , if they are in the func will save the pointer to the term's posting row
     * @param term
     * @throws Exception
     */
    private void useUrlSemantic(String term) throws Exception {
        URL url = new URL("https://api.datamuse.com/words?rel_trg=" + term);
        //URLConnection connection = website.openConnection();
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
        for (int k = 0 ; k < jsonArray.length() ; k++) {
            JSONObject obj = (JSONObject) jsonArray.get(k);
            String synonymous_term= (String) obj.get("word");
            String synonymous_score= (String) obj.get("score");
            String termData ;
            termData = terms_dictionary.get(synonymous_term);
            if ( termData == null )  // try capital term
               termData = terms_dictionary.get(synonymous_term.toUpperCase());
            if ( termData == null )// the term isnt in the corpus
                continue;
            /** set a threshhold for term relavence by score !!! ***/
            synonymous_terms.put(synonymous_term, synonymous_score);
            count_legit_terms++ ;

            if (count_legit_terms == 5 ) //save only the 5 top terms
                break;
        }


        //return null ;
    }

    private ArrayList<String> filterDescTerms(ArrayList<String> descTerms) {
        ArrayList<String> ans = new ArrayList<>();
        TreeMap<Integer, String> termDfInCorpus = new TreeMap<>();
        for (String term : descTerms) {
            if (term.charAt(0) == '*')
                term = StringUtils.substring(term, 1);
            int df = getTermTf(term);
            termDfInCorpus.put(df, term);
        }
        for (int i = 0; i < 3; i++) {
            ans.add(termDfInCorpus.pollFirstEntry().getValue());
        }

//        for (Object o : termDfInCorpus.entrySet()){
//            Map.Entry<Integer, String> dfWithTerm = (Map.Entry<Integer, String>) o;
//            int df = dfWithTerm.getKey();
//            if (df > 10 && df < docs_dictionary.size()/5)
//                ans.add(dfWithTerm.getValue());
//            if (ans.size() > 2)
//                break;
//        }
        return ans;
    }

    private int getTermTf(String term) {
        String dicTermLine = terms_dictionary.get(term);
        if (dicTermLine == null)
            dicTermLine = terms_dictionary.get(term.toUpperCase());
        if (dicTermLine == null)
            return -1;
        String[] split = StringUtils.split(dicTermLine, ",");
        if (split.length < 5)
            return -1;
        String strDf = split[3];
        int ans = Integer.parseInt(strDf);
        return ans;
    }


    private HashMap<String, HashMap<String, ArrayList<String>>> getRelevantDocs(ArrayList<String> queryTerms) {
        HashMap<String, HashMap<String, ArrayList<String>>> queryTermsToDocsWithDetails = new HashMap<>();
        for (int i = 0; i < queryTerms.size(); i++) {
            Posting.initTermPosting(posting);
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
//                    System.out.println(currDocWithTf + " " + firstPartSplited[6]);
                    ArrayList<String> currDocDetails = new ArrayList<>();
                    currDocDetails.add(docDetails);
                    currDocDetails.add(docHeaders);
                    specificQueryTermDocs.put(currDocWithTf, currDocDetails);
                }
                queryTermsToDocsWithDetails.put(queryTerm, specificQueryTermDocs);
            } catch (Exception e) {
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

        /**
         * For the document received its identification number,
         * The method will return the five most dominant entities in this document ranked in order of importance.
         * An entity is defined as: an expression that is reserved as only uppercase letters.
         * If the document has less than five entities, all will be returned.
         * @param docId
         * @return
         */
        public SortedSet<String> getDocDominantEntities (String docId){
            return null;
        }


    }
