package com.HumanDecisionSupportSystemsLaboratory.DD_P2P;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

		titleTextView = (TextView) findViewById(R.id.news_detail_title);
		contentTextView = (TextView) findViewById(R.id.news_detail_content);
		
		Intent intent = this.getIntent();
		Bundle b = intent.getExtras();
		
		titleTextView.setText("some title");
		
		contentTextView.setText("something...");
		
	}

	
}
