
package dd_p2p.plugin;

import java.awt.Dialog;
import java.awt.TextField;
import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.GridLayout;
import java.awt.Button;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.TextArea;

public class InviteFriend extends Dialog implements ActionListener,CommonSettings
{
	InviteFriend(ChatPeer Parent)
	{		
		super(Parent,ChatApp_NAME+" - Login",true);
		chatPeer = Parent;				
		setFont(chatPeer.TextFont);				
		setLayout(new BorderLayout());
		
		addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {setVisible(false);}});
		
		Panel ButtonPanel = new Panel(new GridLayout(3,1,15,30));				
		ButtonPanel.setBackground(chatPeer.ColorMap[3]);
		
		Label emailNameLbl = new Label("Email : ");
		emailNameTxt = new TextField();		
		//ButtonPanel.add(emailNameLbl);
		//ButtonPanel.add(emailNameTxt);
		
		Panel emailPanel = new Panel();
		emailPanel.add(emailNameLbl);
		emailPanel.add(emailNameTxt);
		ButtonPanel.add(emailPanel);
		
		
		String defaultMessage=chatPeer.UserName+" Invite you to start a chat with him. Please add the attachment image in your peers";
		Label messageLbl = new Label("Content: ");
		message=new TextArea(defaultMessage);	
		//ButtonPanel.add(messageLbl);
		//ButtonPanel.add(message);
		Panel messagePanel = new Panel();
		messagePanel.add(messageLbl);
		messagePanel.add(message);
		ButtonPanel.add(messagePanel);
		
		CmdOk = new Button("Send");
		CmdOk.addActionListener(this);
		CmdCancel = new Button("Cancel");
		CmdCancel.addActionListener(this);
		ButtonPanel.add(CmdOk);
		//ButtonPanel.add(CmdCancel);
		
		message.setColumns(20);
		//message.setLineWrap(true);
		message.setRows(5);
	//	message.setWrapStyleWord(true); 
		message.setEditable(false);
		//jScrollPane1 = new JScrollPane(message);   
		
		add("Center",ButtonPanel);
		
		/*Panel EmptyNorthPanel = new Panel();
		EmptyNorthPanel.setBackground(chatPeer.ColorMap[3]);
		add("North",EmptyNorthPanel);
		
		Panel EmptySouthPanel = new Panel();
		EmptySouthPanel.setBackground(chatPeer.ColorMap[3]);
		add("South",EmptySouthPanel);
		
		Panel EmptyEastPanel = new Panel();
		EmptyEastPanel.setBackground(chatPeer.ColorMap[3]);
		add("East",EmptyEastPanel);
		
		Panel EmptyWestPanel = new Panel();
		EmptyWestPanel.setBackground(chatPeer.ColorMap[3]);
		add("West",EmptyWestPanel);*/
		
		setSize(200,200);
		setVisible(true);				
	}	
	
	/******** Action Event Coding Starts **************/
	public void actionPerformed(ActionEvent evt)
	{
		if (evt.getSource().equals(CmdOk))
		{
			
			dispose();
		}	
		
		if (evt.getSource().equals(CmdCancel))
		{
			
			dispose();
		}	
	}
	
	/********* Global Variable Declarations **********/
	ChatPeer chatPeer;
	protected TextField emailNameTxt;
	protected TextArea message;
	protected Button CmdOk,CmdCancel;
	
}