/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    private HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        PostingsList postingsList = getPostings(token);
        if (postingsList == null) {
            postingsList = new PostingsList();
            postingsList.addEntry(docID, offset);
            index.put(token, postingsList);
        } else {
            postingsList.addEntry(docID, offset);
        }
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
            BufferedWriter writer = new BufferedWriter(new FileWriter("./index/euclidean.txt"));
            writer.write(s.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        return index.get(token);
    }


    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }
}
