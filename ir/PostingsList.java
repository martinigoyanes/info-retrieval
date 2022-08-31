/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class PostingsList implements Comparable<PostingsList> {

    /**
     * The postings list
     */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
    public double weight = 1.;

    public PostingsList() {
    }

    public PostingsList(String posting) {
        String[] postingEntries = posting.split("-");
        for (String entry : postingEntries) {
            if (entry.contains(":"))
                list.add(new PostingsEntry(entry));
        }
    }

    public PostingsList(PostingsList postingsList, double score) {
        list = (ArrayList<PostingsEntry>) postingsList.list.clone();
        weight = score;
    }

    /**
     * Number of postings in this list.
     */
    public int size() {
        return list.size();
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    /**
     * Returns the ith posting.
     */
    public PostingsEntry get(int i) {
        return list.get(i);
    }

    public void addEntry(int docID, int offset) {
        if (list.isEmpty()) {
            list.add(new PostingsEntry(docID, offset));
        } else if (docID != list.get(list.size() - 1).docID)
            list.add(new PostingsEntry(docID, offset));
        else { // is not a new document
            list.get(list.size() - 1).addOffset(offset);
        }
    }

    public void addEntry(PostingsEntry postingsEntry) {
        list.add(postingsEntry);
    }

    public void addAll(PostingsList postingsList, int index) {
        int end = postingsList.size();
        list.addAll(postingsList.list.subList(index, end));
    }

    public ArrayList<PostingsEntry> getList() {
        return list;
    }

    @Override
    public int compareTo(PostingsList o) {
        return Integer.compare(o.size(), this.size());
    }

    @Override
    public String toString() {
        return list.stream().map(Object::toString).collect(Collectors.joining("-"));
    }
}
