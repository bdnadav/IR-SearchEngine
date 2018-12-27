package Engine.Model;
/**
 * This class is responsible for creating the various indexes.
 * The methods in this class manage the creation of the Posting files for the terms index,
 * And the creation of dictionaries:
 *  1) Doc to Terms
 *  2) Term to Docs
 *  3) City to Docs
 */
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

class Indexer {
    static TreeMap<String, String> terms_dictionary;
    static TreeMap<String, String> cities_dictionary;
    static TreeMap<String, Integer> docs_dictionary;
    static HashMap<String, String> headers_dictionary;
    static HashMap<String, TreeMap<String, Integer>> docs_entities;
    static TreeMap<Integer, String> entitiesPointers;
    private static String staticPostingsPath;
    private static BufferedWriter termDictionary_bf;
    private static BufferedWriter headersDictionary_bw;
    private static BufferedWriter docsEntities_bw;



    static void initIndexer(String postingPath) {
        terms_dictionary = new TreeMap<>(new TermComparator());
        try {
            FileWriter termDictionary_fw = new FileWriter(postingPath + "\\termDictionary.txt");
            termDictionary_bf = new BufferedWriter(termDictionary_fw);
            FileWriter headersDictionary_fw = new FileWriter(postingPath + "\\headersDictionary.txt");
            headersDictionary_bw = new BufferedWriter(headersDictionary_fw);
            FileWriter docsEntities_fw = new FileWriter(postingPath + "\\docsEntities.txt");
            docsEntities_bw = new BufferedWriter(docsEntities_fw);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cities_dictionary = new TreeMap<>();
        docs_dictionary = new TreeMap<>(new DocComparator());
        headers_dictionary = new HashMap<>();
        staticPostingsPath = postingPath;
        docs_entities = new HashMap<>();
        entitiesPointers = new TreeMap<>();
    }

    private String[] chunksCurrLines;
    private BufferedReader[] segsReaders;
    private Posting termsPosting;
    private HashMap<String, Boolean> ifTermStartsWithCapital;


    Indexer(Posting termsPostingFile) {
        termsPosting = termsPostingFile;
        //docsPosting = docsPostingFile;
    }

    void startIndexerOperations() throws FileNotFoundException{
        appendSegmentPartitionRangeToPostingAndIndexes();
        buildDocsEntities();
        System.out.println("lala");

    }

    private void buildDocsEntities() {
        HashSet<String> getEntitiesChunkFromPosting;
        while ((getEntitiesChunkFromPosting = Posting.getChunkOfEntitiesLines()) != null) {
            for (String docEntityLine : getEntitiesChunkFromPosting) {
                TreeMap<String, Integer> ansEntities = new TreeMap<>();
                TreeMap<String, Integer> tmpEntitiesTm = new TreeMap();
                String[] splitToDocNoAndTmpEntities = StringUtils.split(docEntityLine, "|");
                String docNo = splitToDocNoAndTmpEntities[0];
                if (splitToDocNoAndTmpEntities.length < 2){
                    System.out.println("--FIRST BUG--");
                    System.out.println(docEntityLine);
                    System.out.println(docNo);
                    System.out.println("--END FIRST BUG--");
                    continue;
                }
                String tmpEntities = splitToDocNoAndTmpEntities[1];
                tmpEntities = StringUtils.substring(tmpEntities, 1, tmpEntities.length()-1);
                String[] entities = StringUtils.split(tmpEntities, ",");
                for (String entity : entities) {
                    String[] splitEntityAndTf = StringUtils.split(entity, "=");
                    String currEntity = splitEntityAndTf[0];
                    try {
                        String strTf = splitEntityAndTf[1];
                        if (currEntity.charAt(0) == ' ')
                            currEntity = StringUtils.substring(currEntity, 1);
                        int tf = Integer.parseInt(strTf);
                        tmpEntitiesTm.put(currEntity, tf);
                    }
                    catch (ArrayIndexOutOfBoundsException a){
                        System.out.println("--SECOND BUG--");
                        System.out.println(currEntity);
                        System.out.println(docEntityLine);
                        System.out.println("--END SECOND BUG--");
                    }
                }
                TreeSet<Map.Entry<String, Integer>> sortedByValue = Ranker.entriesSortedByValues(tmpEntitiesTm);
                while (ansEntities.size() < 5 && sortedByValue.size() > 0) {
                    Map.Entry lastEntry = sortedByValue.pollLast();
                    String currEntityWithKohavit = (String)lastEntry.getKey();
                    int tf = (int)lastEntry.getValue();
                    String currEntity = StringUtils.substring(currEntityWithKohavit, 1).toUpperCase();
                    if (terms_dictionary.containsKey(currEntity))
                        ansEntities.put(currEntity.toUpperCase(), tf);
                }
                docs_entities.put(docNo, ansEntities);
            }
        }
    }

    private void buildDocsEntitiesFromTermDictionary(){
        TreeMap<Integer, String > entitiesPointersFromDics = new TreeMap<>();
        Iterator termIt = terms_dictionary.entrySet().iterator();
        int counter = 0;
        while (termIt.hasNext()) {
            Map.Entry pair = (Map.Entry) termIt.next();
            try {
                String entity = (String)pair.getKey();
                if (Character.isUpperCase(entity.charAt(0))) {
                    String entityDetails = (String)pair.getValue();
                    String[] split = StringUtils.split(entityDetails, ",");
                    String strPointer = split[4];
                    int intPointer = Integer.parseInt(strPointer);
                    entitiesPointersFromDics.put(intPointer, entity);
                }
                termDictionary_bf.append(pair.getKey().toString()).append(",").append(pair.getValue().toString()).append("\n");
                counter++;
                if (counter > 400) {
                    termDictionary_bf.flush();
                    counter = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            termDictionary_bf.flush();
            termIt.remove(); // avoids a ConcurrentModificationException
        }
        termDictionary_bf.flush();
        termDictionary_bf.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    AtomicReference<FileWriter> docDictionary_fw = new AtomicReference<>(null);



}

    /**
     * This method is triggered by a number of threads as the number of Partitions we have divided into each Segment File.
     * Each thread is actually contains an instance of the indexer and is responsible for processing a specific alphabetic range of terms.
     * The following method makes preliminary processing of information in the Segment Files Partitions;
     * it creates a TreeMap array when the keys of each TreeMap is the Term value and the value is details about the Term.
     * Each TreeMap contains the information from one Segment File Partition.
     * After the information of all segment file partitions has been entered into the array, we send the array to write in posting.
     */
    void appendSegmentPartitionRangeToPostingAndIndexes() throws FileNotFoundException {
        ArrayList<String> chunksPath = getChunkPath();
        chunksCurrLines = new String[chunksPath.size()];
        segsReaders = new BufferedReader[chunksPath.size()];
        HashMap<String, String> termToDocs = new HashMap<>();
        ifTermStartsWithCapital = new HashMap<>();
        for (int i = 0; i < segsReaders.length; i++) {
            segsReaders[i] = new BufferedReader(new FileReader(chunksPath.get(i)));
            try {
                chunksCurrLines[i] = segsReaders[i].readLine(); // each readers reads it's first term.
                if (chunksCurrLines[i].charAt(0) == '*' && chunksCurrLines[i].length() > 1) {
                    chunksCurrLines[i] = StringUtils.substring(chunksCurrLines[i], 1);
                    if (!ifTermStartsWithCapital.containsKey(chunksCurrLines[i]))
                        ifTermStartsWithCapital.put(chunksCurrLines[i], true);
                } else if (chunksCurrLines[i].charAt(0) != '*') {
                    ifTermStartsWithCapital.put(chunksCurrLines[i], false);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        while (!finishToRead()) {

            while (termToDocs.size() < 500 || isContainsTheNextMin(termToDocs)) {
                String minTermDetails = getMinimum(chunksCurrLines);
                if (minTermDetails.contains("null"))
                    break;
                int termIsUntilIndex = StringUtils.indexOf(minTermDetails, "@");
                String term = StringUtils.substring(minTermDetails, 0, termIsUntilIndex);

                String listOfDocs = StringUtils.substring(minTermDetails, termIsUntilIndex + 1);
                if (termToDocs.containsKey(term)) {
                    String curValue = termToDocs.get(term);
                    String newValue = curValue + listOfDocs;
                    termToDocs.put(term, newValue);
                } else
                    termToDocs.put(term, listOfDocs);
            }
            termsPosting.writeToTermsPosting(termToDocs, ifTermStartsWithCapital);
            ifTermStartsWithCapital.clear();
            ifTermStartsWithCapital = new HashMap<>();
            termToDocs.clear();
            termToDocs = new HashMap<>();
        }
        for (BufferedReader segsReader : segsReaders) {
            try {
                segsReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private boolean isContainsTheNextMin(HashMap<String, String> termToDocs) {
        for (String chunksCurrLine : chunksCurrLines) {
            if (termToDocs.containsKey(chunksCurrLine))
                return true;
        }
        return false;
    }

    private String getMinimum(String[] chunksCurrLines) {
        String docsOfTerm = "";
        String minSoFar = ""; // The term it self
        for (String chunksCurrLine : chunksCurrLines) {
            if (chunksCurrLine != null && !chunksCurrLine.equals("")) {
                minSoFar = chunksCurrLine;
                break;
            }
        }
        int indexOfMin = 0;
        for (int i = 0; i < chunksCurrLines.length; i++) {
            if (chunksCurrLines[i] != null && minSoFar != null && chunksCurrLines[i].compareTo(minSoFar) < 1) {
                minSoFar = chunksCurrLines[i];
                indexOfMin = i;
            }
        }

        try {
            docsOfTerm = segsReaders[indexOfMin].readLine();  // The docs which contains the minimum term
            String nextTerm = segsReaders[indexOfMin].readLine(); // Hold the next term
            chunksCurrLines[indexOfMin] = nextTerm;
            if (chunksCurrLines[indexOfMin] != null) {
                if (nextTerm.charAt(0) == '*' && nextTerm.length() > 1) {
                    nextTerm = StringUtils.substring(nextTerm, 1);
                    chunksCurrLines[indexOfMin] = nextTerm;
                    if (!ifTermStartsWithCapital.containsKey(nextTerm))
                        ifTermStartsWithCapital.put(nextTerm, true);
                } else if (nextTerm.charAt(0) != '*') {
                    ifTermStartsWithCapital.put(nextTerm, false);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return minSoFar + "@" + docsOfTerm; // <Term>"@"<ListOfDocs>
    }

    private boolean finishToRead() {
        for (String chunksCurrLine : chunksCurrLines) {
            if (chunksCurrLine != null)
                return false;
        }
        return true;
    }


    static void writeDictionariesToDisc() {
        try {
            Iterator termIt = terms_dictionary.entrySet().iterator();
            int counter = 0;
            while (termIt.hasNext()) {
                Map.Entry pair = (Map.Entry) termIt.next();
                try {
                    termDictionary_bf.append(pair.getKey().toString()).append(",").append(pair.getValue().toString()).append("\n");
                    counter++;
                    if (counter > 400) {
                        termDictionary_bf.flush();
                        counter = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                termDictionary_bf.flush();
                termIt.remove(); // avoids a ConcurrentModificationException
            }
            termDictionary_bf.flush();
            termDictionary_bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        AtomicReference<FileWriter> docDictionary_fw = new AtomicReference<>(null);

        try {
            docDictionary_fw.set(new FileWriter(staticPostingsPath + "\\docDictionary.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert docDictionary_fw.get() != null;
        BufferedWriter docDictionary_bf = new BufferedWriter(docDictionary_fw.get());
        Iterator docIt = docs_dictionary.entrySet().iterator();
        int counter = 0;
        while (docIt.hasNext()) {
            Map.Entry pair = (Map.Entry) docIt.next();
            try {
                docDictionary_bf.append(pair.getKey().toString()).append(",").append(pair.getValue().toString()).append("\n");
                counter++;
                if (counter > 400) {
                    docDictionary_bf.flush();
                    counter = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            docIt.remove(); // avoids a ConcurrentModificationException
        }
        try {
            docDictionary_bf.flush();
            docDictionary_bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        terms_dictionary.clear();

        try {
            Iterator termIt = cities_dictionary.entrySet().iterator();
            BufferedWriter citiesDictionary_bf = new BufferedWriter(new FileWriter(staticPostingsPath + "\\citiesDictionary.txt"));
            counter = 0;
            while (termIt.hasNext()) {
                Map.Entry pair = (Map.Entry) termIt.next();
                try {
                    String key = pair.getKey().toString();
                    if (Character.isLowerCase(key.charAt(0)))
                        continue;
                    String cityDetailsFromApi = getCityDetailsFromApi(key);
                    citiesDictionary_bf.append(key).append(",").append(cityDetailsFromApi).append(",").append(pair.getValue().toString()).append("\n");
                    counter++;
                    if (counter > 400) {
                        citiesDictionary_bf.flush();
                        counter = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                termIt.remove(); // avoids a ConcurrentModificationException
            }
            citiesDictionary_bf.flush();
            citiesDictionary_bf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Iterator termIt = headers_dictionary.entrySet().iterator();
            counter = 0;
            while (termIt.hasNext()) {
                Map.Entry pair = (Map.Entry) termIt.next();
                try {
                    headersDictionary_bw.append(pair.getKey().toString()).append(",").append(pair.getValue().toString()).append("\n");
                    counter++;
                    if (counter > 400) {
                        headersDictionary_bw.flush();
                        counter = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                headersDictionary_bw.flush();
                termIt.remove(); // avoids a ConcurrentModificationException
            }
            headersDictionary_bw.flush();
            headersDictionary_bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Iterator termIt = docs_entities.entrySet().iterator();
            counter = 0;
            while (termIt.hasNext()) {
                Map.Entry pair = (Map.Entry) termIt.next();
                try {
                    docsEntities_bw.append(pair.getKey().toString()).append(",").append(pair.getValue().toString()).append("\n");
                    counter++;
                    if (counter > 400) {
                        docsEntities_bw.flush();
                        counter = 0;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                docsEntities_bw.flush();
                termIt.remove(); // avoids a ConcurrentModificationException
            }
            docsEntities_bw.flush();
            docsEntities_bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("writeDictionariesToDisc Done");
    }

    private static String getCityDetailsFromApi(String s) {
        StringBuilder sb = new StringBuilder();
        s = s.toLowerCase();
        boolean test = CorpusProcessingManager.cities.containsKey(s);
        if (test) {
            City city = CorpusProcessingManager.cities.get(s.toLowerCase());
            String currency = city.getCurrency();
            String pop = city.getPopulation();
            sb.append(currency).append(",").append(pop);
            return sb.toString();
        }
        return sb.toString();
    }


    synchronized static void addNewDocToDocDictionary(String docNo, int docValue) {
        docs_dictionary.put(docNo, docValue);
    }

    private ArrayList<String> getChunkPath() {
        ArrayList<String> filesPathsList = new ArrayList<>();
        final File folder = new File(staticPostingsPath + "\\Segment Files");
        for (final File fileEntry : folder.listFiles())
            filesPathsList.add(fileEntry.getPath());
        return filesPathsList;
    }

    public static class TermComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            String s1 = ((String) (o1));
            String s2 = ((String) (o2));
            if (s1.charAt(0) == '*')
                s1 = s1.substring(1);
            if (s2.charAt(0) == '*')
                s2 = s2.substring(1);
            return s1.compareTo(s2);
        }
    }

    public static class DocComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {
            String s1 = ((String) (o1));
            String s2 = ((String) (o2));
            return s1.compareTo(s2);
        }
    }
}



