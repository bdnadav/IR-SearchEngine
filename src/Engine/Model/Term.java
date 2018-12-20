package Engine.Model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * represent a term inside corpus
 */
public class Term implements Comparable, Serializable {
    private int tf; // term frequency in in doc 
    private String content;
    private LinkedList<Integer> location_in_docs;// < doc obj , a list of locations(row num) in doc text >
    private boolean startsWithCapital;

    public Term(int position, int tf, String content) {
        location_in_docs = new LinkedList<>();
        location_in_docs.add(position);
        this.tf = tf;
        this.content = content.toLowerCase();
        if (Character.isUpperCase(content.codePointAt(0)))
            startsWithCapital = true;
        //tf_docs = new HashMap<>();
    }

    public void advanceTf(){
        tf++;
    }

    public String getContent() {
        return content;
    }

   //public void addDoc(Document doc) { // check if we need to save row
   //    //check if the doc exists and add accordinaly
   //    // if not df +1
   //    if (location_docs.containsKey(doc)) {
   //       location_docs.get(doc).add(0) ; // need to add location in doc
   //        tf_docs.put(doc, tf_docs.get(doc) + 1);
   //    } else { // create new doc
   //        df++;
   //        location_docs.put(doc, new LinkedList<Integer>());
   //        tf_docs.put ( doc , 1) ;
   //    }
   //}

    @Override
    public String toString() {
        return "Term{" +
                //"df=" + df +
                ", tf=" + tf +
                ", content='" + content + '\'' +
                ", location in doc=" + location_in_docs +
                '}';
    }

    // The short format will be: <TermContent>,<df>,<tf>,"<LOCS>:"<location_docs>,
    public String lightToString() {
        return indicateCapital() + content+ "," + tf + "," + location_in_docs;
    }

    public String postingToString(){
        return indicateCapital() + content+ "," + tf;
    }

    private String indicateCapital() {
        if (startsWithCapital)
            return "*";
        else
            return "";
    }

    @Override
    public int compareTo(Object o) {
        return content.compareTo(((Term) (o)).content);
    }

    public void addPosition(int termPosition) {
        int firstFullPosition = location_in_docs.getFirst();
        location_in_docs.add(termPosition - firstFullPosition);
    }
}


