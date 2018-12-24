package Engine.Model;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Ranker {
    private final int MAX_DOCS_TO_RETURN = 50;

    /* Ranking factors and their weight*/
    private final double BM25_WEIGHT = 0.6;
    private final double IN_TITLE_WEIGHT = 0.4;

    /* BM25 Constants*/
    private final double K = 1.5;
    private final double B = 0.75;
    private final double AVG_LENGTH_OF_DOCS_IN_CORPUS;
    private final int NUM_OF_DOCS_IN_CORPUS;

    private TreeMap<String, Double> rankedDocs;


    Ranker(int numberOfDocsInCorpus, double avgDocsLength) {
        this.NUM_OF_DOCS_IN_CORPUS = numberOfDocsInCorpus;
        this.AVG_LENGTH_OF_DOCS_IN_CORPUS = avgDocsLength;
        rankedDocs = new TreeMap<>();
    }

    public ArrayList<String> getRankDocs(HashMap<String, HashMap<String, ArrayList<String>>> relevantDocs, String queryId){
        rankDocs(relevantDocs);
        ArrayList<String> ans = getSortedDocs();
        System.out.println("status");
        printResultToFile(ans, queryId);
        return ans;
    }

    private void printResultToFile(ArrayList<String> ans,String queryId) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("C:\\Users\\Nadav\\IdeaProjects\\IR-SearchEngine\\IR-SearchEngine\\src\\Engine\\resources\\queryResults.txt"));
            for (int i = 0; i < ans.size(); i++) {
                bw.append(queryId).append(" ").append("0").append(" ").
                        append(ans.get(i)).append(" ").append("1").append(" ").append("float-sim").append(" ").append("mt").append("\n");
            }
            bw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsForEachQueryTerm; // <QueryTerm, <DocNo|tf, [DocDetails, DocHeaders]>>
        /* DocDetails = mostFreqTerm, mostFreqTermAppearanceNum, uniqueTermsNum, fullDocLength
           DocHeaders = [headerTerm, headerTerm, ... ] */

    private void rankDocs(HashMap<String, HashMap<String, ArrayList<String>>> relevantDocs) {
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
                String[] docDetailsSplited = StringUtils.split(docDetails, ",");
                String strDocLength = docDetailsSplited[3];
                int docLength = Integer.parseInt(strDocLength);


                addBM25ValueToDoc(docNo, tf, docLength, termDf);
                addInHeadlinesValueToDoc(docNo, queryTerm, docHeaders);
            }
        }
//        it_queryTerms.remove(); // avoids a ConcurrentModificationException
    }
    /* DocHeaders = [headerTerm, headerTerm, ... ] */
    private void addInHeadlinesValueToDoc(String docNo, String queryTerm, String docHeaders) {

    }


    private void addBM25ValueToDoc(String docNo, int tf, int docLength, int df) {
        double bm25Value = ( ( ( (K + 1) * tf ) / ( tf + K * (1 - B + B * docLength/AVG_LENGTH_OF_DOCS_IN_CORPUS) ) )
                * Math.log((NUM_OF_DOCS_IN_CORPUS + 1) / df));
        if (rankedDocs.get(docNo) != null){
            double currValue = rankedDocs.get(docNo);
            currValue += bm25Value;
            rankedDocs.put(docNo, currValue);
        }
        else
            rankedDocs.put(docNo, bm25Value);
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
