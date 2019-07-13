package Util;

import java.util.*;
import java.lang.String;

import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.model.*;
import org.json.simple.JSONObject;

// This class is for providing some usefull methods for the rest of the program
public class FilterUtil {
    // Variables
    private List<CyRow> rows;
    private CyNetwork network;
    private CyTable table;

    public FilterUtil(CyNetwork network, CyTable table){
        // Initializations
        this.network = network;
        this.table = table;
    }
    // This method returns all the nodes which has "filterString" string in its "ColName" column
    public ArrayList<CyNode> FilterRowByNodeType(String filterString, String ColName){
        ArrayList<CyNode> matchingNodes = new ArrayList<CyNode>();
        this.rows = table.getAllRows(); // getting all rows
        for (int i=0;i<rows.size();i++){
            // If row contains "filterString" then add the node to the matchingNodes list
            if(rows.get(i).get(ColName, String.class) != null){
                if(rows.get(i).get(ColName,String.class).contains(filterString)){
                    long suid = rows.get(i).get(CyIdentifiable.SUID, Long.class);
                    matchingNodes.add(network.getNode(suid));
                }
            }
        }
        return matchingNodes;
    }
    // This method is for getting all nodes in the network
    public ArrayList<CyNode> getAllNodes (){
        ArrayList<CyNode> allNodes = new ArrayList<CyNode>();
        rows=table.getAllRows();
        // Get all rows and transform them to the nodes and add it to allNodes list
        for (int i=0;i<rows.size();i++){
            long suid = rows.get(i).get(CyIdentifiable.SUID, Long.class);
            allNodes.add(network.getNode(suid));
        }
        return allNodes;
    }
    // This method is for getting all nodes in the network with their id's
    public HashMap<String, CyNode> getAllNodesWithId (){
        HashMap<String, CyNode> allNodes = new HashMap<>();
        rows=table.getAllRows();
        // Get all rows and transform them to the nodes and add it to allNodes list
        for (int i=0;i<rows.size();i++){
            long suid = rows.get(i).get(CyIdentifiable.SUID, Long.class);
            allNodes.put(rows.get(i).get("name", String.class), network.getNode(suid));
        }
        return allNodes;
    }

    public Object getValueById(String id, String idColumn, String attributeName){
        for (int i=0;i<rows.size();i++){
            long suid = rows.get(i).get(CyIdentifiable.SUID, Long.class);
            if(rows.get(i).get(idColumn, String.class).equals(id)){
                return rows.get(i).get(attributeName, Object.class);
            }
        }

        return null;
    }
    // This method is for finding node type of a node
    public String findNodeType(CyNode node){
        String nodeType;
        // Gets related row for a node and finds value which is in the "nodeType" column
        nodeType=network.getRow(node).get("nodeType",String.class);
        return nodeType;
    }

    // This method finds all available node types. It is necessary to show all node type in the show only, hide and highlight panels
    public String[] findAvailableNodeTypes(){
        List<String> tmp = new ArrayList<String>();
        String[] nodeTypes = new String[3];
        rows = table.getAllRows(); // Getting all nodes
        // If a row has a different value in its nodeType column then add value to nodeTypes array
        for (CyRow row : rows){
            if(tmp.contains(row.get("nodeType",String.class))==false){
                tmp.add(row.get("nodeType",String.class));
            }
        }
        for(int i=0;i<tmp.size();i++){
            nodeTypes[i+1] = new String();
            nodeTypes[i+1]=tmp.get(i);
        }
        nodeTypes[0]="None";
        return nodeTypes;
    }
    // This method is for finding a nodes neighbors which has the "filterString" node type
    public List<CyNode> findNodeNeighbors(String filterString, CyNode node){
        List<CyNode> neighbors;
        List<CyNode> filteredNeighbors = new ArrayList<CyNode>();
        neighbors = network.getNeighborList(node, CyEdge.Type.ANY); // Getting all neighbors
        // Add node to filteredNeighbors List if its node type is same with "filteredString"
        for(CyNode node1 : neighbors){
            if(findNodeType(node1).compareTo(filterString) == 0){
                filteredNeighbors.add(node1);
            }
        }
        return filteredNeighbors;
    }

    public int findIndex(ArrayList<CyNode> allNodes, CyNode node){
        int counter = 0;
        for(CyNode nodes : allNodes){
            if(node == nodes){
                return counter;
            }
            counter++;
        }
        return 0;
    }

    public List<String> getTimeFromColumn(CyColumn column){
        List<String> list = column.getValues(String.class);
        int i=0;
        while(i<list.size()){
            if(list.get(i) == null){
                list.remove(i);
            }else{
                i++;
            }
        }
        return list;
    }

    public CyNode getNode(String nodeID, CySwingAppAdapter adapter, String idParameter){
        CyNode result = null;

        ArrayList<CyNode> nodes = getAllNodes();
        for(CyNode node : nodes){
            if(adapter.getCyApplicationManager().getCurrentNetwork().getRow(node).get(idParameter, String.class)
                    .equals(nodeID)){
                result = node;
                break;
            }
        }

        return result;
    }

    public String getNodeId(CyNode node, CySwingAppAdapter adapter, String nodeIDString){
        String nodeId           = new String();
        List<CyNode> nodes = adapter.getCyApplicationManager().getCurrentNetwork().getNodeList();

        for(CyNode tempNode : nodes){
            if(tempNode.getSUID() == node.getSUID()){
                nodeId = adapter.getCyApplicationManager().getCurrentNetwork().getRow(tempNode).get(nodeIDString, String.class);
                break;
            }
        }

        return nodeId;
    }

    // This method gets Node ID list of selected nodes
    public ArrayList<String> getSelectedNodeIdList(CySwingAppAdapter adapter, String nodeIdColumnName){
        ArrayList<String> selectedNodeIdList = new ArrayList<>();

        List<CyRow> allRows = adapter.getCyApplicationManager().getCurrentNetwork().getDefaultNodeTable().getAllRows();
        for(CyRow row : allRows){
            if(row.get("selected", Boolean.class)){
                selectedNodeIdList.add(row.get(nodeIdColumnName, String.class));
            }
        }

        return selectedNodeIdList;
    }

    public void deSelectAllNodes(CySwingAppAdapter adapter){
        List<CyRow> allRows = adapter.getCyApplicationManager().getCurrentNetwork().getDefaultNodeTable().getAllRows();
        for(CyRow row : allRows){
            row.set("selected", false);
        }
    }

    // Get neighbours of the nodes that id list's is given in the arraylist
    public ArrayList<CyNode> getNeighbourList(CySwingAppAdapter adapter, ArrayList<String> nodesToFindNeighbours){
        ArrayList<CyNode> neighbours = new ArrayList<>();
        CyNode temp;

        for(String nodeId : nodesToFindNeighbours){
            temp = getNode(nodeId, adapter, "Node ID");
            neighbours.addAll(adapter.getCyApplicationManager().getCurrentNetwork().getNeighborList(temp, CyEdge.Type.ANY));
        }

        return neighbours;
    }

}