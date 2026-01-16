package pl.edu.agh.kt.web;

import java.util.HashMap;
import java.util.Map;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

import pl.edu.agh.kt.NetworkMonitor;

public class NetworkMonitorMetricsResource extends ServerResource {
    
    private static NetworkMonitor networkMonitorInstance = null;
    
    public static void setNetworkMonitorInstance(NetworkMonitor instance) {
        networkMonitorInstance = instance;
    }
    
    @Get("json")
    public Map<String, Object> retrieve() {
        Map<String, Object> result = new HashMap<String, Object>();
        
        if (networkMonitorInstance != null) {
            result.put("currentBandwidthMbps", networkMonitorInstance.getCurrentBandwidth());
            result.put("currentState", networkMonitorInstance.getCurrentState());
            result.put("pollingIntervalMs", networkMonitorInstance.getCurrentPollingInterval());
            
            Map<String, String> switches = new HashMap<String, String>();
            switches.put("a1", networkMonitorInstance.getDpidA1());
            switches.put("a2", networkMonitorInstance.getDpidA2());
            result.put("monitoredSwitches", switches);
            
            Map<String, Double> thresholds = new HashMap<String, Double>();
            thresholds.put("lowTrafficMbps", networkMonitorInstance.getThresholdLowTraffic());
            thresholds.put("highTrafficMbps", networkMonitorInstance.getThresholdHighTraffic());
            result.put("thresholds", thresholds);
            
            result.put("analysisWindowMs", networkMonitorInstance.getAnalysisWindow());
        } else {
            result.put("error", "NetworkMonitor service not available");
        }
        
        return result;
    }
}
