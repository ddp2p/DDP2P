package hds;
/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
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

import java.util.ArrayList;

import util.Util;
import ciphersuits.Cipher;

import util.P2PDDSQLException;

import config.Application;

public class GenerateKeys extends Thread {
	int size=1024;
	String name;

	public Cipher keys;
	public String gID;
	public String sID;
	public String gIDhash;
	public String type;
	long id;
	String date;
	ArrayList<WorkerListener> listeners = new ArrayList<WorkerListener>();
	/**
	 * 
	 * @param _size
	 * @param _name (e.g.,       "GRASSROOT", ""+name+creation_date)
	 */
	public GenerateKeys(int _size, String _name) {
		size = _size; name = _name;
	}
	public GenerateKeys(String _name, WorkerListener listener) {
		name = _name;
		listeners.add(listener);
	}
	public void generateKey(int size, String name){
		//Cipher keys = Util.getKeyedGlobalID(name);
		keys = ciphersuits.Cipher.getCipher(Util.usedCipherGenkey, Util.usedMDGenkey, name);
		keys.genKey(size);
		gID = Util.getKeyedIDPK(keys);
		sID = Util.getKeyedIDSK(keys);
		//gIDhash = Util.getHash(keys.getPK().encode());
		gIDhash = Util.getGIDhash(gID);
		type = Util.getKeyedIDType(keys);
		try {
			date = Util.getGeneralizedTime();
			id = Application.db.insert(table.key.TNAME, 
					new String[]{table.key.ID_hash,table.key.public_key,table.key.secret_key,table.key.type,table.key.creation_date},
					new String[]{gIDhash, gID, sID, type, date});
			for (WorkerListener l : listeners) {
				try{l.Done(this);}catch(Exception e){e.printStackTrace();}
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public void run() {
		generateKey(size, name);
	}
}
