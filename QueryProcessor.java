import java.util.ArrayList;

public class QueryProcessor {
    private String folderName;
    private PositionalIndex PI;

    QueryProcessor(String folder){
        this.folderName = folder;
        PI = new PositionalIndex(folderName);
    }



    public ArrayList<String> topKDocs (String query, int k){

        ArrayList<String> res = PI.topK (query, k);
//        for (String doc : res) {
//            double relevance = 0.6*PI.cacheTPS.get(doc) + 0.4 * PI.cacheVSS.get(doc);
//            System.out.printf("File: %s, VSS = %.2f, TPS = %.2f, Relevance Scoere = %.2f\n",doc, PI.cacheVSS.get(doc), PI.cacheTPS.get(doc), relevance);
//        }
        return res ;
    }
}
