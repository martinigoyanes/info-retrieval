/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;


import java.io.Serializable;

public class KGramPostingsEntry implements Serializable, Comparable<KGramPostingsEntry> {
    int tokenID;
    int num_grams;

    public KGramPostingsEntry(int tokenID, int num_grams) {
        this.tokenID = tokenID;
        this.num_grams = num_grams;
    }

    public KGramPostingsEntry(KGramPostingsEntry other) {
        this.tokenID = other.tokenID;
        this.num_grams = other.num_grams;
    }

    public String toString() {
        return tokenID + "";
    }


    @Override
    public int compareTo(KGramPostingsEntry o) {
        return Integer.compare(o.tokenID, this.tokenID);
    }
}
