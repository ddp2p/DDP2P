/*   Copyright (C) 2014 Authors: Hang Dong <hdong2012@my.fit.edu>, Marius Silaghi <silaghi@fit.edu>
		Florida Tech, Human Decision Support Systems Laboratory
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */
package com.HumanDecisionSupportSystemsLaboratory.DDP2P;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class News extends ListActivity {

	private String[] newsTitle;
	private ActionBar actionbar = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
         
		actionbar = this.getActionBar();
		actionbar.setHomeButtonEnabled(true);

		newsTitle = new String[] { "News1", "News2", "News3"

		};

		ListAdapter listAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, newsTitle);
		setListAdapter(listAdapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.news_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		
		case R.id.add_new_news:
			Intent i = new Intent();
			i.setClass(this, AddNews.class);
			startActivity(i);
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {

		Toast.makeText(this,
				getListView().getItemAtPosition(position).toString(),
				Toast.LENGTH_SHORT).show();

		
		Intent intent = new Intent(); 
		intent.setClass(this, NewsDetail.class); 
	    Bundle b = new Bundle(); 
	    
	    //should pass the news name to newsdetail
/*	    b.putString("org_name",mo[position]); 
	    intent.putExtras(b); */
	    startActivity(intent);
		 
	}

}
