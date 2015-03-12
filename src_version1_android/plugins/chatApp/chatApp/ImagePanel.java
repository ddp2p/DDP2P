/*************************************************************************/
/*************************************************************************/
/*************************************************************************/
/*****************Chat Client Customized Panel****************************/
/*************************************************************************/
/*************************************************************************/
/*************************************************************************/
//package com.jeeva.chatclient;
package chatApp;

import java.awt.Panel;
import java.awt.Image;
import java.awt.Graphics;
public class ImagePanel extends Panel implements CommonSettings
{
	ImagePanel(ChatPeer chatPeer, Image image)
	{
		setLayout(null);
		Parent = chatPeer;		
		DisplayImage = image;
		int XPos = image.getWidth(this);
		int YPos = image.getHeight(this);
		setBounds(0,0,XPos+TOP_PANEL_START_POS,YPos+TOP_PANEL_START_POS);				
	}
	
	public void paint(Graphics graphics)
	{
		graphics.drawImage(DisplayImage,TOP_PANEL_START_POS,TOP_PANEL_START_POS,this);	
	}
	
	/*************Global Variable Declarations*********/
	ChatPeer Parent;		
	Image DisplayImage;
}