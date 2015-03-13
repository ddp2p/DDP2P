//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.awt.Panel;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.*;
import java.awt.event.*;
public class TapPanel extends Panel implements CommonSettings,ActionListener
{
	TapPanel(ChatPeer parent)
	{
	  /***********Initialize the Components***********/
	  chatPeer = parent;
	  
	  Panel Tappanel = new Panel(new BorderLayout());
	  CardLayout cardlayout = new CardLayout();
	  Panel mainPanel = new Panel(cardlayout);
	  
	  /*******User Panel Coding Starts***********/
	  Panel UserPanel = new Panel(new BorderLayout());
	  UserCanvas = new ListViewCanvas(chatPeer,USER_CANVAS);
	  
	  UserScrollView = new ScrollView(UserCanvas,true,true,TAPPANEL_CANVAS_WIDTH,TAPPANEL_CANVAS_HEIGHT,SCROLL_BAR_SIZE);
	  UserCanvas.scrollview = UserScrollView;	  	 
	  UserPanel.add("Center",UserScrollView);
	  
	  Panel UserButtonPanel = new Panel(new BorderLayout());
	  cmdInviteFriend = new CustomButton(chatPeer,"Invite a Friend");
	  cmdInviteFriend.addActionListener(this);
	  UserButtonPanel.add("North",cmdInviteFriend);
	  cmdIgnore = new CustomButton(chatPeer,"Ignore a Friend");
	  cmdIgnore.addActionListener(this);
	  //UserButtonPanel.add("Center",cmdIgnore);
	  UserPanel.add("South",UserButtonPanel);
	  
	  /********Wait Panel Coding Starts***********/
	  Panel waitPanel = new Panel(new BorderLayout());
	  waitCanvas = new ListViewCanvas(chatPeer,ROOM_CANVAS);
	  
	  waitScrollView = new ScrollView(waitCanvas,true,true,TAPPANEL_CANVAS_WIDTH,TAPPANEL_CANVAS_HEIGHT,SCROLL_BAR_SIZE);
	  waitCanvas.scrollview = waitScrollView;	  
	  waitPanel.add("Center",waitScrollView);	  
	  
//	  Panel RoomButtonPanel = new Panel(new BorderLayout());
//	  Panel RoomCountPanel = new Panel(new BorderLayout());
//	  Label LblCaption = new Label("ROOM COUNT",1);
//	  RoomCountPanel.add("North",LblCaption);
//	  TxtUserCount = new TextField();
//	  TxtUserCount.setEditable(false);
//	  RoomCountPanel.add("Center",TxtUserCount);	  	  
//	  RoomButtonPanel.add("Center",RoomCountPanel);
//	  
//	  CmdChangeRoom = new CustomButton(chatPeer,"Change Room");
//	  CmdChangeRoom.addActionListener(this);
//	  RoomButtonPanel.add("South",CmdChangeRoom);
//	  
//	  RoomPanel.add("South",RoomButtonPanel);
//	  
	  
	  /********Image Panel Coding Starts***********/
	  Panel ImagePanel = new Panel(new BorderLayout());
	  
	  imagecanvas = new ImageCanvas(chatPeer);
	  ImageScrollView = new ScrollView(imagecanvas,true,true,TAPPANEL_CANVAS_WIDTH,TAPPANEL_CANVAS_HEIGHT,SCROLL_BAR_SIZE);
	  imagecanvas.scrollview = ImageScrollView;
	  /**********Add Icons into MessageObject *********/
	  imagecanvas.AddIconsToMessageObject();
	  ImagePanel.add("Center",ImageScrollView);
	  
	  /*********Add All the Panel in to Main Panel*********/
	  mainPanel.add("UserPanel",UserPanel);
	  mainPanel.add("waitPanel",waitPanel);
	  mainPanel.add("ImagePanel",ImagePanel);
	  cardlayout.show(mainPanel,"UserPanel");
	  BorderPanel borderpanel = new BorderPanel(this,chatPeer,cardlayout,mainPanel,TAPPANEL_WIDTH,TAPPANEL_HEIGHT);
	  
	  borderpanel.addTab("Friends","UserPanel");
	  borderpanel.addTab("Waiting","waitPanel");
	  borderpanel.addTab("Icons","ImagePanel");
	  
	  Tappanel.add(borderpanel);
	  add("Center",Tappanel);	  		  
	  
	  
	  /********Common Things***********/	  	    	  
	}
	
	/***********Action Listener coding **********/
	public void actionPerformed(ActionEvent evt)
	{
//		if(evt.getSource().equals(CmdChangeRoom))
//		{
//			/******** Change Room Coding *********/
//			chatPeer.ChangeRoom();	
//		}
		
		if(evt.getSource().equals(cmdIgnore))
		{			
			if(evt.getActionCommand().equals("Ignore a Friend"))
			{
				UserCanvas.IgnoreUser(true);				
			}
			else
			{
				UserCanvas.IgnoreUser(false);					
			}
		}
		
		if(evt.getSource().equals(cmdInviteFriend))
		{
			//UserCanvas.SendDirectMessage();	
			new InviteFriendDialog(chatPeer);
		}					
	}
	
	/*********Global Variable Declarations***********/	
	ChatPeer chatPeer;
	protected TextField TxtUserCount;
	ScrollView ImageScrollView,UserScrollView,waitScrollView;
	protected ImageCanvas imagecanvas;
	protected ListViewCanvas UserCanvas,waitCanvas;
	Button CmdChangeRoom,cmdIgnore,cmdInviteFriend ;
}