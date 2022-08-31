/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Johan Boye, 2017
 */

package ir;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import static java.lang.Math.abs;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /**
     * The index to be searched by this Searcher.
     */
    Index index;

    /**
     * The k-gram index to be searched by this Searcher
     */
    KGramIndex kgIndex;

    HITSRanker hitsRanker;

    Double totalTfIdf = 0.;
    Double totalPageRank= 0.;

    static double Widf = 0.6;
    static double Wpr = 0.4;

    /**
     * Constructor
     */
    public Searcher(Index index, KGramIndex kgIndex, HITSRanker hitsRanker) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.hitsRanker = hitsRanker;
    }

    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;
    }

    private ArrayList<PostingsList> processQuery(Query query, QueryType queryType) {
        ArrayList<PostingsList> postingsLists = new ArrayList<>();

        for (Query.QueryTerm q : query.queryterm) {
            String term = q.term;
            PostingsList post = index.getPostings(term);
            if (post == null) {
                postingsLists.add(null);
            } else {
                postingsLists.add(new PostingsList(post, q.weight));
            }
        }

        return postingsLists;
    }

    /**
     * Searches the index for postings matching the query.
     *
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType, NormalizationType normalizationType) {

        // list of postingsList fot each token in the query
        ArrayList<PostingsList> postingsLists = processQuery(query, queryType);

        //query.queryterm.forEach((q) -> postingsLists.add(new PostingsList(index.getPostings(q.term), q.weight)));
        //postingsLists.removeAll(Collections.singleton(null));

        if (postingsLists.isEmpty()) return null;

        switch (queryType) {
            case INTERSECTION_QUERY:
                if (postingsLists.contains(null)) return null;
                if (postingsLists.size() == 1) return postingsLists.get(0);
                return searchIntersection(postingsLists);
            case PHRASE_QUERY:
                if (postingsLists.contains(null)) return null;
                if (postingsLists.size() == 1) return postingsLists.get(0);
                return searchPhrase(postingsLists);
            case RANKED_QUERY:
                if (rankingType.equals(RankingType.COMBINATION)) {
                    return combination(postingsLists, normalizationType);
                } else
                    return searchRanking(postingsLists, rankingType, normalizationType);
            default:
                return postingsLists.get(0); // just to do something
        }
    }

    private PostingsList combination(ArrayList<PostingsList> postingsLists, NormalizationType normalizationType) {
        PostingsList tfidf = searchRankingComb(postingsLists, RankingType.TF_IDF, normalizationType);
        PostingsList pagerank = searchRankingComb(postingsLists, RankingType.PAGERANK, normalizationType);
        totalTfIdf = 0.;
        for (PostingsEntry postingsEntry : tfidf.getList()) {
            totalTfIdf += postingsEntry.score;
        }

        totalPageRank = 0.;
        for (PostingsEntry postingsEntry : pagerank.getList()) {
            totalPageRank += postingsEntry.score;
        }

        PostingsList answer = new PostingsList();

        for (int i = 0; i < pagerank.size(); ++i) {
            PostingsEntry p = pagerank.get(i);
            PostingsEntry tf = tfidf.get(i);
            if (p.docID != tf.docID) {
                System.err.println("uuuuups");
            } else {
                double score = Widf * (tf.score/ totalTfIdf) + Wpr*(p.score/totalPageRank);
                answer.addEntry(new PostingsEntry(p.docID, score));
            }
        }

        Collections.sort(answer.getList());
        return answer;
    }


    private PostingsList searchRankingComb(ArrayList<PostingsList> postingsLists, RankingType rankingType,
                                           NormalizationType normalizationType) {

        for (PostingsList postingsList : postingsLists) {
            switch (rankingType) {
                case TF_IDF:
                    if (postingsLists.size() == 1) return searchTfidf1(postingsLists.get(0), normalizationType);
                    calculateTfIdf(postingsList, normalizationType);
                    break;
                case PAGERANK:
                    if (postingsLists.size() == 1) return pagerank1(postingsLists.get(0));
                    calculatePageRank(postingsList);
                    break;
            }
        }
        PostingsList answer = mergePostingList(postingsLists.get(0), postingsLists.get(1), rankingType);
        for (PostingsList postingsList : postingsLists) {
            answer = mergePostingList(answer, postingsList, rankingType);
        }
        return answer;
    }


    private PostingsList searchTfidf1(PostingsList postingsList, NormalizationType normalizationType) {
        calculateTfIdf(postingsList, normalizationType);
        Collections.sort(postingsList.getList());
        return postingsList;
    }

    private PostingsList pagerank1(PostingsList postingsList) {
        calculatePageRank(postingsList);
        Collections.sort(postingsList.getList());
        return postingsList;
    }

    private PostingsList searchRanking(ArrayList<PostingsList> postingsLists, RankingType rankingType,
                                       NormalizationType normalizationType) {

        for (PostingsList postingsList : postingsLists) {
            if (postingsList == null) continue;
            switch (rankingType) {
                case TF_IDF:
                    if (postingsLists.size() == 1) return searchTfidf1(postingsLists.get(0),normalizationType);
                    calculateTfIdf(postingsList,normalizationType);
                    break;
                case PAGERANK:
                    if (postingsLists.size() == 1) return pagerank1(postingsLists.get(0));
                    calculatePageRank(postingsList);
                    break;
                case HITS:
                    if (postingsLists.size() == 1) return hitsRanker.rank(postingsLists.get(0));
                    return calculateHits(postingsLists);
            }
        }
        if (postingsLists.size() < 2) return null;
        return union(postingsLists, rankingType);
    }

    private PostingsList union(ArrayList<PostingsList> postingsLists, RankingType rankingType) {
        PostingsList answer = mergePostingList(postingsLists.get(0), postingsLists.get(1), rankingType);
        for (int i = 2; i < postingsLists.size(); ++i) {
            answer = mergePostingList(answer, postingsLists.get(i), rankingType);
        }
        Collections.sort(answer.getList());
        if (answer.isEmpty()) return null;
        return answer;
    }

    private PostingsList mergePostingList(PostingsList p1, PostingsList p2,RankingType rankingType) {
        PostingsList answer = new PostingsList();

        if (p1 == null) p1 = new PostingsList();
        if (p2 == null) p2 = new PostingsList();

        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            PostingsEntry postingsEntry1 = p1.get(i);
            PostingsEntry postingsEntry2 = p2.get(j);
            if (postingsEntry1.docID == postingsEntry2.docID) {
                double score = 0.;
                switch (rankingType) {
                    case TF_IDF:
                        score = postingsEntry1.score + postingsEntry2.score;
                        break;
                    case PAGERANK:
                        score = postingsEntry1.score;
                        break;
                    case COMBINATION:
                        score = Widf * (postingsEntry1.score/ totalTfIdf) + Wpr*(postingsEntry2.score/totalPageRank);
                        break;
                    case HITS:
                        score = 0; // we don't care
                }

                answer.addEntry(new PostingsEntry(postingsEntry1.docID, score));
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

    private void calculatePageRank(PostingsList postingsList) {
        for (PostingsEntry postingsEntry : postingsList.getList()) {
            postingsEntry.score = index.pageRank.getOrDefault(postingsEntry.docID, 0.);
        }
    }

    private PostingsList calculateHits(ArrayList<PostingsList> postingsLists) {

        PostingsList answer = mergePostingList(postingsLists.get(0), postingsLists.get(1), RankingType.HITS);
        for (PostingsList postingsList : postingsLists) {
            answer = mergePostingList(answer, postingsList, RankingType.HITS);
        }

        return hitsRanker.rank(answer);
    }

    private void calculateTfIdf(PostingsList postingsList, NormalizationType normalizationType) {
        int N = index.docNames.size();
        int df = postingsList.size();
        double idf = Math.log((double) N / df);
        for (PostingsEntry postingsEntry : postingsList.getList()) {
            int tf = postingsEntry.offsets.size();
            double lend;
            if (normalizationType.equals(NormalizationType.EUCLIDEAN)) {
                lend = index.euclideanLength.get(postingsEntry.docID);
            }
            else lend = Double.valueOf(index.docLengths.get(postingsEntry.docID));
            double score = calculate_tf_idf(lend, tf, idf);
            postingsEntry.score = score * postingsList.weight;
        }
    }


    private double calculate_tf_idf(double lend, int tf, double idf) {
        return tf * idf / lend;
    }

    private PostingsList searchIntersection(PostingsList q1, PostingsList q2) {
        PostingsList answer = new PostingsList();
        int i = 0;
        int j = 0;
        while (i < q1.size() && j < q2.size()) {
            PostingsEntry postingsEntry1 = q1.get(i);
            PostingsEntry postingsEntry2 = q2.get(j);
            if (postingsEntry1.docID == postingsEntry2.docID) {
                answer.addEntry(postingsEntry1);
                ++i;
                ++j;
            } else if (postingsEntry1.docID < postingsEntry2.docID) ++i;
            else ++j;
        }
        return answer;
    }

    private PostingsList searchIntersection(ArrayList<PostingsList> postingsLists) {
        Collections.sort(postingsLists, Collections.reverseOrder());
        PostingsList answer = searchIntersection(postingsLists.get(0), postingsLists.get(1));
        for (int i = 2; i < postingsLists.size(); ++i) {
            answer = searchIntersection(answer, postingsLists.get(i));
        }
        return answer;
    }

    private PostingsList searchPhrase(ArrayList<PostingsList> postingsLists) {
        ArrayList<PostingsList> copy = (ArrayList<PostingsList>) postingsLists.clone();
        PostingsList documents = searchIntersection(copy);
        HashSet<Integer> documentsID = getDocumentsID(documents);
        PostingsList answer = searchPhrase(postingsLists.get(0), postingsLists.get(1), documentsID);

        for (int i = 2; i < postingsLists.size(); ++i) {
            answer = searchPhrase(answer, postingsLists.get(i), documentsID);
        }

        return answer;
    }


    private HashSet<Integer> getDocumentsID(PostingsList documents) {
        HashSet<Integer> result = new HashSet<>();

        for (PostingsEntry postingsEntry : documents.getList()) {
            result.add(postingsEntry.docID);
        }

        return result;
    }


    private PostingsList searchPhrase(PostingsList p1, PostingsList p2, HashSet<Integer> documents) {
        PostingsList answer = new PostingsList();
        int entry1 = 0;
        int entry2 = 0;

        // the docID has to be the same
        while (entry1 < p1.size() && entry2 < p2.size()) {
            PostingsEntry postingsEntry1 = p1.get(entry1);
            PostingsEntry postingsEntry2 = p2.get(entry2);

            if (postingsEntry1.docID < postingsEntry2.docID) {
                ++entry1;
                continue;
            } else if (postingsEntry1.docID > postingsEntry2.docID) {
                ++entry2;
                continue;
            } else {
                if (!documents.contains(postingsEntry1.docID)) {
                    ++entry1;
                    ++entry2;
                    continue;
                }
            }

            ArrayList<Integer> offset1 = p1.get(entry1).offsets;
            ArrayList<Integer> offset2 = p2.get(entry2).offsets;
            int i = 0;
            int j = 0;
            ArrayList<Integer> words_post = new ArrayList<>();
            while (i < offset1.size() && j < offset2.size()) {
                int diff = offset1.get(i) - offset2.get(j);

                if (diff == -1) {
                    words_post.add(offset2.get(j));
                    ++i;
                    ++j;
                } else if (diff < 0) ++i;
                else if (diff > 0) ++j;
                else {
                    // if is the same word the diff will be 0, then add the second word
                    ++j;
                }

                if (i == offset1.size() && j < offset2.size() && offset1.get(i-1) > offset2.get(j)) {
                    i = offset1.size() - 1;
                } else if (i < offset1.size() && j == offset2.size() && offset1.get(i) < offset2.get(j-1)) {
                    j = offset2.size() - 1;
                }
            }
            if (!words_post.isEmpty())
                answer.addEntry(new PostingsEntry(p1.get(entry1).docID, words_post));
            ++entry1;
            ++entry2;
        }

        return answer;
    }

}