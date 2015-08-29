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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;

public
class Android_GUI implements net.ddp2p.common.config.Vendor_GUI_Dialogs {
	Context context;
	Android_GUI (Context _context) {
		context = _context;
	}

	@Override
	public void ThreadsAccounting_ping(String arg0) {
		// TODO Auto-generated method stub
		Log.d("Android_GUI", "Android_GUI: ping: "+Thread.currentThread().getName()+"::"+arg0);
		Toast_makeText(context, Thread.currentThread().getName()+"::"+arg0, Toast.LENGTH_LONG);//.show();
	}

	@Override
	public int ask(String arg0, String arg1, int arg2) {
		// TODO Auto-generated method stub
		Log.d("Android_GUI", "Android_GUI: ??? ask "+arg0+" "+arg1);
		Toast_makeText(context, arg0 + " " + arg1, Toast.LENGTH_LONG);//.show();
		return 0;
	}

	@Override
	public int ask(String arg0, String arg1, Object[] arg2, Object arg3,
			Object arg4) {
		// TODO Auto-generated method stub
		Log.d("Android_GUI", "Android_GUI: ??? ask "+arg0+" "+arg1+" "+arg2);
		Toast_makeText(context, arg0 + " " + arg1, Toast.LENGTH_LONG);//.show();
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

		Log.d("Android_GUI", "Android_GUI: info "+arg0+" "+arg1);
		Toast_makeText(context, arg0 + " " + arg1, Toast.LENGTH_LONG);//.show();
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
		//context;
		Log.d("Android_GUI", "Android_GUI: warning "+arg0+" "+arg1);
		Toast_makeText(context, arg0 + " " + arg1, Toast.LENGTH_LONG);//.show();
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

	class ToastCtx {
		Context c; String t; int len;
		ToastCtx(Context c, String t, int len) {
			this.c = c; this.t = t; this.len = len;
		}
	}
	public void Toast_makeText(Context c, String t, int len) {
		if (! (c instanceof Activity)) {
			Log.d("Toast", "Android_GUI: makeText: Not an activity: "+t);

			Intent intent = new Intent(Main.BROADCAST_MAIN_RECEIVER);
			intent.putExtra(Main.BROADCAST_PARAM_TOAST, t);
			LocalBroadcastManager.getInstance(c).sendBroadcast(intent);

			return;
		}
		Activity a = (Activity) c;
		a.runOnUiThread(new net.ddp2p.common.util.DDP2P_ServiceRunnable("Toaster",false, false, new ToastCtx(c,t,len))  {
			@Override
			public void _run() {
				Toast.makeText(((ToastCtx)ctx).c, ((ToastCtx)ctx).t, ((ToastCtx)ctx).len).show();
			}
		});
	}
}