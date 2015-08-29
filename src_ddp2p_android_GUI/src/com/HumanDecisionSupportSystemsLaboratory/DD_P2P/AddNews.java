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

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_OrgParams;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.Util;


public class AddNews extends ActionBarActivity {

    private String name;
    private String body;
	private Button but;
	private EditText add_name;
	private EditText add_body;

    public String organization_LID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


        Intent i = this.getIntent();
        Bundle b = i.getExtras();

        // top panel setting
        organization_LID = b.getString(Orgs.O_LID);
        net.ddp2p.common.data.D_Constituent me = DD.getCrtConstituent(Util.lval(organization_LID, -1));
        if (me == null) {
            Toast.makeText(AddNews.this, "Create a Profile First", Toast.LENGTH_SHORT).show();
            //this.finalize();
            return;
        }

		setContentView(R.layout.add_news);
			
		Application_GUI.dbmail = new Android_DB_Email(this);
		if (Application_GUI.gui == null)
			Application_GUI.gui = new Android_GUI(this);
    	
        but = (Button) findViewById(R.id.submit_add_news);
		add_name = (EditText) findViewById(R.id.add_news_name);
        add_body = (EditText) findViewById(R.id.add_news_body);

    	but.setOnClickListener(new View.OnClickListener() {
  

			public void onClick(View v) {

                name = add_name.getText().toString();
                body = add_body.getText().toString();

            	newNews(name, body);
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
	private void newNews(String _name, String _body) {
		Log.d("onCreateNewsCT", "AddNews: newNews: run: start");
		
		/*
		 * ciphersuite did not initialize, add a new ciphersuit class
		 */
		
		new NewsCreatingThread(name, body).start();
		Log.d("onCreateNewsCT", "AddNews: newNews: run: done");
		
	}
	class NewsCreatingThread extends Thread {
        String name;
        String body;
		NewsCreatingThread(String _name, String _body) {
			name = _name; body = _body;
		}
		public void run() {
			net.ddp2p.common.data.D_News new_obj = D_News.getEmpty();
            new_obj.setTitle(name);
            new_obj.setBody(body);
            new_obj.setOrganizationLID(organization_LID);
            net.ddp2p.common.data.D_Constituent me = DD.getCrtConstituent(Util.lval(organization_LID, -1));
            if (me == null) {
                Toast.makeText(AddNews.this, "Create a Profile First", Toast.LENGTH_SHORT).show();
                return;
            }
            new_obj.setConstituent(me);
            new_obj.setCreationDate();
            Log.d("AddNews", "Date="+new_obj);
            new_obj.setGID(new_obj.make_ID());
            new_obj.sign();
            new_obj.setArrivalDate();

            try {
                new_obj.storeVerified();
            } catch (Exception e){}
            Log.d("AddNews", "Added="+new_obj);
			// Message msgObj = handler.obtainMessage();
			// handler.sendMessage(msgObj);
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
    /*
	 private final Handler handler = new Handler() {
		 
         // Create handleMessage function

        public void handleMessage(Message msg) {
                 
                String aResponse = msg.getData().getString("message");
    	        Toast.makeText(AddNews.this, "Added a new news successfully!", Toast.LENGTH_LONG).show();
    			if (News.activ != null) {
    				@SuppressWarnings("unchecked")
					ArrayAdapter<String> adapt = ((ArrayAdapter<String>) Orgs.activ.getListAdapter());
    				if (adapt != null) adapt.notifyDataSetChanged();
    			}

            News.listAdapter = new ArrayAdapter<OrgItem>(News.activ.getActivity(), android.R.layout.simple_list_item_1, News.orgName);
            News.activ.setListAdapter(News.listAdapter);
    			
       }
	 };
    */
}
