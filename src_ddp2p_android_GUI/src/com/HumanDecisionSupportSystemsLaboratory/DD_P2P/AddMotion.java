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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_Document;
import net.ddp2p.common.data.D_Document_Title;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Organization;


public class AddMotion extends ActionBarActivity {
	
	private String name;
	private String body;
	private String enhancedLID;
	private D_Motion enhanced;
    TextView motionOrgName;
    WebView motionInstructions;
    D_Organization org;
    String orgLID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_motion);	
			
		Application_GUI.dbmail = new Android_DB_Email(this);
		Application_GUI.gui = new Android_GUI();

		
		Intent i = this.getIntent();
		Bundle b = i.getExtras();	
		if (b != null) {
            enhancedLID = b.getString(Motion.M_MOTION_LID);
            if (enhancedLID != null)
                enhanced = D_Motion.getMotiByLID(enhancedLID, true, false);

            orgLID = b.getString(Orgs.O_LID);
            if (orgLID != null)
                org = D_Organization.getOrgByLID(orgLID, true, false);
        }

        this.motionInstructions = (WebView) findViewById(R.id.motion_instructions);
        this.motionOrgName = (TextView) findViewById(R.id.motion_orgname);
        if (org != null && org.getName() != null) {
            motionOrgName.setText(org.getName());
            motionOrgName.setVisibility(View.VISIBLE);
        }
        if (org != null && org.getInstructionsNewMotions() != null) {
            //profileInstructions.loadData(org.getInstructionsRegistration(), "text/html", null);
            motionInstructions.loadUrl("data:text/html;charset=UTF-8,"+org.getInstructionsNewMotions());
            motionInstructions.setVisibility(View.VISIBLE);
        }

        final Button but = (Button) findViewById(R.id.submit_add_motion);
    	final EditText add_name = (EditText) findViewById(R.id.add_motion_name);
    	final EditText add_body = (EditText) findViewById(R.id.add_motion_body);
    	if (enhanced != null) {
	    	final TextView _enhanced = (TextView) findViewById(R.id.motion_enhancing);
	    	Object obj = enhanced.getTitleOrMy();
	    	String e_title = null;
	    	if (obj instanceof D_Document)  e_title =  ((D_Document)obj).getDocumentUTFString();
	    	if (obj instanceof D_Document_Title)  e_title =  ((D_Document_Title)obj).title_document.getDocumentUTFString();
	    	// if (obj instanceof String)  e_title =  (String)obj;
	    	if (obj instanceof String)  e_title =  obj.toString();
	    	_enhanced.setText(e_title);
    	}
    	
    	but.setOnClickListener(new View.OnClickListener() {
  

			public void onClick(View v) {
            	
	           	name = add_name.getText().toString();
	           	body = add_body.getText().toString();
            	newMot(name, body, enhanced);
            	finish();
            	
            }
        });
        
	}
  
	
	//return button on left-top corner
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home)
        {
            finish();
            return true;
        }
		
        return super.onOptionsItemSelected(item);
	}

	//function for add a new peer
	private void newMot(String _name, String _body, D_Motion _enhanced) {
		Log.d("onCreateMotCT", "AddMotion: newMot: run: start tit="+_name+" b="+_body);
		
		/*
		 * ciphersuite did not initialize, add a new ciphersuit class
		 */
		
		new MotCreatingThread(_name, _body).start();
		Log.d("onCreateMotCT", "AddMotion: newMot: run: done");
		
	}
	class MotCreatingThread extends Thread {
		String name;
		String body;
		MotCreatingThread(String _name, String _body) {
			name = _name;
			body = _body;
		}
		public void run() {
			D_Motion new_motion = D_Motion.getEmpty();
			
			D_Document_Title title = new D_Document_Title();
			title.title_document = new D_Document();
			title.title_document.setDocumentString(name);
			title.title_document.setFormatString(D_Document.TXT_FORMAT);
			new_motion.setMotionTitle(title);
			
			if (body != null) {
				D_Document body_d = new D_Document();
				body_d.setDocumentString(body);
				body_d.setFormatString(D_Document.TXT_FORMAT);
				new_motion.setMotionText(body_d);
			}
			
			if (enhanced != null) {
				new_motion.setEnhancedMotion(enhanced);
				new_motion.setEnhancedMotionLIDstr(enhancedLID);
				new_motion.setEnhancedMotionGID(enhanced.getGID());
			}
			
			new_motion.setTemporary(false);
			new_motion.__setGID(new_motion.make_ID());
			//D_Motion.DEBUG = true;
			String GID = new_motion.getGID();
			
			D_Motion m = D_Motion.getMotiByGID(GID, true, true, true, null, Motion.getOrganizationLID(), new_motion);
			if (m != new_motion) {
				m.loadRemote(new_motion, null, null, null);
				new_motion = (m);
			}
			new_motion.setTemporary(false);
			
			new_motion.setBroadcasted(true); // if you sign it, you probably want to broadcast it...
			new_motion.setArrivalDate();
			long m_id = new_motion.storeRequest_getID();
			new_motion.storeRequest();
			new_motion.releaseReference();
			Motion.reloadMotions();
			
			Message msgObj = handler.obtainMessage();
			handler.sendMessage(msgObj);
		}
	}
	/*
	class PeerCreatingThread extends Thread {
		PeerInput pi;
		PeerCreatingThread(PeerInput _pi) {
			pi = _pi;
		}
		public void run() {
			Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: run: start");
			gen();
			Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: run: generated");
			threadMsg("");
			Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: run: announced");
		}
		public D_Peer gen() {
			
			D_Peer peer = HandlingMyself_Peer.createMyselfPeer_w_Addresses(pi, true);
			Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: gen: start");
			peer.setEmail(email);
			peer.setSlogan(slogan);
			peer.storeRequest();
		
			Log.d("onCreatePeerCreatingThread", "PeerCreatingThread: gen: inited");
			data.HandlingMyself_Peer.setMyself(peer, true, false);
			if (!Main.serversStarted)
				Main.startServers();
			return peer;
		}
		private void threadMsg(String msg) {
		     if (!msg.equals(null) && !msg.equals("")) {
		           Message msgObj = handler.obtainMessage();
		           Bundle b = new Bundle();
		           b.putString("message", msg);
		           msgObj.setData(b);
		           handler.sendMessage(msgObj);
		       }
		   }
	}
	*/

	 private final static Handler handler = new Handler() {
		 
         // Create handleMessage function

        public void handleMessage(Message msg) {
                 
                String aResponse = msg.getData().getString("message");
    			if (Motion.activ != null) {
                    Toast.makeText(Motion.activ, "Added a new motion successfully!", Toast.LENGTH_LONG).show();
                    //@SuppressWarnings("unchecked")
                    //ArrayAdapter<MotionItem> adapt = ((ArrayAdapter<MotionItem>) Motion.activ.getListAdapter());
                    //if (adapt != null) adapt.notifyDataSetChanged();


                    //Motion.listAdapter = new ArrayAdapter<MotionItem>(Motion.activ, android.R.layout.simple_list_item_1, Motion.motionTitle);
                    //Motion.activ.setListAdapter(Motion.listAdapter);

                    if (Build.VERSION.SDK_INT >= 11) {
                        Motion.activ.recreate();
                    } else {
                        Intent intent = Motion.activ.getIntent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        Motion.activ.finish();
                        Motion.activ.overridePendingTransition(0, 0);

                        Motion.activ.startActivity(intent);
                        Motion.activ.overridePendingTransition(0, 0);
                    }
                }
       }
	 };

}
