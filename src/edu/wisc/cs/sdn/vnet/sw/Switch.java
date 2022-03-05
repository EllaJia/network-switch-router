package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.MACAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Switch(String host, DumpFile logfile)
	{
		super(host,logfile);
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
		/***
		 * 1.它收到一个帧的时候，先检查源MAC地址，看看自己维护的一个地址表中有没有这个地址。如果有，则2；如果没有，则将这个MAC地址、进入的端口、进入的时间放入这个表中；
		 * 2.检查目的MAC地址，然后到该表中查找，如果有匹配项，则按照表项中的端口号进行转发；如果没有，则转发到除进口之外的其他所有端口。
		 */

		//学习mac地址
		MACAddress sourceMacAddress = etherPacket.getSourceMAC();
		Iface iface = MacTable.getIface(sourceMacAddress);
		if (iface == null){
			MacTable.addMacIface(sourceMacAddress,inIface);
		}

		//转发
		MACAddress destinationMAC = etherPacket.getDestinationMAC();
		Iface iface1 = MacTable.getIface(destinationMAC);

		if (iface1 == null || etherPacket.isBroadcast()){
			//如果没有，则转发到除进口之外的其他所有端口,广播也是
			Map<String,Iface> interfaces = this.getInterfaces();

			java.util.Iterator it = interfaces.entrySet().iterator();
			while(it.hasNext()){
				java.util.Map.Entry entry = (java.util.Map.Entry)it.next();
				Iface ifaceSend = (Iface)entry.getValue();
				if (!inIface.equals(ifaceSend)){
					this.sendPacket(etherPacket,ifaceSend);
				}
			}

		}else{
			this.sendPacket(etherPacket,iface1);
		}

		/********************************************************************/
	}
}
