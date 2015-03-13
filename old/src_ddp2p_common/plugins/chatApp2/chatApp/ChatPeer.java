
/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Khalid Alhamed
		Author: Khalid Alhamed: kalhamed2011@fit.edu
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
/*                      Chat Client Application                              */
/* ------------------------------------------------------------------------- */


package dd_p2p.plugin;

	
import dd_p2p.plugin.*;
import static java.lang.System.out;
//import static util.Util._;

import java.util.Date;
import java.util.Random;
import java.util.Hashtable;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

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
		public void windowClosing(WindowEvent evt) { 
		                                             //System.exit(0);}
		                                             evt.getWindow().hide();
		                                             }
		                                             });
				
		/*****Loading menubar ***********/
		MenuBar menubar = new MenuBar();
		Menu fileMenu = new Menu("File");		
		saveItem = new MenuItem("Save Chat");
		saveItem.addActionListener(this);
		hideItem = new MenuItem("Hide");
		hideItem.addActionListener(this);
		seperatoritem = new MenuItem("-");
		quititem = new MenuItem("Quit");
		quititem.addActionListener(this);
		fileMenu.add(saveItem);
		fileMenu.add(hideItem);
		fileMenu.add(seperatoritem);
		fileMenu.add(quititem);
		
		Menu aboutmenu = new Menu("Help ");
		aboutitem = new MenuItem("About "+ChatApp_NAME);
		aboutitem.addActionListener(this);
		aboutmenu.add(aboutitem);
		
		menubar.add(fileMenu);
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
	    ImgBanner 	= widgets.app.DDIcons.getImageFromResource("/chatApp_icons/",BannerName, this);	
		tracker.addImage(ImgBanner,ImageCount);
		ImageCount++;
				
		/**********Loading Icons....***********/
		IconArray = new Image[IconCount];
		for(G_ILoop = 0; G_ILoop < IconCount; G_ILoop++)
		{
			IconArray[G_ILoop] = widgets.app.DDIcons.getImageFromResource("/chatApp_icons/","photo"+G_ILoop+".gif", this);//toolkit.getImage("icons/photo"+G_ILoop+".gif");
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
		SetAppStatus("");		
		/**********Initializing all the Components*********/
		InitializeAppComponents();	
		
	}

	public void setUser(String user){
			UserName 	= user;
			UpdateInformationLabel();
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
		

	}
	public boolean peerExist(String peer_GID){
		if(tappanel.UserCanvas.getIndexOfGID(peer_GID)==-1)
			return false;
		return true;
	}

	public void addPeer(String name, String GID, boolean s){
		tappanel.UserCanvas.AddListItemToMessageObject(GID,name,s);
		if(s==true){ UserRoom = name; UserRoomGID=GID;}
		UpdateInformationLabel();
	}
	private void saveChat()
	{
	}
	
	/*********Button Events *****/
	public void actionPerformed(ActionEvent evt)
	{
		if(evt.getSource().equals(CmdSend))
		{   if (UserRoom.equals("No One"))
			{
			   messagecanvas.AddMessageToMessageObject(null,null,null,"You need to select a friend first!  ",MESSAGE_TYPE_INFO );
						
			//Application.warning("You need to select a friend first!  ", "Info");
			return;
			}
			
			if (!(TxtMessage.getText().trim().equals("")))
				SendMessage(UserRoomGID,TxtMessage.getText().trim());	
		}	
		
		if ((evt.getSource().equals(CmdExit)) || (evt.getSource().equals(quititem)))
		{
			//DisconnectChat();
			this.hide();
		//	System.exit(0);
		}
		
		if(evt.getSource().equals(saveItem))
		{
			saveChat();				
		}
		
		if(evt.getSource().equals(hideItem))
		{	this.hide();		
			//DisconnectChat();						
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
			SendMessage(UserRoomGID, TxtMessage.getText().trim());
		}
	}
		
	public void keyTyped(KeyEvent e){}
	public void keyReleased(KeyEvent e){}
	
	public static byte[] simple_hash(byte[] msg, String hash_alg) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(hash_alg);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace(); return null;}
		digest.update(msg);
		byte mHash[] = digest.digest();
		return mHash;
	}
	
	Hashtable msgTable = new Hashtable();
 	ArrayList<String> msgHash;
 	
 	public void confirmMessage(String PeerGID, byte[] session_id, BigInteger sequence){
 		messagecanvas.confirmMsg(PeerGID, session_id, sequence, Color.green );
 	}

    public void ReceiveMessage(String m, String pName, String PeerGID, byte[] session_id, BigInteger sequence){
    	String displayMsg=pName+": "+m;
    	
    	messagecanvas.AddMessageToMessageObject(PeerGID, session_id, sequence, displayMsg, MESSAGE_TYPE_DEFAULT);
    	
//    	if(m.indexOf("^")!=-1)
//    			displayMsg = m.substring(m.indexOf("^")+1,m.length());
//    	if(msgTable.isEmpty()||(msgHash=(ArrayList<String>)msgTable.get(peer_GID))==null)
//    	{
//    		msgHash= new ArrayList<String>();
//    		msgHash.add(new String(simple_hash(m.getBytes(),"SHA-256")));
//    		msgTable.put(peer_GID, msgHash);
//    		messagecanvas.AddMessageToMessageObject(displayMsg,MESSAGE_TYPE_DEFAULT);
//    		return;
//    	}
//    	if(!msgExistInHashtable(msgHash,new String(simple_hash(m.getBytes(),"SHA-256"))))
//    	{
//    		messagecanvas.AddMessageToMessageObject(displayMsg,MESSAGE_TYPE_DEFAULT);
//    		msgHash.add(new String(simple_hash(m.getBytes(),"SHA-256")));
//    		msgTable.put(peer_GID, msgHash);
//    	}
    			
    }
    public boolean msgExistInHashtable(ArrayList msgHash,String msg){
    	for(int i=0; i<msgHash.size();i++){
    		if(msgHash.get(i).equals( msg))
    		{   //System.out.println("#### Plugin:Chat: EQUAL");
    			return true;
    		}
    			
    	}
    	return false;
    }
	/******** Function For Sending a Message To a Peer *************/
	private void SendMessage(String to, String m)
	{
		/********Sending a Message To Peer *********/
		//sendMessage(String message, String peerGID, D_Peer peer)
		 
		 Main.sendMessage(m, to, null);
//		 plugin_data.PluginRequest msg = new plugin_data.PluginRequest();
//		 msg.type=plugin_data.PluginRequest.MSG;
//		 msg.plugin_GID=(String)Main.getPluginDataHashtable().get("plugin_GID");
//		 Date time = new Date();
//		 Random rand = new Random();
//		 String msgID=rand.nextInt(100000)+""+ time.getTime()+"^";
//		 msg.msg=( msgID+UserName+": "+m).getBytes();// "hi khalid PC".getBytes();
//		 msg.peer_GID= UserRoomGID;
//		 Main.enqueue(msg);
		//SendMessageToServer("MESS "+UserRoom+"~"+UserName+": "+TxtMessage.getText());
		messagecanvas.AddMessageToMessageObject(null, null, null, UserName+": "+TxtMessage.getText(),MESSAGE_TYPE_DEFAULT);	
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
		
	}
	
	/***** Enable the Private Chat when the End User logged (in)****/
	private void EnablePrivateWindow(String ToUserName)
	{
//		for(G_ILoop = 0; G_ILoop < PrivateWindowCount; G_ILoop++)
//		{
//			if(privatewindow[G_ILoop].UserName.equals(ToUserName))
//			{
//				privatewindow[G_ILoop].messagecanvas.AddMessageToMessageObject(ToUserName + " is Currently Online!",MESSAGE_TYPE_ADMIN);	
//				privatewindow[G_ILoop].EnableAll();			
//				return;	
//			}
//		}	
	}
	
	/***** Disable the Private Chat when the End User logged out****/
	private void RemoveUserFromPrivateChat(String ToUserName)
	{
//		for(G_ILoop = 0; G_ILoop < PrivateWindowCount; G_ILoop++)
//		{
//			if(privatewindow[G_ILoop].UserName.equals(ToUserName))
//			{
//				privatewindow[G_ILoop].messagecanvas.AddMessageToMessageObject(ToUserName + " is Currently Offline!",MESSAGE_TYPE_ADMIN);	
//				privatewindow[G_ILoop].DisableAll();			
//				return;	
//			}
//		}	
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
//		if(tappanel.waitCanvas.SelectedUser.equals(""))
//		{
//			messagecanvas.AddMessageToMessageObject("Invalid Room Selection!",MESSAGE_TYPE_ADMIN);
//			return;	
//		}
//		
//		if(tappanel.waitCanvas.SelectedUser.equals(UserRoom))
//		{
//			messagecanvas.AddMessageToMessageObject("You are already in that ROOM!",MESSAGE_TYPE_ADMIN);
//			return;	
//		}
//		
//		SendMessageToServer("CHRO "+UserName+"~"+tappanel.waitCanvas.SelectedUser);
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
	}
	
	/***** Function To Disable All Components ********/
	private void DisableAll()
	{		
		TxtMessage.setEnabled(false);
		CmdSend.setEnabled(false);
		tappanel.enable(false);			
		hideItem.setEnabled(false);
		saveItem.setEnabled(true);
		
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
		hideItem.setEnabled(true);
		saveItem.setEnabled(false);
	}
	

	
	/*********Setting the AppletStatus********/
	private void SetAppStatus(String Message)
	{
//		if (messagecanvas != null)
//			messagecanvas.AddMessageToMessageObject(Message,MESSAGE_TYPE_ADMIN);		
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
	MenuItem saveItem;
	MenuItem hideItem;
	MenuItem seperatoritem;
	MenuItem quititem,aboutitem;
}
