package edu.wisc.cs.sdn.vnet.sw;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.MACAddress;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * @author SuJian
 * @date 2022/3/3 11:00
 */
public class MacTable {

    private static LinkedList<MacIface> macIfaces = new LinkedList<>();

    public static Iface getIface(MACAddress macAddress){

        long currentTime = System.currentTimeMillis()/1000;
        for (MacIface macIface : macIfaces){
            if (macAddress.equals(macIface.getMacAddress())){
                if (macIface.getExpiredTime() >= currentTime){
                    return macIface.getIface();
                }

                macIfaces.remove(macIface);
                return null;
            }
        }

        return null;

    }

    public static void addMacIface(MACAddress macAddress,Iface iface){
        MacIface macIface = new MacIface(macAddress,iface);
        macIfaces.add(macIface);
    }
}
