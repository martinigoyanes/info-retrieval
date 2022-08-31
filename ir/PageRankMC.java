package ir;

import java.util.*;

public class PageRankMC extends PageRank {

    //public static String FILE = "linksSvwiki.txt";
    public static String FILE = "./pagerank/linksDavis.txt";

    public static int M = 2;

    public int N;

    public static Double C = 0.85;

    public PageRankMC(String filename, int MC) {
        super(filename, true);
        this.N = 2*NUMBER_OF_DOCS;
        switch (MC) {
            case 1:
                walk1();
                break;
            case 2:
                walk2();
                break;
            case 4:
                walk4();
                break;
            case 5:
                walk5();
                break;
            default:
                walk1();

        }
        topN(30);
    }

    public void walk1() {
        Random random = new Random();
        pageRankValue = new double[NUMBER_OF_DOCS];
        // walks
        for (int i = 0; i < N; i++) {
            int node = random.nextInt(NUMBER_OF_DOCS);
            while (random.nextFloat() <= C) {
                HashMap<Integer, Boolean> nexts = link.get(node);
                if (nexts != null) {
                    List<Integer> options = new ArrayList(nexts.keySet());
                    int index = random.nextInt(options.size());
                    node = options.get(index);

                } else {
                    node = random.nextInt(NUMBER_OF_DOCS);
                }
            }
            pageRankValue[node] +=1./N;
        }
    }

    public void walk2() {
        Random random = new Random();
        pageRankValue = new double[NUMBER_OF_DOCS];
        int n = N / (M*NUMBER_OF_DOCS);
        // walks
        for (int m=0; m < M; ++m) {
            for (int doc=0; doc < NUMBER_OF_DOCS; ++doc) {
                for (int i = 0; i < n; i++) {
                    int node = doc;
                    while (random.nextFloat() <= C) {
                        HashMap<Integer, Boolean> nexts = link.get(node);
                        if (nexts != null) {
                            List<Integer> options = new ArrayList(nexts.keySet());
                            int index = random.nextInt(options.size());
                            node = options.get(index);
                        } else {
                            node = random.nextInt(NUMBER_OF_DOCS);
                        }
                    }
                    pageRankValue[node] +=1.;
                }
            }
        }
        for (int i = 0; i < pageRankValue.length; ++i) {
            pageRankValue[i] /= N;
        }

    }

    public void walk4() {
        Random random = new Random();
        pageRankValue = new double[NUMBER_OF_DOCS];
        int n = N / (M*NUMBER_OF_DOCS);
        // walks
        for (int m=0; m < M; ++m) {
            for (int doc=0; doc < NUMBER_OF_DOCS; ++doc) {
                for (int i = 0; i < n; i++) {
                    int node = doc;
                    HashMap<Integer, Integer> visited = new HashMap<>();
                    while (random.nextFloat() <= C) {
                        HashMap<Integer, Boolean> nexts = link.get(node);
                        if (nexts != null) {
                            List<Integer> options = new ArrayList(nexts.keySet());
                            int index = random.nextInt(options.size());
                            node = options.get(index);
                        } else {
                            node = random.nextInt(NUMBER_OF_DOCS);
                        }
                        pageRankValue[node] +=1.;
                    }
                }
            }
        }
        double numVisits = Arrays.stream(pageRankValue).sum();
        for (int i = 0; i < pageRankValue.length; ++i) {
            pageRankValue[i] /= numVisits;
        }

    }

    public void walk5() {
        Random random = new Random();
        pageRankValue = new double[NUMBER_OF_DOCS];
        int numVisits = 0;
        // walks
        for (int i = 0; i < N; i++) {
            int node = random.nextInt(NUMBER_OF_DOCS);
            HashMap<Integer, Integer> visited = new HashMap<>();
            while (random.nextFloat() <= C) {
                HashMap<Integer, Boolean> nexts = link.get(node);
                if (nexts != null) {
                    List<Integer> options = new ArrayList(nexts.keySet());
                    int index = random.nextInt(options.size());
                    node = options.get(index);
                } else {
                    node = random.nextInt(NUMBER_OF_DOCS);
                }
                pageRankValue[node] +=1.;
                numVisits += 1;
            }
        }
        for (int i = 0; i < pageRankValue.length; ++i) {
            pageRankValue[i] /= numVisits;
        }
    }

    private boolean checkCircle(HashMap<Integer, Integer> visited, int node) {
        Integer count = visited.get(node);
        if (count == null) {
            count = 1;
            visited.put(node, 1);
        } else {
            count++;
            visited.put(node, count);
        }
        return count > 3;
    }

    public static void main(String[] args) {
        new PageRankMC(FILE, 4);

    }

}
