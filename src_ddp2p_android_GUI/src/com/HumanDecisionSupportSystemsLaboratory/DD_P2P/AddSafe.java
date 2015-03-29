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

import java.util.ArrayList;
import java.util.HashMap;

import com.HumanDecisionSupportSystemsLaboratory.DD_P2P.Safe.SafeAdapter;

import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.ciphersuits.Cipher;
import net.ddp2p.ciphersuits.CipherSuit;
import net.ddp2p.ciphersuits.ECDSA;
import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.HandlingMyself_Peer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

public class AddSafe extends ActionBarActivity {

	private String name, email, slogan;
	private String quality;
	final static String FAST = "FAST";
	protected static final String TAG = "addSafe";

	private int keysize;
	private String hash;
	private String cipher;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_safe);

		Application_GUI.dbmail = new Android_DB_Email(this);
		Application_GUI.gui = new Android_GUI();

		final Button but = (Button) findViewById(R.id.submit);
		final EditText add_name = (EditText) findViewById(R.id.add_name);
		final EditText add_email = (EditText) findViewById(R.id.add_email);
		final EditText add_slogan = (EditText) findViewById(R.id.add_slogan);

		but.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				name = add_name.getText().toString();
				email = add_email.getText().toString();
				slogan = add_slogan.getText().toString();
				quality = FAST; // You will change this based on input

				if (FAST.equals(quality)) {
					cipher = Cipher.RSA;
					keysize = 150;
					hash = Cipher.MD5;
				} else {
					cipher = Cipher.ECDSA;
					keysize = ECDSA.P_256;
					hash = Cipher.SHA1;
				}

				newPeer(name, email, slogan, cipher, keysize, hash);
/*				Safe.loadPeer();
				Safe.safeAdapter.notifyDataSetChanged();*/
				finish();
			}
		});

	}

	// return button on left-top corner
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			finish();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	// function for add a new peer
	private void newPeer(String name, String email, String slogan,
			String _cipher, int _keysize, String _hash) {
		Log.d("onCreatePeerCreatingThread", "AddSafe: newPeer: run: start");
		PeerInput pi = new PeerInput();
		pi.name = name;
		pi.email = email;
		pi.slogan = slogan;

		/*
		 * ciphersuite did not initialize, add a new ciphersuit class
		 */

		CipherSuit cs = new CipherSuit();
		cs.cipher = _cipher; // Cipher.RSA;
		cs.hash_alg = _hash;// Cipher.SHA256;
		cs.ciphersize = _keysize;// 2048;
		Log.d("onCreatePeerCreatingThread", "AddSafe: newPeer:  will start");

		pi.cipherSuite = cs;
		new PeerCreatingThread(pi).start();
		Log.d("onCreatePeerCreatingThread", "AddSafe: newPeer: run: done");

	}

	class PeerCreatingThread extends Thread {
		PeerInput pi;

		PeerCreatingThread(PeerInput _pi) {
			Log.d(TAG, "add safe: peerCreatingThread constructor");
			pi = _pi;
		}

		public void run() {

			Log.d("onCreatePeerCreatingThread",
					"PeerCreatingThread: run: start");
			gen();
			
			Log.d("onCreatePeerCreatingThread",
					"PeerCreatingThread: run: announced");
			Log.d(TAG, "add safe: run()");
			
			Safe.loadPeer();
			
			Log.d("onCreatePeerCreatingThread",
					"PeerCreatingThread: run: generated");
			threadMsg("a");

		}

		public D_Peer gen() {
			Log.d(TAG, "add safe: gen()");
			D_Peer peer = HandlingMyself_Peer.createMyselfPeer_w_Addresses(pi,
					true);
			D_Peer myself = HandlingMyself_Peer.get_myself_or_null();
			myself = D_Peer.getPeerByPeer_Keep(myself);
			if (myself == null) {
				Toast.makeText(getApplicationContext(),
						"Could not set slogan/email", Toast.LENGTH_SHORT)
						.show();
				return peer;
			}
			Log.d("onCreatePeerCreatingThread",
					"PeerCreatingThread: gen: start");
			myself.setEmail(email);
			myself.setSlogan(slogan);
			myself.storeRequest();

			Log.d("onCreatePeerCreatingThread",
					"PeerCreatingThread: gen: inited");
			// data.HandlingMyself_Peer.setMyself(myself, true, false);
			if (!Main.serversStarted)
				Main.startServers();
			return peer;
		}

		private void threadMsg(String msg) {
			if (!msg.equals(null) && !msg.equals("")) {
				Log.d(TAG, "add safe: threadMsg");
				Message msgObj = handler.obtainMessage();
				Bundle b = new Bundle();
				b.putString("message", msg);
				msgObj.setData(b);
				handler.sendMessage(msgObj);
				Log.d(TAG, "add safe: threadMsg finished");
			}
		}
	}

	private final Handler handler = new Handler() {

		// Create handleMessage function

		public void handleMessage(Message msg) {

			String aResponse = msg.getData().getString("message");
			Log.d(TAG, "add safe: handler");
			Toast.makeText(AddSafe.this, "add a new safe successfully!",
					Toast.LENGTH_LONG).show();
			if (Safe.safeItself != null) {
				SafeAdapter adapt = (SafeAdapter) Safe.safeItself.getListAdapter();
				if (adapt != null)
					adapt.notifyDataSetChanged();
			}
			

			Log.d(TAG, "add safe: handler: set adapter");
			Log.d(TAG, "Safe: handler: list size:" + Safe.list.size());
			ArrayList<HashMap<String, String>> _list = new ArrayList<HashMap<String, String>>();
			
			Log.d(TAG, "safe: list hash other list: " + _list.hashCode() + " size: " + _list.size());
			Log.d(TAG, "safe: list hash before: " + Safe.list.hashCode() + " size: " + Safe.list.size());
			Safe.list.removeAll(Safe.list);
			Log.d(TAG, "safe: list hash removed: " + Safe.list.hashCode() + " size: " + Safe.list.size());
			Safe.list.addAll(_list);
			Log.d(TAG, "safe: list hash after: " + Safe.list.hashCode() + " size: " + Safe.list.size());
			ArrayList<Bitmap> _imgData = Safe.imgData;
			Safe.imgData.removeAll(Safe.imgData);
			Safe.imgData.addAll(_imgData);

			Safe.safeAdapter.notifyDataSetChanged();
			
/*			Safe.safeAdapter = new SafeAdapter(Safe.safeItself.getActivity(),
					Safe.list, Safe.imgData);
			Safe.safeItself.setListAdapter(Safe.safeAdapter);*/
		}
	};
}
