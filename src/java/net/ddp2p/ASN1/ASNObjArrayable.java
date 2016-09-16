/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2014 Marius C. Silaghi
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
 package net.ddp2p.ASN1;

import java.util.ArrayList;

/**
 * This is the class that should be extended by objects that are encoded in arrays.
 * @author msilaghi
 *
 */
public abstract class ASNObjArrayable {
	public abstract Encoder getEncoder();
	public byte[] encode() {
		//System.out.println("will encode: " +this);
		return getEncoder().getBytes();
	}
	public abstract Object decode(Decoder dec) throws ASN1DecoderFail;
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {ASN1_Util.printCallPath("getEncoder: you need to implement getEncoder(dictionaries) for objects of type: "+this); return getEncoder();}
	/**
	 * Must be implemented whenever this object is encoded in a sequence (array/list)
	 * @return
	 * @throws CloneNotSupportedException
	 */
	public abstract ASNObjArrayable instance() throws CloneNotSupportedException;
	/**
	 * 
	 * @param dictionary_GIDs
	 * @param dependants : pass 0 for no dependents (ASNObj.DEPENDANTS_NONE)
	 *    pass -1 for DEPENDANTS_ALL.
	 *    Any positive number is decremented at each level.
	 *    
	 *    Other custom schemas can be defined using remaining negative numbers.
	 * @return
	 */
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		ASN1_Util.printCallPath("getEncoder: you need to implement getEncoder(dictionaries, dependants) for objects of type: "+this); return getEncoder(dictionary_GIDs);}
}