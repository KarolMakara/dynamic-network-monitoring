package pl.edu.agh.kt;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.Ethernet;

import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdnLabListener implements IFloodlightModule, IOFMessageListener {

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;
	
	private Map<Long, Map<MacAddress, OFPort>> macTables;

	@Override
	public String getName() {
		return SdnLabListener.class.getSimpleName();
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
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {

		if (msg.getType() != OFType.PACKET_IN) {
			return Command.CONTINUE;
		}

		OFPacketIn pin = (OFPacketIn) msg;
		
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();
		OFPort inPort = (pin.getVersion().getWireVersion() < 4) ? pin.getInPort() : pin.getMatch().get(MatchField.IN_PORT);
		long switchId = sw.getId().getLong();
		
		if (!macTables.containsKey(switchId)) {
			macTables.put(switchId, new ConcurrentHashMap<MacAddress, OFPort>());
		}
		
		Map<MacAddress, OFPort> macTable = macTables.get(switchId);
		
		if (srcMac != null && inPort != null) {
			macTable.put(srcMac, inPort);
		}
		
		OFPort outPort = null;
		if (dstMac != null && macTable.containsKey(dstMac)) {
			outPort = macTable.get(dstMac);
			
			Flows.simpleAdd(sw, pin, cntx, outPort);
			
		} else {
			outPort = OFPort.FLOOD;
			
			floodPacket(sw, pin, inPort, outPort);
		}

		return Command.CONTINUE;
	}
	
	private void floodPacket(IOFSwitch sw, OFPacketIn pin, OFPort inPort, OFPort outPort) {
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();
		
		if (pin.getBufferId() == OFBufferId.NO_BUFFER) {
			byte[] packetData = pin.getData();
			pob.setData(packetData);
		} else {
			pob.setBufferId(pin.getBufferId());
		}
		
		OFAction output = sw.getOFFactory().actions().buildOutput()
				.setPort(outPort)
				.setMaxLen(0xFFffFFff)
				.build();
		
		pob.setActions(Collections.singletonList(output));
		pob.setInPort(inPort);
		
		sw.write(pob.build());
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
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(SdnLabListener.class);
		macTables = new ConcurrentHashMap<>();
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		logger.info("******************* SdnLabListener (Learning Switch) STARTED **************************");
	}

}
