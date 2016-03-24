/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2015 Marius C. Silaghi
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
package net.ddp2p.common.population;

public interface ConstituentsInterfaceInput {
	public void updateCensus(Object source, Object[] path);
	public void updateCensusStructure(Object source, Object[] path);

	public ConstituentsAddressNode getRoot();

	public void updateCensus(Object source,
			Object[] path2parent, int idx);

	public long getConstituentIDMyself();
	public String getSubDivisions();
	public long getOrganizationID();
	public long[] getFieldIDs();
	public void updateCensusInserted(
			Object source, Object[] path2parent,
			int[] idx, Object[] children);
}