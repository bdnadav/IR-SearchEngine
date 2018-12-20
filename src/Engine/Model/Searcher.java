package Engine.Model;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;

public class Searcher {
    private Parse parse;
    private TreeMap<String, String> terms_dictionary;
    private TreeMap<String, String> cities_dictionary;
    private TreeMap<String, String> docs_dictionary;
    private Posting posting;
    private Ranker ranker;

    public Searcher(TreeMap<String, String> terms_dictionary, TreeMap<String, String> cities_dictionary, TreeMap<String, String> docs_dictionary, String posting, Boolean stemming) {
        this.terms_dictionary = terms_dictionary;
        this.cities_dictionary = cities_dictionary;
        this.docs_dictionary = docs_dictionary;
        parse = new Parse(posting, stemming);
        this.posting = new Posting(posting);
    }

    private SortedSet<String> handleQuery (String query){
        ArrayList<String> queryTerms = parse.parse(query, null);
        Set<String> relevantDocs = getRelevantDocs(queryTerms);
        SortedSet<String> rankedDocs = ranker.rankDocs(relevantDocs, queryTerms);
        return  rankedDocs;
    }

    private Set<String> getRelevantDocs(ArrayList<String> queryTerms) {
        Set<String> docs = new HashSet<>();
        for (int i = 0; i < queryTerms.size(); i++) {
            String term = queryTerms.get(i);
            try {
                ArrayList<String> listTermDocs = getTermDocs(term);
                docs.addAll(listTermDocs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return docs;
    }

    public ArrayList<String> getTermDocs(String term) throws IOException {
        ArrayList<String> listTermDocs = new ArrayList<>();
        String dictionaryTermLine = terms_dictionary.get(term); // ancestor,FBIS3-40057,1,2,2,6376427
        String[] splitLine = StringUtils.split(dictionaryTermLine, ",");
        String strPointer = splitLine[splitLine.length-1];
        int intPointer = Integer.parseInt(strPointer);
        String termPostingLine = Posting.getTermPostingLineByPointer(intPointer);
        String[] docsWithTf = StringUtils.split(termPostingLine, "#"); // =
        listTermDocs.addAll(Arrays.asList(docsWithTf));
        return listTermDocs;
    }
}
