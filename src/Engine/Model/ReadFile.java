package Engine.Model;

import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;

import javax.print.Doc;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

/**
 * read a file and splits it to docs , by sending the splited text to parse
 */
public class ReadFile {
    private final String postingPath;
    private final boolean stemming;
    ExecutorService executor;

    int counterToFullChunk = 0 ;
    int CHUNK = 0 ;
    Parse parser ;

    ConcurrentHashMap< String ,City> cities = new ConcurrentHashMap<>() ; // save all the doc info cities
    ConcurrentSkipListSet< String  > languages = new ConcurrentSkipListSet<>(); // save all docs lang

    public ReadFile(String postingPath, boolean stemming ) {
        //parser =new Parse(postingPath , stemming ) ;
        this.postingPath = postingPath ;
        this.stemming = stemming ;
        executor = Executors.newFixedThreadPool(4);
    }

    /**
     * read line by line from giving file path and sends doc  & doc text's to parser
     * @param
     */
    public void readAndParseLineByLine(ArrayList<String> paths_list , int chunk  ) {
        int y = 0 ;
        String filePathName = "" ;
        Parse parser = new Parse(postingPath , stemming) ;
        boolean write = false ;
        while ( y < paths_list.size()) {
            filePathName = paths_list.get(y) ;
            BufferedReader br = null;
            System.out.println(filePathName);
            String parentFileName = getParentFileName(filePathName);
            counterToFullChunk++;
            try {
                br = new BufferedReader(new FileReader(filePathName));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                boolean text_adding = false;
                String line = "";
                StringBuilder sb_docInfo = new StringBuilder();
                StringBuilder sb_text = new StringBuilder();
                StringBuilder sb_docHeadlines = new StringBuilder();
                String docNo = "";
                String docCity = "";
                String doc_language = "";
                String doc_date = "";
                String doc_Headline = "";
                while ((line = br.readLine()) != null) {
                    while (line != null) {
                        if (line.equals("<DOC>")) {
                            line = br.readLine();
                            text_adding = false;
                            continue;
                        }
                        if (line.equals("</TEXT>")) {
                            line = br.readLine();
                            continue;
                        }
                        if (line.equals("</DOC>")) {
                            line = br.readLine();
                            break;
                        }

                        if (line.equals("<TEXT>")) {
                            text_adding = true;
                            line = br.readLine();
                        }
                        //clean
                        if (line.startsWith("<F P=106>")) {
                            String[] temp = StringUtils.split(line, "><");
                            line = temp[1];
                        }

                        if (line.contains("P>")) {
                            String[] temp = StringUtils.split(line, ">");
                            if (temp.length > 1)
                                line = temp[1];
                        }
                        // Headlines
                        if (  line.contains("<H") && !line.contains("<HEADER>") && !line.contains("<HT>")){
                            if ( line.equals("<HEADLINE>")){
                                while( line !=null && line.startsWith("<"))
                                    line = br.readLine() ;
                                sb_docHeadlines.append( " " +line) ;

                            }
                            else { //<H ..somthing
                                String[] temp = StringUtils.split(line, "><") ;
                                if (  line.startsWith("<H3> <TI>"))
                                    sb_docHeadlines.append(" "+temp[3]);
                                else if (line.startsWith("<H5>") || line.startsWith("<H4>")|| line.startsWith("<H2>") || line.startsWith("<H3>"))
                                    sb_docHeadlines.append(" "+temp[1]) ;


                            }
                        }

                        // CITY
                        if (!text_adding &&line.startsWith("<F P=104>")) {
                            String[] arr = StringUtils.split(line, " ");
                            if (arr.length < 4) {
                                line = br.readLine();
                                continue;
                            }
                            int i = 4;
                            docCity = arr[2]; // only the first word between tags
                            this.cities.put(docCity.toLowerCase(), new City(docCity));
                            docCity = docCity.toUpperCase();
                        }
                        // Doc Language
                        if (line.contains("<F P=105>")) {
                            int k = 0;
                            String[] arr = StringUtils.split(line, "> <");
                            while (k < arr.length && !arr[k].equals("P=105")) k++;
                            doc_language = arr[k + 1];
                            doc_language = doc_language.toUpperCase();
                            languages.add(doc_language);
                        }
                        // Doc num
                        if (line.startsWith("<DOCNO>")) {
                            String[] arr = StringUtils.split(line, "<> ");
                            if (arr.length >= 2)
                                docNo = arr[1];
                        }

                        if ( line.startsWith("<") && text_adding && !line.startsWith("<H") && !line.startsWith("<F") && !line.startsWith("<DATE")) {
                            line = br.readLine();  // start doc
                            continue;
                        }
                        if (text_adding) // add to doc info
                        sb_text.append(" " + line); // add to text



                        line = br.readLine();
                    }
                    //sb_docInfo.append(line);
                    String text = sb_text.toString();
                    String headlines = sb_docHeadlines.toString();
                    Document doc = new Document(docNo, parentFileName, docCity, doc_language);


                    sb_docHeadlines.delete(0  , sb_docHeadlines.length());
                    sb_docHeadlines.setLength(0);
//                    sb_docInfo.delete(0, sb_docInfo.length());
//                    sb_docInfo.setLength(0);
                    sb_text.delete(0, sb_text.length());
                    sb_text.setLength(0);
                    parser.parseHeadLines(headlines);
                    parser.parse(text, doc );
                    if ( doc.docNo.equals(""))
                        System.out.println("stop!!!");
                    write = false;

                    // line= br.readLine();
                    //executor.execute(parseThread);


                }
                if ( y ==paths_list.size() -1 ) {
                    parser.sendToSeg(chunk);
                    parser.cleanAll();
                }
                y++ ;
                br.close();
                //return splitDocumentsFromFile(sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private String getParentFileName(String filePathName) {
        String[] split = StringUtils.split(filePathName , "\\\\");
        return split[split.length-1];
    }

    /**
     * check if all the chars are upper case
     * @param str
     * @return
     */
    public static boolean testAllUpperCase(String str){
        for(int i=0; i<str.length(); i++){
            char c = str.charAt(i);
            if(c >= 97 && c <= 122) {
                return false;
            }
        }
        //str.charAt(index)
        return true;
    }

    public  ConcurrentHashMap<String, City> getCities() {
        return cities;
    }

    /**
     * get all doc lang
     * @return
     */
    public  String[] getLanguagesList() {
        //String[] str = map1.keySet().toArray(new String[map1.size()]);
        return  languages.toArray(new String[languages.size()]);
    }

    public void CleanAll() {
//        cities.clear();
//        languages.clear();
    }
//    private ArrayList<Pair<String, String>> splitDocumentsFromFile(String fileContent) {
//        ArrayList<Pair<String, String>> doNODocument = new ArrayList<>();
//        String[] fileDocuments = fileContent.split("</DOC>");
//        for (int i = 0; i < fileDocuments.length; i++) {
//            String currentFullDocument = fileDocuments[i];
//            String docNumber = getDocNumber(currentFullDocument);
//            if (docNumber != null) {
//                doNODocument.add(new Pair<>(docNumber, currentFullDocument));
//            }
//        }
//        return doNODocument;
//    }

//    private String getDocNumber(String fileDocument) {
//        String[] str = fileDocument.split("DOCNO>");
//        if (str.length > 1) {
//            if (str[1].contains(" "))
//                str[1].replaceAll("\\s+", "");
//            int indexOfArrow = str[1].indexOf("<");
//            String docNumber = str[1].substring(0, indexOfArrow);
//            return docNumber;
//        }
//        return null;
//    }
}

