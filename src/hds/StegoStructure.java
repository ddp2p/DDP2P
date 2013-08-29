package hds;

import util.P2PDDSQLException;
import ASN1.ASN1DecoderFail;

public interface StegoStructure{
	public void save() throws P2PDDSQLException;
	/**
	 * Method to decode from an ASN1 representation
	 * @param asn1
	 * @throws ASN1DecoderFail
	 */
	public void setBytes(byte[] asn1) throws ASN1DecoderFail;
	/**
	 * Method to encode an ASN1 representation
	 * @return
	 */
	public byte[] getBytes();
	/**
	 * String to display in a console dump
	 * @return
	 */
	public String toString();
	/**
	 * String to display in a confirmation or warning
	 * @return
	 */
	public String getNiceDescription();
	/**
	 * Parse-able string to save in a text file
	 * @return
	 */
	public String getString();
	/**
	 * Parse a string saved in a text file
	 * @return
	 */
	public boolean parseAddress(String content);
}