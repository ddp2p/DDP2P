//package com.jeeva.chatclient;
package chatApp;	

import java.awt.Button;
class CustomButton extends Button
{
	public CustomButton(ChatPeer Parent, String label)
	{
		chatPeer = Parent;
		setLabel(label);
		setBackground(chatPeer.ColorMap[3]);
	    setForeground(chatPeer.ColorMap[2]);		
	}
ChatPeer chatPeer;
}