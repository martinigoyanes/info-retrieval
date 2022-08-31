package ir;

import java.util.*;
import java.io.*;

public class PageRank {

    /**
     * Maximal number of documents. We're assuming here that we
     * don't have more docs than we can keep in main memory.
     */
    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     * Mapping from document names to document numbers.
     */
    HashMap<String, Integer> docNumber = new HashMap<String, Integer>();

    /**
     * Mapping from document numbers to document names
     */
    String[] docName = new String[MAX_NUMBER_OF_DOCS];

    HashMap<String, String> intToName = new HashMap<>();

    /**
     * A memory-efficient representation of the transition matrix.
     * The outlinks are represented as a HashMap, whose keys are
     * the numbers of the documents linked from.<p>
     * <p>
     * The value corresponding to key i is a HashMap whose keys are
     * all the numbers of documents j that i links to.<p>
     * <p>
     * If there are no outlinks from i, then the value corresponding
     * key i is null.
     */
    HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();

    /**
     * The number of outlinks from each node.
     */
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    /**
     * The probability that the surfer will be bored, stop
     * following links, and take a random jump somewhere.
     */
    final static double BORED = 0.15;

    /**
     * Convergence criterion: Transition probabilities do not
     * change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.0001;

    double[] pageRankValue;

    public int NUMBER_OF_DOCS;

    /* --------------------------------------------- */


    public PageRank(String filename) {
        NUMBER_OF_DOCS = readDocs(filename);
        iterate(NUMBER_OF_DOCS, 1000);
        mapping();
    }

    public PageRank(String filename, boolean MC) {
        NUMBER_OF_DOCS = readDocs(filename);
    }

    /* --------------------------------------------- */


    /**
     * Reads the documents and fills the data structures.
     *
     * @return the number of documents read.
     */
    int readDocs(String filename) {
        int fileIndex = 0;
        try {
            System.err.print("Reading file... ");
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                String title = line.substring(0, index);
                Integer fromdoc = docNumber.get(title);
                //  Have we seen this document before?
                if (fromdoc == null) {
                    // This is a previously unseen doc, so add it to the table.
                    fromdoc = fileIndex++;
                    docNumber.put(title, fromdoc);
                    docName[fromdoc] = title;
                }
                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
                    String otherTitle = tok.nextToken();
                    Integer otherDoc = docNumber.get(otherTitle);
                    if (otherDoc == null) {
                        // This is a previousy unseen doc, so add it to the table.
                        otherDoc = fileIndex++;
                        docNumber.put(otherTitle, otherDoc);
                        docName[otherDoc] = otherTitle;
                    }
                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if (link.get(fromdoc) == null) {
                        link.put(fromdoc, new HashMap<Integer, Boolean>());
                    }
                    if (link.get(fromdoc).get(otherDoc) == null) {
                        link.get(fromdoc).put(otherDoc, true);
                        out[fromdoc]++;
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + filename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + filename);
        }
        System.err.println("Read " + fileIndex + " number of documents");
        return fileIndex;
    }


    /* --------------------------------------------- */

    private int[] topNIndexes(int n) {
        int[] indexes = new int[n];
        double max = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double actualMax = 0.0;
            int idx = 0;
            for (int j = 0; j < pageRankValue.length; j++) {
                if (pageRankValue[j] > actualMax && pageRankValue[j] < max) {
                    actualMax = pageRankValue[j];
                    idx = j;
                }
            }
            indexes[i] = idx;
            max = actualMax;
        }
        return indexes;
    }

    public void topN(int n) {
        for (int i : topNIndexes(n)) {
            System.out.println(docName[i] + ": " + pageRankValue[i]);
        }
    }

    private void mapping() {
        try (BufferedReader br = new BufferedReader(new FileReader("./pagerank/davisTitles.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] res = line.split(";");
                //String[] nameList = res[1].split("/");
                //intToName.put(res[0], nameList[nameList.length-1]);
                intToName.put(res[0], res[1]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writePageRank(HashMap<String, Integer> namesToId, String rank_file) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < pageRankValue.length; ++i) {
            Integer id = namesToId.get(intToName.get(docName[i]));
            if (id != null) {
                s.append(id).append(",").append(pageRankValue[i]).append("\n");
            }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(rank_file));
            writer.write(s.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public String toString() {
        String s = "";
        for (int i = 0; i < pageRankValue.length; ++i) {
            s += intToName.get(docName[i]) + "," + pageRankValue[i] +"\n";
        }
        return s;
    }


    /*
     *   Chooses a probability vector a, and repeatedly computes
     *   aP, aP^2, aP^3... until aP^i = aP^(i+1).
     */
    void iterate(int numberOfDocs, int maxIterations) {

        pageRankValue = new double[numberOfDocs];
        Arrays.fill(pageRankValue, 0.);
        pageRankValue[0] = 1.;

        int iteration = 0;
        boolean converged = false;
        while (!converged && iteration++ < maxIterations) {
            double[] newX = new double[numberOfDocs];
            Arrays.fill(newX, BORED / numberOfDocs);

            for (Map.Entry<Integer, HashMap<Integer, Boolean>> row : link.entrySet()) {
                int index = row.getKey();
                double p_value = (1. - BORED) / out[index]; //value of the 1-bored*p matrix for that row
                for (int column : row.getValue().keySet()) { // iterate over the row that has value
                    newX[column] += pageRankValue[index] * p_value;
                }
            }
            double diff = 0.;
            for (int i = 0; i < newX.length; ++i) {
                diff += Math.abs(newX[i] - pageRankValue[i]);
            }
            System.err.println(diff);
            converged = diff < EPSILON;

            pageRankValue = newX.clone();
        }
        double sum = Arrays.stream(pageRankValue).sum();
        for (int i = 0; i < pageRankValue.length; i++) {
            pageRankValue[i] /= sum;
        } // why no every iteration?
        System.err.println("iteration "+iteration);
    }



    /* --------------------------------------------- */


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please give the name of the link file");
        } else {
            new PageRank(args[0]).topN(30);;
        }
    }
}
