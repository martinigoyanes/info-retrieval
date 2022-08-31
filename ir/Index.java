/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 *  Defines some common data structures and methods that all types of
 *  index should implement.
 */
public interface Index {

    /** Mapping from document identifiers to document names. */
    public HashMap<Integer,String> docNames = new HashMap<Integer,String>();

    /** Mapping from document identifier to document length. */
    public HashMap<Integer,Integer> docLengths = new HashMap<Integer,Integer>();

    /** Mapping from document identifier to its pagerank. */
    public HashMap<Integer, Double> pageRank = new HashMap<>();

    /** Mapping from document identifier to its euclidean length. */
    public HashMap<Integer, Double> euclideanLength = new HashMap<>();

    /** Inserts a token into the index. */
    public void insert( String token, int docID, int offset );

    /** Returns the postings for a given term. */
    public PostingsList getPostings( String token );

    public void computeEuclideanLength();

    /** This method is called on exit. */
    public void cleanup();

}

