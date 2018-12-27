package Engine.Model;


import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static Engine.Model.CorpusProcessingManager.ifStemming;

/**
 * will break each doc to terms , by getting a :
 * Segmentfile to Write ,an array of tokens from Current doc Text , Doc obj .
 * The class will use java patterns and conditions to find the right way the
 * term should be written to posting file . we are checking from long length patterns to short
 * this way we will not miss longer terms first
 */
public class Parse {

    private static int DOC_NUM =0;
    private static int TOTOAL_TERMS =0 ;
    private static final int CHUNK_SIZE = 50;
    // enums
    private static double THOUSAND = Math.pow(10, 3);
    private static double MILLION = Math.pow(10, 6);
    private static double BILLION = Math.pow(10, 9);
    private static double TRILLION = Math.pow(10, 12);
    private String posting_path;

    private String mostFreqTerm = "";
    private int tf_mft = 0;
    int num_unique_term = 0;
    int doc_length = 0 ;


    boolean debug = false;


    TreeMap<String, String> TermsOnly;
    HashMap<String, StringBuilder> FilesTerms;
    TreeMap<String, Integer> FBIS3_Terms;
    TreeSet HeadLinesTerms;
    TreeMap<String, Integer> potentialEntities;
    ArrayList <String> QueryTerms;

    String lastDoc = "";

    private static HashSet<String> stopwords = new HashSet<>(); // list of all stop words
    private static HashSet<String> specialwords = new HashSet<>(); // list of words might be in terms
    private static HashSet<String> specialchars = new HashSet<>(); // list of chars that will be
    // removed when term is cleaned
    private static HashSet<String> months = new HashSet<>(); // list of months

    private FileReader stopwords_fr; // read stop words from the file
    private static FileReader specialwords_fr;
    private static FileReader specialchars_fr;
    private static FileReader months_fr;

    String path;
    boolean stemming = false;

    private int termPosition; // counts the term position inside the doc text

    static {
//        try {
//            specialwords_fr = new FileReader("src\\Engine\\resources\\special_words.txt"); // read stop words from the file
//            specialchars_fr = new FileReader("src\\Engine\\resources\\special_chars.txt");
//            months_fr = new FileReader("src\\Engine\\resources\\months.txt");
//
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    private static Pattern NUMBER_ADDS = Pattern.compile("\\d+" + " " + "(Thousand|Million|Billion|Trillion|percent|percentage|Dollars)");
    private static Pattern PRICE_MBT_US_DOLLARS = Pattern.compile("\\d+" + " " + "(million|billion|trillion)" + " " + "U.S" + " " + "dollars");
    private static Pattern PRICE_DOU = Pattern.compile("\\d+" + "(m|bn) " + "(Dollars)");
    private static Pattern PRICE_FRACTION_DOLLARS = Pattern.compile("[0-9]*" + " " + "[0-9]*" + "/" + "[0-9]*" + " " + "Dollars");
    private static Pattern DATE_DD_MONTH = Pattern.compile(/*"(3[01]|[0-2][0-9])"*/"(3[0-1]|[0-2][0-9]|[0-9])" + " " + "(january|february|march|april|may|june|july|august|september|october|november|december|jan|fab|mar|apr|jun|jul|aug|sep|oct|nov|dec)");
    private static Pattern DATE_MONTH_DD = Pattern.compile("(january|february|march|april|may|june|july|august|september|october|november|december|jan|fab|mar|apr|jun|jul|aug|sep|oct|nov|dec)" + " " + "(3[0-1]|[0-2][0-9]|[0-9])$" /*"[0-9]{1,2}" /*"(3[0-1]|[0-2][0-9])" */);
    private static Pattern PRICE_SIMPLE = Pattern.compile("\\$" + "\\d+");
    private static Pattern FRACTURE_SIMPLE = Pattern.compile("[0-9]*" + " " + "[0-9]*" + "/" + "[0-9]*$");
    private static Pattern DATE_MONTH_YYYY = Pattern.compile("(january|february|march|april|may|june|july|august|september|october|november|december|jan|fab|mar|apr|jun|jul|aug|sep|oct|nov|dec)" + " " + "([1-2][0-9][0-9][0-9]|[0-9][0-9][0-9] )$"); /*"[0-9]{4}");*/
    private static Pattern REGULAR_NUM = Pattern.compile("^[0-9]*$");
    private static Pattern DOUBLE_NUM = Pattern.compile("^[0-9]*$" + "." + "^[0-9]*$");
    private static Pattern BETWEEN = Pattern.compile("\\d+" + "and" + "\\d+");


    public Parse(String path, boolean stemming , String corpusPath) {
        try {

            stopwords_fr = new FileReader(corpusPath + "\\stop_words.txt");
            this.posting_path = path + "\\Postings" + ifStemming(stemming);
            this.path = path;
            this.stemming = stemming;
            BufferedReader stopwords_br = new BufferedReader(stopwords_fr);
//            BufferedReader specialwords_br = new BufferedReader(specialwords_fr);
//            BufferedReader specialchars_br = new BufferedReader(specialchars_fr);
//           BufferedReader months_br = new BufferedReader(months_fr);
            FBIS3_Terms = new TreeMap<>();
            FilesTerms = new HashMap<>();
            HeadLinesTerms = new TreeSet() ;
            potentialEntities = new TreeMap();
            TermsOnly = new TreeMap<String, String>((Comparator) (o1, o2) -> {
                String s1 = ((String)(o1)).toLowerCase();
                String s2 = ((String)(o2)).toLowerCase();
                if (s1.charAt(0) == '*')
                    s1 = StringUtils.substring(s1, 1);
                if (s2.charAt(0) == '*')
                    s2 = StringUtils.substring(s2, 1);
                return s1.compareTo(s2);
            });
//            TermsOnly = new TreeMap<>();
            String curr_line;

            while ((curr_line = stopwords_br.readLine()) != null) {
                stopwords.add(curr_line);
            }
            String[] words = { "thousand","million","billion","trillion","percent","percentage","dollars","january","february","march","april","may","june","july","august","september","october","november","december","jan","feb","mar","apr","may","jun","jul","aug","sep","oct","nov","dec"} ;
            for (String s: words
            ) {
                specialwords.add(s);
            }
            String[] chars = { ",","}","{","\\","/",">","<","(",")","'",":",";","\"","[","]","&",".","?","|","`","*","+","!","~","@","#","^","-"} ;
            for (String s: chars
            ) {
                specialchars.add(s);
            }
            String[] months_arr = {"January","February","March","April","May","June","July","August","September","October","November","December","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"} ;
            for (String s: months_arr
            ) {
                months.add(s);
            }
            //SegmentFile parserSegmentFile = new SegmentFile();
            termPosition = 0;

        } catch (Exception e) {

        }


    }

    public static double getDocNum() {
        return DOC_NUM;
    }

    public static double getTotalTerms() {
        return TOTOAL_TERMS;
    }

    public void parseHeadLines(String text) {
        termPosition = 0;
        HeadLinesTerms = new TreeSet() ;
        //text = remove_stop_words(text);
        String[] tokens;
        tokens = StringUtils.split(text, "\\`:)?*(|+@#^;!&=}{[]'<> ");
        getTerms(tokens, null , "DocHeadline");


    }

    public void parseQuery(String text ) {
        QueryTerms = new ArrayList<>();
        termPosition = 0;
        //text = remove_stop_words(text);
        String[] tokens;
        tokens = StringUtils.split(text, "\\`:)?*(|+@#^;!&=}{[]'<> ");
        getTerms(tokens, null , "Query");


    }

    /**
     * passes the terms from the func getTerms and passes it to the segment file
     *
     * @param text    - doc's text
     * @param currDoc - cuur doc obj
     * @return
     */
    public void parse(String text, Document currDoc ) {
        termPosition = 0;
        potentialEntities = new TreeMap();
        //text = remove_stop_words(text);
        String[] tokens;
        tokens = StringUtils.split(text, "\\`:)?*(|+@#^;!&=}{[]'<> ");

        getTerms(tokens, currDoc ,"DocText");
//        if ( currDoc.docNo.equals("FBIS3-3366"))
//        printFBIS3ToFile() ;
        currDoc.updateAfterParsing();


    }

    private void printFBIS3ToFile() {
        String segmantPartitionFilePath = path + "\\FBIS3-3366_Terms" + ".txt";
        File newFile = new File(segmantPartitionFilePath);
        Map.Entry<String, Integer> en;
        try {
            BufferedWriter file_buffer_writer = new BufferedWriter(new FileWriter(segmantPartitionFilePath));

            newFile.createNewFile();
            while (!FBIS3_Terms.isEmpty()) {
                en = FBIS3_Terms.firstEntry();
                String term = en.getKey();
                int tf = en.getValue();
                file_buffer_writer.append(term + ": " + tf + "\n");
                FBIS3_Terms.remove(term);
            }
            file_buffer_writer.flush();
            file_buffer_writer.close();
        } catch (Exception e) {
        }


    }

    public void sendToSeg(int chunk) {
        // write to file
        //Thread seg = new Thread(() ->writeSeg(chunk));
        //seg.start();
        writeSeg(chunk);
    }

    public void writeSeg(int chunk) {

        SegmentFilePartition sfp = new SegmentFilePartition(posting_path, chunk);

        Iterator it = TermsOnly.keySet().iterator();
        //signNewDocSection(currDoc);
        while (it.hasNext()) {
            //String key = (String)it.next();
            String term = (String) it.next();
            StringBuilder value = FilesTerms.get(term);
            if (stemming) {
                Stemmer stemmer = new Stemmer();
                stemmer.add(term.toCharArray(), term.length());
                String stemmed = "";
                stemmer.stem();
                term = stemmer.toString();

            }
            sfp.signNewTerm(term, value);
            value.delete(0, value.length());
            value.setLength(0);
        }
        System.out.println(sfp.getPath());
        sfp.flushFile();
        sfp.closeBuffers();
    }

    /**
     * go through the tokens array and finds the right pattern to create
     * a legal Term
     *
     * @param tokensArray - doc text splited by ","
     * @param currDoc     the curr doc obj
     * @param
     * @return a sorted map of all the terms in curr doc text
     */
    private SortedMap<String, Term> getTerms(String[] tokensArray, Document currDoc, String type) {
        // < str_term , obj_term >  // will store all the terms in curpos
        //doc no. , perent ,term , tf , n,uniqueterm , pointer
        String addTerm = "";
        num_unique_term = 0;
        doc_length = 0 ;
        mostFreqTerm = "";
        tf_mft = 0;
        for (int i = 0; i < tokensArray.length; ) {

            addTerm = "";
            boolean is_joint_term = false;
            if (!isNumber(cleanToken(tokensArray[i]))
                    && (tokensArray[i].equals("") || tokensArray[i].length() < 2 || cleanToken(tokensArray[i]).length() < 2)) { // not a term
                i += 1;
                continue;
            }

            // catch point joint terms
            String temp_char = cleanToken(tokensArray[i]);
            if ((tokensArray[i].length() > 5 && Character.isUpperCase(temp_char.charAt(0)) || Character.isLowerCase(temp_char.charAt(0)))
                    && StringUtils.containsAny(temp_char, "?/\"\\':)(`*[}|{=&@~%+^;]#!,.<>")) {
                // break it to single words
                String[] arr = StringUtils.split(temp_char, "/\"\\`:)?*(|@;&%!=~+^}{#[],'.<>");

                tokensArray[i] = StringUtils.join(arr, ".", 1, arr.length);
                if (arr[0].length() > 2)
                    addTerm = cleanToken(arr[0]);
                else continue;
                is_joint_term = true;

            }
            //First law - save " phrase" - will be saved as phrase and single words
            if (addTerm.equals("") && tokensArray[i].startsWith("\"") && !tokensArray[i].endsWith("\"")) {
                int j = i;
                StringBuilder phrase = new StringBuilder(tokensArray[j]);
                j++;
                while (j < tokensArray.length && (j - i) < 6) {
                    if (tokensArray[j].endsWith("\"")) { // end of phrase
                        phrase = phrase.append(" " + tokensArray[j]);
                        String phrase_temp = phrase.toString();
                        phrase_temp = cleanToken(phrase_temp);
                        if (debug) System.out.println(phrase_temp);
                        String doc_num = "" ;
                        if ( currDoc!= null )
                            doc_num  = currDoc.docNo ;
                        addTermFunc(phrase_temp, doc_num , type);
                        break;
                    } else {
                        phrase = phrase.append(" " + tokensArray[j]);
                        j++;
                    }

                }
            }
            // Second law  - save terms of capitals letters - Ashley Cummins Brittingham
            String temp_token = cleanToken(tokensArray[i]);
            if (addTerm.equals("") && temp_token.length() > 1 && i < tokensArray.length - 1 && Character.isUpperCase(temp_token.charAt(0)) // check first letter is a capital
                    && !specialchars.contains(tokensArray[i].charAt(tokensArray[i].length() - 1)) //check Cummins,
                    && !specialchars.contains(tokensArray[i + 1].charAt(0)) // check ,Cummins
                    && cleanToken(tokensArray[i + 1]).length() > 1
                    && Character.isUpperCase(cleanToken(tokensArray[i + 1]).charAt(0)) // check capital of the second word
                    ) {
                int j = i;
                StringBuilder long_term = new StringBuilder();
                //j++ ;
                boolean stop = false;
                boolean insert_and_stop = false;
                String what_to_add = "";
                while (j < tokensArray.length && (j - i) < 6) {
                    temp_token = cleanToken(tokensArray[j]);
                    if (!insert_and_stop
                            && tokensArray[j].length() > 1
                            && !specialchars.contains(tokensArray[j].charAt(0))
                            && temp_token.length() > 1
                            && Character.isUpperCase(temp_token.charAt(0))
                            && (j < tokensArray.length - 1
                            && !(months.contains(tokensArray[j]) && isNumber(tokensArray[j + 1])))
                            ) {  // add one word term
                        long_term = long_term.append(temp_token + " ");
                        String doc_num = "" ;
                        if ( currDoc!= null )
                            doc_num  = currDoc.docNo ;
                        if (cleanToken(long_term.toString()).contains(" "))
                        addTermFunc(cleanToken(long_term.toString()), doc_num,type); //add part of long term
                        what_to_add = temp_token;
                        if (specialchars.contains(tokensArray[j].charAt(tokensArray[j].length() - 1))) // end
                            insert_and_stop = true;
                        j++;

                    } else { // end of long term
                        if (long_term.length() < 2) {
                            i = j;
                            break;
                        }
                        what_to_add = long_term.toString();
                        what_to_add = cleanToken(what_to_add);
                        stop = true;
                        i = j;
                    }
                    if (debug) System.out.println(what_to_add);
                    String doc_num = "" ;
                    if ( currDoc!= null )
                        doc_num  = currDoc.docNo ;
                    addTermFunc(what_to_add, doc_num ,type);
                    if (stop) break;
                }
                i = j;
                continue;
            }// second law


            if (addTerm.equals("") && !tokensArray[i].equals("") && tokensArray[i] != null)
                tokensArray[i] = cleanToken(tokensArray[i]);
            //tokensArray[i] = remove_stop_words(tokensArray[i]);
            // check stop word
            if (addTerm.equals("") && !tokensArray[i].equals("may") && stopwords.contains(tokensArray[i])) {
                i += 1;
                continue;
            }
            // check number with no special term
            if (isNumber(tokensArray[i]) && (i == tokensArray.length - 1 || (i < tokensArray.length - 1 && (!specialwords.contains(cleanToken(tokensArray[i + 1].toLowerCase())) || !tokensArray[i + 1].contains("/"))))) {

                if (addTerm.equals("")) addTerm = check1WordPattern(tokensArray[i]); //regular num
                addTerm = "";
            }
            // check between
            if (addTerm.equals("") && (tokensArray[i].equals("Between") || tokensArray[i].equals("between")) && i < tokensArray.length - 3) {
                Matcher bet = BETWEEN.matcher(tokensArray[i + 1] + tokensArray[i + 2] + tokensArray[i + 3]);
                if (bet.find()) {
                    addTerm = tokensArray[i] + " " + tokensArray[i + 1] + " " + tokensArray[i + 2] + " " + tokensArray[i + 3];
                    i = i + 3;
                }
            }
            //  check if its date first ..
            if (addTerm.equals("") && i < tokensArray.length - 1 && !isNumber(tokensArray[i])) {
                String temp_token1 = cleanToken(tokensArray[i + 1]);
                //date - < Month + decimal >
                Matcher dateFormatMatcher2 = DATE_MONTH_DD.matcher(tokensArray[i].toLowerCase() + " " + temp_token1.toLowerCase());
                if (dateFormatMatcher2.find()) {
                    String term = PairTokensIsDate2Format(tokensArray[i].toLowerCase(), temp_token1.toLowerCase());
                    //System.out.println("Term added: " + term);
                    addTerm = term;

                }
                //date - < Month + YYYY >
                Matcher dateFormatMatcherYear = DATE_MONTH_YYYY.matcher(tokensArray[i].toLowerCase() + " " + temp_token1.toLowerCase());
                if (addTerm.equals("") && dateFormatMatcherYear.find()) {
                    String term = PairTokensIsDate3Format(tokensArray[i].toLowerCase(), temp_token1.toLowerCase());
                    //System.out.println("Term added: " + term);
                    addTerm = term;

                }
                if (!addTerm.equals("")) i += 1;
                if (tokensArray[i].equals("may") && addTerm.equals("")) // stop word  - fix may
                {
                    i++;
                    continue;
                }
            }
            //  check if its $ or % ..
            if (addTerm.equals("") && (tokensArray[i].startsWith("$") || tokensArray[i].startsWith("%")) && i < tokensArray.length) {
                if (i < tokensArray.length - 1) {
                    String temp_token1 = cleanToken(tokensArray[i + 1]);
                    addTerm = check2WordsPattern(tokensArray[i], temp_token1);
                    if (!addTerm.equals("")) i += 1;
                }
                if (addTerm.equals("")) addTerm = check1WordPattern(tokensArray[i]);

            }
            // check a term with  num
            // Matcher regularNUMmatcher = REGULAR_NUM.matcher(tokensArray[i]);
            Matcher regularNUMmatcher2 = REGULAR_NUM.matcher(cleanToken(tokensArray[i].replaceAll("[mbn]", "")));
            if (addTerm.equals("") && (isNumber(tokensArray[i]) || regularNUMmatcher2.find() || isNumber(tokensArray[i].replaceAll("[mbn]", "")))) {  // change the term only if the first token is a number !!!!

                if (i < tokensArray.length - 3) {
                    String temp_token3 = cleanToken(tokensArray[i + 3]);
                    String temp_token2 = cleanToken(tokensArray[i + 2]);
                    String temp_token1 = cleanToken(tokensArray[i + 1]);
                    addTerm = check4WordsPattern(tokensArray[i], temp_token1, temp_token2, temp_token3);
                    if (!addTerm.equals("")) i += 3;

                }
                if (addTerm.equals("") && i < tokensArray.length - 2) {
                    String temp_token1 = cleanToken(tokensArray[i + 1]);
                    String temp_token2 = cleanToken(tokensArray[i + 2]);
                    addTerm = check3WordsPattern(tokensArray[i], temp_token1, temp_token2);
                    if (!addTerm.equals("")) i += 2;
                }
                if (addTerm.equals("") && i < tokensArray.length - 1) {
                    String temp_token1 = cleanToken(tokensArray[i + 1]);
                    addTerm = check2WordsPattern(tokensArray[i], temp_token1);
                    if (!addTerm.equals("")) i += 1;
                }
                if (addTerm.equals("") && i < tokensArray.length) {
                    addTerm = check1WordPattern(tokensArray[i]);
                }

            }
            //REGULAR WORD
            if (addTerm.equals("")) {
                if (!tokensArray[i].equals("F><F"))
                    addTerm = tokensArray[i];
            }

            if (StringUtils.containsAny(addTerm, "-")) {
                String[] term = StringUtils.split(addTerm, "-");
                for (String s : term
                        ) {
                    if (s.length() < 2) {
                        addTerm = "";
                        break;
                    }
                }
            }

            if (addTerm.length() < 2 || addTerm.equals("") || StringUtils.containsAny(addTerm, "?\"\\':)(`*[}|{=&@~%$+^;]#!<>")) {
                i++;
                continue;
            }

            if (debug) System.out.println(addTerm);


            String doc_num = "" ;
            if ( currDoc!= null )
                doc_num  = currDoc.docNo ;
            addTermFunc(addTerm, doc_num ,type);
            if (!is_joint_term)
                i++;
        }//end for
        if ( type.equals("DocText")) {
            updateAVL();
            writeToDocsFiles(currDoc.docNo, currDoc.getParentFileName(), mostFreqTerm, tf_mft, num_unique_term, currDoc.getCity());
        }
        return null;
    }

    public static double getAVL ( ){
        return TOTOAL_TERMS/DOC_NUM ;
        //return  250 ;
    }

    synchronized private void updateAVL() {
        TOTOAL_TERMS += doc_length;
        DOC_NUM++;
    }

    private void writeToDocsFiles(String docNo, String parentFileName, String mostFreqTerm, int tf_mft, int numOfUniqueTerms, String city) {
        if (mostFreqTerm == null || mostFreqTerm.equals("")){
            HeadLinesTerms.clear();
            return;
        }
        remove1TfEntities();
        Posting.writeToDocumentsPosting(docNo, parentFileName, mostFreqTerm, tf_mft, numOfUniqueTerms, city , HeadLinesTerms ,doc_length, potentialEntities);
        HeadLinesTerms.clear();
        potentialEntities.clear();
    }

    private void remove1TfEntities() {
        if (potentialEntities.size() > 5){
            Iterator termIt = potentialEntities.entrySet().iterator();
            while (termIt.hasNext()) {
                Map.Entry pair = (Map.Entry) termIt.next();
                if ((int) pair.getValue() < 2) {
                    termIt.remove();
                }
            }
        }
    }


    private synchronized void addTermFunc(String addTerm, String docNo , String type) {
        //help with testin for the word doc
//        if (docNo.equals("FBIS3-3366"))
//            addTo_FBIS3_Terms(addTerm);

        if (stopwords.contains(addTerm.toLowerCase()) || addTerm.toLowerCase().equals("xx") || addTerm.toLowerCase().equals("page"))
            return;
        StringBuilder sb = new StringBuilder();
        if (Character.isUpperCase(addTerm.charAt(0))) {
            addTerm = addTerm.toLowerCase();
            addTerm = "*" + addTerm;
        }

//        if ( FilesTerms == null ||FilesTerms.isEmpty()  )
//            return;
        // System.out.println( addTerm + " , " + docNo);




        if ( type.equals("DocHeadline") ) {
            HeadLinesTerms.add(addTerm);
            return;
        }

        if ( type.equals("Query")) {
            QueryTerms.add(addTerm);
            return;
        }






        if ( type.equals("DocText") ) {
            doc_length++;
            if (FilesTerms.containsKey(addTerm)) {
                StringBuilder value = FilesTerms.get(addTerm);
                //String[] docs = StringUtils.split(value.toString(), "#") ;
                //String[] getnum =StringUtils.split(docs[docs.length-1] , "|") ;
                int start = value.lastIndexOf("#");
                String doc = StringUtils.substring(value.toString(), start + 1, value.lastIndexOf("|"));
                //String doc = value.substring(start+1 , value.lastIndexOf("|"));
                if (!doc.equals(docNo)) { //new doc
                    sb.append(value);
                    value.append("#" + docNo + "|" + "1");
                } else { //existing doc
                    int num = 0;
                    //if ( isNumber(value.substring(value.lastIndexOf("|")+1 , value.length())))
                    if (isNumber((StringUtils.substring(value.toString(), StringUtils.lastIndexOf(value, "|") + 1, value.length()))))
                        //num= Integer.parseInt(value.substring(value.lastIndexOf("|")+1 , value.length())) ;
                        num = Integer.parseInt(StringUtils.substring(value.toString(), StringUtils.lastIndexOf(value, "|") + 1, value.length()));
                    else {
                        System.out.println("problem : " + value + addTerm);
                        return;
                    }
                    num++;
                    if (num >tf_mft) {
                        tf_mft = num;
                        mostFreqTerm = addTerm;
                    }
                    value.replace(value.lastIndexOf("|") + 1, value.length(), num + "");
                    //value = StringUtils.substring(value, 0 , value.length()-(getnum[1].length()+1));
                    //sb.append(value);
                    //sb.append(num) ;
                    //value= sb.toString() ;
                }
                FilesTerms.put(addTerm, value);
            } else {
                if (mostFreqTerm.equals("")){
                    mostFreqTerm = addTerm ;
                    tf_mft = 1 ;
                }
                num_unique_term++;
                sb.append("#" + docNo + "|" + "1");
                FilesTerms.put(addTerm, sb);
                TermsOnly.put(addTerm, addTerm);
            }
            if (addTerm.charAt(0) == '*' && StringUtils.split(addTerm, " ") != null && StringUtils.split(addTerm, " ").length > 1 && !StringUtils.containsAny(addTerm, "=,")){
                if (potentialEntities.containsKey(addTerm)) {
                    int count = potentialEntities.get(addTerm);
                    count += 1;
                    potentialEntities.put(addTerm, count);
                }
                else
                    potentialEntities.put(addTerm, 1);
            }
        }

        //sb.delete(0 , sb.length()) ;

    }

    private void addTo_FBIS3_Terms(String addTerm) {
        if (FBIS3_Terms.containsKey(addTerm)) {
            int n = FBIS3_Terms.get(addTerm);
            n++;
            FBIS3_Terms.put(addTerm, n);
        } else {
            FBIS3_Terms.put(addTerm, 1);

        }

    }

    /**
     * checks if a string is a number - by moving throuhg chars
     *
     * @param string
     * @return true / false
     */
    public static boolean isNumber(String string) {

        if (string == null || string.isEmpty()) {
            return false;
        }
        string = string.replaceAll(",", "");
        if (string.equals(""))
            return false;
        int i = 0;
        if (string.charAt(0) == '-') {
            if (string.length() > 1) {
                i++;
            } else {
                return false;
            }
        }
        for (; i < string.length(); i++) {
            if (string.charAt(i) == '.') continue;
            if (!Character.isDigit(string.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * get a string and remove access chars from both sided of the term
     *
     * @param token
     * @return clean token
     */
    public static String cleanToken(String token) {
        StringBuilder s = null;
        boolean changed = true;
        while (token != null && token.length() > 0 && !token.equals("") && changed) {
            changed = false;
            s = new StringBuilder(token);
            String a = " ";
            if (specialchars.contains("" + s.charAt(0)) || a.equals("" + s.charAt(0))) {
                if (s.charAt(0) == '-' && s.length() > 1 && isNumber(s.charAt(1) + ""))
                    continue;
                s.deleteCharAt(0);
                token = s.toString();
                changed = true;
            }
            if (token != null && token.length() > 0 && !token.equals("") && (specialchars.contains("" + s.charAt(s.length() - 1)) || a.equals("" + s.charAt(s.length() - 1)))) {
                s.deleteCharAt(s.length() - 1);
                changed = true;
            }
            token = s.toString();
        }
        return token;
        //return s.toString();
    }

    private boolean isExpression(String token) {
        if (token.equals("U.S."))
            return true;
        return false;
    }

    // the funcs handle a size i terms , by checking the next tokens of the curr token
    // the funcs getting calls from getTerms() func , according to conditions & patters checked
    private String check1WordPattern(String token) {
        if (token.equals("")) return "";

        String term;
        String originalToken = token;
        token = cleanToken(token);
        // < $number >

        if (token.startsWith("$")) {

            String temp = token.replace("$", "");
            temp = temp.replaceAll(",", "");
            //Matcher regularNUMmatcher = REGULAR_NUM.matcher(temp);
            if (isNumber(temp)) {
                term = get_term_from_simple_price(temp, originalToken);
                return term;
            }
        }
        //< number + % >
        if (token.endsWith("%")) {
            term = token;
            //System.out.println("Term added: " + term);
            return term;
        }

        // < number >
        //Matcher regularNUMmatcher = REGULAR_NUM.matcher(token);
        token = token.replaceAll(",", "");
        if (isNumber(token)) {
            //if (Character.isDigit(token.charAt(0))) {
            term = get_term_from_simple_number(token);
            //System.out.println("Term added: " + term);
            return term;
        }

        // < simple token - just add as is >
        term = token;
        //System.out.println("Term added: " + term);
        return term;
    }

    private String check2WordsPattern(String token1, String token2) {
        String term = "";
        String saved_original = token1;
//        token1 = cleanToken(token1);
//        token2 = cleanToken(token2);

        //datre < decimal + decimal\decimal  >
        Matcher fractureMatcher = FRACTURE_SIMPLE.matcher(token1 + " " + token2);
        if (fractureMatcher.find()) {
            term = token1 + " " + token2;
            //System.out.println("Term added: " + term);

            return term;
        }

        // check < $ + Decimal + million|billion >
        Matcher priceDouMatcher$ = PRICE_SIMPLE.matcher(token1);
        if (priceDouMatcher$.find() && token2.equals("million") || token2.equals("billion")) {
            String temp = "";
            BigDecimal value = new BigDecimal(0);
            temp = token1.replace("$", "");
            temp = cleanToken(temp);
            if (token2.equals("billion")) {
                temp = temp.replaceAll(",", "");
                try {
                    if (isNumber(temp)) value = new BigDecimal(Double.parseDouble(temp) * BILLION);
                } catch (Exception e) {

                }
            }
            if (token2.equals("million")) {
                temp = temp.replaceAll(",", "");
                if (isNumber(temp)) value = new BigDecimal(Double.parseDouble(temp) * MILLION);
            }

            term = get_term_from_simple_price(value + "", "");
            //System.out.println("Term added: " + term);
            return term;
        }

        // check < Decimal+m|bn + Dollars >
        Matcher priceDouMatcher = PRICE_DOU.matcher(token1 + " " + token2);
        if (priceDouMatcher.find()) {
            String temp = "";
            BigDecimal value = new BigDecimal(0);
            if (token1.endsWith("bn")) {
                temp = saved_original.replaceAll("bn", "");
                Matcher regularNUMmatcher = REGULAR_NUM.matcher(temp);
                try {
                    if (regularNUMmatcher.find()) value = new BigDecimal(Double.parseDouble(temp) * BILLION);
                } catch (Exception e) {

                }

            }
            if (token1.endsWith("m")) {
                temp = saved_original.replaceAll("m", "");
                try{
                    if (isNumber(temp)) value = new BigDecimal(Double.parseDouble(temp) * MILLION);
                }
                catch (Exception e){

                }
            }

            term = get_term_from_simple_price(value + "", "");
            //System.out.println("Term added: " + term);
            return term;
        }

        // check <decimal + NumberSize >
        Matcher numberSizeMatcher = NUMBER_ADDS.matcher(token1 + " " + token2);
        if (isNumber(token1) && numberSizeMatcher.find()) {
            term = PairTokensIsNumberFormat(token1, token2);
            //System.out.println("Term added: " +term);
            return term;
        }
        //date < decimal + Month >
        Matcher dateFormatMatcher = DATE_DD_MONTH.matcher(token1 + " " + token2.toLowerCase());
        if (!saved_original.startsWith("$") && !saved_original.endsWith("%") && dateFormatMatcher.find()) {
            term = PairTokensIsDateFormat(token1, token2.toLowerCase());
            //System.out.println("Term added: " + term);
            return term;
        }
        return term;
    }

    private String check3WordsPattern(String token1, String token2, String token3) {
        String term = "";
        token1 = cleanToken(token1);
        token2 = cleanToken(token2);
        token3 = cleanToken(token3);

        // check <decimal + fraction + dollars>
        Matcher decFractionDollarsMatcher = PRICE_FRACTION_DOLLARS.matcher(token1 + " " + token2 + " " + token3);
        if (decFractionDollarsMatcher.find()) {
            term = token1 + " " + token2 + " " + token3;
            //System.out.println("Term added: " + term);

            return term;
        }
//
//        // check <decimal + NumberSize >
//        Matcher numberSizeMatcher = NUMBER_SIZE.matcher(token1 + " " + token2);
//        if (numberSizeMatcher.find()) {
//            token1 =
//                    term = PairTokensIsNumberFormat(token1, token2);
//            System.out.println("Term added: " + term);
//            addToDocTerms(term , currDoc)  ; ;
//            return true;
//        }
//        //datre < decimal + Month >
//        Matcher dateFormatMatcher = DATE_DD_MONTH.matcher(token1 + " " + token2);
//        if (dateFormatMatcher.find()) {
//            term = PairTokensIsDateFormat(token1, token2);
//            System.out.println("Term added: " + term);
//            addToDocTerms(term , currDoc)  ; ;
//            return true;
//        }
////        //date - < Month + decimal >
////        Matcher dateFormatMatcher2 = DATE_MONTH_DD.matcher(token1 + " " + token2);
////        if (dateFormatMatcher2.find()) {
////            term = PairTokensIsDate2Format(token1, token2);
////            System.out.println("Term added: " + term);
////            addToDocTerms(term)  ; ;
////            return true;
////        }
        return term;


    }

    private String check4WordsPattern(String token1, String token2, String token3, String token4) {
        String term = "";
        token1 = cleanToken(token1);
        token2 = cleanToken(token2);
        token3 = cleanToken(token3);
        token4 = cleanToken(token4);

        Matcher priceSizeUSdollarsMatcher = PRICE_MBT_US_DOLLARS.matcher(token1 + " " + token2 + " " + token3 + " " + token4);
        if (priceSizeUSdollarsMatcher.find()) {
            String temp = token2;
            switch (temp.toLowerCase()) {
                case "million":
                    term = token1 + " M Dollars";
                    break;
                case "billion":
                    token1 = token1.replaceAll(",", "");
                    if (isNumber(token1))
                        term = ((checkVal(Double.parseDouble(token1) * THOUSAND))) + " M Dollars";
                    break;
                case "trillion":
                    token1 = token1.replaceAll(",", "");
                    if (isNumber(token1)) ;
                    term = ((checkVal(Double.parseDouble(token1) * MILLION))) + " M Dollars";
                    break;

            }
            //System.out.println(term);

            return term;
        }
        return term;
    }

    /**
     * replace the second toke to the right sign by prog rule
     *
     * @param token        number
     * @param anotherToken million| billion| percent ..
     * @return a term
     */
    private String PairTokensIsNumberFormat(String token, String anotherToken) {
        String term = "";
        String temp = anotherToken;
        switch (temp.toLowerCase()) {
            case "thousand":
                term = token + "K";
                break;
            case "million":
                term = token + "M";
                break;
            case "billion":
                term = token + "B";
                break;
            case "percent":
                term = token + "%";
                break;
            case "percentage":
                term = token + "%";
                break;
            case "dollars":
                term = get_term_from_simple_price(token, token);
                break;
            case "trillion":
                token = token.replaceAll(",", "");
                if (isNumber(token)) ;
                double value = Double.parseDouble(token) * TRILLION;
                term = get_term_from_simple_number(value + "");
                break;
        }
        return term;
    }

    //the next funcs change the anotherToken to the right sign and add it
    // to the term , return  the changed term

    /* Month DD */
    private String PairTokensIsDate2Format(String token, String anotherToken) {
        String term = "";
        String temp = token;
        if (anotherToken.length() == 1) anotherToken = "0" + anotherToken;
        switch (temp.toLowerCase().substring(0, 3)) {
            case "jan":
                term = "01-" + anotherToken;
                break;
            case "feb":
                term = "02-" + anotherToken;
                break;
            case "mar":
                term = "03-" + anotherToken;
                break;
            case "apr":
                term = "04-" + anotherToken;
                break;
            case "may":
                term = "05-" + anotherToken;
                break;
            case "jun":
                term = "06-" + anotherToken;
                break;
            case "jul":
                term = "07-" + anotherToken;
                break;
            case "aug":
                term = "08-" + anotherToken;
                break;
            case "sep":
                term = "09-" + anotherToken;
                break;
            case "oct":
                term = "10-" + anotherToken;
                break;
            case "nov":
                term = "11-" + anotherToken;
                break;
            case "dec":
                term = "12-" + anotherToken;
                break;
        }
        return term;

    }


    /* Month YYYY */
    private String PairTokensIsDate3Format(String token, String anotherToken) {
        String term = "";
        String temp = token;
        switch (temp.toLowerCase().substring(0, 3)) {
            case "jan":
                term = anotherToken + "-01";
                break;
            case "feb":
                term = anotherToken + "-02";
                break;
            case "mar":
                term = anotherToken + "-03";
                break;
            case "apr":
                term = anotherToken + "-04";
                break;
            case "may":
                term = anotherToken + "-05";
                break;
            case "jun":
                term = anotherToken + "-06";
                break;
            case "jul":
                term = anotherToken + "-07";
                break;
            case "aug":
                term = anotherToken + "-08";
                break;
            case "sep":
                term = anotherToken + "-09";
                break;
            case "oct":
                term = anotherToken + "-10";
                break;
            case "nov":
                term = anotherToken + "-11";
                break;
            case "dec":
                term = anotherToken + "-12";
                break;
        }
        return term;

    }

    /* DD Month */
    private String PairTokensIsDateFormat(String token, String anotherToken) {
        String term = "";
        String temp = anotherToken;
        if (token.length() == 1) token = "0" + token;
        switch (temp.toLowerCase().substring(0, 3)) {
            case "jan":
                term = "01-" + token;
                break;
            case "feb":
                term = "02-" + token;
                break;
            case "mar":
                term = "03-" + token;
                break;
            case "apr":
                term = "04-" + token;
                break;
            case "may":
                term = "05-" + token;
                break;
            case "jun":
                term = "06-" + token;
                break;
            case "jul":
                term = "07-" + token;
                break;
            case "aug":
                term = "08-" + token;
                break;
            case "sep":
                term = "09-" + token;
                break;
            case "oct":
                term = "10-" + token;
                break;
            case "nov":
                term = "11-" + token;
                break;
            case "dec":
                term = "12-" + token;
                break;
        }
        return term;
    }

    /**
     * check and handle a token of decimal num
     *
     * @param token a number
     */
    private String get_term_from_simple_number(String token) {
        double value = 0;
        try {
            value = Double.parseDouble(token);
        } catch (Exception e) {

        }

        if (isBetween(value, 0, THOUSAND - 1))
            return checkVal(value) + "";
        if (isBetween(value, THOUSAND, MILLION - 1))
            return checkVal(value / THOUSAND) + "K";

        if (isBetween(value, MILLION, BILLION - 1))
            return checkVal(value / MILLION) + "M";

        if (isBetween(value, BILLION, Double.MAX_VALUE))
            return checkVal(value / BILLION) + "B";


        return "ERROR!!!";
    }

    /**
     * handle all patterns < $ + decimal + M Dolllars |Dollars ></>
     *
     * @param token
     * @param originalToken
     * @return the final term changed by prog rules
     */
    private String get_term_from_simple_price(String token, String originalToken) {
        originalToken = originalToken.replace("$", "");
        token = token.replaceAll(",", "");

        double value = 0;
        if (isNumber(token)) {
            try {
                value = Double.parseDouble(token);
            } catch (Exception e) {

            }
            if (isBetween(value, 0, MILLION - 1))
                return originalToken + " Dollars";

            if (isBetween(value, MILLION, Double.MAX_VALUE))
                return checkVal(value / MILLION) + " M Dollars";
        }
        return "ERROR!!!";

    }

    /**
     * change a double token to int if the decimal point is 0 -  of 1.0 to 1
     *
     * @param v
     * @return the fixed num
     */
    private String checkVal(double v) {
        Double d = v;
        if (v == d.intValue())
            return d.intValue() + "";
        else return v + "";
    }

    /**
     * check if a num is between to values
     *
     * @param x
     * @param lower
     * @param upper
     * @return true / false
     */
    public static boolean isBetween(double x, double lower, double upper) {
        return lower <= x && x <= upper;
    }

    public void cleanAll() {
        Posting.flushAndClose();
        this.FBIS3_Terms.clear();
        this.FilesTerms.clear();
        this.TermsOnly.clear();
    }

    public ArrayList<String> getQueryTerms() {
        return  QueryTerms ;
    }
}
