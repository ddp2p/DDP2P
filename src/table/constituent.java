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

import java.util.regex.Pattern;

import util.Util;

public class constituent {
	 public static final String constituent_ID = "constituent_ID";
	 public static final String submitter_ID = "submitter_ID";
	 public static final String global_constituent_ID = "global_constituent_ID";
	 public static final String global_constituent_ID_hash = "global_constituent_ID_hash";
	 public static final String hash_constituent_alg = "hash_constituent_alg";
	 public static final String hash_constituent = "hash_constituent";
	 public static final String sign = "sign";
	 public static final String cert_hash_alg = "cert_hash_alg";
	 public static final String certChain = "certChain";
	 public static final String certificate = "certificate";
	 public static final String certRequest = "certRequest";
	 public static final String email = "email";
	 public static final String external = "external";
	 public static final String forename = "forename";
	 public static final String languages = "languages";
	 public static final String name = "name";
	 public static final String neighborhood_ID = "neighborhood_ID";
	 public static final String organization_ID = "organization_ID";
	 public static final String slogan = "slogan";
	 public static final String picture = "picture";
	 public static final String weight="weight";
	 public static final String op = "op";
	 public static final String revoked = "revoked";
	 public static final String version = "version";
	 public static final String hidden = "hidden";
	 public static final String peer_transmitter_ID = "peer_transmitter_ID"; // who gave it to me
	 public static final String creation_date = "creation_date";
	 public static final String arrival_date = "arrival_date";
		public static final String blocked = "blocked";
		public static final String requested = "requested";
		public static final String broadcasted = "broadcasted";
	public static final String TNAME = "constituent";

	public static final String INIT_SLOGAN = util.Util._("I have just arrived");

	public static final String fields_constituents_no_ID = 
		" "+global_constituent_ID+
		", "+global_constituent_ID_hash+
		", "+hash_constituent_alg+
		//", "+hash_constituent+
		", "+certChain+
		", "+certificate+
		", "+certRequest+
		", "+creation_date+
		", "+email+
		", "+external+
		", "+forename+
		", "+languages+
		", "+name+
		", "+neighborhood_ID+
		", "+organization_ID+
		", "+slogan+
		", "+picture+
		", "+arrival_date+
		", "+op+
		", "+cert_hash_alg+
		", "+weight+
		", "+sign+
		", "+submitter_ID+
		", "+blocked+
		", "+requested+
		", "+broadcasted+
		", "+revoked+
		", "+version+
		", "+hidden+
		", "+peer_transmitter_ID+
		" ";
	public static final String fields_constituents = 
		fields_constituents_no_ID+", "+constituent_ID+
		" ";

	public static final int CONST_COL_GID = 0;
	public static final int CONST_COL_GID_HASH = 1;
	public static final int CONST_COL_HASH_ALG = 2;
	//public static final int CONST_COL_HASH = 2; // hash of fields signed by user
	public static final int CONST_COL_CERT_CHAIN = 3;
	public static final int CONST_COL_CERTIF = 4;
	public static final int CONST_COL_CERREQ = 5;
	public static final int CONST_COL_DATE = 6;
	public static final int CONST_COL_EMAIL = 7;
	public static final int CONST_COL_EXTERNAL = 8;
	public static final int CONST_COL_FORENAME = 9;
	public static final int CONST_COL_LANG = 10;
	public static final int CONST_COL_SURNAME = 11;
	public static final int CONST_COL_NEIGH = 12;
	public static final int CONST_COL_ORG = 13;
	public static final int CONST_COL_SLOGAN = 14;
	public static final int CONST_COL_PICTURE = 15;
	public static final int CONST_COL_ARRIVAL = 16;
	public static final int CONST_COL_OP = 17;
	public static final int CONST_COL_CERT_HASH_ALG = 18;
	public static final int CONST_COL_WEIGHT = 19;
	public static final int CONST_COL_SIGNATURE = 20;
	public static final int CONST_COL_SUBMITTER = 21;
	public static final int CONST_COL_BLOCKED = 22;
	public static final int CONST_COL_REQUESTED = 23;
	public static final int CONST_COL_BROADCASTED = 24;
	public static final int CONST_COL_REVOKED = 25;
	public static final int CONST_COL_VERSION = 26;
	public static final int CONST_COL_HIDDEN = 27;
	public static final int CONST_COL_PEER_TRANSMITTER_ID = 28;
	public static final int CONST_COL_ID = 29;

	public static final String CURRENT_HASH_CONSTITUENT_ALG = "V1";
	
	public static final String[] fields_constituents_no_ID_list = Util.trimmed(table.constituent.fields_constituents_no_ID.split(Pattern.quote(",")));
	public static final String[] fields_constituents_list = Util.trimmed(table.constituent.fields_constituents.split(Pattern.quote(",")));
	public static final int CONST_COLs_NOID = fields_constituents_no_ID_list.length;
	public static final int CONST_COLs = fields_constituents_list.length;
	public static final String SEP_languages = ":";
	public static final String INIT_EXTERNAL_SLOGAN = util.Util._("I may not be aware that I am listed here!");
}
