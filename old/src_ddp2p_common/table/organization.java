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
package table;

import util.Util;

public class organization {
	public static final int WEIGHTS_TYPE_NONE = 0;
	public static final int WEIGHTS_TYPE_0_1 = 1;
	public static final int WEIGHTS_TYPE_INT = 2;
	public static final int WEIGHTS_TYPE_DEFAULT = WEIGHTS_TYPE_NONE;
	public static final int WEIGHTS_MAX_DEFAULT = 1;

	public static final String Gs_global_organization_ID = "global_organization_IDs_served";
	 public static final String organization_ID = "organization_ID";
	 public static final String global_organization_ID = "global_organization_ID";
	 public static final String global_organization_ID_hash = "global_organization_ID_hash";
     public static final String name = "name"; //name of this organization
     public static final String creator_ID = "creator_ID"; //peer
	 public static final String certification_methods = "certification_methods";
	 public static final String preapproved = "preapproved";
	 public static final String hash_org_alg = "hash_org_alg";
	 //public static final String hash_org = "hash_org";
	 public static final String category = "category";
	 public static final String description = "description";
	 public static final String certificate = "certificate"; // a certificate from globalID from an upper authority
	 public static final String signature = "signature";   // a signature of OrgData (authoritarian) or a hash of OrgData 
	 public static final String signature_initiator = "signature_initiator";   // a signature of OrgData (authoritarian) or a hash of OrgData 
	 public static final String crl = "crl";
	 public static final String icon = "icon";
	 public static final String crl_date = "crl_date";
	 public static final String default_scoring_options = "default_scoring_options";
	 public static final String instructions_new_motions = "instructions_new_motions";
	 public static final String instructions_registration = "instructions_registration";
	 public static final String languages = "languages";
	 public static final String name_forum = "name_forum";
	 public static final String name_constituent = "name_constituent";
	 public static final String name_justification = "name_justification";
	 public static final String name_motion = "name_motion";
	 public static final String name_organization = "name_organization";
	 //public static final String hash_orgID = "hash_orgID";
	 public static final String motions_excluding = "motions_excluding";
	 public static final String plugins_excluding = "plugins_excluding";
	 public static final String reset_date = "reset_date";
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
	 public static final String blocked = "blocked";      // don't tell me about it # do not stored data received for this
	 public static final String requested = "requested";  // request data about this item, in filters
	 public static final String broadcasted = "broadcasted";  // send data for this
	 public static final String specific_requests = "specific_requests";
	 public static final String broadcast_rule = "broadcast_rule";  // set to 1 if by default this org should not be broadcasted
	 public static final String neighborhoods_rule = "neighborhoods_rule";  // set to 1 to allow users to change neighborhood hierarchy structures, 0 for fix structures, 2 for dynamic neighborhoods but no language specs
	 public static final String preferences_date = "preferences_date";
	 public static final String temporary = "temporary";
	 public static final String hidden = "hidden";
	 public static final String weights_type = "weights_type";
	 public static final String weights_max = "weights_max";
	 public static final String first_provider_peer = "peer_source_ID";
	 public static final String TNAME = "organization";	
	 public static final String field_list_noID = " "+util.Util.trimInterFieldSpaces(
			global_organization_ID+","+name+","+             temporary+","+      name_organization+","+        name_forum+"," +
			name_motion+","+           name_justification+","+languages+","+      instructions_registration+","+instructions_new_motions+"," +
			default_scoring_options+","+category+","+         certification_methods+","+hash_org_alg+","+       creation_date+","+  
			certificate+","+      crl+","+                  crl_date+","+           arrival_date+"," +        motions_excluding+","+      
			plugins_excluding+","+description+","+ 			creator_ID+","+ 	    global_organization_ID_hash+"," +signature+","+				
			requested+","+		  blocked+","+		  		broadcasted+","+			specific_requests+","	+reset_date+","+
			signature_initiator+","+broadcast_rule+","+     preapproved+","+            preferences_date+","+   first_provider_peer+","+
			hidden+","+			weights_type+","+			weights_max+","+			icon+","+      		name_constituent+","+
			neighborhoods_rule)+" ";
	 public static final String field_list = field_list_noID + "," + organization_ID;
	 //hash_org+"," +
	public static final int _GRASSROOT = 0;
	public static final int _AUTHORITARIAN = 1;
	public static final int _EXPRESSION = 2;

	public static final String SEP_PREAPPROVED = ",";
	public static final String ORG_HASH_BYTE_SEP = " ";
	//public static final String ORG_SCORE_SEP = ";";
	public static final String ORG_LANG_SEP = ";";
	//public static final String ORG_TRANS_SEP = ";";
	public static final String ORG_VAL_SEP = ";";
	public static final String ORG_ICON_BYTE_SEP = " ";
	//public static final String SEP_scoring_options = ";";
	//public static final String SEP_languages = ";";
	
	public static final int ORG_COL_GID = 0;
	public static final int ORG_COL_NAME = 1;
	public static final int ORG_COL_TEMPORARY = 2;
	public static final int ORG_COL_NAME_ORG = 3;
	public static final int ORG_COL_NAME_FORUM = 4;
	public static final int ORG_COL_NAME_MOTION = 5;
	public static final int ORG_COL_NAME_JUST = 6;
	public static final int ORG_COL_LANG = 7;
	public static final int ORG_COL_INSTRUC_REGIS = 8;
	public static final int ORG_COL_INSTRUC_MOTION = 9;
	public static final int ORG_COL_SCORES = 10;
	public static final int ORG_COL_CATEG = 11;
	public static final int ORG_COL_CERTIF_METHODS = 12;
	public static final int ORG_COL_HASH_ALG = 13;
	//public static final int ORG_COL_HASH = 14;
	public static final int ORG_COL_CREATION_DATE = 14;
	public static final int ORG_COL_CERTIF_DATA = 15;
	// crl 16
	// crl_date 17
	public static final int ORG_COL_ARRIVAL = 18;
	// motions_excluding
	// plugins_excluding
	public static final int ORG_COL_DESCRIPTION = 21;
	public static final String hash_org_alg_crt = "v1";
	public static final int ORG_COL_CREATOR_ID = 22;
	public static final int ORG_COL_GID_HASH = 23;
	public static final int ORG_COL_SIGN = 24;
	public static final int ORG_COL_REQUEST = 25;
	public static final int ORG_COL_BLOCK = 26;
	public static final int ORG_COL_BROADCASTED = 27;
	public static final int ORG_COL_SPECIFIC = 28;
	public static final int ORG_COL_RESET_DATE = 29;
	public static final int ORG_COL_SIGN_INITIATOR = 30;
	public static final int ORG_COL_BROADCAST_RULE = 31;
	public static final int ORG_COL_PREAPPROVED = 32;
	public static final int ORG_COL_PREFERENCES_DATE = 33;
	public static final int ORG_COL_FIRST_PROVIDER_PEER = 34;
	public static final int ORG_COL_HIDDEN = 35;
	public static final int ORG_COL_WEIGHTS_TYPE = 36;
	public static final int ORG_COL_WEIGHTS_MAX = 37;
	public static final int ORG_COL_ICON = 38;
	public static final int ORG_COL_NAME_CONS = 39;
	public static final int ORG_COL_NEIGHBORHOODS_RULE = 40;
	public static final int ORG_COL_ID = 41;
	public static final String[] fields_noID = Util.trimmed(field_list_noID.split(","));
	public static final int FIELDS_NOID = fields_noID.length;
	public static final String[] fields = Util.trimmed(field_list.split(","));
	public static final int FIELDS = fields.length;
}
