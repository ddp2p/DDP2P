package net.ddp2p.common.population;

public class ConstituentProperty {
	public long field_valuesID;
	public String OID;
	public String explain;
	public String OID_name;
	public long certificateID;
	public String certificate;
	public String value;
	public String label;
	public String toString(){
		return "ConstituentProperty: "+value+"("+OID+")";
	}
}