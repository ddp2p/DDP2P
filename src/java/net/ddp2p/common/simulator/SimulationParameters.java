/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Osamah Dhannoon
 		Author: Osamah Dhannoon: odhannoon2011@fit.edu
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

package net.ddp2p.common.simulator;
public class SimulationParameters{
	
    public static float
    intended_witnesses = 1000000,
    intended_constituents = 100000,
	intended_orgs = 100,
	intended_peers = 5000,
	intended_motions = 5000,
	intended_neighborhoods = 200,
	intended_votes = 10000000,
	intended_all=intended_constituents+intended_votes+intended_motions+intended_peers+intended_orgs+intended_witnesses;


    public static float adding_new_organization = intended_orgs/intended_all,
	adding_new_constituent =  intended_constituents/intended_all,
	adding_new_peer = intended_peers/intended_all,
	adding_new_motion = intended_motions/intended_all,
	adding_new_witness = intended_witnesses/intended_all,
	adding_new_neighbor = intended_neighborhoods/intended_all,
	adding_new_vote = 1-adding_new_motion-adding_new_constituent-adding_new_organization-adding_new_peer-adding_new_witness,
	adding_new_justification_in_vote = 1/10.f,
	using_old_justification_in_vote = 1-adding_new_justification_in_vote,
	no_justification_vote = 1/2.f,
	agreeing = 1/.2f;



	public static String getCurrentProbabilitiesListAsString(
			String generationProbabilities) {
		if(generationProbabilities==null) return "Something to replace :)";
		return generationProbabilities;
	}
}
