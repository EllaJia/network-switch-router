package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.MACAddress;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{
	/** Routing table for the router */
	private RouteTable routeTable;

	/** ARP cache for the router */
	private ArpCache arpCache;

	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}

	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }

	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}

		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}

	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}

		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
		System.out.println("*** -> Received packet: " +
				etherPacket.toString().replace("\n", "\n\t"));

		/********************************************************************/
		/* TODO: Handle packets                                             */

		/**
		 * When an Ethernet frame is received, you should first check if it contains an IPv4 packet.
		 * You can use the getEtherType() method in the net.floodlightcontroller.packet.
		 * Ethernet class to determine the type of packet contained in the payload of the Ethernet frame.
		 * If the packet is not IPv4, you do not need to do any further processing - i.e., your router should drop the packet.
		 */

		short etherType = etherPacket.getEtherType();
		if (etherType != Ethernet.TYPE_IPv4){
			return;
		}

		IPacket iPacket= etherPacket.getPayload();
		IPv4 iPv4 = (IPv4) iPacket;

		//checksum verify
		byte[] ipv4Bytes = iPv4.serialize();
		boolean ret =  checkSumVerify(ipv4Bytes);
		if (ret == false){
			return;
		}


		/**
		 * After verifying the checksum, you should decrement the IPv4 packet’s TTL by 1.
		 * If the resulting TTL is 0, then you do not need to do any further processing - i.e.,
		 * your router should drop the packet.
		 */
		if(iPv4.getTtl()-1 == 0){
			return;
		}

		/**
		 *If the packet’s destination IP address exactly matches one of the interface’s IP addresses
		 * (not necessarily the incoming interface), then you do not need to do any further processing
		 * - i.e., your router should drop the packet.
		 */
		int destinationAddress = iPv4.getDestinationAddress();
		java.util.Iterator it = interfaces.entrySet().iterator();
		while(it.hasNext()){
			java.util.Map.Entry entry = (java.util.Map.Entry)it.next();
			Iface iface = (Iface)entry.getKey();
			if (destinationAddress == iface.getIpAddress()){
				return;
			}
		}

		RouteTable routeTable = new RouteTable();
		RouteEntry routeEntry = routeTable.lookup(destinationAddress);
		if (routeEntry == null){
			return;
		}

		ArpEntry arpEntry = new ArpCache().lookup(routeEntry.getDestinationAddress());
		MACAddress newDestinationMacAddress = arpEntry.getMac();
		MACAddress newSourceMacAddress = routeEntry.getInterface().getMacAddress();

		etherPacket.setDestinationMACAddress(newDestinationMacAddress.toString());
		etherPacket.setSourceMACAddress(newSourceMacAddress.toString());

		byte[] ipv4BytesSend = etherPacket.getPayload().serialize();
		short checkSum = checkSumGenarate(ipv4BytesSend);

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buf);
		try {
			out.writeShort(checkSum);
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] b1 = buf.toByteArray();
		if (b1.length == 1){
			ipv4BytesSend[10] = b1[0];
			ipv4BytesSend[11] = 0x00;
		}else{
			ipv4BytesSend[10] = b1[0];
			ipv4BytesSend[11] = b1[1];
		}


		IPacket iPacket1 = etherPacket.deserialize(ipv4BytesSend,0,24);
		etherPacket.setPayload(iPacket1);

		this.sendPacket(etherPacket,routeEntry.getInterface());


		/********************************************************************/
	}

	private boolean checkSumVerify(byte[] ipv4Bytes){

		int size = 24;//头部长度24字节
		int cksum = 0;
		int index = 0;

		if(size % 2 != 0)
			return false;

		while(index < size)
		{
			cksum += ipv4Bytes[index+1]; //高位
			cksum += ipv4Bytes[index] << 8;

			index += 2;
		}

		while(cksum > 0xffff)
		{
			cksum = (cksum >> 16) + (cksum & 0xffff);
		}

		if (~cksum == 0)
			return true;

		return false;

	}


	private short checkSumGenarate(byte[] ipv4Bytes){

		int size = 24;//头部长度24字节
		int cksum = 0;
		int index = 0;

		ipv4Bytes[10] = 0;
		ipv4Bytes[11] = 0;


//		if(size % 2 != 0)
//			return false;

		while(index < size)
		{
			cksum += ipv4Bytes[index+1]; //高位
			cksum += ipv4Bytes[index] << 8;

			index += 2;
		}

		while(cksum > 0xffff)
		{
			cksum = (cksum >> 16) + (cksum & 0xffff);
		}

		return (short) ~cksum;
	}




}
