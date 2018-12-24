package Engine.Model;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

public class Searcher {
    private final int MAX_DOCS_TO_RETURN = 50;

    private Parse parse;
    private TreeMap<String, String> terms_dictionary;
    private TreeMap<String, String> cities_dictionary;
    private TreeMap<String, String> docs_dictionary;
    private Ranker ranker;
    private ArrayList<String> speceficCities;
    private HashSet<String> legalDocs; // If cities constraint, this data structure will hold all the docs whose can be return.
    private boolean citiesConstraint;
    private String posting;

    public Searcher(String posting, Boolean stemming, ArrayList<String> specificCities, TreeMap<String, String> termsDic, TreeMap<String, String> docsDic, TreeMap<String, String> citiesDic) {
        this.terms_dictionary = termsDic;
        this.cities_dictionary = citiesDic;
        this.docs_dictionary = docsDic;
        this.ranker = new Ranker(docsDic.size(), 250);
        this.parse = new Parse(posting, stemming);
        this.posting = posting;
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
            String citiesDicValue = cities_dictionary.get(currCity);
            String[] splited = StringUtils.split(citiesDicValue, "#");
            for (int j = 1; j < splited.length; j++) {
                int indexOfSeparator = StringUtils.indexOf(splited[j], "|");
                String docNo = StringUtils.substring(splited[j], 0, indexOfSeparator);
                ans.add(docNo);
            }
        }
        return ans;
    }



    public ArrayList<String> handleQuery(String query_id, String query, String desc, String narr) {
        parse.parseQuery(query);
        ArrayList<String> queryTerms = parse.getQueryTerms();
        HashMap<String, HashMap<String, ArrayList<String>>> relevantDocsForEachQueryTerm; // <QueryTerm, <DocNo|tf, [DocDetails, DocHeaders]>>
        /* DocDetails = mostFreqTerm, mostFreqTermAppearanceNum, uniqueTermsNum, fullDocLength
           DocHeaders = [headerTerm, headerTerm, ... ] */
        relevantDocsForEachQueryTerm = getRelevantDocs(queryTerms);

        String queryId = query_id; // need to change

        ArrayList<String> rankedDocs = ranker.getRankDocs(relevantDocsForEachQueryTerm, queryId);
        // NEED TO DO: Create SubSet of rankedDocs according to the final integer MAX_DOCS_TO_RETURN
        return rankedDocs;
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
                    int index = 6;
                    if (firstPartSplited.length == 6)
                        index = 5;
                    else if (firstPartSplited.length < 6){
                        System.out.println("Problematic DocNo: " + currDocWithTf + " ");
                        continue;
                    }
                    String docDetails = firstPartSplited[2] + "," + firstPartSplited[3] + "," + firstPartSplited[4] + "," + firstPartSplited[index];
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
