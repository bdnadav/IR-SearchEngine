package Engine.Model;

import org.apache.commons.lang3.StringUtils;

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

    TreeMap<String, Double> rankedDocs;


    public Ranker(int numberOfDocsInCorpus, double avgDocsLength) {
        this.NUM_OF_DOCS_IN_CORPUS = numberOfDocsInCorpus;
        this.AVG_LENGTH_OF_DOCS_IN_CORPUS = avgDocsLength;
        rankedDocs = new TreeMap<>();
    }

    public ArrayList<String> getRankDocs(HashMap<String, HashMap<String, ArrayList<String>>> relevantDocs){
        rankDocs(relevantDocs);
        ArrayList<String> ans = getSortedDocs();
        System.out.println("status");
        System.out.println(rankedDocs.firstEntry());
        return ans;
    }

    //HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsForEachQueryTerm; // <QueryTerm, <DocNo|tf, [DocDetails, DocHeaders]>>
        /* DocDetails = mostFreqTerm, mostFreqTermAppearanceNum, uniqueTermsNum, fullDocLength
           DocHeaders = [headerTerm, headerTerm, ... ] */

    public void rankDocs(HashMap<String, HashMap<String, ArrayList<String>>> relevantDocs) {
        Iterator it_queryTerms = relevantDocs.entrySet().iterator();
        while (it_queryTerms.hasNext()) {
            Map.Entry pair = (Map.Entry) it_queryTerms.next(); // <QueryTerm, <HashMap<DocNo|tf, [DocDetails, DocHeaders]>>>
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
        SortedSet<Map.Entry<String, Double>> sortedSet = entriesSortedByValues(rankedDocs);
        for (int i = 0; i < MAX_DOCS_TO_RETURN; i++) {
            if (i > sortedSet.size())
                break;
            Map.Entry pair = sortedSet.first();
            ans.add((String)pair.getKey());
        }
        return ans;
    }


    static <K,V extends Comparable<? super V>> SortedSet<Map.Entry<K,V>> entriesSortedByValues(Map<K,V> map) {
    SortedSet<Map.Entry<K,V>> sortedEntries = new TreeSet<>(
            (e1, e2) -> {
                int res = e1.getValue().compareTo(e2.getValue());
                return res != 0 ? res : 1;
            }
    );
        sortedEntries.addAll(map.entrySet());
        return sortedEntries;
    }
}
