/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;


public class HITSRanker {

    /**
     *   Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    final static int MAX_NUMBER_OF_DOCS = 2000000;

    /**
     *   Convergence criterion: hub and authority scores do not 
     *   change more that EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     *   The inverted index
     */
    Index index;

    /**
     *   Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String,Integer> titleToId = new HashMap<String,Integer>();

    /** Mapping from document identifiers to document names. */
    public HashMap<Integer, String> idToTitle = new HashMap<>();

    /**
     * Mapping from document names to document numbers.
     */
    HashSet<Integer> docs = new HashSet<>();

    /**
     *   Sparse vector containing hub scores
     */
    HashMap<Integer,Double> hubs;

    /**
     *   Sparse vector containing authority scores
     */
    HashMap<Integer,Double> authorities;

    HashMap<Integer, HashSet<Integer>> A = new HashMap<>();
    HashMap<Integer, HashSet<Integer>> At = new HashMap<>();

    Integer number_of_docs;

    HashMap<String, Integer> nameToRealID;

    
    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph.
     * Each page is a node in graph with a distinct nodeID associated with it.
     * There is an edge between two nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     *  nodeID;outNodeID1,outNodeID2,...,outNodeIDK
     * This means that there are edges between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format:
     *  nodeID;pageTitle
     *  
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the same
     *       as docIDs used by search engine's Indexer
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     * @param      index           The inverted index
     */
    public HITSRanker( String linksFilename, String titlesFilename, Index index ) {
        this.index = index;
        readDocs( linksFilename, titlesFilename );
        reverseDocNames();
    }


    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path.
     * For example, given the path "davisWiki/hello.f",
     * the function will return "hello.f".
     *
     * @param      path  The file path
     *
     * @return     The file name.
     */
    private String getFileName( String path ) {
        String result = "";
        StringTokenizer tok = new StringTokenizer( path, "\\/" );
        while ( tok.hasMoreTokens() ) {
            result = tok.nextToken();
        }
        return result;
    }


    void readDocs2( String linksFilename, String titlesFilename ) {


    }


    /**
     * Reads the files describing the graph of the given set of pages.
     *
     * @param      linksFilename   File containing the links of the graph
     * @param      titlesFilename  File containing the mapping between nodeIDs and pages titles
     */
    void readDocs( String linksFilename, String titlesFilename ) {

        try (BufferedReader br = new BufferedReader(new FileReader(titlesFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] res = line.split(";");
                //String[] nameList = res[1].split("/");
                titleToId.put(res[1], Integer.valueOf(res[0]));
                idToTitle.put(Integer.valueOf(res[0]), res[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (BufferedReader br = new BufferedReader(new FileReader(linksFilename))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] res = line.split(";");
                int docID = Integer.parseInt(res[0]);
                docs.add(docID);
                if (res.length > 1) {
                    String[] pointingTo = res[1].split(",");
                    HashSet<Integer> toList = new HashSet<>();

                    for (String id : pointingTo) {
                        toList.add(Integer.valueOf(id));
                    }
                    A.put(docID, toList);

                    HashSet<Integer> from;
                    for (int target : toList) {
                        docs.add(target);
                        from = At.get(target);
                        if (from == null) {
                            from = new HashSet<>();
                            At.put(target, from);
                        }
                        from.add(docID);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.err.println("Read " + docs.size() + " number of documents");
        number_of_docs = docs.size();
        docs.clear();
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param      titles  The titles of the documents in the root set
     */
    private void iterate(String[] titles) {

        HashSet<Integer> docID = new HashSet<>();

        for (int i = 0; i < titles.length; ++i) {
            Integer linkFile = titleToId.get(titles[i]);
            //Integer idMatrix = docNumber.get(Integer.toString(linkFile));
            docID.add(linkFile);
        }
        iterate(docID);
    }


    private boolean diffVector(double[] hub, double[] hub_ant, double[] auth, double[] auth_ant) {
        double diffH = 0.;
        double diffA = 0.;
        for (int i = 0; i < hub.length; ++i) {
            diffH += Math.abs(hub[i] - hub_ant[i]);
            diffA += Math.abs(auth[i] - auth_ant[i]);
        }

        //System.err.println("Diff hub "+diffH+" diff auth "+diffA);
        return diffH < EPSILON && diffA < EPSILON;
    }

    private void normalize(double[] hub, double[] auth) {
        double valueH = 0;
        double valueA = 0;
        for (int i = 0; i < hub.length; ++i) {
            valueH += Math.pow(hub[i], 2);
            valueA += Math.pow(auth[i], 2);
        }
        valueH = Math.sqrt(valueH);
        valueA = Math.sqrt(valueA);
        for (int i = 0; i < hub.length; i++) {
            hub[i] /= valueH;
            auth[i] /= valueA;
        }
    }

    private void iterate(HashSet<Integer> docsID) {
        double[] auth_ant = new double[number_of_docs+1];

        double[] auth = new double[number_of_docs+1];
        Arrays.fill(auth, 1.);

        double[] hub = new double[number_of_docs+1];
        Arrays.fill(hub, 1.);
        double[] hub_ant = new double[number_of_docs+1];

        int iteration = 0;
        boolean stop = false;
        while (!stop) {
            double[] aux_auth = auth.clone();
            double[] aux_hub = hub.clone();
            for (int id : docsID) {
                if (At.containsKey(id)) {
                    for (int pointed : At.get(id)) {
                        auth[id] += aux_hub[pointed];
                    }
                }
                if (A.containsKey(id)) {
                    for (int pointed : A.get(id)) {
                        hub[id] += aux_auth[pointed];
                    }
                }
            }

            normalize(hub, auth);

            stop = diffVector(hub, hub_ant, auth, auth_ant);
            auth_ant = auth.clone();
            hub_ant = hub.clone();
            ++iteration;
            //System.out.println("iteration " + iteration);
        }
        hubs = new HashMap<>();
        authorities = new HashMap<>();
        for (int i = 0; i < hub.length; ++i) {
            if (hub[i] != 0) {
                hubs.put(i, hub[i]);
            }
            if (auth[i] != 0) {
                authorities.put(i, auth[i]);
            }
        }

    }

    /**
     * Rank the documents in the subgraph induced by the documents present
     * in the postings list `post`.
     *
     * @param      post  The list of postings fulfilling a certain information need
     *
     * @return     A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        PostingsList answer = new PostingsList();

        HashSet<Integer> docID = new HashSet<>();
        HashMap<Integer, Integer> IDtoInternalID = new HashMap<>();
        for (int i = 0; i < post.size(); ++i) {
            int goodID = post.get(i).docID;
            String name = index.docNames.get(goodID);
            name = name.split("\\\\davisWiki\\\\")[1];

            Integer linkFile = titleToId.get(name);
            if (linkFile != null) {
                docID.add(linkFile);
                IDtoInternalID.put(linkFile, goodID);
            }

        }
        long startTime = System.currentTimeMillis();

        HashSet<Integer> base = calculateBaseSet(docID);

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("Indexing done in %.1f seconds. "+ String.valueOf(elapsedTime/1000.0 ));

        startTime = System.currentTimeMillis();

        iterate(base);

        elapsedTime = System.currentTimeMillis() - startTime;
        System.err.println("Indexing done in %.1f seconds. "+ String.valueOf(elapsedTime/1000.0 ));

        int cont = 0;
        //double sumHubs = hubs.values().stream().reduce(0., Double::sum);
        //double sumAuth = hubs.values().stream().reduce(0., Double::sum);
        for (int id : base) {
            String name = idToTitle.get(id);
            Integer realID = nameToRealID.get(name);
            if (realID != null) {
                ++cont;
                double h = hubs.getOrDefault(id, 0.);
                double auth = authorities.getOrDefault(id, 0.);
                double score = ((h) + (auth));
                answer.addEntry(new PostingsEntry(realID, score));
            }
        }
        System.err.println(cont);
        Collections.sort(answer.getList());
        return answer;
    }

    public void reverseDocNames() {
        nameToRealID = new HashMap<>();
        for(Map.Entry<Integer, String> entry : index.docNames.entrySet()) {
            String name = entry.getValue();
            nameToRealID.put(name.split("\\\\")[2], entry.getKey());
        }
    }

    private HashSet<Integer> calculateBaseSet(HashSet<Integer> docID) {
        HashSet<Integer> base = new HashSet<>();
        for (int id : docID) {
            base.add(id);
            HashSet<Integer> set = A.get(id);
            if (set != null) base.addAll(set);
            set = At.get(id);
            if (set != null) base.addAll(set);
        }
        return base;
    }


    /**
     * Sort a hash map by values in the descending order
     *
     * @param      map    A hash map to sorted
     *
     * @return     A hash map sorted by values
     */
    private HashMap<Integer,Double> sortHashMapByValue(HashMap<Integer,Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer,Double> > list = new ArrayList<Map.Entry<Integer,Double> >(map.entrySet());
      
            Collections.sort(list, new Comparator<Map.Entry<Integer,Double>>() {
                public int compare(Map.Entry<Integer,Double> o1, Map.Entry<Integer,Double> o2) { 
                    return (o2.getValue()).compareTo(o1.getValue()); 
                } 
            }); 
              
            HashMap<Integer,Double> res = new LinkedHashMap<Integer,Double>(); 
            for (Map.Entry<Integer,Double> el : list) { 
                res.put(el.getKey(), el.getValue()); 
            }
            return res;
        }
    } 


    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param      map        A hash map
     * @param      fname      The filename
     * @param      k          A number of entries to write
     */
    void writeToFile(HashMap<Integer,Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));
            
            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer,Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k) break;
                }
            }
            writer.close();
        } catch (IOException e) {}
    }


    /**
     * Rank all the documents in the links file. Produces two files:
     *  hubs_top_30.txt with documents containing top 30 hub scores
     *  authorities_top_30.txt with documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer,Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer,Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }


    /* --------------------------------------------- */


    public static void main( String[] args ) {
        if ( args.length != 2 ) {
            System.err.println( "Please give the names of the link and title files" );
        }
        else {
            HITSRanker hr = new HITSRanker( args[0], args[1], null );
            hr.rank();
        }
    }
} 