/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2011 Marius C. Silaghi
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
 package widgets.identities;

public class Queries{
	public static String sql_identity_enum_leafs =	
		"SELECT i."+table.identity_value.oid_ID+", "+table.oid.OID_name+", "+table.identity_value.value+", i."+table.identity_value.identity_value_ID+", "+table.identity_value.certificate_ID+", o."+table.oid.explanation +", i."+table.identity_value.sequence_ordering +
			" FROM "+table.identity_value.TNAME+" AS i " +
			" LEFT JOIN "+table.oid.TNAME+" AS o ON o."+table.oid.oid_ID+" = i."+table.oid.oid_ID +
		" WHERE "+table.identity_value.identity_ID+" = ? " +
				" ORDER BY "+" i."+table.identity_value.sequence_ordering+", o."+table.oid.sequence+", o."+table.oid.OID_name+";";
	public static String sql_identity_fertility_node =
		"select * " +
		" from "+table.identity.TNAME + " as i " +
		" join "+table.identity_value.TNAME+" as v ON i."+table.identity.identity_ID+"=v."+table.identity_value.identity_ID +
		" WHERE i."+table.identity.identity_ID+" = ?;";
	/**
	 *  Used by IdentityBranch constructor in MyIdentityModel
	 */
	public final static String sql_identity_fertility_nodes =
		"select i."+table.identity.identity_ID+
		  ", i."+table.identity.profile_name+
		  //", o."+table.organization.name +
		  ", COUNT(*) AS count " +
		  ", i."+table.identity.default_id +
		  ", k."+ table.key.secret_key+
		  ", i."+ table.identity.secret_credential+
		  ", k."+ table.key.public_key+
		" from "+table.identity.TNAME+" as i " +
		//" left join "+table.organization.TNAME+" as o ON i."+table.identity.organization_ID+"=o."+table.organization.organization_ID +
		" left join "+table.identity_value.TNAME+" as v ON i."+table.identity.identity_ID+"=v."+table.identity_value.identity_ID +
		" LEFT JOIN "+table.key.TNAME+" AS k ON k."+table.key.ID_hash+"=i."+table.identity.secret_credential+
		" GROUP BY i."+table.identity.identity_ID+";";
	public final static int IFN_IDENTITY = 0;
	public final static int IFN_PROFILE_NAME = 1;
	//public final static int IFN_ORG_NAME = 2;
	public final static int IFN_COUNT = 2;
	public final static int IFN_DEFAULT = 3;
	public final static int IFN_SK = 4;
	public final static int IFN_S_CRED = 5;
	public final static int IFN_PK = 6;
	public static String sql_oids="select "+table.oid.oid_ID+", "+table.oid.OID_name+", "+table.oid.explanation+" from "+table.oid.TNAME+";";
}
