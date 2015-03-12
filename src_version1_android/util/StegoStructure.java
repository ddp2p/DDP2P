/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2013 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package util;

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
	/**
	 * Return a type signature
	 * @return
	 */
	public short getSignShort();
}