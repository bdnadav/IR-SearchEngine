package Engine.Model;

/**
 * This class represents a Posting File.
 * The Posting File contains additional information
 * we need to save for keys that are in a particular index.
 * We save this information in a text file and link the
 * relevant information to the keys by adding a pointer in the dictionary,
 * in our case the pointer will be represented
 * by a line number of the relevant information in posting file.
 */

import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.*;


public class Posting {
    private static int docsPointer = 1;
    private static int termsPointer = 1;
    private static int counter = 0;
    private static int docsCounter = 0;
    private static BufferedWriter terms_buffer_writer;
    private static BufferedWriter documents_buffer_writer;

    public Posting(String postingsPath) {
        String termsPostingPath = postingsPath + "\\Terms\\termsPosting.txt";
        try {
            terms_buffer_writer = new BufferedWriter(new FileWriter(termsPostingPath));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static void initPosting(String postingPath) {
        try {
            documents_buffer_writer = new BufferedWriter(new FileWriter(postingPath + "\\docsPosting.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes to the disk the list of terms that are contained in the TreeMap.
     * In addition, the method adds the terms to the term dictionary.
     * @param termDocs A collection of terms that are sorted in alphabetical order
     * @param ifTermStartsWithCapital A Hashmap that provides an indication of whether
     * we should keep a certain term in upper or lower case letters.
     * The size of this data structure will be the size of the data structure termDocs
     */
    void writeToTermsPosting(HashMap<String, String> termDocs, HashMap<String, Boolean> ifTermStartsWithCapital) {
        for (Object o : termDocs.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            String key = pair.getKey().toString(); // term
            String listOfDocs = (String) pair.getValue(); // list of docs
            //String listOfDocs = sb.toString();
            String termDetails = getMostFreqDocAndTotalTf(listOfDocs); // "<D>"<DOC-NO>","<MaxTf>","<TotalTf>,<df>
            String[] termDetailsSplited = StringUtils.split(termDetails, ",");
            int df = Integer.parseInt(termDetailsSplited[termDetailsSplited.length - 1]);
            int totalTf = Integer.parseInt(termDetailsSplited[termDetailsSplited.length - 2]);
            // Filtering low tf & df terms
            if ((df == 1 && totalTf < 3)) {
                continue;
            }

            if (ifTermStartsWithCapital.containsKey(key) && ifTermStartsWithCapital.get(key))
                key = key.toUpperCase();
            Indexer.terms_dictionary.put(key, termDetails + "," + termsPointer);
            termsPointer += 2;
            if (CorpusProcessingManager.cities.containsKey(key.toLowerCase()) && !key.toLowerCase().equals("china")) {
                Indexer.cities_dictionary.put(key, listOfDocs);
            }
            try {
                terms_buffer_writer.append(key).append('\n');
                counter++;
                terms_buffer_writer.append(listOfDocs).append(String.valueOf('\n'));
                counter++;

                if (counter > 500) {
                    terms_buffer_writer.flush();
                    counter = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * The method accepts a string of a collection of docs in which a particular term is contained.
     * And returns the following information:
     *      1. Which document in the document collection contains
     *          the highest number of instances from Term.
     *      2. How many documents there are in the collection
     *      3. What is the total number of occurrences
     *          of Term within these documents.
     * @param listOfTermDocs string of a collection of docs in which a particular term is contained
     * @return "<D>"<DOC-NO>","<MaxTf>","<TotalTf>,<df>
     */
    private String getMostFreqDocAndTotalTf(String listOfTermDocs) { // return "<D>"<DOC-NO>","<MaxTf>","<TotalTf>,<df>
        String[] docs = StringUtils.split(listOfTermDocs, "#");
        int df = getDf(listOfTermDocs);
        int maxTf = 0;
        int totalTf = 0;
        String docNoOfMax = "";
        for (int i = 0; i < docs.length; i++) {
            String[] splited = StringUtils.split(docs[i], "|");
            if (splited.length < 2)
                continue;
            int tmp = Integer.parseInt(splited[1]);
            totalTf += tmp;
            if (tmp > maxTf) {
                maxTf = tmp;
                docNoOfMax = splited[0];
            }
        }
        return docNoOfMax + "," + maxTf + "," + totalTf + "," + df;
    }



    private int getDf(String listOfTermDocs) {
        int count = 0;

        for (int i = 0; i < listOfTermDocs.length(); i++) {
            if (listOfTermDocs.charAt(i) == '#')
                count++;
        }

        return count;
    }

    public static void closeIO() {
        try {
            if (documents_buffer_writer != null)
                documents_buffer_writer.close();
            if (terms_buffer_writer != null)
                terms_buffer_writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is responsible for writing to Docs,
     * it is synchronized because it is called a method that is run by a thread number at the same time.
     */
    synchronized public static void writeToDocumentsPosting(String docNo, String parentFileName, String mostFreqTerm, int tf_mft, int numOfUniqueTerms, String city) {
        try {
            documents_buffer_writer.append(docNo + "," + parentFileName + "," + mostFreqTerm + "," + tf_mft + "," + numOfUniqueTerms + "," + city+ "\n");
            docsCounter++;
            if (docsCounter > 500) {
                documents_buffer_writer.flush();
                docsCounter = 0;
            }
            Indexer.addNewDocToDocDictionary(docNo, docsPointer);
            docsPointer++;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



