/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    long size_dict = 12;
    long SIZE_DATAFILE;


    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        public long ptr;
        public int size;
        public int collisions = 0;

        public Entry(long ptr) {
            this.ptr = ptr;
        }

        public Entry(long ptr, int size) {
            this.ptr = ptr;
            this.size = size;
        }

        public Entry() {}
    }


    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        File f = new File(INDEXDIR + "/" + DICTIONARY_FNAME);
        f.createNewFile();
        File f = new File(INDEXDIR + "/" + DATA_FNAME);
        f.createNewFile();
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
            SIZE_DATAFILE = dictionaryFile.length();
        } catch ( IOException e ) {
            System.out.println("No index found, have to create it....")
        }

        try {
            readDocInfo();
            readEuclideanLength();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        try {
            dictionaryFile.seek(ptr);
            dictionaryFile.writeLong(entry.ptr);
            dictionaryFile.seek(ptr+8);
            dictionaryFile.writeInt(entry.size);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {
        Entry entry = new Entry();
        try {
            dictionaryFile.seek(ptr);
            entry.ptr = dictionaryFile.readLong();
            //dictionaryFile.seek(ptr+8);
            entry.size = dictionaryFile.readInt();
        } catch ( IOException e ) {
            return null;
        }
        return entry;
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
         File f = new File(INDEXDIR + "/docInfo");
        f.createNewFile();
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
            }
        }
        freader.close();
    }

    private void readEuclideanLength() throws IOException {
        File file = new File( INDEXDIR + "/euclidean.txt" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(":");
                euclideanLength.put(new Integer(data[0]), new Double(data[1]));
            }
        }
        freader.close();
    }


    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            for (Map.Entry<String, PostingsList> pair : index.entrySet()) {
                String key = pair.getKey();
                PostingsList value = pair.getValue();
                String data = key + "*" + value.toString();
                int bytesRead = writeData(data, free);
                if (bytesRead <= 0) continue;
                Entry entry = new Entry(free, bytesRead);
                free += bytesRead+1;

                long hash = hashcode(key);

                Entry position = readEntry(hash);
                while (position != null && position.ptr != 0) {
                    hash += size_dict;
                    collisions += 1;
                    position = readEntry(hash);
                }
                writeEntry(entry, hash);
                //System.err.println(entry.collisions);
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }


    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        long pointer = hashcode(token);
        int coll = 0;
        while (true) {
            Entry entry = readEntry(pointer);
            if (entry.ptr == 0) break;
            String data = readData(entry.ptr, entry.size);
            String[] info = data.split("\\*");
            String word = info[0];
            if (word.equals(token)) {
                return new PostingsList(info[1]);
            }
            ++coll;
            pointer += size_dict;
        }
        return null;
    }

    public PostingsList getPostingsMemory( String token ) {
        return index.get(token);
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ){
        PostingsList postingsList = getPostingsMemory(token);
        if (postingsList == null) {
            postingsList = new PostingsList();
            postingsList.addEntry(docID, offset);
            index.put(token, postingsList);
        } else {
            postingsList.addEntry(docID, offset);
        }
    }

    public long hashcode(String code) {
        long hash = (code.hashCode() & 0xfffffff ) % TABLESIZE;
        return hash*size_dict;
    }

    public void computeEuclideanLength() {
        int N = docNames.size();
        for (Map.Entry<String, PostingsList> entry : index.entrySet()) {
            int df = entry.getValue().size();
            double idf = Math.log((double) N / df);
            for (PostingsEntry postingsEntry : entry.getValue().getList()) {
                Double value = euclideanLength.get(postingsEntry.docID);
                if (value != null) {
                    double v = postingsEntry.offsets.size() * idf;
                    value += Math.pow(v, 2);
                } else {
                    value = Math.pow(postingsEntry.offsets.size() * idf, 2);
                }
                euclideanLength.put(postingsEntry.docID, value);
            }
        }

        StringBuilder s = new StringBuilder();
        for (Map.Entry<Integer, Double> entry : euclideanLength.entrySet()) {
            Double v = Math.sqrt(entry.getValue());
            euclideanLength.put(entry.getKey(), v);
            s.append(entry.getKey()).append(":").append(v).append("\n");
        }

        try {
            File f = new File(INDEXDIR + "/filename.txt");
            f.createNewFile();

            BufferedWriter writer = new BufferedWriter(new FileWriter(INDEXDIR + "/euclidean.txt"));
            writer.write(s.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
    }
}
