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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class NewsDetail extends Activity{

	private TextView titleTextView; 
	private TextView contentTextView;
	private String title;
	private String content;
	private String[] review;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.news_detail);
		
		review = new String[] { "Right!", "Good!"};
		
		titleTextView = (TextView) findViewById(R.id.news_detail_title);
		contentTextView = (TextView) findViewById(R.id.news_detail_content);
		ListView listView = (ListView) findViewById(R.id.news_detail_listview);
		
		Intent intent = this.getIntent();
		Bundle b = intent.getExtras();
		
		title = b.getString("motion_title");
		Log.d("motion_title", title);
		titleTextView.setText(title);
		
		contentTextView.setText("something...");
		
		listView.setAdapter( new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, review));
		
	}

	
}
