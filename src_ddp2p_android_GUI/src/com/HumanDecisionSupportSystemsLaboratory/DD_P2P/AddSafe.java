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

import android.content.Intent;
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
    final public static String PI = "PI";
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

				PeerInput pi = newPeer(name, email, slogan, cipher, keysize, hash);
/*				Safe.loadPeer();
				Safe.safeAdapter.notifyDataSetChanged();*/
                Intent intent = new Intent();
                Safe.peerInput = pi; // no longer needed
                intent.putExtra(AddSafe.PI, pi.encode());
                AddSafe.this.setResult(RESULT_OK, intent);
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
	private PeerInput newPeer(String name, String email, String slogan,
			String _cipher, int _keysize, String _hash) {
		Log.d("onCreatePeerCreatingTh", "AddSafe: newPeer: run: start");
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
		Log.d("onCreatePeerCreatingTh", "AddSafe: newPeer:  will start");

		pi.cipherSuite = cs;
        //new PeerCreatingThread(pi).start();
		Log.d("onCreatePeerCreatingTh", "AddSafe: newPeer: run: done");
        return pi;
	}
}
