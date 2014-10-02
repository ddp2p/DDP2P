//package com.jeeva.chatclient;
package dd_p2p.plugin;

import java.awt.Color;
import java.math.BigInteger;

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
        msgColor = Color.black;        
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
    Color msgColor;
    int MessageType;
    String PeerGID;
    byte[] session_id;
    BigInteger sequence;    
}