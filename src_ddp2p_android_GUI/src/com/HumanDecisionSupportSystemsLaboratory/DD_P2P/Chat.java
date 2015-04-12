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

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.AndroidChatReceiver;
import com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.ChatMessage;

import net.ddp2p.common.data.D_Peer;

public class Chat extends Activity {
    private static final int NOTIFICATION_MSG = 1;
    EditText chat_input;
	private String chatContent;
	ListView chatListView;
	public List<ChatEntity> chatEntityList=new ArrayList<ChatEntity>();
	//private int safe_position;
	private String safe_lid;
	private String safe_gidh;
	private String chatName;
	public static int[] avatar=new int[]{};
	private static Chat crtChat = null;
	
	//broadcast receiver action name, notification builder and notification manager
	private static final String CHAT_ACTION = "com.HumanDecisionSupportSystemsLaboratory.DDP2P.broadcast.CHAT_RECEIVER_ACTION";
	static NotificationManager notiManager;
	static Builder notificationB;
    static Context context;
	
/*	MyBroadcastReceiver br;*/
	private D_Peer peer;
	public D_Peer getPeer() {
		return peer;
	}
	public static Chat getCrtChat() {
		return crtChat;
	}
    public void setChatEntityList (List<ChatEntity> chatEntityList) {
        handler.sendMessage(new Message());
    }
    public static void setChatNewMessage (String peer_name, String peer_GID, ChatMessage cmsg) {
        if (context == null) return;
        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(CHAT_ACTION);

            context.sendBroadcast(broadCastIntent);

            //sendBroadcast(new Intent(CHAT_ACTION), Manifest.permission.VIBRATE);
        } catch(Exception e) {
            e.printStackTrace();
        }
        //handler.sendMessage(new Message());
    }
	public void _setChatEntityList (List<ChatEntity> chatEntityList) {
		this.chatEntityList = chatEntityList;
		chatListView = (ListView) findViewById(R.id.chat_listview);
		chatListView.setAdapter(new ChatAdapter(this,chatEntityList));
		
	}
    Intent mNotificationIntent;
    PendingIntent mContentIntent;
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        context = getApplicationContext();

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.chat_window);
		
		Intent i = this.getIntent();
		Bundle b = i.getExtras();
		
		//top panel setting
		//safe_position = b.getInt(Safe.P_SAFE_ID);
		safe_lid = b.getString(Safe.P_SAFE_LID);
		safe_gidh = b.getString(Safe.P_SAFE_GIDH);

		chatName=b.getString(Safe.P_SAFE_WHO);

		TextView top_name=(TextView) findViewById(R.id.chat_top_name);
		top_name.setText(chatName);
		
		peer = D_Peer.getPeerByLID(safe_lid, true, false);
		crtChat = this;
		
		//define notification
		String notiService = Context.NOTIFICATION_SERVICE;
		notiManager = (NotificationManager) this.getSystemService(notiService);
		
		int icon = R.drawable.ic_launcher;
		CharSequence nTitle  = "DDP2P Notification";
		long when = System.currentTimeMillis();

        CharSequence contentTitle ="DDP2P Chat";
        CharSequence contentText = chatName + " sent you a message";

        mNotificationIntent = new Intent(getApplicationContext(), Main.class);
        mNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContentIntent = PendingIntent.getActivity(getApplicationContext(), 0,
                mNotificationIntent, PendingIntent.FLAG_UPDATE_CURRENT); //
                // Intent.FLAG_ACTIVITY_NEW_TASK);
		notificationB = new Notification.Builder(getApplicationContext())
                .setAutoCancel(true)
                .setSmallIcon(icon)
                .setContentTitle(nTitle)
                .setTicker(contentTitle + " // " + contentText)
				.setContentIntent(mContentIntent)
				.setWhen(when)
                //.addAction(icon, contentTitle, mContentIntent)
                .setContentText(contentText);
        

		try {
			this.chatEntityList = AndroidChatReceiver.getListEntity(peer.getGID());

			
		} catch(Exception e) {}
		if (this.chatEntityList == null) {
			this.chatEntityList = new ArrayList<ChatEntity>();
		}
		chat_input=(EditText) findViewById(R.id.chat_input);
		findViewById(R.id.chat_send).setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				chatContent=chat_input.getText().toString();
				
				com.HumanDecisionSupportSystemsLaboratory.DD_P2P.AndroidChat.Main.sendMessage(chatContent, peer.getGID(), peer);
				
				chat_input.setText("");//clean edit text
				//focus again on edit text
				chat_input.setFocusable(true);
				chat_input.setFocusableInTouchMode(true);//set can be get focus
				chat_input.requestFocus();
				chat_input.requestFocusFromTouch();//get focus
			}
		});
	}
	@Override
	public void finish() {
/*		 unregisterReceiver(br);*/
		super.finish();
	}
	
	//broadcast receiver
	public static class MyBroadCastReceiver extends BroadcastReceiver {
		
		private static final String TAG = "BroadCastReceiver";
			
		public  MyBroadCastReceiver(){}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
                NotificationManager notiManager = (NotificationManager)
                        context.getSystemService(Context.NOTIFICATION_SERVICE);

                notiManager.notify(Chat.NOTIFICATION_MSG, notificationB.build());
				Log.d(TAG, "notification+broadcastreceiver successful!");
				// Toast.makeText(context, "notification broadcastreceiver successful!", Toast.LENGTH_SHORT).show();
				
			} catch (Exception e){
				e.printStackTrace();
				Log.d(TAG, "error in notification+broadcastreceiver!");
				Toast.makeText(context, "Error in Chat Notification! "+e.getLocalizedMessage()
						, Toast.LENGTH_SHORT).show();
			}			
		}
	}
/*	public void updateChatView(ChatEntity chatEntity){
		chatEntityList.add(chatEntity);
		chatListView=(ListView) findViewById(R.id.chat_listview);
		chatListView.setAdapter(new ChatAdapter(this,chatEntityList));
	}*/
	public String getPeerGID() {
		return getPeer().getGID();
	}
	public void setPeerName(String peerName) {
		
	}
	private Handler handler = new Handler() {  
        @Override  
        public void handleMessage(Message msg) {  
        	_setChatEntityList(AndroidChatReceiver.getListEntity(getPeerGID()));

        	/*
            if (msg.what == COMPLETED) {  
                stateText.setText("completed");  
            }  
            */
        }  
    };  
}

