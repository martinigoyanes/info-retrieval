/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.io.Serializable;
import java.util.stream.Collectors;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public ArrayList<Integer> offsets = new ArrayList<>();

    public PostingsEntry(int docID, int offset) {
        this.docID = docID;
        this.offsets.add(offset);
    }

    public PostingsEntry(int docID, ArrayList<Integer> offsets) {
        this.docID = docID;
        this.offsets = offsets;
    }

    public PostingsEntry(int docID, double score) {
        this.docID = docID;
        this.score = score;
    }

    public PostingsEntry(String postingEntry) {
        String[] array = postingEntry.split(":");
        String docID = array[0];
        ArrayList<String> myList = new ArrayList<>(Arrays.asList(array[1].split(",")));
        myList.forEach((s) -> offsets.add(Integer.parseInt(s)));
        this.docID = Integer.parseInt(docID);
    }

    public void addOffset(int offset) {
        this.offsets.add(offset);
    }

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }

    @Override
    public String toString() {
        String offsets_string = offsets.stream().map(Object::toString).collect(Collectors.joining(","));
        return docID + ":" + offsets_string;
    }

}

