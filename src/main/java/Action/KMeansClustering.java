package Action;

import Util.FilterUtil;
import Util.MathUtil;
import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.json.simple.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class KMeansClustering {

    public MathUtil mathUtil;
    public CySwingAppAdapter adapter;
    public ArrayList<CyNode> allNodes;
    public FilterUtil filter;
    public CyNetworkView networkView;
    public static int TRESHOLD = 2;

    public KMeansClustering(CySwingAppAdapter adapter){
        this.mathUtil = new MathUtil();
        this.adapter  = adapter;

        // Getting necessary components from network
        CyApplicationManager manager = adapter.getCyApplicationManager();
        networkView = manager.getCurrentNetworkView();
        CyNetwork network = networkView.getModel();
        CyTable table = network.getDefaultNodeTable();

        filter = new FilterUtil(network, table);
        allNodes = filter.getAllNodes();
    }

    public ArrayList<ArrayList<String>> applyKMeansClustering(int maxClusterCount){

        ArrayList<ArrayList<String>> clusters = new ArrayList<>();
        int totalClusterCount = 0;
        for(CyNode node : allNodes){
            String nodeId = filter.getNodeId(node, adapter, "name");
            Integer clusterIndex = doesNodeFitsInAnyCluster(clusters, nodeId);
            if(clusterIndex != null){
                clusters.get(clusterIndex).add(nodeId);
            }else {
                clusters.add(new ArrayList<String>(){{add(nodeId);}});
                totalClusterCount++;
                if(totalClusterCount > maxClusterCount){
                    clusters = mergeNearestClusters(clusters);
                }
            }
        }

        drawClusters(clusters);
        return clusters;
    }

    public ArrayList<ArrayList<String>> mergeNearestClusters(ArrayList<ArrayList<String>> clusters){

        int minDistance = Integer.MAX_VALUE;
        int cluster1 = -1;
        int cluster2 = -1;

        for (int i=0; i<clusters.size()-1; i++){
            for (int j=i+1; j<clusters.size(); j++){
                int tempDistance = findMinDistanceBetweenClusters(clusters.get(i), clusters.get(j));
                if(tempDistance < minDistance){
                    minDistance = tempDistance;
                    cluster1 = i;
                    cluster2 = j;
                }
            }
        }

        clusters.get(cluster1).addAll(clusters.get(cluster2));
        clusters.remove(cluster2);

        return clusters;
    }

    public int findMinDistanceBetweenClusters(ArrayList<String> c1, ArrayList<String> c2){
        int minDistance = Integer.MAX_VALUE;

        for(String node1 : c1){
            for(String node2: c2){
                int tempDistance = mathUtil.calculate(node1, node2);
                if(tempDistance < minDistance){
                    minDistance = tempDistance;
                }
            }
        }

        return minDistance;
    }

    public Integer doesNodeFitsInAnyCluster(ArrayList<ArrayList<String>> clusters, String nodeId){
        Integer minDistance = Integer.MAX_VALUE;
        Integer clusterIndex = null;

        for(int j=0; j<clusters.size(); j++){
            for(int i=0; i<clusters.get(j).size(); i++){
                int tempDistance = mathUtil.calculate(clusters.get(j).get(i), nodeId);
                if(tempDistance < TRESHOLD){
                    if(minDistance > tempDistance){
                        minDistance = tempDistance;
                        clusterIndex = j;
                    }
                }
            }
        }

        return clusterIndex;
    }

    public void drawClusters(ArrayList<ArrayList<String>> clusters){
        int x = 0;
        int y = 0;

        for (int i=0; i<clusters.size(); i++){
            for (int j=0; j<clusters.get(i).size(); j++){
                CyNode node = filter.getNode(clusters.get(i).get(j), adapter, "name");
                networkView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
                networkView.getNodeView(node).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, y);
                x += (new Random().nextInt(5));
                y += (new Random().nextInt(5));
            }

            if(i != (clusters.size()-1)){
                x += (findMinDistanceBetweenClusters(clusters.get(i), clusters.get(i+1)) * 50);
                y += (findMinDistanceBetweenClusters(clusters.get(i), clusters.get(i+1)) * 50);
            }

        }
    }
}
