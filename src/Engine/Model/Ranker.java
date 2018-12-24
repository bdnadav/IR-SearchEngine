package Engine.Model;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Ranker {
    private final int MAX_DOCS_TO_RETURN = 50;

    /* Ranking factors and their weight*/
    private final double BM25_WEIGHT_CLASSIC = 0.5;
    private final double BM25_WEIGHT_DESCRIPTION = 0.25;
    private final double IN_TITLE_WEIGHT = 0.25;

    /* BM25 Constants*/
    private final double K = 1.5;
    private final double B = 0.75;
    private final double AVG_LENGTH_OF_DOCS_IN_CORPUS;
    private final int NUM_OF_DOCS_IN_CORPUS;

    private TreeMap<String, Double> rankedDocs;
    private TreeMap<String, Double> BM25_ClassicWeight;
    private TreeMap<String, Double> BM25_DescriptionWeight;
    private TreeMap<String, Double> QueryTermInTitleWeight;
    private TreeMap<String, Double> DescTermInTitleWeight;
    static private BufferedWriter results_bw;

    static {
        try {
            results_bw = new BufferedWriter(new FileWriter("C:\\Users\\Nadav\\QueriesTests\\results\\results.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    Ranker(int numberOfDocsInCorpus, double avgDocsLength) {
        this.NUM_OF_DOCS_IN_CORPUS = numberOfDocsInCorpus;
        this.AVG_LENGTH_OF_DOCS_IN_CORPUS = avgDocsLength;
        rankedDocs = new TreeMap<>();
        BM25_ClassicWeight = new TreeMap<>();
        BM25_DescriptionWeight = new TreeMap<>();
        QueryTermInTitleWeight = new TreeMap<>();
        DescTermInTitleWeight = new TreeMap<>();
    }

    public ArrayList<String> getRankDocs(HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsByQuery,
                                         HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsByDesc,
                                         String queryId){
        calculateBM25AndInHeadersWeight(relevantDocsByQuery, "Query");
        calculateBM25AndInHeadersWeight(relevantDocsByDesc, "Description");
        mergeValues();
        ArrayList<String> ans = getSortedDocs();
        System.out.println("Finish query number: " + queryId);
        printResultToFile(ans, queryId);
        return ans;
    }

    private void mergeValues() {
        for (Object o : BM25_ClassicWeight.entrySet()){
            Map.Entry<String, Double> docWithValue = (Map.Entry<String, Double>) o;
            String docNo = docWithValue.getKey();
            Double bm25ClassicWeight = docWithValue.getValue();
            Double bm25DescriptionWeight = 0.0;
            Double queryTermInHeadersWeight = 0.0;
            Double descTermInHeadersWeight = 0.0;
            if (BM25_DescriptionWeight.containsKey(docNo)){
                bm25DescriptionWeight = BM25_DescriptionWeight.get(docNo);
            }
            if (QueryTermInTitleWeight.containsKey(docNo)){
                queryTermInHeadersWeight = QueryTermInTitleWeight.get(docNo);
            }
            if (DescTermInTitleWeight.containsKey(docNo)){
                descTermInHeadersWeight = QueryTermInTitleWeight.get(docNo);
            }
            double bm25Classic = BM25_WEIGHT_CLASSIC * bm25ClassicWeight;
            double bm25Description = BM25_WEIGHT_DESCRIPTION * bm25DescriptionWeight;
            double inTitle = IN_TITLE_WEIGHT * (queryTermInHeadersWeight + descTermInHeadersWeight);

            double mergedValue = bm25Classic + bm25Description + inTitle;

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

    private void calculateBM25AndInHeadersWeight(HashMap<String, HashMap<String, ArrayList<String>>> relevantDocs, String mode) {
        HashMap<String, ArrayList<String>> docsHeaders = new HashMap<>();
        for (Object o1 : relevantDocs.entrySet()) {
            Map.Entry pair = (Map.Entry) o1; // <QueryTerm, <HashMap<DocNo|tf, [DocDetails, DocHeaders]>>>
            String queryTerm = (String) pair.getKey();
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
                docsHeaders.put(docNo, createHeaderArray(docHeaders));
                String[] docDetailsSplited = StringUtils.split(docDetails, ",");
                String strDocLength = docDetailsSplited[3];
//                System.out.println("Ranker" + " " + docNo + " " + strDocLength);
                int docLength = Integer.parseInt(strDocLength);
                addBM25ValueToDoc(docNo, tf, docLength, termDf, mode);
            }
        }
        TreeMap<String, Double> tm;
        if (mode.equals("Query"))
            tm = QueryTermInTitleWeight;
        else
            tm = DescTermInTitleWeight;
        Set<String> queryTerms = relevantDocs.keySet();
        for (Object o : docsHeaders.entrySet()){
            Map.Entry docHeaders = (Map.Entry) o;
            String docNo = (String) docHeaders.getKey();
            if (!tm.containsKey(docNo)){
                ArrayList<String> headers = (ArrayList<String>) docHeaders.getValue();
                calculateHeadersWeight(docNo, queryTerms, headers, mode);
            }
        }
    }

    private void calculateHeadersWeight(String docNo, Set<String> queryTerms, ArrayList<String> headers, String mode) {
        TreeMap<String, Double> tm;
        if (mode.equals("Query"))
            tm = QueryTermInTitleWeight;
        else
            tm = DescTermInTitleWeight;
        int counter = 0;
        int headersSize = headers.size();
        for (String term : queryTerms){
            if (headers.contains(term))
                counter++;
        }
        double value = 0.0;
        if (headersSize != 0)
            value = counter/headersSize;
        tm.put(docNo, value);
    }

    // DocHeaders = [headerTerm, headerTerm, ... ]
    private ArrayList<String> createHeaderArray(String docHeaders) {
        ArrayList<String> ans = new ArrayList<>();
        docHeaders = StringUtils.substring(docHeaders, 1, docHeaders.length()-1); // trim the '[ ]'
        String[] headersTerms = StringUtils.split(docHeaders, ",");
        ans.addAll(Arrays.asList(headersTerms));
        return ans;
    }


    private void addBM25ValueToDoc(String docNo, int tf, int docLength, int df, String mode) {
        TreeMap<String, Double> tm;
        if (mode.equals("Query"))
            tm = BM25_ClassicWeight;
        else
            tm = BM25_DescriptionWeight;
        double bm25Value = ( ( ( (K + 1) * tf ) / ( tf + K * (1 - B + B * docLength/AVG_LENGTH_OF_DOCS_IN_CORPUS) ) )
                * Math.log((NUM_OF_DOCS_IN_CORPUS + 1) / df));
        if (tm.get(docNo) != null){
            double currValue = tm.get(docNo);
            currValue += bm25Value;
            tm.put(docNo, currValue);
        }
        else
            tm.put(docNo, bm25Value);
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


    private <K,V extends Comparable<? super V>> TreeSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
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
