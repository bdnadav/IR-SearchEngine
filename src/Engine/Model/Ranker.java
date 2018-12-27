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

public class Ranker {
    private final int MAX_DOCS_TO_RETURN = 50;
    /* Ranking factors and their weight*/
    private final double BM25_TITLE_FACTOR_WEIGHT = 1;
    private final double BM25_DESCRIPTION_FACTOR_WEIGHT = 0.0;
    private final double TITLE_IN_HEADERS_FACTOR_WEIGHT = 0.0;
    private final double DESC_IN_HEADERS_FACTOR_WEIGHT = 0.0;

    /* BM25 Constants*/
    private final double K = 2.5;
    private final double B = 0.37;
    private final double AVG_LENGTH_OF_DOCS_IN_CORPUS;
    private final int NUM_OF_DOCS_IN_CORPUS;

    private TreeMap<String, Double> rankedDocs;
    private TreeMap<String, Double> BM25_QueryTitleWeight;
    private TreeMap<String, Double> BM25_QueryDescriptionWeight;
    private TreeMap<String, Double> QueryTitleTermInHeaders;
    private TreeMap<String, Double> QueryDescTermInHeaders;
    static private BufferedWriter results_bw;

    static {
        try {
            results_bw = new BufferedWriter(new FileWriter("C:\\Users\\harelsa\\QueriesTests\\results\\results.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Ranker(int numberOfDocsInCorpus, double avgDocsLength) {
        this.NUM_OF_DOCS_IN_CORPUS = numberOfDocsInCorpus;
        this.AVG_LENGTH_OF_DOCS_IN_CORPUS = avgDocsLength;
        rankedDocs = new TreeMap<>();
        BM25_QueryTitleWeight = new TreeMap<>();
        BM25_QueryDescriptionWeight = new TreeMap<>();
        QueryTitleTermInHeaders = new TreeMap<>();
        QueryDescTermInHeaders = new TreeMap<>();
    }

    public ArrayList<String> getRankDocs(String queryId, HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsByTitle, ArrayList<String> queryOtherTerms){ // queryOtherTerms is title/desc.
        calculateWeights(relevantDocsByTitle, queryOtherTerms);
        mergeValues();
        ArrayList<String> ans = getSortedDocs();
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


            double bm25Classic = BM25_TITLE_FACTOR_WEIGHT * bm25ClassicWeight * 1.5;
            double bm25Description = BM25_DESCRIPTION_FACTOR_WEIGHT * bm25DescriptionWeight;
            double titleTermInHeader = TITLE_IN_HEADERS_FACTOR_WEIGHT * titleTermInHeadersWeight*75;
            double descTermInHeader = DESC_IN_HEADERS_FACTOR_WEIGHT * descTermInHeadersWeight*75;

            double mergedValue = bm25Classic + bm25Description + titleTermInHeader + descTermInHeader;

            rankedDocs.put(docNo,mergedValue);
        }

    }

    private void printResultToFile(ArrayList<String> ans,String queryId) {
        try {
            for (int i = 0; i < ans.size(); i++) {
                results_bw.append(queryId).append(" ").append("0").append(" ").
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
                addBM25ValueToDoc(docNo, tf, docLength, termDf, mode);
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

    private void calculateHeadersWeight(String docNo, String term, ArrayList<String> headers, String mode) {
        int counter = 0;
        int headersSize = headers.size();
        if (headers.contains(term))
            counter++;
        double value = 0;
        if (headersSize != 0)
            value = (counter/headersSize)*10;
        if (QueryDescTermInHeaders.containsKey(docNo)){
            double currValue = QueryDescTermInHeaders.get(docNo);
            double newValue = currValue + value;
            QueryDescTermInHeaders.put(docNo, newValue);
        }
        else
            QueryDescTermInHeaders.put(docNo, value);
        if (mode.equals("BOTH")){
            if (QueryTitleTermInHeaders.containsKey(docNo)){
                double currValue = QueryTitleTermInHeaders.get(docNo);
                double newValue = currValue + value;
                QueryTitleTermInHeaders.put(docNo, newValue);
            }
            else
                QueryTitleTermInHeaders.put(docNo, value);
        }
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


    private void addBM25ValueToDoc(String docNo, int tf, int docLength, int df, String mode) {
        double bm25Value = ( ( ( (K + 1) * tf ) / ( tf + K * (1 - B + B * docLength/AVG_LENGTH_OF_DOCS_IN_CORPUS) ) )
                * Math.log((NUM_OF_DOCS_IN_CORPUS + 1) / df));

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

    private ArrayList<String> getSortedDocs() {
        ArrayList<String> ans = new ArrayList<>();
        TreeSet<Map.Entry<String, Double>> sortedSet = entriesSortedByValues(rankedDocs);

        for (int i = 0; i < MAX_DOCS_TO_RETURN; i++) {
            if (i > sortedSet.size())
                break;
            Map.Entry pair = sortedSet.pollLast();
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
}
