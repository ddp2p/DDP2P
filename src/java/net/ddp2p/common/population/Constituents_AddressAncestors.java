package net.ddp2p.common.population;

public class Constituents_AddressAncestors {
	private String value;
	private long fieldID;
	public Constituents_AddressAncestors(long _fieldID, String _value) {
		setFieldID(_fieldID);
		setValue(_value);
	}
	public String toString(){
		return getValue()+"::"+getFieldID();
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public long getFieldID() {
		return fieldID;
	}
	public void setFieldID(long fieldID) {
		this.fieldID = fieldID;
	}
}