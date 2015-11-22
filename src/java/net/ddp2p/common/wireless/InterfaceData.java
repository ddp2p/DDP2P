package net.ddp2p.common.wireless;
public class InterfaceData{
	public static final String SEP = ":";
	public String interface_name;
	public String SSID;
	public String IP;
	public String toString(){
		return this.interface_name+SEP+IP+SEP+SSID;
	}
}
