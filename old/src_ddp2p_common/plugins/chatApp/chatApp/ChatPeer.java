/*************************************************************************/
/*************************************************************************/
/*************************************************************************/
/*****************Chat Client Application*********************************/
/*************************************************************************/
/*************************************************************************/
/*************************************************************************/
//package com.jeeva.chatclient;
package chatApp;
//package plugin;
	
import dd_p2p.plugin.*;
import static java.lang.System.out;
import static util.Util._;

import java.awt.Panel;
import java.awt.Label;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Window;
import java.awt.TextField;
import java.awt.TextArea;
import java.awt.Frame;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.CardLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.MediaTracker;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.Serializable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.Socket;
import java.awt.Toolkit;
import java.awt.MenuBar;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MenuShortcut;

//import com.almworks.sqlite4java.SQLiteException;

//import config.DD;
//import config.Identity;
//import util.Application;
//import util.DBInterface;
/**********Chat Client**************/
public class ChatPeer extends Frame implements Serializable,Runnable,KeyListener,ActionListener,CommonSettings {
	// create GUI components, add listeners and initials values 
	public ChatPeer(String user) 
	{		
		toolkit = Toolkit.getDefaultToolkit();// system info		
		if(toolkit.getScreenSize().getWidth() > 778)
			setSize(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT);
		else
			setSize((int)toolkit.getScreenSize().getWidth(),(int)toolkit.getScreenSize().getHeight() - 20);			
		setResizable(false);
		dimension = getSize();	
		setLayout(new BorderLayout());	

		setTitle(ChatApp_NAME);		
		addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent evt) { //DisconnectChat();
		                                             //System.exit(0);}
		                                             evt.getWindow().hide();
		                                             }
		                                             });
				
		/*****Loading menubar ***********/
		MenuBar menubar = new MenuBar();
		Menu loginmenu = new Menu("Login");		
		loginitem = new MenuItem("Login");
		loginitem.addActionListener(this);
		disconnectitem = new MenuItem("Disconnect");
		disconnectitem.addActionListener(this);
		seperatoritem = new MenuItem("-");
		quititem = new MenuItem("Quit");
		quititem.addActionListener(this);
		loginmenu.add(loginitem);
		loginmenu.add(disconnectitem);
		loginmenu.add(seperatoritem);
		loginmenu.add(quititem);
		
		Menu aboutmenu = new Menu("Help ");
		aboutitem = new MenuItem("About "+ChatApp_NAME);
		aboutitem.addActionListener(this);
		aboutmenu.add(aboutitem);
		
		menubar.add(loginmenu);
		menubar.add(aboutmenu);
		setMenuBar(menubar);
				
		/**********Getting all the Parameteres***********/	
		UserName 	= user;//"";			
		UserRoom	="No One";
		IconCount 	= 21;
		ChatLogo	= "logo.png";
		BannerName 	= "P2PChatBanner3.png";
		RoomList 	= "";
		IsProxy = false;
		/*********Assigning Global Colors*************/
		ColorMap 	= new Color[MAX_COLOR];
		/*******Backgorund*********/
		ColorMap[0] =  new Color(159,159,159); 
		/*******Information Panel Background*********/
		ColorMap[1] =  new Color(77,77,77);
		/*******Button Foreground*********/
		ColorMap[2] =  Color.black; 
		/*******Button Background**************/
		ColorMap[3] =  new Color(159,159,159); 
		/*******sstab button****************/
		ColorMap[4] =  new Color(77,77,77);
		/*******message canvas*********/
		ColorMap[5] =  Color.black;
		/*******Top Panel Background*********/
		ColorMap[6] =  Color.gray;
		/*******Label Text Colors*********/
		ColorMap[7] =  Color.white;
		
		/**********Loading Images*********/		
		tracker = new MediaTracker(this);
		int ImageCount = 0;
		//ImgLogo 	= toolkit.getImage(ChatLogo);
	//	tracker.addImage(ImgLogo,ImageCount);
		//ImageCount++;
	//	ImgBanner 	= toolkit.getImage(BannerName);	
	    ImgBanner 	= config.DDIcons.getImageFromResource("/chatApp_icons/",BannerName, this);	
		tracker.addImage(ImgBanner,ImageCount);
		ImageCount++;
				
		/**********Loading Icons....***********/
		IconArray = new Image[IconCount];
		for(G_ILoop = 0; G_ILoop < IconCount; G_ILoop++)
		{
			IconArray[G_ILoop] = config.DDIcons.getImageFromResource("/chatApp_icons/","photo"+G_ILoop+".gif", this);//toolkit.getImage("icons/photo"+G_ILoop+".gif");
			tracker.addImage(IconArray[G_ILoop],ImageCount);
			ImageCount++;
		}
		
		/*********Initialize Private Window **********/
		privatewindow = new PrivateChat[MAX_PRIVATE_WINDOW];
		PrivateWindowCount = 0;
		
				
		try{
			SetAppStatus("Loading Images and Icons.....");			
			tracker.waitForAll();
		}catch (InterruptedException e){System.out.println("Error");}
		
		//setIconImage(toolkit.getImage("images/logo.gif"));
		SetAppStatus("");		
		/**********Initializing all the Components*********/
		InitializeAppComponents();	
		
	}
	/*
	public static Image getImageFromResource(String entry, String _splash, Object _frame){
		System.err.println("ChatPeer: getImageFromResource: "+entry+_splash);
		Class<?> cl;
		if(_frame == null) cl = ChatPeer.class;
		else cl = _frame.getClass();
		String splash = entry+_splash;
		
		Image image; // = new Image();//");
        image = Toolkit.getDefaultToolkit().getImage(cl.getResource(splash));
    	//Image newimg = img.getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH);
        //if(descr!=null) image.setDescription(descr);
		System.err.println("ChatPeer: getImageFromResource: done");
        return image;
	}
	*/
	public void setUser(String user){
			UserName 	= user;
			UpdateInformationLabel();
	}
	private void ConnectToServer()
	{
		/***********Initialize the Socket*******/
		messagecanvas.ClearAll();
		messagecanvas.AddMessageToMessageObject("Connecting To Server... Please Wait...",MESSAGE_TYPE_ADMIN);								
		try {
			
			if(IsProxy)
			{				
				/*********Proxy***********/				
				SocksSocketImplFactory factory=new SocksSocketImplFactory(ProxyHost, ProxyPort);							
				SocksSocket.setSocketImplFactory(factory);						
				socket = new SocksSocket(ServerName,ServerPort);
				socket.setSoTimeout(0);
			}
			else
			{
				/*******Not Proxy*********/
				socket = new Socket(ServerName,ServerPort);					
			}				
			dataoutputstream = new DataOutputStream(socket.getOutputStream());
			SendMessageToServer("HELO "+UserName);			
			datainputstream  = new DataInputStream(socket.getInputStream());
			/***********Send HELO To Server **********/
			StartFlag = true;			
			thread = new Thread(this);
			thread.start();				
			EnableAll();					
		}catch(IOException _IoExc) { QuitConnection(QUIT_TYPE_NULL);}			
	}
	
	private void SendMessageToServer(String Message)
	{
		try {
			dataoutputstream.writeBytes(Message+"\r\n");	
		}catch(IOException _IoExc) { QuitConnection(QUIT_TYPE_DEFAULT);}			
	}
	/*******Initialize all the App Components********/
	private void InitializeAppComponents()
	{
		/*******Common Settings***********/
		setBackground(ColorMap[0]);	
		Font font = new Font("Dialog",Font.BOLD,11);
		TextFont = new Font("Dialog",0,11);	
		setFont(font);	
		
		/***********Top Panel Coding*************/
		Panel TopPanel = new Panel();
		TopPanel.setBackground(ColorMap[6]);
		//Panel LogoPanel = new ImagePanel(this,ImgLogo);		
		//TopPanel.add("East",LogoPanel);		
		Panel BannerPanel = new ImagePanel(this,ImgBanner);
		TopPanel.add(BannerPanel);		
		add("North",TopPanel);	
		
		/*************Information Label Panel Coding*************/
		Panel CenterPanel = new Panel(new BorderLayout());
		Panel InformationPanel = new Panel(new BorderLayout());	
		InformationPanel.setBackground(ColorMap[1]);			
		InformationLabel = new Label();		
		InformationLabel.setAlignment(1);
		UpdateInformationLabel();  
		InformationLabel.setForeground(ColorMap[7]); 
		InformationPanel.add("Center",InformationLabel);
		CenterPanel.add("North",InformationPanel);
		
		/*********Message Canvas and SSTAB Coding********/
		Panel MessagePanel = new Panel(new BorderLayout());
		messagecanvas = new MessageCanvas(this);				
		MessageScrollView = new ScrollView(messagecanvas,true,true,TAPPANEL_CANVAS_WIDTH,TAPPANEL_CANVAS_HEIGHT,SCROLL_BAR_SIZE);
	  	messagecanvas.scrollview = MessageScrollView;	
		MessagePanel.add("Center",MessageScrollView);
		
		tappanel = new TapPanel(this);
						
	    MessagePanel.add("East",tappanel);  	    
	    CenterPanel.add("Center",MessagePanel);
	    
	    /*********Input Panel Coding Starts..*********/
	    Panel InputPanel = new Panel(new BorderLayout());
	    Panel TextBoxPanel = new Panel(new BorderLayout());
	    Label LblGeneral = new Label("Message!");	    
	    TxtMessage = new TextField();
	    TxtMessage.addKeyListener(this);
	    TxtMessage.setFont(TextFont);
	    CmdSend = new CustomButton(this,"Send Message!");
	    CmdSend.addActionListener(this);
	    TextBoxPanel.add("West",LblGeneral);
	    TextBoxPanel.add("Center",TxtMessage);
	    TextBoxPanel.add("East",CmdSend);
	    InputPanel.add("Center",TextBoxPanel);
	    
	    Panel InputButtonPanel =new Panel(new BorderLayout());
	    CmdExit = new CustomButton(this,"Exit Chat");
	    CmdExit.addActionListener(this);
	    InputButtonPanel.add("Center",CmdExit);
	  	InputPanel.add("East",InputButtonPanel);
	  	
	  	Panel EmptyPanel = new Panel();
	  	//InputPanel.add("South",EmptyPanel);
	  	
	  	CenterPanel.add("South",InputPanel);
	  	
	  	
		add("Center",CenterPanel);	
		
		//DisableAll();
		
		//LoginToChat();// beginChat
		beginChat();
	}
	public boolean peerExist(String peer_GID){
		if(tappanel.UserCanvas.getIndexOfGID(peer_GID)==-1)
			return false;
		return true;
	}
	private void beginChat(){
	//	setVisible(true);
		//connectToDB();
//		try {
//			UserName= DD.getAppText("my_peer_name");
//			System.out.println(UserName);
//		} catch (SQLiteException e1) {
//			// TODO Auto-generated catch block
//			System.err.println("chatApp.beginChat():  I'm not a peer in my database: ");
//			e1.printStackTrace();
//			return;
//		}
//		
//		ArrayList<ArrayList<Object>> peers=null;
//		try {
//			peers = Application.db.select("SELECT name, slogan FROM peers where global_peer_ID <> ? ;",new String[]{DD.getAppText("my_global_peer_ID")});
//		} catch (SQLiteException e) {
//			Application.warning(_("Database unreachable: ")+e, _("Database unreachable"));
//		}
//		if((peers==null) || (peers.size()==0)){
//			System.err.println("chatApp.beginChat():  no peers in my database: ");
//			return ;
//		}
//		System.out.println(peers.get(0).get(0));
//		for(int i=0; i<peers.size();i++)
//		tappanel.UserCanvas.AddListItemToMessageObject(peers.get(i).get(0).toString());
//		UpdateInformationLabel();
	}
	public void addPeer(String name, String GID){
		tappanel.UserCanvas.AddListItemToMessageObject(GID,name);
		UpdateInformationLabel();
	}
	private void LoginToChat()
	{
		/********* Open the Dialog *********/
		dialog = new InformationDialog(this); // display dialog	
		if (dialog.IsConnect == true)
		{
			UserName 	= dialog.TxtUserName.getText();
			//UserRoom 	= dialog.roomchoice.getSelectedItem();
			ServerName 	= dialog.TxtServerName.getText();
			ServerPort 	= Integer.parseInt(dialog.TxtServerPort.getText());
			if(dialog.IsProxyCheckBox.getState() == true)
			{
				IsProxy = true;
				ProxyHost = dialog.TxtProxyHost.getText();
				ProxyPort = Integer.parseInt(dialog.TxtProxyPort.getText());
			}
			else
			{
				IsProxy = false;	
			}
			ConnectToServer();				
		}		
	}
	
	/*********Button Events *****/
	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource().equals(CmdSend))
		{   if (UserRoom.equals("No One")){
			   messagecanvas.AddMessageToMessageObject("You need to select a friend first!  ",MESSAGE_TYPE_INFO );
						
			//Application.warning("You need to select a friend first!  ", "Info");
			return;
		}
			
			if (!(TxtMessage.getText().trim().equals("")))
				SendMessage(UserRoom,TxtMessage.getText().trim());	
		}	
		
		if ((evt.getSource().equals(CmdExit)) || (evt.getSource().equals(quititem)))
		{
			DisconnectChat();
			this.hide();
		//	System.exit(0);
		}
		
		if(evt.getSource().equals(loginitem))
		{
			LoginToChat();				
		}
		
		if(evt.getSource().equals(disconnectitem))
		{			
			DisconnectChat();						
		}		
		if(evt.getSource().equals(aboutitem))
		{			
			MessageBox messagebox = new MessageBox(this,false);					
			messagebox.AddMessage("~~13 "+ChatApp_NAME);
			messagebox.AddMessage("Developed By...");
			messagebox.AddMessage(COMPANY_NAME);
			
		}
		
		
	}
	
	/********* Key Listener Event *************/
	public void keyPressed(KeyEvent evt)
	{
		if((evt.getKeyCode() == 10) && (!(TxtMessage.getText().trim().equals(""))))		
		{
			SendMessage(UserRoom, TxtMessage.getText().trim());
		}
	}
		
	public void keyTyped(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
    public void ReceiveMessage(String m){
    	messagecanvas.AddMessageToMessageObject(m,MESSAGE_TYPE_DEFAULT);	
    }
	/******** Function To Send MESS Rfc to Server *************/
	private void SendMessage(String to, String m)
	{
		/********Sending a Message To Peer *********/
		 plugin_data.PluginRequest msg = new plugin_data.PluginRequest();
		 msg.type=plugin_data.PluginRequest.MSG;
		 msg.plugin_GID=(String)Main.getPluginDataHashtable().get("plugin_GID");
		 msg.msg=(UserName+": "+m).getBytes();// "hi khalid PC".getBytes();
		 msg.peer_GID= UserRoomGID;
		 Main.enqueue(msg);
		//SendMessageToServer("MESS "+UserRoom+"~"+UserName+": "+TxtMessage.getText());
		messagecanvas.AddMessageToMessageObject(UserName+": "+TxtMessage.getText(),MESSAGE_TYPE_DEFAULT);	
		TxtMessage.setText("");
		TxtMessage.requestFocus();

			
	}

	/*********Function To Update the Information Label*****/
	protected void UpdateInformationLabel()
	{
		stringbuffer = new StringBuffer();
		stringbuffer.append("User Name: ( ");
		stringbuffer.append(UserName+ " )");
		stringbuffer.append("                    ");
		stringbuffer.append("Chat with: ( ");
		stringbuffer.append(UserRoom+" )");
//		stringbuffer.append("       ");
//		stringbuffer.append("No. Of Users: ");
//		stringbuffer.append(TotalUserCount);
//		stringbuffer.append("       ");	
		InformationLabel.setText(stringbuffer.toString());
		
	}
	
	/********Implements the Thread ****************/
	public void run()
	{
		while(thread != null)
		{
			try {
					ServerData = datainputstream.readLine();									
					/********* LIST UserName;UserName; RFC Coding***********/
					if(ServerData.startsWith("LIST"))
					{  
						Tokenizer = new StringTokenizer(ServerData.substring(5),";");						
						/********Update the Information Label *********/
						TotalUserCount = Tokenizer.countTokens();						
						UpdateInformationLabel();
						/**********Add User Item into User Canvas *********/						
						tappanel.UserCanvas.ClearAll();
						while(Tokenizer.hasMoreTokens())
						{							
							tappanel.UserCanvas.AddListItemToMessageObject("",Tokenizer.nextToken());							
						}	
						
						messagecanvas.ClearAll();										
						messagecanvas.AddMessageToMessageObject("Welcome To The "+UserRoom+" Room!",MESSAGE_TYPE_JOIN);		
					}
					
					/*********Room Rfc ********/
					if( ServerData.startsWith("ROOM"))
					{
						/********** Loading Room List in to Room Canvas **************/
						Tokenizer = new StringTokenizer(ServerData.substring(5),";");
						UserRoom = Tokenizer.nextToken();
						UpdateInformationLabel();
						/**********Add User Item into User Canvas *********/						
						tappanel.waitCanvas.ClearAll();
						tappanel.waitCanvas.AddListItemToMessageObject("",UserRoom);
						while(Tokenizer.hasMoreTokens())
						{							
							tappanel.waitCanvas.AddListItemToMessageObject("", Tokenizer.nextToken());							
						}
					}
					
					/********** ADD RFC *********/
					if(ServerData.startsWith("ADD"))
					{
						/********Update the Information Label *********/
						TotalUserCount++;
						UpdateInformationLabel();
						
						/**********Add User Item into User Canvas *********/
						SplitString = ServerData.substring(5);
						EnablePrivateWindow(SplitString);
						tappanel.UserCanvas.AddListItemToMessageObject("", SplitString);							
						messagecanvas.AddMessageToMessageObject(SplitString + " joins chat...",MESSAGE_TYPE_JOIN);						
					}
					
					/*********If User Name Alread Exists **********/
					if (ServerData.startsWith("EXIS"))
					{						
						messagecanvas.AddMessageToMessageObject(" User Name Already Exists... Try Again With Some Other Name!",MESSAGE_TYPE_ADMIN);								
						thread = null;
						QuitConnection(QUIT_TYPE_NULL);
					}					 
					
					/******** REMOVE User RFC Coding **********/
					if (ServerData.startsWith("REMO"))
					{						
						SplitString = ServerData.substring(5);	
						
						tappanel.UserCanvas.RemoveListItem(SplitString);
						RemoveUserFromPrivateChat(SplitString);
						messagecanvas.AddMessageToMessageObject(SplitString+" has been logged Out from Chat!",MESSAGE_TYPE_LEAVE );
						
						/*****Update the Information Label ********/
						TotalUserCount--;
						UpdateInformationLabel();
						
					}
					
					/******** MESS RFC Coding Starts **********/
					if( ServerData.startsWith("MESS"))
					{
						/**** Chk whether ignored user *********/
						if(!(tappanel.UserCanvas.IsIgnoredUser(ServerData.substring(5,ServerData.indexOf(":")))))						
							messagecanvas.AddMessageToMessageObject(ServerData.substring(5),MESSAGE_TYPE_DEFAULT);							
					}
					
					/***** KICK RFC Starts ***********/
					if (ServerData.startsWith("KICK"))
					{
						messagecanvas.AddMessageToMessageObject("You are Kicked Out From Chat for flooding the message!",MESSAGE_TYPE_ADMIN);
						thread = null;
						QuitConnection(QUIT_TYPE_KICK);	
					}
					
					/***** INKI RFC (Information about kicked off User *********/
					if( ServerData.startsWith("INKI"))
					{
						SplitString = ServerData.substring(5);							
						tappanel.UserCanvas.RemoveListItem(SplitString);
						RemoveUserFromPrivateChat(SplitString);
						messagecanvas.AddMessageToMessageObject(SplitString+" has been kicked Out from Chat by the Administrator!",MESSAGE_TYPE_ADMIN );
						
						/*****Update the Information Label ********/
						TotalUserCount--;
						UpdateInformationLabel();	
					}
					
					/***** Change Room RFC **********/
					if( ServerData.startsWith("CHRO"))
					{
						UserRoom = ServerData.substring(5);	
					}
					
					/********** Join Room RFC *************/
					if( ServerData.startsWith("JORO"))
					{
						SplitString = ServerData.substring(5);
						tappanel.UserCanvas.AddListItemToMessageObject("", SplitString);
						/*****Update the Information Label ********/
						TotalUserCount++;
						UpdateInformationLabel();	
						
						messagecanvas.AddMessageToMessageObject(SplitString + " joins chat...",MESSAGE_TYPE_JOIN);
					}
					
					/***********Leave Room RFC **********/
					if( ServerData.startsWith("LERO"))
					{
						SplitString = ServerData.substring(5,ServerData.indexOf("~"));
						tappanel.UserCanvas.RemoveListItem(SplitString);
						messagecanvas.AddMessageToMessageObject(SplitString+" has leaves "+UserRoom+" Room and join into "+ServerData.substring(ServerData.indexOf("~")+1)+" Room",MESSAGE_TYPE_ADMIN);													
						
						/*****Update the Information Label ********/
						TotalUserCount--;
						UpdateInformationLabel();	
					}
					
					/********** Room Count RFC ********/					
					if( ServerData.startsWith("ROCO"))
					{
						SplitString = ServerData.substring(5,ServerData.indexOf("~"));
						tappanel.TxtUserCount.setText("Total Users in "+SplitString+" : "+ServerData.substring(ServerData.indexOf("~")+1));
					}
					
					
					/******* Private Message RFC **********/
					if( ServerData.startsWith("PRIV"))
					{												
						SplitString = ServerData.substring(5,ServerData.indexOf(":"));
						/**** Chk whether ignored user *********/
						if(!(tappanel.UserCanvas.IsIgnoredUser(SplitString)))	
						{
							boolean PrivateFlag = false;
							for(G_ILoop = 0; G_ILoop < PrivateWindowCount;G_ILoop++)
							{								
								if(privatewindow[G_ILoop].UserName.equals(SplitString))
								{
									privatewindow[G_ILoop].AddMessageToMessageCanvas(ServerData.substring(5));
									privatewindow[G_ILoop].show();
									privatewindow[G_ILoop].requestFocus();
									PrivateFlag = true;
									break;										
								}
							}	
							
							if(!(PrivateFlag))
							{	
								if(PrivateWindowCount >= MAX_PRIVATE_WINDOW)
								{
									messagecanvas.AddMessageToMessageObject("You are Exceeding private window limit! So you may lose some message from your friends!",MESSAGE_TYPE_ADMIN);	
								}
								else
								{														
									privatewindow[PrivateWindowCount++] = new PrivateChat(this,SplitString);
									privatewindow[PrivateWindowCount-1].AddMessageToMessageCanvas(ServerData.substring(5));
									privatewindow[PrivateWindowCount-1].show();
									privatewindow[PrivateWindowCount-1].requestFocus();																
								}
							}
							
						}						
					}
				}catch(Exception _Exc) { messagecanvas.AddMessageToMessageObject(_Exc.getMessage(),MESSAGE_TYPE_ADMIN);QuitConnection(QUIT_TYPE_DEFAULT); }	
		}	
	}
	
	/***** Enable the Private Chat when the End User logged (in)****/
	private void EnablePrivateWindow(String ToUserName)
	{
		for(G_ILoop = 0; G_ILoop < PrivateWindowCount; G_ILoop++)
		{
			if(privatewindow[G_ILoop].UserName.equals(ToUserName))
			{
				privatewindow[G_ILoop].messagecanvas.AddMessageToMessageObject(ToUserName + " is Currently Online!",MESSAGE_TYPE_ADMIN);	
				privatewindow[G_ILoop].EnableAll();			
				return;	
			}
		}	
	}
	
	/***** Disable the Private Chat when the End User logged out****/
	private void RemoveUserFromPrivateChat(String ToUserName)
	{
		for(G_ILoop = 0; G_ILoop < PrivateWindowCount; G_ILoop++)
		{
			if(privatewindow[G_ILoop].UserName.equals(ToUserName))
			{
				privatewindow[G_ILoop].messagecanvas.AddMessageToMessageObject(ToUserName + " is Currently Offline!",MESSAGE_TYPE_ADMIN);	
				privatewindow[G_ILoop].DisableAll();			
				return;	
			}
		}	
	}
	
	/*******Function To Send Private Message To Server ***********/
	protected void SentPrivateMessageToServer(String Message, String ToUserName)
	{
		SendMessageToServer("PRIV "+ToUserName+"~"+UserName+": "+Message);	
	}
	
	/******* Function To Remove Private Window ***************/
	protected void RemovePrivateWindow(String ToUserName)
	{		
		int m_UserIndex = 0;
		for(G_ILoop = 0; G_ILoop < PrivateWindowCount; G_ILoop++)
		{
			m_UserIndex++;
			if(privatewindow[G_ILoop].UserName.equals(ToUserName)) break;
		}						
		for(int m_iLoop = m_UserIndex;m_iLoop < PrivateWindowCount; m_iLoop++)
		{
			privatewindow[m_iLoop] = privatewindow[m_iLoop+1];	
		}
		
		PrivateWindowCount--;		
	}	
	
	/********* Function to Change Room *******/
	protected void ChangeRoom()
	{
		if(tappanel.waitCanvas.SelectedUser.equals(""))
		{
			messagecanvas.AddMessageToMessageObject("Invalid Room Selection!",MESSAGE_TYPE_ADMIN);
			return;	
		}
		
		if(tappanel.waitCanvas.SelectedUser.equals(UserRoom))
		{
			messagecanvas.AddMessageToMessageObject("You are already in that ROOM!",MESSAGE_TYPE_ADMIN);
			return;	
		}
		
		SendMessageToServer("CHRO "+UserName+"~"+tappanel.waitCanvas.SelectedUser);
	}
	
	/******* Function to Send a RFC for Get a Room User Count ********/
	protected void GetRoomUserCount(String RoomName)
	{
		SendMessageToServer("ROCO "+RoomName);	
	}
	
	/******** Function to Set the Image Name into Text Field ************/
	protected void AddImageToTextField(String ImageName)
   	{
   		if(TxtMessage.getText()==null || TxtMessage.getText().equals(""))
			TxtMessage.setText("~~"+ImageName+" ");
		else
			TxtMessage.setText(TxtMessage.getText()+" "+"~~"+ImageName+" ");
   	}
	
	
	/*********Function to Destroy all the Objects********/
	private void QuitConnection(int QuitType)
	{		
		/*if(socket != null)
		{
			try {
				if (QuitType == QUIT_TYPE_DEFAULT)
					SendMessageToServer("QUIT "+UserName+"~"+UserRoom);
				if (QuitType == QUIT_TYPE_KICK)
					SendMessageToServer("KICK "+UserName+"~"+UserRoom);
				socket.close();	
				socket = null;
				tappanel.UserCanvas.ClearAll();					
			}catch(IOException _IoExc) { }			
		}
		if(thread != null)
		{
			thread.stop();
			thread = null;
		}		
		DisableAll();
		StartFlag = false;
		SetAppStatus("ADMIN: CONNECTION TO THE SERVER CLOSED.");*/					
	}
	
	/***** Function To Disable All Components ********/
	private void DisableAll()
	{		
		TxtMessage.setEnabled(false);
		CmdSend.setEnabled(false);
		tappanel.enable(false);			
		disconnectitem.setEnabled(false);
		loginitem.setEnabled(true);
		
		UserName = "";
		UserRoom = "no one";
		TotalUserCount = 0;
	}
	/***** Function To Enable All Components ********/
	private void EnableAll()
	{
		TxtMessage.setEnabled(true);
		CmdSend.setEnabled(true);
		tappanel.enable(true);	
		disconnectitem.setEnabled(true);
		loginitem.setEnabled(false);
	}
	
	/*******Disconnect Chat ********/
	private void DisconnectChat()
	{
		/*if(socket != null)
			{
				messagecanvas.AddMessageToMessageObject("CONNECTION TO THE SERVER CLOSED",MESSAGE_TYPE_ADMIN);				
				QuitConnection(QUIT_TYPE_DEFAULT);			
			}*/	
		
		
	}
	
	/*********Setting the AppletStatus********/
	private void SetAppStatus(String Message)
	{
		if (messagecanvas != null)
			messagecanvas.AddMessageToMessageObject(Message,MESSAGE_TYPE_ADMIN);		
	}
	
	public static void main(String args[]) {
	//	String dfname = Application.DELIBERATION_FILE;
//		if(args.length>0) {
//			dfname = args[0];
//		}
	//	if(DEBUG) out.println("Opening database: "+dfname);
		//Application.db = new DBInterface(dfname);
	//	ChatPeer mainFrame = new ChatPeer();				
	}
	
	/**************Global Variable Declarations*****************/
	String UserName,UserRoom, UserRoomGID, ServerName,AppletStatus,ChatLogo,BannerName,ProxyHost,ServerData,RoomList,SplitString;
	Image ImgLogo,ImgBanner;
	int ServerPort,ProxyPort,IconCount,TotalUserCount,G_ILoop;
	boolean StartFlag,IsProxy;	
	Socket socket;
	DataInputStream datainputstream;
	DataOutputStream dataoutputstream;
	Color[] ColorMap;
	Dimension dimension;
	MediaTracker tracker;
	Label InformationLabel;
	StringBuffer stringbuffer;
	Image[] IconArray;
	MessageCanvas messagecanvas;
	ScrollView MessageScrollView;
	Thread thread;
	StringTokenizer Tokenizer;
	TapPanel tappanel;
	TextField TxtMessage;
	Button CmdSend,CmdExit;
	Font TextFont;
	protected PrivateChat[] privatewindow;
	protected int PrivateWindowCount;
	InformationDialog dialog;
	Toolkit toolkit;
	MenuItem loginitem;
	MenuItem disconnectitem;
	MenuItem seperatoritem;
	MenuItem quititem,aboutitem;
}
