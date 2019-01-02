/***
 * This class is responsible for rating all documents suspected to be relevant to a particular query.
 * The rating is based on various factors that can be determined in the final fields defined at the beginning of the class.
 */
package Engine.Model;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Ranks the Docs returned from a Query according to a formula we decided and developed to get the best Retrivel results
 */
public class Ranker {
    private final int MAX_DOCS_TO_RETURN = 50; // The require number of docs to return
    /* Ranking factors and their weight*/
    private final double BM25_TITLE_FACTOR_WEIGHT = 0.70;
    private final double BM25_DESCRIPTION_FACTOR_WEIGHT = 0.05;
    private final double TITLE_IN_HEADERS_FACTOR_WEIGHT = 0.2;
    private final double DESC_IN_HEADERS_FACTOR_WEIGHT = 0.05;

    /* BM25 Constants*/
    private final double K = 1.5;
    private final double B = 0.37;
    private final double AVG_LENGTH_OF_DOCS_IN_CORPUS;
    private final int NUM_OF_DOCS_IN_CORPUS;

    private TreeMap<String, Double> rankedDocs;
    private TreeMap<String, Double> BM25_QueryTitleWeight;
    private TreeMap<String, Double> BM25_QueryDescriptionWeight;
    private TreeMap<String, Double> QueryTitleTermInHeaders;
    private TreeMap<String, Double> QueryDescTermInHeaders;
    static StringBuilder sb_queriesResults = new StringBuilder();
    static StringBuilder sb_trecResults = new StringBuilder();
    static private BufferedWriter results_bw;


    static {
        try {
            results_bw = new BufferedWriter(new FileWriter("C:\\Users\\bardanad\\queriesTests\\results\\results.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private ArrayList<String> originalTitleTerms;
    private ArrayList<String> originalDescTerm;

    /**
     * Ranks the Docs returned from a Query according to a formula we decided and developed to get the best Retrivel results
     */
    Ranker(int numberOfDocsInCorpus, double avgDocsLength) {
        this.NUM_OF_DOCS_IN_CORPUS = numberOfDocsInCorpus;
        this.AVG_LENGTH_OF_DOCS_IN_CORPUS = avgDocsLength;
        rankedDocs = new TreeMap<>();
        BM25_QueryTitleWeight = new TreeMap<>();
        BM25_QueryDescriptionWeight = new TreeMap<>();
        QueryTitleTermInHeaders = new TreeMap<>();
        QueryDescTermInHeaders = new TreeMap<>();
    }

    public static String getQueriesResults() {
        return sb_queriesResults.toString();
    }

    /**
     * The function returns the identification numbers of the documents found relevant to the query.
     * The format to be returned is supported by treceval
     * @return
     */
    public static String getTrecFormatResults() {
        String results = sb_trecResults.toString();
        sb_trecResults = new StringBuilder();
        sb_queriesResults = new StringBuilder();
        return results;
    }

    /**
     *init the rank procces
     * @param queryId
     * @param relevantDocsByTitle
     * @param queryOtherTerms
     * @param originalTitleTerms
     * @param queryDescTerms
     * @return
     */
    public ArrayList<String> getRankDocs(String queryId, HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsByTitle, ArrayList<String> queryOtherTerms, ArrayList<String> originalTitleTerms, ArrayList<String> queryDescTerms){ // queryOtherTerms is title/desc.
        this.originalTitleTerms = originalTitleTerms;
        this.originalDescTerm = queryDescTerms;
        calculateWeights(relevantDocsByTitle, queryOtherTerms);
        mergeValues();
        ArrayList<String> ans = getSortedDocs();
        if (Model.status)
            System.out.println("Finish query number: " + queryId);
        printResultToFile(ans, queryId);
        return ans;
    }

    /**
     * After we have stored the values ​​of the various documents according to a specific factor in a specific data structure
     * We would like to sum these values ​​to a single value on which we will base the documents.
     */
    private void mergeValues() {
        for (Object o : BM25_QueryTitleWeight.entrySet()){
            Map.Entry<String, Double> docWithValue = (Map.Entry<String, Double>) o;
            String docNo = docWithValue.getKey();
            Double bm25ClassicWeight = docWithValue.getValue();
            Double bm25DescriptionWeight = 0.0;
            Double titleTermInHeadersWeight = 0.0;
            Double descTermInHeadersWeight = 0.0;
            if (BM25_QueryDescriptionWeight.containsKey(docNo)){
                bm25DescriptionWeight = BM25_QueryDescriptionWeight.get(docNo);
                if (bm25DescriptionWeight == null)
                    bm25DescriptionWeight = 0.0;
            }

            if (QueryTitleTermInHeaders.containsKey(docNo)){
                titleTermInHeadersWeight = QueryTitleTermInHeaders.get(docNo);
                if (titleTermInHeadersWeight == null)
                    titleTermInHeadersWeight = 0.0;
            }
            if (QueryDescTermInHeaders.containsKey(docNo)){
                descTermInHeadersWeight = QueryTitleTermInHeaders.get(docNo);
                if (descTermInHeadersWeight == null)
                    descTermInHeadersWeight = 0.0;
            }


            double bm25Classic = BM25_TITLE_FACTOR_WEIGHT * bm25ClassicWeight * 1.83;
            double bm25Description = BM25_DESCRIPTION_FACTOR_WEIGHT * bm25DescriptionWeight;
            double titleTermInHeader = TITLE_IN_HEADERS_FACTOR_WEIGHT * titleTermInHeadersWeight*37;
            double descTermInHeader = DESC_IN_HEADERS_FACTOR_WEIGHT * descTermInHeadersWeight*23;

            double mergedValue = bm25Classic + bm25Description + titleTermInHeader + descTermInHeader;

            rankedDocs.put(docNo,mergedValue);
        }

    }

    /**
     * This function prints the results of the queries according to
     * a format supported by the TREC EVAL software.
     * It also prints the results to be displayed to the GUI user
     * @param ans
     * @param queryId
     */
    private void printResultToFile(ArrayList<String> ans,String queryId) {
        try {
            sb_queriesResults.append("----QUERY ID: ").append(queryId).append("----").append("\n");
            for (int i = 0; i < ans.size(); i++) {
                results_bw.append(queryId).append(" ").append("0").append(" ").
                        append(ans.get(i)).append(" ").append("1").append(" ").append("float-sim").append(" ").append("mt").append("\n");
                sb_queriesResults.append(i+1).append(". ").append(ans.get(i)).append("\n");
                sb_trecResults.append(queryId).append(" ").append("0").append(" ").
                        append(ans.get(i)).append(" ").append("1").append(" ").append("float-sim").append(" ").append("mt").append("\n");
            }
            results_bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    //HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsForEachQueryTerm; // <QueryTerm, <DocNo|tf, [DocDetails, DocHeaders]>>
        /* DocDetails = mostFreqTerm, mostFreqTermAppearanceNum, uniqueTermsNum, fullDocLength
           DocHeaders = [headerTerm, headerTerm, ... ] */

    /**
     * calculate the diffrent mesures for each doc , and then sends to other funcs to calc the value for the query terms
     * @param relevantDocs
     * @param queryOtherTerms
     */
    private void calculateWeights(HashMap<String, HashMap<String, ArrayList<String>>> relevantDocs, ArrayList<String> queryOtherTerms) {
        HashMap<String, ArrayList<String>> allDocsHeaders = new HashMap<>(); // For future use (after the for loop)
        for (Object o1 : relevantDocs.entrySet()) {
            Map.Entry pair = (Map.Entry) o1; // <DescTerm, <HashMap<DocNo|tf, [DocDetails, DocHeaders]>>>
            String queryTitleTerm = (String) pair.getKey();
            HashMap<String, ArrayList<String>> docs = (HashMap<String, ArrayList<String>>) pair.getValue();
            int termDf = docs.size();
            for (Object o : docs.entrySet()) {
                Map.Entry doc = (Map.Entry) o; // <DocNo|tf, [DocDetails, DocHeaders]>>
                String key = (String) doc.getKey();
                String docNo = StringUtils.substringBefore(key, "|");
                String strTf = StringUtils.substringAfter(key, "|");
                int tf = Integer.parseInt(strTf);
                ArrayList<String> value = (ArrayList<String>) doc.getValue(); // ArrayList<String> = [DocDetails, DocHeaders]
                String docDetails = value.get(0);
                String docHeaders = value.get(1);
                if (!allDocsHeaders.containsKey(docNo))
                    allDocsHeaders.put(docNo, createHeaderArray(docHeaders));
                String[] docDetailsSplited = StringUtils.split(docDetails, ",");
                String strDocLength = docDetailsSplited[3];
//                System.out.println("Ranker" + " " + docNo + " " + strDocLength);
                if (!Character.isDigit(strDocLength.charAt(0))) {
                    if (docDetailsSplited.length >= 5)
                        strDocLength = docDetailsSplited[4];
                    else
                        continue;
                }
                String mode = "";
                int docLength = Integer.parseInt(strDocLength);
                if (queryOtherTerms.contains(queryTitleTerm)) {
                    mode = "BOTH"; // The description term is title term as well.
                }
                addBM25ValueToDoc(docNo, tf, docLength, termDf, mode, queryTitleTerm);
            }
        }
        /* Handle headers weight calculates */
        for (int i = 0; i < queryOtherTerms.size(); i++) {
            String queryTitleTerm = queryOtherTerms.get(i);
            if (queryTitleTerm.charAt(0) == '*')
                queryOtherTerms.set(i, StringUtils.substring(queryTitleTerm, 1));
        }

        Set<String> queryDescTerms = relevantDocs.keySet();
        Set<String> relevantDocsWithHeaders = getRelevantDocsWithHeaders(queryDescTerms, queryOtherTerms);
        for (String docNum : relevantDocsWithHeaders){
            ArrayList<String> docHeaders = allDocsHeaders.get(docNum);
            calculateHeadersWeight(docNum, docHeaders, queryDescTerms, queryOtherTerms);
        }
    }

    /**
     * return id's of relavent docs - which have at least on one of their headlines the certein term
     * @param queryDescTerms
     * @param queryTitleTerms
     * @return
     */
    private Set<String> getRelevantDocsWithHeaders(Set<String> queryDescTerms, ArrayList<String> queryTitleTerms) {
        HashSet<String> docsNum = new HashSet<>();
        for (String queryDescTerm : queryDescTerms){
            if (queryDescTerm.charAt(0) == '*')
                queryDescTerm = StringUtils.substring(queryDescTerm, 1);
            String inDocsHeaderList = Searcher.headers_dictionary.get(queryDescTerm);
            if (inDocsHeaderList == null)
                continue;
            String[] tmpDocsNum = StringUtils.split(inDocsHeaderList, "#");
            docsNum.addAll(Arrays.asList(tmpDocsNum));
        }
        for (String queryTitleTerm : queryTitleTerms) {
            if (queryTitleTerm.charAt(0) == '*')
                queryTitleTerm = StringUtils.substring(queryTitleTerm, 1);
            String inDocsHeaderList = Searcher.headers_dictionary.get(queryTitleTerm);
            if (inDocsHeaderList == null)
                continue;
            String[] tmpDocsNum = StringUtils.split(inDocsHeaderList, "#");
            docsNum.addAll(Arrays.asList(tmpDocsNum));
        }
        return docsNum;
    }

    /**
     * calc the weight for each doc that has the certein term in its headlines
     * @param docNum
     * @param docHeaders
     * @param queryDescTerms
     * @param queryTitleTerms
     */
    private void calculateHeadersWeight(String docNum, ArrayList<String> docHeaders, Set<String> queryDescTerms, ArrayList<String> queryTitleTerms) {
        if (docHeaders == null || docHeaders.size() == 0)
            return;

        int queryDescInHeaderCounter = 0;
        double bonus = 0;
        double queryDescInHeaderValue;
        int headersSize = docHeaders.size();
        for (String queryDescTerm : queryDescTerms){
            if (queryDescTerm.charAt(0) == '*')
                queryDescTerm = StringUtils.substring(queryDescTerm, 1);
            if (docHeaders.contains(queryDescTerm)){
                queryDescInHeaderCounter++;
                if (StringUtils.split(queryDescTerm, " -").length > 1)
                    bonus += 2;
            }
        }
        queryDescInHeaderValue = ((double)queryDescInHeaderCounter/(double)headersSize);
        if (bonus != 0)
            queryDescInHeaderValue *= bonus;
        if (QueryDescTermInHeaders.containsKey(docNum)){
            double currValue = QueryDescTermInHeaders.get(docNum);
            double newValue = currValue + queryDescInHeaderValue;
            QueryDescTermInHeaders.put(docNum, newValue);
        }
        else
            QueryDescTermInHeaders.put(docNum, queryDescInHeaderValue);

        bonus = 0;
        int queryTitleInHeaderCounter = 0;
        double queryTitleInHeaderValue;
        for (String queryTitleTerm : queryTitleTerms) {
            if (queryTitleTerm.charAt(0) == '*')
                queryTitleTerm = StringUtils.substring(queryTitleTerm, 1);
            if (docHeaders.contains(queryTitleTerm)){
                queryTitleInHeaderCounter++;
                if (StringUtils.split(queryTitleTerm, " -").length > 1)
                    bonus += 2;
            }
        }
        queryTitleInHeaderValue = ((double)queryTitleInHeaderCounter/(double)headersSize*(double)bonus);
        if (bonus != 0)
            queryDescInHeaderValue *= bonus;
        if (QueryTitleTermInHeaders.containsKey(docNum)){
            double currValue = QueryTitleTermInHeaders.get(docNum);
            double newValue = currValue + queryTitleInHeaderValue;
            QueryTitleTermInHeaders.put(docNum, newValue);
        }
        else
            QueryTitleTermInHeaders.put(docNum, queryDescInHeaderValue);
    }

    // DocHeaders = [headerTerm, headerTerm, ... ]
    private ArrayList<String> createHeaderArray(String docHeaders) {
        ArrayList<String> ans = new ArrayList<>();
        docHeaders = StringUtils.substring(docHeaders, 1, docHeaders.length()-1); // trim the '[ ]'
        String[] headersTerms = StringUtils.split(docHeaders, ",");
        for (int i = 0; i < headersTerms.length; i++) {
            String header = headersTerms[i];
            if (StringUtils.startsWith(header, " "))
                header = StringUtils.substring(header,1);
            if (header.charAt(0) == '*')
                header = StringUtils.substring(header,1);

            ans.add(header);
        }
        return ans;
    }

    /**
     * calc the value of BM25 saprately for title and Description of a query
     * @param docNo
     * @param tf
     * @param docLength
     * @param df
     * @param mode
     * @param queryTitleTerm
     */
    private void addBM25ValueToDoc(String docNo, int tf, int docLength, int df, String mode, String queryTitleTerm) {
        double bm25Value = ( ( ( (K + 1) * tf ) / ( tf + K * (1 - B + B * docLength/AVG_LENGTH_OF_DOCS_IN_CORPUS) ) )
                * Math.log((NUM_OF_DOCS_IN_CORPUS + 1) / df));
//        double bm25Value = ( ( ( (K + 1) * tf ) / ( tf + K * (1 - B + B * docLength/AVG_LENGTH_OF_DOCS_IN_CORPUS) ) )
//                * Math.log((NUM_OF_DOCS_IN_CORPUS - df + 0.5) / df + 0.5));
        if (originalTitleTerms.contains(queryTitleTerm)) {
            bm25Value *= 2;
        }

        if (BM25_QueryTitleWeight.get(docNo) != null){
            double currValue = BM25_QueryTitleWeight.get(docNo);
            currValue += bm25Value;
            BM25_QueryTitleWeight.put(docNo, currValue);
        }
        else
            BM25_QueryTitleWeight.put(docNo, bm25Value);

        if (mode.equals("BOTH")){
            if (BM25_QueryDescriptionWeight.get(docNo) != null){
                double currValue = BM25_QueryDescriptionWeight.get(docNo);
                currValue += bm25Value;
                BM25_QueryDescriptionWeight.put(docNo, currValue);
            }
            else
                BM25_QueryDescriptionWeight.put(docNo, bm25Value);
        }
    }

    /**
     * return the top 50 most relevant docs
     * @return
     */
    private ArrayList<String> getSortedDocs() {
        ArrayList<String> ans = new ArrayList<>();
        TreeSet<Map.Entry<String, Double>> sortedSet = entriesSortedByValues(rankedDocs);

        for (int i = 0; i < MAX_DOCS_TO_RETURN; i++) {
            if (i > sortedSet.size())
                break;
            Map.Entry pair = sortedSet.pollLast();
            if (pair == null)
                continue;
            ans.add((String)pair.getKey());
        }
        return ans;
    }


    public static <K,V extends Comparable<? super V>> TreeSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
        TreeSet<Map.Entry<K,V>> sortedEntries = new TreeSet<>(
                (e1, e2) -> {
                    int res = e1.getValue().compareTo(e2.getValue());
                    return res != 0 ? res : 1;
                }
        );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }

    /**
     * reset all String Builders
     */
    public static void clearSB() {
        sb_queriesResults.delete(0,sb_queriesResults.length());
        sb_queriesResults.setLength(0);
        sb_trecResults.delete(0 , sb_queriesResults.length());
        sb_trecResults.setLength(0);
    }
}