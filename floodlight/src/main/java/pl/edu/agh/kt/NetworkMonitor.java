package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;

import pl.edu.agh.kt.web.NetworkMonitorWebRoutable;


public class NetworkMonitor implements IFloodlightModule, IOFMessageListener {

    protected IFloodlightProviderService floodlightProvider;
    protected IOFSwitchService switchService;
    protected IRestApiService restApiService;
    protected static Logger logger;
    
    private static final long POLLING_INTERVAL_LOW_TRAFFIC = 5000;
    private static final long POLLING_INTERVAL_HIGH_TRAFFIC = 1000;
    private static final double THRESHOLD_LOW_TRAFFIC = 10.0;
    private static final double THRESHOLD_HIGH_TRAFFIC = 20.0;
    private static final long ANALYSIS_WINDOW = 60000;
    
    private static final String DPID_A1 = "00:00:00:00:00:00:00:05";
    private static final String DPID_A2 = "00:00:00:00:00:00:00:06";
    
    private enum NetworkState {
        NORMAL,
        CRITICAL
    }
    
    private NetworkState currentState = NetworkState.NORMAL;
    private long currentPollingInterval = POLLING_INTERVAL_LOW_TRAFFIC;
    private ScheduledExecutorService scheduler;
    private Map<DatapathId, Map<Short, Long>> previousByteCount;
    private Map<DatapathId, Map<Short, Long>> previousTimestamp;
    private List<MeasurementEntry> rawMeasurements;
    private long windowStartTime;
    
    private static class MeasurementEntry {
        long timestamp;
        double bandwidthMbps;
        
        MeasurementEntry(long timestamp, double bandwidthMbps) {
            this.timestamp = timestamp;
            this.bandwidthMbps = bandwidthMbps;
        }
    }
    
    @Override
    public String getName() {
        return NetworkMonitor.class.getSimpleName();
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        return Command.CONTINUE;
    }
    

    private void handlePortStatsReply(IOFSwitch sw, OFPortStatsReply reply) {
        DatapathId dpid = sw.getId();
        long currentTime = System.currentTimeMillis();
        
        List<OFPortStatsEntry> entries = reply.getEntries();
        
        logger.info("DEBUG handlePortStatsReply: Got {} entries from switch {}", entries.size(), dpid);
        
        for (OFPortStatsEntry entry : entries) {
            if (entry.getPortNo().equals(OFPort.LOCAL)) {
                continue;
            }
            
            short portNum = (short) entry.getPortNo().getPortNumber();
            long currentBytes = entry.getTxBytes().getValue() + entry.getRxBytes().getValue();
            
            logger.info("RAW STATS: Switch {} Port {} Bytes: {}", new Object[]{dpid, portNum, currentBytes});
            
            if (!previousByteCount.containsKey(dpid)) {
                previousByteCount.put(dpid, new HashMap<Short, Long>());
                previousTimestamp.put(dpid, new HashMap<Short, Long>());
            }
            
            Map<Short, Long> portByteMap = previousByteCount.get(dpid);
            Map<Short, Long> portTimeMap = previousTimestamp.get(dpid);
            
            if (portByteMap.containsKey(portNum) && portTimeMap.containsKey(portNum)) {
                long previousBytes = portByteMap.get(portNum);
                long previousTime = portTimeMap.get(portNum);
                
                long bytesDelta = currentBytes - previousBytes;
                long timeDelta = currentTime - previousTime;
                
                if (timeDelta > 0) {
                    double bandwidthMbps = (bytesDelta * 8.0) / (timeDelta * 1000.0);
                    
                    if (bandwidthMbps > 0) {
                         logger.info("Switch {} Port {}: {} Mb/s (BytesDelta: {}, TimeDelta: {} ms)", 
                            new Object[] { dpid, portNum, String.format("%.2f", bandwidthMbps), bytesDelta, timeDelta });
                    }

                    synchronized (rawMeasurements) {
                        rawMeasurements.add(new MeasurementEntry(currentTime, bandwidthMbps));
                    }
                }
            }
            
            portByteMap.put(portNum, currentBytes);
            portTimeMap.put(portNum, currentTime);
        }
    }
    

    private void requestPortStatistics() {
        DatapathId dpidA1 = DatapathId.of(DPID_A1);
        DatapathId dpidA2 = DatapathId.of(DPID_A2);
        
        IOFSwitch switchA1 = switchService.getSwitch(dpidA1);
        if (switchA1 != null) {
            requestPortStatsSync(switchA1);
        }
        
        IOFSwitch switchA2 = switchService.getSwitch(dpidA2);
        if (switchA2 != null) {
            requestPortStatsSync(switchA2);
        }
    }
    

    private void requestPortStatsSync(IOFSwitch sw) {
        try {
            String dpid = sw.getId().toString();
            String url = "http://localhost:8080/wm/core/switch/" + dpid + "/port/json";
            

            
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(2000);
            con.setReadTimeout(2000);
            
            int responseCode = con.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                String jsonResponse = response.toString();
                DatapathId dpidObj = sw.getId();
                long currentTime = System.currentTimeMillis();
                

                Pattern portPattern = Pattern.compile("\\{[^}]*\"portNumber\":\"(\\d+)\"[^}]*\"receiveBytes\":\"(\\d+)\"[^}]*\"transmitBytes\":\"(\\d+)\"[^}]*\\}");
                Matcher matcher = portPattern.matcher(jsonResponse);
                
                int portCount = 0;
                while (matcher.find()) {
                    String portNumStr = matcher.group(1);
                    long rxBytes = Long.parseLong(matcher.group(2));
                    long txBytes = Long.parseLong(matcher.group(3));
                    
                    short portNum = Short.parseShort(portNumStr);
                    long currentBytes = rxBytes + txBytes;
                    portCount++;
                    

                    

                    if (!previousByteCount.containsKey(dpidObj)) {
                        previousByteCount.put(dpidObj, new HashMap<Short, Long>());
                        previousTimestamp.put(dpidObj, new HashMap<Short, Long>());
                    }
                    
                    Map<Short, Long> portByteMap = previousByteCount.get(dpidObj);
                    Map<Short, Long> portTimeMap = previousTimestamp.get(dpidObj);
                    

                    if (portByteMap.containsKey(portNum) && portTimeMap.containsKey(portNum)) {
                        long previousBytes = portByteMap.get(portNum);
                        long previousTime = portTimeMap.get(portNum);
                        
                        long bytesDelta = currentBytes - previousBytes;
                        long timeDelta = currentTime - previousTime;
                        
                        if (timeDelta > 0) {
                            double bandwidthMbps = (bytesDelta * 8.0) / (timeDelta * 1000.0);
                            


                            synchronized (rawMeasurements) {
                                rawMeasurements.add(new MeasurementEntry(currentTime, bandwidthMbps));
                            }
                        }
                    }
                    
                    portByteMap.put(portNum, currentBytes);
                    portTimeMap.put(portNum, currentTime);
                }
                

            } else {
                logger.error("REST API returned code {} for switch {}", responseCode, dpid);
            }
        } catch (Exception e) {
            logger.error("Error getting stats via REST from switch {}: {}", sw.getId(), e.getMessage());
        }
    }
    

    
    /**
     * Analyze measurements and update network state
     */
    private void analyzeAndUpdateState() {
        long currentTime = System.currentTimeMillis();
        
        // Always calculate and log current status for visibility
        double currentAverage = calculateWeightedAverage();
        logger.info(
            "PERIODIC UPDATE: Current weighted average bandwidth: " 
            + String.format("%.2f", currentAverage)
            + " Mb/s (State: "
            + currentState
            + ", Interval: "
            + currentPollingInterval
            + " ms)"
        );

        if (currentTime - windowStartTime >= ANALYSIS_WINDOW) {
            synchronized (rawMeasurements) {
                if (!rawMeasurements.isEmpty()) {
                    double weightedAverage = currentAverage;
                    
                    logger.info("Analysis Window Complete - Weighted Average Bandwidth: {} Mb/s, Current State: {}", 
                              String.format("%.2f", weightedAverage), currentState);
                    
                    if (currentState == NetworkState.NORMAL && weightedAverage > THRESHOLD_HIGH_TRAFFIC) {
                        currentState = NetworkState.CRITICAL;
                        currentPollingInterval = POLLING_INTERVAL_HIGH_TRAFFIC;
                        logger.info("*** STATE CHANGE: NORMAL -> CRITICAL (Bandwidth: {} Mb/s > {} Mb/s) ***", 
                                  String.format("%.2f", weightedAverage), THRESHOLD_HIGH_TRAFFIC);
                        logger.info("*** Polling interval changed to {} ms ***", currentPollingInterval);
                        
                        reschedulePolling();
                        
                    } else if (currentState == NetworkState.CRITICAL && weightedAverage < THRESHOLD_LOW_TRAFFIC) {
                        currentState = NetworkState.NORMAL;
                        currentPollingInterval = POLLING_INTERVAL_LOW_TRAFFIC;
                        logger.info("*** STATE CHANGE: CRITICAL -> NORMAL (Bandwidth: {} Mb/s < {} Mb/s) ***", 
                                  String.format("%.2f", weightedAverage), THRESHOLD_LOW_TRAFFIC);
                        logger.info("*** Polling interval changed to {} ms ***", currentPollingInterval);
                        
                        reschedulePolling();
                    }
                    
                    rawMeasurements.clear();
                }
            }
            
            windowStartTime = currentTime;
        }
    }
    
    private double calculateWeightedAverage() {
        if (rawMeasurements.isEmpty()) {
            return 0.0;
        }
        
        long currentTime = System.currentTimeMillis();
        double weightedSum = 0.0;
        double totalWeight = 0.0;
        
        Iterator<MeasurementEntry> iter = rawMeasurements.iterator();
        while (iter.hasNext()) {
            MeasurementEntry entry = iter.next();
            if (currentTime - entry.timestamp > ANALYSIS_WINDOW) {
                iter.remove();
            }
        }
        
        for (MeasurementEntry entry : rawMeasurements) {
            long age = currentTime - entry.timestamp;
            double weight = 1.0 - (age / (double) ANALYSIS_WINDOW);
            
            weightedSum += entry.bandwidthMbps * weight;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0.0;
    }
    
    private void reschedulePolling() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        requestPortStatistics();
                        analyzeAndUpdateState();
                    } catch (Exception e) {
                        logger.error("Error in monitoring task", e);
                    }
                }
            },
            currentPollingInterval,
            currentPollingInterval,
            TimeUnit.MILLISECONDS
        );
        
        logger.info(">>> POLLING INTERVAL CHANGED TO: {} ms <<<", currentPollingInterval);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        Collection<Class<? extends IFloodlightService>> l = 
            new ArrayList<Class<? extends IFloodlightService>>();
        l.add(IFloodlightProviderService.class);
        l.add(IOFSwitchService.class);
        l.add(IRestApiService.class);
        return l;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
        switchService = context.getServiceImpl(IOFSwitchService.class);
        restApiService = context.getServiceImpl(IRestApiService.class);
        logger = LoggerFactory.getLogger(NetworkMonitor.class);
        
        // Initialize data structures
        previousByteCount = new HashMap<DatapathId, Map<Short, Long>>();
        previousTimestamp = new HashMap<DatapathId, Map<Short, Long>>();
        rawMeasurements = new ArrayList<MeasurementEntry>();
        
        windowStartTime = System.currentTimeMillis();
        
        logger.info("NetworkMonitor initialized");
        logger.info("Configuration:");
        logger.info("  - Low traffic polling: {} ms", POLLING_INTERVAL_LOW_TRAFFIC);
        logger.info("  - High traffic polling: {} ms", POLLING_INTERVAL_HIGH_TRAFFIC);
        logger.info("  - Low traffic threshold: {} Mb/s", THRESHOLD_LOW_TRAFFIC);
        logger.info("  - High traffic threshold: {} Mb/s", THRESHOLD_HIGH_TRAFFIC);
        logger.info("  - Analysis window: {} ms", ANALYSIS_WINDOW);
        logger.info("  - Monitoring switches: a1 ({}), a2 ({})", DPID_A1, DPID_A2);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        logger.info("Executing periodic polling task...");
                        requestPortStatistics();
                        analyzeAndUpdateState();
                    } catch (Exception e) {
                        logger.error("Error in monitoring task", e);
                    }
                }
            },
            0,
            currentPollingInterval,
            TimeUnit.MILLISECONDS
        );
        
        pl.edu.agh.kt.web.NetworkMonitorMetricsResource.setNetworkMonitorInstance(this);
        restApiService.addRestletRoutable(new NetworkMonitorWebRoutable());
        
        logger.info("******************* NetworkMonitor STARTED **************************");
        logger.info("Initial state: {}, Polling interval: {} ms", currentState, currentPollingInterval);
    }
    
    public double getCurrentBandwidth() {
        return calculateWeightedAverage();
    }
    
    public String getCurrentState() {
        return currentState.toString();
    }
    
    public long getCurrentPollingInterval() {
        return currentPollingInterval;
    }
    
    public String getDpidA1() {
        return DPID_A1;
    }
    
    public String getDpidA2() {
        return DPID_A2;
    }
    
    public double getThresholdLowTraffic() {
        return THRESHOLD_LOW_TRAFFIC;
    }
    
    public double getThresholdHighTraffic() {
        return THRESHOLD_HIGH_TRAFFIC;
    }
    
    public long getAnalysisWindow() {
        return ANALYSIS_WINDOW;
    }
}
