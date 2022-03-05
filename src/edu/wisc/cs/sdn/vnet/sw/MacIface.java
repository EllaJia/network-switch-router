package edu.wisc.cs.sdn.vnet.sw;

import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.MACAddress;

/**
 * @author SuJian
 * @date 2022/3/3 11:01
 */
public class MacIface {

    private MACAddress macAddress;

    private Iface iface;

    private Long expiredTime;

    public MacIface(MACAddress macAddress,Iface iface){
        this.macAddress = macAddress;
        this.iface = iface;
        this.expiredTime = System.currentTimeMillis()/1000 + 15;
    }

    public MACAddress getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(MACAddress macAddress) {
        this.macAddress = macAddress;
    }

    public Iface getIface() {
        return iface;
    }

    public void setIface(Iface iface) {
        this.iface = iface;
    }

    public Long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(Long expiredTime) {
        this.expiredTime = expiredTime;
    }
}
