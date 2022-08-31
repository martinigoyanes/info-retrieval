/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the main class for the search engine.
 */
public class Engine {

    /** The inverted index. */
    // Index index = new HashedIndex();
    Index index = new PersistentHashedIndex();
    // Index index = new PersistentScalableHashedIndex();

    /** The indexer creating the search index. */
    Indexer indexer;

    // HITSRanker hitsRanker = new HITSRanker("./pagerank/linksDavis.txt",
    // "./pagerank/davisTitles.txt", index);

    /** K-gram index */
    KGramIndex kgIndex = new KGramIndex(2);

    /** The searcher used to search the index. */
    Searcher searcher;

    /** Spell checker */
    SpellChecker speller;

    /** The engine GUI. */
    SearchGUI gui;

    /** Directories that should be indexed. */
    ArrayList<String> dirNames = new ArrayList<String>();

    /** Lock to prevent simultaneous access to the index. */
    Object indexLock = new Object();

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file = null;

    /** The file containing the logo. */
    String pic_file = "";

    /** The file containing the pageranks. */
    String rank_file = "./pagerank/pageranks.txt";

    /** For persistent indexes, we might not need to do any indexing. */
    boolean is_indexing = true;

    /* ----------------------------------------------- */

    /**
     * Constructor.
     * Indexes all chosen directories and files
     */
    public Engine(String[] args) {
        decodeArgs(args);
        indexer = new Indexer(index, kgIndex, patterns_file);
        searcher = new Searcher(index, kgIndex);
        speller = new SpellChecker(index, kgIndex);
        gui = new SearchGUI(this);
        gui.init();
        /*
         * Calls the indexer to index the chosen directory structure.
         * Access to the index is synchronized since we don't want to
         * search at the same time we're indexing new files (this might
         * corrupt the index).
         */
        if (is_indexing) {
            synchronized (indexLock) {
                gui.displayInfoText("Indexing, please wait...");
                long startTime = System.currentTimeMillis();
                for (int i = 0; i < dirNames.size(); i++) {
                    File dokDir = new File(dirNames.get(i));
                    indexer.processFiles(dokDir, is_indexing);
                }
                index.computeEuclideanLength();

                kgIndex.save();

                index.cleanup();
                long elapsedTime = System.currentTimeMillis() - startTime;
                gui.displayInfoText(String.format("Indexing done in %.1f seconds.", elapsedTime / 1000.0));
            }
        } else {
            long startTime = System.currentTimeMillis();

            kgIndex.load();
            kgIndex.tokensContaining("ve");
            kgIndex.tokensContaining("th he");

            long elapsedTime = System.currentTimeMillis() - startTime;
            gui.displayInfoText(String.format("Indexing done in %.1f seconds.", elapsedTime / 1000.0));
            gui.displayInfoText("Index is loaded from disk");
        }
        calculatePagerank();
    }

    private void calculatePagerank() {
        File file = new File(rank_file);
        if (file.exists()) {
            readPageRank();
        } else {
            PageRank pageRank = new PageRank("./pagerank/linksDavis.txt");
            pageRank.topN(30);
            pageRank.writePageRank(reverseDocNames(), rank_file);
            readPageRank();
        }
    }

    private void readPageRank() {
        try (BufferedReader br = new BufferedReader(new FileReader(rank_file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] res = line.split(",");
                index.pageRank.put(Integer.parseInt(res[0]), Double.parseDouble(res[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, Integer> reverseDocNames() {
        HashMap<String, Integer> answer = new HashMap<>();
        for (Map.Entry<Integer, String> entry : index.docNames.entrySet()) {
            String name = entry.getValue();
            answer.put(name.split("\\\\")[2], entry.getKey());
        }
        return answer;
    }

    /* ----------------------------------------------- */

    /**
     * Decodes the command line arguments.
     */
    private void decodeArgs(String[] args) {
        int i = 0, j = 0;
        while (i < args.length) {
            if ("-d".equals(args[i])) {
                i++;
                if (i < args.length) {
                    dirNames.add(args[i++]);
                }
            } else if ("-p".equals(args[i])) {
                i++;
                if (i < args.length) {
                    patterns_file = args[i++];
                }
            } else if ("-l".equals(args[i])) {
                i++;
                if (i < args.length) {
                    pic_file = args[i++];
                }
            } else if ("-r".equals(args[i])) {
                i++;
                if (i < args.length) {
                    rank_file = args[i++];
                }
            } else if ("-ni".equals(args[i])) {
                i++;
                is_indexing = false;
            } else {
                System.err.println("Unknown option: " + args[i]);
                break;
            }
        }
    }

    /* ----------------------------------------------- */

    public static void main(String[] args) {
        Engine e = new Engine(args);
    }

}
