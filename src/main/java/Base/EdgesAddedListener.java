package Base;

import App.CytoVisProject;
import Util.BackwardDependency;
import Util.EnhancedVersionOfBDM;
import Util.FilterUtil;
import org.cytoscape.app.swing.CySwingAppAdapter;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.AddedEdgesEvent;
import org.cytoscape.model.events.AddedEdgesListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class EdgesAddedListener implements AddedEdgesListener {

    private CytoVisProject          cytoVisProject;
    private EnhancedVersionOfBDM    enhancedVersionOfBDM;
    private BackwardDependency      backwardDependency;
    private CySwingAppAdapter       adapter;

    public EdgesAddedListener(CytoVisProject cytoVisProject){
        this.cytoVisProject     = cytoVisProject;
        this.enhancedVersionOfBDM = cytoVisProject.getMyControlPanel().getEnhancedVersionOfBDM();
        this.adapter            = cytoVisProject.getMyControlPanel().getAdapter();
        this.backwardDependency = cytoVisProject.getMyControlPanel().getBackwardDependency();
    }

    @Override
    public void handleEvent(AddedEdgesEvent addedEdgesEvent) {
        String sourceNodeId         = new String();
        String destNodeId           = new String();
        CyTable currentEdgeTable    = adapter.getCyApplicationManager().getCurrentNetwork().getDefaultEdgeTable();

        long startTime = new Date().getTime();

        for (CyEdge edge : addedEdgesEvent.getPayloadCollection()){
            enhancedVersionOfBDM.updateState(currentEdgeTable.getRow(edge.getSUID()).get("Source", String.class), currentEdgeTable.getRow(edge.getSUID()).get("Destination", String.class));
        }

        // If there is a backward dependency applied than show only related nodes
        if(enhancedVersionOfBDM.getDoesFilterApplied()){
            ArrayList<String> nodesToBeShownOnly = new ArrayList<>();
            // Get nodes to be shown only
            for(String nodeId : enhancedVersionOfBDM.getSelectedNodeIdList()){
                nodesToBeShownOnly.addAll(enhancedVersionOfBDM.getBackwardProvenance(nodeId, enhancedVersionOfBDM.getStateCurrent(), new ArrayList<>()));
            }

            nodesToBeShownOnly.addAll(enhancedVersionOfBDM.getSelectedNodeIdList());
            cytoVisProject.getMyControlPanel().getNetworkViewOrganizer().showOnly(nodesToBeShownOnly,
                    new FilterUtil(adapter.getCyApplicationManager().getCurrentNetwork(), adapter.getCyApplicationManager().getCurrentTable()));
        }

        adapter.getCyApplicationManager().getCurrentNetworkView().updateView();
        System.out.println("[" + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format(new Date()) + "] Total time to run BDM: "
                + (new Date().getTime() - startTime));
    }
}
