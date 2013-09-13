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

package hds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import streaming.UpdatePeersTable;
import util.P2PDDSQLException;
import util.Util;
import config.Application;
import config.DD;
import data.D_PeerAddress;
import data.TypedAddressComparator;
import ASN1.ASN1DecoderFail;
import ASN1.ASNObj;
import ASN1.Decoder;
import ASN1.Encoder;

public class TypedAddress extends ASNObj{
	public static final int COMPONENTS = 2; // http://domain:tcp:udp/priority
	public static final String PRI_SEP = "/"; // http://domain:tcp:udp/priority
	public static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	public String address; //utf8
	public String type; //PrintableString
	public boolean certified = false;
	public int priority = 0;
	
	public String last_contact; // not encoded (just for local use)
	public String arrival_date; // not encoded (just for local use)
	public String peer_address_ID; // not encoded (just for local use)

	public TypedAddress(){}
	/**
	 * Order of "in" is according to peer_address.fields
	 * @param in
	 */
	public TypedAddress(ArrayList<Object> in) {
		init(in);
	}
	public void init(ArrayList<Object> in){
		address = Util.getString(in.get(table.peer_address.PA_ADDRESS));
		type = Util.getString(in.get(table.peer_address.PA_TYPE));
		certified = Util.stringInt2bool(in.get(table.peer_address.PA_CERTIFIED), false);
		priority = Util.ival(in.get(table.peer_address.PA_PRIORITY), 0);
		last_contact = Util.getString(in.get(table.peer_address.PA_CONTACT));
		arrival_date = Util.getString(in.get(table.peer_address.PA_ARRIVAL));
		peer_address_ID = Util.getString(in.get(table.peer_address.PA_PEER_ADDR_ID));
		//peer_ID = Util.getString(in.get(table.peer_address.PA_PEER_ID));
	}
	static String sql_by_peer_and_address = 
				"SELECT "+table.peer_address.fields+
				" FROM "+table.peer_address.TNAME+
				" WHERE "+table.peer_address.peer_ID+"=?"+
				" AND "+table.peer_address.address+"=?"+
				" AND "+table.peer_address.type+"=?;";
	public TypedAddress(String peer_ID, String address2, String type2) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> a = Application.db.select(sql_by_peer_and_address, new String[]{peer_ID, address2, type2}, DEBUG);
		if(a.size()>0)
			init(a.get(0));
	}
	public TypedAddress(TypedAddress i) {
		if(i==null) return;
		address = i.address;
		type = i.type;
		certified = i.certified;
		priority = i.priority;
		last_contact = i.last_contact;
		arrival_date = i.arrival_date;
		peer_address_ID = i.peer_address_ID;
	}
	public TypedAddress instance(){return new TypedAddress();}
	public String toString(){
		return "\n\t TypedAddress: [" +
				" cert="+Util.bool2StringInt(certified)+
				" pri="+priority+
				" ID="+peer_address_ID+
				" type="+type+
				" addr="+address+
				" cont="+last_contact+
				" arri="+arrival_date+
				"]";
	}
	public Encoder getEncoder() {
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(address).setASN1Type(Encoder.TAG_UTF8String));
		enc.addToSequence(new Encoder(type).setASN1Type(Encoder.TAG_PrintableString));
		if(certified) {
			//enc.addToSequence(new Encoder(certified));
			enc.addToSequence(new Encoder(priority));
		}
		return enc;
	}
	public TypedAddress decode(Decoder dec){
		Decoder content;
		try {
			content = dec.getContent();
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return this;
		}
		address = content.getFirstObject(true).getString();
		type = content.getFirstObject(true).getString();
		Decoder d = content.getFirstObject(true);
		if(d!=null) {
			certified=true;
			// certified = d.getBoolean();
			// d = content.getFirstObject(true);
			priority = d.getInteger().intValue();
		}
		return this;
	}
	static Object monitored_store_or_update = new Object();
	/**
	 * 
	 * @param peer_ID
	 * @param assume_absent set true to try insert, and update on failure
	 */

	public void store_or_update(String peer_ID, boolean assume_absent) {
		synchronized(monitored_store_or_update){
			try{
				_monitored_store_or_update(peer_ID, assume_absent);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	public void _monitored_store_or_update(String peer_ID, boolean assume_absent) {
		if(DEBUG) System.out.println("TypedAddress:store_and_update: enter peer_ID="+peer_ID);
		String[] params;
		if((this.peer_address_ID == null)&&!assume_absent) {
			try {
				TypedAddress ta = new TypedAddress(peer_ID, address, type);
				this.peer_address_ID = ta.peer_address_ID;
				if(ta.peer_address_ID != null) {
					this.arrival_date = ta.arrival_date;
					this.last_contact = ta.last_contact;
				}
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}
		if(this.peer_address_ID == null) {
			params = new String[table.peer_address.FIELDS_NOID];
			params[table.peer_address.PA_ADDRESS] = this.address;
			params[table.peer_address.PA_TYPE] = this.type;
			params[table.peer_address.PA_CERTIFIED] = Util.bool2StringInt(this.certified);
			params[table.peer_address.PA_PRIORITY] = ""+this.priority;
			params[table.peer_address.PA_PEER_ID] = peer_ID;
			params[table.peer_address.PA_ARRIVAL] = this.arrival_date;
			params[table.peer_address.PA_CONTACT] = this.last_contact;
			try {
				long id = Application.db.insert(table.peer_address.TNAME,
						table.peer_address.fields_noID_list,
						params, DEBUG);
				this.peer_address_ID = Util.getStringID(id);
				if(DEBUG) System.out.println("TypedAddress:store_and_update: return done");
				return;
			} catch (Exception e) {
				//e.printStackTrace();
				if(DEBUG) System.out.println("TypedAddress:store_or_update:"+peer_ID+"->err="+e.getLocalizedMessage());
				TypedAddress ta;
				try {
					ta = new TypedAddress(peer_ID, address, type);
				} catch (P2PDDSQLException e1) {
					e1.printStackTrace();
					return;
				}
				if(ta.peer_address_ID==null) return;
				this.peer_address_ID = ta.peer_address_ID;
			}
		}
		// on failure will update
		params = new String[table.peer_address.FIELDS];			
		params[table.peer_address.PA_ADDRESS] = this.address;
		params[table.peer_address.PA_TYPE] = this.type;
		params[table.peer_address.PA_CERTIFIED] = Util.bool2StringInt(this.certified);
		params[table.peer_address.PA_PRIORITY] = ""+this.priority;
		params[table.peer_address.PA_PEER_ID] = peer_ID;
		params[table.peer_address.PA_ARRIVAL] = this.arrival_date;
		params[table.peer_address.PA_CONTACT] = this.last_contact;
		params[table.peer_address.PA_PEER_ADDR_ID] = this.peer_address_ID;
		try {
			Application.db.update(table.peer_address.TNAME,
					table.peer_address.fields_noID_list,
					new String[]{table.peer_address.peer_address_ID},
					params, DEBUG);
		} catch (Exception e) {
			System.out.println("TypedAddress: unnecessary updating: ID="+this.peer_address_ID+"" +this);
			//e.printStackTrace();
		}
		
		if(DEBUG) System.out.println("TypedAddress:store_and_update: return done");
	}
	public static boolean knownAddress(TypedAddress[] old, String address3,
			String type) {
		if(old==null) return false;
		for(int k=0; k<old.length; k++) {
			if(Util.equalStrings_null_or_not(old[k].address, address3) && Util.equalStrings_null_or_not(old[k].type, type))
				return true;
		}
		return false;
	}
	public static TypedAddress[] parseStringAddresses(String addr){
			String[]addresses_l = Address.split(addr);
			// may have to control addresses, to defend from DOS based on false addresses
			TypedAddress[] address = new TypedAddress[addresses_l.length];
			for(int k=0; k<addresses_l.length; k++) {
				TypedAddress a = new TypedAddress();
				String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_PART_SEP),COMPONENTS);
				if(taddr.length < 2) continue;
				a.type = taddr[0];
				a.type = taddr[0];
				String adr[] = taddr[1].split(Pattern.quote(PRI_SEP), 2);
				a.address = adr[0];
				if(adr.length >= 2) {
					a.certified = true;
					try{
						a.priority = Integer.parseInt(adr[1]);
					}catch(Exception e){e.printStackTrace();}
				}
				address[k] = a;
			}
			return address;
	}
	/*
	public static TypedAddress[] getPeerAddresses(String peer_ID){
		TypedAddress[] result = null;
		try {
			String addresses = UpdatePeersTable.getPeerAddresses(peer_ID, Encoder.getGeneralizedTime(0), Util.getGeneralizedTime());
			if(D_PeerAddress.DEBUG) System.out.println("D_PeerAddress:getPeerAddresses: addresses="+addresses);
			result=getAddress(addresses);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	*/
	/**
	 * Parses addresses found in the DDAddress string form
	 * @param addresses
	 * @return
	 */
	public static TypedAddress[] getAddress(String addresses) {
		//boolean DEBUG = true;
		if(D_PeerAddress.DEBUG) System.out.println("PeerAddress:getAddress: parsing addresses "+addresses);
		String[]addresses_l = Address.split(addresses);
		// may have to control addresses, to defend from DOS based on false addresses
		TypedAddress[] address = new TypedAddress[addresses_l.length];
		for(int k=0; k<addresses_l.length; k++) {
			if(D_PeerAddress.DEBUG)System.out.println("PeerAddress:getAddress: parsing address "+addresses_l[k]);
			TypedAddress a = new TypedAddress();
			String taddr[] = addresses_l[k].split(Pattern.quote(Address.ADDR_TYPE_SEP),COMPONENTS);
			if(taddr.length < 2) continue;
			a.type = taddr[0];
			String adr = taddr[1];
			if(adr==null){address[k]=a; continue;}
			String pri[] = adr.split(Pattern.quote(TypedAddress.PRI_SEP));
			a.address = pri[0];
			if(pri.length > 1) {
				a.certified = true;
				try{
					a.priority = Integer.parseInt(pri[1]);
				}catch(Exception e){e.printStackTrace();}
			}
			address[k] = a;
			if(D_PeerAddress.DEBUG)System.out.println("PeerAddress:getAddress: got typed address "+address[k]);
		}
		return address;
	}
	public static String sql_get_by_peerID=
			"SELECT "+table.peer_address.fields+
			" FROM "+table.peer_address.TNAME+
			" WHERE "+table.peer_address.peer_ID+"=?" +
			" ORDER BY "+table.peer_address.certified+" DESC," +
					table.peer_address.priority+"," +
					table.peer_address.type+"," +
					table.peer_address.address+";";
	/**
	 * They are already in order
	 * @param peer_ID
	 * @return
	 */
	public static TypedAddress[] loadPeerAddresses(String peer_ID) {
		if(DEBUG) System.out.println("TypedAddress:loadPeerAddresses: start = "+peer_ID);
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(sql_get_by_peerID, new String[]{peer_ID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		int len = a.size();
		if(len==0){
			if(DEBUG) System.out.println("TypedAddress:loadPeerAddresses: empty = "+len);
			return null;
		}
		TypedAddress[] result = new TypedAddress[len];
		for(int k = 0; k<len; k++){
			TypedAddress adr = new TypedAddress(a.get(k));
			if(DEBUG) System.out.println("TypedAddress:loadPeerAddresses: getting = "+adr);
			result [k] = adr;
		}
		if(DEBUG) System.out.println("TypedAddress:loadPeerAddresses: done = "+peer_ID);
		return result;
	}
	/**
	 * Used to prepare the addresses for the digital signatures.
	 * Takes only certified entries, and reorders them in the order of priority.
	 * Equal priority is ordered alphabetically by the domain.
	 * Equal domains is by tcp port, then by udp port
	 * Compatible with old addresses.
	 * Returns null on empty result
	 * 
	 * @param addr
	 * @return
	 */
	public static TypedAddress[] getOrderedCertifiedTypedAddresses(TypedAddress[] addr) {
		if((addr==null) || (addr.length==0)) return null;
		ArrayList<TypedAddress> result = new ArrayList<TypedAddress>();
		int k=0;
		for(TypedAddress ta : addr) {
			if(ta==null){
				if(DEBUG||DD.DEBUG_TODO){
					System.out.println("TypedAddress:getOrdCer: No address: "+k);
				
					for(int i=0;i<addr.length; i++) {
						System.out.println("TypedAddress:getOrdCert: adr["+i+"]="+ta);
					}
				}
				continue;
			}
			if(!ta.certified) continue;
			if(ta.address == null) continue;
			result.add(ta);
			k++;
		}
		if(result.size() == 0) return null;
		TypedAddress[] r = result.toArray(new TypedAddress[]{});
		//Collections.sort(result, new TypedAddressComparator());
		Arrays.sort(r, new TypedAddressComparator());
		return r;
	}
	/**
	 * Keeps certified and directories
	 * Ideally would keep certified plus a limited number of the most recent
	 * @param addr
	 * @return
	 */
	public static TypedAddress[] trimTypedAddresses(TypedAddress[] addr, int LIMIT) {
		if((addr==null) || (addr.length==0)) return null;
		int k =0;
		ArrayList<TypedAddress> result = new ArrayList<TypedAddress>();
		for(TypedAddress ta : addr) {
			if(ta.certified){ result.add(ta); continue;}
			if(ta.address == null) continue;
			if(k<LIMIT){
				if(Address.DIR.equals(ta.type)) result.add(ta);
				k++;
			}
		}
		if(result.size() == 0) return null;
		TypedAddress[] r = result.toArray(new TypedAddress[]{});
		//Collections.sort(result, new TypedAddressComparator());
		Arrays.sort(r, new TypedAddressComparator());
		return r;
	}
	/**
	 * list should not already contain ta
	 * @param list
	 * @param ta
	 */
	public static TypedAddress[] insert(TypedAddress[] list, TypedAddress ta) {
		if(ta==null) return list;
		if(list==null) return new TypedAddress[]{ta};
		TypedAddress[] _list = Arrays.copyOf(list, list.length+1);
		_list[list.length]=ta;
		Arrays.sort(_list, new TypedAddressComparator());
		return _list;
	}
	public static TypedAddress[] deep_clone(TypedAddress[] in) {
		if(in==null) return null;
		ArrayList<TypedAddress> result = new ArrayList<TypedAddress>();
		for(int k=0; k<in.length; k++){
			if(in[k]==null) continue;
			result.add(new TypedAddress(in[k]));
		}
		TypedAddress[] out = result.toArray(new TypedAddress[0]);// new TypedAddress[in.length];
		return out;
	}
	public static void delete_difference(TypedAddress[] old_with_contact, TypedAddress[] arr){
		if(DEBUG) System.out.println("TypedAddress:delete_difference: start ");
		if(old_with_contact==null) return;
		for(TypedAddress a: old_with_contact){
			if(DEBUG) System.out.println("delete_difference: old=="+a);
			TypedAddress b = getLastContact(arr, a);
			if(b!=null) continue;
			if(DEBUG) System.out.println("delete_difference: equib new=="+b);
			try {
				Application.db.delete(table.peer_address.TNAME,
						new String[]{table.peer_address.peer_address_ID},
						new String[]{a.peer_address_ID}, DEBUG);
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
		}		
		if(DEBUG) System.out.println("TypedAddress:delete_difference: done ");
	}
	public static TypedAddress[] init_intersection(TypedAddress[] old_with_contact, TypedAddress[] arr){
		if(DEBUG) System.out.println("TypedAddress:init_intersection: start ");
		if((arr==null) || (old_with_contact==null)) return arr;
		for(TypedAddress a: arr){
			if(DEBUG) System.out.println("TypedAddress:init_intersection: new=="+a);
			TypedAddress b = getLastContact(old_with_contact, a);
			if(DEBUG) System.out.println("TypedAddress:init_intersection: equiv old="+b);
			if(b==null) continue;
			a.last_contact = b.last_contact;
			a.arrival_date = b.arrival_date;
			a.peer_address_ID = b.peer_address_ID;
		}
		if(DEBUG) System.out.println("TypedAddress:init_intersection: done");
		return arr;
	}
	/**
	 * Returns from list one that has the same type and address
	 * @param list
	 * @param b
	 * @return
	 */
	public static TypedAddress getLastContact(TypedAddress[] list, TypedAddress b) {
		TypedAddressComparator c = new TypedAddressComparator();
		for(TypedAddress r: list) {
			// if(c.compare(r, b)==0) return r; else continue; // this would check priority
			if(!Util.equalStrings_null_or_not(r.type, b.type)) continue;
			if(!Util.equalStrings_null_or_not(r.address, b.address)) continue;
			return r;
		}
		return null;
	}
	public static void store_and_update(TypedAddress[] addr, String peer_ID){
		if(DEBUG) System.out.println("TypedAddress:store_and_update: start ");
		if(addr==null){
			if(DEBUG) System.out.println("TypedAddress:store_and_update: return empty");
			return;
		}
		for(TypedAddress a: addr){
			if(a==null) {
				if(_DEBUG) System.out.println("TypedAddress:store_and_update: handle null address from "+peer_ID);				
				continue;
			}
			if(DEBUG) System.out.println("TypedAddress:store_and_update: handle "+a);
			a.store_or_update(peer_ID, true);
		}
		if(DEBUG) System.out.println("TypedAddress:store_and_update: return done");
	}
}
