/*************************************************************************/
/*************************************************************************/
/*************************************************************************/
/*****************Chat Client Private Chat********************************/
/*************************************************************************/
/*************************************************************************/
/*************************************************************************/
//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.awt.Panel;
import java.awt.Label;
import java.awt.Window;
import java.awt.Frame;
import java.awt.TextField;
import java.awt.TextArea;
import java.awt.Button;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Cursor;
import java.awt.Image;
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
public class PrivateChat extends Frame implements CommonSettings,KeyListener,ActionListener
{
	PrivateChat(ChatPeer Parent, String ToUserName)
	{
		chatPeer = Parent;
		UserName = ToUserName;
		setTitle("Private Chat With "+UserName); 
		Image IconImage = Toolkit.getDefaultToolkit().getImage("images/logo.gif");
		setIconImage(IconImage);	
		setBackground(chatPeer.ColorMap[0]);
		setFont(chatPeer.getFont());
		EmotionFlag = false;
		InitializeComponents();
		/****Window Closing Event *****/
		addWindowListener(new WindowAdapter(){
        public void windowClosing(WindowEvent evt) { ExitPrivateWindow(); }});	
	}	
	
	/******* Initialize All Components **********/
	private void InitializeComponents()
	{
		setLayout(null);
		Label LblConversation = new Label("Conversation With "+UserName);
		LblConversation.setForeground(chatPeer.ColorMap[5]);
		LblConversation.setBounds(5, 30, 400, 20);
		add(LblConversation);
		
		Panel MessagePanel = new Panel(new BorderLayout());
		messagecanvas = new MessageCanvas(chatPeer);				
		MessageScrollView = new ScrollView(messagecanvas,true,true,TAPPANEL_CANVAS_WIDTH,TAPPANEL_CANVAS_HEIGHT,SCROLL_BAR_SIZE);
	  	messagecanvas.scrollview = MessageScrollView;	
		MessagePanel.add("Center",MessageScrollView);
		MessagePanel.setBounds(5, 50, 400, 200);
		add(MessagePanel);
		
		TxtMessage = new TextField();
		TxtMessage.addKeyListener(this);
		TxtMessage.setFont(chatPeer.TextFont);
		TxtMessage.setBounds(5, 260, 320, 20);
		add(TxtMessage);
		
		CmdSend = new CustomButton(chatPeer,"Send");
		CmdSend.addActionListener(this);
		CmdSend.setBounds(335, 260, 70, 20);
		add(CmdSend);
		
		CmdClear = new CustomButton(chatPeer,"Clear");
		CmdClear.addActionListener(this);
		CmdClear.setBounds(5, 290, 80, 20);
		
		CmdIgnore = new CustomButton(chatPeer,"Ignore User");
		CmdIgnore.addActionListener(this);
		CmdIgnore.setBounds(105, 290, 80, 20);
		
		CmdClose = new CustomButton(chatPeer,"Close");
		CmdClose.addActionListener(this);
		CmdClose.setBounds(205, 290, 80, 20);
		
		CmdEmoticons = new CustomButton(chatPeer,"Emoticons");
		CmdEmoticons.addActionListener(this);
		CmdEmoticons.setBounds(305, 290, 80, 20);
		
		add(CmdClear);
		add(CmdIgnore);
		add(CmdClose);
		add(CmdEmoticons);
		
		EmotionPanel = new Panel(new BorderLayout());
		emotioncanvas = new EmotionCanvas(chatPeer,this);
		EmotionScrollView = new ScrollView(emotioncanvas,true,true,EMOTION_CANVAS_WIDTH,EMOTION_CANVAS_HEIGHT,SCROLL_BAR_SIZE);
	  	emotioncanvas.scrollview = EmotionScrollView;
	  	/**********Add Icons into MessageObject *********/
	  	emotioncanvas.AddIconsToMessageObject();
		EmotionPanel.add("Center",EmotionScrollView);
		EmotionPanel.setVisible(false);
		EmotionPanel.setBounds(5,320,EMOTION_CANVAS_WIDTH,EMOTION_CANVAS_HEIGHT);
		add(EmotionPanel);
		
		setSize(PRIVATE_WINDOW_WIDTH,PRIVATE_WINDOW_HEIGHT);
		setResizable(false);
		show();
		this.requestFocus();
	}
	
	/***********Action Listener coding **********/
	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource().equals(CmdSend))
		{
			/******** Send Message *********/
			if (!(TxtMessage.getText().trim().equals("")))
				SendMessage();
		}
		
		/*****Close Button Event ********/
		if(evt.getSource().equals(CmdClose))
		{
			ExitPrivateWindow();	
		}
		
		/*********Clear Button Event ********/
		if(evt.getSource().equals(CmdClear))
		{
			messagecanvas.ClearAll();
		}
		
		/***** Ignore Action Event ********/
		if(evt.getSource().equals(CmdIgnore))
		{			
			if(evt.getActionCommand().equals("Ignore User"))
			{
				chatPeer.tappanel.UserCanvas.IgnoreUser(true,UserName);
				messagecanvas.AddMessageToMessageObject(null, null, null, UserName +" has been ignored!",MESSAGE_TYPE_ADMIN);
				CmdIgnore.setLabel("Allow User");				
			}
			else
			{
				messagecanvas.AddMessageToMessageObject(null, null, null,UserName +" has been removed from ignored list!",MESSAGE_TYPE_ADMIN);
				chatPeer.tappanel.UserCanvas.IgnoreUser(false,UserName);
				CmdIgnore.setLabel("Ignore User");					
			}
		}
		
		/***** Emoticons Action Event ********/
		if(evt.getSource().equals(CmdEmoticons))
		{
			if(EmotionFlag)
			{
				EmotionFlag = false;	
				EmotionPanel.setVisible(false);	
				setSize(PRIVATE_WINDOW_WIDTH,PRIVATE_WINDOW_HEIGHT);			
			}
			else
			{
				EmotionFlag = true;		
				EmotionPanel.setVisible(true);				
				setSize(PRIVATE_WINDOW_WIDTH,PRIVATE_WINDOW_HEIGHT+EMOTION_CANVAS_HEIGHT);
			}
		}
										
	}
	
	/********* Key Listener Event *************/
	public void keyPressed(KeyEvent evt)
	{
		if((evt.getKeyCode() == 10) && (!(TxtMessage.getText().trim().equals(""))))		
		{
			SendMessage();
		}
	}
		
	public void keyTyped(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
	
	private void SendMessage()
	{
		messagecanvas.AddMessageToMessageObject(null, null, null,chatPeer.UserName+": "+TxtMessage.getText(),MESSAGE_TYPE_DEFAULT);
		chatPeer.SentPrivateMessageToServer(TxtMessage.getText(),UserName);			
		TxtMessage.setText("");
		TxtMessage.requestFocus();
	}
	
	/******** Function to Set the Image Name into Text Field ************/
	protected void AddImageToTextField(String ImageName)
   	{
   		if(TxtMessage.getText()==null || TxtMessage.getText().equals(""))
			TxtMessage.setText("~~"+ImageName+" ");
		else
			TxtMessage.setText(TxtMessage.getText()+" "+"~~"+ImageName+" ");
   	}
	
	/*********Function to Add a Message To Messagecanvas *********/
	protected void AddMessageToMessageCanvas(String Message)
	{		
		messagecanvas.AddMessageToMessageObject(null, null, null, Message,MESSAGE_TYPE_DEFAULT);			
	}
	
	protected void DisableAll()
	{
		TxtMessage.setEnabled(false);
		CmdSend.setEnabled(false);	
	}
	
	protected void EnableAll()
	{
		TxtMessage.setEnabled(true);
		CmdSend.setEnabled(true);	
	}
	
	/****** Exit from Private Chat */
    private void ExitPrivateWindow() {
    	chatPeer.RemovePrivateWindow(UserName);        
        setVisible(false);        
    }
    
	/*************** Global Variable Declarations ****************/
	ChatPeer chatPeer;
	protected String UserName;
	MessageCanvas messagecanvas;
	ScrollView MessageScrollView;
	TextField TxtMessage;
	Button CmdSend,CmdClose,CmdIgnore,CmdClear,CmdEmoticons;
	EmotionCanvas emotioncanvas;
	ScrollView EmotionScrollView;
	boolean EmotionFlag;
	Panel EmotionPanel;
}