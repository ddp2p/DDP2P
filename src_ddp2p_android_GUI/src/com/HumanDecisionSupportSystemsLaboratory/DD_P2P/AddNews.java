package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

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
import net.ddp2p.common.data.D_OrgParams;
import net.ddp2p.common.data.D_Organization;


public class AddNews extends ActionBarActivity {
	
	private String name;
	private Button but;
	private EditText add_name;
	private EditText add_body;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_news);	
			
		Application_GUI.dbmail = new Android_DB_Email(this);
		Application_GUI.gui = new Android_GUI();
    	
        but = (Button) findViewById(R.id.submit_add_news);
		add_name = (EditText) findViewById(R.id.add_news_name);
		add_body = (EditText) findViewById(R.id.add_news_body);
		
    	but.setOnClickListener(new View.OnClickListener() {
  

			public void onClick(View v) {
            	
            	name = add_name.getText().toString();
                
            	newOrg(name);
            	
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
	private void newOrg(String _name) {
		Log.d("onCreatePeerCreatingThread", "AddSafe: newPeer: run: start");
		
		/*
		 * ciphersuite did not initialize, add a new ciphersuit class
		 */
		
		new OrgCreatingThread(name).start();
		Log.d("onCreatePeerCreatingThread", "AddSafe: newPeer: run: done");
		
	}
	class OrgCreatingThread extends Thread {
		String name;
		OrgCreatingThread(String _name) {
			name = _name;
		}
		public void run() {
			D_Organization new_org = D_Organization.getEmpty();
			new_org.setName(name);
			//new_org.concepts.name_motion=new String[]{name};
			if (new_org.params == null)
				new_org.params = new D_OrgParams();
			new_org.params.certifMethods = net.ddp2p.common.table.organization._GRASSROOT;
			new_org.setTemporary(false);
			new_org.setCreationDate();
			D_Organization.DEBUG = true;
			String GID = new_org.global_organization_IDhash = new_org.global_organization_ID = new_org.getOrgGIDandHashForGrassRoot(); // sign();
			
			//D_Organization org = D_Organization.getOrgByGID_or_GIDhash(GID, GID, true, true, true, null);
			D_Organization.storeRemote(new_org, null);
			Orgs.reloadOrgs();
			
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
	 private final Handler handler = new Handler() {
		 
         // Create handleMessage function

        public void handleMessage(Message msg) {
                 
                String aResponse = msg.getData().getString("message");
    	        Toast.makeText(AddNews.this, "Added a new news successfully!", Toast.LENGTH_LONG).show();
    			if (Orgs.activ != null) {
    				@SuppressWarnings("unchecked")
					ArrayAdapter<String> adapt = ((ArrayAdapter<String>) Orgs.activ.getListAdapter());
    				if (adapt != null) adapt.notifyDataSetChanged();
    			}

    			Orgs.listAdapter = new ArrayAdapter<OrgItem>(Orgs.activ.getActivity(), android.R.layout.simple_list_item_1, Orgs.orgName);
    			Orgs.activ.setListAdapter(Orgs.listAdapter);
    			
       }
	 };
}
