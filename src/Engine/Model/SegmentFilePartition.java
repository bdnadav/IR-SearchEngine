package Engine.Model;
/**
 * This class represents a Segment File of chunk.
 * In fact, this department manages the writing
 * text file intended to hold information in the following format: <Term>
 *                                                                 "#"<DocNo>"|"<tf>"#"<DocNo>"|"<tf>"#"....
 */

import java.io.*;

public class SegmentFilePartition implements Serializable {
    private BufferedWriter file_buffer_writer;
    private int counter;
    private int chunk_num;
    private String path_u ;

    public SegmentFilePartition(String path, int chunk_num) {
        this.chunk_num = chunk_num;
        String segmantPartitionFilePath = path+"\\Segment Files\\seg" + "_" + this.chunk_num + ".txt";
        File newFile = new File(segmantPartitionFilePath );
        try {
            newFile.createNewFile();
        }
        catch (Exception e ){}
        this.path_u = segmantPartitionFilePath ;
        try {
            file_buffer_writer = new BufferedWriter(new FileWriter(segmantPartitionFilePath));
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * With this method we write a new term to seg file
     * @param term to be written to the file
     * @param sb object that holds the list of documents (from those belonging to the chunk) in which the Term is contained
     */
    synchronized public void signNewTerm(String term , StringBuilder sb) {
        try {
            file_buffer_writer.append(term + "\n");
            file_buffer_writer.append(sb.toString() + "\n");
            counter++;
            if (counter > 400){
                file_buffer_writer.flush();
                counter = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void flushFile() {
        try {
            file_buffer_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeBuffers() {
        try {
            if (file_buffer_writer != null){
                file_buffer_writer.flush();
                file_buffer_writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getPath() {
        return  path_u ;
    }
}
