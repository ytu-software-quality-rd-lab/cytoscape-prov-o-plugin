package Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

public class EnhancedVersionOfBDM {

    private HashMap<String, ArrayList<String>>  stateCurrent;
    private HashMap<String, ArrayList<String>>  statePurge;
    private HashMap<String, String>             varIdToNodeIdCurrent;
    private HashMap<String, String>             varIdToNodeIdPurge;
    private Integer                             uniqueNodeId;
    private Boolean                             doesFilterApplied;
    private ArrayList<String>                   filterNode;
    private ArrayList<String>                   selectedNodeIdList;

    public EnhancedVersionOfBDM(){
        stateCurrent            = new HashMap<>();
        statePurge              = new HashMap<>();
        varIdToNodeIdCurrent    = new HashMap<>();
        varIdToNodeIdPurge      = new HashMap<>();
        uniqueNodeId            = new Integer(0);
        doesFilterApplied       = false;
        filterNode              = new ArrayList<>();
    }

    public void printMatrix(HashMap<String, ArrayList<String>> matrix){
        for(String row : matrix.keySet()){
            for(String cell : matrix.get(row)){
                System.out.print(cell + " ");
            }
            System.out.print("\n");
        }

        System.out.println("--------------------------------------------------------------------------------------------------------------------");
    }

    // @param element: A prov-o notification (an edge)
    // @param node1Id: Node Id of source node
    // @param node2Id: Node Id of dest node

    public void updateState(String sourceNode, String destNode){
        ArrayList<String> inputVars;
        String node1Id;
        String node2Id;

        if(varIdToNodeIdCurrent.containsKey(sourceNode)){
            node1Id = varIdToNodeIdCurrent.get(sourceNode);
        }else {
            node1Id = (uniqueNodeId++).toString();
        }

        if(varIdToNodeIdCurrent.containsKey(destNode)){
            node2Id = varIdToNodeIdCurrent.get(destNode);
        }else {
            node2Id = (uniqueNodeId++).toString();
        }

        /*System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] Backward Dependency Vol2\n" + "Initial State:");

        System.out.println("Current Matrix's Rows: " + stateCurrent.keySet().toString());
        System.out.println("Current Matrix:");
        printMatrix(stateCurrent);

        System.out.println("Purge Matrix's Rows: " + statePurge.keySet().toString());
        System.out.println("Purge Matrix:");
        printMatrix(statePurge);

        System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] New edge --> Source: " + sourceNode + " ->> Destination: " + destNode);*/
        // if incoming source node is a new value of a variable then cache current state and remove source node from current stat
        if(varIdToNodeIdCurrent.containsKey(sourceNode) && varIdToNodeIdCurrent.get(sourceNode) != node1Id){
            Object[] keyList = stateCurrent.keySet().toArray();
            for(int i=0; i<stateCurrent.size(); i++){
                if(keyList[i].toString().equals(sourceNode)){
                    cacheDependencies(keyList[i].toString());
                    stateCurrent.remove(keyList[i]);
                    break;
                }
            }
        }

        // add new mapping sourceNode --> node1Id
        varIdToNodeIdCurrent.put(sourceNode, node1Id);

        // If source node is not in the matrix, add it to the matrix and also to rows list
        if(!stateCurrent.keySet().contains(sourceNode)){
            stateCurrent.put(sourceNode, new ArrayList<String>() {{ add(destNode); }});
        }else {
            stateCurrent.get(sourceNode).add(destNode);
        }

        // If source node equals to destNode then get backward provenance from state.purge, else get it from state.current
        if(sourceNode.equals(destNode)){
            inputVars = getBackwardProvenance(destNode, statePurge, new ArrayList<>());
        }else {
            inputVars = getBackwardProvenance(destNode, stateCurrent, new ArrayList<>());
        }

        // Update backward provenance of source node in state.current
        stateCurrent.get(sourceNode).addAll(inputVars);

        /*System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "]\n" + "Last State:");

        System.out.println("Current Matrix's Rows: " + stateCurrent.keySet().toString());
        System.out.println("Current Matrix:");
        printMatrix(stateCurrent);

        System.out.println("Purge Matrix's Rows: " + statePurge.keySet().toString());
        System.out.println("Purge Matrix:");
        printMatrix(statePurge);*/
    }

    public ArrayList<String> getBackwardProvenance(String varId, HashMap<String, ArrayList<String>> matrix, ArrayList<String> resultList){

        if(!matrix.containsKey(varId)){
            return resultList;
        }else {
            for(String varItem : matrix.get(varId)){
                if(!resultList.contains(varItem)){
                    resultList.add(varItem);
                    resultList = getBackwardProvenance(varItem, matrix, resultList);
                }
            }
        }

        return resultList;
    }

    // Cache dependencies that removed from state.current
    public void cacheDependencies(String rowToCash){
        statePurge.put(rowToCash, stateCurrent.get(rowToCash));
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

    public HashMap<String, ArrayList<String>> getStateCurrent() {
        return stateCurrent;
    }

    public void setStateCurrent(HashMap<String, ArrayList<String>> stateCurrent) {
        this.stateCurrent = stateCurrent;
    }

    public HashMap<String, ArrayList<String>> getStatePurge() {
        return statePurge;
    }

    public void setStatePurge(HashMap<String, ArrayList<String>> statePurge) {
        this.statePurge = statePurge;
    }

    public Boolean getDoesFilterApplied() {
        return doesFilterApplied;
    }

    public void setDoesFilterApplied(Boolean doesFilterApplied) {
        this.doesFilterApplied = doesFilterApplied;
    }

    public ArrayList<String> getFilterNode() {
        return filterNode;
    }

    public void setFilterNode(ArrayList<String> filterNode) {
        this.filterNode = filterNode;
    }

    public ArrayList<String> getSelectedNodeIdList() {
        return selectedNodeIdList;
    }

    public void setSelectedNodeIdList(ArrayList<String> selectedNodeIdList) {
        this.selectedNodeIdList = selectedNodeIdList;
    }
}
