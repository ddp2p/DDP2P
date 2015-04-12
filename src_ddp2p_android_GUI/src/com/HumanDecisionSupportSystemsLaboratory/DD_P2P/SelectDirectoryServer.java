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

import java.util.ArrayList;

import net.ddp2p.common.util.DD_DirectoryServer;
import net.ddp2p.common.util.DirectoryAddress;
import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;

public class SelectDirectoryServer extends ListActivity {

	private static final boolean DEBUG = false;
	private static final String TAG = "SelectDirectoryServer";
	private String[] servers =  new String[0];
	private int checkedPos[];
	private ActionBar actionbar;
    final static int RESULT_ADD_DIR = 10;

	private ArrayAdapter<String> adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		actionbar = this.getActionBar();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setTitle("Directory Server");

		setContentView(R.layout.list_fragement);

        updateView();
	}

    void updateView() {
        // Getting object reference to listview of main.xml
        ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        try {
            DD.load_listing_directories();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this.getApplicationContext(),
                    "Error: " + e.getLocalizedMessage(), Toast.LENGTH_LONG)
                    .show();
            return;
        }
        ArrayList<Address> lda = Identity.getListing_directories_addr();
/*		servers = new String[Math.max(1, lda.size())];*/
        servers = new String[lda.size()];
        //Log.d(TAG, servers[0]);
        checkedPos = new int[servers.length];
        // servers = new String[Identity.listing_directories_addr.size()];
        for (int k = 0; k < lda.size(); k++) {
            Address adr = lda.get(k);
            if (adr.name != null)
                servers[k] = adr.name + "(" + adr.domain + " :" + adr.udp_port
                        + ")";
            else
                servers[k] = adr.domain + " :" + adr.udp_port;
        }
		/* if (lda.size() == 0) servers[0] = "Add a server"; */

        // Instantiating array adapter to populate the listView
        // The layout android.R.layout.simple_list_item_single_choice creates
        // radio button for each listview item
        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, servers);

        listView.setAdapter(adapter);
        // if (Identity.listing_directories_addr.size() == 0)
        listView.setEmptyView(findViewById(R.layout.select_directory_server_empty));
        for (int k = 0; k < lda.size(); k++) {
            checkedPos[k] = lda.get(k).active ? 1 : 0;
            if (DEBUG)
                Log.d("DIR", "SelectDirectoryServer: crt pos=" + k + " check="
                        + checkedPos[k]);
            if (checkedPos[k] > 0)
                listView.setItemChecked(k, true);
            else
                listView.setItemChecked(k, false);
        }
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		ListView listView = getListView();

		if (DEBUG)
			Log.d("DIR", "SelectDirectoryServer: old pos=" + position
					+ " check=" + checkedPos[position]);

		checkedPos[position] = 1 - checkedPos[position];

		if (DEBUG)
			Log.d("DIR", "SelectDirectoryServer: new pos=" + position
					+ " check=" + checkedPos[position]);

		if (checkedPos[position] > 0)
			listView.setItemChecked(position, true);
		else
			listView.setItemChecked(position, false);

		if (position >= Identity.getListing_directories_addr().size()) {
			Toast.makeText(getApplicationContext(), "First add some directory",
					Toast.LENGTH_SHORT).show();
			return;
		}
		Address addr = Identity.getListing_directories_addr().get(position);
		addr.setActive(checkedPos[position] > 0);

		DD_DirectoryServer ds = new DD_DirectoryServer();
		DirectoryAddress dirAddr = new DirectoryAddress(addr);
		dirAddr.setActive(checkedPos[position] > 0);
		ds.add(dirAddr);
		if (DEBUG) {
			DirectoryAddress.DEBUG = true;
            net.ddp2p.common.util.DirectoriesSaverThread.DEBUG = true;
            net.ddp2p.common.util.DD_DirectoryServer.DEBUG = true;
		}
		ds.save();
		if (DEBUG)
			Log.d("DIR", "SelectDirectoryServer: " + dirAddr.active + " "
					+ dirAddr);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.directory_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.add_directory_server:
			Intent i = new Intent();
			i.setClass(this, AddDirectoryServer.class);
			startActivityForResult(i, RESULT_ADD_DIR);
		}

		return super.onOptionsItemSelected(item);
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode == RESULT_OK && requestCode == this.RESULT_ADD_DIR) {

            Toast.makeText(this, "Result size="+servers.length, Toast.LENGTH_SHORT).show();

            updateView();
            ArrayAdapter<String> a = ((ArrayAdapter<String>)this.getListAdapter());
            if (a != null) a.notifyDataSetChanged();


            super.onActivityResult(requestCode, resultCode, resultData);
            finish();
            return;
        }
        Toast.makeText(this, "Result unknown", Toast.LENGTH_SHORT).show();
        super.onActivityResult(requestCode, resultCode, resultData);
    }
	@SuppressWarnings("unchecked")
	@Override
	protected void onResume() {
		super.onResume();
        //((ArrayAdapter<String>)getListAdapter()).notifyDataSetChanged();
	}

}
