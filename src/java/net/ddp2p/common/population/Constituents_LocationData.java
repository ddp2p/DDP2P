package net.ddp2p.common.population;

public class Constituents_LocationData {
	public String value;
	public int inhabitants;
	public long organizationID;
	private String label;
	public long fieldID;
	public long field_valuesID;
	public String list_of_values;
	public int partNeigh;
	public long fieldID_above;
	private long fieldID_default_next;
	public String tip;
	public long neighborhood;
	public boolean censusDone = false;
	
	public String toString(){
		return "LocationData: "+value+" #"+fieldID;
	}
	public long getFieldID_default_next() {
		return fieldID_default_next;
	}
	public void setFieldID_default_next(long fieldID_default_next) {
		this.fieldID_default_next = fieldID_default_next;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
}