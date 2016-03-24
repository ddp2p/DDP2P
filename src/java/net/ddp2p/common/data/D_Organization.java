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
package net.ddp2p.common.data;

import static java.lang.System.err;
import static java.lang.System.out;
import static net.ddp2p.common.util.Util.__;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.PK;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Motion.D_Motion_Node;
import net.ddp2p.common.data.D_Peer.D_Peer_Node;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.hds.ASNSyncRequest;
import net.ddp2p.common.hds.ResetOrgInfo;
import net.ddp2p.common.streaming.OrgPeerDataHashes;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node_Payload;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Summary;
import net.ddp2p.common.util.Util;

public
class D_Organization extends ASNObj implements  DDP2P_DoubleLinkedList_Node_Payload<D_Organization>, Summary { 
	public static boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	public static final int A_NON_FORCE_COL = 4; 
	private static final String V0 = "0";
	//public static final byte TAG = Encoder.TAG_SEQUENCE; // using DD.TYPE_ORG_DATA
	public static final boolean DEFAULT_BROADCASTED_ORG = true;
	public static final boolean DEFAULT_BROADCASTED_ORG_ITSELF = false;

	// Data signed
	public String version=V0; //PrintableString
	private String global_organization_ID; //PrintableString
	private String global_organization_IDhash;
	private String name; //UTF8String OPTIONAL
	public Calendar reset_date;
	public Calendar last_sync_date;
	public String _last_sync_date;
	public D_OrgParams params = new D_OrgParams(); // AC1 OPTIONAL
	public D_OrgConcepts concepts = new D_OrgConcepts(); //AC2 OPTIONAL
	public byte[] signature;//=new byte[0]; in String representation may start with "G:" 
	public byte[] signature_initiator;// in String representation may start with "G:"

	// Data to be transmitted, relevant to this organization
	public RequestData availableHashes;
	private OrgPeerDataHashes specific_request = new OrgPeerDataHashes();
	
	public D_Peer creator;
	public net.ddp2p.common.data.D_Message requested_data[]; //AC11, data requested on a GID basis
	public ASNNeighborhoodOP neighborhoods[]; //AC3 OPTIONAL
	public ASNConstituentOP constituents[]; //AC4 OPTIONAL
	public D_Witness[] witnesses; //AC5 PTIONAL
	public D_Motion motions[]; //AC6 OPTIONAL
	public D_Justification justifications[]; //AC7 OPTIONAL
	public D_Vote signatures[]; // AC8 OPT
	public D_Translations translations[]; //AC9 OPTIONAL
	public D_News news[];	// AC10 OPTIONAL
	
	// locals
	private String organization_ID;
	private long _organization_ID;
	public String creator_ID;
	
	// preferences
	public Calendar arrival_date;
	public Calendar preferences_date; 
	public String _preferences_date; 
	private String first_provider_peer;
	public boolean blocked = false;
	public boolean requested = false;
	public boolean broadcasted = DEFAULT_BROADCASTED_ORG_ITSELF;
	/** If this is false, then the organization is broadcasted only to the users that are manually specified */
	public boolean broadcast_rule = true;
	/** If this is false, then users should (TODO not implemented) refuse to receive for this organization a neighborhood whose parents/children divisions/labels are not defined in the org params */
	public boolean neighborhoods_rule = true;
	public HashSet<String> preapproved = new HashSet<String>();
	public String _preapproved = null; // ordered (for saving)
	//public int status_references = 0;
	
	net.ddp2p.ciphersuits.Cipher keys;
	
	public boolean dirty_main = false;
	public boolean dirty_params = false;
	public boolean dirty_locals = true;
	public boolean dirty_preferences = true;
	public boolean dirty_mydata = false;
	
	public boolean loaded_globals = false;
	public boolean loaded_locals = false;
	private static Object monitor_object_factory = new Object();
	static Object lock_organization_GID_storage = new Object(); // duplicate of the above
	D_Organization_Node component_node = new D_Organization_Node(null, null);
	private int status_references = 0;
	private boolean temporary = true;
	private boolean hidden = false;
	
	D_Organization_My mydata = new D_Organization_My();
	private long peer_source_LID_unused;

	public static class D_Organization_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Organization> loaded_objects = new DDP2P_DoubleLinkedList<D_Organization>();
		private static Hashtable<Long, D_Organization> loaded_By_LocalID = new Hashtable<Long, D_Organization>();
		private static Hashtable<String, D_Organization> loaded_By_GID = new Hashtable<String, D_Organization>();
		private static Hashtable<String, D_Organization> loaded_By_GIDhash = new Hashtable<String, D_Organization>();
		private static long current_space = 0;
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Organization> my_node_in_loaded;

		public static D_Organization search_GIDhash(String c) {
			DDP2P_DoubleLinkedList_Node<D_Organization> h = loaded_objects.getHead();
			if (h == null) return null;
			if (h.payload != null && c.equals(h.payload.getGIDH()))
				return h.payload;
			for (; h != loaded_objects.getHead(); h = h.getNext()) {
				if (h.payload != null && c.equals(h.payload.getGIDH()))
					return h.payload;
			}
			return null;
		}
	
		public D_Organization_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Organization> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Organization crt) {
			assert((crt.component_node.message==null) && (crt.loaded_globals));
			if(crt.component_node.message != null) return;
			if(!crt.loaded_globals) return;
			if ((crt.getGID() != null) && (!crt.isTemporary())) {
				byte[] message = crt.encode();
				synchronized(loaded_objects) {
					crt.component_node.message = message; // crt.encoder.getBytes();
					if(crt.component_node.message != null) current_space += crt.component_node.message.length;
				}
			}
		}
		/*
		private static void unregister_loaded(D_Organization crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static void register_loaded(D_Organization crt) {
			crt.reloadMessage(); 
			synchronized(loaded_objects) {
				String gidh = crt.getGIDH_or_guess();
				String gid = gidh; //crt.getGID();
				long lid = crt.getLID();
				
				loaded_objects.offerFirst(crt);
				if (lid > 0) {
					// section added for duplication control
					Long oLID = new Long(lid);
					D_Organization old = loaded_By_LocalID.get(oLID);
					if (old != null && old != crt) {
						Util.printCallPath("Double linking of: old="+old+" vs crt="+crt);
						//return false;
						// here we are trying a dirty recovery, but really we should not be here!!!!
						String o_gid = old.getGID();
						String o_gidh = old.getGIDH();
						if (o_gid != null) loaded_By_GID.remove(o_gid);
						if (o_gidh != null) loaded_By_GIDhash.remove(o_gidh);
					}
					
					loaded_By_LocalID.put(oLID, crt);
				}
				if (gid != null) {
					// section added for duplication control
					D_Organization old = loaded_By_GID.get(gid);
					if (old != null && old != crt) {
						Util.printCallPath("D_Organization conflict: gid old="+old+" crt="+crt);
					}
					loaded_By_GID.put(gid, crt);
				}
				if (gidh != null) {
					// section added for duplication control
					D_Organization old = loaded_By_GIDhash.get(gidh);
					if (old != null && old != crt) {
						Util.printCallPath("D_Organization conflict: gidh old="+old+" crt="+crt);
					}
					loaded_By_GIDhash.put(gidh, crt);
				}
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_objects.size() > SaverThreadsConstants.MAX_LOADED_ORGS)
						|| (current_space > SaverThreadsConstants.MAX_ORGS_RAM)) {
					if (loaded_objects.size() <= SaverThreadsConstants.MIN_LOADED_ORGS) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Organization candidate = loaded_objects.getTail();
					if ((candidate.getStatusReferences() > 0)
							||
							D_Organization.is_crt_org(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Organization removed = loaded_objects.removeTail();//remove(loaded_peers.size()-1);
					loaded_By_LocalID.remove(new Long(removed.getLID_forced())); 
					loaded_By_GID.remove(removed.getGID());
					loaded_By_GIDhash.remove(removed.getGIDH_or_guess());
					if (DEBUG) System.out.println("D_Organization: register_loaded: remove GIDH="+removed.getGIDH_or_guess());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Organization crt) {
			loaded_objects.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Organization removed, boolean force) {
			boolean result = true;
			synchronized(loaded_objects) {
				if (removed.getStatusReferences() > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Organization: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				loaded_objects.remove(removed);
				if (removed.getLIDstr_forced() != null) loaded_By_LocalID.remove(new Long(removed.getLID_forced())); 
				if (removed.getGID() != null) loaded_By_GID.remove(removed.getGID());
				if (removed.getGIDH_or_guess() != null) loaded_By_GIDhash.remove(removed.getGIDH_or_guess());
				if (DEBUG) System.out.println("D_Organization: drop_loaded: remove GIDH="+removed.getGIDH_or_guess());
				if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
				if (DEBUG) System.out.println("D_Organization: dropLoaded: exit with force="+force+" result="+result);
				return result;
			}
		}
		/**
		 * This function is used to link an object by its LID when this is obtained
		 * by storing an object already linked by its GIDH (if it was linked)
		 * @param crt
		 * @return
		 * true if i is linked and false if it is not
		 */
		public static boolean register_newLID_ifLoaded(D_Organization crt) {
			if (DEBUG) System.out.println("D_Organization: register_newLID_ifLoaded: start crt = "+crt);
			synchronized (loaded_objects) {
				//String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				if (gidh == null) {
					if (_DEBUG) { System.out.println("D_Organization: register_newLID_ifLoaded: had no gidh! no need of this call.");
					Util.printCallPath("Path");}
					return false;
				}
				if (lid <= 0) {
					Util.printCallPath("Why call without LID="+crt);
					return false;
				}
				
				D_Organization old = loaded_By_GIDhash.get(gidh); //getByGIDH(gidh, );
				if (old == null) {
					if (DEBUG) System.out.println("D_Organization: register_newLID_ifLoaded: was not registered.");
					return false;
				}
				
				if (old != crt)	{
					Util.printCallPath("Different linking of: old="+old+" vs crt="+crt);
					return false;
				}
				
				Long pLID = new Long(lid);
				D_Organization _old = loaded_By_LocalID.get(pLID);
				if (_old != null && _old != crt) {
					Util.printCallPath("Double linking of: old="+_old+" vs crt="+crt);
					return false;
				}
				loaded_By_LocalID.put(pLID, crt);
				if (DEBUG) System.out.println("D_Organization: register_newLID_ifLoaded: store lid="+lid+" crt="+crt.getGIDH());

				return true;
			}
		}
		/**
		 * Fails if not already registered (lid not in hash)
		 * @param crt
		 * @return
		 */
		private static boolean register_newGID_ifLoaded(D_Organization crt) {
			if (DEBUG) System.out.println("D_Organization: register_newGID_ifLoaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_objects) {
				String gidh = crt.getGIDH();
				String gid = gidh;//crt.getGID();
				long lid = crt.getLID();
				if (gidh == null) {
					Util.printCallPath("Why call without GIDH="+crt);
					return false;
				}
				if (lid <= 0) {
					if (_DEBUG) { System.out.println("D_Organization: register_newGID_ifLoaded: had no lid! no need of this call.");
					Util.printCallPath("Path");}
					return false;
				}
								
				Long oLID = new Long(lid);
				D_Organization _old = loaded_By_LocalID.get(oLID);
				if (_old == null) {
					if (DEBUG) System.out.println("D_Organization: register_newGID_ifLoaded: was not loaded");
					return false;
				}
				if (_old != null && _old != crt) {
					Util.printCallPath("Using expired: old="+_old+" vs crt="+crt);
					return false;
				}
				
//				D_Peer_Node.putByGID(gid, crt.getOrganizationLID(), crt); //loaded_const_By_GID.put(gid, crt);
//				if (DEBUG) System.out.println("D_Motion: register_newGID_ifLoaded: store gid="+gid);
//				D_Peer_Node.putByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
//				if (DEBUG) System.out.println("D_Motion: register_newGID_ifLoaded: store gidh="+gidh);
				if (gid != null) { // it is always true since we only locally create ready peers
					// section added for duplication control
					D_Organization old = loaded_By_GID.get(gid);
					if (old != null && old != crt) {
						Util.printCallPath("D_Organization conflict: gid old="+old+" crt="+crt);
					}
					loaded_By_GID.put(gid, crt);
				}
				
				if (gidh != null) { // it is always true since we only locally create ready peers
					// section added for duplication control
					D_Organization old = loaded_By_GIDhash.get(gidh);
					if (old != null && old != crt) {
						Util.printCallPath("D_Organization conflict: gidh old="+old+" crt="+crt);
					}
					loaded_By_GIDhash.put(gidh, crt);
				}
				
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				if (DEBUG) System.out.println("D_Organization: register_newGID_ifLoaded: store lid="+lid+" crt="+crt.getGIDH());

				return true;
			}
		}
	}
	static class D_Organization_My {
		String name;// table.my_organization_data.
		String category;
		String creator;
		//String data_to_request; //replicates organization.specific_requests
		long row;
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (getStatusReferences() <= 0) Util.printCallPath("Null reference already!");
		else decStatusReferences();
		//System.out.println("D_Organization: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (getStatusReferences() > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<String> _need_saving = new HashSet<String>();
	private static HashSet<D_Organization> _need_saving_obj = new HashSet<D_Organization>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Organization> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Organization> node) {
		DDP2P_DoubleLinkedList_Node<D_Organization> old = this.component_node.my_node_in_loaded;
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Organization> 
	get_DDP2P_DoubleLinkedList_Node() {
		return component_node.my_node_in_loaded;
	}
	/*
	static boolean need_saving_contains(String GIDH) {
		return _need_saving.contains(GIDH);
	}
	static void need_saving_add(String GIDH, String instance){
		_need_saving.add(GIDH);
	}
	*/
	static void need_saving_remove(String GIDH) {
		if (DEBUG) System.out.println("D_Organization:need_saving_remove: remove "+GIDH);
		_need_saving.remove(GIDH);
		if (DEBUG) dumpNeeds(_need_saving);
	}
	static void need_saving_obj_remove(D_Organization org) {
		if (DEBUG) System.out.println("D_Organization:need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Organization need_saving_next() {
		Iterator<String> i = _need_saving.iterator();
		if (! i.hasNext()) return null;
		String c = i.next();
		if (DEBUG) System.out.println("D_Organization: need_saving_next: next: "+c);
		D_Organization r = D_Organization_Node.loaded_By_GIDhash.get(c);
		if (r == null) {
			// A BUG?! TRY a last minuyte fix....
			r = D_Organization_Node.search_GIDhash(c);
			if (r == null) {
				_need_saving.remove(c);				
			} else {
				D_Organization_Node.loaded_By_GIDhash.put(c, r);
			}
			if (_DEBUG) {
				System.out.println("D_Organization Cache: need_saving_next null entry "
						+ "needs saving next: " + c+"\nr="+r);
				System.out.println("D_Organization Cache: "+dumpDirCache());
			}
			return r;
		}
		return r;
	}
	static D_Organization need_saving_obj_next() {
		Iterator<D_Organization> i = _need_saving_obj.iterator();
		if (! i.hasNext()) return null;
		D_Organization r = i.next();
		if (DEBUG) System.out.println("D_Organization: need_saving_obj_next: next: "+r);
		//D_Organization r = D_Organization_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Organization Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Organization Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	private static void dumpNeeds(HashSet<String> _need_saving2) {
		System.out.println("Needs:");
		for ( String i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	private static void dumpNeedsObj(HashSet<D_Organization> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Organization i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Organization_Node.loaded_objects.toString();
		s += "]";
		return s;
	}
	
	/**
	 * Methods
	 */
	
	@Override
	public String toSummaryString() {
		String result = "\n OrgData: [\n";
		//";\n  ver = "+version+
		//";\n  id  = "+global_organization_ID+";\n"+
		result += " name="+getName()+"; ";
		result += " b_r="+broadcast_rule+"; ";
		result += " n_r="+neighborhoods_rule+"; ";
		//";\n  l_s_d="+_last_sync_date+
		//";\n  params="+params+
		//";\n  creator="+creator+
		//";\n  concepts=<"+concepts+">"+
		//";\n  signature=<"+Util.byteToHexDump(signature)+">"+
		if(creator != null) result += ";\n  creator="+creator.toSummaryString();
		if(neighborhoods != null) result += ";\n  neighborhoods="+Util.concatSummary(neighborhoods, "\nOrgData-Neighborhood:", null);
		if(constituents != null) result += ";\n  constituents="+Util.concatSummary(constituents, "\nOrgData-Constituents:", null);
		if(witnesses != null) result += ";\n  witnesses="+Util.concatSummary(witnesses, "\nOrgData-Witnesses:", null);
		if(motions != null) result += ";\n  motions="+Util.concatSummary(motions, "\nOrgData-Motions:", null);
		if(justifications != null) result += ";\n  justifications="+Util.concat(justifications, "\nOrgData-Justifications:");
		if(signatures != null) result += ";\n  signatures="+Util.concat(signatures, "\nOrgData-Signatures:");
		if(translations != null) result += ";\n  translations="+Util.concat(translations, "\nOrgData-Translations:");
		if(news != null) result += ";\n  news="+Util.concat(news, "\nOrgData-News:");
		if(availableHashes!=null) result += ";\n  available="+availableHashes.toSummaryString();
		//else result += ";\n  available=null"; // not encoded (sent in advertised in SyncPayload)
		result += "\n]";
		return result;
	}
	public String toString() {
		String result = "OrgData: [tmp="+temporary;
		result += ";\n  ver = "+version;
		result += ";\n  id  = "+getGID();
		result += ";\n  name="+getName();
		result += ";\n  broadcast_rule="+broadcast_rule;
		result += ";\n  neighborhoods_rule="+neighborhoods_rule;
		result += ";\n  l_s_d="+_last_sync_date;
		result += ";\n  params="+params;
		result += ";\n  creator_ID="+creator_ID;
		result += ";\n  creator="+creator;
		result += ";\n  concepts=<"+concepts+">";
		result += ";\n  signature=<"+Util.byteToHexDump(signature)+">";
		result += ";\n  signature_initiator=<"+Util.byteToHexDump(signature_initiator)+">";
		if(neighborhoods != null)
			result += ";\n  neighborhoods="+Util.concat(neighborhoods, "\nOrgData-Neighborhood:");
		if(constituents != null)
			result += ";\n  constituents="+Util.concat(constituents, "\nOrgData-Constituents:");
		if(witnesses != null)
			result += ";\n  witnesses="+Util.concat(witnesses, "\nOrgData-Witnesses:");
		if(motions != null)
			result += ";\n  motions="+Util.concat(motions, "\nOrgData-Motions:");
		if(justifications != null)
			result += ";\n  justifications="+Util.concat(justifications, "\nOrgData-Justifications:");
		if(signatures != null)
			result += ";\n  signatures="+Util.concat(signatures, "\nOrgData-Signatures:");
		if(translations != null)
			result +=  ";\n  translations="+Util.concat(translations, "\nOrgData-Translations:");
		if(news != null)
			result +=  ";\n  news="+Util.concat(news, "\nOrgData-News:");
		if(availableHashes!=null)
			result +=  ";\n  available="+availableHashes;
		result += "\n]";
		return result;
	}
	/**
	 * Allocate an empty organization with the current creation time, and return lID.
	 * To be used during editing.
	 * @return
	 */
	public static long createNewOrg() {
		String currentTime = Util.getGeneralizedTime();
		long org_id = -1;
		try {
			org_id = Application.getDB().insert(net.ddp2p.common.table.organization.TNAME,
					new String[]{net.ddp2p.common.table.organization.creation_date, net.ddp2p.common.table.organization.creation_date, net.ddp2p.common.table.organization.hash_org_alg},
					new String[]{currentTime, currentTime, net.ddp2p.common.table.organization.hash_org_alg_crt});
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return org_id;
	}
	private static final String sql_getOrgByLID =
			"SELECT " + net.ddp2p.common.table.organization.field_list +
			" FROM " + net.ddp2p.common.table.organization.TNAME +
			" WHERE "+ net.ddp2p.common.table.organization.organization_ID + " = ?;";
	public static D_Organization getEmpty() {return new D_Organization();}
	@Override
	public D_Organization instance() {return new D_Organization();}
	private D_Organization() {}
	/**
	 * To be used only privately. Loads from database!
	 * @param __organization_ID
	 * @throws P2PDDSQLException
	 */
	private D_Organization (long __organization_ID) throws P2PDDSQLException {
		if (__organization_ID <= 0) throw new RuntimeException("Invalid org LID: "+__organization_ID);
		this.setLID(__organization_ID);
		init_ByLID(getLIDstr());
	}

	private D_Organization (String __organization_ID) throws P2PDDSQLException {
		this.setLID(__organization_ID);
		if (getLID() <= 0) throw new RuntimeException("Invalid org LID: "+__organization_ID);
		init_ByLID(getLIDstr());
	}
	/**
	 * On creation sets dirty flags but caller should call storeRequest()
	 * Assumes input (GID, GIDH) is sanitized already
	 * @param gID
	 * @param ma 
	 * @param isGID
	 * @param failOnNew
	 * @param __org
	 * @throws P2PDDSQLException 
	 */
	private D_Organization(String gID, String gIDH, boolean create, D_Peer __peer,
			ArrayList<ArrayList<Object>> ma) throws P2PDDSQLException {
//		if (DEBUG) err.println("D_Organization: <init(gID_gIDH)>: org_gidh: "+gIDH);
//		if (gIDH == null) gIDH = D_Organization.getOrgGIDHashGuess(gID);
		if (DEBUG) err.println("D_Organization: <init(gID_gIDH)>: org_gidh: "+gIDH);
		if (ma != null && ma.size() > 0) init(ma.get(0), true, true);
		else init_ByGID(gID, gIDH, create, __peer);
	}
	/**
	 * Create Organization having just this name
	 * @param _org_name
	 * @return
	 */
	public static D_Organization createGrassrootOrganization(String _org_name, D_OrgParam[] params) {
		D_Organization new_org = D_Organization.getEmpty();
		new_org.setName(_org_name);
		//new_org.concepts.name_motion=new String[]{name};
		if (new_org.params == null) new_org.params = new D_OrgParams();
		new_org.params.orgParam = params;
		new_org.params.certifMethods = net.ddp2p.common.table.organization._GRASSROOT;
		new_org.setTemporary(false);
		new_org.setCreationDate();
		//D_Organization.DEBUG = true;
		String GID = new_org.getOrgGIDandHashForGrassRoot();
		//String GID = 
				new_org.setGID(GID, GID); // sign();
		
		//D_Organization org = D_Organization.getOrgByGID_or_GIDhash(GID, GID, true, true, true, null);
		D_Organization o = D_Organization.storeRemote(new_org, null);
		return o;
	}
	/**
	 * 
	 * @param GID
	 * @param GIDhash
	 * @param exception (true to throw RuntimeException of failure)
	 * @return
	 */
	static public boolean verifyGIDhash(String GID, String GIDhash, boolean exception) {
		if (GID == null || GIDhash == null) return true;
			String hash = D_Organization.getOrgGIDHashGuess(GID);
			if (!hash.equals(GIDhash)) {
				System.out.println("D_Organization:getOrgByGID_or_GIDhash_Attempt: mismatch "+GIDhash+" vs "+hash);
				if (exception) throw new RuntimeException("No GID and GIDhash match");
				return false;
			} 
			return true;
	}
	/**
	 * This should return non-null if the coomparison fails!
	 * @param GID
	 * @param GIDhash
	 * @return the correct hash for the GID
	 */
	static public String verifyGIDhashGetGIDH(String GID, String GIDhash) {
		if (GID == null || GIDhash == null) return null;
			String hash = D_Organization.getOrgGIDHashGuess(GID);
			if (!hash.equals(GIDhash)) {
				System.out.println("D_Organization:getOrgByGID_or_GIDhash_Attempt: mismatch "+GIDhash+" vs "+hash);
				//throw new RuntimeException("No GID and GIDhash match");
				return hash;
			} 
			return null;
	}

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static public D_Organization getOrgByLID_AttemptCacheOnly_NoKeep(long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Organization crt = D_Organization_Node.loaded_By_LocalID.get(id);
		if (crt == null) return null;
		
		synchronized (monitor_object_factory) {
			if (load_Globals && !crt.loaded_globals){
				crt.loadGlobals();
				D_Organization_Node.register_fully_loaded(crt);
			}
		}
		D_Organization_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static public D_Organization getOrgByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Organization  crt = getOrgByLID_AttemptCacheOnly_NoKeep(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.incStatusReferences();
					if (crt.getStatusReferences() > 1) {
						System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.getStatusReferences());
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getOrgByLID_AttemptCacheOnly_NoKeep(LID.longValue(), load_Globals);
		}
	}

	/**
	 * This calls the version with "long LID"
	 * @param ID
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	static public D_Organization getOrgByLID(String ID, boolean load_Globals, boolean keep) {
		return getOrgByLID(Util.lval(ID), load_Globals, keep);
	}
	/**
	 * 
	 * @param ID
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	static public D_Organization getOrgByLID(long ID, boolean load_Globals, boolean keep) {
		if (ID <= 0) {
			if (DEBUG) Util.printCallPath("Why?");
			return null;
		}
		Long id = new Long(ID);
		// try first to see if one can avoid waiting for the monitor
		// can comment this out if the structure is not safe for multithreading
		D_Organization crt = D_Organization.getOrgByLID_AttemptCacheOnly(id, load_Globals, keep);
		if (crt != null) return crt;
		
		synchronized (monitor_object_factory) {
			// retry, just in case some other thread just got it
			crt = D_Organization.getOrgByLID_AttemptCacheOnly(id, load_Globals, keep);
			if (crt != null) return crt;
			
			try {
				crt = new D_Organization(ID);
				if (keep) {
					crt.incStatusReferences();
					if (crt.getStatusReferences() > 1) {
						System.out.println("D_Organization: getOrgByLID_AttemptCacheOnly: "+crt.getStatusReferences());
						Util.printCallPath("");
					}
				}
			} catch (Exception e) {
				if (_DEBUG) e.printStackTrace();
				return null;
			}
			D_Organization_Node.register_loaded(crt);
			return crt;
		}
	}
	/**
	 * If the data is not in the cache, it will load it
	 * @param ID
	 * @param load_Globals
	 * @return
	 */
	static public D_Organization getOrgByLID_NoKeep(long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		return getOrgByLID(ID, load_Globals, false);
	}
	static public D_Organization getOrgByLID_NoKeep(String ID, boolean load_Globals){
		try {
			return getOrgByLID_NoKeep(Util.lval(ID), load_Globals);
		} catch (Exception e) {
			return null;
		}
	}

	static private D_Organization getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(String GID, String GIDhash, boolean load_Globals) {
		D_Organization  crt = null;
		if (GIDhash != null) crt = D_Organization_Node.loaded_By_GIDhash.get(GIDhash);
		if ((crt == null) && (GID != null)) crt = D_Organization_Node.loaded_By_GID.get(GID);
		
//		if ((GID != null) && ((crt == null) || (GIDhash == null) || DD.VERIFY_GIDH_ALWAYS)) {
//			if (crt == null) crt = D_Organization_Node.loaded_org_By_GIDhash.get(GIDhash);
//		}
		
		if (crt != null) {
			if (load_Globals && !crt.loaded_globals) {
				crt.loadGlobals();
				D_Organization_Node.register_fully_loaded(crt);
			}
			D_Organization_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * No keep
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static private D_Organization getOrgByGID_or_GIDhash_AttemptCacheOnly(String GID, String GIDhash, boolean load_Globals, boolean keep) {
		if ((GID == null) && (GIDhash == null)) return null;
		if ((GID != null) && (GIDhash == null)) GIDhash = D_Organization.getOrgGIDHashGuess(GID);
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Organization crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
				if (crt != null) {
					crt.incStatusReferences();
					if (crt.getStatusReferences() >  1) {
						Util.printCallPath("Why?");
					}
				}
				return crt;
			}
		} else 
			return getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
	}
	/**
	 * 
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	static public D_Organization getOrgByGID_or_GIDhash_NoCreate(String GID, String GIDhash, boolean load_Globals, boolean keep) {
		return getOrgByGID_or_GIDhash(GID, GIDhash, load_Globals, false, keep, null);
	}
	/**
	 * On create, set temporary
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer : provider
	 * @return
	 */
	static public D_Organization getOrgByGID_or_GIDhash(String GID, String GIDhash, boolean load_Globals, boolean create, boolean keep, D_Peer __peer) {
		return getOrgByGID_or_GIDhash(GID, GIDhash, load_Globals, create, keep, __peer, null);
	}
	/** If storage is not null, then on create does not set temporary */
	static public D_Organization getOrgByGID_or_GIDhash(String GID, String GIDhash, 
			boolean load_Globals, boolean create, boolean keep, D_Peer __peer, D_Organization storage) {
		// boolean DEBUG = true;
		if ((GID == null) && (GIDhash == null)) {
			if (DEBUG) { System.out.println("D_Organization: getOrgByGID_or_GIDhash: all null -> quit");
						Util.printCallPath(""); }
			return null;
		}
		if ((GIDhash != null) && ! D_Organization.isOrgGIDH(GIDhash)) {
			if (GID == null) GID = GIDhash;
			GIDhash = D_Organization.getOrgGIDHashGuess(GIDhash);
		}
		if ((GID != null) && (GIDhash == null)) GIDhash = D_Organization.getOrgGIDHashGuess(GID);
		if (!D_Organization.isOrgGID(GID)) GID = null;
		
		if (create) {
			if (! keep) Util.printCallPath("Why?");
			keep = true;
		}
		D_Organization crt;
		try {
			crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals, keep);
			if (crt != null) return crt;
		} catch (Exception e) {e.printStackTrace();}

		
		synchronized (monitor_object_factory) {
			try {
				crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(GID, GIDhash, load_Globals, keep);
				if (crt != null) return crt;
			} catch (Exception e) {e.printStackTrace();}
			
			try {
				
				/**
				 * Sanitizing input (is it redundant?)
				 */
				GIDhash = D_Organization.getOrgGIDHashGuess(GIDhash);
				GID = D_Organization.getOrgGIDGuess(GID);
				if (DEBUG) System.out.println("D_Organization: init: 2 gID: GIDhash: "+GIDhash+" gID="+GID);
				if (GIDhash == null && GID != null) {
					GIDhash = D_Organization.getOrgGIDHashGuess(GID);
				}

				
				if (DEBUG) err.println("D_Organization: getOrgByGID_or_GIDhash: load org_gidh= "+GIDhash);
				ArrayList<ArrayList<Object>> ma = D_Organization.getOrganizationArrayByGID(GID, GIDhash);
				if (storage == null || ma.size() > 0)
					crt = new D_Organization(GID, GIDhash, create, __peer, ma);
				else {
					D_Organization_Node.dropLoaded(storage, true);
					//if (ma.size() > 0) storage.init(ma.get(0));
					crt = storage.initNew(GID, GIDhash, __peer);
				}
				if (keep) {
					crt.incStatusReferences();
				}
				D_Organization_Node.register_loaded(crt);
				return crt;
			} catch (Exception e) {
				if (DEBUG) e.printStackTrace();//simply not present
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_or_GIDhash: error loading");
				return null;
			}
			// if newly created, then they are dirty
			//if (crt.dirty_any()) crt.storeRequest();
			//Util.printCallPath("CRT="+crt);
		}
	}
	
	
	
	private D_Organization initNew(String gID, String gIDH, D_Peer __peer) {
		if (this.getGID() != null)
			assert(this.getGID().equals(gID));
		gIDH = D_Organization.getOrgGIDHashGuess(gIDH);
		gID = D_Organization.getOrgGIDGuess(gID);
		if (DEBUG) System.out.println("D_Organization: init: 2 gID: GIDhash: "+gIDH+" gID="+gID);
		if (gIDH == null && gID != null) {
			gIDH = D_Peer.getGIDHashGuess(gID);
		}
		
		this.setGID(gID, gIDH);
		if (__peer != null) this.setPeerSourceLID((__peer.getLID()));
		this.dirty_main = true;
		return this;
	}

	private void setPeerSourceLID(long __peer) {
		this.peer_source_LID_unused = __peer;
	}

	//	/**
//	 * exception raised on error
//	 * @param GIDhash
//	 * @param load_Globals 
//	 * @param keep : if true, avoid releasing this until calling releaseReference()
//	 * @return
//	 */
//	static private D_Organization getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
//		if (GIDhash == null) return null;
//		if (keep) {
//			synchronized(monitor_object_factory) {
//				D_Organization  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(null, GIDhash, load_Globals);
//				if (crt != null) {			
//					crt.status_references ++;
//					if (crt.status_references > 1) {
//						System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
//						Util.printCallPath("");
//					}
//				}
//				return crt;
//			}
//		} else {
//			return getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(null, GIDhash, load_Globals);
//		}
//	}
//	/**
//	 * Only attempts too load (only based on GID) if the data is already in the cache
//	 * exception raised on error
//	 * @param GID
//	 * @param load_Globals 
//	 * @return
//	 */
//	static private D_Organization getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
//		if (GID == null) return null;
//		D_Organization crt = D_Organization_Node.loaded_org_By_GID.get(GID);
//		if (crt == null) return null;
//		
//		if (load_Globals && !crt.loaded_globals){
//			crt.loadGlobals();
//			D_Organization_Node.register_fully_loaded(crt);
//		}
//		D_Organization_Node.setRecent(crt);
//		return crt;
//	}
	/**
	 * 
	 * @param GID
	 * @param GIDH
	 * @param load_Globals
	 * @param create
	 * @param __peer
	 * @return
	 */
	static public D_Organization getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(String GID, String GIDH, boolean load_Globals, boolean create, D_Peer __peer) {
		D_Organization p = //getOrgByGID_UpdatingNewGID(GID, load_Globals, create, __peer);
				D_Organization.getOrgByGID_or_GIDhash(GID, GIDH, load_Globals, create, true, __peer);
		// if newly created, then they are dirty
		if ((p != null) && p.dirty_any()) {
			p.storeRequest();
		}
		p.releaseReference();
		return p;
	}
//	/**
//	 * No keep
//	 * @param GID
//	 * @param load_Globals
//	 * @param create
//	 * @param __peer
//	 * @return
//	 */
	/*
	static public D_Organization getOrgByGID_UpdatingNewGID(String GID, boolean load_Globals, boolean create, D_Peer __peer) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if (GID == null) {
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: null GID");
			return null;
		}
		D_Organization crt = D_Organization.getOrgByGID_only_AttemptCacheOnly(GID, load_Globals);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: got GID cached crt="+crt);
			return crt;
		}
		String gIDH = D_Organization.getOrgGIDHashGuess(GID);

		crt = D_Organization.getOrgByGIDhash_AttemptCacheOnly(gIDH, load_Globals, false);
		
		if (crt != null) {
			crt.setGID(GID, gIDH);
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: got GIDH cached crt="+crt);
			return crt;
		}

		synchronized(monitor_object_factory) {
			crt = D_Organization.getOrgByGIDhash_AttemptCacheOnly(gIDH, load_Globals, false);
			if (crt != null) {
				crt.setGID(GID, gIDH);
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Organization(GID, gIDH, create, __peer);
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: loaded crt="+crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: error loading");
				if (DEBUG) e.printStackTrace();
				return null;
			}
			D_Organization_Node.register_loaded(crt);
			if (DEBUG) System.out.println("D_Organization: getOrgByGID_NoStore: Done");
			return crt;
		}
	}
	*/
//	/**
//	 * 
//	 * @param GIDhash
//	 * @param load_Globals
//	 * @param keep
//	 * @param createTemp
//	 * @param __peer
//	 * @return
//	 */
//	static public D_Organization getOrgByGIDhash(String GIDhash, boolean load_Globals, boolean keep, boolean createTemp, D_Peer __peer) {
//		return D_Organization.getOrgByGID_or_GIDhash(null, GIDhash, load_Globals, createTemp, keep, __peer);
//	}
//		if (GIDhash == null) return null;
//		
//		D_Organization crt = getOrgByGIDhash_AttemptCacheOnly(GIDhash, load_Globals, keep);
//		if (crt != null) return crt;
//		
//		synchronized(monitor_object_factory) {
//			crt = getOrgByGIDhash_AttemptCacheOnly(GIDhash, load_Globals, keep);
//			if(crt != null) return crt;
//
//			try {
//				crt = new D_Organization(null, GIDhash, createTemp, null);
//				if (__peer != null) crt.first_provider_peer = __peer.getLIDstr_keep_force();
//				if (keep) {
//					crt.status_references ++;
//					System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
//					Util.printCallPath("");
//				}
//			} catch (Exception e) {
//				//e.printStackTrace();
//				return null;
//			}
//			D_Organization_Node.register_loaded(crt);
//			return crt;
//		}
//	}
	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * @param peer
	 * @return
	 */
	static public D_Organization getOrgByOrg_Keep(D_Organization org) {
		if (org == null) return null;
		String gid = org.getGID();
		String gidh = org.getGIDH_or_guess();
		D_Organization result = null;
		if (gid != null && gidh != null)
			result = getOrgByGID_or_GIDhash_NoCreate(gid, gidh, true, true);
		if (result == null) {
			result = getOrgByLID(org.getLID_forced(), true, true);
		}
		if (result == null) {
			if ((org.getLIDstr_forced() == null) && (org.getGIDH_or_guess() == null)) {
				result = org;
				
				//synchronized (monitor_object_factory) 
				{
					org.incStatusReferences();
					System.out.println("D_Organization: getOrgByOrg_Keep: "+org.getStatusReferences());
					Util.printCallPath("");
				}
			}
		}
		if (result == null) {
			System.out.println("D_Organization: getOrgByOrg_Keep: got null for "+org);
			Util.printCallPath("");
		}
		return result;
	} 
//	/**
//	 * Used when we are now sure whether what we have is a GID or a GIDhash
//	 * exception raised on error
//	 * @param load_Globals (should GIDs in served orgs load full D_Orgs)
//	 * @param createTemp TODO
//	 * @param GID
//	 * @param GIDhash
//	 * @return
//	 */
//	static public D_Organization getOrgBy_or_hash(String GID_or_hash, boolean load_Globals, boolean createTemp){
//		if (GID_or_hash == null) return null;
//		String GID = null;
//		String GIDhash = D_Organization.getOrgGIDHashGuess(GID_or_hash);
//		if (!GIDhash.equals(GID_or_hash)) GID = GID_or_hash;
//
//		D_Organization crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
//		if (crt != null) return crt;
//
//		synchronized (monitor_object_factory) {
//			crt = getOrgByGID_or_GIDhash_AttemptCacheOnly_NoKeep(GID, GIDhash, load_Globals);
//			if (crt != null) return crt;
//			
//			try {
//				crt = new D_Organization(GID, GIDhash, createTemp, null);
//			} catch (Exception e) {
//				e.printStackTrace();
//				return null;
//			}
//			D_Organization_Node.register_loaded(crt);
//			// if newly created, then they are dirty
//			if (crt.dirty_any()) crt.storeRequest();
//			return crt;
//		}
//	}
	
	private void loadGlobals() {
		if (this.loaded_globals) return;

		if ((this.params == null) || (this.creator_ID == null)) {
			this.loaded_globals = true;
 			return;
		}
		if ((this.creator_ID != null ) && (this.params.creator_global_ID == null))
			this.params.creator_global_ID =
				D_Peer.getGIDfromLID(this.creator_ID);
		
		this.loaded_globals = true;
	}

	private static final String sql_getOrgByGIDH =
			"SELECT "+net.ddp2p.common.table.organization.field_list+
			" FROM "+net.ddp2p.common.table.organization.TNAME+
			" WHERE "+net.ddp2p.common.table.organization.global_organization_ID_hash+"=?;";
	private void init_ByLID(String __organization_ID) throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> p_data;

		p_data = Application.getDB().select(sql_getOrgByLID, new String[]{getLIDstr()}, DEBUG);
		if (p_data.size() >= 1) {
				ArrayList<Object> row = p_data.get(0);
				init(row);
		} else {
				throw new RuntimeException("Invalid org LID: "+__organization_ID);
		}
		if (DEBUG) System.out.println("D_Organization:<init(long)>:creatorID="+this.creator_ID+
					"; param.GID="+this.params.creator_global_ID+"; peer_creat="+this.creator);
	}
	/**
	 * sets dirty but does not store, if absent
	 * @param gID
	 * @param orgGIDhash
	 * @param create
	 * @param __peer
	 * @param ma 
	 * @throws P2PDDSQLException
	 */
	public void init_ByGID_inputNotSanitizzed(String gID, String orgGIDhash, boolean create, D_Peer __peer) throws P2PDDSQLException{
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Organization: init: gID: GIDhash: "+orgGIDhash+" gID="+gID);
		// sanitize inputs
		orgGIDhash = D_Organization.getOrgGIDHashGuess(orgGIDhash);
		gID = D_Organization.getOrgGIDGuess(gID);
		if (DEBUG) System.out.println("D_Organization: init: 2 gID: GIDhash: "+orgGIDhash+" gID="+gID);
		if (orgGIDhash == null && gID != null) {
			orgGIDhash = D_Organization.getOrgGIDHashGuess(gID);
		}
		init_ByGID(gID, orgGIDhash, create, __peer);
	}
	/**
	 * sets dirty but does not store, if absent.
	 * Assumes that gID and GIDH are already sanitized
	 * 
	 * @param gID
	 * @param orgGIDhash
	 * @param create
	 * @param __peer
	 * @throws P2PDDSQLException
	 */
	public void init_ByGID(String gID, String orgGIDhash, boolean create, D_Peer __peer) throws P2PDDSQLException{
		if (DEBUG) System.out.println("D_Organization: init: 2 gID: GIDhash: "+orgGIDhash+" gID="+gID);
		assert (orgGIDhash != null);

		
		ArrayList<ArrayList<Object>> p = Application.getDB().select(sql_getOrgByGIDH, new String[]{orgGIDhash}, DEBUG);
		if (p.size() == 0) {
			if (!create) throw new RuntimeException("Absent org hash "+orgGIDhash);
			this.setGID(gID, orgGIDhash);
			if (__peer != null) {
				assert(__peer.getLIDstr_force() != null);
				this.first_provider_peer = __peer.getLIDstr_force();
			}
			this.setTemporary();
			dirty_main = true;
			dirty_params = true;
			init_new();
		} else {
			init(p.get(0), true, true);
		}
	}
	/**
	 * Assumes input sanitized
	 * @param gID
	 * @param orgGIDhash
	 * @return
	 */
	static ArrayList<ArrayList<Object>> getOrganizationArrayByGID(
			String gID, String orgGIDhash) {
		try {
			ArrayList<ArrayList<Object>> p = Application.getDB().select(sql_getOrgByGIDH, new String[]{orgGIDhash}, DEBUG);
			return p;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return new ArrayList<ArrayList<Object>>();
	}
	/**
	 * If input already sanitized, use: getOrganizationArrayByGID
	 * @param gID
	 * @param orgGIDhash
	 * @return
	 */
	static ArrayList<ArrayList<Object>> getOrganizationArrayByGID_InputNotSatitized(
			String gID, String orgGIDhash) {
		orgGIDhash = D_Organization.getOrgGIDHashGuess(orgGIDhash);
		gID = D_Organization.getOrgGIDGuess(gID);
		if (DEBUG) System.out.println("D_Organization: init: 2 gID: GIDhash: "+orgGIDhash+" gID="+gID);
		if (orgGIDhash == null && gID != null) {
			orgGIDhash = D_Organization.getOrgGIDHashGuess(gID);
		}
		return getOrganizationArrayByGID(gID, orgGIDhash);
//		try {
//			ArrayList<ArrayList<Object>> p = Application.db.select(sql_getOrgByGIDH, new String[]{orgGIDhash}, DEBUG);
//			return p;
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//		return new ArrayList<ArrayList<Object>>();
	}
	
	/**
	 * Set all "loaded" markers to true.
	 * The only one actually used is loaded_served_orgs, but all seems to be discardable.
	 */
	private void init_new() {
		if (DEBUG) System.out.println("D_Peer: init_new");
		loaded_globals = true;
		loaded_locals = true;
//		params = new D_OrgParams();
//		concepts = new D_OrgConcepts();
	}
	
	/**
	 * Load creator and extra fields
	 * @param row
	 * @throws P2PDDSQLException
	 */
	private void init(ArrayList<Object> row) throws P2PDDSQLException {
		init(row, true, true);
		if(DEBUG)System.out.println("D_Organization:<init(AL)>:creatorID="+this.creator_ID+
				"; param.GID="+this.params.creator_global_ID+"; peer_creat="+this.creator);
	}
	/**
	 * 
	 * @param row
	 * @param extra_fields
	 * @param extra_creator
	 * @throws P2PDDSQLException
	 */
	private void init(ArrayList<Object> row, boolean extra_fields, boolean extra_creator) throws P2PDDSQLException {
		setLID(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_ID),null));
		this.arrival_date = Util.getCalendar(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_ARRIVAL)),null);
		this.setGID(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_GID),null),
				Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_GID_HASH),null));
		this.setName(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_NAME),null));
		this.params = new D_OrgParams();
		this.params.languages = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_LANG)));
		this.params.instructions_registration = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_INSTRUC_REGIS),null);
		this.params.instructions_new_motions = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_INSTRUC_MOTION),null);
		this.params.description = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_DESCRIPTION),null);
		this.params.default_scoring_options = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_SCORES),null));//new String[0];//null;
		this.params.category = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_CATEG),null);
		
		try {this.params.certifMethods = Integer.parseInt(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_CERTIF_METHODS)));} catch(NumberFormatException e) {this.params.certifMethods = 0;}
		this.params.hash_org_alg = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_HASH_ALG),null);
		this.params.creation_time = Util.getCalendar(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_CREATION_DATE)), Util.CalendargetInstance());
		this.reset_date = Util.getCalendar(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_RESET_DATE)), null);
		this.params.certificate = Util.byteSignatureFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_CERTIF_DATA),null));
		//this.params._icon
		String i64 = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_ICON),null);
		this.params.icon = Util.byteSignatureFromString(i64); //this.params._icon);
		this.params.mWeightsType = Util.ival(row.get(net.ddp2p.common.table.organization.ORG_COL_WEIGHTS_TYPE),net.ddp2p.common.table.organization.WEIGHTS_TYPE_DEFAULT);
		this.params.mWeightsMax = Util.ival(row.get(net.ddp2p.common.table.organization.ORG_COL_WEIGHTS_MAX),net.ddp2p.common.table.organization.WEIGHTS_MAX_DEFAULT);
		//String 
		this.creator_ID = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_CREATOR_ID));
		this.blocked = Util.stringInt2bool(row.get(net.ddp2p.common.table.organization.ORG_COL_BLOCK),false);
		this.temporary = Util.stringInt2bool(row.get(net.ddp2p.common.table.organization.ORG_COL_TEMPORARY),false);
		this.hidden = Util.stringInt2bool(row.get(net.ddp2p.common.table.organization.ORG_COL_HIDDEN),false);
		this.requested = Util.stringInt2bool(row.get(net.ddp2p.common.table.organization.ORG_COL_REQUEST),false);
		this.broadcasted = Util.stringInt2bool(row.get(net.ddp2p.common.table.organization.ORG_COL_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		this.broadcast_rule = Util.stringInt2bool(row.get(net.ddp2p.common.table.organization.ORG_COL_BROADCAST_RULE), true);
		this.neighborhoods_rule = Util.stringInt2bool(row.get(net.ddp2p.common.table.organization.ORG_COL_NEIGHBORHOODS_RULE), true);

		_preapproved = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_PREAPPROVED));
		updatePreapproved();
		setPreferencesDate(null, Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_PREFERENCES_DATE)));
		this.first_provider_peer = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_FIRST_PROVIDER_PEER),null);
		
		this.concepts = new D_OrgConcepts();
		this.concepts.name_organization = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_NAME_ORG),null));
		this.concepts.name_constituent = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_NAME_CONS),null));
		this.concepts.name_forum = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_NAME_FORUM),null));
		this.concepts.name_motion = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_NAME_MOTION),null));
		this.concepts.name_justification = D_OrgConcepts.stringArrayFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_NAME_JUST),null));
		this.signature = getSignatureFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_SIGN),null));
		this.signature_initiator = getSignatureFromString(Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_SIGN_INITIATOR),null));
		
		try {
			String d = Util.getString(row.get(net.ddp2p.common.table.organization.ORG_COL_SPECIFIC));
			if (d != null) {
				this.setSpecificRequest(new OrgPeerDataHashes(new Decoder(Util.byteSignatureFromString(d))));
			}
			if(DEBUG) System.out.println("OrgPeerDataHashes: "+this.getSpecificRequest());
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		
		if (extra_fields) this.params.orgParam = D_Organization.getOrgParams(getLIDstr());
		if (DEBUG) System.out.println("D_Organization:init: extra_creator="+extra_creator+" ID="+creator_ID);
		creator = D_Peer.getPeerByLID_NoKeep(creator_ID, true); // for creator one also needs _served
		if (extra_creator) {
			if (creator == null) {
				if (DEBUG) System.out.println("D_Organization:init: no creator");
				//if(DEBUG)Util.printCallPath("No creator:"+creator_ID);
				//Application.warning(Util._("Missing organization creator, or may have incomplete data. You should create a new one!"), Util._("Missing organization creator."));
				//throw new Exception("No creator");
			} else {
				this.params.creator_global_ID = creator.component_basic_data.globalID;
				if (DEBUG) System.out.println("D_Organization:init: creator="+creator);
			}
		} else {
			if (creator != null) this.params.creator_global_ID = creator.getGID();
			creator = null;
			if (DEBUG) System.out.println("D_Organization:init: param_creator="+this.params.creator_global_ID);
		}

		if ((this.signature_initiator == null) && (this.params.creator_global_ID != null)) {
			if (_DEBUG) System.out.println("D_Organization: init: this organization has a creator but no signature:"+this);
			//byte[] msg = this.getSignableEncoder().getBytes();
			//SK sk = Util.getStoredSK(this.params.creator_global_ID, null);
			//if (sk != null) {
			//	this.signature_initiator = Util.sign(msg, sk);
			if (signIni(false) != null) {
				Application.getDB().updateNoSync(net.ddp2p.common.table.organization.TNAME,
						new String[]{net.ddp2p.common.table.organization.signature_initiator},
						new String[]{net.ddp2p.common.table.organization.organization_ID},
						new String[]{Util.stringSignatureFromByte(this.signature_initiator), this.getLIDstr()}, _DEBUG);
				Application_GUI.warning(__("Updated org initiator signature for:")+this.getName(), __("Updated org Signature"));
			}
		}
		
		String my_org_sql = "SELECT "+net.ddp2p.common.table.my_organization_data.fields_list+
				" FROM "+net.ddp2p.common.table.my_organization_data.TNAME+
				" WHERE "+net.ddp2p.common.table.my_organization_data.organization_ID+" = ?;";
		ArrayList<ArrayList<Object>> my_org = Application.getDB().select(my_org_sql, new String[]{getLIDstr_forced()}, DEBUG);
		if (my_org.size() != 0) {
			ArrayList<Object> my_data = my_org.get(0);
			mydata.name = Util.getString(my_data.get(net.ddp2p.common.table.my_organization_data.COL_NAME));
			mydata.category = Util.getString(my_data.get(net.ddp2p.common.table.my_organization_data.COL_CATEGORY));
			mydata.creator = Util.getString(my_data.get(net.ddp2p.common.table.my_organization_data.COL_CREATOR));
			mydata.row = Util.lval(my_data.get(net.ddp2p.common.table.my_organization_data.COL_ROW));
		}
		
		initSK();
	}
	/**
	 * Init the hashset from the string _preapproved
	 */ 
	private void updatePreapproved() {
		if (_preapproved != null) {
			preapproved = getEmailsSet(_preapproved);
		} else
			preapproved = new HashSet<String>();
	}
	public static D_Organization updatePreapprovedRequest(D_Organization crt_org, String __preapproved,  Calendar _currentTime, String currentTime) {
		crt_org = D_Organization.getOrgByOrg_Keep(crt_org);
		crt_org._preapproved = __preapproved;
		crt_org.updatePreapproved();
		crt_org.setPreferencesDate(_currentTime, currentTime);
		crt_org.storeLocalFlags();
		crt_org.releaseReference();
		return crt_org;
	}
	/**
	 * Splits email list by any separator
	 *  (table.organization.SEP_PREAPPROVED ; : , space) and trims spaces.
	 * Intended for simple emails, without names
	 * @param emails
	 * @return
	 */
	public static HashSet<String> getEmailsSet(String emails) {
		HashSet<String> emailsSet = new HashSet<String>();
		String[] _p;
		_p = Util.trimmed(emails.split(Pattern.quote(net.ddp2p.common.table.organization.SEP_PREAPPROVED)));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(";")));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(":")));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(",")));
		if (_p.length == 1)
			_p = Util.trimmed(emails.split(Pattern.quote(" ")));
		for (String p: _p) {
			emailsSet.add(p);
		}
		return emailsSet;
	}
	/**
	 * Assumes the signature starts with "G:" (D_GIDH.d_OrgSign)
	 * @param s
	 * @return
	 */
	private static byte[] getSignatureFromString(String s) {
		if (s == null) return null;
		int pref_len = D_GIDH.d_OrgGrassSign.length(); // 2
		if (s.length() < pref_len) return null;
		if (s.startsWith(D_GIDH.d_OrgGrassSign)) return Util.byteSignatureFromString(s.substring(pref_len));
		return Util.byteSignatureFromString(s);
	}
	
	/**
	 * Without id, (which is the result of hash/signing, and without constituents etc
	 * @return
	 */
	private Encoder getSignableEncoder() {
		if (ASNSyncRequest.DEBUG || DEBUG) System.out.println("Encoding OrgData sign: "+this);
		if (DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING && this.getCreatorGID() == null) {
			Util.printCallPath("No creator for this org!");
			//return null;
		}
		//boolean DEBUG = true;
		Encoder enc = new Encoder().initSequence();
		if (DEBUG) System.out.println("signHash = 1: "+hashEnc(enc));
		enc.addToSequence(new Encoder(version,false));
		if (DEBUG) System.out.println("signHash = 2: "+hashEnc(enc));
		//enc.addToSequence(new Encoder(id,false));
		if (getName() != null) enc.addToSequence(new Encoder(getName()));
		if (DEBUG) System.out.println("signHash = 3: "+hashEnc(enc));
		//if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder().setASN1Type(DD.TAG_AC1));
		if (DEBUG) System.out.println("signHash = 4: "+hashEnc(enc));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if (DEBUG) System.out.println("signHash = 5: "+hashEnc(enc));
		if (broadcast_rule != true) enc.addToSequence(new Encoder(broadcast_rule).setASN1Type(DD.TAG_AC15));
		if (DEBUG) System.out.println("signHash = 6: "+hashEnc(enc));
		if (neighborhoods_rule != true) enc.addToSequence(new Encoder(neighborhoods_rule).setASN1Type(DD.TAG_AC16));
		if (DEBUG) System.out.println("signHash = 7: "+hashEnc(enc));
		if (ASNSyncRequest.DEBUG || DEBUG) System.out.println("Encoded OrgData sign: "+this);
		if (DEBUG) System.out.println("signHash = F: "+hashEnc(enc));
		return enc;
	}
	/**
	 * Used to compare between two authoritarian orgs with identical GID and creation date
	 * @return
	 */
	private Encoder getEntityEncoder() {
		if (ASNSyncRequest.DEBUG) System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		//enc.addToSequence(new Encoder(version,false));
		enc.addToSequence(new Encoder(getGID(),false));
		if (getName() != null) enc.addToSequence(new Encoder(getName()));
		//if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder().setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if (broadcast_rule != true) enc.addToSequence(new Encoder(broadcast_rule).setASN1Type(DD.TAG_AC15));
		if (neighborhoods_rule != true) enc.addToSequence(new Encoder(neighborhoods_rule).setASN1Type(DD.TAG_AC16));
		//if (signature != null) enc.addToSequence(new Encoder(signature));
		//if (creator != null) enc.addToSequence(creator.getEncoder().setASN1Type(DD.TAG_AC0));
		//if (neighborhoods != null) enc.addToSequence(Encoder.getEncoder(neighborhoods).setASN1Type(DD.TAG_AC3));
		//if (constituents != null) enc.addToSequence(Encoder.getEncoder(constituents).setASN1Type(DD.TAG_AC4));
		//if (witnesses != null) enc.addToSequence(Encoder.getEncoder(witnesses).setASN1Type(DD.TAG_AC5));
		//if (motions != null) enc.addToSequence(Encoder.getEncoder(motions).setASN1Type(DD.TAG_AC6));
		//if (justifications != null) enc.addToSequence(Encoder.getEncoder(justifications).setASN1Type(DD.TAG_AC7));		
		//if (signatures != null) enc.addToSequence(Encoder.getEncoder(signatures).setASN1Type(DD.TAG_AC8));
		//if (translations != null) enc.addToSequence(Encoder.getEncoder(translations).setASN1Type(DD.TAG_AC9));
		//if (news != null) enc.addToSequence(Encoder.getEncoder(news).setASN1Type(DD.TAG_AC10));
		//if (requested_data != null) enc.addToSequence(Encoder.getEncoder(requested_data).setASN1Type(DD.TAG_AC11));
		enc.setASN1Type(getASN1Type());//DD.TYPE_ORG_DATA);
		if (ASNSyncRequest.DEBUG) System.out.println("Encoded OrgData: "+this);
		return enc;
	}

	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	/**
	 * parameter used for dictionaries
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) { 
		return getEncoder(dictionary_GIDs, 0);
	}
	/**
	 * TODO: Have to decide later if neighborhoods and submitter should be send even when descendants is none...
	 * TODO: Also have to decide if to encode descendants using the provided dependants (or inferred new_dependents)
	 * Currently only passing new_dependants to the creator
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		
		if (ASNSyncRequest.DEBUG) System.out.println("Encoding OrgData: "+this);
		Encoder enc = new Encoder().initSequence();
		enc.addToSequence(new Encoder(version,false));
		String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
		enc.addToSequence(new Encoder(repl_GID,false));
		if (getName() != null) enc.addToSequence(new Encoder(getName()));
		if (last_sync_date != null) enc.addToSequence(new Encoder(last_sync_date));
		if (params != null) enc.addToSequence(params.getEncoder(dictionary_GIDs).setASN1Type(DD.TAG_AC1));
		if (concepts != null) enc.addToSequence(concepts.getEncoder().setASN1Type(DD.TAG_AC2));
		if (broadcast_rule != true) enc.addToSequence(new Encoder(broadcast_rule).setASN1Type(DD.TAG_AC15));
		if (neighborhoods_rule != true) enc.addToSequence(new Encoder(neighborhoods_rule).setASN1Type(DD.TAG_AC16));
		if (signature != null) enc.addToSequence(new Encoder(signature));
		if (signature_initiator != null) enc.addToSequence(new Encoder(signature_initiator).setASN1Type(DD.TAG_AC14));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (ASNSyncPayload.STREAMING_SEND_CREATOR_IN_PEER)
				if (creator != null) enc.addToSequence(creator.getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC0));
		}
		
		if (neighborhoods != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in neighborhoods: "+neighborhoods);
			enc.addToSequence(Encoder.getEncoder(neighborhoods, dictionary_GIDs).setASN1Type(DD.TAG_AC3));
		}
		if (constituents != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in constituents: "+constituents);
			enc.addToSequence(Encoder.getEncoder(constituents, dictionary_GIDs).setASN1Type(DD.TAG_AC4));
		}
		if (witnesses != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in witnesses: "+witnesses);
			enc.addToSequence(Encoder.getEncoder(witnesses, dictionary_GIDs).setASN1Type(DD.TAG_AC5));
		}
		if (motions != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in motions: "+motions);
			enc.addToSequence(Encoder.getEncoder(motions, dictionary_GIDs).setASN1Type(DD.TAG_AC6));
		}
		if (justifications != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in justifications: "+justifications);
			enc.addToSequence(Encoder.getEncoder(justifications, dictionary_GIDs).setASN1Type(DD.TAG_AC7));		
		}
		if (signatures != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in signatures: "+signatures);
			enc.addToSequence(Encoder.getEncoder(signatures).setASN1Type(DD.TAG_AC8));
		}
		if (translations != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in translations: "+translations);
			enc.addToSequence(Encoder.getEncoder(translations, dictionary_GIDs).setASN1Type(DD.TAG_AC9));
		}
		if (news != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in news: "+news);
			enc.addToSequence(Encoder.getEncoder(news, dictionary_GIDs).setASN1Type(DD.TAG_AC10));
		}
	
		if (requested_data != null) {
			if (_DEBUG) System.out.println("D_Organization: getEncoder: why? data available in requested_data: "+requested_data);
			enc.addToSequence(Encoder.getEncoder(requested_data, dictionary_GIDs).setASN1Type(DD.TAG_AC11));
		}
		// these are aggregated in ASNSyncPayload in "advertised"
		//if (this.availableHashes != null) enc.addToSequence(availableHashes.getEncoder().setASN1Type(DD.TAG_AC12));
		
		enc.setASN1Type(getASN1Type());
		if (ASNSyncRequest.DEBUG) System.out.println("Encoded OrgData: "+this);
		return enc;
	}

	@Override
	public D_Organization decode(Decoder decoder) throws ASN1DecoderFail {
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoding OrgData: "+this);
		if((decoder==null)||(decoder.getTypeByte()==Encoder.TAG_NULL)) return null;
		Decoder dec = decoder.getContent();
		version = dec.getFirstObject(true, Encoder.TAG_PrintableString).getString(Encoder.TAG_PrintableString);
		setGID(dec.getFirstObject(true, Encoder.TAG_PrintableString).getString(Encoder.TAG_PrintableString), null);
		
		if (DEBUG) System.out.println("OrgData id="+getGID());
		if (dec.getTypeByte() == Encoder.TAG_UTF8String) {
			setName(dec.getFirstObject(true, Encoder.TAG_UTF8String).getString(Encoder.TAG_UTF8String));
			if (DEBUG) System.out.println("OrgData name="+getName());
		}
		
		if(dec.getTypeByte()==Encoder.TAG_GeneralizedTime){ setLastSyncDate(dec.getFirstObject(true).getGeneralizedTimeCalenderAnyType()); if(DEBUG )System.out.println("OrgData d="+last_sync_date);}
		if(dec.getTypeByte()==DD.TAG_AC1){ params=new D_OrgParams().decode(dec.getFirstObject(true)); if(DEBUG )System.out.println("OrgData p="+params);}
		if(dec.getTypeByte()==DD.TAG_AC2){ concepts=new D_OrgConcepts().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData c="+concepts);}
		if(dec.getTypeByte()==DD.TAG_AC15){ broadcast_rule = dec.getFirstObject(true).getBoolean(DD.TAG_AC15); if(DEBUG)System.out.println("OrgData b_r="+broadcast_rule);}
		if(dec.getTypeByte()==DD.TAG_AC16){ neighborhoods_rule = dec.getFirstObject(true).getBoolean(DD.TAG_AC16); if(DEBUG)System.out.println("OrgData n_r="+neighborhoods_rule);}
		if(dec.getTypeByte()==Encoder.TAG_OCTET_STRING){ signature=dec.getFirstObject(true).getBytes(); if(DEBUG)System.out.println("OrgData s="+Util.byteToHexDump(signature));}
		if(dec.getTypeByte()==DD.TAG_AC14){ signature_initiator=dec.getFirstObject(true).getBytes(DD.TAG_AC14); if(DEBUG)System.out.println("OrgData s="+Util.byteToHexDump(signature_initiator));}
		if(dec.getTypeByte()==DD.TAG_AC0){ creator = D_Peer.getEmpty().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData cr="+creator);}
		if(dec.getTypeByte()==DD.TAG_AC3){ neighborhoods = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNNeighborhoodOP[]{}, new ASNNeighborhoodOP()); if(DEBUG)System.out.println("OrgData n="+neighborhoods);}
		if(dec.getTypeByte()==DD.TAG_AC4){ constituents = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new ASNConstituentOP[]{}, new ASNConstituentOP()); if(DEBUG)System.out.println("OrgData co="+constituents);}
		if(dec.getTypeByte()==DD.TAG_AC5){ witnesses = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Witness[]{}, new D_Witness()); if(DEBUG)System.out.println("OrgData w="+witnesses);}
		if(dec.getTypeByte()==DD.TAG_AC6){ motions = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Motion[]{}, D_Motion.getEmpty()); if(DEBUG)System.out.println("OrgData m="+motions);}
		if(dec.getTypeByte()==DD.TAG_AC7){ justifications = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Justification[]{}, D_Justification.getEmpty()); if(DEBUG)System.out.println("OrgData j="+justifications);}
		if(dec.getTypeByte()==DD.TAG_AC8){ signatures = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Vote[]{}, new D_Vote()); if(DEBUG)System.out.println("OrgData s="+signatures);}
		if(dec.getTypeByte()==DD.TAG_AC9){ translations = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_Translations[]{}, new D_Translations()); if(DEBUG)System.out.println("OrgData t="+translations);}
		if(dec.getTypeByte()==DD.TAG_AC10){ news = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new D_News[]{}, D_News.getEmpty()); if(DEBUG)System.out.println("OrgData nw="+news);}
		if(dec.getTypeByte()==DD.TAG_AC11){ requested_data = dec.getFirstObject(true).getSequenceOf(Encoder.TYPE_SEQUENCE, new net.ddp2p.common.data.D_Message[]{}, new net.ddp2p.common.data.D_Message()); if(DEBUG)System.out.println("OrgData rd="+requested_data);}
		//if(dec.getTypeByte()==DD.TAG_AC12){ availableHashes = new RequestData().decode(dec.getFirstObject(true)); if(DEBUG)System.out.println("OrgData available="+availableHashes);}
		if(dec.getFirstObject(false)!=null) throw new ASN1DecoderFail("Extra Objects in decoder: "+dec.dumpHex()+"\n"+decoder.dumpHex());
		if(ASNSyncRequest.DEBUG)System.out.println("DEncoded OrgData: "+this);
		return this;
	}
	
	public String getCreationDate_str() {
		return Encoder.getGeneralizedTime(this.params.creation_time);
	}
	private void setCreationDate(Calendar time) {
		this.params.creation_time = time;
		this.dirty_main = true;
	}
	public void setCreationDate(String currentTime) {
		this.params.creation_time = Util.getCalendar(currentTime);
		this.dirty_main = true;
	}
	public void setCreationDate() {
		this.params.creation_time = Util.CalendargetInstance();
		this.dirty_main = true;
	}
	
	private String getArrivalDate_str() {
		return Encoder.getGeneralizedTime(this.arrival_date);
	}
	public void setArrivalDate(String currentTime, Calendar _currentTime) {
		this.arrival_date = _currentTime;
		if (_currentTime == null)
			this.arrival_date = Util.getCalendar(currentTime);
		this.dirty_locals = true;
	}
	/**
	 * Sets the next arrival time using the sequencer: ArrivalDateStream.getNextArrivalDate();
	 */
	public void setArrivalDate() {
		this.arrival_date = ArrivalDateStream.getNextArrivalDate();
		this.dirty_locals = true;
	}
	
	/**
	 * Sets both the Calendar and String version.
	 * @param p_last_sync_date
	 */
	public void setLastSyncDate(Calendar p_last_sync_date){
		last_sync_date = p_last_sync_date;
		_last_sync_date = Encoder.getGeneralizedTime(p_last_sync_date);
	}
	/**
	 * sets the last_sync_date
	 * @param lsd
	 */
	public void setLastSyncDate(String lsd) {
		_last_sync_date = lsd;
		last_sync_date = Util.getCalendar(lsd);
	}	
	/**
	 * 
	 * @param c
	 * @param _c
	 */
	public void setPreferencesDate(Calendar c, String _c) {
		if ((c == null) && (_c == null)) return;
		preferences_date = ((c == null) ? Util.getCalendar(_c) : c);
		_preferences_date = ((_c == null) ? Encoder.getGeneralizedTime(c) : _c);
	}
	public Calendar setPreferencesDate() {
		this.preferences_date = Util.CalendargetInstance();
		this._preferences_date = Encoder.getGeneralizedTime(preferences_date);
		this.dirty_preferences = true;
		return preferences_date;
	}
	public void setRequested(boolean val) {
		if (DEBUG) System.out.println("Orgs:setRequested: set="+val);
		this.requested = val;
		setPreferencesDate();
		this.dirty_preferences = true;
	}
	public void setBlocking(boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		if (this.blocked == val) return;
		this.blocked = val;
		setPreferencesDate();
		this.dirty_preferences = true;
	}
	/**
	 * 
	 * @param val
	 */
	public void setBroadcasting(boolean val) {
		if(DEBUG) System.out.println("Orgs:setBlocking: set="+val);
		if (this.broadcasted == val) return;
		this.broadcasted = val;
		if (val)
			this.reset_date = Util.CalendargetInstance();
		setPreferencesDate();
		this.dirty_preferences = true;
	}
	/**
	 * This function just sets the "broadcasted" flag (calling sync on the database).
	 * To change "org.broadcasted" (if also advertising it!), better change with toggleServing
	 *  which sets also "peer.served_orgs" (by calling this).
	 * @param orgID
	 * @param val
	 */
	public static void setBroadcasting(String orgID, boolean val) {
		if (DEBUG) System.out.println("Orgs:setBroadcasting: set="+val+" for orgID="+orgID);
		D_Organization org = D_Organization.getOrgByLID(orgID, true, true);
		
		org.setBroadcasting(val);
		//org.broadcasted = val;
		//org.reset_date = Util.CalendargetInstance();
		org.dirty_main = true; //?? why
		//org.dirty_locals = true;
		org.storeRequest();
		org.releaseReference();
		
		if (DEBUG) System.out.println("Orgs:setBroadcasting: Done");
	}

	public static boolean is_crt_org(D_Organization candidate) {
		//D_Peer myself = data.HandlingMyself_Peer.get_myself();
		//if (myself == candidate) return true;
		return Application_GUI.is_crt_org(candidate);
	}
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}
	/**
	 * Returns the GIDH. If null but GID nonull, computes it
	 * @return
	 */
	public String getGIDH_or_guess() {
		if ((getGIDH() == null) && (getGID() != null))
			setGIDH(D_Organization.getOrgGIDHashGuess(getGID()));
		return this.getGIDH();
	}
	/**
	 * Returns the GIDH variables as is
	 * @return
	 */
	public String getGIDH() {
		return this.global_organization_IDhash;
	}
	/**
	 * Returns GID as is
	 * @return
	 */
	public String getGID() {
		return this.global_organization_ID;
	}
	
	/**
	 * Remove the "G:" prefix (D_GIDH.d_OrgSign)
	 * @param id
	 * @return
	 */
	public static byte[] getHashFromGrassrootGID(String gID) {
		if ((gID == null) || (gID.length() < D_GIDH.d_OrgGrassSign.length())) return null;
		return Util.byteSignatureFromString(gID.substring(D_GIDH.d_OrgGrassSign.length()));
	}
	/**
	 * Returns 
	 * 
	 * "O:"(D_GIDH.d_OrgAuthSign)+DD.APP_ID_HASH+DD.APP_ID_HASH_SEP+base64(hash(decode64(GID)))
	 * @param GID
	 * @return
	 */
	public static String getOrgGIDHashAuthoritarian(String GID) {
		if (GID == null) return null;
		String hash = Util.getGIDhash(GID);
		if (hash == null) return null;
		return D_GIDH.d_OrgAuthSign + hash;
	}
	/**
	 * Gets GIDhash from internal GIFfunction on type
	 * @return
	 */
	public String getOrgGIDHashFromGID() {
		String result = null;
		if (params == null) return null;
		if (params.certifMethods == net.ddp2p.common.table.organization._GRASSROOT) {
			result = getGID();
		} else if (params.certifMethods == net.ddp2p.common.table.organization._AUTHORITARIAN) {
			result = getOrgGIDHashAuthoritarian(getGID());
		} else result = null;
		return  result;
	}
	/**
	 * decides based on prefix (O: GIDH, G: grassroot GID/GIDH, else GID->GIDH) 
	 * old version: try to compute hash. if not shorter, then this is already the hash
	 * @param GID
	 * @return
	 */
	public static String getOrgGIDHashGuess(String GID) {
		if (GID == null) return null;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return GID; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return GID; // grass root
		String GID_hash = getOrgGIDHashAuthoritarian(GID);
		//if(GID_hash.length() == GID.length()) return GID;
		return GID_hash;
	}
	/**
	 * returns GID if it is not hash of authoritarian
	 * @param GID
	 * @return
	 */
	public static String getOrgGIDGuess(String GID) {
		if (GID == null) return null;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return null; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return GID; // grass root
		return GID;
	}
	public static boolean isOrgGID(String GID) {
		if (GID == null) return false;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return false; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return true; // grass root
		return true;
	}
	public static boolean isOrgGIDH(String GID) {
		if (GID == null) return false;
		if (GID.startsWith(D_GIDH.d_OrgAuthSign)) return true; // already hash
		if (GID.startsWith(D_GIDH.d_OrgGrassSign)) return true; // grass root
		return false;
	}
	public void setGID_AndLink (String gID, String gIDH) {
		setGID (gID, gIDH);
		if (this.getGIDH() != null)
			D_Organization_Node.register_newGID_ifLoaded(this);
	}
	public void setGID (String gID, String gIDH) {
		//boolean DEBUG = true;
		//boolean loaded_in_cache = this.isLoadedInCache();
//		if (DEBUG) System.out.println("D_Organization:setGID: start loaded="+loaded_in_cache);
		String oldGID = this.getGID();
		if ( ! Util.equalStrings_null_or_not(oldGID, gID)) {		
			if (DEBUG) System.out.println("D_Organization:setGID: change GID");
//			if (oldGID != null)
//				loaded_in_cache |= (null!=D_Organization_Node.loaded_By_GID.remove(oldGID));
			//this.setGID (gID);
			this.global_organization_ID = gID;
			this.dirty_main = true;
		}
		if ((gID != null) && (gIDH == null)) {
			if (DEBUG) System.out.println("D_Organization:setGID: compute GIDH");
			if (! D_GIDH.isCompactedGID(gID)) {
				gIDH = D_Organization.getOrgGIDHashGuess(gID);
				if (gIDH == null) Util.printCallPath("D_Organization: setGID:"+gID+" for: "+this);
			} else {
				// the next would be wrong if this is authoritarian!
				//gIDH = gID;
			}
		}

		if ( ! Util.equalStrings_null_or_not(getGIDH(), gIDH)) {		
			if (DEBUG) System.out.println("D_Organization:setGID: change GIDH");
//			if (this.global_organization_IDhash != null)
//				loaded_in_cache |= (null!=D_Organization_Node.loaded_By_GIDhash.remove(this.getGIDH_or_guess()));
			this.setGIDH(gIDH);
			this.dirty_main = true;
		}
		
//		if (loaded_in_cache) {
//			if (DEBUG) System.out.println("D_Organization:setGID: register");
//			if (this.getGID() != null) {
//				D_Organization_Node.loaded_By_GID.put(this.getGID(), this);
//				if (DEBUG) System.out.println("D_Organization:setGID: register GID="+this.getGID());
//			}
//			if (this.getGIDH_or_guess() != null) {
//				D_Organization_Node.loaded_By_GIDhash.put(this.getGIDH_or_guess(), this);
//				if (DEBUG) System.out.println("D_Organization:setGID: register GIDH="+this.getGIDH_or_guess());
//			}
//		}
//		if (DEBUG) System.out.println("D_Organization:setGID: quit loaded="+loaded_in_cache);
	}
	public void setGID_relink_gross (String gID, String gIDH) {
		//boolean DEBUG = true;
		boolean loaded_in_cache = this.isLoadedInCache();
		if (DEBUG) System.out.println("D_Organization:setGID: start loaded="+loaded_in_cache);
		String oldGID = this.getGID();
		if ( ! Util.equalStrings_null_or_not(oldGID, gID)) {		
			if (DEBUG) System.out.println("D_Organization:setGID: change GID");
			if (oldGID != null)
				loaded_in_cache |= (null!=D_Organization_Node.loaded_By_GID.remove(oldGID));
			this.setGID(gID);
			this.dirty_main = true;
		}
		if ((gID != null) && (gIDH == null)) {
			if (DEBUG) System.out.println("D_Organization:setGID: compute GIDH");
			if (!D_GIDH.isCompactedGID(gID)) {
				gIDH = D_Organization.getOrgGIDHashGuess(gID);
			} else
				// the next would be wrong if this si authoritarian!
				// gIDH = gID;
			if (gIDH == null) Util.printCallPath("D_Organization: setGID:"+gID+" for: "+this);
		}

		if ( ! Util.equalStrings_null_or_not(getGIDH(), gIDH)) {		
			if (DEBUG) System.out.println("D_Organization:setGID: change GIDH");
			if (this.getGIDH() != null)
				loaded_in_cache |= (null!=D_Organization_Node.loaded_By_GIDhash.remove(this.getGIDH_or_guess()));
			this.setGIDH(gIDH);
			this.dirty_main = true;
		}
		
		if (loaded_in_cache) {
			if (DEBUG) System.out.println("D_Organization:setGID: register");
			if (this.getGID() != null) {
				D_Organization_Node.loaded_By_GID.put(this.getGID(), this);
				if (DEBUG) System.out.println("D_Organization:setGID: register GID="+this.getGID());
			}
			String gidh = this.getGIDH_or_guess();
			if (gidh != null) {
				D_Organization_Node.loaded_By_GIDhash.put(gidh, this);
				if (DEBUG) System.out.println("D_Organization:setGID: register GIDH="+gidh);
			}
		}
		if (DEBUG) System.out.println("D_Organization:setGID: quit loaded="+loaded_in_cache);
	}
	
	
	
	public boolean isLoadedInCache() {
		String GIDH;
		if (!D_Organization_Node.loaded_objects.inListProbably(this)) return false;
		long lID = this.getLID_forced();
		if (lID > 0)
			if ( null != D_Organization_Node.loaded_By_LocalID.get(new Long(lID)) ) return true;
		if ((GIDH = this.getGIDH_or_guess()) != null)
			if ( null != D_Organization_Node.loaded_By_GIDhash.get(GIDH) ) return true;
		return false;
	}

	/** Storing */
	public static D_Organization_SaverThread saverThread = new D_Organization_SaverThread();
	public boolean dirty_any() {
		return dirty_main || dirty_params || dirty_locals || dirty_mydata || dirty_preferences;
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * 
	 * This returns asynchronously (without waiting for the storing to happen).
	 */
	public void storeRequest() {
		//Util.printCallPath("Why store?");
		if (!this.dirty_any()) {
			Util.printCallPath("Why store when not dirty?");
			return;
		}
		
		String save_key = this.getGIDH_or_guess();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Organization._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Peer:storeRequest: added to _need_saving_obj");
		} else {
			if (D_Organization_Node.loaded_By_GIDhash.get(save_key) == null) {
				D_Organization_Node.loaded_By_GIDhash.put(save_key, this);
				String gid = this.getGID();
				if (gid != null) D_Organization_Node.loaded_By_GID.put(gid, this);
				Util.printCallPath("Saving unlinked org key:"+save_key+"\n"+this);
			}
			if (DEBUG) System.out.println("D_Organization:storeRequest: GIDH="+save_key);
			D_Organization._need_saving.add(save_key);
		}
		try {
			if (!saverThread.isAlive()) { 
				if (DEBUG) System.out.println("D_Peer:storeRequest:startThread");
				saverThread.start();
			}
			synchronized(saverThread) {saverThread.notify();}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * This function has to be called after the appropriate dirty flags are set:
	 * dirty_main - for elements in the peer table
	 * dirty_instances - ...
	 * dirty_served_orgs - ...
	 * dirty_my_data - ...
	 * dirty_addresses - ...
	 * 
	 * @return This returns synchronously (waiting for the storing to happen), and returns the local ID.
	 */
	public long storeRequest_getID() {
		//if ( this.getLIDstr() == null ) return this.storeSynchronouslyNoException();
		this.storeRequest();
		return this.getLID_forced();
	}
	/**
	 * This can be delayed saving
	 * @return
	 */
	public long storeSynchronouslyNoException() {
		try {
			return storeAct();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
	/**
	 * The next monitor is used for the storeAct, to avoid concurrent modifications of the same object.
	 * Potentially the monitor can be a field in the same object (since saving of different objects
	 * is not considered dangerous, even when they are of the same type)
	 * 
	 * What we do is the equivalent of a synchronized method "storeAct" that we avoid to avoid accidental synchronization
	 * with other methods.
	 */
	final Object monitor = new Object();
	//static final Object monitor = new Object();
	
	/**
	 * use:  this.arrival_date = Util.getCalendar(arrival_time);
	 * sync not sent
	 * 
	 * @throws P2PDDSQLException
	 */
	public long storeAct() throws P2PDDSQLException {
		synchronized(monitor) {
			return _storeAct();
		}
	}
	/**
	 * This is not synchronized
	 * @return
	 * @throws P2PDDSQLException
	 */
	private long _storeAct() throws P2PDDSQLException {
		// final boolean DEBUG = true;
		if (DEBUG) out.println("D_Organization: storeAct: start "+this.getGIDH());
		//D_Peer source_peer = null;
		synchronized(D_Organization.lock_organization_GID_storage ) {
			if (DEBUG) out.println("D_Organization: storeAct: synced "+this.getGIDH());
			long result = -1;
			/*
			//this.arrival_date = Util.getCalendar(arrival_time);
			//arrival_time = Encoder.getGeneralizedTime(arrival_date);
					
			if (this.global_organization_IDhash == null) global_organization_IDhash = getOrgGIDHashFromGID();
			
			String creation_time = Encoder.getGeneralizedTime(params.creation_time);
			String old_date[] = new String[1];
			String organization_ID = D_Organization_D.getLocalOrgIDandDate(global_organization_ID, old_date);
			if ((organization_ID != null) && (old_date != null) && (old_date[0] != null) && (creation_time.compareTo(old_date[0]) <= 0)) {
				if (! Util.equalStrings_null_or_not(creation_time, old_date[0]) || hashConflictCreationDateDropThis()) {
					if (DEBUG) out.println("D_Organization: storeVerified: Will not integrate old ["+creation_time+"]: "+this);
					if (changed != null) changed[0] = false;
					return Util.lval(organization_ID,-1);
				}
			}
			if (changed != null) changed[0] = true;
			*/
			fillLocals();
			
			if (this.dirty_params || dirty_locals || dirty_main || this.dirty_preferences) {
				int filter = 0;
				this.dirty_params = dirty_locals = dirty_main = this.dirty_preferences = false;
				
				if (DEBUG) out.println("D_Organization: storeAct: dirty main & locals & params");

				String field_sign = (signature != null) ? Util.stringSignatureFromByte(signature) : null;
				
				if ((field_sign != null) && (params.certifMethods == net.ddp2p.common.table.organization._GRASSROOT)) {
					field_sign = D_GIDH.d_OrgGrassSign+field_sign;
				}
							
				//String[] fieldNames = table.organization.fields; // Util.trimmed(table.organization.org_list.split(","));
				//if (organization_ID != null) filter = 1;
				if (getLIDstr() != null) filter = 1;
				
				String[] p = new String[net.ddp2p.common.table.organization.FIELDS_NOID + filter];
				p[net.ddp2p.common.table.organization.ORG_COL_GID] = getGID();
				p[net.ddp2p.common.table.organization.ORG_COL_GID_HASH] = getGIDH();
				p[net.ddp2p.common.table.organization.ORG_COL_NAME] = getName();
				p[net.ddp2p.common.table.organization.ORG_COL_NAME_CONS] = D_OrgConcepts.stringFromStringArray(concepts.name_constituent);
				p[net.ddp2p.common.table.organization.ORG_COL_NAME_ORG] = D_OrgConcepts.stringFromStringArray(concepts.name_organization);
				p[net.ddp2p.common.table.organization.ORG_COL_NAME_FORUM] = D_OrgConcepts.stringFromStringArray(concepts.name_forum);
				p[net.ddp2p.common.table.organization.ORG_COL_NAME_MOTION] = D_OrgConcepts.stringFromStringArray(concepts.name_motion);
				p[net.ddp2p.common.table.organization.ORG_COL_NAME_JUST] = D_OrgConcepts.stringFromStringArray(concepts.name_justification);//, table.organization.ORG_TRANS_SEP,null);
				p[net.ddp2p.common.table.organization.ORG_COL_LANG] = D_OrgConcepts.stringFromStringArray(params.languages);//, table.organization.ORG_LANG_SEP,null);
				p[net.ddp2p.common.table.organization.ORG_COL_INSTRUC_REGIS] = params.instructions_registration;
				p[net.ddp2p.common.table.organization.ORG_COL_INSTRUC_MOTION] = params.instructions_new_motions;
				p[net.ddp2p.common.table.organization.ORG_COL_DESCRIPTION] = params.description;
				p[net.ddp2p.common.table.organization.ORG_COL_SCORES] = D_OrgConcepts.stringFromStringArray(params.default_scoring_options);//, table.organization.ORG_SCORE_SEP,null);
				p[net.ddp2p.common.table.organization.ORG_COL_CATEG] = params.category;
				p[net.ddp2p.common.table.organization.ORG_COL_CERTIF_METHODS] = ""+params.certifMethods;
				p[net.ddp2p.common.table.organization.ORG_COL_WEIGHTS_TYPE] = ""+params.mWeightsType;
				p[net.ddp2p.common.table.organization.ORG_COL_WEIGHTS_MAX] = ""+params.mWeightsMax;
				p[net.ddp2p.common.table.organization.ORG_COL_HASH_ALG] = params.hash_org_alg;
				//p[table.organization.ORG_COL_HASH] = (params.hash_org!=null)?Util.byteToHex(params.hash_org, table.organization.ORG_HASH_BYTE_SEP):null;
				p[net.ddp2p.common.table.organization.ORG_COL_CREATION_DATE] = getCreationDate_str();
				p[net.ddp2p.common.table.organization.ORG_COL_CERTIF_DATA] = (params.certificate!=null)?Util.stringSignatureFromByte(params.certificate):null;
				p[net.ddp2p.common.table.organization.ORG_COL_ICON] = (params.icon!=null)?Util.stringSignatureFromByte(params.icon):null;
				p[net.ddp2p.common.table.organization.ORG_COL_CREATOR_ID] = creator_ID;
				p[net.ddp2p.common.table.organization.ORG_COL_SIGN] = field_sign;
				p[net.ddp2p.common.table.organization.ORG_COL_SIGN_INITIATOR] = Util.stringSignatureFromByte(this.signature_initiator);
				p[net.ddp2p.common.table.organization.ORG_COL_ARRIVAL] = getArrivalDate_str(); //Util.getGeneralizedTime(); s
				p[net.ddp2p.common.table.organization.ORG_COL_RESET_DATE] = (reset_date!=null)?Encoder.getGeneralizedTime(reset_date):null; //Util.getGeneralizedTime();
				p[net.ddp2p.common.table.organization.ORG_COL_BLOCK] = Util.bool2StringInt(blocked);
				p[net.ddp2p.common.table.organization.ORG_COL_REQUEST] = Util.bool2StringInt(requested);
				p[net.ddp2p.common.table.organization.ORG_COL_BROADCASTED] = Util.bool2StringInt(broadcasted);
				p[net.ddp2p.common.table.organization.ORG_COL_BROADCAST_RULE] = Util.bool2StringInt(broadcast_rule);
				p[net.ddp2p.common.table.organization.ORG_COL_NEIGHBORHOODS_RULE] = Util.bool2StringInt(neighborhoods_rule);
				p[net.ddp2p.common.table.organization.ORG_COL_PREAPPROVED] = _preapproved;
				p[net.ddp2p.common.table.organization.ORG_COL_PREFERENCES_DATE] = this._preferences_date;
				p[net.ddp2p.common.table.organization.ORG_COL_FIRST_PROVIDER_PEER] = this.first_provider_peer;
				p[net.ddp2p.common.table.organization.ORG_COL_TEMPORARY] = Util.bool2StringInt(this.temporary);
				p[net.ddp2p.common.table.organization.ORG_COL_HIDDEN] = Util.bool2StringInt(this.hidden);
				if (this.getSpecificRequest() != null) p[net.ddp2p.common.table.organization.ORG_COL_SPECIFIC] = Util.stringSignatureFromByte(this.getSpecificRequest().encode());

				if (DEBUG || DD.DEBUG_TMP_GIDH_MANAGEMENT) out.println("D_Organization: _storeAct: saving spec req: "+this.getSpecificRequest());
				
				//String orgID;
				if (filter == 0) {//organization_ID == null) {
					setLID_AndLink(result = Application.getDB().insertNoSync(net.ddp2p.common.table.organization.TNAME, net.ddp2p.common.table.organization.fields_noID, p, DEBUG));//changed = true;
					if (DEBUG) out.println("D_Organization: storeVerified: Inserted: "+this);
				} else {
					//orgID = Util.getString(p_data.get(0).get(table.organization.ORG_COL_ID));
					p[net.ddp2p.common.table.organization.ORG_COL_ID] = getLIDstr();
					//if (filter > 0) p[table.organization.FIELDS] = organization_ID;
					Application.getDB().updateNoSync(net.ddp2p.common.table.organization.TNAME,  net.ddp2p.common.table.organization.fields_noID, new String[]{net.ddp2p.common.table.organization.organization_ID}, p, DEBUG);//changed = true;
					result = Util.lval(getLIDstr(), -1);
					if (DEBUG) out.println("\nD_Organization: storeVerified: Updated: "+this);
				}
				//if (sync) 
				Application.getDB().sync(new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.organization.TNAME)));
				//organization_ID = orgID;
				if (params != null) D_Organization.storeOrgParams(getLIDstr(), params.orgParam, filter == 0);
				this.setLID(result);
			}
			
			if (this.dirty_mydata) {
				if (DEBUG) out.println("D_Organization: storeAct: dirty my data");
				this.dirty_mydata = false;
				String p[];
				if (mydata.row > 0) {
					p = new String[net.ddp2p.common.table.my_organization_data.FIELDS_NB];
				} else {
					p = new String[net.ddp2p.common.table.my_organization_data.FIELDS_NB_NOID];
				}
				p[net.ddp2p.common.table.my_organization_data.COL_ORGANIZATION_LID] = this.getLIDstr_forced();
				p[net.ddp2p.common.table.my_organization_data.COL_CATEGORY] = mydata.category;
				p[net.ddp2p.common.table.my_organization_data.COL_CREATOR] = mydata.creator;
				p[net.ddp2p.common.table.my_organization_data.COL_NAME] = mydata.name;
				if (mydata.row <= 0) {
					mydata.row = Application.getDB().insertNoSync(net.ddp2p.common.table.my_organization_data.TNAME, net.ddp2p.common.table.my_organization_data.fields_noID, p, DEBUG);//changed = true;
					if (DEBUG) out.println("D_Organization: storeVerified: Inserted: "+mydata);
				} else {
					p[net.ddp2p.common.table.my_organization_data.COL_ROW] = mydata.row + "";
					Application.getDB().updateNoSync(
							net.ddp2p.common.table.my_organization_data.TNAME,
							net.ddp2p.common.table.my_organization_data.fields_noID,
							new String[]{net.ddp2p.common.table.my_organization_data.row},
							p, DEBUG);//changed = true;
					if(DEBUG) out.println("\nD_Organization: storeVerified: Updated: "+mydata);
				}
			}
			
			return 
					result;
		}
	}
	private void fillLocals() {
		if ((creator_ID == null) && (this.params.creator_global_ID != null)) {
			////creator_ID = D_Peer.storePeerAndGetOrInsertTemporaryLocalForPeerGID(null, params.creator_global_ID,creator,arrival_time);
			//D_Peer _creator = D_Peer.getPeerByGID(this.params.creator_global_ID, true, true, source_peer);
			//if (creator != null) _creator.loadRemote(creator);
			if (creator == null) {
				creator = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, null, true, true, true, null);
				creator.storeRequest();
				creator.releaseReference();
			}
			creator_ID = creator.getLIDstr_keep_force();
			System.out.println("D_Organization: fillLocals: should not be here: unknown source peer");
		}
	}
	/**
	 * Gets the components of field_extra, ordered by GID (needed because encoder is used in comparing orgs with same date)
	 * including temporary
	 * @param local_id
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static D_OrgParam[]  getOrgParams(String local_id) throws P2PDDSQLException {
		if (DEBUG) out.println("\n****************\nOrgHandling:getOrgParams: start organization orgID="+local_id);
		D_OrgParam[] result=null;
		String psql = "SELECT "+net.ddp2p.common.table.field_extra.org_field_extra +
				" FROM "+net.ddp2p.common.table.field_extra.TNAME+
				" WHERE "+net.ddp2p.common.table.field_extra.organization_ID+" = ?" +
						// " AND ("+table.field_extra.tmp+" IS NULL OR "+table.field_extra.tmp+"='0' )" +
				" ORDER BY "+net.ddp2p.common.table.field_extra.field_extra_ID+";";
		ArrayList<ArrayList<Object>>p_org = Application.getDB().select(psql, new String[]{local_id}, DEBUG);
		if (p_org.size() > 0) {
			result = new D_OrgParam[p_org.size()];
			for (int p_i = 0; p_i < p_org.size(); p_i ++) {
				D_OrgParam op = D_OrgParam.getOrgParam(p_org.get(p_i));
				result[p_i] = op;
				if (DEBUG) System.out.println("OrgHandling:signOrgData: encoded orgParam="+op);
			}
		}
		if (DEBUG) out.println("OrgHandling:getOrgParams: exit\n****************\n");
		return result;
	}
	/**
	 * 
	 * @return
	 */
	public D_OrgParam[] getNeighborhoodSubdivisionsDefault() {
		D_OrgParam[] gop = getOrgParams();
		if (gop == null) return new D_OrgParam[0];
		int nb = getNumberOfNeighborhoods(gop);
		
		// first fill the result array with all neighborhoods without an order;
		D_OrgParam[] result = new D_OrgParam[nb];
		int j = 0;
		for (int i = 0; i < gop.length; i++) {
			if (gop[i].partNeigh > 0)
				result[j ++] = gop[i];
		}
		
		Arrays.sort(result, new Comparator<D_OrgParam>() {

			@Override
			public int compare(D_OrgParam o1, D_OrgParam o2) {
				return o1.partNeigh - o2.partNeigh;
			}
			
		});
		
		return result;
	}
	public int getNumberOfNeighborhoods(D_OrgParam[] gop) {
		if (gop == null) return 0;
		int result = 0;
		for (int i = 0; i < gop.length; i ++) {
			if (gop[i].partNeigh > 0) result ++;
		}
		return result;
	}

	/**
	 * This is loading them.
	 * 
	 * @param orgLID local organization ID
	 * @param orgParam An OrgParam[] array
	 * @param just_created To avoid redundant search for IDs when known to not be there (e.g, when org first inserted)
	 * @throws P2PDDSQLException
	 */
	public static void storeOrgParams(String orgLID, D_OrgParam[] orgParam, boolean just_created) throws P2PDDSQLException{
		// Cannot be deleted and rewritten since we would lose references to the IDs from const values
		if (! just_created) {
			Application.getDB().updateNoSync(net.ddp2p.common.table.field_extra.TNAME,
					new String[]{net.ddp2p.common.table.field_extra.tmp},
					new String[]{net.ddp2p.common.table.organization.organization_ID},
					new String[]{"1", orgLID}, DEBUG);
		}		
		long _orgLID = Util.lval(orgLID, -1);
		
		if (orgParam != null) {
			String[] fieldNames_extra = net.ddp2p.common.table.field_extra.org_field_extra_insert_list;
			//String[] fieldNames_extra_update = Util.trimmed(table.field_extra.org_field_extra_insert.split(","));
			for (int k = 0; k < orgParam.length; k ++) {
				D_OrgParam op = orgParam[k];
				if (DEBUG) out.println("OrgHandling: updateOrg: Inserting/Updating field: "+op);
				String fe;
				if (op.field_LID <= 0 && ! just_created) // happen on repeated import with DD_SK
					fe = D_Organization.getFieldExtraLID_FromTable(op.global_field_extra_ID, _orgLID);
				else fe = Util.getStringID(op.field_LID);
				
				String[] p_extra;
				if (fe == null) p_extra = new String[fieldNames_extra.length];
				else p_extra = new String[fieldNames_extra.length+1];
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_LABEL] = op.label;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_LABEL_L] = op.label_lang;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_LATER] = Util.bool2StringInt(op.can_be_provided_later);
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_CERT] = Util.bool2StringInt(op.certificated);
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_SIZE] = op.entry_size+"";
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_NEIGH] = op.partNeigh+"";
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_REQ] = Util.bool2StringInt(op.required);
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_DEFAULT] = op.default_value;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_DEFAULT_L] = op.default_value_lang;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_LIST_VAL] = Util.concat(op.list_of_values, net.ddp2p.common.table.organization.ORG_VAL_SEP, null);
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_LIST_VAL_L] = op.list_of_values_lang;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_TIP] = op.tip;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_TIP_L] = op.tip_lang;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_OID] = Util.BNOID2String(op.oid);
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_ORG_ID] = orgLID;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_GID] = op.global_field_extra_ID;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_VERSION] = op.version;
				p_extra[net.ddp2p.common.table.field_extra.OPARAM_TMP] = op.tmp;
				if (fe == null) {
					Application.getDB().insertNoSync(net.ddp2p.common.table.field_extra.TNAME, fieldNames_extra, p_extra, DEBUG);
				} else {
					p_extra[net.ddp2p.common.table.field_extra.OPARAM_EXTRA_FIELD_ID] = fe;
					Application.getDB().updateNoSync(net.ddp2p.common.table.field_extra.TNAME, fieldNames_extra,
							new String[]{net.ddp2p.common.table.field_extra.field_extra_ID},
							p_extra, DEBUG);
				}
			}
		}
		if (! just_created) {
			Application.getDB().deleteNoSync(net.ddp2p.common.table.field_extra.TNAME,
				new String[]{net.ddp2p.common.table.field_extra.tmp, net.ddp2p.common.table.organization.organization_ID},
				new String[]{"1",orgLID}, DEBUG);
		}
	}
	
	public static String getGIDbyLIDstr(String p_organization_ID) {
		long lID = Util.lval(p_organization_ID);
		D_Organization org = D_Organization.getOrgByLID_NoKeep(lID, true);
		if (org == null) return null;
		return org.getGID();
	}
	public static String getGIDbyLID(Long p_organization_ID) {
		if (DEBUG) System.out.println("D_Organization: getGIDbyLID: lID="+p_organization_ID);
		D_Organization org = D_Organization.getOrgByLID_NoKeep(p_organization_ID, true);
		if (org == null) {
			if (_DEBUG) System.out.println("D_Organization: getGIDbyLID: no org for LID="+p_organization_ID);
			return null;
		}
		String result = org.getGID();
		if (result == null)
			if (_DEBUG) System.out.println("D_Organization: getGIDbyLID: no orgGID for LID="+p_organization_ID+"\n in "+org);
		return result;
	}
	public static String getGIDHbyLID(Long p_organization_ID) {
		if (_DEBUG) System.out.println("D_Organization: getGIDHbyLID: lID="+p_organization_ID);
		D_Organization org = D_Organization.getOrgByLID_NoKeep(p_organization_ID, true);
		if (org == null) {
			if (_DEBUG) System.out.println("D_Organization: getGIDHbyLID: no org for LID="+p_organization_ID);
			return null;
		}
		String result = org.getGIDH_or_guess();
		if (result == null)
			if (_DEBUG) System.out.println("D_Organization: getGIDHbyLID: no orgGIDH for LID="+p_organization_ID+"\n in "+org);
		return result;
	}
	/**
	 * returns -1 id not existent
	 * @param p_global_organization_IDH
	 * @return
	 */
	public static long getLIDbyGIDH(String p_global_organization_IDH) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, p_global_organization_IDH, true, false);
		if (org == null) return -1;
		return org.getLID_forced();
	}
	/**
	 * returns -1 if inexistent
	 * @param p_global_organization_ID
	 * @return
	 */
	public static long getLIDbyGID(String p_global_organization_ID) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(p_global_organization_ID, null, true, false);
		if (org == null) {
			if (DEBUG) System.out.println("D_Organization: getLIDbyGID: not found GID: "+p_global_organization_ID);
			return -1;
		}
		return org.getLID_forced();
	}
	/**
	 * returns -1 if inexistent
	 * @param p_global_organization_ID
	 * @return
	 */
	public static String getLIDstrByGID_(String p_global_organization_ID) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(p_global_organization_ID, null, true, false);
		if (org == null) return null;
		return org.getLIDstr_forced();
	}
	/**
	 * Synonym for getLocalOrgID
	 * @param orgGID
	 * @param orgGIDH
	 * @return
	 */
	public static long getLIDbyGIDorGIDH(String orgGID, String orgGIDH) {
		return getLocalOrgID (orgGID, orgGIDH);
	}
	/**
	 * Synonym for getLIDbyGIDorGIDH
	 * @param p_global_organization_ID
	 * @param orgGID_hash
	 * @return
	 */
	public static long getLocalOrgID(String p_global_organization_ID, String orgGID_hash) {
		if ((p_global_organization_ID != null) && (orgGID_hash == null)) {
			orgGID_hash = D_Organization.getOrgGIDHashGuess(p_global_organization_ID);
			//return getLocalOrgID(p_global_organization_ID);
		}
		if (orgGID_hash != null)
			return getLIDbyGIDH(orgGID_hash);
		return -1;
	}
	public static void setTemporary(D_Organization org) {
		org = D_Organization.getOrgByOrg_Keep(org);
		org.setTemporary();
		org.storeRequest();
		org.releaseReference();
	}
	/**
	 * Sets the temporary flag
	 */
	public void setTemporary() {
		this.setTemporary(true);
		//this.setSignature(null);
	}
	public void setTemporary(boolean b) {
		this.temporary = b;
		this.dirty_locals = true;
	}
	/**
	 * If marked temporary, no signature or no GID
	 * @return
	 */
	public boolean isTemporary() {
		return this.temporary || this.getGID() == null || 
				(! this.isGrassroot() && (this.signature == null || this.signature.length == 0));
	}
	/**
	 * Only returns true if known to be grassroot (false for temporary)
	 * @return
	 */
	public boolean isGrassroot() {
		String GIDH = this.getGIDH_or_guess();
		if (GIDH == null) return false;
		if (GIDH.startsWith(D_GIDH.d_OrgGrassSign)) return true; // grass root
		if (GIDH.startsWith(D_GIDH.d_OrgAuthSign)) return false;
		return this.getCertifyingMethod() == net.ddp2p.common.table.organization._GRASSROOT;
	}
	/**
	 * Only returns true if known to be authoritarian (false for temporary)
	 * @return
	 */
	public boolean isAuthoritarian() {
		String GIDH = this.getGIDH_or_guess();
		if (GIDH == null) return false;
		if (GIDH.startsWith(D_GIDH.d_OrgGrassSign)) return false; // grass root
		if (GIDH.startsWith(D_GIDH.d_OrgAuthSign)) return true;
		return this.getCertifyingMethod() == net.ddp2p.common.table.organization._AUTHORITARIAN;
	}
	public boolean isExpression() {
		return this.getCertifyingMethod() == net.ddp2p.common.table.organization._EXPRESSION;
	}

	public void setHidden() {
		this.hidden = true;
		setPreferencesDate();
		this.dirty_preferences = true;
	}
	public boolean getHidden() {
		return this.hidden;
	}
	public static long insertTemporaryGID(String p_global_organization_ID,
			String orgGID_hash, boolean default_blocked_org, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, orgGID_hash, true, true, __peer);
		return org.getLID_forced(); 
	}
	public static D_Organization insertTemporaryGID_org(String p_global_organization_ID, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, null, true, true, __peer);
		return org; 
	}
	public static String insertTemporaryGID(String p_global_organization_ID, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, null, true, true, __peer);
		return org.getLIDstr_forced(); 
	}
	public static long insertTemporaryGID_long(String p_global_organization_ID, D_Peer __peer) {
		D_Organization org;
		org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, null, true, true, __peer);
		return org.getLID_forced(); 
	}
	public static long insertTemporaryGID(String p_global_organization_ID,
			String orgGID_hash, boolean blocked2, String name2, D_Peer __peer) {
		D_Organization org = D_Organization.insertTemporaryGID_org(p_global_organization_ID, orgGID_hash, __peer, blocked2, name2);
		return org.getLID_forced();
	}
	/**
	 * 
	 * @param p_global_organization_ID
	 * @param orgGID_hash
	 * @param __peer
	 * @param blocked2
	 * @param name2
	 * @return
	 */
	public static D_Organization insertTemporaryGID_org(
			String p_global_organization_ID, String orgGID_hash, D_Peer __peer,
			boolean blocked2, String name2) {
		D_Organization org;
		//org = D_Organization.getOrgByGID_or_GIDhash_StoreIfNew_NoKeep(p_global_organization_ID, orgGID_hash, true, true, __peer);
		org = D_Organization.getOrgByGID_or_GIDhash(p_global_organization_ID, orgGID_hash, true, true, true, __peer);
		if ((name2 != null) || (blocked2 != org.blocked)) {
			//org = D_Organization.getOrgByOrg_Keep(org);
			org.setName(name2);
			org.setBlocked(blocked2);
		}
		if (org.dirty_any()) org.storeRequest();
		org.releaseReference();
		return org;
	}
	/**
	 * Used to check if an incoming org is already known or needs to be adopted.
	 * @param gIDhash
	 * @param DBG
	 * @return
	 *  0 for absent,
	 *  1 for present&signed,
	 *  -1 for temporary
	 * @throws P2PDDSQLException
	 */
	public static int isOrgAvailableForGIDH(String gIDhash, boolean DBG) throws P2PDDSQLException {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, gIDhash, true, false);
		if (org == null) return 0;
		if (org.isTemporary()) return -1;
		return 1;
	}
	public static int isOrgAvailableForLID(long _organizationID, boolean DBG) {
		D_Organization org = D_Organization.getOrgByLID_NoKeep(_organizationID, true);
		if (org == null) {
			if(DEBUG||DBG) System.out.println("D_Org:isIDavailable: 0");
			return 0;
		}
		if (org.isTemporary()) {
			if(DEBUG||DBG) System.out.println("D_Org:isIDavailable: -1");
			return -1;
		}
		if (DEBUG || DBG) System.out.println("D_Org:isIDavailable: 1:"+org.signature);
		return 1;
	}
	/**
	 * Creates choices by taking a set of String alternatives and setting their short names to numbers starting from 0
	 * @param opts
	 * @return
	 */
	public static D_MotionChoice[] getDefaultMotionChoices(String[] opts) {
		D_MotionChoice[] result;
		if (opts == null) return null;
		result = new D_MotionChoice[opts.length];
		for (int k = 0; k < result.length; k ++) {
			result[k] = new D_MotionChoice(opts[k], "" + k);
		}
		return result;		
	}
	/**
	 * Returns custom options or in their absence
	 * the default array of strings: __("Endorse"),__("Abstain"),__("Oppose")
	 * with short names: 0, 1, 2, ...
	 * @return
	 */
	public D_MotionChoice[] getDefaultMotionChoices() {
		D_MotionChoice[] result;
		String[] opts = this.params.default_scoring_options;
		if (opts == null) opts = get_DEFAULT_OPTIONS();
		result = getDefaultMotionChoices(opts);
		return result;
	}
	/**
	 * Returns the array of strings: __("Endorse"),__("Abstain"),__("Oppose")
	 * @return
	 */
	public static String[] get_DEFAULT_OPTIONS() {
		String[] DEFAULT_OPTIONS = new String[]{__("Endorse"),__("Abstain"),__("Oppose")};
		return DEFAULT_OPTIONS;
	}
	
	public static byte getASN1Type() {
		return DD.TYPE_ORG_DATA;
	}
	public static D_Organization getOrgFromDecoder(Decoder decoder) {
		D_Organization org;
		try {
			org = new D_Organization().decode(decoder);
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
			return null;
		}
		return org;
	}
	/**
	 * Do I know the organization with this GIDH
	 * I should probably also add the creation date in the test (and RequestData)
	 * @param orgGIDH
	 * @return
	 */
	public static boolean hasGIDH(String orgGIDH) {
		D_Organization o = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, orgGIDH, true, false);
		return o != null; 
	}
	public boolean verifyExtraFieldGIDs() {
		return verifyExtraFieldGIDs(this);
	}
	public static boolean verifyExtraFieldGIDs(D_Organization od) {
		if ((od.params==null) || (od.params.orgParam==null)) return true;
		for (int i = 0; i < od.params.orgParam.length; i ++) {
			D_OrgParam dop = od.params.orgParam[i];
			String eGID = dop.makeGID();
			if (! eGID.equals(dop.global_field_extra_ID)) {
				if (_DEBUG) System.out.println("D_Organization: verifyExtraFieldGIDs: fail to verify field ID: "+dop.label+" for "+dop);
				return false;
			}
		}
		return true;
	}
	/*
	public static String _getOrgGIDandHashForGrassRoot(String org_ID, String currentTime, D_Organization[] org) {
		D_Organization orgData = D_Organization.getOrgByLID(org_ID, true);
		if (orgData == null) return null;
		byte[] h = orgData.getOrgGIDandHashForGrassRoot(currentTime);
		if (h == null) {
			Util.printCallPath("Failure to compute Hash, incomplete data");
			return null;
		}
		String _gid = Util.stringSignatureFromByte(h);
		return D_GIDH.d_OrgGrassSign+_gid;
	}
	public byte[] getOrgGIDandHashForGrassRoot(String currentTime) {
		setCreationData(currentTime);
		if(DEBUG) out.println("OrgHandling:getOrgHash: Prepared org: "+this);
		if(DEBUG) out.println("*****************");
		
		byte[]h = this.hash(DD.APP_ORGID_HASH);
		SK sk_ini = Util.getStoredSK(this.params.creator_global_ID, null);
		if (sk_ini != null) this.signature_initiator = Util.sign(h, sk_ini);
		
		return h;
	}
	*/
	/**
	 * makeGID for grassroot without setting it
	 * @return
	 */
	public String getOrgGIDandHashForGrassRoot() {
		return getOrgGIDandHashForGrassRoot(null);
	}
	/**
	 * Makes the GID for grassroot without setting it
	 * @param verif
	 * @return
	 */
	public String getOrgGIDandHashForGrassRoot(boolean[] verif){
		byte[] hash = this.hash(DD.APP_ORGID_HASH);
		if (verif != null)
			verif[0] =
			! DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING
			|| Util.verifySignByID(hash, this.getCreatorGID(), this.signature_initiator);
		return D_GIDH.d_OrgGrassSign+Util.stringSignatureFromByte(hash);
	}
	public byte[]hash(String _h) {
		return hash(_h, null);
	}
	public byte[]hash(String _h, net.ddp2p.ciphersuits.SK sk_ini) {
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if (sk_ini != null) this.signature_initiator = Util.sign(msg, sk_ini);
		return Util.simple_hash(msg, _h);	
	}
	/**
	 * Gets the hash of an encoder (for debugging) with DD.APP_ORGID_HASH
	 * @param enc
	 * @return
	 */
	public static String hashEnc(Encoder enc) {
		byte[] msg = enc.getBytes();
		return D_GIDH.d_OrgGrassSign+Util.stringSignatureFromByte(Util.simple_hash(msg, DD.APP_ORGID_HASH));
	}
	/**
	 * Signs and sets the current creation date
	 * @param id
	 * @return
	 */
	public static D_Organization readSignSave(long id) {
		D_Organization org = D_Organization.getOrgByLID(id, true, true);
		org.updateExtraGIDs();
		org.setCreationDate(Util.CalendargetInstance());
		org.setSignature(org.sign());
		
		org.storeRequest();
		org.releaseReference();
		return org;
	}
	/**
	 * 
	 * @param id
	 * @param creationTime
	 * @return
	 */
	public static D_Organization readSignSave(String id, String creationTime) {
		D_Organization org = D_Organization.getOrgByLID(id, true, true);
		org.updateExtraGIDs();
		
		if (creationTime == null) org.setCreationDate(Util.CalendargetInstance());
		else org.setCreationDate(creationTime);
		
		org.setSignature(org.sign());
		
		org.storeRequest();
		org.releaseReference();
		return org;
	}
	/**
	 * This assigns the initiator signature before returning.
	 * @return
	 */
	public byte[] signIni() {
		return signIni(true);
	}
	/**
	 * Optimized for signing both with a creator and an organization key
	 * @return
	 */
	public byte[]sign() {
		SK sk = getSK(); //Util.getStoredSK(global_organization_ID, global_organization_IDhash);
		SK sk_ini = getCreatorSK();
		return sign(sk, sk_ini);
	}
	/**
	 * sk of the organization
	 * @param sk
	 * @return
	 */
	/*
	public byte[]sign(ciphersuits.SK sk) {
		return sign(sk, null);
	}
	*/
	/**
	 * Will compute the signature using the provided secret key
	 * @param sk
	 * @return
	 * @throws P2PDDSQLException
	 *
	 */
	/*
	public byte[] signAndGetOrgSignature(ciphersuits.SK sk) {
		if(DEBUG) out.println("OrgHandling:getOrgSignature: Prepared org: "+this);
		if(DEBUG) out.println("*****************");
		SK sk_ini = this.getCreatorSK(); //Util.getStoredSK(this.params.creator_global_ID, null);
		//orgData.signature_initiator = Util.sign(h, sk_ini);
		return this.sign(sk, sk_ini);
	}
	*/
	/**
	 * sign data with Org (for authoritarian), and with initiator key 
	 * Stores signature_initiator, but does not store the signature with organization key.
	 * @param sk (must be non-null) else call sign_ini()
	 * @param sk_ini
	 * @return
	 */
	public byte[]sign(net.ddp2p.ciphersuits.SK sk, net.ddp2p.ciphersuits.SK sk_ini){
		return sign(sk, sk_ini, true);
	}
	public byte[]sign(net.ddp2p.ciphersuits.SK sk, net.ddp2p.ciphersuits.SK sk_ini, boolean set_dirty){
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(DEBUG) System.out.println("D_Organization:sign:");
		if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
		if(DEBUG) System.out.println("sk_ini=#"+sk_ini);
		if (sk_ini != null){
			this.signature_initiator = Util.sign(msg, sk_ini);
			if (set_dirty) this.dirty_main = true;
			if(DEBUG) System.out.println("sgn_ini=#"+signature_initiator.length+" hash(sgn_ini)="+Util.getGID_as_Hash(signature_initiator));
		}else{
			this.signature_initiator = null;			
		}
		if(DEBUG) System.out.println("sk=#"+sk);
		if (sk != null) {
			byte[] sgn = Util.sign(msg, sk);	
			if (set_dirty) this.dirty_main = true;
			if(DEBUG) System.out.println("sgn=#"+sgn.length+" hash(sgn)="+Util.getGID_as_Hash(sgn));
			return sgn;
		} else
			return null;
	}
	/**
	 * 				organization.setSignatureInitiator(organization.getSignatureInitiator());
	 * Optimized for signing only initiator (not caring about authoritarian org signature)
	 * @param set_dirty
	 * @return
	 */
	private byte[] signIni(boolean set_dirty) {
		if (this.params.creator_global_ID == null) return null;
		// reusing, but does not controll dirty 
		sign(null, getCreatorSK(), false);
		return this.signature_initiator; // avoid recalling getSignatureInitiator(); since this is recursive
		
		/*
		SK sk_ini = getCreatorSK();
		if (sk_ini != null) {
			byte[] msg = this.getSignableEncoder().getBytes();
			this.signature_initiator = Util.sign(msg, sk_ini);
			if (set_dirty) this.dirty_main = true;
		}
		return this.signature_initiator;
		*/
	}
	/**
	 * Sets the signature (which is a GIDH for a grassroot)
	 * organization.hash(DD.APP_ORGID_HASH)
	 * @param sgn
	 */
	public void setSignature(byte[] sgn) {
		this.signature = sgn;
		this.dirty_main = true;
	}
	public byte[] getSignatureInitiator() {
		if (this.signature_initiator == null) signIni();
		return this.signature_initiator;
	}
	public void setSignatureInitiator(byte[] signatureInitiator) {
		this.signature_initiator = signatureInitiator;
		this.dirty_main = true;
	}
	public boolean verifySignInitiator() {
		return verifySignInitiator(null);
	}
	/**
	 * If the message encoded is already available
	 * @param msg
	 * @return
	 */
	public boolean verifySignInitiator(byte[] msg) {
		if (msg == null) {
			this.loadGlobals();
			Encoder enc = getSignableEncoder();
			msg = enc.getBytes();
		}
		PK creator_pk = Cipher.getPK(this.params.creator_global_ID);
		if((creator_pk!=null) && !Util.verifySign(msg, creator_pk, this.signature_initiator)){
			if(DEBUG) System.out.println("D_Organization:Failed Creator verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			if(DEBUG) System.out.println("sgn=#"+((signature_initiator==null)?"null":signature_initiator)+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			if(DEBUG) System.out.println("pk=#"+creator_pk);
			return false;
		}else{
			if(DEBUG) System.out.println("D_Organization:Success Creator verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			if(DEBUG) System.out.println("sgn=#"+((signature_initiator==null)?"null":signature_initiator)+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			if(DEBUG) System.out.println("pk=#"+creator_pk);
		}
		return true;
	}
	public boolean isAnonymous() {
		return this.signature_initiator == null;
	}
	/**
	 * Checks both the org sign and (if creator claimed) creator sign
	 * @param sign
	 * @return
	 */
	public boolean verifySignAuthoritarian() {
		if(DEBUG) System.out.println("OrgData:verifySign: KEY=="+getGID());
		if(DEBUG) System.out.println("OrgData:verifySign: sign="+Util.byteToHex(signature, ":"));
		this.loadGlobals();
		Encoder enc = getSignableEncoder();
		byte[] msg = enc.getBytes();
		if(DEBUG) System.out.println("OrgData:verifySign: msg="+Util.byteToHex(msg, ":"));
		
		if ((! this.isAnonymous()) && !this.verifySignInitiator(msg)) return false;
		
		PK org_pk = Cipher.getPK(getGID());
		boolean result = Util.verifySign(msg, org_pk, signature);
		if (! result) {
			if(DEBUG) System.out.println("D_Organization:Failed Org verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			//if(DEBUG) System.out.println("sgn=#"+signature_initiator.length+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			//if(DEBUG) System.out.println("pk=#"+creator_pk);
			return false;
		}else{
			if(DEBUG) System.out.println("D_Organization:Success Org verification:");
			if(DEBUG) System.out.println("msg=#"+msg.length+" hash(msg)="+Util.getGID_as_Hash(msg));
			//if(DEBUG) System.out.println("sgn=#"+signature_initiator.length+" hash(sgn)="+Util.getGID_as_Hash(signature_initiator));
			//if(DEBUG) System.out.println("pk=#"+creator_pk);
		}
		return result;
	}
	/**
	 * @return
	 */
	public boolean verifySignature() {
		boolean verified = true;
		//verify signature!
		if (params == null) {
			if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: null param for="+this);
			return false;
		}
		if (params.certifMethods == net.ddp2p.common.table.organization._GRASSROOT) {
			if (! this.verifyExtraFieldGIDs()) {
				if(DEBUG) out.println("D_Organization:verifySignature: grassroot extras failed");
				verified = false;
			} else {
				//byte[] hash = hash(DD.APP_ORGID_HASH);
				boolean[]verif = new boolean[]{false};
				String tmpGID = this.getOrgGIDandHashForGrassRoot(verif);
				//if(!Util.equalBytes(hash, getHashFromGrassrootGID(global_organization_ID))) {
				if (! verif[0] || (tmpGID == null) || (getGID() == null) || (tmpGID.compareTo(getGID())!=0)) {
					verified = false;
					if (_DEBUG) out.println("D_Organization:verifySignatureAllTypes: recomp hash="+tmpGID);
					if (_DEBUG) out.println("D_Organization:verifySignatureAllTypes:      IDhash="+getGID());
					if (_DEBUG) out.println("D_Organization:verifySignatureAllTypes: exit grassroot signature verification failed:\n"+this);
					if (_DEBUG) Util.printCallPath("");
				}
			}
			if (verified && (! isAnonymous()) && !this.verifySignInitiator()) {
				verified = false;
				if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes: exit grassroot signature ini verification failed");
			}
			if (! verified) {
				return false;
			}
		} else if(params.certifMethods == net.ddp2p.common.table.organization._AUTHORITARIAN) {
			if ((signature == null) || (signature.length == 0) || ! verifySignAuthoritarian()){
				if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes exit authoritarian signature verification failed");
				return false;
			}
		} else {
			if(_DEBUG) out.println("D_Organization:verifySignatureAllTypes exit unknown type signature verification failed");
			return false;
		}
		return true;
	}

	public PK getPK() {
		if (this.getCertifyingMethod() != net.ddp2p.common.table.organization._AUTHORITARIAN) return null;
		//if (stored_pk != null) return stored_pk;
		//SK sk = getSK();
		if (keys != null) return keys.getPK();
		PK pk = net.ddp2p.ciphersuits.Cipher.getPK(getGID());
		//stored_pk = pk;
		keys = Cipher.getCipher(pk);
		return pk;
	}
	/**
	 * Gets the SK from cache or DB.
	 * Assumes cache loaded (should be loaded when importing keys)!
	 * @return
	 */
	public SK getSK() {
		if (this.getCertifyingMethod() != net.ddp2p.common.table.organization._AUTHORITARIAN) return null;
		SK sk = null;
		PK pk = null;
		String gID = this.getGID();
		if (this.getGID() == null) {
			this.loadGlobals();
			gID = this.getGID();
		}
		if (gID == null) return null;
		if (keys != null) {
			sk = keys.getSK();
			pk = keys.getPK();
		}
		if (sk != null) return sk;
		/*
		if (keys != null) {
			//System.out.println("D_Peer:getSK: has to load SK keys when importing keys");
			//Util.printCallPath(""+this);
			return null; // this under assumption no sk if keys seen
		}
		*/
		
		sk = Util.getStoredSK(gID, this.getGIDH_or_guess());
		if (sk == null) return null;
		keys = Cipher.getCipher(sk, pk);
		//if (keys == null) return sk;
		return sk;
	}
	public void initSK() {
		if (this.getCertifyingMethod() != net.ddp2p.common.table.organization._AUTHORITARIAN) return;
		String gID = this.getGID();
		SK sk = Util.getStoredSK(gID, this.getGIDH_or_guess());
		PK pk = null;
		if (sk == null) return;
		keys = Cipher.getCipher(sk, pk);
	}
	public void setKeys(Cipher keys2) {
		this.keys = keys2;
	}
	public SK getCreatorSK() {
		SK sk_ini = null;
		if (DEBUG) System.out.println("D_Organization:sign:*******************");
		if (DEBUG) System.out.println("D_Organization:sign:creatorID="+creator_ID+
				" GID="+params.creator_global_ID+" peer="+creator);
		if (creator != null) {
			sk_ini = creator.getSK(); //Util.getStoredSK(creator.component_basic_data.globalID, creator.component_basic_data.globalIDhash);
			if (sk_ini == null)
				Util.printCallPath("Why!! "+this);
			if ( ! Util.equalStrings_null_or_not(creator.component_basic_data.globalID, params.creator_global_ID))
				System.out.println("D_Organization:readSignSave: diff GIDs:!!!");
		} else
			if (params.creator_global_ID != null) {
				this.creator = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, null, true, false, false, null);
				if (this.creator == null) return null;
				//SK sk;
				sk_ini = this.creator.getSK();
				if (sk_ini == null)
					sk_ini = Util.getStoredSK(this.params.creator_global_ID, null);
				//sk_ini = Util.getStoredSK(params.creator_global_ID, null);
				if (sk_ini == null)
					Util.printCallPath("Why!!");				
			}
		return sk_ini;
	}
	
	public boolean fillLocals(D_Peer __peer, RequestData _new_rq, boolean tempPeer, String arrival_time) {
		if(DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING &&
				((this.params==null)
						||((this.params.creator_global_ID==null)
								&&(this.creator_ID ==  null)))) {
			if(DEBUG)Util.printCallPath("cannot store org with no peerGID (while enforcing INITIATOR)");
			return false;
		}
		
		if((this.params.creator_global_ID!=null)&&(this.creator_ID == null)) {
			creator_ID = D_Peer.getLocalPeerIDforGID(this.params.creator_global_ID);
			
			if(tempPeer && (creator_ID == null))  {
				//creator_ID = D_Peer.storePeerAndGetOrInsertTemporaryLocalForPeerGID(__peer, this.params.creator_global_ID, creator, arrival_time);
				
				//__peer.setArrivalDate(null, arrival_time);
				__peer = D_Peer.storeReceived(__peer, !DD.BLOCK_NEW_ARRIVING_PEERS_CONTACTING_ME, false, arrival_time);
				if (__peer == null) return false;
				
				D_Peer _creator = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, null, true, true, false, __peer);
				if (creator != null) _creator.loadRemote(creator, null, _new_rq);
				creator_ID = _creator.getLIDstr_keep_force();

				String creaGID_hash = D_Peer.getGIDHashFromGID(this.params.creator_global_ID);
				if (_new_rq != null) _new_rq.peers.put(creaGID_hash, DD.EMPTYDATE);
				//creator_ID = Util.getStringID(D_PeerAddress.insertTemporaryGID(this.params.creator_global_ID, consGID_hash));
			}
			if(DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING && (creator_ID == null)) return false;
		}
		
		return true;
	}
	/**
	 * Called on a new arriving org with same creation date as old, to decide whether it is newer based on hash.
	 * Store the one with smaller hash.
	 * @param old_org
	 * @return
	 */
	private boolean hashConflictCreationDateDropThis(D_Organization old_org) {
		D_Peer o_creator = this.creator;
		this.creator = null;
		String this_hash=new String(this.getEntityEncoder().getBytes());
		creator = o_creator;
//		D_Organization dorg;
//		try {
//			dorg = D_Organization.getOrgByGID_or_GIDhash_NoCreate(this.global_organization_ID, this.global_organization_IDhash, true, false); // no creator
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false; // old not valid!
//		}
		String old = new String(old_org.getEntityEncoder().getBytes());
		if (old.compareTo(this_hash) >= 0) return true;
		return false;
	}
	public static long storeRemote(D_Organization oRG, boolean[] _changed,
			RequestData __sol_rq, RequestData __new_rq, D_Peer provider) {
		//boolean DEBUG = true;
		if (DEBUG) out.println("D_Organization: storeRemote: start");
		if (DEBUG) out.println("D_Organization: storeRemote: received="+oRG);
/*		
		if (oRG.getLID() == 0) { oRG.setLID(null);}
		boolean locals = oRG.fillLocals(provider, __new_rq, true, Util.getGeneralizedTime());
		if ( ! locals )//return -1;
			if (_DEBUG) out.println("D_Organization: store: locals failed");
*/
		if ( //( ! locals ) || ( oRG.params == null ) || 
				! oRG.verifySignature() ) {
			if ( ( oRG.signature != null ) || (DD.ANONYMOUS_ORG_ENFORCED_AT_HANDLING && (oRG.signature_initiator != null)))
				if (_DEBUG) out.println("D_Organization: storeRemote: exit signature verification failed for:" + oRG);
			if (_changed != null) _changed[0] = false;
			//long local_org_ID = D_Organization.getLocalOrgID(global_organization_ID);
			//return local_org_ID;
			return  D_Organization.getLIDbyGID(oRG.getGID());// -1;//oRG.getLID();
		}
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash(oRG.getGID(), oRG.getGIDH_or_guess(), true, true, true, provider);
		if (DEBUG) out.println("D_Organization: storeRemote: old = "+org);
		if (! org.isTemporary() && org.getLID() <= 0) Util.printCallPath("Why not temporary?"+org);
		if (org.loadRemote(oRG, provider, _changed, __sol_rq, __new_rq)) {
			if (DEBUG) out.println("D_Organization: storeRemote: loaded="+org);
			Application_GUI.inform_arrival(org, provider);
			//org.dirty_main = true;
			if (org.dirty_any()) org.storeRequest_getID();
		} else {
			if (DEBUG) out.println("D_Organization: storeRemote: no load");
		}
		org.getLID_forced();
		org.releaseReference();
		return org.getLID();
	}
	/**
	 * Calls loadRemote which currently also synchronously stores data.
	 * @param oRG
	 * @param provider
	 * @return
	 */
	public static D_Organization storeRemote(D_Organization oRG, D_Peer provider) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash(oRG.getGID(), oRG.getGIDH_or_guess(), true, true, true, provider);
		if (org.loadRemote(oRG, provider, null, null, null)) {
			Application_GUI.inform_arrival(org, provider);
			if (org.dirty_any()) org.storeRequest_getID();
		}
		org.releaseReference();
		return org;
	}
	/**
	 * This loads and also synchronously stores!
	 * @param oRG
	 * @param provider
	 * @param changed
	 * @param _sol_rq
	 * @param _new_rq
	 * @return
	 */
	private boolean loadRemote(D_Organization oRG, D_Peer provider, boolean[] changed, RequestData _sol_rq, RequestData _new_rq) {
		//boolean DEBUG = true;
		if (DEBUG) out.println("D_Organization: loadRemote: Test integrate old: "+this+"\nvs\n incoming: "+oRG);
		
		if (! this.isTemporary() && ! newer(oRG, this)) {
			if (DEBUG) out.println("D_Organization: loadRemote: Will not integrate old ["+oRG.getCreationDate_str()+"]: "+this);
			if (changed != null) changed[0]=false;
			if (this.getLID() <= 0) Util.printCallPath("Why is this not tempory?"+this);
			this.getLID_forced();
			return false;
		}

		if (changed != null) changed[0] = true;

		this.broadcast_rule = oRG.broadcast_rule;
		this.neighborhoods_rule = oRG.neighborhoods_rule;
		
		this.setName(oRG.getName());
		this.signature = oRG.signature;
		this.signature_initiator = oRG.signature_initiator;
		this.concepts = oRG.concepts.getClone();
		
		if (! Util.equalStrings_null_or_not(this.getGID(), oRG.getGID())) {
			this.setGID(oRG.getGID(), null);
			this.setLID(null);
		}
		
		D_OrgParams oldParams = this.params;
		this.params = oRG.params.getClone();
		if (! Util.equalStrings_null_or_not(oldParams.creator_global_ID, oRG.params.creator_global_ID)) {
			//this.creator_ID = oRG.creator_ID;
			String GIDhash = null;//D_Peer.getGIDHashFromGID(this.params.creator_global_ID); 
			D_Peer p = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, GIDhash, true, false, false, null);
			if (p != null) 	this.setCreatorID(p.getLIDstr(), oRG.getCreatorGID());
			else {
				String creaGID_hash = oRG.getCreatorGIDH_orGuess();
				if (_new_rq != null) _new_rq.peers.put(creaGID_hash, DD.EMPTYDATE);

				p = D_Peer.getPeerByGID_or_GIDhash(oRG.params.creator_global_ID, null, true, true, true, provider);
				if (provider != null) p.setProvider(provider.getLIDstr_keep_force());
				p.storeRequest_getID();
				p.releaseReference();
				this.setCreatorID(p.getLIDstr(), oRG.getCreatorGID());
			}
		}
		if (this.params.orgParam != null) {
			Hashtable<String, Long> fLIDs = new Hashtable<String, Long>();
			if (oldParams.orgParam != null)
				for (D_OrgParam _op : oldParams.orgParam) {
					if (_op.global_field_extra_ID != null)
						fLIDs.put(_op.global_field_extra_ID, _op.field_LID);
				}
				
			for (D_OrgParam _p : this.params.orgParam) {
				if (_p.global_field_extra_ID == null) continue;
				Long old_fLID = fLIDs.get(_p.global_field_extra_ID);
				if (old_fLID != null) _p.field_LID = old_fLID;
					
//					long old_fLID = -1;
//					for (D_OrgParam _op : oldParams.orgParam) {
//						if (Util.equalStrings_and_not_null(_p.global_field_extra_ID, _op.global_field_extra_ID)) {
//							old_fLID = _op.field_LID;
//							break;
//						}
//					}
//					if (old_fLID > 0) _p.field_LID = old_fLID;
			}
		}
		if ((creator_ID == null) && (this.params.creator_global_ID != null)) {
			////creator_ID = D_Peer.storePeerAndGetOrInsertTemporaryLocalForPeerGID(null, params.creator_global_ID,creator,arrival_time);
			//D_Peer _creator = D_Peer.getPeerByGID(this.params.creator_global_ID, true, true, source_peer);
			//if (creator != null) _creator.loadRemote(creator);
			if (creator == null) {
				creator = D_Peer.getPeerByGID_or_GIDhash(this.params.creator_global_ID, null, true, true, true, provider);
				creator.storeRequest();
				creator.releaseReference();
			}
			creator_ID = creator.getLIDstr_keep_force();
			if (_DEBUG) System.out.println("D_Organization: loadRemote: fillLocals: why here?"+this);
		}

		this.setArrivalDate();
		dirty_main = true;
		
		if (this.isTemporary() && this.first_provider_peer == null && provider != null) this.first_provider_peer = provider.getLIDstr_keep_force();
		setTemporary(false);
		getLID_forced();
		return true;
	}

	private String getCreatorGIDH_orGuess() {
		return D_Peer.getGIDHashFromGID(this.getCreatorGID());
	}

	/**
	 * called for 
	 * @param a : incoming
	 * @param b : old
	 * @return true if a should be stored (a nonull creation and > b,  or == b but a's hash smaller)
	 */
	private static boolean newer(D_Organization a, D_Organization b) {
		if (a.getCreationDate_str() == null) return false;
		if (b.getCreationDate_str() == null) return true;
		int date_diff = a.getCreationDate_str().compareTo(b.getCreationDate_str());
		if (date_diff > 0) return true;
		if (date_diff < 0) return false;
		return !a.hashConflictCreationDateDropThis(b);
	}

	public boolean readyToSend() {
		if(this.getGID()==null) return false;
		if(this.params.certifMethods == net.ddp2p.common.table.organization._AUTHORITARIAN)
			if((this.signature == null)||(this.signature.length==0)) return false;
		return true;
	}
	/**
	 * Fill locals (lID/blocked/broadcasted/requested/temporary) of this based on its GID
	 */
	public void getLocalIDfromGIDandBlock() {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(this.getGID(), this.getGIDH(), true, false);
		if (org == null) {
			this.blocked = DD.BLOCK_NEW_ARRIVING_ORGS;
			return;
		}
		this.setLID(org.getLIDstr(), org.getLID());
		this.blocked = org.blocked;
		this.broadcasted = org.broadcasted;
		this.requested = org.requested;
		this.temporary = org.temporary;
	}
	/**
	 * called if both parameters are valid
	 * @param liDstr
	 * @param lid
	 */
	public void setLID(String liDstr, long lid) {
		this.organization_ID = liDstr;
		this._organization_ID = lid;
	}

	public static String getLocalOrgID_fromHashIfNotBlocked(String gIDH) {
		D_Organization org = D_Organization.getOrgByGID_or_GIDhash_NoCreate(null, gIDH, true, false);
		if (org == null) return null;
		if (org.blocked) return null;
		return org.getLIDstr_forced();
	}
	/**
	 * Dirty: broadcasted, blocked, requested, preapproved, preferences_date
	 */
	public void storeLocalFlags() {
		this.dirty_locals = true;
		this.storeRequest();
	}
	/**
	 * Is this editable(not ready)?
	 * @param method   grassroot?
	 * @param GID		global ID
	 * @param cID		creator ID
	 * @return
	 */
	public static boolean isEditable(int method, String GID, String cID) {
		if ((method == net.ddp2p.common.table.organization._GRASSROOT) && (GID != null))
			return false;
		
		// AUTHORITARIAN
		
		// AURHORITARIAN TEMPORARY (NO GID)
		if(/*(method==table.organization._GRASSROOT) &&*/ (GID == null))
			return true;
		
		// AUTHORITARIAN with GID
		//String _sql = "SELECT p."+table.peer.name+" FROM "+table.peer.TNAME +" AS p JOIN "+table.key.TNAME+" AS k"+
		//" ON ("+table.peer.global_peer_ID_hash+"=k."+table.key.ID_hash+") WHERE "+table.peer.peer_ID +"=?;";
		//String sql = "SELECT COUNT(*) FROM "+table.key.TNAME+" WHERE "+table.key.ID_hash +"=?;";
		String sql = "SELECT "+net.ddp2p.common.table.key.name+" FROM "+net.ddp2p.common.table.key.TNAME+" WHERE "+net.ddp2p.common.table.key.public_key +"=?;";

		//String cID=Util.getString(org.get(table.organization.ORG_COL_CREATOR_ID));
		if (DD.ORG_CREATOR_REQUIRED) {
			if (cID == null) return true; // Unknown creator? probably just not set => editable
			D_Peer _creator = D_Peer.getPeerByLID_NoKeep(cID, true);
			ArrayList<ArrayList<Object>> a;
			try {
				a = Application.getDB().select(sql, new String[]{_creator.getGID()});
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
				return false;
			}
			if (a.size() > 0) return true; // I have the key => editable
			return false; // I do not have the key => not editable;
		}
		
		String gsql = "SELECT k."+net.ddp2p.common.table.key.name+" FROM "+net.ddp2p.common.table.key.TNAME+" AS k "+
		" WHERE "+net.ddp2p.common.table.key.public_key +"=?;";
		ArrayList<ArrayList<Object>> a;
		try {
			if(DEBUG) System.out.println("D_Organization:isEditable: check authoritarian GID");
			a = Application.getDB().select(gsql, new String[]{GID}, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		if (a.size() > 0) return true; // I have the key => editable
		return false; // I do not have the key => not editable;
	}
	public int getCertifyingMethod() {
		return this.params.certifMethods;
	}
	public void setCertifyingMethod(int method) {
		this.params.certifMethods = method;
		this.dirty_main = true;
	}

	public String getCreatorGID() {
		if (this.params == null) return null;
		return this.params.creator_global_ID;
	}
	public String getCreatorLID() {
		return this.creator_ID;
	}
	public boolean getRequested() {
		return this.requested;
	}
	public boolean getBlocked() {
		return this.blocked;
	}
	public void setBlocked(boolean blocked2) {
		this.blocked = blocked2;
		dirty_main = true;
	}
	/**
	 * Returns false if WEIGHTS_TYPE_NONE
	 * @return
	 */
	public boolean hasWeights() {
		if (this.getWeightsType() == net.ddp2p.common.table.organization.WEIGHTS_TYPE_NONE)
			return false;
		return true;
	}
	/**
	 * table.organization.WEIGHTS_TYPE_NONE = 0
	 * table.organization.WEIGHTS_TYPE_0_1 = 1
	 * table.organization.WEIGHTS_TYPE_INT = 2
	 * @return
	 */
	public int getWeightsType() {
		if (this.params == null) return net.ddp2p.common.table.organization.WEIGHTS_TYPE_DEFAULT;
		return this.params.mWeightsType;
	}
	/**
	 * table.organization.WEIGHTS_TYPE_NONE = 0
	 * table.organization.WEIGHTS_TYPE_0_1 = 1
	 * table.organization.WEIGHTS_TYPE_INT = 2
	 * 
	 * @param weightsType
	 */
	public void setWeightsType(int weightsType) {
		if (this.params == null) this.params = new D_OrgParams();
		this.params.mWeightsType = weightsType;
		dirty_main = true;
	}
	/**
	 * A negative number -1 signals infinity (no limit)
	 * 
	 * @return
	 */
	public int getWeightsMax() {
		if (this.params == null) return net.ddp2p.common.table.organization.WEIGHTS_MAX_DEFAULT;
		return this.params.mWeightsMax;
	}
	/**
	 * A negative number -1 signals infinity (no limit)
	 * 
	 * @param weightsMax
	 */
	public void setWeightsMax(int weightsMax) {
		if (this.params == null) this.params = new D_OrgParams();
		this.params.mWeightsMax = weightsMax;
		dirty_main = true;
	}
	/**
	 * The value that is disseminated (If users are allowed to create custom neighborhood levels)
	 * @return
	 */
	public boolean getNetworkRule() {
		return this.neighborhoods_rule;
	}
	/**
	 * The value that is certified (requested by creator)
	 * @param val
	 */
	public void setNetworkRule(boolean val) {
		this.neighborhoods_rule = val;
		this.dirty_main = true;
	}
	/**
	 * The value that is disseminated
	 * @return
	 */
	public boolean getBroadcastRule() {
		return this.broadcast_rule;
	}
	/**
	 * The value that is certified (requested by creator)
	 * @param val
	 */
	public void setBroadcastRule(boolean val) {
		this.broadcast_rule = val;
		this.dirty_main = true;
	}
	public boolean getBroadcasted() {
		return this.broadcasted;
	}
	/*
	public void setBroadcasted(boolean val) {
		this.broadcasted = val;
		this.dirty_locals = true;
	}
	*/
	public String[] getLanguages() {
		return this.params.languages;
	}
	public void setLanguages(String new_text, String[] lang) {
		this.params.languages = lang;
		this.dirty_main = true;
	}
		
	public String[] getNamesOrg() {
		return this.concepts.name_organization;
	}
	/**
	 * The first parameter is just a string for usage in tha database (not used),
	 * if null it is computed with: D_OrgConcepts.stringFromStringArray(_names_justification);
	 * 
	 * @param new_text
	 * @param names_org
	 */
	public void setNamesOrg(String new_text, String[] names_org) {
		this.concepts.name_organization = names_org;
		this.dirty_main = true;
	}

	public String[] getNamesForum() {
		return this.concepts.name_forum;
	}
	/**
	 * The first parameter is just a string for usage in tha database (not used),
	 * if null it is computed with: D_OrgConcepts.stringFromStringArray(_names_justification);
	 * 
	 * @param new_text
	 * @param _names_forum
	 */
	public void setNamesForum(String new_text, String[] _names_forum) {
		this.concepts.name_forum = _names_forum;
		this.dirty_main = true;
	}
	
	public String getNamesMotion_Default() {
		if  (this.concepts.name_motion == null || this.concepts.name_motion.length < 1) return __(DD.DEFAULT_MOTION);
		return this.concepts.name_motion[0];
	}
	
	public String[] getNamesMotion() {
		return this.concepts.name_motion;
	}
	/**
	 * The first parameter is just a string for usage in tha database (not used),
	 * if null it is computed with: D_OrgConcepts.stringFromStringArray(_names_justification);
	 * 
	 * @param new_text
	 * @param _names_motion
	 */
	public void setNamesMotion(String new_text, String[] _names_motion) {
		this.concepts.name_motion = _names_motion;
		this.dirty_main = true;
	}
	
	public String getNamesJustification_Default() {
		if  (this.concepts.name_justification == null || this.concepts.name_justification.length < 1) return __(DD.DEFAULT_JUSTIFICATION);
		return this.concepts.name_justification[0];
	}
	
	public String[] getNamesJustification() {
		return this.concepts.name_justification;
	}
	/**
	 * The first parameter is just a string for usage in tha database (not used),
	 * if null it is computed with: D_OrgConcepts.stringFromStringArray(_names_justification);
	 * @param just
	 * @param _names_justification
	 */
	public void setNamesJustification(String just,
			String[] _names_justification) {
		//if (just == null) just = D_OrgConcepts.stringFromStringArray(_names_justification);
		this.concepts.name_justification = _names_justification;
		dirty_main = true;
	}	
	
	public String[] getNamesConstituent() {
		return this.concepts.name_constituent;
	}
	/**
	 * The first parameter is just a string for usage in tha database (not used),
	 * if null it is computed with: D_OrgConcepts.stringFromStringArray(_names_justification);
	 * @param just
	 * @param _names_justification
	 */
	public void setNamesConstituent(String just,
			String[] _name_constituent) {
		//if (just == null) just = D_OrgConcepts.stringFromStringArray(_names_justification);
		this.concepts.name_constituent = _name_constituent;
		dirty_main = true;
	}	
	
	public String[] getDefaultScoringOptions() {
		if (this.params == null) return null;
		return this.params.default_scoring_options;
	}
	public void setDefaultScoring(String new_text, String[] _scores) {
		if (this.params == null) params = new D_OrgParams();
		this.params.default_scoring_options = _scores;
		this.dirty_main = true;
	}
	
	public String getDescription() {
		if (this.params == null) return null;
		return this.params.description;
	}
	/**
	 * Sets dirty, creates param if needed
	 * @param descr
	 */
	public void setDescription(String descr) {
		if (this.params == null) params = new D_OrgParams();
		this.params.description = descr;
		this.dirty_main = true;
	}
	
	public String getInstructionsNewMotions() {
		if (this.params == null) return null;
		return this.params.instructions_new_motions;
	}
	public void setInstructionsNewMotions(String instr) {
		if (this.params == null) params = new D_OrgParams();
		this.params.instructions_new_motions = instr;
		this.dirty_main = true;
	}
	
	public String getInstructionsRegistration() {
		return this.params.instructions_registration;
	}
	public void setInstructionsRegistration(String instr) {
		if (this.params == null) params = new D_OrgParams();
		this.params.instructions_registration = instr;
		this.dirty_main = true;
	}
	
	public String getPreapproved() {
		return this._preapproved;
	}
	public String getCategory() {
		return this.params.category;
	}
	public void setCategory(String new_category) { 
		if (this.params == null) params = new D_OrgParams();
		this.params.category = new_category;
		this.dirty_main = true;
	}
/**
 * If cGID null, get it from cLID. Sets both the creator GID and the creator_ID
 * @param cLID
 * @param cGID
 */
	public void setCreatorID(String cLID, String cGID) {
		if ((cGID == null) && (cLID != null)) cGID = D_Peer.getGIDfromLID(cLID);
		if ((cLID == null) && (cGID != null)) cLID = D_Peer.getLocalPeerIDforGID(cGID);
		this.creator_ID = cLID;
		this.params.creator_global_ID = cGID;
		this.dirty_main = true;
	}
	
	public static ArrayList<String> getExistingCategories() {
		String sql_cat = "SELECT "+net.ddp2p.common.table.organization.category+
		" FROM "+net.ddp2p.common.table.organization.TNAME+
		" GROUP BY "+net.ddp2p.common.table.organization.category;
		ArrayList<ArrayList<Object>> c;
		ArrayList<String> result = new ArrayList<String>();
		try {
			c = Application.getDB().select(sql_cat, new String[]{});
			for (ArrayList<Object> i: c) {
				String crt = Util.getString(i.get(0));
				result.add(crt);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return result;
	}

	public boolean hasCreatorID() {
		return this.getCreatorLID() != null;
	}

//	public String getNameOrMy() {
//		if (getNameMy() != null) return getNameMy();
//		return this.getOrgName();
//	}
	public String getOrgNameOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getName();
	}

	public void setOrgNameMy(String _name) {
		this.mydata.name = _name;
		setPreferencesDate();
		this.dirty_mydata = true;
	}
	public String getOrgNameMy() {
		return this.mydata.name;
	}
	
	/**
	 * Loads the creator, if not already loaded.
	 * @return
	 */
	public D_Peer getCreator() {
		String cLID = this.getCreatorLID();
		if ((this.creator == null) && (cLID != null))
			this.creator = D_Peer.getPeerByLID_NoKeep(cLID, true);
		return this.creator;
	}
	/**
	 * Returns null if not yet loaded
	 * @return
	 */
	public D_Peer getCreatorIfLoaded() {
		return this.creator;
	}

	public String getCreatorNameMy() {
		return mydata.creator;
	}

	public String getCreatorPeerNameMy() {
		D_Peer c = getCreator();
		if (c == null) return null;
		return c.getNameMy();
	}

	public String getCreatorNameOriginal() {
		D_Peer c = getCreator();
		if (c == null) return null;
		return c.getName();
	}

	public boolean getCreatorNameVerified() {
		D_Peer c = getCreator();
		if (c == null) return false;
		return c.isVerifiedName();
	}

	public String getCategoryOrMy() {
		if (getCategoryMy() != null) return getCategoryMy();
		return getCategory();
	}
	public String getCategoryMy() {
		return mydata.category;
	}

	public void setCreatorMy(String creator2) {
		this.mydata.creator = creator2;
		setPreferencesDate();
		this.dirty_mydata = true;
	}

	public void setCategoryMy(String cat2) {
		this.mydata.category = cat2;
		setPreferencesDate();
		this.dirty_mydata = true;
	}
	/*
	private void set_my_data(String field_name, String value, int row) {
		if (row >= data.size()) return;
		if ("".equals(value)) value = null;
		if (_DEBUG) System.out.println("Set value =\""+value+"\"");
		String org_ID = getLIDstr(row);
		try {
			String sql = "SELECT "+field_name+" FROM "+table.my_organization_data.TNAME+" WHERE "+table.my_organization_data.organization_ID+"=?;";
			ArrayList<ArrayList<Object>> orgs = db.select(sql,new String[]{org_ID});
			if(orgs.size()>0){
				db.update(table.my_organization_data.TNAME, new String[]{field_name},
						new String[]{table.my_organization_data.organization_ID}, new String[]{value, org_ID}, _DEBUG);
			}else{
				if(value==null) return;
				db.insert(table.my_organization_data.TNAME,
						new String[]{field_name,table.my_organization_data.organization_ID},
						new String[]{value, org_ID}, _DEBUG);
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	*/

	public boolean haveCreatorKey() {
		D_Peer c = getCreator();
		if (c == null) return false;
		return c.getSK() != null;
	}

	public boolean currentlyEdited() {
		if (this.getLID() <= 0) {
			return true;
		}
		return false;
	}
	public long addEmptyOrgExtraParam(String partNeigh) {
		synchronized(D_Organization.lock_organization_GID_storage) {
			assertReferenced();
			D_OrgParam[] old = params.orgParam;
			if (old == null) old = new D_OrgParam[0];
			D_OrgParam[] nou = new D_OrgParam[old.length+1];
			for (int k = 0; k < old.length; k ++) {
				nou[k] = old[k];
			}
			nou[old.length] = new D_OrgParam();
			nou[old.length].partNeigh = Util.ival(partNeigh, net.ddp2p.common.table.field_extra.partNeigh_non_neighborhood_indicator);
			long extra_ID;
			try {
				String new_global_extra = null;
	 			extra_ID = Application.getDB().insert(net.ddp2p.common.table.field_extra.TNAME,
						new String[]{net.ddp2p.common.table.field_extra.organization_ID, net.ddp2p.common.table.field_extra.global_field_extra_ID, net.ddp2p.common.table.field_extra.partNeigh},
						new String[]{this.getLIDstr_forced(), new_global_extra, partNeigh
						}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return -1;
			}
			nou[old.length].field_LID = extra_ID;
			params.orgParam = nou;
			if (DEBUG) System.out.println("D_Organization:addEmptyOrgExtraParam: nou["+old.length+"]="+nou[old.length]);
			return extra_ID;
		}
	}
	/**
	 * Sts dirty
	 * @param new_global_extra
	 * @return
	 */
	public long addEmptyOrgExtraParamTmp(String new_global_extra) {
		
		synchronized(D_Organization.lock_organization_GID_storage) {
			long eID = this.getFieldExtraID(new_global_extra);
			if (eID > 0) return eID;

			assertReferenced();
			D_OrgParam[] old = params.orgParam;
			if (old == null) old = new D_OrgParam[0];
			D_OrgParam[] nou = new D_OrgParam[old.length+1];
			for (int k = 0; k < old.length; k ++) {
				nou[k] = old[k];
			}
			nou[old.length] = new D_OrgParam();
			nou[old.length].partNeigh = net.ddp2p.common.table.field_extra.partNeigh_non_neighborhood_indicator;
			nou[old.length].global_field_extra_ID = new_global_extra;
			nou[old.length].tmp = "1";
			nou[old.length].dirty = true;
			this.dirty_params = true;
			long extra_ID;
			try {
				//String new_global_extra = null;
	 			extra_ID = Application.getDB().insert(net.ddp2p.common.table.field_extra.TNAME,
						new String[]{
	 					net.ddp2p.common.table.field_extra.organization_ID,
	 					net.ddp2p.common.table.field_extra.global_field_extra_ID,
	 					net.ddp2p.common.table.field_extra.partNeigh,
	 					net.ddp2p.common.table.field_extra.tmp
	 					},
						new String[]{this.getLIDstr_forced(), new_global_extra, nou[old.length].partNeigh+"", "1"
						}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return -1;
			}
			nou[old.length].field_LID = extra_ID;
			params.orgParam = nou;
			return extra_ID;
		}
	}
	public D_OrgParam getOrgParam(int row) {
		if (row < 0) return null;
		if (params == null) return null;
		if (params.orgParam == null) return null;
		if (row >= params.orgParam.length) return null;
		return params.orgParam[row];
	}
	public void deleteOrgParam(int row) {
		synchronized(D_Organization.lock_organization_GID_storage) {
			assertReferenced();
			if (row < 0) return;
			if (params == null) return;
			if (params.orgParam == null) return;
			if (params.orgParam.length >= row) return;

			D_OrgParam[] old = params.orgParam;
			if ((old == null) || (old.length == 0)) return;
			D_OrgParam[] nou = new D_OrgParam[old.length - 1];
			int n = 0;
			for (int k = 0; k < old.length; k ++) {
				if (k == row) continue;
				nou[n ++] = old[k];
			}
			params.orgParam = nou;
	    	try {
				Application.getDB().delete(net.ddp2p.common.table.field_extra.TNAME, new String[]{net.ddp2p.common.table.field_extra.field_extra_ID}, new String[]{Util.getStringID(old[row].field_LID)}, DEBUG);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	/**
	 * Returning the preloaded params
	 * @return
	 */
	public D_OrgParam[] getOrgParams() {
		if (params == null) return null;
		if (params.orgParam == null) return null;
		return params.orgParam;
	}
	/**
	 * This also computed the ExtraGIDS using this.updateExtraGIDs();
	 * and sets dirty_params
	 * @param param
	 */
	public void setOrgParams(D_OrgParam[] param) {
		if (params == null) params = new D_OrgParams();
		params.orgParam = param;
		this.updateExtraGIDs();
		//this.dirty_main = true;
		this.dirty_params = true;
	}
	/*
	public static boolean set_my_data(String field_name, String value, D_OrgParam row) {
		//synchronized()
		//boolean DEBUG = true;
		if(D_OrgParam.DEBUG)System.out.println("OrgExtraModel:set_my_data: field="+field_name);
		//ArrayList<Object> field = this.m_extras.get(row);
		String fieldID = Util.getStringID(row.field_LID);//Util.getString(field.get(table.field_extra.OPARAM_EXTRA_FIELD_ID));
		String hash = row.global_field_extra_ID;
		try {
			if (table.field_extra.label.equals(field_name) || table.field_extra.list_of_values.equals(field_name)) {
				hash = D_OrgParam.makeGID(value, row.list_of_values, row.version);
			}
			Application.db.updateNoSync(table.field_extra.TNAME,
					new String[]{field_name, table.field_extra.global_field_extra_ID},
					new String[]{table.field_extra.field_extra_ID}, 
					new String[]{value,hash,fieldID}, D_OrgParam.DEBUG);
		
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	*/
	/**
	 * Called from GUI to discard an organisation completely.
	 * @param orgID
	 * @return
	 */
	public static D_Organization deleteAllAboutOrg(String orgID) {
		try {
			D_Organization o = D_Organization_Node.loaded_By_GID.get(orgID);
			if (o != null) {
				if (o.getStatusReferences() <= 0) {
					if (!D_Organization_Node.dropLoaded(o, false)) {
						System.out.println("D_Organization: deleteAllAboutOrg: referred = "+o.getStatusReferences());
					}
				}
			}
			
			//Application.db.delete(table.field_value.TNAME, new String[]{table.field_value.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.witness.TNAME, new String[]{table.witness.organization_ID}, new String[]{orgID}, DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.justification.TNAME, new String[]{net.ddp2p.common.table.justification.organization_ID}, new String[]{orgID}, DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.motion.TNAME, new String[]{net.ddp2p.common.table.motion.organization_ID}, new String[]{orgID}, DEBUG);
			//Application.db.delete(table.signature.TNAME, new String[]{table.signature.organization_ID}, new String[]{orgID}, DEBUG);
			String sql="SELECT "+net.ddp2p.common.table.constituent.constituent_ID+" FROM "+net.ddp2p.common.table.constituent.TNAME+" WHERE "+net.ddp2p.common.table.constituent.organization_ID+"=?;";
			ArrayList<ArrayList<Object>> constits = Application.getDB().select(sql, new String[]{orgID}, DEBUG);
			for (ArrayList<Object> a: constits) {
				String cID = Util.getString(a.get(0));
				Application.getDB().delete(net.ddp2p.common.table.witness.TNAME, new String[]{net.ddp2p.common.table.witness.source_ID}, new String[]{cID}, DEBUG);
				Application.getDB().delete(net.ddp2p.common.table.witness.TNAME, new String[]{net.ddp2p.common.table.witness.target_ID}, new String[]{cID}, DEBUG);
	
	   			Application.getDB().delete(net.ddp2p.common.table.signature.TNAME, new String[]{net.ddp2p.common.table.signature.constituent_ID}, new String[]{cID}, DEBUG);
				Application.getDB().delete(net.ddp2p.common.table.justification.TNAME, new String[]{net.ddp2p.common.table.justification.constituent_ID}, new String[]{cID}, DEBUG);
				Application.getDB().delete(net.ddp2p.common.table.motion.TNAME, new String[]{net.ddp2p.common.table.motion.constituent_ID}, new String[]{cID}, DEBUG);
	   			// Application.db.delete(table.news.TNAME, new String[]{table.news.constituent_ID}, new String[]{cID}, DEBUG);
	   		}
			Application.getDB().delete(net.ddp2p.common.table.news.TNAME, new String[]{net.ddp2p.common.table.news.organization_ID}, new String[]{orgID}, DEBUG);
			String sql_del_fv_fe =
					"DELETE FROM "+net.ddp2p.common.table.field_value.TNAME+
					" WHERE "+net.ddp2p.common.table.field_value.field_extra_ID+
					" IN ( SELECT "+net.ddp2p.common.table.field_extra.field_extra_ID+" FROM "+net.ddp2p.common.table.field_extra.TNAME+" WHERE "+net.ddp2p.common.table.field_extra.organization_ID+"=? )";
			Application.getDB().delete(sql_del_fv_fe, new String[]{orgID}, DEBUG);
			String sql_del_fv =
					"DELETE FROM "+net.ddp2p.common.table.field_value.TNAME+
					" WHERE "+net.ddp2p.common.table.field_value.constituent_ID+
					" IN ( SELECT DISTINCT c."+net.ddp2p.common.table.constituent.constituent_ID+" FROM "+net.ddp2p.common.table.constituent.TNAME+" AS c " +
							" JOIN "+net.ddp2p.common.table.field_value.TNAME+" AS v ON (c."+net.ddp2p.common.table.constituent.constituent_ID+"=v."+net.ddp2p.common.table.field_value.constituent_ID+") " +
									" WHERE c."+net.ddp2p.common.table.constituent.organization_ID+"=? GROUP BY c."+net.ddp2p.common.table.constituent.constituent_ID+" ) ";
			Application.getDB().delete(sql_del_fv, new String[]{orgID}, DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.constituent.TNAME, new String[]{net.ddp2p.common.table.constituent.organization_ID}, new String[]{orgID}, DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.neighborhood.TNAME, new String[]{net.ddp2p.common.table.neighborhood.organization_ID}, new String[]{orgID}, DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.identity_ids.TNAME, new String[]{net.ddp2p.common.table.identity_ids.organization_ID}, new String[]{orgID}, DEBUG);
			Application.getDB().update(net.ddp2p.common.table.identity.TNAME, new String[]{net.ddp2p.common.table.identity.organization_ID}, new String[]{net.ddp2p.common.table.identity.organization_ID}, new String[]{"-1",orgID}, DEBUG);

			return deleteOrgDescription(orgID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	public static D_Organization deleteOrgDescription(String orgID) {
		try {
			Application.getDB().delete(net.ddp2p.common.table.field_extra.TNAME, new String[]{net.ddp2p.common.table.field_extra.organization_ID}, new String[]{orgID}, DEBUG);
			Application.getDB().delete(net.ddp2p.common.table.organization.TNAME, new String[]{net.ddp2p.common.table.organization.organization_ID}, new String[]{orgID}, DEBUG);
			return unlinkMemory(orgID);
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		return null;
	}
	private static D_Organization unlinkMemory(String orgID) {
		Long lID = new Long(orgID);
		D_Organization org = D_Organization_Node.loaded_By_LocalID.get(lID);
		if (org == null) { 
			System.out.println("D_Organization: unlinkMemory: referred null org for: "+orgID);
			return null;
		}
		if (DEBUG) System.out.println("D_Organization: unlinkMemory: dropped org");
		if (!D_Organization_Node.dropLoaded(org, true)) {
			if (DEBUG) System.out.println("D_Organization: unlinkMemory: referred = "+org.getStatusReferences());
		} else {
			if (DEBUG) System.out.println("D_Organization: unlinkMemory: no problem dropping");
		}
		return org;
	}
	public long insertTmpFieldExtra(String fieldGID) throws P2PDDSQLException {
		boolean _DEBUG = true;
		if (_DEBUG) System.out.println("D_FieldExtra: insertTmpFieldExtra: start");
		if (fieldGID == null) Util.printCallPath("Null field");
		
		D_Organization org = D_Organization.getOrgByOrg_Keep(this);
		long id = org.addEmptyOrgExtraParamTmp(fieldGID);
		if (org.dirty_any()) org.storeRequest();
//		long id = Application.db.insert(table.field_extra.TNAME,
//				new String[]{table.field_extra.global_field_extra_ID, table.field_extra.organization_ID, table.field_extra.tmp},
//				new String[]{fieldGID, Util.getStringID(org_ID), "1"}, _DEBUG );
		org.releaseReference();
		return id;
	}
	/**
	 * Search in existing fields, else may insert a temporary
	 * @param fieldGID
	 * @param insertTmp
	 * @return
	 * @throws P2PDDSQLException
	 * @throws ExtraFieldException
	 */
	public String getFieldExtraID(String fieldGID, boolean insertTmp) throws P2PDDSQLException, ExtraFieldException {
		if (fieldGID == null) return null;
		//if ((org_ID <= 0) || (org == null)) return null;
		String id = Util.getStringID(getFieldExtraID(fieldGID)); //D_Organization.getFieldExtraID_FromTable(fieldGID, org_ID);
		if (id != null) return id;
		if (! insertTmp) return id;
		if (insertTmp)
			return Util.getStringID(insertTmpFieldExtra(fieldGID));
		throw new ExtraFieldException("Unknown field type: \""+fieldGID+"\" for org \""+getLID_forced()+"\"");
	}
	/**
	 * Search in existing fields (even on disk if not in memory- which should not happen)
	 * @param field_extra_GID
	 * @return
	 */
	public long getFieldExtraID(String field_extra_GID) {
		if (params == null) return -1;
		if (params.orgParam == null) return -1;
		for (D_OrgParam op : params.orgParam) {
			if (Util.equalStrings_null_or_not(field_extra_GID, op.global_field_extra_ID)) {
				if (op.field_LID > 0) return op.field_LID;
				try {
					return Util.lval(D_Organization.getFieldExtraLID_FromTable(field_extra_GID, this.getLID_forced()));
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
		}
		return -1;
	}
	public String getFieldExtraGID(long field_extra_LID) {
		if (field_extra_LID <= 0) return null;
		if (params == null) return null;
		if (params.orgParam == null) return null;
		for (D_OrgParam op : params.orgParam) {
			if (field_extra_LID == op.field_LID) {
				if (op.global_field_extra_ID != null) return op.global_field_extra_ID;
				try {
					return Util.sval(D_Organization.getFieldExtraGID_FromTable(field_extra_LID), null);
				} catch (P2PDDSQLException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}
	final public static String sql_getFieldsExtraByGID = "SELECT "+net.ddp2p.common.table.field_extra.field_extra_ID+
			" FROM "+net.ddp2p.common.table.field_extra.TNAME+
			" WHERE "+net.ddp2p.common.table.field_extra.global_field_extra_ID+"=? AND "+net.ddp2p.common.table.field_extra.organization_ID+"=?;";
	final public static String sql_getFieldsExtraByLID = "SELECT "+net.ddp2p.common.table.field_extra.global_field_extra_ID+
			" FROM "+net.ddp2p.common.table.field_extra.TNAME+
			" WHERE "+net.ddp2p.common.table.field_extra.field_extra_ID+"=?;";
	
	/**
	 * Get the LID of fiels in org_ID, directly from the database
	 * @param fieldGID
	 * @param org_LID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static String getFieldExtraLID_FromTable(String fieldGID, long org_LID) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) Util.printCallPath("D_Organization: getFieldExtraID: should not get here! field="+fieldGID+" oID="+org_LID);
		if (fieldGID == null) return null;
		fieldGID = fieldGID.trim();
		if ("".equals(fieldGID)) return null;
		
		
		ArrayList<ArrayList<Object>> n = Application.getDB().select(sql_getFieldsExtraByGID, new String[]{fieldGID, Util.getStringID(org_LID)}, DEBUG);
		if (n.size() != 0) {// happeded on savind orgs from DD_SK
			if (_DEBUG) Util.printCallPath("D_Organization: getFieldExtraID: unexpectedly found "+fieldGID+" ("+org_LID+") as "+Util.getString(n.get(0).get(0)));
			return Util.getString(n.get(0).get(0));
		}
		return null;
	}
	public static String getFieldExtraGID_FromTable(long fieldLID) throws P2PDDSQLException {
		//boolean DEBUG = true;
		if (DEBUG) Util.printCallPath("D_Organization: getFieldExtraGID: should not get here! field="+fieldLID);
		if (fieldLID <= 0) return null;
		
		ArrayList<ArrayList<Object>> n = Application.getDB().select(sql_getFieldsExtraByLID, new String[]{Util.getStringID(fieldLID)}, DEBUG);
		if (n.size() != 0) {// happeded on savind orgs from DD_SK
			if (_DEBUG) Util.printCallPath("D_Organization: getFieldExtraGID: unexpectedly found "+fieldLID+" as "+Util.getString(n.get(0).get(0)));
			return Util.getString(n.get(0).get(0));
		}
		return null;
	}
	public static D_OrgParam[] getNonEphemeral(D_OrgParam[] orgParam) {
		ArrayList<D_OrgParam> aop = new ArrayList<D_OrgParam>(Arrays.asList(orgParam));
		ArrayList<D_OrgParam> nop = new ArrayList<D_OrgParam>();
		for (D_OrgParam op : aop) {
			if (
					(op.tmp == null) 
					|| "0".equals(op.tmp) //|| Util.equalStrings_null_or_not("0",op.tmp)
					) nop.add(op);
		}
		return nop.toArray(new D_OrgParam[0]);
	}
	public int getOrgParamsLen() {
		if (getOrgParams() == null) return 0;
		return getOrgParams().length;
	}

	public static ArrayList<ResetOrgInfo> buildResetPayload() throws P2PDDSQLException{
		String sql = 
				"SELECT "+net.ddp2p.common.table.organization.reset_date+","+net.ddp2p.common.table.organization.global_organization_ID_hash+
				" FROM "+net.ddp2p.common.table.organization.TNAME+
				" WHERE "+net.ddp2p.common.table.organization.signature+" IS NOT NULL AND "+
				net.ddp2p.common.table.organization.reset_date+" IS NOT NULL AND "+net.ddp2p.common.table.organization.broadcasted+"='1';";
		ArrayList<ArrayList<Object>> a = Application.getDB().select(sql, new String[]{});
		if(a.size()==0) return null;
		ArrayList<ResetOrgInfo> changed_orgs = new ArrayList<ResetOrgInfo>();
		for(ArrayList<Object> o: a) {
			ResetOrgInfo roi = new ResetOrgInfo();
			roi.reset_date = Util.getCalendar(Util.getString(o.get(0)));
			roi.hash = Util.getString(o.get(1));
			changed_orgs.add(roi);
		}
		
		return changed_orgs;
	}

	public D_OrgParam getFieldExtra(String field_extra_GID) {
		if (params == null || params.orgParam == null) return null;
		for (int k = 0 ; k < this.params.orgParam.length ; k ++ ) {
			if ( Util.equalStrings_null_or_not(this.params.orgParam[k].global_field_extra_ID, field_extra_GID) ) {
				return this.params.orgParam[k];
			}
		}
		return null;
	}

	public static final String sql_extra_fields_RootSubdivisions =
				"SELECT "+net.ddp2p.common.table.field_extra.field_extra_ID+
				", "+net.ddp2p.common.table.field_extra.label+
				" FROM "+net.ddp2p.common.table.field_extra.TNAME+
				" WHERE "+net.ddp2p.common.table.field_extra.organization_ID+" = ? AND "+net.ddp2p.common.table.field_extra.partNeigh+" > 0 ORDER BY "+net.ddp2p.common.table.field_extra.partNeigh+";";
	public static ArrayList<Object> getDefaultRootSubdivisions(long organizationID) {
		ArrayList<Object> result = new ArrayList<Object>();
		String subdivisions;
		long[] fieldIDs = new long[0];
		ArrayList<ArrayList<Object>> fields_neighborhood;
		
		subdivisions = net.ddp2p.common.table.neighborhood.SEP_names_subdivisions;
		try {
			fields_neighborhood = Application.getDB().select(sql_extra_fields_RootSubdivisions,	new String[] {"" + organizationID}, DEBUG);
			
			fieldIDs = new long[fields_neighborhood.size()];
			for (int i = 0; i < fields_neighborhood.size(); i ++) {
				fieldIDs[i] = Util.Lval(fields_neighborhood.get(i).get(0));
				subdivisions = subdivisions+Util.sval(fields_neighborhood.get(i).get(1), "")+net.ddp2p.common.table.neighborhood.SEP_names_subdivisions;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		result.add(subdivisions);
		result.add(fieldIDs);
		return result;
	}
	public ArrayList<Object> getDefaultRootSubdivisionsFromDB() {
		ArrayList<Object> result = new ArrayList<Object>();
		String subdivisions;
		long[] fieldIDs = new long[0];
		ArrayList<ArrayList<Object>> fields_neighborhood;
		
		subdivisions = net.ddp2p.common.table.neighborhood.SEP_names_subdivisions;
		try {
			fields_neighborhood = //getNeighborhoodsList();
			Application.getDB().select(sql_extra_fields_RootSubdivisions,	new String[] {this.getLIDstr()}, DEBUG);
			
			fieldIDs = new long[fields_neighborhood.size()];
			for (int i = 0; i < fields_neighborhood.size(); i ++) {
				fieldIDs[i] = Util.Lval(fields_neighborhood.get(i).get(0));
				subdivisions = subdivisions+Util.sval(fields_neighborhood.get(i).get(1), "")+net.ddp2p.common.table.neighborhood.SEP_names_subdivisions;
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		result.add(subdivisions);
		result.add(fieldIDs);
		return result;
	}
	public ArrayList<Object> getDefaultRootSubdivisions() {
		ArrayList<Object> result = new ArrayList<Object>();
		String subdivisions;
		long[] fieldIDs = new long[0];
		ArrayList<D_OrgParam> fields_neighborhood;
		
		subdivisions = net.ddp2p.common.table.neighborhood.SEP_names_subdivisions;

		fields_neighborhood = getNeighborhoodsList();			
		fieldIDs = new long[fields_neighborhood.size()];
		if(DEBUG)System.out.println("D_Organization:  getDefaultRootSubdivisions: params= #"+fieldIDs.length);
		for (int i = 0; i < fields_neighborhood.size(); i ++) {
			fieldIDs[i] = Util.Lval(fields_neighborhood.get(i).field_LID);
			subdivisions = subdivisions+Util.sval(fields_neighborhood.get(i).label, "")+net.ddp2p.common.table.neighborhood.SEP_names_subdivisions;
			if(DEBUG)System.out.println("D_Organization:  getDefaultRootSubdivisions: param= #"+i+"/"+fieldIDs[i]+" " +subdivisions);
		}
		result.add(subdivisions);
		result.add(fieldIDs);
		return result;
	}

	public ArrayList<D_OrgParam> getNeighborhoodsList() {
		if(DEBUG)System.out.println("D_Organization:  getNeighborhoodsList: params= #"+this.getOrgParamsLen());
		ArrayList<D_OrgParam> result = new ArrayList<D_OrgParam>();
		for (int i = 0; i < this.getOrgParamsLen(); i ++) {
			D_OrgParam op = this.getOrgParam(i);
			if(DEBUG)System.out.println("D_Organization:  getNeighborhoodsList: param "+i+" = "+op);
			if (net.ddp2p.common.table.field_extra.isANeighborhood(op.partNeigh)) {
//				for (int k = 0; k <= result.size(); k ++) {
//				    if (k >= result.size()) {result.add(k, op);break;}
//					if (op.partNeigh > result.get(k).partNeigh) {result.add(k, op);break;}
//				}
				for (int k = result.size(); k >= 0; k --) {
					if(DEBUG)System.out.println("D_Organization:  getNeighborhoodsList: insert "+k+" try ");
					if (k <= 0) {result.add(0, op);break;}
					if(DEBUG)System.out.println("D_Organization:  getNeighborhoodsList: insert cmp "+(k-1)+" = "+result.get(k-1));
					if (op.partNeigh <= result.get(k - 1).partNeigh) {result.add(k, op);break;}
					if(DEBUG)System.out.println("D_Organization:  getNeighborhoodsList: insert "+k+" No ");
				}
			} else {
				if(DEBUG)System.out.println("D_Organization:  getNeighborhoodsList: not ANeigh "+op.partNeigh);
			}
		}
		if(DEBUG)System.out.println("D_Organization:  getNeighborhoodsList: got #"+result.size());
		return result;
	}
	public ArrayList<D_OrgParam> getNeighborhoodsListDec() {
		ArrayList<D_OrgParam> result = new ArrayList<D_OrgParam>();
		for (int i = 0; i < this.getOrgParamsLen(); i ++) {
			D_OrgParam op = this.getOrgParam(i);
			if (net.ddp2p.common.table.field_extra.isANeighborhood(op.partNeigh)) {
				for (int k = 0; k <= result.size(); k ++) {
					if (k >= result.size()) {result.add(op);break;}
					if (op.partNeigh > result.get(k).partNeigh) {result.add(k, op);break;}
				}
			}
		}
		return result;
	}
	public ArrayList<D_OrgParam> getConstituentFieldsListDec() {
		ArrayList<D_OrgParam> result = new ArrayList<D_OrgParam>();
		for (int i = 0; i < this.getOrgParamsLen(); i ++) {
			D_OrgParam op = this.getOrgParam(i);
			//if (net.ddp2p.common.table.field_extra.isANeighborhood(op.partNeigh)) {
				for (int k = 0; k <= result.size(); k ++) {
					if (k >= result.size()) {result.add(op);break;}
					if (op.partNeigh > result.get(k).partNeigh) {result.add(k, op);break;}
				}
			//}
		}
		return result;
	}
	public ArrayList<D_OrgParam> getConstituentFieldsListAsc() {
		ArrayList<D_OrgParam> result = new ArrayList<D_OrgParam>();
		for (int i = 0; i < this.getOrgParamsLen(); i ++) {
			D_OrgParam op = this.getOrgParam(i);
			//if (net.ddp2p.common.table.field_extra.isANeighborhood(op.partNeigh)) {
				for (int k = 0; k <= result.size(); k ++) {
					if (k >= result.size()) {result.add(op);break;}
					if (op.partNeigh < result.get(k).partNeigh) {result.add(k, op);break;}
				}
			//}
		}
		return result;
	}
	public ArrayList<D_OrgParam> getConstituentsPropertiesList() {
		ArrayList<D_OrgParam> result = new ArrayList<D_OrgParam>();
		for (int i = 0; i < this.getOrgParamsLen(); i ++) {
			D_OrgParam op = this.getOrgParam(i);
			if (! net.ddp2p.common.table.field_extra.isANeighborhood(op.partNeigh)) {
				result.add(op);
			}
		}
		return result;
	}

	/**
	 * 
	 * First a store request
	 * @return
	 */
	/*
	public long getLID_force_save() {
		this.storeRequest();
		return this.getLID_forced();
	}
*/
	public void setLID(String __organization_ID) {
		this._organization_ID = Util.lval(__organization_ID);
		this.organization_ID = __organization_ID;
	}
	public void setLID(long __organization_ID) {
		this._organization_ID = __organization_ID;
		this.organization_ID = Util.getStringID(__organization_ID);
	}
	public void setLID_AndLink(long __organization_ID) {
		setLID(__organization_ID);
		if (__organization_ID > 0)
			D_Organization_Node.register_newLID_ifLoaded(this);
		//this.component_node.loaded_By_LocalID.put(new Long(getLID()), this);
	}
	/**
	 * Returns crt value (no force saving)
	 * @return
	 */
	public long getLID() {
		return this._organization_ID;
	}
	/**
	 * Return current value (no forced saving)
	 * @return
	 */
	public String getLIDstr() {
		return this.organization_ID;
	}
	/**
	 * If none yet and dirty, then save!
	 * return -1 if not dirty and null
	 * 
	 * @return
	 */
	public long getLID_forced() {
		if (this.getLID() <= 0) {
			//String lid = 
			getLIDstr_forced();
		}
		return this.getLID();
	}
	/**
	 * If none yet and dirty, then save!
	 * return null if not dirty and null
	 * @return
	 */
	public String getLIDstr_forced() {
//		if ((this.getLIDstr() == null) && (this.getLID() > 0))
//			this.setLID(Util.getStringID(getLID()));
		if ((this.getLIDstr() == null) && (this.dirty_any())) {
			this.storeSynchronouslyNoException();
			return this.getLIDstr();
		}
		return this.getLIDstr();
	}

	public void updateExtraGIDs() {
		if (this.params == null) return;
		if (this.params.orgParam == null) return;
		for (D_OrgParam p : this.params.orgParam) {
			String eGID = p.makeGID();
			if (!Util.equalStrings_null_or_not(eGID, p.global_field_extra_ID)) {
				p.global_field_extra_ID = eGID;
				p.dirty = true;
				this.dirty_params = true;
			}
		}
	}

	public int getStatusReferences() {
		return status_references;
	}

	StackTraceElement[] lastPath;
	final private Object monitor_reserve = new Object();
	public void incStatusReferences() {
		if (this.getStatusReferences() > 0) {
			//Util.printCallPath("Why getting: "+getStatusReferences());
			Util.printCallPath("D_Organization: incStatusReferences: Will sleep for getting: "+getStatusReferences()+" for "+getName());
			synchronized(monitor_reserve) {
				if (this.getStatusReferences() > 0)
					try {
						do {
							Application_GUI.ThreadsAccounting_ping("Wait org references for "+getName());
							monitor_reserve.wait(10000); // wait 5 seconds, and do not re-sleep on spurious wake-up
							Application_GUI.ThreadsAccounting_ping("Got org references for "+getName());
							Util.printCallPath("D_Organization: incStatusReferences: After sleep getting: "+getStatusReferences()+" for "+getName());
						} while (this.getStatusReferences() > 0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				if (this.getStatusReferences() > 0) {
					Util.printCallPath(this+"\nSpurious wake after 5s this=: "+getStatusReferences()+" for "+getName());
					Util.printCallPath(lastPath, "Last path was: ", "     ");
				}
			}
		}
		lastPath = Util.getCallPath();
		this.status_references++;
	}
	public void decStatusReferences() {
		if (this.getStatusReferences() <= 0) {
			Util.printCallPath("Why getting: "+getStatusReferences());
			return;
		}
		this.status_references--;
		Application_GUI.ThreadsAccounting_ping("Drop org references for "+getName());
		synchronized(monitor_reserve) {
			monitor_reserve.notify();
		}
		Application_GUI.ThreadsAccounting_ping("Dropped org references for "+getName());
	}

	public OrgPeerDataHashes getSpecificRequest() {
		if (specific_request != null && specific_request.getOrganizationGIDH() == null)
			specific_request.setOrganizationGIDH(this.getGIDH());
		return specific_request;
	}

	public void setSpecificRequest(OrgPeerDataHashes specific_request) {
		this.specific_request = specific_request;
	}
	
	/**
	 * Checks if name is null.
	 * Possible when not DD.ACCEPT_ORGANIZATIONS_WITH_NULL_NAME
	 * @return
	 */
	public boolean justGIDContainer() {
		if (DD.ACCEPT_ORGANIZATIONS_WITH_NULL_NAME) return false;
		return getName() == null;
	}
	public final static String sql_all_orgs = "SELECT "+net.ddp2p.common.table.organization.organization_ID+" FROM "+net.ddp2p.common.table.organization.TNAME+";";
	public final static String sql_all_orgs_limit = "SELECT "+net.ddp2p.common.table.organization.organization_ID+" FROM "+net.ddp2p.common.table.organization.TNAME+" LIMIT ? OFFSET ?"+";";
	/**
	 * The index of organization LID in array returned by getAllOrganizations
	 */
	public final static int SELECT_ALL_ORG_LID = 0;
	/**
	 * 
	 * @return
	 */
	public static java.util.ArrayList<java.util.ArrayList<Object>> getAllOrganizations() {
		ArrayList<ArrayList<Object>> result;
		try {
			if (Application.getDB() != null)
				result = Application.getDB().select(sql_all_orgs, new String[0]);
			else result = new ArrayList<ArrayList<Object>>();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Object>>();
		}
		return result;
	}
	/**
	 * 
	 * @param offset (use negative or 0 for no offset)
	 * @param limit (use negative for no limit)
	 * @return
	 */
	public static java.util.ArrayList<java.util.ArrayList<Object>> getAllOrganizations(int offset, int limit) {
		ArrayList<ArrayList<Object>> result;
		try {
			if (Application.getDB() != null)
				result = Application.getDB().select(sql_all_orgs_limit, new String[]{limit+"",offset+""});
			else result = new ArrayList<ArrayList<Object>>();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return new ArrayList<ArrayList<Object>>();
		}
		return result;
	}
//	/**
//	 * Returns the org LIDs
//	 * @return
//	 */
//	public static ArrayList<ArrayList<Object>> getListOrgLIDs() {
//		final String sql_orgs = "SELECT "+
//				table.organization.organization_ID
//				//+","+table.organization.certification_methods+","
//				//+table.organization.global_organization_ID_hash+","+table.organization.creator_ID+
//				//  ","+table.organization.blocked+","+table.organization.broadcasted+","+table.organization.requested
//				+" FROM "+table.organization.TNAME+";";
//		try {
//			ArrayList<ArrayList<Object>> orgs = Application.db.select(sql_orgs, new String[]{},DEBUG);
//			return orgs;
//		} catch (P2PDDSQLException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

	Hashtable<Integer, Long> countNews_memory = new Hashtable<Integer, Long>();
	/**
	 * With cache
	 * 
	 * @param days
	 * @param refresh
	 * @return
	 */
	public long getCountNews_WithCache(int days, boolean refresh) {
		if (! refresh) {
			Long r = countNews_memory.get(new Integer(days));
			if (r != null) return r;
		}
		long result = D_News.getCount(this.getLIDstr_forced(), days);
		countNews_memory.put(new Integer(days), new Long(result));
		return result;
	}
	
	Hashtable<Integer, Long> orgActivity_memory = new Hashtable<Integer, Long>();
	/**
	 * With cache
	 * 
	 * @param days
	 * @param refresh
	 * @return
	 */
	public long getCountActivity_WithCache(int days, boolean refresh) {
		if (! refresh) {
			Long r = orgActivity_memory.get(new Integer(days));
			if (r != null) return r;
		}
		long result = D_Vote.getOrgCount(this.getLIDstr_forced(), days);
		orgActivity_memory.put(new Integer(days), new Long(result));
		return result;
	}
	
	/**
	 * A cache of the number of constituents in this organization
	 */
	Long orgConstituentsNB_memory = null;
	/**
	 * With cache
	 * 
	 * @param days
	 * @param refresh
	 * @return
	 */
	public Object getConstNBinOrganization_WithCache(boolean refresh) {
		if (! refresh) {
			Long r = orgConstituentsNB_memory;
			if (r != null) return r;
		}
		Object result = D_Constituent.getConstNBinOrganization(this.getLIDstr_forced());
		orgConstituentsNB_memory = Util.Lval(result);
		return result;
	}
	/**
	 * TODO
	 * This does not yet have cached statistics.
	 */
	public void resetCache() {
		// TODO Auto-generated method stub
		
	}

	public String getName() {
		return name;
	}
	public void setName(String name2) {
		this.name = name2;
		dirty_main = true;
	}
	/**
	 * Returns false if the image is larger than DD.MAX_ORG_ICON_LENGTH
	 * 
	 * @param icon
	 * @return
	 */
	public boolean setIcon(byte[] icon) {
		if (icon != null && icon.length > DD.MAX_ORG_ICON_LENGTH) return false;
		if (icon == null && this.params.icon == null) return true; // no dirty
		this.params.icon = icon;
		dirty_main = true;
		return true;
	}
	/**
	 * Sets an object with at least an image, a url, or a non-negative is
	 * @param _icon
	 * @return
	 */
	public boolean setIconObject(IconObject _icon) {
		if (_icon == null || _icon.empty()) {
			return setIcon(null);
		}
		return setIcon(_icon.encode());
	}
	public IconObject getIconObject() {
		IconObject ic = new IconObject();
		if (params.icon == null) return ic;
		try {
			Decoder dec = new Decoder(params.icon);
			if (dec.getTypeByte() == IconObject.getASN1Type()) {
				ic.decode(dec);
				if (! ic.empty()) {
					return ic;
				}
			}
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		ic.setImage(params.icon);
		return ic;
	}
	public byte[] getIcon() {
		if (params.icon == null) return null;
		try {
			Decoder dec = new Decoder(params.icon);
			if (dec.getTypeByte() == IconObject.getASN1Type()) {
				IconObject ic = new IconObject().decode(dec);
				if (! ic.empty()) {
					return ic.getImage();
				}
			}
		} catch (ASN1DecoderFail e) {
			e.printStackTrace();
		}
		return params.icon;
	}
//	public int getIconID() {
//		if (params.icon == null) return -1;
//		try {
//			Decoder dec = new Decoder(params.icon);
//			if (dec.getTypeByte() == IconClass.getASN1Type()) {
//				IconClass ic = new IconClass().decode(dec);
//				if (! ic.empty()) {
//					return ic.id;
//				}
//			}
//		} catch (ASN1DecoderFail e) {
//			e.printStackTrace();
//		}
//		return -1;
//	}
//	public String getIconURL() {
//		if (params.icon == null) return null;
//		try {
//			Decoder dec = new Decoder(params.icon);
//			if (dec.getTypeByte() == IconClass.getASN1Type()) {
//				IconClass ic = new IconClass().decode(dec);
//				if (! ic.empty()) {
//					return ic.url;
//				}
//			}
//		} catch (ASN1DecoderFail e) {
//			e.printStackTrace();
//		}
//		return null;
//	}

	public static int getNumberItemsNeedSaving() {
		return _need_saving.size() + _need_saving_obj.size();
	}
	/**
	 * Returns the new value
	 * @param global_organization_IDhash
	 * @return
	 */
	public String setGIDH(String global_organization_IDhash) {
		this.global_organization_IDhash = global_organization_IDhash;
		return global_organization_IDhash;
	}
	/**
	 * Returns the new value
	 * @param global_organization_ID
	 * @return
	 */
	public String setGID(String global_organization_ID) {
		this.global_organization_ID = global_organization_ID;
		return global_organization_ID;
	}
	public static void stopSaver() {
		saverThread.turnOff();
	}
}

class D_Organization_SaverThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	//private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Organization_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	public void turnOff() {
		stop = true;
		this.interrupt();
	}
	D_Organization_SaverThread() {
		super("D_Organization Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			if (net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverThreads() < SaverThreadsConstants.MAX_NUMBER_CONCURRENT_SAVING_THREADS && D_Organization.getNumberItemsNeedSaving() > 0)
			synchronized(saver_thread_monitor) {
				new D_Organization_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Organization de = D_Organization.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Organization_Saver: loop saving "+de);
					ThreadsAccounting.ping("Saving");
					D_Peer.need_saving_remove(de.getGIDH(), de.instance);
					// try 3 times to save
					for (int k = 0; k < 3; k++) {
						try {
							de.storeAct();
							break;
						} catch (P2PDDSQLException e) {
							e.printStackTrace();
							synchronized(this){
								try {
									wait(SAVER_SLEEP_ON_ERROR);
								} catch (InterruptedException e2) {
									e2.printStackTrace();
								}
							}
						}
					}
				} else {
					ThreadsAccounting.ping("Nothing to do!");
					//System.out.println("D_Organization_Saver: idle ...");
				}
			}
			*/
			synchronized(this) {
				try {
					long timeout = (D_Organization.getNumberItemsNeedSaving() > 0)?
							SaverThreadsConstants.SAVER_SLEEP_BETWEEN_ORGANIZATION_MSEC:
								SaverThreadsConstants.SAVER_SLEEP_WAITING_ORGANIZATION_MSEC;
					wait(timeout);
				} catch (InterruptedException e) {
					//e.printStackTrace();
				}
			}
		}
	}
}

class D_Organization_SaverThreadWorker extends net.ddp2p.common.util.DDP2P_ServiceThread {
	boolean stop = false;
	// public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Organization_SaverThreadWorker() {
		super("D_Organization Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_orgs ++;}
		try{__run();} catch (Exception e){}
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_orgs --;}
	}
	@SuppressWarnings("unused")
	public void __run() {
		synchronized(D_Organization_SaverThread.saver_thread_monitor) {
			D_Organization de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Organization_Saver: start");
			
			 // first try objects being edited
			de = D_Organization.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Organization.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Organization_Saver: loop saving "+de.getName());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Organization.need_saving_obj_remove(de);
				else D_Organization.need_saving_remove(de.getGIDH_or_guess());//, de.instance);
				if (DEBUG) System.out.println("D_Organization_Saver: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < SaverThreadsConstants.ATTEMPTS_ON_ERROR; k++) {
					try {
						if (DEBUG) System.out.println("D_Organization_Saver: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Organization_Saver: stored org:"+de.getName());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Organization_Saver: sleep");
								wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_ORGANIZATION_ON_ERROR);
								if (DEBUG) System.out.println("D_Organization_Saver: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Organization_Saver: idle ...");
			}
		}
		if (SaverThreadsConstants.SAVER_SLEEP_WORKER_BETWEEN_ORGANIZATION_MSEC >= 0) {
			synchronized(this) {
				try {
					if (DEBUG) System.out.println("D_Organization_Saver: sleep");
					wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_BETWEEN_ORGANIZATION_MSEC);
					if (DEBUG) System.out.println("D_Organization_Saver: waked");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

