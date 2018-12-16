package Util;

import com.opencsv.CSVReader;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

public class BackwardDependency {

    private ArrayList<ArrayList<Integer>> stateCurrent;
    private ArrayList<ArrayList<Integer>> statePurge;
    private ArrayList<String> rowsCurrent;
    private ArrayList<String> columnsCurrent;
    private ArrayList<String> rowsPurge;
    private ArrayList<String> columnsPurge;
    private HashMap<String, String> varIdToNodeIdCurrent;
    private HashMap<String, String> varIdToNodeIdPurge;
    private Integer uniqueNodeId;

    public BackwardDependency (){
        stateCurrent            = new ArrayList<>();
        statePurge              = new ArrayList<>();
        varIdToNodeIdCurrent    = new HashMap<>();
        varIdToNodeIdPurge      = new HashMap<>();
        rowsCurrent             = new ArrayList<>();
        columnsCurrent          = new ArrayList<>();
        rowsPurge               = new ArrayList<>();
        columnsPurge            = new ArrayList<>();
        uniqueNodeId            = new Integer(0);
    }

    public void printMatrix(ArrayList<ArrayList<Integer>> matrix){
        for(ArrayList<Integer> row : matrix){
            for(Integer cell : row){
                System.out.print(cell + " ");
            }
            System.out.print("\n");
        }

        System.out.println("--------------------------------------------------------------------------------------------------------------------");
    }

    // @param element: A prov-o notification (an edge)
    // @param node1Id: Node Id of source node
    // @param node2Id: Node Id of dest node

    public void updateState(String source, String destination){
        String sourceNode;
        String destNode;
        ArrayList<String> inputVars;
        String node1Id;
        String node2Id;

        if(varIdToNodeIdCurrent.containsKey(source)){
            node1Id = varIdToNodeIdCurrent.get(source);
        }else {
            node1Id = (uniqueNodeId++).toString();
        }

        if(varIdToNodeIdCurrent.containsKey(destination)){
            node2Id = varIdToNodeIdCurrent.get(destination);
        }else {
            node2Id = (uniqueNodeId++).toString();
        }

        // get source and destination nodes
        sourceNode   = source;
        destNode     = destination;

        /*System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] Backward Dependency Vol1\n" + "Initial State:");
        System.out.println("Var Id to Node Id List: " + varIdToNodeIdCurrent.keySet().toString() + varIdToNodeIdCurrent.values().toArray().toString() + "\n" +
                "Node1ID: " + node1Id + "\nNode2ID: " + node2Id + "\n\n");
        System.out.println("Current Rows: " + rowsCurrent.toString());
        System.out.println("Current Columns: " + columnsCurrent.toString());

        System.out.println("Purge Rows: " + rowsPurge.toString());
        System.out.println("Purge Columns: " + columnsPurge.toString());

        System.out.println("Current Matrix:");
        printMatrix(stateCurrent);

        System.out.println("Purge Matrix:");
        printMatrix(statePurge);

        System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] New edge --> Source: " + sourceNode + " ->> Destination: " + destNode);*/
        // if incoming source node is a new value of a variable then cache current state and remove source node from current stat
        if(varIdToNodeIdCurrent.containsKey(sourceNode) && varIdToNodeIdCurrent.get(sourceNode) != node1Id){
            for(int i=0; i<rowsCurrent.size(); i++){
                if(rowsCurrent.get(i).equals(sourceNode)){
                    cacheDependencies(i);

                    rowsCurrent.remove(sourceNode);
                    stateCurrent.remove(i);

                    break;
                }
            }
        }

        // add new mapping sourceNode --> node1Id
        varIdToNodeIdCurrent.put(sourceNode, node1Id);

        // If source node is not in the matrix, add it to the matrix and also to rows list
        if(!rowsCurrent.contains(sourceNode)){
            rowsCurrent.add(sourceNode);
            stateCurrent.add(new ArrayList<Integer>());

            for(String temp : columnsCurrent){
                stateCurrent.get(stateCurrent.size()-1).add(new Integer(0));
            }
        }

        // If destination node is not in the matrix, add it to the matrix and also to columns list
        // After that, add new dependency sourceNode --> destNode
        if(!columnsCurrent.contains(destNode)){
            columnsCurrent.add(destNode);
            for(ArrayList<Integer> columns : stateCurrent){
                columns.add(new Integer(0));
            }
        }

        stateCurrent.get(rowsCurrent.indexOf(sourceNode)).set(columnsCurrent.indexOf(destNode), 1);

        // If source node equals to destNode then get backward provenance from state.purge, else get it from state.current
        if(sourceNode.equals(destNode)){
            inputVars = getBackwardProvenance(destNode, statePurge, rowsPurge, columnsPurge, new ArrayList<String>());
        }else {
            inputVars = getBackwardProvenance(destNode, stateCurrent, rowsCurrent, columnsCurrent, new ArrayList<String>());
        }

        // Update backward provenance of source node in state.current
        for(String var : inputVars){
            stateCurrent.get(rowsCurrent.indexOf(sourceNode)).set(columnsCurrent.indexOf(var), 1);
        }

        /*System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "]\n" + "Last State:");
        System.out.println("Current Rows: " + rowsCurrent.toString());
        System.out.println("Current Columns: " + columnsCurrent.toString());

        System.out.println("Purge Rows: " + rowsPurge.toString());
        System.out.println("Purge Columns: " + columnsPurge.toString());

        System.out.println("Current Matrix:");
        printMatrix(stateCurrent);

        System.out.println("Purge Matrix:");
        printMatrix(statePurge);*/
    }

    // Bütün bir satır çekilemez mi??
    public ArrayList<String> getBackwardProvenance(String varId, ArrayList<ArrayList<Integer>> matrix, ArrayList<String> sourceList, ArrayList<String> columns, ArrayList<String> resultList){

        Integer varIndex            = sourceList.indexOf(varId);

        if(varIndex < 0){
            return resultList;
        }

        for(int i=0; i<columns.size(); i++){
            if(matrix.get(varIndex).get(i) == 1 && !resultList.contains(columns.get(i))){
                resultList.add(columns.get(i));
                resultList = getBackwardProvenance(columns.get(i), matrix, sourceList, columns, resultList);
            }
        }

        return resultList;
    }

    // Cache dependencies that removed from state.current
    public void cacheDependencies(Integer rowToCash){
        if(!rowsPurge.contains(rowsCurrent.get(rowToCash))){
            rowsPurge.add(rowsCurrent.get(rowToCash));
        }else {
            statePurge.remove(rowsPurge.indexOf(rowsCurrent.get(rowToCash)));
        }

        statePurge.add(stateCurrent.get(rowToCash));
        columnsPurge    = columnsCurrent;
    }

    public ArrayList<String> getRowsCurrent() {
        return rowsCurrent;
    }

    public void setRowsCurrent(ArrayList<String> rowsCurrent) {
        this.rowsCurrent = rowsCurrent;
    }

    public ArrayList<String> getColumnsCurrent() {
        return columnsCurrent;
    }

    public void setColumnsCurrent(ArrayList<String> columnsCurrent) {
        this.columnsCurrent = columnsCurrent;
    }

    public ArrayList<String> getRowsPurge() {
        return rowsPurge;
    }

    public void setRowsPurge(ArrayList<String> rowsPurge) {
        this.rowsPurge = rowsPurge;
    }

    public ArrayList<String> getColumnsPurge() {
        return columnsPurge;
    }

    public void setColumnsPurge(ArrayList<String> columnsPurge) {
        this.columnsPurge = columnsPurge;
    }

    public HashMap<String, String> getVarIdToNodeIdCurrent() {
        return varIdToNodeIdCurrent;
    }

    public void setVarIdToNodeIdCurrent(HashMap<String, String> varIdToNodeIdCurrent) {
        this.varIdToNodeIdCurrent = varIdToNodeIdCurrent;
    }

    public HashMap<String, String> getVarIdToNodeIdPurge() {
        return varIdToNodeIdPurge;
    }

    public void setVarIdToNodeIdPurge(HashMap<String, String> varIdToNodeIdPurge) {
        this.varIdToNodeIdPurge = varIdToNodeIdPurge;
    }

    public ArrayList<ArrayList<Integer>> getStateCurrent() {
        return stateCurrent;
    }

    public void setStateCurrent(ArrayList<ArrayList<Integer>> stateCurrent) {
        this.stateCurrent = stateCurrent;
    }

    public ArrayList<ArrayList<Integer>> getStatePurge() {
        return statePurge;
    }

    public void setStatePurge(ArrayList<ArrayList<Integer>> statePurge) {
        this.statePurge = statePurge;
    }
}