import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class PositionalIndex {
    private String folderAddress;
    private HashMap<String, HashMap<String, ArrayList<Integer>>> termDictionary;
    private HashMap<String, Integer> termID;
    private int NumberOfDocs;
    HashMap<String, Integer> queryVec;
    double queryVecSize;
    HashMap <String, Double> docSumSquared;
    HashMap <String, Double> cacheVSS ;
    HashMap <String, Double> cacheTPS ;
    File[] listOfFiles;

    public PositionalIndex(String folder){
        this.folderAddress = folder + "/";
        termDictionary = new HashMap<>();
        preProcess(folderAddress);
    }

    //returns the number of times term appears in doc
    public int termFrequency (String term, String Doc){
        if (termDictionary.get(term).containsKey(Doc) == false)
            return 0;
        return termDictionary.get(term).get(Doc).size();
    }

    //returns the number of docs in which the term appears
    public int docFrequency (String term){
        return termDictionary.get(term).keySet().size();
    }

    public String postingsList(String t){
        if (termDictionary.containsKey(t) == false)
            return "Not found!" ;
        t = t.toLowerCase();
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("[");
        boolean firstDoc = true;
        for (String doc : termDictionary.get(t).keySet()){
            boolean firstPos = true;
            if (firstDoc == false){
                strBuilder.append(", ");
            }
            firstDoc = false;
            strBuilder.append("<" + doc + " : ");

            for (int i : termDictionary.get(t).get(doc)) {
                if (firstPos == false) {
                    strBuilder.append(", ");
                }
                firstPos = false;
                strBuilder.append(i);
            }
            strBuilder.append(">");
        }
        strBuilder.append("]");
        return strBuilder.toString();
    }

    public double weight (String t, String d){
        return Math.sqrt(termFrequency(t, d)) * Math.log10((double)NumberOfDocs / docFrequency(t));
    }

    public double TPScore(String [] queries, String doc){
        int distSum = 0;
        int dist;
        double tps ;
        if (queries.length == 1)
            tps = 0.0;
        else {
            for (int i = 0; i < queries.length - 1; i ++){
                dist = 17;
                if (termDictionary.containsKey(queries [i]) == false
                    || termDictionary.containsKey(queries[i + 1]) == false) {
                    distSum += dist ;
                    continue;
                }
                if ( (termDictionary.get(queries[i]).containsKey(doc) == true) && (termDictionary.get(queries[i + 1]).containsKey(doc) == true )){
                    ArrayList<Integer> t1Positions = termDictionary.get(queries[i]).get(doc);
                    ArrayList<Integer> t2Positions = termDictionary.get(queries[i + 1]).get(doc);
                    int sizeT1 = t1Positions.size();
                    int sizeT2 = t2Positions.size();
                    int startIdxK = 0;
                    for (int j = 0; j < sizeT1; j++){
                        while (startIdxK < sizeT2 && t2Positions.get(startIdxK) < t1Positions.get(j))
                            startIdxK ++;
                        if (startIdxK < sizeT2)
                            dist = Math.min((t2Positions.get(startIdxK) - t1Positions.get(j)), dist);
                    }
                }
                distSum += dist;
            }
            tps = (double)queries.length / distSum ;
        }
        cacheTPS.put(doc, tps) ;
        return tps;
    }

    public double VSScore (String [] queries, String doc){
        //make Vd
        double squared = 0;
        double numerator = 0;

        for (String query : queries){
            if (termDictionary.containsKey(query) == false)
                continue ;
            if (termDictionary.get(query).containsKey(doc)){
                double w = weight(query, doc);
                numerator += queryVec.get(query) * w;
            }
        }
        double VSscore;
        if (queryVecSize * docSumSquared.get(doc) == 0)
            VSscore = 0 ;
        else
            VSscore = numerator / (queryVecSize * docSumSquared.get(doc));
        cacheVSS.put(doc, VSscore) ;
        return VSscore;
    }

    private double findQueryVec(String [] queries){
        int squared = 0;
        queryVec = new HashMap<>() ;
        for (String str : queries){
            if (queryVec.containsKey(str) == false)
                queryVec.put(str, 1);
            else
                queryVec.put(str, queryVec.get(str) + 1);
            squared += 2 * (queryVec.get(str) - 1) + 1;
        }
        return Math.sqrt(squared);
    }

    public double Relevance(String [] queries, String doc){
        return 0.6 * TPScore(queries, doc) + 0.4 * VSScore(queries, doc);
    }

    public String polishWord (String line) {
        line = line.toLowerCase();
        char[] lineChar = line.toCharArray();
        for (int i = 1; i < line.length(); i++){
            if (lineChar[i] == '.' && isDigit(lineChar[i - 1]) && isDigit(lineChar[i + 1]))
                lineChar[i] = 0;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineChar.length; i++)
            sb.append(lineChar[i]);
        line = sb.toString();

        line = line.replaceAll("[\\[\\].,\"?';:{}()]", "");
        line = line.replace((char) 0, '.');
        return line ;
    }

    private void preProcess(String folder){
        File file = new File(folder);
        listOfFiles = file.listFiles();
        NumberOfDocs = listOfFiles.length;
        termDictionary = new HashMap<>();
        cacheVSS = new HashMap<>() ;
        cacheTPS = new HashMap<>();
        termID = new HashMap<>();
        int termid = 0;
        docSumSquared =  new HashMap<>() ;
        for (File f : listOfFiles){
            String docName = f.getName();
            int pos = 0;
            try{
                Scanner in = new Scanner(f);
                HashSet <String> words = new HashSet<>() ;
                while(in.hasNext()){
                    String line = in.nextLine();

                    line = polishWord(line) ;

                    StringTokenizer st = new StringTokenizer(line);

                    while (st.hasMoreTokens()){
                        String word = st.nextToken();
                        words.add(word) ;
                        if (termDictionary.containsKey(word) == false){
                            HashMap<String, ArrayList<Integer>> termPosition = new HashMap<>();
                            ArrayList<Integer> positions = new ArrayList<>();
                            positions.add(pos);
                            termPosition.put(docName, positions);
                            termDictionary.put(word, termPosition);
                        }else{
                            if (termDictionary.get(word).containsKey(docName) == false){
                                ArrayList<Integer> positions = new ArrayList<>();
                                positions.add(pos);
                                termDictionary.get(word).put(docName, positions);
                            }
                            else{
                                termDictionary.get(word).get(docName).add(pos);
                            }
                        }
                        pos++;
                        if (termID.containsKey(word) == false){
                            termID.put(word, termid);
                            termid++;
                        }
                    }
                }
                double squaredWeight = 0 ;
                for (String word : words) {
                    double w = weight(word, docName) ;
                    squaredWeight += w * w ;
                }
                docSumSquared.put(docName, Math.sqrt(squaredWeight)) ;
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }
        }

    }
    private boolean isDigit (char c) {
        return '0' <= c && c <= '9' ;
    }


    public ArrayList<String> topK (String query, int k){
        String [] queries = query.toLowerCase().split(" ") ;
        for (int i = 0 ; i < queries.length ; i ++) {
            queries[i] = polishWord(queries[i]);
        }
        queryVecSize = findQueryVec(queries) ;
        TreeSet <Pair<Double, String>> tree = new TreeSet<>();
        int a = 0;
        for (File file: listOfFiles){
            String doc = file.getName();
            tree.add(new Pair (Relevance(queries, doc), doc));
            if (tree.size() > k) {
                tree.remove(tree.last());
            }
        }
        ArrayList <String> topk = new ArrayList<>();
        for (Pair p : tree) {
            topk.add((String) p.e2) ;
            if (topk.size() == k) break ;
        }
        return topk;
    }

    private class Pair<T1, T2> implements Comparable<Pair<T1, T2>>{
        final T1 e1;
        final T2 e2;
        final boolean e1Comparable;
        final boolean e2Comparable;

        Pair(final T1 e1, T2 e2) {
            this.e1 = e1;
            this.e2 = e2;
            this.e1Comparable = e1 instanceof Comparable;
            this.e2Comparable = e2 instanceof Comparable;
        }

        @Override
        public int compareTo(Pair<T1, T2> o) {
            if (e1Comparable) {
                final int k = ((Comparable<T1>) e1).compareTo(o.e1);
                if (k < 0)
                    return 1;
                if (k > 0)
                    return -1;
            }
            if (e2Comparable) {
                final int k = ((Comparable<T2>) e2).compareTo(o.e2);
                if (k < 0)
                    return 1;
                if (k > 0)
                    return -1;
            }
            return 0;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Pair) {
                final Pair<T1, T2> o = (Pair<T1, T2>) obj;
                return (e1.equals(o.e1) && e2.equals(o.e2));
            } else {
                return false;
            }
        }
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 19 * hash + (e1 != null ? e1.hashCode() : 0);
            hash = 19 * hash + (e2 != null ? e2.hashCode() : 0);
            return hash;
        }
    }
}
