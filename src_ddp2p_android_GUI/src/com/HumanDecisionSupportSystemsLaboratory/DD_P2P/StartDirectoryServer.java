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

import net.ddp2p.common.util.DD_Address;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;

public class StartDirectoryServer extends Activity {

	private EditText edIp; 
	private Switch swtStart;
	private ListView serversList;
	private String myServerIP;
	private static ArrayAdapter<String> adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.start_directory_server);
		
		edIp = (EditText) findViewById(R.id.start_directory_server_edtxt_ip);
		swtStart = (Switch) findViewById(R.id.start_directory_server_btn_start);

		serversList  = (ListView) findViewById(R.id.start_directory_server_listview);
		
		//TODO
		String[] safes = {"peer 1 : 192.168.0.1", "peer 2 : 112.128.52.13"};
		
		
		//TODO load from database
		myServerIP = "a ip address";
		edIp.setText(myServerIP);
		
	    //TODO set up this switch
		swtStart.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				
			}
		});
			

		
		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, safes);
		serversList.setAdapter(adapter);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
        adapter.notifyDataSetChanged();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.org_export) {
			String testText = "something about directory";
			String testSubject = "subject";
/*			if (organization_gidh == null) {
				Toast.makeText(this, "No peer. Reload!", Toast.LENGTH_SHORT).show();
				return true;
			}*/
			DD_Address adr = new DD_Address();
			
			Intent i = new Intent(Intent.ACTION_SEND);
			i.setType("text/plain");
			i.putExtra(Intent.EXTRA_TEXT, testText);
			i.putExtra(Intent.EXTRA_SUBJECT, "DDP2P: directory Address of \""+ testSubject);
			i = Intent.createChooser(i, "Send my directory server Public key");
			startActivity(i);
		}
		return super.onOptionsItemSelected(item);
	}
}
