package Base;

import App.CytoVisProject;
import App.MyControlPanel;
import Action.*;

import java.io.*;
import java.util.*;
import javax.swing.*;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CompareGraphsCore {

    private Double THRESHOLD;
    private Double minThreshold;
    private Integer algorithm;
    private Double nodePropertyWeight;
    private Double edgePropertyWeight;
    private Double neighbourNodePropertyWeight;

    private CySwingAppAdapter adapter;
    private CytoVisProject cytoVisProject;
    private MyControlPanel myControlPanel;
    private DrawComparedGraphs drawComparedGraphs;

    public Double similarity;

    public String node1Path;
    public String node2Path;
    public String edge1Path;
    public String edge2Path;

    private String node1FileName;
    private String node2FileName;
    private String edge1FileName;
    private String edge2FileName;

    private Boolean ignorDifferentNodeTypes;

    private JFileChooser fileChooser;

    JSONArray firstGraphsNodes;
    JSONArray firstGraphsEdges;
    JSONArray secondGraphsNodes;
    JSONArray secondGraphsEdges;
    JSONArray attendanceList;

    ArrayList firstGraphNodeIdList;
    ArrayList secondGraphNodeIdList;
    ArrayList matchedParts;
    ArrayList similarNodePairs;

    public CompareGraphsCore(CytoVisProject cytoVisProject) {
        this.cytoVisProject = cytoVisProject;
        this.myControlPanel = cytoVisProject.getMyControlPanel();
        this.adapter        = cytoVisProject.getAdapter();
        this.similarity     = 1.0;
        this.attendanceList = new JSONArray();

        firstGraphsNodes  = new JSONArray();
        firstGraphsEdges  = new JSONArray();
        secondGraphsNodes = new JSONArray();
        secondGraphsEdges = new JSONArray();
        similarNodePairs  = new ArrayList();

        algorithm = 2;
        nodePropertyWeight = 2.0;
        edgePropertyWeight = 2.0;
        neighbourNodePropertyWeight = 1.0;
        THRESHOLD = 0.7;
        minThreshold = 0.3;
        ignorDifferentNodeTypes = false;
    }

    public boolean chooseFirstGraphsNode() {
        Boolean result = true;
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose First Graph's .csv File");
        if (fileChooser.showOpenDialog(fileChooser) == 0) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.node1Path = new String();
                this.setNode1Path(file.getAbsolutePath());
                this.node2FileName = new String();
                this.setNode1FileName(file.getName());
            }
        }

        return result;
    }

    public boolean chooseFirstGraphsEdge() {
        Boolean result = true;
        this.fileChooser = new JFileChooser();
        this.fileChooser.setDialogTitle("Choose First Graph's Edges .csv File");
        if (this.fileChooser.showOpenDialog(this.fileChooser) == 0) {
            File file = this.fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.edge1Path = new String();
                this.setEdge1Path(file.getAbsolutePath());
                this.edge1FileName = new String();
                this.setEdge1FileName(file.getName());
            }
        }

        return result;
    }

    public boolean chooseSecondGraphsNode() {
        Boolean result = true;
        this.fileChooser = new JFileChooser();
        this.fileChooser.setDialogTitle("Choose Second Graph's Nodes .csv File");
        if (this.fileChooser.showOpenDialog(this.fileChooser) == 0) {
            File file = this.fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.node2Path = new String();
                this.setNode2Path(file.getAbsolutePath());
                this.node2FileName = new String();
                this.setNode2FileName(file.getName());
            }
        }

        return result;
    }

    public boolean chooseSecondGraphsEdge(){
        Boolean result = true;
        this.fileChooser = new JFileChooser();
        this.fileChooser.setDialogTitle("Choose Second Graph's Edges .csv File");
        if (this.fileChooser.showOpenDialog(this.fileChooser) == 0) {
            File file = this.fileChooser.getSelectedFile();
            if (!file.getName().contains(".csv")) {
                result = false;
            } else {
                this.edge2Path = new String();
                this.setEdge2Path(file.getAbsolutePath());
                this.edge2FileName = new String();
                this.setEdge2FileName(file.getName());
            }
        }

        return result;
    }

    public Integer compareGraphs() {
        Integer result = 1;

        node1Path = "/home/erkan/Desktop/1n.csv";
        node2Path = "/home/erkan/Desktop/2n.csv";
        edge1Path = "/home/erkan/Desktop/1e.csv";
        edge2Path = "/home/erkan/Desktop/2e.csv";

        firstGraphsEdges  = new JSONArray();
        firstGraphsNodes  = new JSONArray();
        secondGraphsEdges = new JSONArray();
        secondGraphsNodes = new JSONArray();

        if(node1Path != null && node2Path != null && edge1Path != null && edge2Path != null){
            this.readGraphs();
        }else{
            result = 0;
        }

        return result;
    }

    // Finding similar node pairs via greedy approach
    public void findSimilarNodePairsWithGreedyApproach(){
        similarNodePairs = new ArrayList();

        // Finding node pair which has biggest attendance value
        JSONObject peakAttendanceNodePair = (JSONObject) attendanceList.get(0);
        for(int i=1; i<attendanceList.size(); i++){
            if(Double.parseDouble(((JSONObject) attendanceList.get(i)).get("attendance").toString()) > Double.parseDouble(peakAttendanceNodePair.get("attendance").toString())){
                peakAttendanceNodePair = (JSONObject) attendanceList.get(i);
            }
        }

        if(Double.parseDouble(peakAttendanceNodePair.get("attendance").toString()) > THRESHOLD){
            JSONArray L = new JSONArray();
            JSONArray S = new JSONArray();

            S.add(peakAttendanceNodePair);
            L.add(peakAttendanceNodePair);

            System.out.println(peakAttendanceNodePair.get("firstNode").toString() + " and " + peakAttendanceNodePair.get("secondNode").toString() +
                    " are similar with attendace value " + peakAttendanceNodePair.get("attendance").toString());
            for(int k=0; k<L.size(); k++){
                ArrayList<String> connectedNodesToFirst = findConnectNodes(((JSONObject) L.get(k)).get("firstNode").toString(), true);
                for (int i=0; i<connectedNodesToFirst.size(); i++){
                    JSONObject tempPeakAttendance = new JSONObject(){{put("attendance", 0.0);}};
                    ArrayList<String> connectedNodesToSecond = findConnectNodes(((JSONObject) L.get(k)).get("secondNode").toString(), false);
                    for(int j=0; j<connectedNodesToSecond.size(); j++){
                        if(!checkIfNodeAdded(S, connectedNodesToFirst.get(i), connectedNodesToSecond.get(j))
                                && Double.parseDouble(((JSONObject) tempPeakAttendance).get("attendance").toString()) < getAttendance(connectedNodesToFirst.get(i), connectedNodesToSecond.get(j))){
                            tempPeakAttendance = new JSONObject();
                            tempPeakAttendance.put("attendance", getAttendance(connectedNodesToFirst.get(i), connectedNodesToSecond.get(j)));
                            tempPeakAttendance.put("firstNode", connectedNodesToFirst.get(i));
                            tempPeakAttendance.put("secondNode", connectedNodesToSecond.get(j));
                        }
                    }

                    if(Double.parseDouble(((JSONObject) tempPeakAttendance).get("attendance").toString()) > THRESHOLD){
                        L.add(tempPeakAttendance);
                        S.add(tempPeakAttendance);

                        System.out.println(tempPeakAttendance.get("firstNode").toString() + " and " + tempPeakAttendance.get("secondNode").toString() +
                                " are similar with attendace value " + tempPeakAttendance.get("attendance").toString());

                    }
                }
            }

            ArrayList temp;
            for(int i=0; i<S.size(); i++){
                temp = new ArrayList();
                temp.add(((JSONObject) S.get(i)).get("firstNode"));
                temp.add(((JSONObject) S.get(i)).get("secondNode"));
                similarNodePairs.add(temp);
            }
        }else {
            return;
        }

    }

    public Boolean checkIfNodeAdded(JSONArray list, String firstNode, String secondNode){
        Boolean result = false;
        for(int i=0; i<list.size(); i++){
            if(((JSONObject) list.get(i)).get("firstNode").toString().equals(firstNode) && ((JSONObject) list.get(i)).get("secondNode").toString().equals(secondNode)){
                result = true;
                break;
            }
        }

        return result;
    }

    // Finding similar node pairs with sorting attendance list
    public void findSimilarNodePairsWithSorting(){

        if(algorithm == 1){
            findSimilarNodePairsWithBruteForce();
            return;
        }

        // Sorting attendace list
        long startTime  = System.currentTimeMillis();
        for(int i=0; i<attendanceList.size()-1; i++){
            JSONObject max      = (JSONObject) attendanceList.get(i);
            Integer maxIndex    = i;
            for(int j=i+1; j<attendanceList.size(); j++){
                if(Double.parseDouble(((JSONObject) attendanceList.get(j)).get("attendance").toString()) > Double.parseDouble(max.get("attendance").toString())){
                    maxIndex = j;
                    max = (JSONObject) attendanceList.get(j);
                }
            }

            JSONObject temp = (JSONObject) attendanceList.get(i);
            attendanceList.set(i, max);
            attendanceList.set(maxIndex, temp);
        }
        System.out.println("Total time to sort attendance list: " + (System.currentTimeMillis() - startTime));

        System.out.println("Attendance List: " + attendanceList.toString());

        // Attendance listesinde
        ArrayList<String> differentNodes = new ArrayList<>();
        for(int i=0; i<attendanceList.size(); i++){
            JSONObject temp = (JSONObject) attendanceList.get(i);

            if(!differentNodes.contains(temp.get("firstNode").toString())){
                differentNodes.add(temp.get("firstNode").toString());
            }

            if(!differentNodes.contains(temp.get("secondNode").toString())){
                differentNodes.add(temp.get("secondNode").toString());
            }
        }

        JSONArray tempAttendanceList = new JSONArray();
        for(int i=0; i<differentNodes.size(); i++){
            for(int j=0; j<attendanceList.size(); j++){
                JSONObject temp = (JSONObject) attendanceList.get(j);
                if(!checkIfNodeExist(temp.get("secondNode").toString(), tempAttendanceList) && !checkIfNodeExist(temp.get("firstNode").toString(), tempAttendanceList)){
                    tempAttendanceList.add(temp);
                    break;
                }
            }
        }

        attendanceList = tempAttendanceList;
        try{
            File file = new File("/home/erkan/Desktop/souts.txt");
            file.createNewFile();
            PrintWriter writer = new PrintWriter(file);

            // Getting similar node pairs that attendace value's is bigger than threshold
            similarNodePairs = new JSONArray();
            for(int i=0; i<attendanceList.size(); i++){
                if(Double.parseDouble(((JSONObject) attendanceList.get(i)).get("attendance").toString()) > THRESHOLD){
                    ArrayList tempArrayList = new ArrayList();
                    tempArrayList.add(((JSONObject)attendanceList.get(i)).get("firstNode").toString());
                    tempArrayList.add(((JSONObject)attendanceList.get(i)).get("secondNode").toString());
                    similarNodePairs.add(tempArrayList);
                }else {
                    break;
                }

                System.out.println(((JSONObject)attendanceList.get(i)).get("firstNode").toString() + " - " + ((JSONObject)attendanceList.get(i)).get("secondNode").toString() +
                        ": " + ((JSONObject) attendanceList.get(i)).get("attendance").toString());
            }

            writer.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void findSimilarNodePairsWithBruteForce(){
        Integer i;
        Integer j;

        similarNodePairs = new ArrayList();
        if(algorithm == 2){
            for(i=1; i<firstGraphsNodes.size(); i++){
                for(j=1; j<secondGraphsNodes.size(); j++){
                    if(getAttendance(((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString(),
                            ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString()) > THRESHOLD){
                        ArrayList tempArrayList = new ArrayList();
                        tempArrayList.add(((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString());
                        tempArrayList.add(((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString());
                        similarNodePairs.add(tempArrayList);

                        System.out.println(((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString() + " and " + ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString() +
                            " are similar with attendance value " + getAttendance(((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString(),
                                ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString()));
                    }
                }
            }
        }else if(algorithm == 1){
            for(i=0; i<attendanceList.size(); i++){
                if((Boolean) ((JSONObject) attendanceList.get(i)).get("isSimilar")){
                    ArrayList tempArrayList = new ArrayList();
                    tempArrayList.add(((JSONObject) attendanceList.get(i)).get("firstNode").toString());
                    tempArrayList.add(((JSONObject) attendanceList.get(i)).get("secondNode").toString());
                    similarNodePairs.add(tempArrayList);
                }
            }
        }
    }

    public void compareGraphsChenVersion(){
        readGraphs();

        ArrayList<String> nodePropertyList = getPropertyList(false);
        ArrayList<String> edgePropertyList = getPropertyList(true);
        attendanceList = new JSONArray();

        Double thresholdCoefficient = determineThreshold(nodePropertyList.size(), edgePropertyList.size());
        System.out.println("First graph: " + firstGraphsNodes.toString() + " - " + firstGraphsEdges.toString());
        System.out.println("Second graph: " + secondGraphsNodes.toString() + " - " + secondGraphsEdges.toString());



    }

    public void createAttendanceListChenVersion(){
        Integer     i;
        Integer     j;
        Integer     node1index;
        Integer     node2index;
        String      adjacentOfFirstNode;
        String      adjacentOfSecondNode;
        Double      tempSimilarity;

        ArrayList<String> nodePropertyList = getPropertyList(false);
        ArrayList<String> edgePropertyList = getPropertyList(true);
        attendanceList = new JSONArray();

        Double thresholdCoefficient = determineThreshold(nodePropertyList.size(), edgePropertyList.size());
        System.out.println("Threshold Coefficient: " + thresholdCoefficient + "\n" + "Algorithm: " + algorithm);
        System.out.println("Weights: " + nodePropertyWeight + "-" + edgePropertyWeight + "-" + neighbourNodePropertyWeight);
        System.out.println("Treshold:: " + THRESHOLD);

        for(i=1; i<firstGraphsNodes.size(); i++){
            for(j=1; j<secondGraphsNodes.size(); j++){
                ArrayList<String> incidentNodesForNode1 = findIncidentNodes(((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString(), true);
                ArrayList<String> incidentNodesForNode2 = findIncidentNodes(((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString(), false);
                ArrayList<String> incidentListForSmallerDegree;
                ArrayList<String> incidentListForLargerDegree;
                String node1;
                String node2;
                Boolean isFirstForBigger;
                Boolean isFirstForLower;

                JSONObject nodepair = new JSONObject();
                nodepair.put("firstNode", ((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString());
                nodepair.put("secondNode", ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString());
                nodepair.put("isSimilar", true);

                Integer s1 = 0;
                Integer s2 = 0;
                if(incidentNodesForNode1.size() < incidentNodesForNode2.size()){
                    s1 = incidentNodesForNode1.size();
                    s2 = incidentNodesForNode2.size();
                    isFirstForBigger = true;
                    isFirstForLower  = false;
                    incidentListForSmallerDegree = incidentNodesForNode1;
                    incidentListForLargerDegree = incidentNodesForNode2;
                    node1 = ((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString();
                    node2 = ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString();
                }else{
                    s1 = incidentNodesForNode2.size();
                    s2 = incidentNodesForNode1.size();
                    isFirstForBigger = false;
                    isFirstForLower  = true;
                    incidentListForSmallerDegree = incidentNodesForNode2;
                    incidentListForLargerDegree = incidentNodesForNode1;
                    node2 = ((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString();
                    node1 = ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString();
                }

                JSONArray connectivityArray = new JSONArray();
                for(node1index=0; node1index<s1; node1index++){
                    adjacentOfFirstNode = incidentListForSmallerDegree.get(node1index);

                    for(node2index=0; node2index<s2; node2index++){
                        tempSimilarity = 1.0;
                        adjacentOfSecondNode = incidentListForLargerDegree.get(node2index);
                        JSONObject edgepair = new JSONObject();
                        edgepair.put("adjacent1", adjacentOfFirstNode);
                        edgepair.put("adjacent2", adjacentOfSecondNode);

                        if(algorithm == 2){
                            for(String property : edgePropertyList){
                                if(((JSONObject)firstGraphsEdges.get(0)).containsValue(property) && ((JSONObject)secondGraphsEdges.get(0)).containsValue(property)){
                                    if(findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger) != null &&
                                            findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower) != null){
                                        if(findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger).length() >= 1 &&
                                                findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                            if(!findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger)
                                                    .equals(findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower))){
                                                if(property.equals("edgeType")){
                                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-(edgePropertyWeight))/(thresholdCoefficient));
                                                }else{
                                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-edgePropertyWeight)/(thresholdCoefficient));
                                                }
                                            }
                                        }else{
                                            if(findEdgeProperty(node1, adjacentOfFirstNode,
                                                    property, isFirstForBigger).length() >= 1 || findEdgeProperty(node2,
                                                    adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                                tempSimilarity = tempSimilarity * ((thresholdCoefficient-(edgePropertyWeight))/(thresholdCoefficient));
                                            }
                                        }
                                    }else{
                                        if(findEdgeProperty(node1, adjacentOfFirstNode,
                                                property, isFirstForBigger) != null || findEdgeProperty(node2,
                                                adjacentOfSecondNode, property, isFirstForLower) != null) {
                                            tempSimilarity = tempSimilarity * ((thresholdCoefficient-(edgePropertyWeight))/(thresholdCoefficient));
                                        }
                                    }
                                }else{
                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-(edgePropertyWeight))/(thresholdCoefficient));
                                }
                            }

                            for(String property : nodePropertyList){
                                if(((JSONObject)firstGraphsNodes.get(0)).containsValue(property) && ((JSONObject)secondGraphsNodes.get(0)).containsValue(property)){
                                    if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger) != null &&
                                            findNodeProperty(adjacentOfSecondNode, property, isFirstForLower) != null){
                                        if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).length() >= 1 &&
                                                findNodeProperty(adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                            if(!findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).equals(findNodeProperty(adjacentOfSecondNode, property, isFirstForLower))){
                                                if(property.equals("nodeType")){
                                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-(neighbourNodePropertyWeight))/(thresholdCoefficient));
                                                }else{
                                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-neighbourNodePropertyWeight)/(thresholdCoefficient));
                                                }
                                            }
                                        }else{
                                            if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).length() >= 1 ||
                                                    findNodeProperty(adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                                tempSimilarity = tempSimilarity * ((thresholdCoefficient-(neighbourNodePropertyWeight))/(thresholdCoefficient));
                                            }
                                        }
                                    }else{
                                        if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger) != null ||
                                                findNodeProperty(adjacentOfSecondNode, property, isFirstForLower) != null) {
                                            tempSimilarity = tempSimilarity * ((thresholdCoefficient-(neighbourNodePropertyWeight))/(thresholdCoefficient));
                                        }
                                    }
                                }else{
                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-(neighbourNodePropertyWeight))/(thresholdCoefficient));
                                }
                            }

                            edgepair.put("attendance", tempSimilarity);
                            connectivityArray.add(edgepair);
                        }else if(algorithm == 1){
                            for(String property:edgePropertyList){
                                if(property.equals("interaction")){
                                    if(!findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger).equals(findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower))){
                                        nodepair.put("isSimilar", false);
                                        break;
                                    }else {
                                        nodepair.put("isSimilar", true);
                                    }
                                }
                            }

                            for(String property:nodePropertyList){
                                if(property.equals("nodeType")){
                                    if(!findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).equals(findNodeProperty(adjacentOfSecondNode, property, isFirstForLower))){
                                        nodepair.put("isSimilar", false);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                Double attendance = 1.0;

                if(algorithm == 2){
                    for(String property : nodePropertyList){
                        if(((JSONObject)firstGraphsNodes.get(0)).containsValue(property) && ((JSONObject)secondGraphsNodes.get(0)).containsValue(property)){
                            if(findNodeProperty(node1, property, isFirstForBigger) != null &&
                                    findNodeProperty(node2, property, isFirstForLower) != null){
                                if(findNodeProperty(node1, property, isFirstForBigger).length() >= 1 &&
                                        findNodeProperty(node2, property, isFirstForLower).length() >= 1){
                                    if(!findNodeProperty(node1, property, isFirstForBigger)
                                            .equals(findNodeProperty(node2, property, isFirstForLower))){
                                        if(property.equals("nodeType")){
                                            attendance = attendance * ((thresholdCoefficient - (nodePropertyWeight))/(thresholdCoefficient));
                                        }else{
                                            attendance = attendance * ((thresholdCoefficient - nodePropertyWeight)/(thresholdCoefficient));
                                        }
                                    }
                                }else{
                                    if(findNodeProperty(node1, property, isFirstForBigger).length() >= 1 ||
                                            findNodeProperty(node2, property, isFirstForLower).length() >= 1){
                                        attendance = attendance * ((thresholdCoefficient - (nodePropertyWeight))/(thresholdCoefficient));
                                    }
                                }
                            }else {
                                if (findNodeProperty(((JSONObject) firstGraphsNodes.get(i)).get("nodeID").toString(), property, true) != null ||
                                        findNodeProperty(((JSONObject) secondGraphsNodes.get(j)).get("nodeID").toString(), property, false) != null) {
                                    attendance = attendance * ((thresholdCoefficient - (nodePropertyWeight))/(thresholdCoefficient));
                                }
                            }
                        }else {
                            attendance = attendance * ((thresholdCoefficient - (nodePropertyWeight))/(thresholdCoefficient));
                        }
                    }

                    ArrayList<Double> attendanceList = new ArrayList<>();
                    ArrayList<String>  adjacent1List  = new ArrayList<>();
                    ArrayList<String>  adjacent2List  = new ArrayList<>();

                    for(int t=0; t<connectivityArray.size(); t++){
                        attendanceList.add(Double.parseDouble(((JSONObject) connectivityArray.get(t)).get("attendance").toString()));
                        adjacent1List.add(((JSONObject) connectivityArray.get(t)).get("adjacent1").toString());
                        adjacent2List.add(((JSONObject) connectivityArray.get(t)).get("adjacent2").toString());
                    }

                    for(int t=0; t<attendanceList.size()-1; t++){
                        for(int z=t+1; z<attendanceList.size(); z++){
                            if(attendanceList.get(z) > attendanceList.get(t)){
                                Double temp = attendanceList.get(t);
                                attendanceList.set(t, attendanceList.get(z));
                                attendanceList.set(z, temp);

                                String temp2 = adjacent1List.get(t);
                                adjacent1List.set(t, adjacent1List.get(z));
                                adjacent1List.set(z, temp2);

                                String temp3 = adjacent2List.get(t);
                                adjacent2List.set(t, adjacent2List.get(z));
                                adjacent2List.set(z, temp3);
                            }
                        }
                    }

                    JSONArray resultList = new JSONArray();
                    for(int t=0; t<attendanceList.size(); t++){
                        if(!checkIfEdgeExist(resultList, adjacent1List.get(t), adjacent2List.get(t), attendanceList.get(t))){
                            JSONObject object = new JSONObject();
                            object.put("adjacent1", adjacent1List.get(t));
                            object.put("adjacent2", adjacent2List.get(t));
                            object.put("attendance", attendanceList.get(t));
                            resultList.add(object);
                        }
                    }

                    for(int t=0; t<resultList.size(); t++){
                        attendance = attendance * Double.parseDouble(((JSONObject) resultList.get(t)).get("attendance").toString());
                    }

                    nodepair.put("attendance", attendance);
                }else if(algorithm == 1){
                    for(String property:nodePropertyList){
                        if(property.equals("nodeType")){
                            if(!findNodeProperty(node1, property, isFirstForBigger)
                                    .equals(findNodeProperty(node2, property, isFirstForLower))){
                                nodepair.put("isSimilar", false);
                                break;
                            }
                        }
                    }

                    if(incidentNodesForNode1.size() != incidentNodesForNode2.size()){
                        nodepair.put("isSimilar", false);
                    }
                }

                if(algorithm == 2){
                    attendanceList.add(nodepair);
                }else if(algorithm == 1){
                    if((Boolean) nodepair.get("isSimilar")){
                        attendanceList.add(nodepair);
                    }
                }

            }
        }
    }

    public void createAttendanceList(){
        Integer     i;
        Integer     j;
        Integer     node1index;
        Integer     node2index;
        String      adjacentOfFirstNode;
        String      adjacentOfSecondNode;
        Double      tempSimilarity;

        ArrayList<String> nodePropertyList = getPropertyList(false);
        ArrayList<String> edgePropertyList = getPropertyList(true);
        attendanceList = new JSONArray();

        Double thresholdCoefficient = determineThreshold(nodePropertyList.size(), edgePropertyList.size());
        System.out.println("Threshold Coefficient: " + thresholdCoefficient + "\n" + "Algorithm: " + algorithm + "\nMin Threshold: " + minThreshold);
        System.out.println("Weights: " + nodePropertyWeight + "-" + edgePropertyWeight + "-" + neighbourNodePropertyWeight);
        System.out.println("Treshold:: " + THRESHOLD);

        Integer counter = 0;
        for(i=1; i<firstGraphsNodes.size(); i++){
            for(j=1; j<secondGraphsNodes.size(); j++){
                ArrayList<String> incidentNodesForNode1 = findIncidentNodes(((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString(), true);
                ArrayList<String> incidentNodesForNode2 = findIncidentNodes(((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString(), false);
                ArrayList<String> incidentListForSmallerDegree;
                ArrayList<String> incidentListForLargerDegree;
                String node1;
                String node2;
                Boolean isFirstForBigger;
                Boolean isFirstForLower;

                JSONObject nodepair = new JSONObject();
                nodepair.put("firstNode", ((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString());
                nodepair.put("secondNode", ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString());
                nodepair.put("isSimilar", true);

                Integer s1 = 0;
                Integer s2 = 0;
                if(incidentNodesForNode1.size() < incidentNodesForNode2.size()){
                    s1 = incidentNodesForNode1.size();
                    s2 = incidentNodesForNode2.size();
                    isFirstForBigger = true;
                    isFirstForLower  = false;
                    incidentListForSmallerDegree = incidentNodesForNode1;
                    incidentListForLargerDegree = incidentNodesForNode2;
                    node1 = ((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString();
                    node2 = ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString();
                }else{
                    s1 = incidentNodesForNode2.size();
                    s2 = incidentNodesForNode1.size();
                    isFirstForBigger = false;
                    isFirstForLower  = true;
                    incidentListForSmallerDegree = incidentNodesForNode2;
                    incidentListForLargerDegree = incidentNodesForNode1;
                    node2 = ((JSONObject)firstGraphsNodes.get(i)).get("nodeID").toString();
                    node1 = ((JSONObject)secondGraphsNodes.get(j)).get("nodeID").toString();
                }

                if(getIgnorDifferentNodeTypes() && !findNodeProperty(node1, "nodeType", isFirstForBigger).equals(findNodeProperty(node2, "nodeType", isFirstForLower))){
                    if(algorithm == 2){
                        nodepair.put("attendance", 0.0);
                    }else if(algorithm == 1){
                        nodepair.put("isSimilar", false);
                    }
                }else {
                    JSONArray connectivityArray = new JSONArray();
                    for(node1index=0; node1index<s1; node1index++){
                        adjacentOfFirstNode = incidentListForSmallerDegree.get(node1index);

                        for(node2index=0; node2index<s2; node2index++){
                            tempSimilarity = 1.0;
                            adjacentOfSecondNode = incidentListForLargerDegree.get(node2index);
                            JSONObject edgepair = new JSONObject();
                            edgepair.put("adjacent1", adjacentOfFirstNode);
                            edgepair.put("adjacent2", adjacentOfSecondNode);

                            if(algorithm == 2){
                                for(String property : edgePropertyList){
                                    if(((JSONObject)firstGraphsEdges.get(0)).containsValue(property) && ((JSONObject)secondGraphsEdges.get(0)).containsValue(property)){
                                        if(findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger) != null &&
                                                findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower) != null){
                                            if(findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger).length() >= 1 &&
                                                    findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                                if(!findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger)
                                                        .equals(findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower))){
                                                    if(property.equals("edgeType")){
                                                        tempSimilarity = tempSimilarity * ((thresholdCoefficient-(2*edgePropertyWeight))/(thresholdCoefficient));
                                                    }else{
                                                        tempSimilarity = tempSimilarity * ((thresholdCoefficient-2*edgePropertyWeight)/(thresholdCoefficient));
                                                    }
                                                }
                                            }else{
                                                if(findEdgeProperty(node1, adjacentOfFirstNode,
                                                        property, isFirstForBigger).length() >= 1 || findEdgeProperty(node2,
                                                        adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-(edgePropertyWeight / 2))/(thresholdCoefficient));
                                                }
                                            }
                                        }else{
                                            if(findEdgeProperty(node1, adjacentOfFirstNode,
                                                    property, isFirstForBigger) != null || findEdgeProperty(node2,
                                                    adjacentOfSecondNode, property, isFirstForLower) != null) {
                                                tempSimilarity = tempSimilarity * ((thresholdCoefficient-(edgePropertyWeight / 2))/(thresholdCoefficient));
                                            }
                                        }
                                    }else{
                                        tempSimilarity = tempSimilarity * ((thresholdCoefficient-(edgePropertyWeight / 2))/(thresholdCoefficient));
                                    }
                                }

                                for(String property : nodePropertyList){
                                    if(((JSONObject)firstGraphsNodes.get(0)).containsValue(property) && ((JSONObject)secondGraphsNodes.get(0)).containsValue(property)){
                                        if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger) != null &&
                                                findNodeProperty(adjacentOfSecondNode, property, isFirstForLower) != null){
                                            if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).length() >= 1 &&
                                                    findNodeProperty(adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                                if(!findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).equals(findNodeProperty(adjacentOfSecondNode, property, isFirstForLower))){
                                                    if(property.equals("nodeType")){
                                                        tempSimilarity = tempSimilarity * ((thresholdCoefficient-(2*neighbourNodePropertyWeight))/(thresholdCoefficient));
                                                    }else{
                                                        tempSimilarity = tempSimilarity * ((thresholdCoefficient-neighbourNodePropertyWeight)/(thresholdCoefficient));
                                                    }
                                                }
                                            }else{
                                                if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).length() >= 1 ||
                                                        findNodeProperty(adjacentOfSecondNode, property, isFirstForLower).length() >= 1){
                                                    tempSimilarity = tempSimilarity * ((thresholdCoefficient-(neighbourNodePropertyWeight / 2))/(thresholdCoefficient));
                                                }
                                            }
                                        }else{
                                            if(findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger) != null ||
                                                    findNodeProperty(adjacentOfSecondNode, property, isFirstForLower) != null) {
                                                tempSimilarity = tempSimilarity * ((thresholdCoefficient-(neighbourNodePropertyWeight / 2))/(thresholdCoefficient));
                                            }
                                        }
                                    }else{
                                        tempSimilarity = tempSimilarity * ((thresholdCoefficient-(neighbourNodePropertyWeight / 2))/(thresholdCoefficient));
                                    }
                                }

                                edgepair.put("attendance", tempSimilarity);
                                connectivityArray.add(edgepair);
                            }else if(algorithm == 1){
                                for(String property:edgePropertyList){
                                    if(property.equals("edgeType")){
                                        if(!findEdgeProperty(node1, adjacentOfFirstNode, property, isFirstForBigger).equals(findEdgeProperty(node2, adjacentOfSecondNode, property, isFirstForLower))){
                                            nodepair.put("isSimilar", false);
                                        }else {
                                            nodepair.put("isSimilar", true);
                                        }

                                        break;
                                    }
                                }

                                for(String property:nodePropertyList){
                                    if(property.equals("nodeType")){
                                        if(!findNodeProperty(adjacentOfFirstNode, property, isFirstForBigger).equals(findNodeProperty(adjacentOfSecondNode, property, isFirstForLower))){
                                            nodepair.put("isSimilar", false);
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if(algorithm == 1){
                            if((Boolean) nodepair.get("isSimilar")){
                                break;
                            }
                        }
                    }

                    Double attendance = 1.0;

                    if(algorithm == 2){
                        for(String property : nodePropertyList){
                            if(((JSONObject)firstGraphsNodes.get(0)).containsValue(property) && ((JSONObject)secondGraphsNodes.get(0)).containsValue(property)){
                                if(findNodeProperty(node1, property, isFirstForBigger) != null &&
                                        findNodeProperty(node2, property, isFirstForLower) != null){
                                    if(findNodeProperty(node1, property, isFirstForBigger).length() >= 1 &&
                                            findNodeProperty(node2, property, isFirstForLower).length() >= 1){
                                        if(!findNodeProperty(node1, property, isFirstForBigger)
                                                .equals(findNodeProperty(node2, property, isFirstForLower))){
                                            if(property.equals("nodeType")){
                                                attendance = attendance * ((thresholdCoefficient - (2*nodePropertyWeight))/(thresholdCoefficient));
                                            }else{
                                                attendance = attendance * ((thresholdCoefficient - nodePropertyWeight)/(thresholdCoefficient));
                                            }
                                        }
                                    }else{
                                        if(findNodeProperty(node1, property, isFirstForBigger).length() >= 1 ||
                                                findNodeProperty(node2, property, isFirstForLower).length() >= 1){
                                            attendance = attendance * ((thresholdCoefficient - (nodePropertyWeight / 2))/(thresholdCoefficient));
                                        }
                                    }
                                }else {
                                    if (findNodeProperty(((JSONObject) firstGraphsNodes.get(i)).get("nodeID").toString(), property, true) != null ||
                                            findNodeProperty(((JSONObject) secondGraphsNodes.get(j)).get("nodeID").toString(), property, false) != null) {
                                        attendance = attendance * ((thresholdCoefficient - (nodePropertyWeight / 2))/(thresholdCoefficient));
                                    }
                                }
                            }else {
                                attendance = attendance * ((thresholdCoefficient - (nodePropertyWeight / 2))/(thresholdCoefficient));
                            }
                        }

                        if(incidentNodesForNode1.size() != 0 || incidentNodesForNode2.size() != 0){
                            if(incidentNodesForNode1.size() > incidentNodesForNode2.size()){
                                if(incidentNodesForNode2.size() != 0){
                                    attendance = attendance * incidentNodesForNode2.size() / incidentNodesForNode1.size();
                                }else{
                                    attendance = attendance * 1 / (incidentNodesForNode1.size() + 1);
                                }
                            }else{
                                if(incidentNodesForNode1.size() != 0){
                                    attendance = attendance * incidentNodesForNode1.size() / incidentNodesForNode2.size();
                                }else {
                                    attendance = attendance * 1 / (incidentNodesForNode2.size() + 1);
                                }

                            }
                        }

                        ArrayList<Double> attendanceList = new ArrayList<>();
                        ArrayList<String>  adjacent1List  = new ArrayList<>();
                        ArrayList<String>  adjacent2List  = new ArrayList<>();

                        for(int t=0; t<connectivityArray.size(); t++){
                            attendanceList.add(Double.parseDouble(((JSONObject) connectivityArray.get(t)).get("attendance").toString()));
                            adjacent1List.add(((JSONObject) connectivityArray.get(t)).get("adjacent1").toString());
                            adjacent2List.add(((JSONObject) connectivityArray.get(t)).get("adjacent2").toString());
                        }

                        for(int t=0; t<attendanceList.size()-1; t++){
                            for(int z=t+1; z<attendanceList.size(); z++){
                                if(attendanceList.get(z) > attendanceList.get(t)){
                                    Double temp = attendanceList.get(t);
                                    attendanceList.set(t, attendanceList.get(z));
                                    attendanceList.set(z, temp);

                                    String temp2 = adjacent1List.get(t);
                                    adjacent1List.set(t, adjacent1List.get(z));
                                    adjacent1List.set(z, temp2);

                                    String temp3 = adjacent2List.get(t);
                                    adjacent2List.set(t, adjacent2List.get(z));
                                    adjacent2List.set(z, temp3);
                                }
                            }
                        }

                        JSONArray resultList = new JSONArray();
                        for(int t=0; t<attendanceList.size(); t++){
                            if(!checkIfEdgeExist(resultList, adjacent1List.get(t), adjacent2List.get(t), attendanceList.get(t))){
                                JSONObject object = new JSONObject();
                                object.put("adjacent1", adjacent1List.get(t));
                                object.put("adjacent2", adjacent2List.get(t));
                                object.put("attendance", attendanceList.get(t));
                                resultList.add(object);
                            }
                        }

                        for(int t=0; t<resultList.size(); t++){
                            attendance = attendance * Double.parseDouble(((JSONObject) resultList.get(t)).get("attendance").toString());
                        }

                        nodepair.put("attendance", attendance);
                    }else if(algorithm == 1){
                        for(String property:nodePropertyList){
                            if(property.equals("nodeType")){
                                if(!findNodeProperty(node1, property, isFirstForBigger)
                                        .equals(findNodeProperty(node2, property, isFirstForLower))){
                                    nodepair.put("isSimilar", false);
                                    break;
                                }
                            }
                        }

                        if(incidentNodesForNode1.size() != incidentNodesForNode2.size()){
                            nodepair.put("isSimilar", false);
                        }
                    }
                }

                if(algorithm == 2){
                    if(Double.parseDouble(nodepair.get("attendance").toString()) > minThreshold){
                        attendanceList.add(nodepair);
                    }
                }else if(algorithm == 1){
                    if((Boolean) nodepair.get("isSimilar")){
                        attendanceList.add(nodepair);
                    }
                }

            }
        }
    }

    public JSONObject getNodePair(String nodeId){
        JSONObject result = new JSONObject();
        for(int i=0; i<attendanceList.size(); i++){
            JSONObject temp = (JSONObject) attendanceList.get(i);
            if(temp.get("firstNode").equals(nodeId) || temp.get("secondNode").equals(nodeId)){
                result = temp;
                break;
            }
        }

        return result;
    }

    public Double getAttendance(String nodeId){
        Double result = new Double(1);
        for(int i=0; i<attendanceList.size(); i++){
            JSONObject temp = (JSONObject) attendanceList.get(i);
            if(temp.get("firstNode").equals(nodeId) || temp.get("secondNode").equals(nodeId)){
                result = Double.parseDouble(temp.get("attendance").toString());
                break;
            }
        }

        return result;
    }

    public Boolean checkIfNodeExist(String nodeId, JSONArray attendanceList){
        Boolean result = false;
        for(int i=0; i<attendanceList.size(); i++){
            JSONObject temp = (JSONObject) attendanceList.get(i);
            if(temp.get("firstNode").equals(nodeId) || temp.get("secondNode").equals(nodeId)){
                result = true;
                break;
            }
        }

        return result;
    }

    public Boolean checkIfEdgeExist(JSONArray list, String adjacent1, String adjacent2, Double attendance){
        Boolean result = false;
        Integer i;

        for(i=0; i<list.size(); i++){
            if(((JSONObject) list.get(i)).get("adjacent1").toString().equals(adjacent1) ||
                    ((JSONObject) list.get(i)).get("adjacent1").toString().equals(adjacent2) ||
                    ((JSONObject) list.get(i)).get("adjacent2").toString().equals(adjacent1) ||
                    ((JSONObject) list.get(i)).get("adjacent2").toString().equals(adjacent2)){
                result = true;
                break;
            }
        }

        return result;
    }

    public Boolean edgeExist(JSONArray edgeList, String adjacent1, String adjacent2){
        Boolean result = true;

        for(int i=0; i<edgeList.size(); i++){
            JSONObject tempObject = (JSONObject) edgeList.get(i);
            if(tempObject.get("adjacent1").toString().equals(adjacent1) || tempObject.get("adjacent2").toString().equals(adjacent2)){
                result = false;
            }
        }

        return false;
    }

    public Boolean isNodePairAdded(String node1, String adjacent1, String node2, String adjacent2, Double attendance, JSONArray list){
        Boolean result = false;
        for(Object object : list){
            JSONObject tempObject = (JSONObject) object;
            if(tempObject.get("bestMatchingNode1Source").toString().equals(node1) && tempObject.get("bestMatchingNode1Destination").toString().equals(adjacent1)
                    && tempObject.get("bestMatchingNode2Source").toString().equals(node2) && tempObject.get("bestMatchingNode2Destination").toString().equals(adjacent2)) {
                if(attendance < Double.parseDouble(tempObject.get("attendance").toString())){
                    result = true;
                }
            }
        }

        return result;
    }

    public Double determineThreshold(Integer nodePropCount, Integer edgePropCount){
        Double result;
        result = nodePropCount.doubleValue() * nodePropertyWeight + nodePropCount.doubleValue() * neighbourNodePropertyWeight
                + edgePropCount.doubleValue() * edgePropertyWeight;

        return result;
    }

    public Double getAttendance(String x, String y){
        Double result = -1.0;
        Integer i;

        for(i=0; i<attendanceList.size(); i++){
            JSONObject nodePair = (JSONObject) attendanceList.get(i);

            if(nodePair.get("firstNode").toString().equals(x) && nodePair.get("secondNode").toString().equals(y)){
                result = (Double) nodePair.get("attendance");
                break;
            }
        }

        return result;
    }

    public String findNodeProperty(String nodeID, String property, Boolean isFirst){
        String    result;
        JSONArray tmp;
        Integer   i;
        if(isFirst){
            tmp = firstGraphsNodes;
        }else {
            tmp = secondGraphsNodes;
        }

        result = null;
        for(i=1; i<tmp.size(); i++){
            if(((JSONObject) tmp.get(i)).get("nodeID").toString().equals(nodeID)){
                if(((JSONObject) tmp.get(i)).get(property) != null){
                    result = ((JSONObject) tmp.get(i)).get(property).toString();
                    break;
                }else {
                    return null;
                }
            }
        }

        return result;
    }

    public String findEdgeProperty(String x1, String x2, String property, Boolean isFirst){
        Integer   i;
        String    result;
        JSONArray tmp;

        if(isFirst){
            tmp = firstGraphsEdges;
        }else {
            tmp = secondGraphsEdges;
        }

        result = null;
        for(i=1; i<tmp.size(); i++){
            if(((JSONObject)tmp.get(i)).get("node2ID").toString().equals(x1) &&
                    ((JSONObject) tmp.get(i)).get("node1ID").toString().equals(x2)){
                if(((JSONObject) tmp.get(i)).get(property) != null){
                    result = ((JSONObject) tmp.get(i)).get(property).toString();
                    break;
                }else {
                    return null;
                }
            }
        }

        return result;
    }

    // isEdge: true means edge, false means node propertylist;
    public ArrayList<String> getPropertyList(Boolean isEdge){
        Integer i;
        ArrayList<String> result = new ArrayList<>();
        Set<Map.Entry> headers;
        Set<Map.Entry> headers2;
        if(isEdge){
            headers  = ((JSONObject)firstGraphsEdges.get(0)).entrySet();
            headers2 = ((JSONObject)secondGraphsEdges.get(0)).entrySet();
        }else{
            headers  = ((JSONObject)firstGraphsNodes.get(0)).entrySet();
            headers2 = ((JSONObject)secondGraphsNodes.get(0)).entrySet();
        }

        ArrayList<String> propertyList = new ArrayList<>();
        propertyList.add("nodeID");
        propertyList.add("nodeName");
        propertyList.add("name");
        propertyList.add("shared name");
        propertyList.add("shared interaction");
        propertyList.add("edgeID");
        propertyList.add("node1ID");
        propertyList.add("node2ID");

        Iterator iterator = headers.iterator();
        while (iterator.hasNext()){
            Object object = iterator.next();
            if(!propertyList.contains(object.toString().substring(8))){
                result.add(object.toString().substring(8));
            }
        }

        iterator = headers2.iterator();
        while (iterator.hasNext()){
            Object object = iterator.next();
            if (!propertyList.contains(object.toString().substring(8)) && !result.contains(object.toString().substring(8))){
                result.add(object.toString().substring(8));
            }
        }

        return result;
    }

    public ArrayList<String> findConnectNodes(String nodeID, Boolean isFirstGraph){
        ArrayList<String> result = new ArrayList<>();
        Integer i;
        JSONArray edgeList;

        if(isFirstGraph){
            edgeList = firstGraphsEdges;
        }else{
            edgeList = secondGraphsEdges;
        }

        for(i=1; i<edgeList.size(); i++){
            if(((JSONObject) edgeList.get(i)).get("node2ID").toString().equals(nodeID)){
                result.add(((JSONObject) edgeList.get(i)).get("node1ID").toString());
            }else if(((JSONObject) edgeList.get(i)).get("node1ID").toString().equals(nodeID)){
                result.add(((JSONObject) edgeList.get(i)).get("node2ID").toString());
            }
        }

        return result;
    }

    public ArrayList<String> findIncidentNodes(String nodeID, Boolean isFirstGraph){
        ArrayList<String> result = new ArrayList<>();
        Integer i;
        JSONArray edgeList;

        if(isFirstGraph){
            edgeList = firstGraphsEdges;
        }else{
            edgeList = secondGraphsEdges;
        }
        for(i=1; i<edgeList.size(); i++){
            if(((JSONObject) edgeList.get(i)).get("node2ID").toString().equals(nodeID)){
                result.add(((JSONObject) edgeList.get(i)).get("node1ID").toString());
            }
        }

        return result;
    }

    public ArrayList<String> findNeighborNodes(String nodeID, Boolean isFirstGraph){
        ArrayList<String> result = new ArrayList<>();
        Integer i;
        JSONArray edgeList;

        if(isFirstGraph){
            edgeList = firstGraphsEdges;
        }else{
            edgeList = secondGraphsEdges;
        }
        for(i=1; i<edgeList.size(); i++){
            if(((JSONObject) edgeList.get(i)).get("node2ID").toString().equals(nodeID)){
                result.add(((JSONObject) edgeList.get(i)).get("node1ID").toString());
            }

            if(((JSONObject) edgeList.get(i)).get("node1ID").toString().equals(nodeID)){
                result.add(((JSONObject) edgeList.get(i)).get("node2ID").toString());
            }
        }

        return result;
    }

    public ArrayList getVector(Integer rootID, Integer graphPointer){
        Integer levelPointer;
        Integer nodeCount;
        Integer i;
        ArrayList   vectorList = new ArrayList();
        ArrayList   queue      = new ArrayList();

        levelPointer = 1;
        nodeCount    = 1;
        queue.add(rootID);
        while (!queue.isEmpty()){
            ArrayList tmp = new ArrayList();
            tmp.add(new ArrayList());
            ArrayList<Integer> neighbours = new ArrayList<>();
            for(i=0; i<nodeCount; i++){
                neighbours.addAll(findNeighbours((Integer) (queue.get(0)), graphPointer));
                ((ArrayList)tmp.get(0)).add(queue.get(0));
                queue.remove(queue.get(0));
            }
            queue.addAll(neighbours);

            tmp.add(levelPointer);
            tmp.add(nodeCount);
            tmp.add(((Integer)neighbours.size()).doubleValue() / ((Integer)nodeCount).doubleValue());
            vectorList.add(tmp);

            levelPointer++;
            nodeCount = neighbours.size();
        }

        return vectorList;
    }

    public ArrayList<Integer> findNeighbours(Integer id, Integer graphPointer){
        ArrayList<Integer> result = new ArrayList();
        Integer i;

        if(graphPointer == 1){
            for(i=1; i<firstGraphsEdges.size(); i++){
                if(Integer.valueOf(((JSONObject)(firstGraphsEdges.get(i))).get("node1ID").toString()) == id){
                    result.add(Integer.valueOf((String)((JSONObject)(firstGraphsEdges.get(i))).get("node2ID")));
                }
            }
        }else if(graphPointer == 2){
            for(i=1; i<secondGraphsEdges.size(); i++){
                if(Integer.valueOf(((JSONObject)(secondGraphsEdges.get(i))).get("node1ID").toString()) == id){
                    result.add(Integer.valueOf((String)((JSONObject)(secondGraphsEdges.get(i))).get("node2ID")));
                }
            }
        }

        return result;
    }

    public Integer findGraphRoot(Integer graphPointer){
        Integer rootID = -1;
        Integer i;
        Integer j;
        ArrayList<String> nodeIdList = new ArrayList<>();
        ArrayList<String> childsList = new ArrayList<>();

        if(graphPointer == 1){
            for(i=1; i<firstGraphsNodes.size(); i++){
                nodeIdList.add(((JSONObject) (firstGraphsNodes.get(i))).get("nodeID").toString());
            }

            for(i=1; i<firstGraphsEdges.size(); i++){
                childsList.add(((JSONObject) (firstGraphsEdges.get(i))).get("node2ID").toString());
            }

            firstGraphNodeIdList = nodeIdList;
        }else if(graphPointer == 2){
            for(i=1; i<secondGraphsNodes.size(); i++){
                nodeIdList.add(((JSONObject) (secondGraphsNodes.get(i))).get("nodeID").toString());
            }

            for(i=1; i<secondGraphsEdges.size(); i++){
                childsList.add(((JSONObject) (secondGraphsEdges.get(i))).get("node2ID").toString());
            }

            secondGraphNodeIdList = nodeIdList;
        }

        ArrayList tempList = new ArrayList();
        for(i=1; i<nodeIdList.size(); i++){
            if(childsList.contains(nodeIdList.get(i))){
                for(j=1; j<nodeIdList.size(); j++){
                    if(nodeIdList.get(j).equals(nodeIdList.get(i))){
                        tempList.add(nodeIdList.get(j));
                    }
                }
            }
        }

        for(i=0; i<tempList.size(); i++){
            if(nodeIdList.contains(tempList.get(i))){
                nodeIdList.remove(tempList.get(i));
            }
        }

        if(nodeIdList.size() == 1){
            rootID = Integer.valueOf((String) nodeIdList.get(0));
        }

        return rootID;
    }

    public void readGraphs() {

        // Read all files
        readFile(node1Path,1);
        readFile(edge1Path,2);
        readFile(node2Path,3);
        readFile(edge2Path,4);

    }

    public void readFile(String filePath, Integer filePointer){

        String line = "";
        String csvSplitBy = ",";
        Integer i;
        JSONObject headers;
        JSONObject  row;

        try {
            BufferedReader br = new BufferedReader(new FileReader(filePath));
            headers = new JSONObject();

            if((line = br.readLine()) != null){
                String[] headersString = line.split(csvSplitBy);
                for(i=0; i< headersString.length; i++){
                    headers.put("Header" + (i+1), headersString[i]);
                }

                addToJSONArrays(headers, filePointer);
            }

            while ((line = br.readLine()) != null) {
                String[] data = line.split(csvSplitBy);
                row = new JSONObject();
                for(i=0; i<data.length; i++){
                    row.put(headers.get("Header" + (i+1)), data[i]);
                }

                addToJSONArrays(row, filePointer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addToJSONArrays(JSONObject data, Integer filePointer){
        if(filePointer == 1){
            this.firstGraphsNodes.add(data);
        }else if(filePointer == 2){
            this.firstGraphsEdges.add(data);
        }else if(filePointer == 3){
            this.secondGraphsNodes.add(data);
        }else if(filePointer == 4){
            this.secondGraphsEdges.add(data);
        }
    }

    public void printAll(){
        JOptionPane.showMessageDialog(adapter.getCySwingApplication().getJFrame(),
                firstGraphsEdges.toString() + "\n" + firstGraphsNodes + "\n" + secondGraphsEdges + "\n" + secondGraphsNodes,
                "Error!", JOptionPane.INFORMATION_MESSAGE);
    }

    public void changeFile(Integer algorithm, Double nodeWeight, Double edgeWeight, Double neighbourNodeWeight, Double threshold, Double minThreshold){
        try{
            setAlgorithm(algorithm);
            setNodePropertyWeight(nodeWeight);
            setEdgePropertyWeight(edgeWeight);
            setNeighbourNodePropertyWeight(neighbourNodeWeight);
            setTHRESHOLD(threshold);
            setMinThreshold(minThreshold);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Integer getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(Integer algorithm) {
        this.algorithm = algorithm;
    }

    public CySwingAppAdapter getAdapter() {
        return this.adapter;
    }

    public void setAdapter(CySwingAppAdapter adapter) {
        this.adapter = adapter;
    }

    public String getNode1Path() {
        return this.node1Path;
    }

    public void setNode1Path(String node1Path) {
        this.node1Path = node1Path;
    }

    public String getNode2Path() {
        return this.node2Path;
    }

    public void setNode2Path(String node2Path) {
        this.node2Path = node2Path;
    }

    public String getEdge1Path() {
        return this.edge1Path;
    }

    public void setEdge1Path(String edge1Path) {
        this.edge1Path = edge1Path;
    }

    public String getEdge2Path() {
        return this.edge2Path;
    }

    public void setEdge2Path(String edge2Path) {
        this.edge2Path = edge2Path;
    }

    public String getNode1FileName() {
        return this.node1FileName;
    }

    public void setNode1FileName(String node1FileName) {
        this.node1FileName = node1FileName;
    }

    public String getNode2FileName() {
        return this.node2FileName;
    }

    public void setNode2FileName(String node2FileName) {
        this.node2FileName = node2FileName;
    }

    public String getEdge1FileName() {
        return this.edge1FileName;
    }

    public void setEdge1FileName(String edge1FileName) {
        this.edge1FileName = edge1FileName;
    }

    public String getEdge2FileName() {
        return this.edge2FileName;
    }

    public void setEdge2FileName(String edge2FileName) {
        this.edge2FileName = edge2FileName;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public ArrayList getMatchedParts() {
        return matchedParts;
    }

    public void setMatchedParts(ArrayList matchedParts) {
        this.matchedParts = matchedParts;
    }

    public JSONArray getAttendanceList() {
        return attendanceList;
    }

    public void setAttendanceList(JSONArray attendanceList) {
        this.attendanceList = attendanceList;
    }

    public JSONArray getFirstGraphsNodes() {
        return firstGraphsNodes;
    }

    public void setFirstGraphsNodes(JSONArray firstGraphsNodes) {
        this.firstGraphsNodes = firstGraphsNodes;
    }

    public JSONArray getFirstGraphsEdges() {
        return firstGraphsEdges;
    }

    public void setFirstGraphsEdges(JSONArray firstGraphsEdges) {
        this.firstGraphsEdges = firstGraphsEdges;
    }

    public JSONArray getSecondGraphsNodes() {
        return secondGraphsNodes;
    }

    public void setSecondGraphsNodes(JSONArray secondGraphsNodes) {
        this.secondGraphsNodes = secondGraphsNodes;
    }

    public JSONArray getSecondGraphsEdges() {
        return secondGraphsEdges;
    }

    public void setSecondGraphsEdges(JSONArray secondGraphsEdges) {
        this.secondGraphsEdges = secondGraphsEdges;
    }

    public ArrayList getSimilarNodePairs() {
        return similarNodePairs;
    }

    public void setSimilarNodePairs(ArrayList similarNodePairs) {
        this.similarNodePairs = similarNodePairs;
    }

    public Double getNodePropertyWeight() {
        return nodePropertyWeight;
    }

    public void setNodePropertyWeight(Double nodePropertyWeight) {
        this.nodePropertyWeight = nodePropertyWeight;
    }

    public Double getEdgePropertyWeight() {
        return edgePropertyWeight;
    }

    public void setEdgePropertyWeight(Double edgePropertyWeight) {
        this.edgePropertyWeight = edgePropertyWeight;
    }

    public Double getNeighbourNodePropertyWeight() {
        return neighbourNodePropertyWeight;
    }

    public void setNeighbourNodePropertyWeight(Double neighbourNodePropertyWeight) {
        this.neighbourNodePropertyWeight = neighbourNodePropertyWeight;
    }

    public Double getTHRESHOLD() {
        return THRESHOLD;
    }

    public void setTHRESHOLD(Double THRESHOLD) {
        this.THRESHOLD = THRESHOLD;
    }

    public Double getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(Double minThreshold) {
        this.minThreshold = minThreshold;
    }

    public Boolean getIgnorDifferentNodeTypes() {
        return ignorDifferentNodeTypes;
    }

    public void setIgnorDifferentNodeTypes(Boolean ignorDifferentNodeTypes) {
        this.ignorDifferentNodeTypes = ignorDifferentNodeTypes;
    }
}
