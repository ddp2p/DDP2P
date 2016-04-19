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
package net.ddp2p.common.hds;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.util.Util;
public class SR{
	public static final String HA_GID = "G";
	public static final String HA_NAME = "N";
	public static final String HA_SLOGAN = "S";
	public static final String HA_DATE = "D";
	public static final String HA_ADDRESS = "A";
	public static final String HA_BROAD = "B";
	public static final String HA_HA = "H";
	public static final String tableNames[] = {net.ddp2p.common.table.peer.G_TNAME, net.ddp2p.common.table.news.G_TNAME};
	public static final String[] HASH_ALG_V1 = {HA_GID, HA_NAME, HA_SLOGAN, HA_DATE, HA_ADDRESS, HA_BROAD, HA_HA};
	public static final String hash_alg_str = Util.concat(SR.HASH_ALG_V1,DD.APP_ID_HASH_SEP);
}
