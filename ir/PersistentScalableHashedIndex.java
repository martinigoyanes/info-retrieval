package ir;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class PersistentScalableHashedIndex extends PersistentHashedIndex {

    public static final String INDEXDIR = "./index_guardian";

    public static final int MAX_TOKENS= 350000;
    //public static final int MAX_TOKENS= 100000;

    /** The dictionary hash table on disk can fit this many entries. */
    //public static final long TABLESIZE = 611953L; // change at some point
    public static final long TABLESIZE = 3559999L; // change at some point

    long size_dict = 8;

    public static int number_of_tokens_treated = 0;

    public static int filesWritten = 0;

    public static int mergeStep = 0;

    public static int totalTokensProcessed = 0;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFileFinal;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFileFinal;

    /** The cache as a main-memory hash map. */
    TreeMap<String,PostingsList> index = new TreeMap<>();

    public Merge merge;

    public static class Pair {
        public String data;
        public int size;

        public Pair(String data, int size) {
            this.data = data;
            this.size = size;
        }

        public Pair() {}
    }


    class Merge extends Thread {

        int step;

        public Merge (int mergeStep) {
            this.step = mergeStep;
        }

        public void run() {
            try {
                mergeFiles();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String mergePostingList(String p1, String p2) {
            PostingsList answer = mergePostingList(new PostingsList(p1), new PostingsList(p2));
            return answer.toString();
        }

        private PostingsList mergePostingList(PostingsList p1, PostingsList p2) {
            PostingsList answer = new PostingsList();
            int i = 0;
            int j = 0;
            while (i < p1.size() && j < p2.size()) {
                PostingsEntry postingsEntry1 = p1.get(i);
                PostingsEntry postingsEntry2 = p2.get(j);
                if (postingsEntry1.docID == postingsEntry2.docID) {
                    ArrayList<Integer> offsets = mergeOffsets(postingsEntry1.offsets, postingsEntry2.offsets);
                    answer.addEntry(new PostingsEntry(postingsEntry1.docID, offsets));
                    ++i;
                    ++j;
                } else if (postingsEntry1.docID < postingsEntry2.docID) {
                    answer.addEntry(postingsEntry1);
                    i++;
                } else {
                    answer.addEntry(postingsEntry2);
                    ++j;
                }
            }
            if (i < p1.size()) {
                answer.addAll(p1, i);
            } else if (j < p2.size()) {
                answer.addAll(p2, j);
            }
            return answer;
        }

        private ArrayList<Integer> mergeOffsets (ArrayList<Integer> o1, ArrayList<Integer> o2) {
            ArrayList<Integer> answer = new ArrayList<>();
            int i = 0;
            int j = 0;
            while (i < o1.size() && j < o2.size()) {
                if (o1.get(i).equals(o2.get(j))) {
                    answer.add(o1.get(i));
                    ++i;
                    ++j;
                } else if (o1.get(i) < o2.get(j)) {
                    answer.add(o1.get(i));
                    i++;
                } else {
                    answer.add(o2.get(j));
                    ++j;
                }
            }
            if (i < o1.size()) {
                answer.addAll(o1.subList(i, o1.size()));
            } else if (j < o2.size()) {
                answer.addAll(o2.subList(j, o2.size()));
            }
            return answer;
        }

       private void mergeFiles() throws IOException {
            int lastStep = step-1;
            RandomAccessFile datafileM = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + "M" + lastStep, "r" );
            RandomAccessFile datafile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + lastStep, "r" );
            RandomAccessFile destination = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME + "M" + step, "rw" );

            // do the merge to destination
            long pos1 = 0;
            long posM = 0;
            long pos1_old = -1;
            long posM_old = -1;
            long freeD = 0;
            PersistentScalableHashedIndex.Pair data1 = new PersistentScalableHashedIndex.Pair();
            PersistentScalableHashedIndex.Pair data2 = new PersistentScalableHashedIndex.Pair();
            int deb = 0;
            while (pos1 < datafile.length() && posM < datafileM.length()) {
                if (pos1_old != pos1) data1 = readData(pos1, datafile);
                if (posM_old != posM) data2 = readData(posM, datafileM);

                pos1_old = pos1;
                posM_old = posM;
                //if (deb%50000 ==0) System.err.println("hdhdddhd " + deb + " merge "+ mergeStep);
                String[] info = data1.data.split("\\*");
                String token1 = info[0];
                String posting1 = info[1];

                info = data2.data.split("\\*");
                String token2 = info[0];
                String posting2 = info[1];

                if (token1.equals(token2)) {
                    String posting = mergePostingList(posting1, posting2);

                    int bytesWritten = writeDataWithLength(token1+"*"+posting, freeD, destination);
                    if (bytesWritten <= 0) continue;
                    freeD += bytesWritten+1;

                    pos1 += data1.size + 1;
                    posM += data2.size + 1;
                } else if (token1.compareTo(token2) < 0) {
                    int bytesWritten = writeDataWithLength(data1.data, freeD, destination);
                    if (bytesWritten <= 0) continue;
                    freeD += bytesWritten+1;
                    pos1 += data1.size + 1;

                } else {
                    int bytesWritten = writeDataWithLength(data2.data, freeD, destination);
                    if (bytesWritten <= 0) continue;
                    freeD += bytesWritten+1;
                    posM += data2.size + 1;
                }
                ++deb;
            }

            if (pos1 < datafile.length()) {
                appendDataFiles(datafile, destination, pos1, freeD);
            } else if (posM < datafileM.length()) {
                appendDataFiles(datafileM, destination, posM, freeD);
            }

            System.err.println( "Merge done!" );
            datafile.close();
            datafileM.close();
            destination.close();
            File file = new File(INDEXDIR + "/" + DATA_FNAME + "M" + lastStep);
            if (file.delete()) System.err.println("file " + DATA_FNAME + "M" + lastStep + " deleted");
            file = new File(INDEXDIR + "/" + DATA_FNAME + lastStep);
            if (file.delete()) System.err.println("file " + DATA_FNAME + lastStep + " deleted");

        }

        private void appendDataFiles(RandomAccessFile datafile, RandomAccessFile destination, long pos, long freeD) {
            try {
                datafile.seek(pos);
                int size = (int) (datafile.length() - pos);
                byte[] data = new byte[size];
                datafile.readFully( data );

                destination.seek(freeD);
                destination.write(data);
            } catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }


    public PersistentScalableHashedIndex() {
        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }


    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo", true );
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

    /**
     *  Reads data from the data file
     */
    static Pair readData(long ptr, RandomAccessFile datafile) {
        try {
            datafile.seek(ptr);
            int size = datafile.readInt();
            byte[] data = new byte[size];
            datafile.readFully( data );
            return new Pair(new String(data), size + 4);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */
    static int writeDataWithLength(String dataString, long ptr, RandomAccessFile dataFile) {
        try {
            dataFile.seek( ptr+4 );
            byte[] data = dataString.getBytes();
            dataFile.write(data);
            dataFile.seek(ptr);
            dataFile.writeInt(data.length);
            return data.length + 4;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }

    public void writeIndexToDatafile() {
        String name;
        if (filesWritten == 0) {
            // this is the first time
            name = DATA_FNAME + "M" + mergeStep;
        } else name = DATA_FNAME + mergeStep;

        try {
            RandomAccessFile dataFile = new RandomAccessFile( INDEXDIR + "/" + name, "rw" );
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();

            // Write the dictionary and the postings list
            for (Map.Entry<String, PostingsList> pair : index.entrySet()) {
                String key = pair.getKey();
                PostingsList value = pair.getValue();
                String data = key + "*" + value.toString();
                int bytesWritten = writeDataWithLength(data, free, dataFile);
                if (bytesWritten <= 0) continue;
                free += bytesWritten+1;
            }
            dataFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handle() throws IOException {
        if (filesWritten == 0) {
            // let me delete all the files and make sure its all good
            new File(INDEXDIR + "/" + DATA_FNAME).delete();
            new File(INDEXDIR + "/" + DICTIONARY_FNAME).delete();
            new File( INDEXDIR + "/docInfo").delete();
        }
        writeIndexToDatafile();
        totalTokensProcessed += index.size();
        System.err.println("Total number of tokens " + totalTokensProcessed);
        index.clear();
        docLengths.clear();
        docNames.clear();
        number_of_tokens_treated = 0;
        free = 0;
        if (filesWritten != 0) {
            ++mergeStep;
            System.err.println( "lets merge!" );

            if (mergeStep != 1) {
                try {
                    merge.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            merge = new Merge(mergeStep);
            merge.start();
        }
        filesWritten++;
    }

    public PostingsList getPostingsMemory( String token ) {
        return index.get(token);
    }

    public int insertToIndex(String token, int docID, int offset ) {
        PostingsList postingsList = getPostingsMemory(token);
        if (postingsList == null) {
            postingsList = new PostingsList();
            postingsList.addEntry(docID, offset);
            index.put(token, postingsList);
            return 1;
        } else {
            postingsList.addEntry(docID, offset);
            return 0;
        }
    }

    public void insert(String token, int docID, int offset) {
        number_of_tokens_treated += insertToIndex(token, docID, offset);
        if (number_of_tokens_treated == MAX_TOKENS) {
            System.err.println( "Reach max tokens, start write." );
            try {
                handle();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void writeEntry( Entry entry, long ptr, RandomAccessFile datafile ) {
        try {
            datafile.seek(ptr);
            datafile.writeLong(entry.ptr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    Entry readEntry( long ptr, RandomAccessFile datafile ) {
        Entry entry = new Entry();
        try {
            datafile.seek(ptr);
            entry.ptr = datafile.readLong();
        } catch ( IOException e ) {
            return null;
        }
        return entry;
    }

    private void writeDictionaryFile() throws IOException {
        RandomAccessFile datafile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "r" );
        RandomAccessFile dictionaryFileFinal = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );

        // do the merge to destination
        long pos = 0;
        while (pos < datafile.length()) {
            Pair data = readData(pos, datafile);
            String token = data.data.split("\\*")[0];

            Entry entry = new Entry(pos);
            long hash = hashcode(token);

            Entry position = readEntry(hash, dictionaryFileFinal);
            while (position != null && position.ptr != 0) {
                hash += size_dict;
                position = readEntry(hash, dictionaryFileFinal);
            }
            writeEntry(entry, hash, dictionaryFileFinal);
            pos += data.size + 1;
        }
        dictionaryFileFinal.close();

    }

    public PostingsList getPostings( String token ) {
        if (dictionaryFileFinal == null) {
            try {
                dictionaryFileFinal = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "r" );
                dataFileFinal = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "r" );
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        long pointer = hashcode(token);
        int coll = 0;
        while (true) {
            Entry entry = readEntry(pointer, dictionaryFileFinal);
            if (entry.ptr == 0) break;
            Pair data = readData(entry.ptr, dataFileFinal);
            String[] info = data.data.split("\\*");
            String word = info[0];
            if (word.equals(token)) {
                System.out.println(coll);
                return new PostingsList(info[1]);
            }
            ++coll;
            pointer += size_dict;
        }
        System.out.println(coll);
        return null;
    }

    public void cleanup() {
        System.err.println( "clean up... ");
        try {
            handle();
            merge.join();
            new File(INDEXDIR + "/" + DATA_FNAME + "M" + mergeStep).renameTo(new File(INDEXDIR + "/" + DATA_FNAME));
            System.err.println("Creating dictionary file");
            long startTime = System.currentTimeMillis();
            writeDictionaryFile();
            readDocInfo();
            long elapsedTime = System.currentTimeMillis() - startTime;
            System.err.println("time to write dictionari " + elapsedTime/1000.0);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        System.err.println( "done!" );
    }

    public long hashcode(String code) {
        long hash = (code.hashCode() & 0xfffffff ) % TABLESIZE;
        return hash*size_dict;
    }

    public void computeEuclideanLength() {

    }

}
