package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Vote;


public class AddJustification extends ActionBarActivity {
	
	private String name;
	private String body;

	String mLID = null;
	String jLID = null;
	String choice = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_justification);	
			
		if (Application_GUI.dbmail == null)
			Application_GUI.dbmail = new Android_DB_Email(this);
		if (Application_GUI.gui == null)
			Application_GUI.gui = new Android_GUI();
		
    	final Button but = (Button) findViewById(R.id.submit_add_justification);
    	final EditText add_name = (EditText) findViewById(R.id.add_justification_title);
    	final EditText add_body = (EditText) findViewById(R.id.add_justification_body);
 
		Intent intent = this.getIntent();
		Bundle b = intent.getExtras();

		mLID = b.getString(Motion.M_MOTION_LID);
		Log.d("VOTE", "mLID="+mLID);
		choice = b.getString(Motion.M_MOTION_CHOICE);
		Log.d("VOTE", "choice="+choice);
		jLID = b.getString(Motion.J_JUSTIFICATION_LID);
		if ("".equals(jLID)) jLID = null;
		Log.d("VOTE", "jLID="+jLID);

    	but.setOnClickListener(new View.OnClickListener() {
  

			public void onClick(View v) {
            	
	           	name = add_name.getText().toString();
	           	body = add_body.getText().toString();
            	newJustification(name, body);

                NavUtils.navigateUpFromSameTask(AddJustification.this);
                 
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
	private void newJustification(String _name, String _body) {
		Log.d("onCreateJustCreatingThread", "AddJustification: newJust: run: start tit="+_name+" b="+_body);
		
		/*
		 * ciphersuite did not initialize, add a new ciphersuit class
		 */
		
		new JustificationsCreatingThread(_name, _body, jLID, mLID, choice).start();
		Log.d("onCreateJustCreatingThread", "AddJustification: newJust: run: done");
		
	}
	class JustificationsCreatingThread extends Thread {
		String name;
		String body;

		String choice;
		String jlid;
		String m_LID;
		
		public
		JustificationsCreatingThread(String _name, String _body, String jlid, String mlid, String choice) {
			name = _name;
			body = _body;
			this.m_LID = mlid;
			this.jlid = jlid;
			this.choice =  choice;
		}
		public void run() {
			D_Motion crt_motion = D_Motion.getMotiByLID(m_LID, true, false);
			if (crt_motion == null) return;
			D_Justification _answered = null;
			if (jlid != null) _answered = D_Justification.getJustByLID(jlid, true, false);

			D_Justification new_justification = D_Justification.createJustification(crt_motion, name, body, _answered);
			Log.d("VOTE", "Just = "+new_justification);
			Log.d("VOTE", "Vote for ch= "+choice+" m="+crt_motion);
			D_Vote new_vote = D_Vote.
					createVote(crt_motion, new_justification, choice);
			Log.d("VOTE", "Vote = "+new_vote);
			
			Message msgObj = handler.obtainMessage();
			handler.sendMessage(msgObj);
		}
		/*
		public  D_Vote createVote(D_Motion _motion, D_Justification _justification, String _choice) {
			D_Constituent _constituent = Identity.getCrtConstituent(_motion.getOrganizationLID());
			Log.d("VOTE", "Vote = got constituent="+_constituent);
			if (_constituent != null) {
				Log.d("VOTE", "Vote = got constituent real="+_constituent.realized());
				Log.d("VOTE", "Vote = got constituent real="+_constituent.getSK());
			}
			return createVote( _motion, _justification, _constituent, _choice);
		}
		public  D_Vote createVote(D_Motion _motion, D_Justification _justification, D_Constituent _constituent, String _choice) {
			if (_motion == null || !_motion.realized()) {
				Log.d("VOTE", "Vote = bad motion");
				return null;
			}
			if (_constituent == null || !_constituent.realized() || _constituent.getSK() == null) {
				Log.d("VOTE", "Vote = bad constituent");
				return null;
			}
			if (_choice == null) {
				Log.d("VOTE", "Vote = bad choice");
				return null;
			}
			Calendar now = Util.CalendargetInstance();
			D_Vote v = new D_Vote();
			v.setMotionAndOrganizationAll(_motion);
			v.setConstituentAll(_constituent);
			if (_justification != null) v.setJustificationAll(_justification);
			v.setChoice(_choice);
			v.setCreationDate(now);
			if (!v.sign()) {
				Log.d("VOTE", "Vote = bad signature");
				return null;
			}
			v.setArrivalDate(now);
			try {
				v.storeVerified();
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			return v;
		}
		*/
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
                 
                //String aResponse = msg.getData().getString("message");
    	        Toast.makeText(AddJustification.this, "add a new justification successfully!", Toast.LENGTH_LONG).show();
                
 				Intent intent = MotionDetail.obj.getIntent();
 				MotionDetail.obj.finish();
 				startActivity(intent);
//    			if (Motion.activ != null) {
//    				@SuppressWarnings("unchecked")
//					ArrayAdapter<MotionItem> adapt = ((ArrayAdapter<MotionItem>) Motion.activ.getListAdapter());
//    				if (adapt != null) adapt.notifyDataSetChanged();
//    			}
//
//    			Motion.listAdapter = new ArrayAdapter<MotionItem>(Motion.activ, android.R.layout.simple_list_item_1, Motion.motionTitle);
//    			Motion.activ.setListAdapter(Orgs.listAdapter);
    			
       }
	 };
}
