/* Copyright (C) 2014,2015 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
Florida Tech, Human Decision Support Systems Laboratory
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation; either the current version of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
You should have received a copy of the GNU Affero General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA. */
/* ------------------------------------------------------------------------- */

package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;

public
class Android_GUI implements net.ddp2p.common.config.Vendor_GUI_Dialogs {

	@Override
	public void ThreadsAccounting_ping(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int ask(String arg0, String arg1, int arg2) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ask(String arg0, String arg1, Object[] arg2, Object arg3,
			Object arg4) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clientUpdates_Start() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clientUpdates_Stop() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public D_Peer createPeer(PeerInput arg0, PeerInput[] arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void eventQueue_invokeLater(Runnable arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fixScriptsBaseDir(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public D_Constituent getMeConstituent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] getWitnessScores() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String html2text(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void info(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String input(String arg0, String arg1, int arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean is_crt_peer(D_Peer arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void peer_contacts_update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String queryDatabaseFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void registerThread() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBroadcastClientStatus_GUI(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBroadcastServerStatus_GUI(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClientUpdatesStatus(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setMePeer(D_Peer arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSimulatorStatus_GUI(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void unregisterThread() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateProgress(Object arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update_broadcast_client_sockets(Long arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warning(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean is_crt_org(D_Organization arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_crt_const(D_Constituent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void inform_arrival(Object arg0, D_Peer arg1) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public boolean playThanks() {
        return false;
    }

}