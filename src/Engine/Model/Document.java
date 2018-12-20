package Engine.Model;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.HashMap;

public class Document {
    private  String docLang = ""; // language of the doc
    public String docNo; // id of doc
    private String parentFileName; // File id
    private int unique_t; // quantity of unique terms in doc
    private String city; // city of doc - appear in <F P=104> ...</F>
    private HashMap<Term, Integer> termFrequency;
    private Term maxFreqTerm; // the term with most appearances
    private int maxFreqTermNumber;  // frequency of the most common term in doc


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Document document = (Document) o;

        return docNo != null ? docNo.equals(document.docNo) : document.docNo == null;
    }

    @Override
    public int hashCode() {
        return docNo != null ? docNo.hashCode() : 0;
    }

    public Document(String docNo, String parentFileName, String docCity  , String docLang) {
        this.docNo = docNo;
        this.city = docCity;
        this.docLang = docLang ;
        this.parentFileName = parentFileName;
        termFrequency = new HashMap<>();
    }

    public String getDocNo() {
        return docNo;
    }

    public String getParentFileName() {
        return parentFileName;
    }
    //save the term in termFrequency Hash map
    public void addTerm(Term term){
        if (maxFreqTermNumber == 0){
            maxFreqTerm = term;
            maxFreqTermNumber++;
        }

        if (termFrequency.containsKey(term)){
            int termFreq = termFrequency.get(term) + 1;
            if (termFreq > maxFreqTermNumber) {
                maxFreqTermNumber = termFreq;
                maxFreqTerm = term;
            }
            termFrequency.put(term, termFreq);
        }
        else{
            termFrequency.put(term, 1);
            if (maxFreqTermNumber == 0){
                maxFreqTermNumber = 1;
                maxFreqTerm = term;
            }
        }
    }

    @Override
    public String toString() {
        return "Document{" +
                "docNo='" + docNo + '\'' +
                ", parentFileName='" + parentFileName + '\'' +
                ", max_tf=" + maxFreqTermNumber +
                ", unique_t=" + unique_t +
                ", city='" + city + '\'' +
                //", termFrequency=" + termFrequency +
                ", maxFreqTerm=" + getFreqTermContent() +
                ", maxFreqTermNumber=" + maxFreqTermNumber +
                '}';
    }

    /**
     * updated the num of unique terms in doc after parsing
      */
    public void updateAfterParsing() {
        unique_t = termFrequency.size();
    }

    public String getFreqTermContent() {
        if (maxFreqTerm != null)
            return maxFreqTerm.getContent();
        return null;
    }

    // Format: <docNo>,<parentFileName>,<city>,<max_tf>,<maxTerm>,<num_of_uniques>
    public String lightToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(docNo).append(",").append(parentFileName)
                .append(",").append(city).append(",").append(docLang).append(",").append(maxFreqTermNumber).append(",")
                .append(getFreqTermContent()).append(",").append(unique_t);
        return sb.toString();
    }

    public int getMaxTF() {
        return maxFreqTermNumber;
    }

    public int getNumOfUniqueTerms() {
        return unique_t;
    }

    public String getCity() {
        if (city != null){
            String[] splitedCity = StringUtils.split(city, " ");
            if (splitedCity.length > 0)
                return splitedCity[0].toUpperCase();
            else
                return city.toUpperCase();
        }
        return "";
    }

    public String getLang() {
        return  this.docLang ;
    }


    //private byte[]
}
