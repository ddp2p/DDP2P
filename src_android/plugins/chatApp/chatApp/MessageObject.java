//package com.jeeva.chatclient;
package chatApp;

public class MessageObject
{
	/*********Constructor ************/
    MessageObject()
    {
        Width   = 0;
        Height  = 0;
        StartX  = 0;
        StartY  = 0;
        Message = null;  
        IsImage = false;   
        Selected = false;   
        IsIgnored = false;        
    }
    
    /*********Global Variable Declarations**********/	
    String Message;
    int StartX;
    int StartY;
    int Width;
    int Height;       
    boolean IsImage; 
    boolean Selected;
    boolean IsIgnored;
    int MessageType;
    String PeerGID;    
}