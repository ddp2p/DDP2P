//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.math.BigInteger;
import java.awt.Dimension;
import java.awt.Canvas;
import java.util.ArrayList;
import java.awt.Graphics;
import java.awt.Event;
import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.util.StringTokenizer;
import java.util.Arrays;

public class MessageCanvas extends Canvas implements CommonSettings
{
	/**********Constructor Of Image Canvas *************/
	MessageCanvas(ChatPeer Parent)
	{
		chatPeer = Parent;		
		/***** Waiting for Loadind Chat	Client **********/
		dimension = chatPeer.getSize();		
		MessageArray = new ArrayList<MessageObject>();	
		MessageCount  = 0;			
		TotalWidth = 0;
		HorizantalSpace = 2;
		UserNameFont = chatPeer.getFont();		
		TextFont = chatPeer.TextFont;						
		setFont(chatPeer.getFont());		
		fontmetrics = chatPeer.getFontMetrics(chatPeer.getFont());		
	}
	
	/****** Function To Clear All the Item From ListArray *********/
	protected void ClearAll()
	{
		MessageArray.clear();
		TotalWidth = 0;
		TotalHeight = 0;
		MessageCount = 0;
		scrollview.setValues(TotalWidth,TotalHeight);	
	}
	
	
	protected void AddMessageToMessageObject( String PeerGID, byte[] session_id, BigInteger sequence, 
		                                      String Message, int MessageType)
	{	
		
		String m_Message="";
		tokenizer = new StringTokenizer(Message," ");		
		while(tokenizer.hasMoreTokens())
		{
				TokenString = tokenizer.nextToken();
				if(fontmetrics.stringWidth(m_Message+TokenString) < dimension.width)
				{				
					m_Message = m_Message + TokenString+" ";
				}
				else
				{
					AddMessage(PeerGID, session_id, sequence, m_Message,MessageType);
					m_Message="";	
				}
		}
			
		AddMessage(PeerGID, session_id, sequence, m_Message,MessageType);				
	}
	
	private void AddMessage(String PeerGID, byte[] session_id, BigInteger sequence, String Message,int MessageType)
	{			
			int m_startY = DEFAULT_MESSAGE_CANVAS_POSITION;	
			if(MessageArray.size() > 0)
			{
				messageobject = (MessageObject) MessageArray.get(MessageArray.size()-1);
				m_startY = 	messageobject.StartY + messageobject.Height;
			}
			
			messageobject = new MessageObject();
			messageobject.Message = Message;
			messageobject.StartY = m_startY;
			messageobject.MessageType = MessageType;
			messageobject.PeerGID = PeerGID;
			messageobject.session_id = session_id;
			messageobject.sequence = sequence;
			/*******Is Image True*********/
			if(Message.indexOf("~~") >= 0)
			{
				messageobject.IsImage = true;
				messageobject.Width = DEFAULT_ICON_WIDTH;
				messageobject.Height = DEFAULT_ICON_HEIGHT;
			}
			else
			{
				messageobject.IsImage = false;
				messageobject.Width = fontmetrics.stringWidth(Message);
				messageobject.Height =  fontmetrics.getHeight()+fontmetrics.getDescent();		
			}
			MessageArray.add(messageobject);
			MessageCount++;
			TotalWidth = Math.max(TotalWidth,messageobject.Width);
			TotalHeight = m_startY+messageobject.Height;
			scrollview.setValues(TotalWidth,TotalHeight);
		
			int m_Height = TotalHeight - YOffset;
			if(m_Height > dimension.height)
			{
				YOffset = TotalHeight - dimension.height;						
			}		
			scrollview.setScrollPos(2,2);					
			scrollview.setScrollSteps(2, 1, DEFAULT_SCROLLING_HEIGHT);
			repaint();		
	}
	
	public void confirmMsg(String PeerGID, byte[] session_id, BigInteger sequence, Color confColor){
		
		System.out.println("===========================");
	    System.out.println("= CHANGE COLOR confirmMsg =");
	    System.out.println("===========================");
		
		for(MessageObject mo : MessageArray){
			if(mo.PeerGID.equals(PeerGID) &&
			   Arrays.equals(mo.session_id, session_id) &&
			   mo.sequence.compareTo(sequence)==0)
			   { mo.msgColor = confColor;
			     System.out.println("=========================");
			     System.out.println("= CHANGE COLOR TO GREEN =");
			     System.out.println("=========================");
 			   }
		}
		repaint();
	}
	private void PaintFrame(Graphics graphics)
	{
		if(MessageCount < 1) return;
		int m_YPos = YOffset + dimension.height;
		int m_StartPos = 0;
		int m_listArraySize = MessageArray.size();		
		for(G_ILoop = 0; G_ILoop < MessageCount && m_StartPos < m_YPos; G_ILoop++)
		{			
			if(m_listArraySize < G_ILoop) return;
			messageobject = (MessageObject) MessageArray.get(G_ILoop);			
			if(messageobject.StartY >= YOffset)
			{				
				PaintMessageIntoCanvas(graphics,messageobject);	
				m_StartPos = messageobject.StartY;
			}
		}
		
		if(G_ILoop < MessageCount)
		{
			messageobject = (MessageObject) MessageArray.get(G_ILoop);
			PaintMessageIntoCanvas(graphics,messageobject);	
		}
	}
	
	private void PaintMessageIntoCanvas(Graphics graphics, MessageObject messageObject)
	{				
		graphics.setColor(messageObject.msgColor);
		int m_YPos = messageObject.StartY - YOffset;
		int m_XPos = 5 - XOffset;
		int CustomWidth = 0;
		String Message = messageObject.Message;
		/*************Print The User Name in UserName Font **************/		
		if(Message.indexOf(":") >= 0)
		{
			graphics.setFont(UserNameFont);		
			chatPeer.getGraphics().setFont(UserNameFont);
			fontmetrics = chatPeer.getGraphics().getFontMetrics();
			String m_UserName = Message.substring(0,Message.indexOf(":")+1);
			graphics.drawString(m_UserName,m_XPos+CustomWidth,m_YPos);			
			CustomWidth+=fontmetrics.stringWidth(m_UserName)+HorizantalSpace;
			Message = Message.substring(Message.indexOf(":")+1);
		}
		/*********Set the Text Font **********/
		chatPeer.getGraphics().setFont(TextFont);
		graphics.setFont(TextFont);		
		fontmetrics =  chatPeer.getGraphics().getFontMetrics();
		
		/**********Print Image Area********/
		if(messageObject.IsImage == true)
		{
			tokenizer = new StringTokenizer(Message," ");	
			while(tokenizer.hasMoreTokens())
			{
				TokenString = tokenizer.nextToken();				
				if(TokenString.indexOf("~~") >= 0)
				{
					/********If its a Proper Image*************/
					try {
						int m_ImageIndex = Integer.parseInt(TokenString.substring(2));
						if((m_ImageIndex >= 0) && (m_ImageIndex < chatPeer.IconCount))
						{						
								graphics.drawImage(chatPeer.IconArray[m_ImageIndex],m_XPos+CustomWidth,m_YPos - 15,messageObject.Width,messageObject.Height,this);
								CustomWidth+=messageObject.Width+HorizantalSpace;
							
						}	
					}catch(Exception _Exc) { }				
				}
				else
				{					
					graphics.drawString(TokenString,m_XPos+CustomWidth,m_YPos);
					CustomWidth+=fontmetrics.stringWidth(TokenString)+HorizantalSpace;
				}
				if(TotalWidth < m_XPos+CustomWidth)
				{
					TotalWidth = m_XPos+CustomWidth;
					scrollview.setValues(TotalWidth,TotalHeight);
				}
			}			
					
		}
		/****Not a Image********/
		else		
		{		
		
			switch (messageObject.MessageType)
			{
				case MESSAGE_TYPE_DEFAULT:
					{
						graphics.setColor(messageObject.msgColor/*Color.black*/);
						break;	
					}	
				case MESSAGE_TYPE_JOIN:
					{
						graphics.setColor(Color.blue);
						break;	
					}	
				case MESSAGE_TYPE_LEAVE:
					{
						graphics.setColor(Color.red);
						break;	
					}	
				case MESSAGE_TYPE_ADMIN:
					{
						graphics.setColor(Color.gray);
						break;	
					}	
			}			
			graphics.drawString(Message,m_XPos+CustomWidth,m_YPos);
		}	
		
		graphics.setFont(UserNameFont);	
		chatPeer.getGraphics().setFont(UserNameFont);
		fontmetrics = chatPeer.getGraphics().getFontMetrics();				
	}
	
	public boolean handleEvent(Event event)
    {
        if(event.id == 1001 && event.arg == scrollview)
        {        
            if(event.modifiers == 1)
                XOffset = event.key;
            else
                YOffset = event.key;                                  
            repaint();
            return true;
        } 
        else
        {           	     	
            return super.handleEvent(event);
        }			
    }    
   
    public boolean 	mouseDown(Event event, int i, int j)
	{		
		return true;
	}
	
	public void paint(Graphics graphics)
	{			
		/*************Double Buffering**************/		
		dimension = size();

		/*********** Create the offscreen graphics context**************/
		if ((offGraphics == null) || (dimension.width != offDimension.width)|| (dimension.height != offDimension.height)) 
		{						
	    	offDimension = dimension;
	    	offImage = createImage(dimension.width, dimension.height);
	    	offGraphics = offImage.getGraphics();	    		    		    		    	
		}

		/********* Erase the previous image*********/
		offGraphics.setColor(Color.white);
		offGraphics.fillRect(0, 0, dimension.width, dimension.height);	

		/*************** Paint the frame into the image*****************/
		PaintFrame(offGraphics);

		/****************** Paint the image onto the screen*************/
		graphics.drawImage(offImage, 0, 0, null);						
	}

	public void update(Graphics graphics)	
	{
		paint(graphics);
	}
	
	/***********Global Variable Declarations****************/
	Dimension offDimension,dimension;
    Image offImage;
    Graphics offGraphics;  	
    ChatPeer chatPeer;    
    ArrayList<MessageObject> MessageArray;
    int G_ILoop,XOffset,YOffset,HorizantalSpace;
    MessageObject messageobject;
    ScrollView scrollview;   
    FontMetrics fontmetrics; 
    int TotalWidth,MessageCount,TotalHeight;
    Font UserNameFont,TextFont;
	StringTokenizer tokenizer;    
	String TokenString;
}