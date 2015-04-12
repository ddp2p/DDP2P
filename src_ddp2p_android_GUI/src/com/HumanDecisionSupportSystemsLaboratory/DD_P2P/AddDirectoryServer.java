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

import net.ddp2p.common.hds.Address;
import net.ddp2p.common.util.DD_DirectoryServer;
import net.ddp2p.common.util.DirectoryAddress;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.data.D_Peer;


public class AddDirectoryServer extends ActionBarActivity {
	
	protected static final boolean DEBUG = false;
	private String name;
	private Button but;
	private EditText add_body_version;
	private EditText add_body_ip;
	private EditText add_body_port;
	private EditText add_body_name;
	private EditText add_body_branch;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.add_directory_server);	
			
		Application_GUI.dbmail = new Android_DB_Email(this);
		Application_GUI.gui = new Android_GUI();
    	
        but = (Button) findViewById(R.id.submit_add_directory_server);
		add_body_branch = (EditText) findViewById(R.id.add_directory_server_body_branch);
		add_body_version = (EditText) findViewById(R.id.add_directory_server_body_version);
		add_body_ip = (EditText) findViewById(R.id.add_directory_server_body_ip);
		add_body_port = (EditText) findViewById(R.id.add_directory_server_body_port);
		add_body_name = (EditText) findViewById(R.id.add_directory_server_body_name);
		
    	but.setOnClickListener(new View.OnClickListener() {
  
    		boolean emptyS(String val) {
    			if (val == null) return true;
    			if ("".equals(val.trim())) return true;
    			return false;
    		}
			public void onClick(View v) {
            
				//String dirdd = "DIR%B%0.9.56://163.118.78.40:10000:10000:DD";
				DD_DirectoryServer ds = new DD_DirectoryServer();
				DirectoryAddress dirAddr = new DirectoryAddress();
				
				String branch = add_body_branch.getText().toString();
				String agentVersion = add_body_version.getText().toString();
				String ip = add_body_ip.getText().toString();
				String port = add_body_port.getText().toString();
				String name = add_body_name.getText().toString();
				
				if (emptyS(branch) && emptyS(agentVersion) && emptyS(port) && emptyS(ip) && emptyS(name)) {
					branch = DD.BRANCH;
					agentVersion = DD.VERSION;
					port = net.ddp2p.common.hds.DirectoryServer.PORT+"";
					ip = "163.118.78.40";
					name = "DDP2P_HDSSL";
				}
				
				int _port = 0;
				try {
					_port = Integer.parseInt(port);
					if (_port <= 0 || _port >= (1<<16) ) throw new Exception("val");
				} catch (Exception e) {
					Toast.makeText(getApplicationContext(), "Illegal port integer: "+port, Toast.LENGTH_LONG).show();
					return;
				}
				if (name != null)
					name = name.replace(" ", "_");
				
				dirAddr.pure_protocol = Address.DIR;
				dirAddr.setBranch(branch);
				dirAddr.setAgentVersion(agentVersion);
				dirAddr.setBothPorts(port);
				dirAddr.setActive(true);
				dirAddr.setDomain(ip);
				dirAddr.setName(name);
				//ds.parseAddress(dirdd);
				ds.add(dirAddr);
				if (DEBUG) Log.d("DIR", "Will Save dir: "+dirAddr);
				if (DEBUG) {
					DirectoryAddress.DEBUG = true;
                    net.ddp2p.common.util.DirectoriesSaverThread.DEBUG = true;
                    net.ddp2p.common.util.DD_DirectoryServer.DEBUG = true;
				}
				ds.save(); // can do
				//ds.sync_save();
				if (DEBUG) Log.d("DIR", "Saved dir");
				//dir0 = new Address(dirdd);

                net.ddp2p.common.hds.Address dir0 = new Address(dirAddr);
				dir0.pure_protocol = net.ddp2p.common.hds.Address.DIR;
				dir0.branch = DD.BRANCH;
				dir0.agent_version = DD.VERSION;
				dir0.certified = true;
				dir0.version_structure = net.ddp2p.common.hds.Address.V3;
				dir0.setPorts(_port, _port);
				dir0.address = dir0.domain+":"+dir0.tcp_port;
				System.out.println("Adding address: "+dir0);

				D_Peer myself = net.ddp2p.common.data.HandlingMyself_Peer.get_myself_or_null();
				if (myself != null) {
					if (DEBUG) Log.d("DIR", "A myself addr");
                    Toast.makeText(AddDirectoryServer.this, "Added to myself", Toast.LENGTH_SHORT).show();
					myself.addAddress(dir0, true, null);
				}

				if (DEBUG) Log.d("DIR", "Saved addr: "+dir0);

                Intent intent = new Intent();
                AddDirectoryServer.this.setResult(RESULT_OK, intent);
				
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
}
