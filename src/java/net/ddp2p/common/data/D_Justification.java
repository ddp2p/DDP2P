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

import static java.lang.System.out;
import static net.ddp2p.common.util.Util.__;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import net.ddp2p.ASN1.ASN1DecoderFail;
import net.ddp2p.ASN1.ASNObj;
import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;
import net.ddp2p.ciphersuits.SK;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Motion.D_Motion_Node;
import net.ddp2p.common.hds.ASNSyncPayload;
import net.ddp2p.common.streaming.RequestData;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node;
import net.ddp2p.common.util.DDP2P_DoubleLinkedList_Node_Payload;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

/**
 D_JUSTIFICATION ::= SEQUENCE {
global_justificationID PrintableString,
	justification_title [0] Title_Document  OPTIONAL,
	justification_text [1] Document  OPTIONAL,
	author [2] D_Constituent OPTIONAL
	date GeneralizedDate,
	signature OCTET_STRING,
}
 */

public class D_Justification extends ASNObj implements  DDP2P_DoubleLinkedList_Node_Payload<D_Justification>, net.ddp2p.common.util.Summary {
	private static boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	private static final byte TAG = Encoder.TAG_SEQUENCE;
	private static final String V0 = "0";
	private static final String V1 = "1";
	
	private String hash_alg = V1;
	private String global_justificationID;//Printable
	private String global_motionID;//Printable
	private String global_answerTo_ID;//Printable
	private String global_constituent_ID;//Printable
	private String global_organization_ID;//Printable
	private D_Document_Title justification_title = new D_Document_Title();
	private D_Document justification_text = new D_Document();
	//public D_Constituent author;
	private Calendar last_reference_date; //not to be sent
	private Calendar creation_date;
	private byte[] signature; //OCT STR
	private Calendar arrival_date;
	private Calendar preferences_date;
	
	private D_Organization organization;
	private D_Motion motion;
	private D_Constituent constituent;
	private D_Justification answerTo;
	
	private String justification_ID;
	private String motion_ID;
	private String answerTo_ID;
	private String constituent_ID;
	private String organization_ID;
	
	private long peer_source_ID;
	private boolean hidden = false;
	private boolean temporary = true;
	private boolean requested = false;
	private boolean blocked = false;
	private boolean broadcasted = D_Organization.DEFAULT_BROADCASTED_ORG;
	private int status_references = 0;
	
	// temporary stored only in memory
	boolean loaded_globals = false;
	boolean dirty_main = false;
	boolean dirty_mydata = false;
	boolean dirty_preferences = false;
	boolean dirty_local = false;
		
	private static Object monitor_object_factory = new Object();
	D_Justification_Node component_node = new D_Justification_Node(null, null);
	
	@Override
	public String toSummaryString() {
		return "D_Justification["+getLIDstr()+"/"+isTemporary()+"]:" +
				"\n hash_alg="+getHashAlg()+
				"\n global_justificationID="+getGID()+
				"\n global_motionID=["+this.getMotionLIDstr()+"]"+getMotionGID()+
				"\n global_answerTo_ID=["+this.getAnswerToLIDstr()+"]"+getAnswerTo_GID()+
				"\n global_constituent_ID=["+this.getConstituentLIDstr()+"]"+getConstituentGID()+
				"\n global_organization_ID=["+this.getOrganizationLIDstr()+"]"+this.getOrgGID()+
				"\n last_reference_date="+Encoder.getGeneralizedTime(getLastReferenceDate())+
				"\n creation_date="+Encoder.getGeneralizedTime(getCreationDate())+
				"\n arrival_date="+Encoder.getGeneralizedTime(getArrivalDate())+
				"\n signature="+Util.byteToHexDump(getSignature())+
				"\n justification_title="+getJustificationTitle()+
				"\n justification_text="+getJustificationBody()+
				//"\n author="+author+
//				"\n motion_ID="+getMotionLIDstr()+
//				"\n answerTo_ID="+getAnswerToLIDstr()+
//				"\n constituent_ID="+getConstituentLIDstr()+
				"\n motion="+getMotion()+
				"\n constituent="+getConstituent();
	}
	
	@Override
	public String toString() {
		return "D_Justification["+getLIDstr()+"/"+isTemporary()+"]:" +
				"\n hash_alg="+getHashAlg()+
				"\n global_justificationID="+getGID()+
				"\n global_motionID=["+this.getMotionLIDstr()+"]"+getMotionGID()+
				"\n global_answerTo_ID=["+this.getAnswerToLIDstr()+"]"+getAnswerTo_GID()+
				"\n global_constituent_ID=["+this.getConstituentLIDstr()+"]"+getConstituentGID()+
				"\n global_organization_ID=["+this.getOrganizationLIDstr()+"]"+this.getOrgGID()+
				"\n last_reference_date="+Encoder.getGeneralizedTime(getLastReferenceDate())+
				"\n creation_date="+Encoder.getGeneralizedTime(getCreationDate())+
				"\n arrival_date="+Encoder.getGeneralizedTime(getArrivalDate())+
				"\n signature="+Util.byteToHexDump(getSignature())+
				"\n justification_title="+getJustificationTitle()+
				"\n justification_text="+getJustificationBody()+
				//"\n author="+author+
//				"\n motion_ID="+getMotionLIDstr()+
//				"\n answerTo_ID="+getAnswerToLIDstr()+
//				"\n constituent_ID="+getConstituentLIDstr()+
				"\n motion="+getMotion()+
				"\n constituent="+getConstituent();
	}

	public static class D_Justification_Node {
		private static final int MAX_TRIES = 30;
		/** Currently loaded peers, ordered by the access time*/
		private static DDP2P_DoubleLinkedList<D_Justification> loaded_objects = new DDP2P_DoubleLinkedList<D_Justification>();
		private static Hashtable<Long, D_Justification> loaded_By_LocalID = new Hashtable<Long, D_Justification>();
		//private static Hashtable<String, D_Justification> loaded_const_By_GID = new Hashtable<String, D_Justification>();
		//private static Hashtable<String, D_Justification> loaded_const_By_GIDhash = new Hashtable<String, D_Justification>();
		private static Hashtable<String, Hashtable<Long, D_Justification>> loaded_By_GIDH_ORG = new Hashtable<String, Hashtable<Long, D_Justification>>();
		private static Hashtable<Long, Hashtable<String, D_Justification>> loaded_By_ORG_GIDH = new Hashtable<Long, Hashtable<String, D_Justification>>();
		private static Hashtable<String, Hashtable<Long, D_Justification>> loaded_By_GID_ORG = new Hashtable<String, Hashtable<Long, D_Justification>>();
		private static Hashtable<Long, Hashtable<String, D_Justification>> loaded_By_ORG_GID = new Hashtable<Long, Hashtable<String, D_Justification>>();
		private static long current_space = 0;
		
		//return loaded_const_By_GID.get(GID);
		private static D_Justification getByGID(String GID, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Justification> t1 = loaded_By_ORG_GID.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GID);
			}
			Hashtable<Long, D_Justification> t2 = loaded_By_GID_ORG.get(GID);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		//return loaded_const_By_GID.put(GID, c);
		private static D_Justification putByGID(String GID, Long organizationLID, D_Justification c) {
			Hashtable<Long, D_Justification> v1 = loaded_By_GID_ORG.get(GID);
			if (v1 == null) loaded_By_GID_ORG.put(GID, v1 = new Hashtable<Long, D_Justification>());

			// section added for duplication control
			D_Justification old = v1.get(organizationLID);
			if (old != null && old != c) {
				Util.printCallPath("D_Justification conflict: old="+old+" crt="+c);
				return old;
			}
			
			D_Justification result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Justification> v2 = loaded_By_ORG_GID.get(organizationLID);
			if (v2 == null) loaded_By_ORG_GID.put(organizationLID, v2 = new Hashtable<String, D_Justification>());
			
			// section added for duplication control
			old = v2.get(GID);
			if (old != null && old != c) {
				Util.printCallPath("D_Justification conflict: old="+old+" crt="+c);
				//return old; // in this case, for consistency, store the new one
			}
			
			D_Justification result2 = v2.put(GID, c);
			
			//if (result == null) result = result2;
			return c; //result; 
		}
		//return loaded_const_By_GID.remove(GID);
		private static D_Justification remByGID(String GID, Long organizationLID) {
			D_Justification result = null;
			D_Justification result2 = null;
			Hashtable<Long, D_Justification> v1 = loaded_By_GID_ORG.get(GID);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_By_GID_ORG.remove(GID);
			}
			
			Hashtable<String, D_Justification> v2 = loaded_By_ORG_GID.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GID);
				if (v2.size() == 0) loaded_By_ORG_GID.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Justification getConstByGID(String GID, String organizationLID) {
//			return getConstByGID(GID, Util.Lval(organizationLID));
//		}
		
		private static D_Justification getByGIDH(String GIDH, Long organizationLID) {
			if (organizationLID != null && organizationLID > 0) {
				Hashtable<String, D_Justification> t1 = loaded_By_ORG_GIDH.get(organizationLID);
				if (t1 == null || t1.size() == 0) return null;
				return t1.get(GIDH);
			}
			Hashtable<Long, D_Justification> t2 = loaded_By_GIDH_ORG.get(GIDH);
			if ((t2 == null) || (t2.size() == 0)) return null;
			if ((organizationLID != null) && (organizationLID > 0))
				return t2.get(organizationLID);
			for (Long k : t2.keySet()) {
				return t2.get(k);
			}
			return null;
		}
		private static D_Justification putByGIDH(String GIDH, Long organizationLID, D_Justification c) {
			Hashtable<Long, D_Justification> v1 = loaded_By_GIDH_ORG.get(GIDH);
			if (v1 == null) loaded_By_GIDH_ORG.put(GIDH, v1 = new Hashtable<Long, D_Justification>());

			// section added for duplication control
			D_Justification old = v1.get(organizationLID);
			if (old != null && old != c) {
				Util.printCallPath("D_Justification conflict: old="+old+" crt="+c);
				return old;
			}
			
			D_Justification result = v1.put(organizationLID, c);
			
			Hashtable<String, D_Justification> v2 = loaded_By_ORG_GIDH.get(organizationLID);
			if (v2 == null) loaded_By_ORG_GIDH.put(organizationLID, v2 = new Hashtable<String, D_Justification>());
			
			// section added for duplication control
			old = v2.get(GIDH);
			if (old != null && old != c) {
				Util.printCallPath("D_Justification conflict: old="+old+" crt="+c);
				//return old; // in this case, for consistency, store the new one
			}
			
			D_Justification result2 = v2.put(GIDH, c);
			
			//if (result == null) result = result2;
			return c; //result; 
		}
		//return loaded_const_By_GIDhash.remove(GIDH);
		private static D_Justification remByGIDH(String GIDH, Long organizationLID) {
			D_Justification result = null;
			D_Justification result2 = null;
			Hashtable<Long, D_Justification> v1 = loaded_By_GIDH_ORG.get(GIDH);
			if (v1 != null) {
				result = v1.remove(organizationLID);
				if (v1.size() == 0) loaded_By_GIDH_ORG.remove(GIDH);
			}
			
			Hashtable<String, D_Justification> v2 = loaded_By_ORG_GIDH.get(organizationLID);
			if (v2 != null) {
				result2 = v2.remove(GIDH);
				if (v2.size() == 0) loaded_By_ORG_GIDH.remove(organizationLID);
			}
			
			if (result == null) result = result2;
			return result; 
		}
//		private static D_Justification getConstByGIDH(String GIDH, String organizationLID) {
//			return getConstByGIDH(GIDH, Util.Lval(organizationLID));
//		}
		
		/** message is enough (no need to store the Encoder itself) */
		public byte[] message;
		public DDP2P_DoubleLinkedList_Node<D_Justification> my_node_in_loaded;
	
		public D_Justification_Node(byte[] message,
				DDP2P_DoubleLinkedList_Node<D_Justification> my_node_in_loaded) {
			this.message = message;
			this.my_node_in_loaded = my_node_in_loaded;
		}
		private static void register_fully_loaded(D_Justification crt) {
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
		/**
		 * This function is used to link an object by its LID when this is obtained
		 * by storing an object already linked by its GIDH (if it was linked)
		 * @param crt
		 * @return
		 * true if i is linked and false if it is not
		 */
		private static boolean register_newLID_ifLoaded(D_Justification crt) {
			if (DEBUG) System.out.println("D_Justification: register_newLID_ifLoaded: start crt = "+crt);
			synchronized (loaded_objects) {
				//String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				if (gidh == null) {
					if (_DEBUG) { System.out.println("D_Justification: register_newLID_ifLoaded: had no gidh! no need of this call.");
					Util.printCallPath("Path");}
					return false;
				}
				if (lid <= 0) {
					Util.printCallPath("Why call without LID="+crt);
					return false;
				}
				
				Long organizationLID = crt.getOrganizationLID();
				if (organizationLID <= 0) {
					Util.printCallPath("No orgLID="+crt);
					return false;
				}
				D_Justification old = getByGIDH(gidh, organizationLID);
				if (old == null) {
					if (DEBUG) System.out.println("D_Justification: register_newLID_ifLoaded: was not registered.");
					return false;
				}
				
				if (old != crt)	{
					Util.printCallPath("Different linking of: old="+old+" vs crt="+crt);
					return false;
				}
				
				Long oLID = new Long(lid);
				D_Justification _old = loaded_By_LocalID.get(oLID);
				if (_old != null && _old != crt) {
					Util.printCallPath("Double linking of: old="+_old+" vs crt="+crt);
					return false;
				}
				loaded_By_LocalID.put(oLID, crt);
				if (DEBUG) System.out.println("D_Justification: register_newLID_ifLoaded: store lid="+lid+" crt="+crt.getGIDH());

				return true;
			}
		}
		private static boolean register_newGID_ifLoaded(D_Justification crt) {
			if (DEBUG) System.out.println("D_Justification: register_newGID_ifLoaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_objects) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				if (gidh == null) {
					Util.printCallPath("Why call without GIDH="+crt);
					return false;
				}
				if (lid <= 0) {
					if (_DEBUG) { System.out.println("D_Justification: register_newGID_ifLoaded: had no lid! no need of this call.");
					Util.printCallPath("Path");}
					return false;
				}
				
				Long organizationLID = crt.getOrganizationLID();
				if (organizationLID <= 0) {
					Util.printCallPath("No orgLID="+crt);
					return false;
				}
				
				Long oLID = new Long(lid);
				D_Justification _old = loaded_By_LocalID.get(oLID);
				if (_old == null) {
					if (DEBUG) System.out.println("D_Motion: register_newGID_ifLoaded: was not loaded");
					return false;
				}
				if (_old != null && _old != crt) {
					Util.printCallPath("Using expired: old="+_old+" vs crt="+crt);
					return false;
				}
				
				D_Justification_Node.putByGID(gid, crt.getOrganizationLID(), crt); //loaded_const_By_GID.put(gid, crt);
				if (DEBUG) System.out.println("D_Justification: register_newGID_ifLoaded: store gid="+gid);
				D_Justification_Node.putByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
				if (DEBUG) System.out.println("D_Justification: register_newGID_ifLoaded: store gidh="+gidh);
				
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				if (DEBUG) System.out.println("D_Justification: register_newGID_ifLoaded: store lid="+lid+" crt="+crt.getGIDH());

				return true;
			}
		}
		/*
		private static void unregister_loaded(D_Justification crt) {
			synchronized(loaded_orgs) {
				loaded_orgs.remove(crt);
				loaded_org_By_LocalID.remove(new Long(crt.getLID()));
				loaded_org_By_GID.remove(crt.getGID());
				loaded_org_By_GIDhash.remove(crt.getGIDH());
			}
		}
		*/
		private static boolean register_loaded(D_Justification crt) {
			if (DEBUG) System.out.println("D_Justification: register_loaded: start crt = "+crt);
			crt.reloadMessage(); 
			synchronized (loaded_objects) {
				String gid = crt.getGID();
				String gidh = crt.getGIDH();
				long lid = crt.getLID();
				Long organizationLID = crt.getOrganizationLID();
				
				loaded_objects.offerFirst(crt);
				if (lid > 0) {
					// section added for duplication control
					Long oLID = new Long(lid);
					D_Justification old = loaded_By_LocalID.get(oLID);
					if (old != null && old != crt) {
						Util.printCallPath("Double linking of: old="+old+" vs crt="+crt);
						return false;
					}
					
					loaded_By_LocalID.put(oLID, crt);
					if (DEBUG) System.out.println("D_Justification: register_loaded: store lid="+lid+" crt="+crt.getGIDH());
				}else{
					if (DEBUG) System.out.println("D_Justification: register_loaded: no store lid="+lid+" crt="+crt.getGIDH());
				}
				if (gid != null) {
					D_Justification_Node.putByGID(gid, crt.getOrganizationLID(), crt); //loaded_const_By_GID.put(gid, crt);
					if (DEBUG) System.out.println("D_Justification: register_loaded: store gid="+gid);
				} else {
					if (DEBUG) System.out.println("D_Justification: register_loaded: no store gid="+gid);
				}
				if (gidh != null) {
					D_Justification_Node.putByGIDH(gidh, organizationLID, crt);//loaded_const_By_GIDhash.put(gidh, crt);
					if (DEBUG) System.out.println("D_Justification: register_loaded: store gidh="+gidh);
				} else {
					if (DEBUG) System.out.println("D_Justification: register_loaded: no store gidh="+gidh);
				}
				if (crt.component_node.message != null) current_space += crt.component_node.message.length;
				
				int tries = 0;
				while ((loaded_objects.size() > SaverThreadsConstants.MAX_LOADED_JUSTIFICATIONS)
						|| (current_space > SaverThreadsConstants.MAX_JUSTIFICATIONS_RAM)) {
					if (loaded_objects.size() <= SaverThreadsConstants.MIN_LOADED_JUSTIFICATIONS) break; // at least _crt_peer and _myself
	
					if (tries > MAX_TRIES) break;
					tries ++;
					D_Justification candidate = loaded_objects.getTail();
					if ((candidate.status_references > 0)
							//||
							//D_Justification.is_crt_const(candidate)
							//||
							//(candidate == HandlingMyself_Peer.get_myself())
							) 
					{
						setRecent(candidate);
						continue;
					}
					
					D_Justification removed = loaded_objects.removeTail();//remove(loaded_peers.size()-1);
					if (removed.getLID() > 0) loaded_By_LocalID.remove(new Long(removed.getLID())); 
					if (removed.getGID() != null) D_Justification_Node.remByGID(removed.getGID(), removed.getOrganizationLID());//loaded_const_By_GID.remove(removed.getGID());
					if (removed.getGIDH() != null) D_Justification_Node.remByGIDH(removed.getGIDH(), removed.getOrganizationLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
					if (DEBUG) System.out.println("D_Justification: register_loaded: remove GIDH="+removed.getGIDH());
					if (removed.component_node.message != null) current_space -= removed.component_node.message.length;				
				}
			}
			return true;
		}
		/**
		 * Move this to the front of the list of items (tail being trimmed)
		 * @param crt
		 */
		private static void setRecent(D_Justification crt) {
			loaded_objects.moveToFront(crt);
		}
		public static boolean dropLoaded(D_Justification removed, boolean force) {
			boolean result = true;
			synchronized(loaded_objects) {
				if (removed.status_references > 0 || removed.get_DDP2P_DoubleLinkedList_Node() == null)
					result = false;
				if (! force && ! result) {
					System.out.println("D_Justification: dropLoaded: abandon: force="+force+" rem="+removed);
					return result;
				}
				
				if (loaded_objects.inListProbably(removed)) {
					try {
						loaded_objects.remove(removed); // Exception if not loaded
						if (removed.component_node.message != null) current_space -= removed.component_node.message.length;	
						if (DEBUG) System.out.println("D_Justification: dropLoaded: exit with force="+force+" result="+result);
					} catch (Exception e) {
						if (_DEBUG) e.printStackTrace();
					}
				}
				if (removed.getLIDstr() != null) loaded_By_LocalID.remove(new Long(removed.getLID())); 
				if (removed.getGID() != null) D_Justification_Node.remByGID(removed.getGID(), removed.getOrganizationLID()); //loaded_const_By_GID.remove(removed.getGID());
				if (removed.getGIDH() != null) D_Justification_Node.remByGIDH(removed.getGIDH(), removed.getOrganizationLID()); //loaded_const_By_GIDhash.remove(removed.getGIDH());
				if (DEBUG) System.out.println("D_Justification: drop_loaded: remove GIDH="+removed.getGIDH());
				return result;
			}
		}
	}
	static class D_Justification_My {
		String name;// table.my_organization_data.
		String creator;
		String category;
		long row;
	}
	D_Justification_My mydata = new D_Justification_My();	
	
	public static D_Justification getEmpty() {return new D_Justification();}
	public D_Justification instance() throws CloneNotSupportedException{return new D_Justification();}
	
	private D_Justification() {}
	private D_Justification(Long lID, boolean load_Globals) throws P2PDDSQLException {
		try {
			init(lID);
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	private D_Justification(String gID, boolean load_Globals, boolean create,
			D_Peer __peer, long p_oLID, long p_mLID, ArrayList<ArrayList<Object>> ma) throws P2PDDSQLException {
		try {
			if (ma != null && ma.size() > 0) init(ma.get(0));
			else init(gID);
			
			if (load_Globals) this.fillGlobals();
		} catch (Exception e) {
			//e.printStackTrace();
			if (create) {
				if (DEBUG) System.out.println("D_Justification: <init>: create gid="+gID);
				initNew(gID, __peer, p_oLID, p_mLID);
				if (DEBUG) System.out.println("D_Justification: <init>: got gid obj="+this);
				return;
			}
			throw new P2PDDSQLException(e.getLocalizedMessage());
		}
	}
	/**
	 * This may be called with a justification that may or may not have been registered.
	 * Even if it is new, still a new justification object is created and this is loaded in it.
	 * 
	 * Only global elements are set in it (with loadRemote).
	 * The GID is recomputed.
	 * 
	 * Also sets temporary, broadcasted, and arrival_date.
	 * 
	 * Not recommended for parameters that are known to be really new.
	 * @param _unknown
	 * @return
	 */
	public static D_Justification createFinalJustification_FromUnknown(D_Justification _unknown) {
		if (DEBUG) System.out.println("D_Justification: createJustification_FromUnknown: start " + _unknown);

		long oLID = _unknown.getOrganizationLID();
		if (oLID <= 0 ) D_Organization.getLIDbyGID(_unknown.getOrgGID());
		if (oLID <= 0 ) {
			if (_DEBUG) System.out.println("D_Justification: createJustification_FromUnknown: why null org registering " + _unknown);
			return null;
		};
		
		long m_LID = _unknown.getMotionLID();
		if (m_LID <= 0 ) D_Motion.getLIDFromGID(_unknown.getMotionGID(), oLID);
		if (m_LID <= 0 ) {
			if (_DEBUG) System.out.println("D_Justification: createJustification_FromUnknown: why null motion registering " + _unknown);
			return null;
		};
		
		
		String GID = _unknown.getGID();
		if (GID == null) GID = _unknown.make_ID();
		
		D_Justification m = D_Justification.getJustByGID(GID, true, true, true, null, oLID, m_LID, null);
		if (m != _unknown) {
			m.loadRemote(_unknown, null, null, null);
			//_unknown = (m);
		}
		m.setTemporary(false);
		
		m.setBroadcasted(true); // if you sign it, you probably want to broadcast it...
		m.setArrivalDate();
		long j_id = m.storeRequest_getID();
		//if (m.dirty_any()) m.storeRequest();
		m.releaseReference();
		
		if (DEBUG) System.out.println("D_Justification: createJustification_FromUnknown: obtained " + m);
		return m;
	}
	/**
	 * Get an anonymous justification for known motion.
	 * Uses TXT document formats.
	 * Return it signed, stored and registered.
	 * @param _motion
	 * @param _title
	 * @param _body
	 * @return
	 */
	public static D_Justification createJustification(
			D_Motion _motion, 
			String _title, 
			String _body, 
			D_Justification _answered) {
		return createJustification(_motion, _title, D_Document.TXT_FORMAT, _body, D_Document.TXT_FORMAT, _answered, null, null);
	}
	/**
	 * Get a justification for known motion.
	 * Return it signed, stored and registered.
	 * 
	 * @param _motion
	 * @param _title
	 * @param _title_format (like D_Document.TXT_FORMAT)
	 * @param _body
	 * @param _body_format
	 * @param _answered
	 * @param _enhanced (to be used when supporting enhancements)
	 * @param signer (set to null for anonymous)
	 * @return
	 */
	public static D_Justification createJustification(
			D_Motion _motion, 
			String _title, 
			String _title_format, 
			String _body, 
			String _body_format, 
			D_Justification _answered,
			D_Justification _enhanced,
			D_Constituent signer
			) {
		D_Justification new_justification = D_Justification.getEmpty();
		
		new_justification.setOrgGID(_motion.getOrganizationGID_force());
		new_justification.setMotionGID(_motion.getGID());
		if (_answered != null)
			new_justification.setAnswerTo_SetAll(_answered);
		new_justification.setJustificationTitleTextAndFormat(_title, _title_format);
		if (_body != null)
			new_justification.setJustificationBodyTextAndFormat(_body, _body_format);
		if (signer != null) new_justification.setConstituentAll(signer);
		else new_justification.setConstituentAll(null);
		
//		D_Document_Title title = new D_Document_Title();
//		title.title_document = new D_Document();
//		title.title_document.setDocumentString(_title);
//		title.title_document.setFormatString(_title_format);
//		new_justification.setJustificationTitle(title);		
//		if (_body != null) {
//			D_Document body_d = new D_Document();
//			body_d.setDocumentString(_body);
//			body_d.setFormatString(_title_format);
//			new_justification.setJustificationText(body_d);
//		}
		
		return createFinalJustificationFromNew(new_justification);
	}
	/**
	 * Get a justification for known motion.
	 * 
	 * First extracts the object for motion and for answer_To. Then sets back LIDs and GIDs in _new (in case one was absent).
	 * 
	 * Recompute the GID, and then store it via loadRemote (if another object was returned at registration).
	 * 
	 * And by setting temporary, arrival time, and broadcasted.
	 * 
	 * @param _new
	 * @return
	 */
	public static D_Justification createFinalJustificationFromNew(D_Justification _new) {
		if (DEBUG) System.out.println("D_Justification: createJustification_FromNew: start " + _new);
		long oLID = _new.getOrganizationLID();
		D_Motion _motion = null;
		if (_new.getMotionLIDstr() != null)
			_motion = D_Motion.getMotiByLID(_new.getMotionLIDstr(), true, false);
		if (_motion == null && _new.getMotionGID() != null) {
			_motion = D_Motion.getMotiByGID(_new.getMotionGID(), true, false, oLID);
		}
		if (_motion == null) {
			if (_DEBUG) System.out.println("D_Justification: createJustificationFromNew: why null motion registering " + _new);
			return null;
		}
		long m_LID = _motion.getLID_force();
		
		if (oLID <= 0) oLID = _motion.getOrganizationLID();
		if (oLID <= 0) {
			if (_DEBUG) System.out.println("D_Justification: createJustification_FromUnknown: null org in " + _new);
			return null;
		}

		
		D_Justification _answered = null;
		if (_new.getAnswerToLIDstr() != null)
			_answered = D_Justification.getJustByLID(_new.getAnswerToLIDstr(), true, false);
		if (_answered == null && _new.getAnswerTo_GID() != null) {
			_answered = D_Justification.getJustByGID(_new.getAnswerTo_GID(), true, false, _new.getOrganizationLID(), m_LID);
		}
		
		
		//D_Justification new_justification = D_Justification.getEmpty();
		
		_new.setOrgGID(_motion.getOrganizationGID_force());
		_new.setOrganizationLID(_motion.getOrganizationLID());
		_new.setMotionGID(_motion.getGID());
		_new.setMotionLID(_motion.getLID());
		
		if (_answered != null) {
			_new.setAnswerTo_GID(_answered.getGID());
			_new.setAnswerToLIDstr(_answered.getLIDstr());
			_new.setAnswerTo_SetAll(_answered);
		}
		
		if (_new.getConstituentLID() > 0) {
			_new.setConstituentAll(D_Constituent.getConstByLID(_new.getConstituentLID(), true, false));
		}
		
		/*
		D_Document_Title title = new D_Document_Title();
		String _title = _new.getJustificationTitle().title_document.getDocumentUTFString();
		title.title_document.setDocumentString(_title);
		title.title_document = new D_Document();
		title.title_document.setFormatString(_new.getJustificationTitle().title_document.getFormatString());//D_Document.TXT_FORMAT);
		new_justification.setJustificationTitle(title);
		
		String _body = _new.getJustificationText().getDocumentUTFString();
		if (_body != null) {
			D_Document body_d = new D_Document();
			body_d.setDocumentString(_body);
			body_d.setFormatString(_new.getJustificationText().getFormatString()); //D_Document.TXT_FORMAT);
			new_justification.setJustificationText(body_d);
		}
		*/
		
		_new.setTemporary(false);
		_new.setGID();
		
		// D_Justification.DEBUG = true;
		String GID = _new.getGID();
		
		if (_new.getConstituentLIDstr() != null)
			_new.sign();
		
		D_Justification m = D_Justification.getJustByGID(GID, true, true, true, null, oLID, m_LID, _new);
		if (m != _new) {
			if (_DEBUG) System.out.println("D_Justification: createJustificationFromNew: why registering preexisting " + _new);
			m.loadRemote(_new, null, null, null);
			//new_justification = (m);
		}
		m.setTemporary(false);
		
		m.setBroadcasted(true); // if you sign it, you probably want to broadcast it...
		m.setArrivalDate();
		long j_id = m.storeRequest_getID();
		//if (new_justification.dirty_any()) new_justification.storeRequest();
		m.releaseReference();
		
		if (DEBUG) System.out.println("D_Justification: createJustification_FromNew: obtained " + m);
		return m;
	}
	
	void init(ArrayList<Object> o) throws P2PDDSQLException {
		this.hash_alg = Util.getString(o.get(net.ddp2p.common.table.justification.J_HASH_ALG));
		this.global_justificationID = Util.getString(o.get(net.ddp2p.common.table.justification.J_GID));
		this.justification_title.title_document.setFormatString(Util.getString(o.get(net.ddp2p.common.table.justification.J_TITLE_FORMAT)));	
		this.justification_title.title_document.setDocumentString(Util.getString(o.get(net.ddp2p.common.table.justification.J_TITLE)));
		this.justification_text.setFormatString(Util.getString(o.get(net.ddp2p.common.table.justification.J_TEXT_FORMAT)));
		this.justification_text.setDocumentString(Util.getString(o.get(net.ddp2p.common.table.justification.J_TEXT)));
		//this.status = Util.ival(Util.getString(o.get(table.justification.J_STATUS)), DEFAULT_STATUS);
		this.creation_date = Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.justification.J_CREATION)));
		this.preferences_date = Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.justification.J_PREFERENCES_DATE)));
		this.arrival_date = Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.justification.J_ARRIVAL)));
		this.signature = Util.byteSignatureFromString(Util.getString(o.get(net.ddp2p.common.table.justification.J_SIGNATURE)));
		this.motion_ID = Util.getString(o.get(net.ddp2p.common.table.justification.J_MOTION_ID));
		this.constituent_ID = Util.getString(o.get(net.ddp2p.common.table.justification.J_CONSTITUENT_ID));
		this.answerTo_ID = Util.getString(o.get(net.ddp2p.common.table.justification.J_ANSWER_TO_ID));
		this.last_reference_date = Util.getCalendar(Util.getString(o.get(net.ddp2p.common.table.justification.J_REFERENCE)));
		this.justification_ID = Util.getString(o.get(net.ddp2p.common.table.justification.J_ID));
		this.organization_ID = Util.getString(o.get(net.ddp2p.common.table.justification.J_ORG_ID));

		this.peer_source_ID = Util.lval(o.get(net.ddp2p.common.table.justification.J_PEER_SOURCE_ID));
		this.temporary = Util.stringInt2bool(o.get(net.ddp2p.common.table.justification.J_TEMPORARY),false);
		this.hidden = Util.stringInt2bool(o.get(net.ddp2p.common.table.justification.J_HIDDEN),false);
		this.blocked = Util.stringInt2bool(o.get(net.ddp2p.common.table.justification.J_BLOCKED),false);
		this.requested = Util.stringInt2bool(o.get(net.ddp2p.common.table.justification.J_REQUESTED),false);
		this.broadcasted = Util.stringInt2bool(o.get(net.ddp2p.common.table.justification.J_BROADCASTED), D_Organization.DEFAULT_BROADCASTED_ORG_ITSELF);
		
		this.global_constituent_ID = D_Constituent.getGIDFromLID(constituent_ID); //Util.getString(o.get(table.justification.J_FIELDS+0));
		this.global_answerTo_ID = Util.getString(o.get(net.ddp2p.common.table.justification.J_FIELDS+0)); //+2
		//this.global_organization_ID = Util.getString(o.get(table.justification.J_FIELDS+3));
		
		//this.choices = WB_Choice.getChoices(motionID);
		//this.global_motionID = Util.getString(o.get(table.justification.J_FIELDS+0)); // +1
		//this.organization_ID = Util.getString(o.get(table.justification.J_FIELDS+2)); //+3
		//this.global_organization_ID = D_Organization.getGIDbyLIDstr(organization_ID);
		this.motion = D_Motion.getMotiByLID(motion_ID, true, false);
		if (motion != null) {
			this.global_motionID = motion.getGID();
			String _organization_ID = motion.getOrganizationLIDstr();
			if (this.organization_ID == null) this.organization_ID = _organization_ID;
			else if (! Util.equalStrings_null_or_not(_organization_ID, this.organization_ID)) Util.printCallPath("Diif m_oID:"+ _organization_ID+ "vs =" + this);
			this.global_organization_ID = motion.getOrganizationGID_force();
		}
			
		String my_const_sql = "SELECT "+net.ddp2p.common.table.my_justification_data.fields_list+
				" FROM "+net.ddp2p.common.table.my_justification_data.TNAME+
				" WHERE "+net.ddp2p.common.table.my_justification_data.justification_ID+" = ?;";
		ArrayList<ArrayList<Object>> my_org = Application.db.select(my_const_sql, new String[]{getLIDstr()}, DEBUG);
		if (my_org.size() != 0) {
			ArrayList<Object> my_data = my_org.get(0);
			mydata.name = Util.getString(my_data.get(net.ddp2p.common.table.my_justification_data.COL_NAME));
			mydata.category = Util.getString(my_data.get(net.ddp2p.common.table.my_justification_data.COL_CATEGORY));
			mydata.creator = Util.getString(my_data.get(net.ddp2p.common.table.my_justification_data.COL_CREATOR));
			// skipped preferences_date (used from main)
			mydata.row = Util.lval(my_data.get(net.ddp2p.common.table.my_justification_data.COL_ROW));
		}
	
		this.loaded_globals = true;
	}
	
	private D_Justification initNew(String gID, D_Peer __peer, long p_oLID,
			long p_mLID) 
	{
		if (this.global_justificationID != null)
			assert(this.getGID().equals(gID));
		this.setOrganizationLID(p_oLID);
		this.setMotionLID(p_mLID);
		this.setGID(gID); // removed registration
		if (__peer != null) this.setPeerSourceLID((__peer.getLID()));
		this.dirty_main = true;
		this.setTemporary(true);
		return this;
	}
	
	static final String cond_ID = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.justification.fields,"j")+
			", a."+net.ddp2p.common.table.justification.global_justification_ID+
			" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "+
			" LEFT JOIN "+net.ddp2p.common.table.justification.TNAME+" AS a ON(a."+net.ddp2p.common.table.justification.justification_ID+"=j."+net.ddp2p.common.table.justification.answerTo_ID+")"+
			" WHERE j."+net.ddp2p.common.table.justification.justification_ID+"=?;"
			;
	private void init(Long lID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_ID, new String[]{Util.getStringID(lID)});
		if (a.size() == 0) throw new Exception("D_Justification:init:None for lID="+lID);
		init(a.get(0));
		if(DEBUG) System.out.println("D_Justification: init: got="+this);//result);
	}
	static final String cond_GID = 
			"SELECT "+Util.setDatabaseAlias(net.ddp2p.common.table.justification.fields,"j")+
			", a."+net.ddp2p.common.table.justification.global_justification_ID+
			" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "+
			" LEFT JOIN "+net.ddp2p.common.table.justification.TNAME+" AS a ON(a."+net.ddp2p.common.table.justification.justification_ID+"=j."+net.ddp2p.common.table.justification.answerTo_ID+")"+
			" WHERE j."+net.ddp2p.common.table.justification.global_justification_ID+"=?;"
			;
	private void init(String gID) throws Exception {
		ArrayList<ArrayList<Object>> a;
		a = Application.db.select(cond_GID, new String[]{gID});
		if (a.size() == 0) throw new Exception("D_Justification:init:None for GID="+gID);
		init(a.get(0));
		if(DEBUG) System.out.println("D_Justification: init: got="+this);//result);
	}	

	private static ArrayList<ArrayList<Object>> getJustificationArrayByGID(
			String gID) {
		ArrayList<ArrayList<Object>> a;
		try {
			a = Application.db.select(cond_GID, new String[]{gID});
			return a;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return new ArrayList<ArrayList<Object>>();
	}

	/**
	 * exception raised on error
	 * @param ID
	 * @param load_Globals 
	 * @return
	 */
	static public D_Justification getJustByLID_AttemptCacheOnly (long ID, boolean load_Globals) {
		if (ID <= 0) return null;
		Long id = new Long(ID);
		D_Justification crt = D_Justification_Node.loaded_By_LocalID.get(id);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals) {
			crt.fillGlobals();
			D_Justification_Node.register_fully_loaded(crt);
		}
		D_Justification_Node.setRecent(crt);
		return crt;
	}
	/**
	 * exception raised on error
	 * @param LID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static public D_Justification getJustByLID_AttemptCacheOnly(Long LID, boolean load_Globals, boolean keep) {
		if (LID == null) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Justification  crt = getJustByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 1) {
						System.out.println("D_Justification: getJustByGIDhash_AttemptCacheOnly: "+crt.status_references);
						Util.printCallPath("");
					}
				}
				return crt;
			}
		} else {
			return getJustByLID_AttemptCacheOnly(LID.longValue(), load_Globals);
		}
	}

	static public D_Justification getJustByLID(String LID, boolean load_Globals, boolean keep) {
		return getJustByLID(Util.Lval(LID), load_Globals, keep);
	}
	static public D_Justification getJustByLID(Long LID, boolean load_Globals, boolean keep) {
		// boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Justification: getJustByLID: "+LID+" glob="+load_Globals);
		if ((LID == null) || (LID <= 0)) {
			if (DEBUG) System.out.println("D_Justification: getJustByLID: null LID = "+LID);
			if (DEBUG) Util.printCallPath("?");
			return null;
		}
		D_Justification crt = D_Justification.getJustByLID_AttemptCacheOnly(LID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Justification: getJustByLID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Justification.getJustByLID_AttemptCacheOnly(LID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Justification: getJustByLID: got sync cached crt="+crt);
				return crt;
			}

			try {
				crt = new D_Justification(LID, load_Globals);
				if (DEBUG) System.out.println("D_Justification: getJustByLID: loaded crt="+crt);
				D_Justification_Node.register_loaded(crt);
				if (keep) {
					crt.status_references ++;
				}
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Justification: getJustByLID: error loading");
				// e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Justification: getJustByLID: Done");
			return crt;
		}
	}	
	
	/**
	 * No keep
	 * @param GID
	 * @param GIDhash
	 * @param load_Globals 
	 * @return
	 */
	static private D_Justification getJustByGID_AttemptCacheOnly(String GID, Long organizationLID, boolean load_Globals) {
		D_Justification  crt = null;
		if ((GID == null)) return null;
		if ((GID != null)) crt = D_Justification_Node.getByGID(GID, organizationLID); //.loaded_const_By_GID.get(GID);
		
				
		if (crt != null) {
			//crt.setGID(GID, crt.getOrganizationID());
			
			if (load_Globals && !crt.loaded_globals) {
				crt.fillGlobals();
				D_Justification_Node.register_fully_loaded(crt);
			}
			D_Justification_Node.setRecent(crt);
			return crt;
		}
		return null;
	}
	/**
	 * exception raised on error
	 * @param GIDhash
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	/*
	static private D_Justification getOrgByGIDhash_AttemptCacheOnly(String GIDhash, boolean load_Globals, boolean keep) {
		if (GIDhash == null) return null;
		if (keep) {
			synchronized(monitor_object_factory) {
				D_Justification  crt = getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					//System.out.println("D_Organization: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
					//Util.printCallPath("");
				}
				return crt;
			}
		} else {
			return getOrgByGID_or_GIDhash_AttemptCacheOnly(null, GIDhash, load_Globals);
		}
	}
	*/
	/**
	 * exception raised on error
	 * @param GID
	 * @param oID
	 * @param load_Globals 
	 * @param keep : if true, avoid releasing this until calling releaseReference()
	 * @return
	 */
	static private D_Justification getJustByGID_AttemptCacheOnly(String GID, Long oID, boolean load_Globals, boolean keep) {
		if ((GID == null)) return null;
		if (keep) {
			synchronized (monitor_object_factory) {
				D_Justification  crt = getJustByGID_AttemptCacheOnly(GID, oID, load_Globals);
				if (crt != null) {			
					crt.status_references ++;
					if (crt.status_references > 1) {
						System.out.println("D_Justification: getOrgByGIDhash_AttemptCacheOnly: "+crt.status_references);
						//Util.printCallPath("");
						Util.printCallPathTop("Why references increase for justGID="+GID+" in ("+oID+")");
					}
				}
				return crt;
			}
		} else {
			return getJustByGID_AttemptCacheOnly(GID, oID, load_Globals);
		}
	}
	/**
	 * Only attempts too load (only based on GID) if the data is already in the cache
	 * exception raised on error
	 * @param GID
	 * @param load_Globals 
	 * @return
	 */
	/*
	static private D_Justification getOrgByGID_only_AttemptCacheOnly(String GID, boolean load_Globals) {
		if (GID == null) return null;
		D_Justification crt = D_Justification_Node.loaded_org_By_GID.get(GID);
		if (crt == null) return null;
		
		if (load_Globals && !crt.loaded_globals){
			crt.fillGlobals();
			D_Justification_Node.register_fully_loaded(crt);
		}
		D_Justification_Node.setRecent(crt);
		return crt;
	}
	*/
	/**
	 * 
	 * @param GID
	 * @param load_Globals
	 * @param keep
	 * @return
	 */
	@Deprecated
	static public D_Justification getJustByGID(String GID, boolean load_Globals, boolean keep) {
		if (DEBUG) System.out.println("D_Justification: getJustByGID: Remove me setting orgID");
		return getJustByGID(GID, load_Globals, false, keep, null, -1, -1, null);
	}
	/**
	 * 
	 * @param GID
	 * @param load_Globals
	 * @param keep
	 * @param oID
	 * @param mID
	 * @return
	 */
	static public D_Justification getJustByGID(String GID, boolean load_Globals, boolean keep, Long oID, Long mID) {
		return getJustByGID(GID, load_Globals, false, keep, null, oID, mID, null);
	}
	/**
	 * Does not call storeRequest on creation.
	 * Therefore If create, should also set keep!
	 * 
	 * If new, sets dirty, and temporary.
	 * @param GID
	 * @param load_Globals
	 * @param create
	 * @param keep
	 * @param __peer
	 * @param storage TODO
	 * @param oID
	 * @return
	 */
	static public D_Justification getJustByGID(String GID, 
			boolean load_Globals, boolean create, boolean keep, 
			D_Peer __peer, long p_oLID, long p_mLID, 
			D_Justification storage) 
	{
		
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Justification: getJustByGID: "+GID+" glob="+load_Globals+" cre="+create+" _peer="+__peer);
		if ((GID == null)) {
			if (_DEBUG) System.out.println("D_Justification: getJustByGID: null GID and GIDH");
			return null;
		}
		if (create) keep = true;
		D_Justification crt = D_Justification.getJustByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
		if (crt != null) {
			if (DEBUG) System.out.println("D_Justification: getJustByGID: got GID cached crt="+crt);
			return crt;
		}

		synchronized (monitor_object_factory) {
			crt = D_Justification.getJustByGID_AttemptCacheOnly(GID, p_oLID, load_Globals, keep);
			if (crt != null) {
				if (DEBUG) System.out.println("D_Justification: getJustByGID: got sync cached crt="+crt);
				return crt;
			}

			try {
				ArrayList<ArrayList<Object>> ma = D_Justification.getJustificationArrayByGID(GID);
				if (storage == null || ma.size() > 0)
					crt = new D_Justification(GID, load_Globals, create, __peer, p_oLID, p_mLID, ma);
				else {
					D_Justification_Node.dropLoaded(storage, true);
					//if (ma.size() > 0) storage.init(ma.get(0));
					crt = storage.initNew(GID, __peer, p_oLID, p_mLID);
				}
				if (DEBUG) System.out.println("D_Justification: getJustByGID: loaded crt="+crt);
				if (keep) {
					crt.status_references ++;
				}
				D_Justification_Node.register_loaded(crt);
			} catch (Exception e) {
				if (DEBUG) System.out.println("D_Justification: getJustByGID: error loading");
				if (DEBUG) e.printStackTrace();
				return null;
			}
			if (DEBUG) System.out.println("D_Justification: getJustByGID: Done");
			return crt;
		}
	}	

	/**
	 * Usable until calling releaseReference()
	 * Verify with assertReferenced()
	 * 
	 * Do not use if changing the GID with it
	 * 
	 * @param peer
	 * @return
	 */
	static public D_Justification getJustByJust_Keep(D_Justification _obj) {
		if (_obj == null) return null;
		String GID = _obj.getGID();
		D_Justification result = null;
		if (GID != null)
			result = D_Justification.getJustByGID(GID, true, true, _obj.getOrganizationLID(), _obj.getMotionLID());
		if (result == null) {
			result = D_Justification.getJustByLID(_obj.getLID(), true, true);
		}
		if (result == null) {
			if ((_obj.getLIDstr() == null) && (_obj.getGIDH() == null)) {
				result = _obj;
				{
					_obj.status_references ++;
					//System.out.println("D_Organization: getOrgByOrg_Keep: "+org.status_references);
					//Util.printCallPath("");
				}
			}
		}
		if (result == null) {
			System.out.println("D_Organization: getOrgByOrg_Keep: got null for "+_obj);
		}
		return result;
	}
	
	private void fillGlobals() {
		if (this.loaded_globals) return;
		
		if((this.getAnswerToLIDstr() != null ) && (this.getAnswerTo_GID() == null))
			this.setAnswerTo_GID(D_Justification.getGIDFromLID(this.getAnswerToLIDstr()));
		
		if((this.getOrganizationLIDstr() != null ) && (this.getOrgGID() == null))
			this.setOrgGID(D_Organization.getGIDbyLIDstr(this.getOrganizationLIDstr()));

		if((this.getMotionLIDstr() != null ) && (this.getMotionGID() == null))
			this.setMotionGID(D_Motion.getGIDFromLID(this.getMotionLIDstr()));
		
		if((this.getConstituentLIDstr() != null ) && (this.getConstituentGID() == null))
			this.setConstituentGID(D_Constituent.getGIDFromLID(this.getConstituentLIDstr()));
		
		loaded_globals = true;
	}
	public static String getGIDFromLID(String nlID) {
		return getGIDFromLID(Util.lval(nlID));
	}
	public static String getGIDFromLID(long n_lID) {
		if (n_lID <= 0) return null;
		Long LID = new Long(n_lID);
		D_Justification c = D_Justification.getJustByLID(LID, true, false);
		if (c == null) {
			if (_DEBUG) System.out.println("D_Justification: getGIDFromLID: null for nLID = "+n_lID);
			for (Long l : D_Justification_Node.loaded_By_LocalID.keySet()) {
				if (_DEBUG) System.out.println("D_Justification: getGIDFromLID: available ["+l+"]"+D_Justification_Node.loaded_By_LocalID.get(l).getGIDH());
			}
			return null;
		}
		return c.getGID();
	}
	/**
	 * Does not create but returns -1
	 * @param GID2
	 * @return
	 */
	public static long getLIDFromGID(String GID2, Long oID, Long mID) {
		if (GID2 == null) return -1;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		if (c == null) return -1;
		return c.getLID();
	}
	public static String getLIDstrFromGID(String GID2, Long oID, Long mID) {
		if (GID2 == null) return null;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		if (c == null) return null;
		return c.getLIDstr();
	}
	public static String getLIDstrFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID, Long mID) {
		if (GID2 == null) return null;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		if (c == null) return null;
		return c.getLIDstr();
	}
	public static long getLIDFromGID_or_GIDH(
			String GID2, String GIDH2, Long oID, Long mID) {
		if (GID2 == null) return -1;
		D_Justification c = D_Justification.getJustByGID(GID2, true, false, oID, mID);
		if (c == null) return -1;
		return c.getLID();
	}
	
	/**
	 * Storage Methods
	 */
	
	public void releaseReference() {
		if (status_references <= 0) Util.printCallPath("Null reference already!");
		else status_references --;
		//System.out.println("D_Justification: releaseReference: "+status_references);
		//Util.printCallPath("");
	}
	
	public void assertReferenced() {
		assert (status_references > 0);
	}
	
	/**
	 * The entries that need saving
	 */
	private static HashSet<D_Justification> _need_saving = new HashSet<D_Justification>();
	private static HashSet<D_Justification> _need_saving_obj = new HashSet<D_Justification>();
	
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Justification> 
	set_DDP2P_DoubleLinkedList_Node(DDP2P_DoubleLinkedList_Node<D_Justification> node) {
		DDP2P_DoubleLinkedList_Node<D_Justification> old = this.component_node.my_node_in_loaded;
		if (DEBUG) System.out.println("D_Justification: set_DDP2P_DoubleLinkedList_Node: set = "+ node);
		this.component_node.my_node_in_loaded = node;
		return old;
	}
	@Override
	public DDP2P_DoubleLinkedList_Node<D_Justification> 
	get_DDP2P_DoubleLinkedList_Node() {
		if (DEBUG) System.out.println("D_Justification: get_DDP2P_DoubleLinkedList_Node: get");
		return component_node.my_node_in_loaded;
	}
	static void need_saving_remove(D_Justification c) {
		if (DEBUG) System.out.println("D_Justification: need_saving_remove: remove "+c);
		_need_saving.remove(c);
		if (DEBUG) dumpNeedsObj(_need_saving);
	}
	static void need_saving_obj_remove(D_Justification org) {
		if (DEBUG) System.out.println("D_Justification: need_saving_obj_remove: remove "+org);
		_need_saving_obj.remove(org);
		if (DEBUG) dumpNeedsObj(_need_saving_obj);
	}
	static D_Justification need_saving_next() {
		Iterator<D_Justification> i = _need_saving.iterator();
		if (!i.hasNext()) return null;
		D_Justification c = i.next();
		if (DEBUG) System.out.println("D_Justification: need_saving_next: next: "+c);
		//D_Justification r = D_Justification_Node.getConstByGIDH(c, null);//.loaded_const_By_GIDhash.get(c);
		if (c == null) {
			if (_DEBUG) {
				System.out.println("D_Justification Cache: need_saving_next null entry "
						+ "needs saving next: "+c);
				System.out.println("D_Justification Cache: "+dumpDirCache());
			}
			return null;
		}
		return c;
	}
	static D_Justification need_saving_obj_next() {
		Iterator<D_Justification> i = _need_saving_obj.iterator();
		if (!i.hasNext()) return null;
		D_Justification r = i.next();
		if (DEBUG) System.out.println("D_Justification: need_saving_obj_next: next: "+r);
		//D_Justification r = D_Justification_Node.loaded_org_By_GIDhash.get(c);
		if (r == null) {
			if (_DEBUG) {
				System.out.println("D_Justification Cache: need_saving_obj_next null entry "
						+ "needs saving obj next: "+r);
				System.out.println("D_Justification Cache: "+dumpDirCache());
			}
			return null;
		}
		return r;
	}
	private static void dumpNeeds(HashSet<String> _need_saving2) {
		System.out.println("D_Justification: Needs:");
		for ( String i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	private static void dumpNeedsObj(HashSet<D_Justification> _need_saving2) {
		System.out.println("Needs:");
		for ( D_Justification i : _need_saving2) {
			System.out.println("\t"+i);
		}
	}
	public static String dumpDirCache() {
		String s = "[";
		s += D_Justification_Node.loaded_objects.toString();
		s += "]";
		return s;
	}
	
	public Encoder getSignableEncoder() {
		if (V0.equals(hash_alg)) return getSignableEncoder_V0();
		if (V1.equals(hash_alg)) return getSignableEncoder_V1();
		return getSignableEncoder_V1();
	}
	public Encoder getSignableEncoder_V0() {
		Encoder enc = new Encoder().initSequence();
		if(getHashAlg()!=null)enc.addToSequence(new Encoder(getHashAlg(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getAnswerTo_GID()!=null)enc.addToSequence(new Encoder(getAnswerTo_GID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC4));
		if(getJustificationBody()!=null)enc.addToSequence(getJustificationBody().getEncoder().setASN1Type(DD.TAG_AC5));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC6));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC7));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC8));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	public Encoder getSignableEncoder_V1() {
		Encoder enc = new Encoder().initSequence();
		if(getHashAlg()!=null)enc.addToSequence(new Encoder(getHashAlg(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if(getGID()!=null)enc.addToSequence(new Encoder(getGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getAnswerTo_GID()!=null)enc.addToSequence(new Encoder(getAnswerTo_GID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC4));
		if(getJustificationBody()!=null)enc.addToSequence(getJustificationBody().getEncoder().setASN1Type(DD.TAG_AC5));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC8));
		return enc;
	}
	
	public Encoder getHashEncoder() {
		if (V0.equals(hash_alg)) return getHashEncoder_V0();
		if (V1.equals(hash_alg)) return getHashEncoder_V1();
		return getHashEncoder_V1();
	}
	public Encoder getHashEncoder_V0() {
		Encoder enc = new Encoder().initSequence();
		//if(hash_alg!=null)enc.addToSequence(new Encoder(hash_alg,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		//if(global_justificationID!=null)enc.addToSequence(new Encoder(global_justificationID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getAnswerTo_GID()!=null)enc.addToSequence(new Encoder(getAnswerTo_GID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC4));
		if(getJustificationBody()!=null)enc.addToSequence(getJustificationBody().getEncoder().setASN1Type(DD.TAG_AC5));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC6));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC7));
		//if(creation_date!=null)enc.addToSequence(new Encoder(creation_date).setASN1Type(DD.TAG_AC8));
		//if(signature!=null)enc.addToSequence(new Encoder(signature).setASN1Type(DD.TAG_AC9));
		//if(motion!=null)enc.addToSequence(motion.getEncoder().setASN1Type(DD.TAG_AC10));
		//if(constituent!=null)enc.addToSequence(constituent.getEncoder().setASN1Type(DD.TAG_AC11));
		return enc;
	}
	public Encoder getHashEncoder_V1() {
		Encoder enc = new Encoder().initSequence();
		if(getMotionGID()!=null)enc.addToSequence(new Encoder(getMotionGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		if(getAnswerTo_GID()!=null)enc.addToSequence(new Encoder(getAnswerTo_GID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		if(getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC4));
		if(getJustificationBody()!=null)enc.addToSequence(getJustificationBody().getEncoder().setASN1Type(DD.TAG_AC5));
		if(getConstituentGID()!=null)enc.addToSequence(new Encoder(getConstituentGID(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC6));
		return enc;
	}
	public static byte getASN1Type() {
		return TAG;
	}

	@Override
	public Encoder getEncoder() {
		return getEncoder(new ArrayList<String>());
	}
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs) {
		return getEncoder(dictionary_GIDs, 0);
	}
	/**
	 * parameter used for dictionaries
	 */
	@Override
	public Encoder getEncoder(ArrayList<String> dictionary_GIDs, int dependants) {
		if (V0.equals(hash_alg)) return getEncoder_V0(dictionary_GIDs, dependants);
		if (V1.equals(hash_alg)) return getEncoder_V1(dictionary_GIDs, dependants);
		return getEncoder_V1(dictionary_GIDs, dependants);
	}
	public Encoder getEncoder_V0(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		Encoder enc = new Encoder().initSequence();
		if (getHashAlg() != null)enc.addToSequence(new Encoder(getHashAlg(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if (getGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		}
		if (getMotionGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getMotionGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		}
		if (getAnswerTo_GID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getAnswerTo_GID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		}
		if (getConstituentGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getConstituentGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		}
		if (getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC5));
		if (getJustificationBody()!=null)enc.addToSequence(getJustificationBody().getEncoder().setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC7));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC8));
		if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC9));
		if(getSignature()!=null)enc.addToSequence(new Encoder(getSignature()).setASN1Type(DD.TAG_AC10));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (getMotion()!=null)enc.addToSequence(getMotion().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC11));
			if (getConstituent()!=null)enc.addToSequence(getConstituent().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC12));
			if (getAnswerTo()!=null)enc.addToSequence(getAnswerTo().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC13));
		}
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (getOrgGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getOrgGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC14));
		}
		return enc;
	}
	public Encoder getEncoder_V1(ArrayList<String> dictionary_GIDs, int dependants) {
		int new_dependants = dependants;
		if (dependants > 0) new_dependants = dependants - 1;
		Encoder enc = new Encoder().initSequence();
		if (getHashAlg() != null)enc.addToSequence(new Encoder(getHashAlg(),Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC0));
		if (getGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC1));
		}
		if (getMotionGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getMotionGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC2));
		}
		if (getAnswerTo_GID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getAnswerTo_GID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC3));
		}
		if (getConstituentGID()!=null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getConstituentGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC4));
		}
		if (getJustificationTitle()!=null) enc.addToSequence(getJustificationTitle().getEncoder().setASN1Type(DD.TAG_AC5));
		if (getJustificationBody()!=null)enc.addToSequence(getJustificationBody().getEncoder().setASN1Type(DD.TAG_AC6));
		//if(author!=null)enc.addToSequence(author.getEncoder().setASN1Type(DD.TAG_AC7));
		//if(last_reference_date!=null)enc.addToSequence(new Encoder(last_reference_date).setASN1Type(DD.TAG_AC8));
		//if(getCreationDate()!=null)enc.addToSequence(new Encoder(getCreationDate()).setASN1Type(DD.TAG_AC9));
		if(getSignature()!=null)enc.addToSequence(new Encoder(getSignature()).setASN1Type(DD.TAG_AC10));
		
		if (dependants != ASNObj.DEPENDANTS_NONE) {
			if (getMotion()!=null)enc.addToSequence(getMotion().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC11));
			if (getConstituent()!=null)enc.addToSequence(getConstituent().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC12));
			if (getAnswerTo()!=null)enc.addToSequence(getAnswerTo().getEncoder(dictionary_GIDs, new_dependants).setASN1Type(DD.TAG_AC13));
		}
		/**
		 * May decide to comment encoding of "global_organization_ID" out completely, since the org_GID is typically
		 * available at the destination from enclosing fields, and will be filled out at expansion
		 * by ASNSyncPayload.expand at decoding.
		 * However, it is not that damaging when using compression, and can be stored without much overhead.
		 * So it is left here for now.  Test if you comment out!
		 */
		if (getOrgGID() != null) {
			String repl_GID = ASNSyncPayload.getIdxS(dictionary_GIDs, getOrgGID());
			enc.addToSequence(new Encoder(repl_GID,Encoder.TAG_PrintableString).setASN1Type(DD.TAG_AC14));
		}
		return enc;
	}

	@Override
	public D_Justification decode(Decoder decoder) throws ASN1DecoderFail {
		Decoder dec = decoder.getContent();
		if(dec.getTypeByte()==DD.TAG_AC0)hash_alg = dec.getFirstObject(true).getString(DD.TAG_AC0);
		if (V0.equals(hash_alg)) return decode_V0(dec);
		if (V1.equals(hash_alg)) return decode_V1(dec);
		return decode_V1(dec);
	}
	public D_Justification decode_V0(Decoder dec) throws ASN1DecoderFail {
		if(dec.getTypeByte()==DD.TAG_AC1)global_justificationID = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)global_motionID = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3)global_answerTo_ID = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5)justification_title = new D_Document_Title().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC6)justification_text = new D_Document().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC7)author = new D_Constituent().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC8)last_reference_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC8);
		if(dec.getTypeByte()==DD.TAG_AC9)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC9);
		if(dec.getTypeByte()==DD.TAG_AC10)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC10);
		if(dec.getTypeByte()==DD.TAG_AC11)motion = D_Motion.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC12)constituent = D_Constituent.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC13)answerTo = new D_Justification().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC14)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC14);
		
		if ((global_constituent_ID == null) && (constituent != null)) global_constituent_ID = constituent.getGID();
		if ((global_answerTo_ID == null) && (answerTo != null)) global_answerTo_ID = answerTo.global_justificationID;
		if ((global_motionID == null) && (motion != null)) global_motionID = motion.getGID();
		
		this.setTemporary(false);
		return this;
	}
	public D_Justification decode_V1(Decoder dec) throws ASN1DecoderFail {
		if(dec.getTypeByte()==DD.TAG_AC1)global_justificationID = dec.getFirstObject(true).getString(DD.TAG_AC1);
		if(dec.getTypeByte()==DD.TAG_AC2)global_motionID = dec.getFirstObject(true).getString(DD.TAG_AC2);
		if(dec.getTypeByte()==DD.TAG_AC3)global_answerTo_ID = dec.getFirstObject(true).getString(DD.TAG_AC3);
		if(dec.getTypeByte()==DD.TAG_AC4)global_constituent_ID = dec.getFirstObject(true).getString(DD.TAG_AC4);
		if(dec.getTypeByte()==DD.TAG_AC5)justification_title = new D_Document_Title().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC6)justification_text = new D_Document().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC7)author = new D_Constituent().decode(dec.getFirstObject(true));
		//if(dec.getTypeByte()==DD.TAG_AC8)last_reference_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC8);
		//if(dec.getTypeByte()==DD.TAG_AC9)creation_date = dec.getFirstObject(true).getGeneralizedTimeCalender(DD.TAG_AC9);
		if(dec.getTypeByte()==DD.TAG_AC10)signature = dec.getFirstObject(true).getBytes(DD.TAG_AC10);
		if(dec.getTypeByte()==DD.TAG_AC11)motion = D_Motion.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC12)constituent = D_Constituent.getEmpty().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC13)answerTo = new D_Justification().decode(dec.getFirstObject(true));
		if(dec.getTypeByte()==DD.TAG_AC14)global_organization_ID = dec.getFirstObject(true).getString(DD.TAG_AC14);
		
		if ((global_constituent_ID == null) && (constituent != null)) global_constituent_ID = constituent.getGID();
		if ((global_answerTo_ID == null) && (answerTo != null)) global_answerTo_ID = answerTo.global_justificationID;
		if ((global_motionID == null) && (motion != null)) global_motionID = motion.getGID();
		
		this.setTemporary(false);
		return this;
	}

	public static ArrayList<String> checkAvailability(ArrayList<String> hashes,
			String orgID, String mID, boolean DBG) throws P2PDDSQLException {
		ArrayList<String> result = new ArrayList<String>();
		for (String hash : hashes) {
			if(!available(hash, orgID, mID, DBG)) result.add(hash);
		}
		return result;
	}
	/**
	 * check blocking at this level
	 * @param hash
	 * @param orgID
	 * @return
	 * @throws P2PDDSQLException
	 */
	public static boolean available(String hash, String orgID, String mID, boolean DBG) throws P2PDDSQLException {
		boolean result = true;
		/*
		String sql = 
			"SELECT "+table.justification.justification_ID+
			" FROM "+table.justification.TNAME+
			" WHERE "+table.justification.global_justification_ID+"=? "+
			//" AND "+table.justification.organization_ID+"=? "+
			" AND ( "+table.justification.signature + " IS NOT NULL " +
			" OR "+table.justification.blocked+" = '1');";
		ArrayList<ArrayList<Object>> a = Application.db.select(sql, new String[]{hash}, DEBUG);
		if(a.size()==0) result = false;
		*/
		D_Justification j = D_Justification.getJustByGID(hash, true, false, Util.lval(orgID), Util.lval(mID));
		if (
				(j == null) 
				|| ( j.getOrganizationLID() != Util.lval(orgID)) 
				|| ( (mID != null) && (j.getMotionLID() != 0) && j.getMotionLID() != Util.lval(mID))
				|| ( (
						(j.getSignature() == null )
						|| (j.getSignature().length == 0)
						|| ( j.isTemporary())) 
						&& !j.isBlocked() )
				) result = false;
		if(DEBUG||DBG) System.out.println("D_Justification: available: "+hash+" in "+orgID+"(?) = "+result);
		return result;
	}
	public long storeLinkNewTemporary() {
		synchronized (monitor_object_factory) {
			if (this.getGID() != null) {
				D_Justification m = D_Justification.getJustByGID(this.getGID(), true, false, this.getOrganizationLID(), this.getMotionLID());
				if (m != null) return m.getLID_force();
			}
			this.storeSynchronouslyNoException();
			D_Justification_Node.register_loaded(this);
		}
		return 0;
	}
	/**
	 * Create an new object (with adapted text and body), store it, and reload it using its LID.
	 * 
	 * @param answered
	 * @return
	 */
	public static D_Justification getAnsweringDefaultTemporaryStoredRegistered(D_Justification answered) {
		D_Justification j = getAnsweringDefaultTemporaryStored(answered);
		long a_ID = j.getLID();
		return D_Justification.getJustByLID(a_ID, true, false);
	}
	/**
	 * Copies title and body, prepending them with ("Answer To").
	 * @param answered
	 * @return
	 */
	public static D_Justification getAnsweringDefaultTemporaryStored(D_Justification answered) {
		D_Justification answering = D_Justification.getEmpty();
		
		/**
		 * Copy only formats.
		 */
		answering.getJustificationTitle().title_document.copy(answered.getJustificationTitle().title_document);
		answering.getJustificationBody().copy(answered.getJustificationBody());
		
		answering.getJustificationTitle().title_document.setDocumentString(__("Answer To:")+" "+answered.getJustificationTitle().title_document.getDocumentUTFString());
		answering.getJustificationBody().setDocumentString(__("Answer To:")+" \n"+answered.getJustificationBody().getDocumentUTFString());
		
		answering.setAnswerTo_SetAll(answered);
		answering.setRequested(false);
		answering.setBlocked(false);
		answering.setBroadcasted(false);
		answering.setTemporary(true);
		answering.setCreationDate(answering.setArrivalDate(Util.CalendargetInstance()));
		
		answering.setMotionAndOrganizationAllFromJustification(answered);
		
		long a_ID = answering.storeSynchronouslyNoException();
		
		return answering;
		//return 
	}
	
//	public void changeToDefaultAnswer() {
//		this.setAnswerToLIDstr(getLIDstr());
//		this.setAnswerTo_GID(this.getGID());
//		this.setLIDstr(null);
//		this.setGID(null);
//		this.setSignature(null);
//		this.setConstituentLIDstr_Dirty(null);
//		this.setConstituentGID(null);
//		this.getJustificationTitle().title_document.setDocumentString(__("Answer To:")+" "+this.getJustificationTitle().title_document.getDocumentUTFString());
//		this.getJustificationBody().setDocumentString(__("Answer To:")+" \n"+this.getJustificationBody().getDocumentUTFString());
//		this.setCreationDate(this.setArrivalDate(Util.CalendargetInstance()));
//		this.setRequested(this.setBlocked(this.setBroadcasted(false)));
//	}
	/**
	 * Sets the signature and the GID to null
	 */
	public boolean setEditable() {
		if (! isEditable()) {
			Util.printCallPath("Why allowing it?");
			return false;
		}
		setSignature(null);
		this.setGID(null); // removed registration
		this.setTemporary(true);
		return true;
	}
	/**
	 * Returns true if the GID is null;
	 * Used to consider signature or temporary but that is inappropriate.
	 * @return
	 */
	public boolean isEditable() {
		if (getGID() == null) {
			if (DEBUG) out.println("D_Justification:editable: no GID");
			return true;
		}
		return false;
//		if (getSignature() == null) {
//			if (DEBUG) out.println("D_Justification:editable: no sign");
//			return true;
//		}
		/*
		if(!verifySignature()) {
			if(DEBUG) out.println("D_Justification:editable: not verifiable signature");
			setSignature(null);
			try {
				storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return true;
		}
		*/
		//return false;
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
		//Util.printCallPath(""+this);
		
		if (dirty_main || dirty_preferences || dirty_local) {
			storeAct_main();
		}
		if (this.dirty_mydata) {
			storeAct_mydata();
		}
		return this.getLID();
	}
	
	long storeAct_mydata() throws P2PDDSQLException {
		this.dirty_mydata = false;
		try {
			String param[];
			if (this.mydata.row <= 0) {
				param = new String[net.ddp2p.common.table.my_justification_data.FIELDS_NB_NOID];
			} else {
				param = new String[net.ddp2p.common.table.my_justification_data.FIELDS_NB];
			}
			param[net.ddp2p.common.table.my_justification_data.COL_NAME] = this.mydata.name;
			param[net.ddp2p.common.table.my_justification_data.COL_CATEGORY] = this.mydata.category;
			param[net.ddp2p.common.table.my_justification_data.COL_CREATOR] = this.mydata.creator;
			param[net.ddp2p.common.table.my_justification_data.COL_JUSTIFICATION_LID] = this.getLIDstr();
		
			if (this.mydata.row <= 0) {
				this.mydata.row =
						Application.db.insert(true, net.ddp2p.common.table.my_justification_data.TNAME,
								net.ddp2p.common.table.my_justification_data.fields_noID, param, DEBUG);
			} else {
				param[net.ddp2p.common.table.my_justification_data.COL_ROW] = this.mydata.row+"";
				Application.db.update(true, net.ddp2p.common.table.my_justification_data.TNAME,
						net.ddp2p.common.table.my_justification_data.fields_noID,
						new String[]{net.ddp2p.common.table.my_justification_data.row}, param, DEBUG);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return this.mydata.row;
	}
	long storeAct_main() throws P2PDDSQLException {
		// boolean DEBUG = true;
		dirty_main = dirty_preferences = dirty_local = false;
		long result;


		if (this.arrival_date == null && (this.getGIDH() != null)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Justification: storeAct_main: missing arrival_date");
			Util.printCallPath("");
		}		
		
		if (DEBUG) System.out.println("D_Justification:storeVerified: fixed local="+this);
		if (this.organization_ID == null || Util.lval(organization_ID) <= 0) {
			if (_DEBUG) Util.printCallPath("No organization ID: "+this);
		}
		String params[] = new String[net.ddp2p.common.table.justification.J_FIELDS];
		params[net.ddp2p.common.table.justification.J_HASH_ALG] = this.hash_alg;
		params[net.ddp2p.common.table.justification.J_GID] = this.global_justificationID;
		params[net.ddp2p.common.table.justification.J_TITLE_FORMAT] = this.justification_title.title_document.getFormatString();
		params[net.ddp2p.common.table.justification.J_TEXT_FORMAT] = this.justification_text.getFormatString();
		params[net.ddp2p.common.table.justification.J_TITLE] = this.justification_title.title_document.getDocumentString();
		params[net.ddp2p.common.table.justification.J_TEXT] = this.justification_text.getDocumentString();
		params[net.ddp2p.common.table.justification.J_ANSWER_TO_ID] = this.answerTo_ID;
		params[net.ddp2p.common.table.justification.J_CONSTITUENT_ID] = this.constituent_ID;
		//params[table.justification.J_ORG_ID] = organization_ID;
		//params[table.justification.J_STATUS] = status+"";
		params[net.ddp2p.common.table.justification.J_MOTION_ID] = this.motion_ID;
		params[net.ddp2p.common.table.justification.J_ORG_ID] = this.organization_ID;
		params[net.ddp2p.common.table.justification.J_SIGNATURE] = Util.stringSignatureFromByte(signature);
		params[net.ddp2p.common.table.justification.J_REFERENCE] = Encoder.getGeneralizedTime(this.last_reference_date);
		params[net.ddp2p.common.table.justification.J_CREATION] = Encoder.getGeneralizedTime(this.creation_date);
		params[net.ddp2p.common.table.justification.J_PREFERENCES_DATE] = Encoder.getGeneralizedTime(this.preferences_date);
		params[net.ddp2p.common.table.justification.J_ARRIVAL] = Encoder.getGeneralizedTime(arrival_date);
		params[net.ddp2p.common.table.justification.J_BLOCKED] = Util.bool2StringInt(blocked);
		params[net.ddp2p.common.table.justification.J_REQUESTED] = Util.bool2StringInt(requested);
		params[net.ddp2p.common.table.justification.J_BROADCASTED] = Util.bool2StringInt(broadcasted);
		params[net.ddp2p.common.table.justification.J_TEMPORARY] = Util.bool2StringInt(isTemporary());
		params[net.ddp2p.common.table.justification.J_HIDDEN] = Util.bool2StringInt(hidden);
		params[net.ddp2p.common.table.justification.J_PEER_SOURCE_ID] = Util.getStringID(peer_source_ID);
		if (this.justification_ID == null) {
			if (this.getMotionLID() > 0) {
				//D_Justification.getJustByLID_AttemptCacheOnly(LID, false, false);
				D_Motion m = D_Motion.getMotiByLID_AttemptCacheOnly(this.getMotionLID(), false, false);
				if (m != null) m.resetCache(); // removing memory of statistics about justifications!
			}
			if (this.getOrganizationLIDstr() != null) {
				D_Organization o = D_Organization.getOrgByLID_AttemptCacheOnly_NoKeep(this.getOrganizationLID(), false);
				if (o != null) o.resetCache(); // removing cached memory of statistics about justifications!
			}
			if(DEBUG) System.out.println("WB_Justification:storeVerified:inserting");
			result = Application.db.insert(net.ddp2p.common.table.justification.TNAME,
					net.ddp2p.common.table.justification.fields_array,
					params,
					DEBUG
					);
			//justification_ID=""+result;
			setLID_AndLink(result);
		}else{
			if(DEBUG) System.out.println("WB_Justification:storeVerified:updating ID="+this.justification_ID);
			params[net.ddp2p.common.table.justification.J_ID] = justification_ID;
			Application.db.update(net.ddp2p.common.table.justification.TNAME,
					net.ddp2p.common.table.justification.fields_noID_array,
					new String[]{net.ddp2p.common.table.justification.justification_ID},
					params,
					DEBUG
					);
		}
		return this.getLID();
	}
	
	/** Storing */
	public static D_Justification_SaverThread saverThread = new D_Justification_SaverThread();
	public boolean dirty_any() {
		return dirty_main || dirty_local || dirty_preferences || dirty_mydata;
	}
	/**
	 * 
	 * @param gID
	 * @param DBG
	 * @return
	 *  0 for absent,
	 *  1 for present&signed,
	 *  -1 for temporary
	 * @throws P2PDDSQLException
	 */
	public static int isGIDavailable(String gID, long oID, long mID, boolean DBG) throws P2PDDSQLException {
		D_Justification n = D_Justification.getJustByGID(gID, true, false, oID, mID);
		if (n == null) return 0;
		if (n.isTemporary()) return -1;
		return 1;
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

		if (this.arrival_date == null && (this.getGIDH() != null)) {
			this.arrival_date = Util.CalendargetInstance();
			if (_DEBUG) System.out.println("D_Justification: storeRequest: missing arrival_date");
			Util.printCallPath("");
		}
		
		String save_key = this.getGIDH();
		
		if (save_key == null) {
			//save_key = ((Object)this).hashCode() + "";
			//Util.printCallPath("Cannot store null:\n"+this);
			//return;
			D_Justification._need_saving_obj.add(this);
			if (DEBUG) System.out.println("D_Motion: storeRequest: added to _need_saving_obj");
		} else {		
			if (DEBUG) System.out.println("D_Motion: storeRequest: GIDH="+save_key);
			D_Justification._need_saving.add(this);
		}
		try {
			if (!saverThread.isAlive()) { 
				if (DEBUG) System.out.println("D_Motion: storeRequest:startThread");
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
	 * dirty_my_data - ...
	 * dirty_choices - ...
	 * 
	 * @return This returns synchronously (waiting for the storing to happen), and returns the local ID.
	 */
	public long storeRequest_getID() {
		if ( this.getLIDstr() == null ) 
			return this.storeSynchronouslyNoException();
		this.storeRequest();
		return this.getLID();
	}
	/**
	 * This can be delayed saving
	 * @return
	 */
	public long storeSynchronouslyNoException() {
		if (DEBUG) System.out.println("D_Justification: storeSynchronouslyNoException: start");
//		if (this.getLIDstr() == null)
//			Util.printCallPath("Why store sync?");
		try {
			return storeAct();
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
		
	
	/**
	 * Load this message in the storage cache node.
	 * Should be called each time I load a node and when I sign myself.
	 */
	private void reloadMessage() {
		if (this.loaded_globals) this.component_node.message = this.encode(); //crt.encoder.getBytes();
	}
	
	public boolean readyToSend() {
		if (this.getGID()==null) return false;
		if (this.getMotionGID()==null) return false;
		return true;
	}
	/**
	 * Both store signature and returns it
	 * 
	 * @return
	 */
	public byte[] sign() {
		return sign(this.getConstituentGID());
	}
	/**
	 * Both store signature and returns it
	 * 
	 * @param signer_GID
	 * @return
	 */
	public byte[] sign(String signer_GID) {
		if(DEBUG) System.out.println("WB_Justification:sign: start signer="+signer_GID);
		net.ddp2p.ciphersuits.SK sk = net.ddp2p.common.util.Util.getStoredSK(signer_GID);
		if(sk==null) {
			if(_DEBUG) System.out.println("WB_Justification:sign: no signature");
			Application_GUI.warning(__("No secret key to sign motion, no constituent GID"), __("No Secret Key!"));
			return null;
		}
		if(DEBUG) System.out.println("WB_Justification:sign: sign="+sk);

		return sign(sk);
	}
	/**
	 * Both store signature and returns it
	 * @param sk
	 * @return
	 */
	public byte[] sign(SK sk){
		if(DEBUG) System.out.println("WB_Justification:sign: this="+this+"\nsk="+sk);
		setSignature(Util.sign(this.getSignableEncoder().getBytes(), sk));
		if(DEBUG) System.out.println("WB_Justification:sign:got this="+Util.byteToHexDump(getSignature()));
		return getSignature();
	}
	/**
	 * Computes the GIDH without assigning it.
	 * First tries to fillGlobasl!
	 * @return
	 */
	public String make_ID() {
		fillGlobals();
		// return this.global_witness_ID =  
		return D_GIDH.d_Just+Util.getGID_as_Hash(this.getHashEncoder().getBytes());
	}
	public boolean verifySignature(){
		if(DEBUG) System.out.println("WB_Justification:verifySignature: start");
		String pk_ID = this.getConstituentGID();//.submitter_global_ID;
		if((pk_ID == null) && (this.getConstituent()!=null) && (this.getConstituent().getGID()!=null))
			pk_ID = this.getConstituent().getGID();
		if(pk_ID == null) return false;
		
		String newGID = make_ID();
		if(!newGID.equals(this.getGID())) {
			Util.printCallPath("WB_Justification: WRONG EXTERNAL GID [" + this.getLID()+"]: "+this.getTitleOrMy()+" "+this);
			if(DEBUG) System.out.println("WB_Justification:verifySignature: WRONG HASH GID="+this.getGID()+" vs="+newGID);
			if(DEBUG) System.out.println("WB_Justification:verifySignature: WRONG HASH GID result="+false);
			return false;
		}
		
		boolean result = Util.verifySignByID(this.getSignableEncoder().getBytes(), pk_ID, getSignature());
		if(DEBUG) System.out.println("WB_Justification:verifySignature: result wGID="+result);
		return result;
	}
	/**
	 * Sets serving both in peer.served_orgs and in organization.broadcasted
	 * has to sign the peer again because of served_orgs changes
	 * @return
	 */
	public boolean toggleServing() {
		D_Justification m = D_Justification.getJustByJust_Keep(this);
		m.setBroadcasted(!m.isBroadcasted());
		m.storeRequest();
		m.releaseReference();
		return m.isBroadcasted();
	}
	public Object getTitleOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getJustificationTitle();
	}
	public String getTitleStrOrMy() {
		String n = mydata.name;
		if (n != null) return n;
		return getJustificationTitleStr();
	}
	public Object getCategoryOrMy() {
		String n = mydata.category;
		if (n != null) return n;
		return null;//this.getCategory();
	}
	public Object getCreatorOrMy() {
		String n = mydata.creator;
		if (n != null) return n;
		if (this.constituent_ID != null) this.constituent = D_Constituent.getConstByLID(constituent_ID, true, false);
		if (this.constituent == null) return null;
		return this.constituent.getNameOrMy();
	}
	public void setNameMy(String _name) {
		this.mydata.name = _name;
		this.setPreferencesDate();
		this.dirty_mydata = true;
	}
	public String getNameMy() {
		return this.mydata.name;
	}
	public void setCreatorMy(String _creat) {
		this.mydata.creator = _creat;
		this.setPreferencesDate();
		this.dirty_mydata = true;
	}
	public String getCreatorMy() {
		return this.mydata.creator;
	}
	public void setCategoryMy(String _cat) {
		this.mydata.category = _cat;
		this.setPreferencesDate();
		this.dirty_mydata = true;
	}
	public String getCategoryMy() {
		return this.mydata.category;
	}

	public long getProviderID() {
		return this.peer_source_ID;
	}
	public D_Peer getProvider() {
		if (peer_source_ID <= 0) return null;
		return D_Peer.getPeerByLID(peer_source_ID, true, false);
	}

	public boolean isHidden() {
		return this.hidden;
	}
	/**
	 * sets dirty preferences
	 * @param h
	 * @return
	 */
	public boolean setHidden(boolean h) {
		boolean old = this.hidden;
		this.hidden = h;
		this.setPreferencesDate();
		this.dirty_preferences = true;
		return old;
	}
	
	public String getHashAlg() {
		return hash_alg;
	}
	public void setHashAlg(String hash_alg) {
		this.hash_alg = hash_alg;
		this.dirty_main = true;
	}
	public String getGIDH() {
		return global_justificationID;
	}
	public String getGID() {
		return global_justificationID;
	}
	/**
	 * Set with make_ID(). No registration.
	 * @return
	 */
	public boolean setGID() {
		return null != this.setGID(this.make_ID());
	}
	/**
	 * Just set (with dirty on change) and return parameter
	 * @param _global_justificationID
	 * @return
	 */
	public String setGID(String _global_justificationID) {
		if (Util.equalStrings_null_or_not(_global_justificationID, this.getGID())) return _global_justificationID;
		//this.global_justificationID = _global_justificationID;
		_setGID(_global_justificationID);
		this.dirty_main = true;
		return _global_justificationID;
	}
	/**
	 * Just stores the GID (and returns it too).
	 * @param _global_justificationID
	 * @return
	 */
	public String _setGID(String _global_justificationID) {
		//if (Util.equalStrings_null_or_not(_global_justificationID, this.getGID())) return _global_justificationID;
		this.global_justificationID = _global_justificationID;
		//this.dirty_main = true;
		return _global_justificationID;
	}
	/**
	 * Sets GID and registers saving
	 * @param _global_justificationID
	 * @return
	 */
	public String setGID_fixRegisteration(String _global_justificationID) {
		assert(Util.equalStrings_or_one_null(_global_justificationID, this.global_justificationID));
		if (Util.equalStrings_null_or_not(_global_justificationID, this.getGID())) return _global_justificationID;
//		boolean loaded_in_cache = this.isLoaded();
		//this.global_justificationID = _global_justificationID;
		setGID(_global_justificationID);
		//this.dirty_main = true;
//		Long oID = this.getOrganizationLID();
//		if (loaded_in_cache) {
//			if (this.getGID() != null)
//				D_Justification_Node.putByGID(this.getGID(), oID, this); //.loaded_const_By_GID.put(this.getGID(), this);
//			if (this.getGIDH() != null)
//				D_Justification_Node.putByGIDH(this.getGIDH(), oID, this); //.loaded_const_By_GIDhash.put(this.getGIDH(), this);
//		}
		
		if (this.getGID() != null) // && isLoaded())
			D_Justification_Node.register_newGID_ifLoaded(this);
		return _global_justificationID;
	}
	public boolean isLoaded() {
		String GIDH, GID;
		if (! D_Justification_Node.loaded_objects.inListProbably(this)) return false;
		long lID = this.getLID();
		long oID = this.getOrganizationLID();
		if (lID > 0)
			if ( null != D_Justification_Node.loaded_By_LocalID.get(new Long(lID)) ) return true;
		if ((GIDH = this.getGIDH()) != null)
			if ( null != D_Justification_Node.getByGIDH(GIDH, oID) //.loaded_const_By_GIDhash.get(GIDH)
			) return true;
		if ((GID = this.getGID()) != null)
			if ( null != D_Justification_Node.getByGID(GID, oID)  //.loaded_const_By_GID.get(GID)
			) return true;
		return false;
	}
	public String getMotionGID() {
		return global_motionID;
	}
	public void setMotionGID(String global_motionID) {
		this.global_motionID = global_motionID;
	}
	public String getAnswerTo_GID() {
		return global_answerTo_ID;
	}
	public void setAnswerTo_GID(String global_answerTo_ID) {
		this.global_answerTo_ID = global_answerTo_ID;
	}
	public String getConstituentGIDH() {
		if (global_constituent_ID != null) return D_Constituent.getGIDHashFromGID(this.getConstituentGID());
		if (this.constituent != null) return constituent.getGIDH();
		return null;
	}
	public String getConstituentGID() {
		return global_constituent_ID;
	}
	public void setConstituentGID(String global_constituent_ID) {
		this.global_constituent_ID = global_constituent_ID;
		if (global_constituent_ID == null) {
			this.constituent = null;
			this.constituent_ID = null;
		}
	}
	public String getOrgGID() {
		return global_organization_ID;
	}
	public String getOrgGIDH() {
		if (global_organization_ID != null) return D_Organization.getOrgGIDHashGuess(getOrgGID());
		if (this.organization != null) return organization.getGIDH_or_guess();
		return null;
	}
	public void setOrgGID(String global_organization_ID) {
		this.global_organization_ID = global_organization_ID;
	}
	public void setOrgGIDH(String global_organization_IDH) {
		String GID = D_Organization.getOrgGIDGuess(global_organization_IDH);
		this.organization = D_Organization.getOrgByGID_or_GIDhash_NoCreate(GID, global_organization_IDH, true, false);
		if (this.organization != null)
			this.global_organization_ID = this.organization.getGID();
	}
	public String guessOrganizationGID() throws P2PDDSQLException {
		String motion_org;
		String const_org;
		long oID=-1,m_oID=-1,c_oID=-1;
		
		D_Motion m = D_Motion.getMotiByGID(this.getMotionGID(), true, false);
		if (m != null) return m.getOrganizationGID_force();

		D_Constituent cs = D_Constituent.getConstByGID_or_GIDH(this.getConstituentGID(), null, true, false);
		if (cs != null) c_oID = cs.getOrganizationLID();
		if((c_oID>0)&&(oID>0)&&(c_oID!=oID)){
			if(_DEBUG) System.out.println("D_Just:guess: "+c_oID+" vs "+oID);
			return null;
		}
		if(DEBUG) System.out.println("D_Just:guess right: "+c_oID+" vs "+oID);
		if(c_oID>0) oID=c_oID;
		return D_Organization.getGIDbyLIDstr(Util.getStringID(oID));
	}
	public Calendar getLastReferenceDate() {
		return last_reference_date;
	}
	public void setLastReferenceDate(Calendar last_reference_date) {
		this.last_reference_date = last_reference_date;
		this.dirty_local = true;
	}
	public String getPreferencesDateStr() {
		return Encoder.getGeneralizedTime(this.getPreferencesDate());
	}
	public Calendar getPreferencesDate() {
		return preferences_date;
	}
	public void setPreferencesDate(Calendar preferences_date) {
		this.preferences_date = preferences_date;
		this.dirty_main = true;
	}
	public Calendar setPreferencesDate() {
		this.preferences_date = Util.CalendargetInstance();
		this.dirty_preferences = true;
		return preferences_date;
	}
	public String getCreationDateStr() {
		return Encoder.getGeneralizedTime(this.getCreationDate());
	}
	public Calendar getCreationDate() {
		return creation_date;
	}
	public void setCreationDate(Calendar creation_date) {
		if (! Util.equalCalendars_null_or_not(creation_date, this.creation_date)) {
			this.creation_date = creation_date;
			this.dirty_main = true;
		}
	}
	public byte[] getSignature() {
		return signature;
	}
	public void setSignature(byte[] signature) {
		if (signature != this.signature) {
			this.signature = signature;
			this.dirty_main = true;
		}
	}
	/** returns null for null */
	public String getArrivalDateStr() {
		return Encoder.getGeneralizedTime(this.getArrivalDate());
	}
	/** returns null for null */
	public Calendar getArrivalDate() {
		return arrival_date;
	}
	/**
	 * Returns the new value
	 * @return
	 */
	public Calendar setArrivalDate() {
		return this.setArrivalDate(Util.CalendargetInstance());
	}
	/**
	 * Returns the parameter
	 * @param arrival_date
	 * @return
	 */
	public Calendar setArrivalDate(Calendar arrival_date) {
		this.arrival_date = arrival_date;
		this.dirty_local = true;
		return arrival_date;
	}
	/**
	 * Only reads the org object (only if available)
	 * @return
	 */
	public D_Organization getOrganization() {
		return organization;
	}
	/**
	 * reads and sets the org object
	 * @return
	 */
	public D_Organization getOrganizationForce() {
		if (getOrganization() != null) return getOrganization();
		if (this.getOrganizationLID() > 0)
			return setOrganizationObj(D_Organization.getOrgByLID(this.getOrganizationLID(), true, false));
		return setOrganizationObj(D_Organization.getOrgByGID_or_GIDhash_NoCreate(this.getOrgGID(), this.getOrgGIDH(), true, false));
	}
	/**
	 * Only changes the org object.
	 * @param motion
	 */
	public D_Organization setOrganizationObj(D_Organization organization) {
		this.organization = organization;
		return organization;
	}
	/**
	 * Only reads the motion object (only if available)
	 * @return
	 */
	public D_Motion getMotion() {
		return motion;
	}
	/**
	 * reads and sets the motion object
	 * @return
	 */
	public D_Motion getMotionForce() {
		if (getMotion() != null) return getMotion();
		if (this.getMotionLID() > 0)
			return setMotionObj(D_Motion.getMotiByLID(this.getMotionLID(), true, false));
		return setMotionObj(D_Motion.getMotiByGID(this.getMotionGID(), true, false, this.getOrganizationLID()));
	}
	/**
	 * Only changes the motion object.
	 * @param motion
	 */
	public D_Motion setMotionObj(D_Motion motion) {
		this.motion = motion;
		return motion;
	}
	/**
	 * Sets the motion object, its LID and its GID.
	 * @param _motion
	 */
	public void setMotionAndOrganizationAll(D_Motion _motion) {
		this.motion = _motion;
		if (_motion != null) {
			this.setMotionGID(_motion.getGID());
			this.setMotionLID(_motion.getLID());
			this.setOrganizationLIDstr(_motion.getOrganizationLIDstr());
			this.setOrgGID(_motion.getOrganizationGID_force());
		} else {
			this.setMotionGID(null);
			this.setMotionLID(-1);
		}
	}
	/**
	 * Harvests the motion and organization objects/LID/GID from the parameter.
	 * @param _justification
	 */
	public void setMotionAndOrganizationAllFromJustification(D_Justification _justification) {
		this.motion = _justification.getMotion();
		if (motion != null) {
			this.setMotionGID(motion.getGID());
			this.setMotionLID(motion.getLID());
			this.setOrganizationLIDstr(motion.getOrganizationLIDstr());
			this.setOrgGID(motion.getOrganizationGID_force());
		} else {
			this.setMotionGID(_justification.getMotionGID());
			this.setMotionLID(_justification.getMotionLID());
			this.setOrganizationLIDstr(_justification.getOrganizationLIDstr());
			this.setOrgGID(_justification.getOrgGID());
		}
	}
	/**
	 * Only reads if available
	 * @return
	 */
	public D_Constituent getConstituent() {
		return constituent;
	}
	/**
	 * reads and sets the constituent object
	 * @return
	 */
	public D_Constituent getConstituentForce() {
		if (getConstituent() != null) return getConstituent();
		if (this.getConstituentLID() > 0)
			return setConstituentObj(D_Constituent.getConstByLID(this.getConstituentLID(), true, false));
		return setConstituentObj(D_Constituent.getConstByGID_or_GIDH(this.getConstituentGID(), null, true, false, this.getOrganizationLID()));
	}
	/**
	 * Just sets the object
	 * @param constituent
	 */
	public D_Constituent setConstituentObj(D_Constituent constituent) {
		this.constituent = constituent;
		return constituent;
	}
	/**
	 *  Sets the object, GID and LID
	 * @param constituent
	 */
	public void setConstituentAll(D_Constituent constituent) {
		this.constituent = constituent;
		if (constituent != null) {
			this.setConstituentGID(constituent.getGID());
			this.setConstituentLID_Dirty(constituent.getLID());
		} else {
			this.setConstituentGID(null);
			this.setConstituentLID_Dirty(-1);
		}
	}
	public D_Justification getAnswerTo() {
		return answerTo;
	}
	/**
	 * Sets just the object with no check;
	 * @param answerTo
	 */
	public void setAnswerTo(D_Justification answerTo) {
		this.answerTo = answerTo;
	}
	/**
	 * Sets the object, the LID and the GID
	 * 
	 * @param answerTo
	 */
	public void setAnswerTo_SetAll(D_Justification answerTo) {
		this.answerTo = answerTo;
		if (answerTo != null) {
//			this.setAnswerTo_GID(answerTo.getAnswerTo_GID());
//			this.setAnswerToLIDstr(answerTo.getAnswerToLIDstr());
			this.setAnswerTo_GID(answerTo.getGID());
			this.setAnswerToLIDstr(answerTo.getLIDstr());
		} else {
			this.setAnswerTo_GID(null);
			this.setAnswerToLIDstr(null);
		}
		// dirty set if LID is new
		//this.dirty_main = true;
	}
	public long getLID_force() {
		if (this.justification_ID != null) return this.getLID();
		return this.storeSynchronouslyNoException();
	}
	/**
	 * Return the available value (no force)
	 * @return
	 */
	public long getLID() {
		return Util.lval(justification_ID);
	}
	public String getLIDstr() {
		return justification_ID;
	}
	public void setLIDstr(String justification_ID) {
		this.justification_ID = justification_ID;
	}
	public void setLID(long __justification_ID) {
		this.justification_ID = "" + __justification_ID;
	}
	/**
	 * If _motionLID is positive, and if the object is linked by GID, it will be also linked by LID.
	 * @param _motionLID
	 */
	public void setLID_AndLink(long _justificationLID) {
		setLID(_justificationLID);
		if (_justificationLID > 0)
			D_Justification_Node.register_newLID_ifLoaded(this);
	}
	public String getMotionLIDstr() {
		return motion_ID;
	}
	public long getMotionLID() {
		return Util.lval(motion_ID);
	}
	public void setMotionLIDstr(String motion_ID) {
		this.motion_ID = motion_ID;
		this.dirty_main = true;
	}
	public void setMotionLID(long _motion_ID) {
		this.motion_ID = Util.getStringID(_motion_ID);
		this.dirty_main = true;
	}
	public String getAnswerToLIDstr() {
		return answerTo_ID;
	}
	/**
	 * Would not work an a remote justification just decoded, (where the GID is set but not the LID)
	 * @return
	 */
	public long getAnswerToLID() {
		//if (this.loaded_locals)
		return Util.lval(answerTo_ID);
	}
	public void setAnswerToLIDstr(String answerTo_ID) {
		if (! Util.equalStrings_null_or_not(this.answerTo_ID, answerTo_ID)) {
			this.answerTo_ID = answerTo_ID;
			this.dirty_main = true;
		}
	}
	/**
	 * Converted from string
	 * @return
	 */
	public long getConstituentLID() {
		return Util.lval(constituent_ID);
	}
	/**
	 * Just returned.
	 * @return
	 */
	public String getConstituentLIDstr() {
		return constituent_ID;
	}
	/**
	 * Setting dirty. It is preferable to use setConstituentAll.
	 * @param constituent_ID
	 */
	public void setConstituentLIDstr_Dirty(String constituent_ID) {
		if (! Util.equalStrings_and_not_null(this.constituent_ID, constituent_ID)) {
			this.constituent_ID = constituent_ID;
			this.dirty_main = true;
		}
	}
	/**
	 * Sets dirty. It is preferable to use setConstituentAll.
	 * @param _constituent_ID
	 */
	public void setConstituentLID_Dirty(long _constituent_ID) {
		setConstituentLIDstr_Dirty(Util.getString(_constituent_ID));
	}
	public String getOrganizationLIDstr() {
		return organization_ID;
	}
	public long getOrganizationLID() {
		return Util.lval(organization_ID);
	}
	public void setOrganizationLIDstr(String organization_ID) {
		this.organization_ID = organization_ID;
		//this.dirty_main = true; // not saved in db
	}
	public void setOrganizationLID(long _organization_ID) {
		this.organization_ID = "" + _organization_ID;
		//this.dirty_main = true; // not in db
	}
	public boolean isRequested() {
		return requested;
	}
	public void setRequested(boolean requested) {
		this.requested = requested;
		this.setPreferencesDate();
		this.dirty_preferences = true;
	}
	public boolean isBlocked() {
		return blocked;
	}
	/**
	 * On change Sets dirty_preferences and preferences_date
	 * 
	 * @param blocked
	 * @return
	 */
	public boolean setBlocked(boolean blocked) {
		if (blocked == this.blocked) return blocked;
		this.blocked = blocked;
		this.setPreferencesDate();
		this.dirty_preferences = true;
		return blocked;
	}
	public boolean isBroadcasted() {
		return broadcasted;
	}
	/**
	 * On change Sets dirty_preferences and preferencesdate
	 * @param broadcasted
	 * @return
	 */
	public boolean setBroadcasted(boolean broadcasted) {
		if (broadcasted == this.broadcasted) return broadcasted;
		this.broadcasted = broadcasted;
		this.setPreferencesDate();
		this.dirty_preferences = true;
		return broadcasted;
	}
	public void setPeerSourceLID(long l) {
		this.peer_source_ID = l;
		this.dirty_local = true;
	}

	public boolean isTemporary() {
		return temporary;
	}

	public void setTemporary(boolean temporary) {
		if (temporary != this.temporary) {
			if (DEBUG) {
				System.out.println("D_Justification: setTemporary: "+temporary);
				//Util.printCallPath("");
			}
		} else return;
		this.temporary = temporary;
		this.dirty_local = true;
	}
	/**
	 * On change Sets dirty_preferences
	 * 
	 * @param blocked
	 * @return
	 */
	public boolean setTemporaryCheck(boolean temporary) {
		if (temporary == this.temporary) return temporary;
		if (temporary && this.getGIDH() != null && this.justification_title != null) return false;
		if (!temporary && (this.getGIDH() == null || this.justification_title == null)) return true;
		this.temporary = temporary;
		//this.setPreferencesDate();
		this.dirty_local = true;
		return temporary;
	}
	/**
	 * 
	 * @param p_cGID
	 * @param p_oLID
	 * @param p_mLID
	 * @param __peer
	 * @param default_blocked
	 * @return the LID
	 */
	public static long insertTemporaryGID(String p_GID,
			long p_oLID, long p_mLID, D_Peer __peer, boolean default_blocked) {
		D_Justification obj = D_Justification.insertTemporaryGID_org(p_GID, p_oLID, p_mLID, __peer, default_blocked);
		if (obj == null) return -1;
		return obj.getLID_force(); 
	}
	/**
	 * 
	 * @param p_GID
	 * @param p_oLID
	 * @param p_mLID
	 * @param __peer
	 * @param default_blocked
	 * @return the object
	 */
	public static D_Justification insertTemporaryGID_org(
			String p_GID, long p_oLID, long p_mLID,
			D_Peer __peer, boolean default_blocked) {
		D_Justification obj;
		if ((p_GID != null)) {
			obj = D_Justification.getJustByGID(p_GID, true, true, true, __peer, p_oLID, p_mLID, null);
			//consts.setName(_name);
			obj.setBlocked(default_blocked);
			if (obj.isTemporary() && obj.getArrivalDate() == null)
				obj.setArrivalDate();
			if (obj.dirty_any()) obj.storeRequest();
			obj.releaseReference();
		}
		else return null;
		return obj; 
	}
	/**
	 * To be called for an arriving and decoded object that was checked for GID correctness
	 * and no blocking, and that is saved directly, without loadRemote().
	 * 
	 * Sets Temporary to false and sets an arrival time. It also sets dirty flags.
	 * Created temporary items are set to no blocking.
	 * @param sol_rq
	 * @param new_rq
	 * @param __peer
	 */
	public void fillLocals(RequestData sol_rq, RequestData new_rq, D_Peer __peer) {
		boolean default_blocked_org = DD.BLOCK_NEW_ARRIVING_TMP_ORGS;// false;
		boolean default_blocked_cons = DD.BLOCK_NEW_ARRIVING_TMP_CONSTITUENT; //false;
		boolean default_blocked_mot = DD.BLOCK_NEW_ARRIVING_TMP_MOTIONS; //false;

		if (this.global_organization_ID != null && this.getOrganizationLID() <= 0) {
			long oID = D_Organization.getLIDbyGIDorGIDH(getOrgGID(), getOrgGIDH());
			if (oID <= 0) {
				oID = D_Organization.insertTemporaryGID(getOrgGID(), getOrgGIDH(), default_blocked_org, __peer);
				if (new_rq != null) new_rq.orgs.add(getGIDH());
			}
			this.setOrganizationLID(oID);
		}
		if (this.global_constituent_ID != null && this.getConstituentLID() <= 0) {
			long cID = D_Constituent.getLIDFromGID_or_GIDH(getConstituentGID(), getConstituentGIDH(), getOrganizationLID());
			if (cID <= 0) {
				cID = D_Constituent.insertTemporaryGID(getConstituentGID(), getConstituentGIDH(), this.getOrganizationLID(), __peer, default_blocked_cons);
				if (new_rq != null) new_rq.cons.put(getConstituentGIDH(), DD.EMPTYDATE);
			}
			this.setConstituentLID_Dirty(cID);
			//this.constituent = r.constituent;
		}
		if (this.global_motionID != null && this.getMotionLID() <= 0) {
			long mID = D_Motion.getLIDFromGID(getMotionGID(), this.getOrganizationLID());
			if (mID <= 0) {
				mID = D_Motion.insertTemporaryGID(this.getMotionGID(), this.getOrganizationLID(), __peer, default_blocked_mot);
				if (new_rq != null) new_rq.moti.add(getMotionGID());
			}
			this.setMotionLID(mID);
		}
		if (this.global_answerTo_ID != null && this.getAnswerToLID() <= 0) {
			this.setAnswerToLIDstr(D_Justification.getLIDstrFromGID(getAnswerTo_GID(), this.getOrganizationLID(), this.getMotionLID()));
			if (new_rq != null) new_rq.just.add(getAnswerTo_GID());
			//this.answerTo = r.answerTo;
		}
		this.dirty_main = true;
		if (sol_rq != null) sol_rq.just.add(this.getGID());
		if (this.peer_source_ID <= 0 && __peer != null)
			this.peer_source_ID = __peer.getLID_keep_force();
		this.setTemporary(false);
		this.setArrivalDate();
		this.dirty_local = true;
	}
	/**
	 * Tests this temporary. Set temporary to false.
	 * @param r
	 * @param sol_rq
	 * @param new_rq
	 * @return 
	 */
	public boolean loadRemote(D_Justification r, RequestData sol_rq,
			RequestData new_rq, D_Peer __peer) {
		boolean default_blocked_org = DD.BLOCK_NEW_ARRIVING_TMP_ORGS;// false;
		boolean default_blocked_cons = DD.BLOCK_NEW_ARRIVING_TMP_CONSTITUENT; //false;
		boolean default_blocked_mot = DD.BLOCK_NEW_ARRIVING_TMP_MOTIONS; //false;
		
		if (! this.isTemporary()) {
			if (DEBUG) System.out.println("D_Justification: loadRemote: this not temporary! quit: "+this);
			return false;
		}
		
		this.hash_alg = r.hash_alg;
		this.justification_title = r.justification_title;
		this.justification_text = r.justification_text;
		this.creation_date = r.creation_date;
		this.signature = r.signature;
		
		if (! Util.equalStrings_null_or_not(this.global_justificationID, r.global_justificationID)) {
			this.global_justificationID = r.global_justificationID;
			this.setLIDstr(null);
		}
		if (! Util.equalStrings_null_or_not(this.global_organization_ID, r.global_organization_ID)) {
			this.setOrgGID(r.getOrgGID());
			long oID = D_Organization.getLIDbyGIDorGIDH(r.getOrgGID(), r.getOrgGIDH());
			if (oID <= 0) {
				oID = D_Organization.insertTemporaryGID(r.getOrgGID(), r.getOrgGIDH(), default_blocked_org, __peer);
				if (new_rq != null) new_rq.orgs.add(r.getGIDH());
			}
			this.setOrganizationLID(oID);
		}
		if (! Util.equalStrings_null_or_not(this.global_constituent_ID, r.global_constituent_ID)) {
			this.setConstituentGID(r.getConstituentGID());
			long cID = D_Constituent.getLIDFromGID_or_GIDH(r.getConstituentGID(), r.getConstituentGIDH(), getOrganizationLID());
			if (cID <= 0) {
				cID = D_Constituent.insertTemporaryGID(r.getConstituentGID(), r.getConstituentGIDH(), this.getOrganizationLID(), __peer, default_blocked_cons);
				if (new_rq != null) new_rq.cons.put(r.getConstituentGIDH(), DD.EMPTYDATE);
			}
			this.setConstituentLID_Dirty(cID);
			//this.constituent = r.constituent;
		}
		if (! Util.equalStrings_null_or_not(this.global_motionID, r.global_motionID)) {
			this.setMotionGID(r.getMotionGID());
			long mID = D_Motion.getLIDFromGID(r.getMotionGID(), this.getOrganizationLID());
			if (mID <= 0) {
				mID = D_Motion.insertTemporaryGID(this.getMotionGID(), this.getOrganizationLID(), __peer, default_blocked_mot);
				if (new_rq != null) new_rq.moti.add(r.getMotionGID());
			}
			this.setMotionLID(mID);
		}
		if (! Util.equalStrings_null_or_not(this.global_answerTo_ID, r.global_answerTo_ID)) {
			this.setAnswerTo_GID(r.getAnswerTo_GID());
			this.setAnswerToLIDstr(D_Justification.getLIDstrFromGID(r.getAnswerTo_GID(), this.getOrganizationLID(), this.getMotionLID()));
			if (new_rq != null) new_rq.just.add(r.getAnswerTo_GID());
			//this.answerTo = r.answerTo;
		}
		this.dirty_main = true;
		if (sol_rq != null) sol_rq.just.add(this.getGID());
		if (this.peer_source_ID <= 0 && __peer != null)
			this.peer_source_ID = __peer.getLID_keep_force();
		this.setTemporary(false);
		this.setArrivalDate();
		this.dirty_local = true;
		return true;
	}

	/**
	 * Copies text, motion, org, answerTo.
	 * Sets arrival and creation date.
	 * 
	 * Used in editors when starting a new justification resembling an older one.
	 * @param r
	 * @return
	 */
	public boolean loadDuplicatingInTemporary(D_Justification r) {
		if (! this.isTemporary()) {
			if (_DEBUG) System.out.println("D_Justification: loadRemote: this not temporary! quit: "+this);
			return false;
		}
		// this.hash_alg = r.hash_alg;
		this.justification_title = r.justification_title;
		this.justification_text = r.justification_text;
		if (! Util.equalStrings_null_or_not(this.global_organization_ID, r.global_organization_ID)) {
			this.setOrgGID(r.getOrgGID());
			long oID = r.getOrganizationLID();
			if (oID <= 0)
				oID = D_Organization.getLIDbyGIDorGIDH(r.getOrgGID(), r.getOrgGIDH());
			this.setOrganizationLID(oID);
		}
		if (! Util.equalStrings_null_or_not(this.global_motionID, r.global_motionID)) {
			this.setMotionGID(r.getMotionGID());
			long mID = r.getMotionLID();
			if (mID <= 0) 
				mID = D_Motion.getLIDFromGID(r.getMotionGID(), this.getOrganizationLID());
			this.setMotionLID(mID);
		}
		if (! Util.equalStrings_null_or_not(this.global_answerTo_ID, r.global_answerTo_ID)) {
			this.setAnswerTo_GID(r.getAnswerTo_GID());
			String aLID = r.getAnswerToLIDstr();
			if (aLID == null) aLID = D_Justification.getLIDstrFromGID(r.getAnswerTo_GID(), this.getOrganizationLID(), this.getMotionLID());
			this.setAnswerToLIDstr(aLID);
			this.answerTo = r.answerTo;
		}
		this.dirty_main = true;
		this.setCreationDate(this.setArrivalDate());
		this.dirty_local = true;
		return true;
	}
	/**
	 * Counts the number of Justifications for a given motion.
	 * Not cached!
	 * @param motion_ID2
	 * @return
	 */
	public static long getCountForMotion(long motion_ID2) {
		String sql = "SELECT count(*) FROM "+net.ddp2p.common.table.justification.TNAME+" WHERE "+net.ddp2p.common.table.justification.motion_ID+"=?;";
		ArrayList<ArrayList<Object>> count;
		try {
			count = Application.db.select(sql, new String[]{Util.getStringID(motion_ID2)});
			long justifications = Integer.parseInt(count.get(0).get(0).toString());
			return justifications;
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Maps days_old into # news
	 */
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
		long result = D_News.getCountJust(this, days);
		countNews_memory.put(new Integer(days), new Long(result));
		return result;
	}
	/**
	 * Maps days_old into #signatures (days=0 for all)
	 */
	Hashtable<Integer, Long> activity_memory = new Hashtable<Integer, Long>();
	/**
	 * With cache (use days = 0 for all)
	 * 
	 * @param days (in arrival time)
	 * @param refresh
	 * @return
	 */
	public long getActivityNb_ByAge_WithCache(int days, boolean refresh) {
		if (! refresh) {
			Long r = activity_memory.get(new Integer(days));
			if (r != null) return r;
		}
		long result = getActivityNb_ByAge(days);
		activity_memory.put(new Integer(days), new Long(result));
		return result;
	}
	/**
	 * Maps days_old into #signatures (days=0 for all)
	 */
	Hashtable<String, Long> activity_memory_by_choice = new Hashtable<String, Long>();
	/**
	 * With cache (use days = 0 for all)
	 * 
	 * @param days (in arrival time)
	 * @param refresh
	 * @return
	 */
	public long getActivityNb_ByChoice_WithCache(String choice, boolean refresh) {
		if (! refresh) {
			Long r = activity_memory_by_choice.get(choice);
			if (r != null) return r;
		}
		long result = getActivityNb_ByChoice(choice);
		activity_memory_by_choice.put(choice, new Long(result));
		return result;
	}
	public void setActivityNb_ByChoice(Long v, String choice) {
		if (choice != null)
			activity_memory_by_choice.put(choice, v);
		activity_memory.put(Integer.valueOf(0), v);
	}

	private static final String sql_signatures_count_all = "SELECT count(*) FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.justification_ID+" = ?;";
	private static final String sql_signatures_count_all_recent_only = "SELECT count(*) FROM "+net.ddp2p.common.table.signature.TNAME+
			" WHERE "+net.ddp2p.common.table.signature.justification_ID+" = ? AND "+net.ddp2p.common.table.signature.arrival_date+">?;";
	private static final String sql_signatures_count_with_choice_choice = 
			"SELECT count(*) FROM "+net.ddp2p.common.table.signature.TNAME
			+ " WHERE "+net.ddp2p.common.table.signature.choice+"=? AND " + net.ddp2p.common.table.signature.justification_ID + " =? "+";";
	public long getActivityNb_All() {
		return getActivityNb_ByAge(0);
	}
	/**
	 * Without cache (use days = 0 for all)
	 * @param days
	 * @return
	 */
	public long getActivityNb_ByAge(int days) {
		try {
			ArrayList<ArrayList<Object>> orgs;
			if (days == 0)
				orgs = Application.db.select(sql_signatures_count_all, new String[]{this.getLIDstr()});
			else
				orgs = Application.db.select(sql_signatures_count_all_recent_only, new String[]{this.getLIDstr(), Util.getGeneralizedDate(days)});
			
			if (orgs.size() > 0) return Util.lval(orgs.get(0).get(0));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	/**
	 * No cache.
	 * @param choice
	 * @return
	 */
	public long getActivityNb_ByChoice(String choice) {
		try {
			ArrayList<ArrayList<Object>> orgs = Application.db.select(sql_signatures_count_with_choice_choice,
					new String[]{choice, this.getLIDstr()}, DEBUG);
			if (orgs.size() > 0) return Util.lval(orgs.get(0).get(0));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	/**
	 * Number of signature with the support choice: D_Vote.DEFAULT_YES_COUNTED_LABEL
	 * Not used (not cached)
	 * @return
	 */
	public long getActivityNb_Support_Choice() {
		try {
			ArrayList<ArrayList<Object>> orgs = Application.db.select(sql_signatures_count_with_choice_choice,
					new String[]{D_Vote.DEFAULT_YES_COUNTED_LABEL, this.getLIDstr()}, DEBUG);
			if (orgs.size() > 0) return Util.lval(orgs.get(0).get(0));
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	/**
	 * ForAllJustifications
	 */
	private static final String sql_no_choice_no_answer = 
			"SELECT "
			+ "j." + net.ddp2p.common.table.justification.justification_ID 
			+ ", count(*) AS cnt "
			+ ", max(s."+net.ddp2p.common.table.signature.justification_ID+") AS maxsignID "
			+" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "
			+" LEFT JOIN "+net.ddp2p.common.table.signature.TNAME+" AS s ON(s."+net.ddp2p.common.table.signature.justification_ID+"=j."+net.ddp2p.common.table.justification.justification_ID+")"+
			" WHERE j."+net.ddp2p.common.table.justification.motion_ID + "=?"+
			" GROUP BY j."+net.ddp2p.common.table.justification.justification_ID+
			" ORDER BY cnt DESC"+
			";";
	private static final String sql_choice_no_answer = 
			"SELECT "
			+ "j."+net.ddp2p.common.table.justification.justification_ID 
			+ ", count(*) AS cnt "
			+" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "
			+" JOIN "+net.ddp2p.common.table.signature.TNAME+" AS s ON(s."+net.ddp2p.common.table.signature.justification_ID+"=j."+net.ddp2p.common.table.justification.justification_ID+")"+
			" WHERE s."+net.ddp2p.common.table.signature.choice+"=? AND j."+net.ddp2p.common.table.justification.motion_ID + "=?"+
			" GROUP BY j."+net.ddp2p.common.table.justification.justification_ID+
			" ORDER BY cnt DESC"+
			";";
	private static final String sql_no_choice_answer = 
			"SELECT "
			+ "j."+net.ddp2p.common.table.justification.justification_ID
			+ ", count(*) AS cnt "
			+ ", max(s."+net.ddp2p.common.table.signature.justification_ID+") AS maxsignID "
			+" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "
			+" LEFT JOIN "+net.ddp2p.common.table.signature.TNAME+" AS s ON(s."+net.ddp2p.common.table.signature.justification_ID+"=j."+net.ddp2p.common.table.justification.justification_ID+")"+
			" WHERE j."+net.ddp2p.common.table.justification.motion_ID + "=? AND j."+net.ddp2p.common.table.justification.answerTo_ID+"=? "+
			" GROUP BY j."+net.ddp2p.common.table.justification.justification_ID+
			" ORDER BY cnt DESC"+
			";";
	private static final String sql_choice_answer = 
			"SELECT "
			+ "j."+net.ddp2p.common.table.justification.justification_ID
			+ ", count(*) AS cnt "
			+" FROM "+net.ddp2p.common.table.justification.TNAME+" AS j "
			+" JOIN "+net.ddp2p.common.table.signature.TNAME+" AS s ON(s."+net.ddp2p.common.table.signature.justification_ID+"=j."+net.ddp2p.common.table.justification.justification_ID+")"+
			" WHERE s."+net.ddp2p.common.table.signature.choice+"=? AND j."+net.ddp2p.common.table.justification.motion_ID + "=? AND j."+net.ddp2p.common.table.justification.answerTo_ID+"=? "+
			" GROUP BY j."+net.ddp2p.common.table.justification.justification_ID+
			" ORDER BY cnt DESC"+
			";";
	static final int SELECT_ALL_JUST_LID = 0;
	static final int SELECT_ALL_JUST_CNT = 1; //7;
	static final int SELECT_ALL_JUST_MAX_JUST_ID_FOR_SIGN = 2; //10;// only for: sql_no_choice_no_answer and sql_no_choice_answer
	
	/**
	 * If provided crt_choice is null, then return all justifications for this motion.
	 * If provided crt_answered_LID is not null, filter and get only those justifications answering it.
	 * 
	 * Returns an array of three columns. 
	 * On first column (SELECT_ALL_JUST_LID), get the justification LID
	 * On second column (SELECT_ALL_JUST_CNT), get the number of votes/results for this justification
	 * On third column (SELECT_ALL_JUST_MAX_JUST_ID_FOR_SIGN), get the LID for justification (or NULL if no signature)
	 * 
	 * @param crt_motion_LID
	 * @param crt_choice
	 * @param crt_answered_LID
	 * @return
	 */
	public static ArrayList<ArrayList<Object>> getAllJustifications (String crt_motion_LID, String crt_choice, String crt_answered_LID) {
		ArrayList<ArrayList<Object>> justi = new ArrayList<ArrayList<Object>>();
		//boolean DEBUG = true;
		if (DEBUG) System.out.println("D_Justification: getAllJustifications: start");
		try {
			DBInterface db = Application.db;
			if ((crt_choice == null) && (crt_answered_LID == null)) justi = db.select(sql_no_choice_no_answer, new String[]{crt_motion_LID}, DEBUG);
			if ((crt_choice != null) && (crt_answered_LID == null)) justi = db.select(sql_choice_no_answer, new String[]{crt_choice, crt_motion_LID}, DEBUG);
			if ((crt_choice == null) && (crt_answered_LID != null)) justi = db.select(sql_no_choice_answer, new String[]{crt_motion_LID, crt_answered_LID}, DEBUG);
			if ((crt_choice != null) && (crt_answered_LID != null)) justi = db.select(sql_choice_answer, new String[]{crt_choice, crt_motion_LID, crt_answered_LID}, DEBUG);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return justi;
	}
	public static class JustificationSupportEntry {
		private String justification_LID;
		private int support_cnt;
		public String toString() {
			return "JustifSE[LID="+justification_LID+" cnt="+support_cnt+"]";
		}
		public long getJustification_LID() {
			return Util.lval(justification_LID);
		}
		public String getJustification_LIDstr() {
			return justification_LID;
		}
		public void setJustification_LID(String justification_LID) {
			this.justification_LID = justification_LID;
		}
		public String getSupportCntStr() {
			return "" + support_cnt;
		}
		public int getSupportCnt() {
			return support_cnt;
		}
		public void setSupportCnt(int support_cnt) {
			this.support_cnt = support_cnt;
		}
	}
	/**
	 * Returns a list with the count of support of type "choice" for each justification of the motion in parameter.
	 * Choice type null means all signatures.
	 * 
	 * @param crt_motion_LID
	 * @param crt_choice
	 * @param crt_answered_LID
	 * @return
	 */
	public static ArrayList<JustificationSupportEntry> getAllJustificationsCnt (String crt_motion_LID, String crt_choice, String crt_answered_LID) {
		if (DEBUG) System.out.println("D_Justification: getAllJustificationsCnt: LID="+crt_motion_LID+" ch="+crt_choice+" aLID="+crt_answered_LID);
		ArrayList<JustificationSupportEntry> justi = new ArrayList<JustificationSupportEntry>();
		
		ArrayList<ArrayList<Object>> js = getAllJustifications(crt_motion_LID, crt_choice, crt_answered_LID);
		
		for (ArrayList<Object> j: js) {
			JustificationSupportEntry _j = new JustificationSupportEntry();
			_j.setJustification_LID(Util.getString(j.get(SELECT_ALL_JUST_LID)));
			_j.setSupportCnt(Util.ival(j.get(SELECT_ALL_JUST_CNT), 0));
			if (j.size() > SELECT_ALL_JUST_MAX_JUST_ID_FOR_SIGN && j.get(SELECT_ALL_JUST_MAX_JUST_ID_FOR_SIGN) == null) {
				_j.setSupportCnt(0);
			}
			justi.add(_j);
		}
		return justi;
	}

	Boolean __LastGIDValid = null;
	public boolean isLastGIDChecked() {
		return __LastGIDValid != null;//__isLastGIDChecked;
	}
	public Boolean getLastGIDValid() {
		return __LastGIDValid;
	}
	public void setLastGIDValid(Boolean result) {
		__LastGIDValid = result;
	}
	/**
	 * Returns the last valid GID if available
	 * @param refresh 
	 * Set it to true to force re-verification
	 * @return
	 */
	public Boolean getGIDValid_WithMemory(boolean refresh) {
		Boolean result;
		if (! refresh && isLastGIDChecked()) result = getLastGIDValid();
		else {
			//System.out.println("D_Motion: getSignValid_WithMemory: check GID");
			String newGID = this.make_ID();
			if (newGID == null) result = new Boolean(false);
			else result = new Boolean(newGID.equals(this.getGID()));
			setLastGIDValid((Boolean)result);
		}
		return result;
	}
	Boolean __LastSignValid = null;
	public boolean isLastSignChecked() {
		return __LastSignValid != null;//__isLastGIDChecked;
	}
	public Boolean getLastSignValid() {
		return __LastSignValid;
	}
	public void setLastSignValid(Boolean result) {
		__LastSignValid = result;
	}
	public Boolean getSignValid_WithMemory(boolean refresh) {
		Boolean result;
		if (! refresh && isLastSignChecked()) result = getLastSignValid();
		else {
			byte[] sgn = this.getSignature();
			if (sgn == null) result = new Boolean(false);
			//else result = new Boolean(sgn.length > 0);
//			//System.out.println("D_Motion: getSignValid_WithMemory: check Sign");
//			if ((getGID() == null) || (getConstituentGID() == null)) result = new Boolean(false);
			else result = new Boolean(verifySignature());
			
			setLastSignValid((Boolean)result);
		}
		return result;
	}

	/**
	 * Returns the title document raw
	 * @return
	 */
	public String getJustificationTitleText() {
		if (getJustificationTitle() == null) return null;
		return this.getJustificationTitle().title_document.getDocumentUTFString();
	}
	/**
	 * Returns the title format
	 * @return
	 */
	public String getJustificationTitleFormat() {
		if (getJustificationTitle() == null) return null;
		return this.getJustificationTitle().title_document.getFormatString();
	}
	/**
	 * sets dirty
	 * @param new_text
	 */
	public void setJustificationTitleText(String new_text) {
		if (! Util.equalStrings_null_or_not(new_text, this.getJustificationTitleText())) {
			this.getJustificationTitle().title_document.setDocumentString(new_text);
			this.dirty_main = true;
		}
	}
	/**
	 * sets dirty
	 * @param new_text
	 */
	public void setJustificationTitleFormat(String new_text) {
		if (! Util.equalStrings_null_or_not(new_text, this.getJustificationTitleFormat())) {
			this.getJustificationTitle().title_document.setFormatString(new_text);
			this.dirty_main = true;
		}
	}
	/**
	 * MARKS DIRTY.
	 * @param new_text
	 * @param editor_format
	 */
	public void setJustificationTitleTextAndFormat(String new_text, String editor_format) {
		String old_text = this.getJustificationTitle().title_document.getDocumentString();
		
		String old_old_text = this.getJustificationTitle().title_document.getFormatString();

		if (! Util.equalStrings_null_or_not(new_text, old_text)) {
			this.getJustificationTitle().title_document.setDocumentString(new_text);
			this.dirty_main = true;
		}
		if (! Util.equalStrings_null_or_not(editor_format, old_old_text)) {
			this.getJustificationTitle().title_document.setFormatString(editor_format);//BODY_FORMAT);
			this.dirty_main = true;
		}
	}
	/**
	 * Returns the body document raw
	 * @return
	 */
	public String getJustificationBodyText() {
		if (getJustificationBody() == null) return null;
		return this.getJustificationBody().getDocumentUTFString();
	}
	/**
	 * Returns the body format
	 * @return
	 */
	public String getJustificationBodyFormat() {
		if (getJustificationBody() == null) return null;
		return this.getJustificationBody().getFormatString();
	}
	/**
	 * sets dirty
	 * @param new_text
	 */
	public void setJustificationBodyText(String new_text) {
		if (! Util.equalStrings_null_or_not(new_text, this.getJustificationBodyText())) {
			this.getJustificationBody().setDocumentString(new_text);
			this.dirty_main = true;
		}
	}
	/**
	 * sets dirty
	 * @param new_text
	 */
	public void setJustificationBodyFormat(String new_text) {
		if (! Util.equalStrings_null_or_not(new_text, this.getJustificationBodyFormat())) {
			this.getJustificationBody().setFormatString(new_text);
			this.dirty_main = true;
		}
	}
	/**
	 * MARKS DIRTY.
	 * @param new_text
	 * @param editor_format
	 */
	public void setJustificationBodyTextAndFormat(String new_text, String editor_format) {
		String old_text = this.getJustificationBody().getDocumentString();
		
		String old_old_text = this.getJustificationBody().getFormatString();

		if (! Util.equalStrings_null_or_not(new_text, old_text)) {
			this.getJustificationBody().setDocumentString(new_text);
			this.dirty_main = true;
		}
		if (! Util.equalStrings_null_or_not(editor_format, old_old_text)) {
			this.getJustificationBody().setFormatString(editor_format);//BODY_FORMAT);
			this.dirty_main = true;
		}
	}
	public D_Document_Title getJustificationTitle() {
		return justification_title;
	}
	/**
	 * Returns null if the title is not TXT or HTML, else return the text content
	 * @return
	 */
	public String getJustificationTitleStr() {
		if (justification_title == null || justification_title.title_document == null)
			return null;
		if (
				justification_title.title_document.getFormatString() != null
				&&
				! D_Document.TXT_FORMAT.equals(justification_title.title_document.getFormatString())
				&&
				! D_Document.HTM_BODY_FORMAT.equals(justification_title.title_document.getFormatString())
		) return null;
		return justification_title.title_document.getDocumentUTFString();
	}
	/**
	 * Marks always the dirty flag
	 * 
	 * @param justification_title
	 */
	public void setJustificationTitle(D_Document_Title justification_title) {
		this.justification_title = justification_title;
		this.dirty_main = true;
	}
	public D_Document getJustificationBody() {
		return justification_text;
	}
	/**
	 * Marks always the dirty flag
	 * @param justification_text
	 */
	public void setJustificationBody(D_Document justification_text) {
		this.justification_text = justification_text;
		this.dirty_main = true;
	}

	public void resetCache() {
		__LastSignValid = null;
		__LastGIDValid = null;
		//supports_memory.clear(); //.remove(new Integer(_choice));
		countNews_memory.clear();
		activity_memory.clear();
	}

	public static int getNumberItemsNeedSaving() {
		return _need_saving.size() + _need_saving_obj.size();
	}

	/**
	 * TODO: expand to check that there is at least one D_Vote shipped with it and refering it
	 * @return
	 */
	public boolean isGIDValidAndNotBlocked() {
		if (! getGIDValid_WithMemory(false)) return true;
		if (this.getConstituentGID() != null) {
			if (this.getConstituent().isBlocked()) return false;
			if (! getSignValid_WithMemory(true)) return false;
		}
		if (this.getMotionForce().isBlocked()) return false;
		if (this.getOrganizationForce().getBlocked()) return false;
		return true;
	}
}

class D_Justification_SaverThread extends net.ddp2p.common.util.DDP2P_ServiceThread {
	//private static final long SAVER_SLEEP_ON_ERROR = 2000;
	boolean stop = false;
	/**
	 * The next monitor is needed to ensure that two D_Justification_SaverThreadWorker are not concurrently modifying the database,
	 * and no thread is started when it is not needed (since one is already running).
	 */
	public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Justification_SaverThread() {
		super("D_Justification Saver", true);
		//start ();
	}
	public void _run() {
		for (;;) {
			if (stop) return;
			if (net.ddp2p.common.data.SaverThreadsConstants.getNumberRunningSaverThreads() < SaverThreadsConstants.MAX_NUMBER_CONCURRENT_SAVING_THREADS && D_Justification.getNumberItemsNeedSaving() > 0)
			synchronized(saver_thread_monitor) {
				new D_Justification_SaverThreadWorker().start();
			}
			/*
			synchronized(saver_thread_monitor) {
				D_Justification de = D_Justification.need_saving_next();
				if (de != null) {
					if (DEBUG) System.out.println("D_Justification_Saver: loop saving "+de);
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
					//System.out.println("D_Justification_Saver: idle ...");
				}
			}
			*/
			synchronized(this) {
				try {
					long timeout = (D_Justification.getNumberItemsNeedSaving() > 0)?
							SaverThreadsConstants.SAVER_SLEEP_BETWEEN_JUSTIFICATION_MSEC:
								SaverThreadsConstants.SAVER_SLEEP_WAITING_JUSTIFICATION_MSEC;
					wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

class D_Justification_SaverThreadWorker extends net.ddp2p.common.util.DDP2P_ServiceThread {
	boolean stop = false;
	//public static final Object saver_thread_monitor = new Object();
	private static final boolean DEBUG = false;
	D_Justification_SaverThreadWorker() {
		super("D_Justification Saver Worker", false);
		//start ();
	}
	public void _run() {
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_just ++;}
		try{__run();} catch (Exception e){}
		synchronized (SaverThreadsConstants.monitor_threads_counts) {SaverThreadsConstants.threads_just --;}
	}
	@SuppressWarnings("unused")
	public void __run() {
		synchronized(D_Justification_SaverThread.saver_thread_monitor) {
			D_Justification de;
			boolean edited = true;
			if (DEBUG) System.out.println("D_Justification_Saver: start");
			
			 // first try objects being edited
			de = D_Justification.need_saving_obj_next();
			
			// then try remaining objects 
			if (de == null) { de = D_Justification.need_saving_next(); edited = false; }
			
			if (de != null) {
				if (DEBUG) System.out.println("D_Justification_Saver: loop saving "+de.getJustificationTitle());
				Application_GUI.ThreadsAccounting_ping("Saving");
				if (edited) D_Justification.need_saving_obj_remove(de);
				else D_Justification.need_saving_remove(de);//, de.instance);
				if (DEBUG) System.out.println("D_Justification_Saver: loop removed need_saving flag");
				// try 3 times to save
				for (int k = 0; k < net.ddp2p.common.data.SaverThreadsConstants.ATTEMPTS_ON_ERROR; k++) {
					try {
						if (DEBUG) System.out.println("D_Justification_Saver: loop will try saving k="+k);
						de.storeAct();
						if (DEBUG) System.out.println("D_Justification_Saver: stored org:"+de.getJustificationTitle());
						break;
					} catch (P2PDDSQLException e) {
						e.printStackTrace();
						synchronized(this) {
							try {
								if (DEBUG) System.out.println("D_Justification_Saver: sleep");
								wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_JUSTIFICATION_ON_ERROR);
								if (DEBUG) System.out.println("D_Justification_Saver: waked error");
							} catch (InterruptedException e2) {
								e2.printStackTrace();
							}
						}
					}
				}
			} else {
				Application_GUI.ThreadsAccounting_ping("Nothing to do!");
				if (DEBUG) System.out.println("D_Justification_Saver: idle ...");
			}
		}
		if (SaverThreadsConstants.SAVER_SLEEP_WORKER_BETWEEN_JUSTIFICATION_MSEC >= 0) {
			synchronized(this) {
				try {
					if (DEBUG) System.out.println("D_Justification_Saver: sleep");
					wait(SaverThreadsConstants.SAVER_SLEEP_WORKER_BETWEEN_JUSTIFICATION_MSEC);
					if (DEBUG) System.out.println("D_Justification_Saver: waked");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}

