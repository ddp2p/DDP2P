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
 package net.ddp2p.widgets.identities;
public class Queries{
	public static String sql_identity_enum_leafs =	
		"SELECT i."+net.ddp2p.common.table.identity_value.oid_ID+", "+net.ddp2p.common.table.oid.OID_name+", "+net.ddp2p.common.table.identity_value.value+", i."+net.ddp2p.common.table.identity_value.identity_value_ID+", "+net.ddp2p.common.table.identity_value.certificate_ID+", o."+net.ddp2p.common.table.oid.explanation +", i."+net.ddp2p.common.table.identity_value.sequence_ordering +
			" FROM "+net.ddp2p.common.table.identity_value.TNAME+" AS i " +
			" LEFT JOIN "+net.ddp2p.common.table.oid.TNAME+" AS o ON o."+net.ddp2p.common.table.oid.oid_ID+" = i."+net.ddp2p.common.table.oid.oid_ID +
		" WHERE "+net.ddp2p.common.table.identity_value.identity_ID+" = ? " +
				" ORDER BY "+" i."+net.ddp2p.common.table.identity_value.sequence_ordering+", o."+net.ddp2p.common.table.oid.sequence+", o."+net.ddp2p.common.table.oid.OID_name+";";
	public static String sql_identity_fertility_node =
		"select * " +
		" from "+net.ddp2p.common.table.identity.TNAME + " as i " +
		" join "+net.ddp2p.common.table.identity_value.TNAME+" as v ON i."+net.ddp2p.common.table.identity.identity_ID+"=v."+net.ddp2p.common.table.identity_value.identity_ID +
		" WHERE i."+net.ddp2p.common.table.identity.identity_ID+" = ?;";
	/**
	 *  Used by IdentityBranch constructor in MyIdentityModel
	 */
	public final static String sql_identity_fertility_nodes =
		"select i."+net.ddp2p.common.table.identity.identity_ID+
		  ", i."+net.ddp2p.common.table.identity.profile_name+
		  ", COUNT(*) AS count " +
		  ", i."+net.ddp2p.common.table.identity.default_id +
		  ", k."+ net.ddp2p.common.table.key.secret_key+
		  ", i."+ net.ddp2p.common.table.identity.secret_credential+
		  ", k."+ net.ddp2p.common.table.key.public_key+
		" from "+net.ddp2p.common.table.identity.TNAME+" as i " +
		" left join "+net.ddp2p.common.table.identity_value.TNAME+" as v ON i."+net.ddp2p.common.table.identity.identity_ID+"=v."+net.ddp2p.common.table.identity_value.identity_ID +
		" LEFT JOIN "+net.ddp2p.common.table.key.TNAME+" AS k ON k."+net.ddp2p.common.table.key.ID_hash+"=i."+net.ddp2p.common.table.identity.secret_credential+
		" GROUP BY i."+net.ddp2p.common.table.identity.identity_ID+";";
	public final static int IFN_IDENTITY = 0;
	public final static int IFN_PROFILE_NAME = 1;
	public final static int IFN_COUNT = 2;
	public final static int IFN_DEFAULT = 3;
	public final static int IFN_SK = 4;
	public final static int IFN_S_CRED = 5;
	public final static int IFN_PK = 6;
	public static String sql_oids="select "+net.ddp2p.common.table.oid.oid_ID+", "+net.ddp2p.common.table.oid.OID_name+", "+net.ddp2p.common.table.oid.explanation+" from "+net.ddp2p.common.table.oid.TNAME+";";
}
