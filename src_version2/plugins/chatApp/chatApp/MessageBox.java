//package com.jeeva.chatclient;
package chatApp;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Button;
import java.awt.Label;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
public class MessageBox extends Dialog implements ActionListener,CommonSettings 
{  
	MessageBox(ChatPeer Parent,boolean okcan)
	{
	   super(Parent, "Information", false);
	   chatPeer = Parent;
	   setBackground(chatPeer.ColorMap[0]);
	   setLayout(new BorderLayout());	 
	   setFont(chatPeer.getFont());  
	   messagecanvas = new MessageCanvas(chatPeer);
	   MessageScrollView = new ScrollView(messagecanvas,true,true,200,100,0);
	   messagecanvas.scrollview = MessageScrollView;
	   messagecanvas.setBackground(chatPeer.ColorMap[0]);	   	
	   add("Center",MessageScrollView);	   	   
	   addOKCancelPanel(okcan);
	   createFrame();	  
	   pack();
	   setVisible(true);
	   setSize(200,160);
	   setResizable(false);
	}
	
	protected void AddMessage(String message)
	{	
		messagecanvas.AddMessageToMessageObject(message,MESSAGE_TYPE_JOIN);		
	}
	
   	private void addOKCancelPanel( boolean okcan ) 
  	{
	  Panel panel = new Panel();
   	  panel.setLayout(new FlowLayout());
   	  createOKButton( panel);
   	  if (okcan == true)
      	createCancelButton( panel );
   	  add("South",panel);
   	}

  private void createOKButton(Panel panel) 
  {
   	CmdOk = new CustomButton(chatPeer,"OK");   	
   	panel.add(CmdOk);
   	CmdOk.addActionListener(this); 
  }

  private void createCancelButton(Panel panel) 
  {
   	CmdCancel = new CustomButton(chatPeer,"Cancel");
   	panel.add(CmdCancel);
   	CmdCancel.addActionListener(this);
  }

  private void createFrame() 
  {
   	Dimension dimension = getToolkit().getScreenSize();
   	setLocation(dimension.width/3,dimension.height/3);
  }

  public void actionPerformed(ActionEvent ae)
  {
   if(ae.getSource() == CmdOk) 
   {     
     dispose();
   }
   else if(ae.getSource() == CmdCancel) 
   {
     dispose();
   }
  }
   
   /***********Global Variable Declarations **************/
   ChatPeer chatPeer;
   Button CmdOk, CmdCancel;
   MessageCanvas messagecanvas;
   ScrollView MessageScrollView;
 }
