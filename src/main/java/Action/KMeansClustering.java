package Action;

import Util.FilterUtil;
import Util.MathUtil;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;

import javax.swing.*;
import java.util.*;

public class KMeansClustering {

    public MathUtil mathUtil;
    public CyNetwork network;
    public CyTable table;
    public CySwingAppAdapter adapter;
    public HashMap<String, CyNode> allNodes;
    public FilterUtil filter;
    public CyNetworkView networkView;
    public static int THRESHOLD = 10;

    public KMeansClustering(CySwingAppAdapter adapter){
        this.mathUtil = new MathUtil();
        this.adapter  = adapter;

        // Getting necessary components from network
        CyApplicationManager manager = adapter.getCyApplicationManager();
        networkView = manager.getCurrentNetworkView();
        network = networkView.getModel();
        table = network.getDefaultNodeTable();

        // Get all nodes from Cytoscape node table
        filter = new FilterUtil(network, table);
        allNodes = filter.getAllNodesWithId();
    }

    // This method applies incremental K-Means clustering algorithm according to a max cluster count
    // @param maxClusterCount: Maximum number of clusters
    // @return clusters: name(id) of all nodes within each cluster
    public ArrayList<ArrayList<String>> applyKMeansClustering(int maxClusterCount){
        ArrayList<ArrayList<String>> clusters = new ArrayList<>();              // Stores cluster names(names are actually represents id values)
        ArrayList<ArrayList<Integer>> clusterCentroids = new ArrayList<>();     // Numeric centroid values of clusters

        try {
            int totalClusterCount = 0;  // total cluster count
            Iterator<String> keyIterator = allNodes.keySet().iterator();        // get all keys(names) from nodes
            while (keyIterator.hasNext()){
                String key = keyIterator.next();                                // extract key for current node
                Integer clusterIndex = doesNodeFitsInAnyCluster(clusterCentroids, key); // check if it fits to any cluster
                if(clusterIndex != null){
                    // if it is, then put it into that cluster
                    clusters.get(clusterIndex).add(key);
                    // recalculate cluster centroid for that cluster
                    ArrayList<Integer> newCentroid = reCalculateClusterCentroid(clusters.get(clusterIndex));
                    clusterCentroids.get(clusterIndex).clear();
                    clusterCentroids.get(clusterIndex).addAll(newCentroid);
                }else {
                    // if it does not fit into that cluster, then create a new one
                    clusters.add(new ArrayList<String>(){{add(key);}});
                    clusterCentroids.add(extractAttributeValues(key));
                    totalClusterCount++;

                    // if total cluster count exceeds the max number, then merge two neares cluster
                    if(totalClusterCount > maxClusterCount){
                        clusters = mergeNearestClusters(clusters, clusterCentroids);
                    }
                }

            }

        }catch (Exception e){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),e.getMessage() + " -- " + e.toString(),
                    "Error", JOptionPane.INFORMATION_MESSAGE);
        }

        // draw clusters in cytocape
        drawClusters(clusters);
        return clusters;
    }

    /*
        public ArrayList<Integer> applyInformationGain(){
        ArrayList<Integer> columnsWithHighInfoGain = new ArrayList<>();

        AttributeSelection attributeSelection = new AttributeSelection();
        InfoGainAttributeEval infoGainAttributeEval = new InfoGainAttributeEval();
        Ranker search = new Ranker();
        attributeSelection.setEvaluator(infoGainAttributeEval);
        attributeSelection.setSearch(search);

        ArrayList<Attribute> attributes = new ArrayList<>();
        ArrayList<String> classValues = new ArrayList<String>(){{
            add("A");
            add("B");
            add("C");
            add("D");
            add("E");
            add("F");
        }};

        Set<String> attributeNames = table.getAllRows().get(1).getAllValues().keySet();
        Iterator<String> attributeNamesIterator = attributeNames.iterator();
        while (attributeNamesIterator.hasNext()){
            attributes.add(new Attribute(attributeNamesIterator.next(), new ArrayList<>()));
        }

        attributes.add(new Attribute("@@class@@", classValues));
        try {

            Instances dataSet = new Instances("TestInstances", attributes, 0);
            int rowCounter = 0;
            for(CyRow row : table.getAllRows()){
                Map<String, Object> rowValue = row.getAllValues();
                double[] instanceValues = new double[dataSet.numAttributes()];
                for(Object rowElement : rowValue.values()){
                    instanceValues[rowCounter] = dataSet.attribute(rowCounter).addStringValue(rowElement == null ? (String) null : rowElement.toString());
                    rowCounter++;
                }

                instanceValues[rowCounter] = dataSet.attribute(rowCounter).addStringValue("A");
                dataSet.add(new DenseInstance(1.0, instanceValues));
                rowCounter = 0;
            }

            attributeSelection.SelectAttributes(dataSet);

            double[][] attrRanks = attributeSelection.rankedAttributes();
        }catch (Exception e ){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),
                    e.getMessage() + " - " + e.toString(),
                    "Attribute Ranks", JOptionPane.INFORMATION_MESSAGE);
        }

        return columnsWithHighInfoGain;
    }
    */

    // This method is used to calculate centroid of a given cluster
    // @param cluster: Contains names of elements of that cluster
    // @return newCentroid: Numeric centroid values of given cluster
    public ArrayList<Integer> reCalculateClusterCentroid(ArrayList<String> cluster){
        ArrayList<Integer> newCentroid = new ArrayList<>();

        try {
            // for every node of given cluster
            for (int j=0; j<cluster.size(); j++){
                // extract numeric attribute values
                ArrayList<Integer> attributeValues = extractAttributeValues(cluster.get(j));
                // for the first node, divide values to size and add it to new centroid array
                if(newCentroid.size() == 0){
                    // for each attribute, divide value to size and add it to it's place
                    for (int k=0; k<attributeValues.size(); k++){
                        newCentroid.add((attributeValues.get(k)/cluster.size()));
                    }
                }else {
                    // for other nodes than the first one, add it to the current value after dividing them to size
                    for (int k=0; k<attributeValues.size(); k++){
                        newCentroid.set(k, newCentroid.get(k) + (attributeValues.get(k)/cluster.size()));
                    }
                }
            }

        }catch (Exception e){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),e.getMessage() + " -- " + e.toString(),
                    "recal", JOptionPane.INFORMATION_MESSAGE);
        }

        return newCentroid;
    }

    // This method is used to find nearest clusters according to their attributes
    // @param clusters: Name list of all nodes of clusters
    // @param clusterCentroids: Numeric values of cluster centroids
    public ArrayList<ArrayList<String>> mergeNearestClusters(ArrayList<ArrayList<String>> clusters, ArrayList<ArrayList<Integer>> clusterCentroids){

        int minDistance = Integer.MAX_VALUE;    // minimum distance between two cluster
        int cluster1 = -1;                      // index of first cluster
        int cluster2 = -1;                      // index of second cluster

        try {
            // find min distance between clusters by comparing them all
            for (int i=0; i<clusterCentroids.size()-1; i++){
                for (int j=i+1; j<clusterCentroids.size(); j++){
                    // find distance between current clusters
                    int tempDistance = findDistanceBetweenClusters(clusterCentroids.get(i), clusterCentroids.get(j));
                    if(tempDistance < minDistance){
                        // save indexes if they are the nearest
                        minDistance = tempDistance;
                        cluster1 = i;
                        cluster2 = j;
                    }
                }
            }

            if(cluster1 != -1){
                // combine two clusters and their centroids
                clusters.get(cluster1).addAll(clusters.get(cluster2));
                clusters.remove(cluster2);

                ArrayList<Integer> attributes = new ArrayList<Integer>();
                attributes.addAll((clusterCentroids.get(cluster1)));

                for (int i=0; i<attributes.size(); i++){
                    attributes.set(i, (attributes.get(i) + clusterCentroids.get(cluster2).get(i))/2);
                }

                clusterCentroids.get(cluster1).clear();
                clusterCentroids.get(cluster1).addAll(attributes);
                clusterCentroids.remove(cluster2);
            }

        }catch (Exception e){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),e.getMessage() + " -- " + e.toString(),
                    "merge", JOptionPane.INFORMATION_MESSAGE);
        }

        return clusters;
    }

    // this method is used to find distance between two clusters
    // @param c1: first cluster
    // @param c2: second cluster
    // @return distance: Distance between them
    public int findDistanceBetweenClusters(ArrayList<Integer> c1, ArrayList<Integer> c2){
        int distance = 0;

        try {
            // sum all attribute values
            for(int i=0; i<c1.size(); i++){
                distance += Math.abs(c1.get(i) - c2.get(i));
            }
        }catch (Exception e){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),e.getMessage() + " -- " + e.toString(),
                    "distance", JOptionPane.INFORMATION_MESSAGE);
        }

        return distance;
    }

    // This method is used to find if given node fits two any of clusters
    // @param clusterCentroids: Centroid value of clusters
    // @param key: Name of cluster
    // @return clusterIndex: Index of cluster which the given node fits best
    public Integer doesNodeFitsInAnyCluster(ArrayList<ArrayList<Integer>> clusterCentroids, String key){
        Integer minDistance = Integer.MAX_VALUE;                            // min distance between given node and clusters
        Integer clusterIndex = null;                                        // index of that cluster
        ArrayList<Integer> attributeValues = extractAttributeValues(key);   // extraxt values of given node
        try {
            // for all clusters
            for(int j=0; j<clusterCentroids.size(); j++){
                ArrayList<Integer> clusterCentroid = clusterCentroids.get(j);       // get cluster centroid

                // find distance between given node and current cluster
                int tempDistance = 0;
                for (int i=0; i<clusterCentroid.size(); i++){
                    tempDistance += Math.abs(clusterCentroid.get(i) - attributeValues.get(i));
                }

                // save distance and index if it is the nearest one for now
                if(tempDistance < minDistance){
                    minDistance = tempDistance;
                    clusterIndex = j;
                }
            }
        }catch (Exception e){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),"doesifts: " + e.toString() + " " + e.getMessage(),
                    "doesfits", JOptionPane.INFORMATION_MESSAGE);
        }

        if(minDistance > THRESHOLD){
            clusterIndex = null;
        }

        return clusterIndex;
    }

    // This method is used to extract attributes of a given node
    // @param node: Name of node
    // @return attributeValues: Numeric attribute values of given node
    public ArrayList<Integer> extractAttributeValues(String node){
        ArrayList<Integer> attributeValues = new ArrayList<>();

        try {
            Set<String> attributeNames = table.getAllRows().get(1).getAllValues().keySet();     // Get all attribute names
            Iterator<String> attributeIterator = attributeNames.iterator();                     // Create an iterator for names
            while (attributeIterator.hasNext()){
                String attributeName = attributeIterator.next();                                // Get current attribute name
                if(attributeName.startsWith("*")){                                              // If it starts with *, then this means that it is a numerical attribute
                    int value = (filter.getValueById(node, "name", attributeName) == null ? 0 :         // get value of that attribute
                            Integer.parseInt(filter.getValueById(node, "name", attributeName).toString())) ;
                    attributeValues.add(value);
                }
            }
        }catch (Exception e){
            JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),"doesifts: " + e.toString() + " " + e.getMessage(),
                    "extract", JOptionPane.INFORMATION_MESSAGE);
        }

        return attributeValues;
    }

    // This method is used to draw new clusters on Cytoscape
    // @param clusters: Id of all nodes of all clusters
    public void drawClusters(ArrayList<ArrayList<String>> clusters){
        int x = 0;      // x coordinate
        int y = 0;      // y coordinate

        for (int i=0; i<clusters.size(); i++){
            for (int j=0; j<clusters.get(i).size(); j++){
                // update location of current node
                networkView.getNodeView(allNodes.get(clusters.get(i).get(j))).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
                networkView.getNodeView(allNodes.get(clusters.get(i).get(j))).setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, y);
                // add random digits to current coordinates
                x += (new Random().nextInt(5));
                y += (new Random().nextInt(5));
            }

            // after visualizing a cluster, leave a huge space
            if(i != (clusters.size()-1)){
                x += 500;
                y += 500;
            }

            networkView.updateView();
        }

    }

}
