/*************************************************************************/
/*************************************************************************/
/*************************************************************************/
/*****************Chat Client Coomon Settings*****************************/
/*************************************************************************/
/*************************************************************************/
/*************************************************************************/

//package com.jeeva.chatclient;
package dd_p2p.plugin;	

public interface CommonSettings
{   public int DEFAULT_FRAME_WIDTH			=	778;
    public int DEFAULT_FRAME_HEIGHT			=	660;
	public int MAX_COLOR					=	8;
	public int TOP_PANEL_START_POS			=	0;//10
	public int DEFAULT_ICON_WIDTH			=	30;
	public int DEFAULT_ICON_HEIGHT			=	30;
	public int TAPPANEL_WIDTH				= 	225;
	public int TAPPANEL_HEIGHT				= 	350;
	public int TAPPANEL_CANVAS_WIDTH		= 	120;
	public int TAPPANEL_CANVAS_HEIGHT		= 	260;
	public int TAP_COUNT					=	3; //change
	public int IMAGE_CANVAS_START_POSITION	=	10;
	public String ICON_NAME					=	"PHOTO";
	public int DEFAULT_IMAGE_CANVAS_SPACE	=	35;
	public int DEFAULT_LIST_CANVAS_POSITION	=	0;
	public int DEFAULT_LIST_CANVAS_INCREMENT=	20;
	public int DEFAULT_LIST_CANVAS_HEIGHT	=	15;
	public int SCROLL_BAR_SIZE				= 	15;
	public int USER_CANVAS					=	0;
	public int ROOM_CANVAS					=	1;
	public int USER_CANVAS_NORMAL_ICON		= 	11;
	public int USER_CANVAS_IGNORE_ICON		= 	10;
	public int ROOM_CANVAS_ICON				= 	13;
	public int DEFAULT_MESSAGE_CANVAS_POSITION=	25;
	public int DEFAULT_SCROLLING_HEIGHT		=	20;
	
	public int MESSAGE_TYPE_DEFAULT				= 	0;
	public int MESSAGE_TYPE_JOIN				= 	1;
	public int MESSAGE_TYPE_LEAVE				= 	2;
	public int MESSAGE_TYPE_ADMIN				= 	3;
	public int MESSAGE_TYPE_INFO				= 	4;
	
	public int QUIT_TYPE_DEFAULT				=	0;
	public int QUIT_TYPE_KICK					=	1;
	public int QUIT_TYPE_NULL					=	2;
	
	public int PRIVATE_WINDOW_WIDTH				=	415;
	public int PRIVATE_WINDOW_HEIGHT			=	350;
	
	public int EMOTION_CANVAS_WIDTH				=	400;
	public int EMOTION_CANVAS_HEIGHT			=	270;
	
	public int MAX_PRIVATE_WINDOW				=	40;
	public String ChatApp_NAME					= "P2PChat v1.0";
	public String COMPANY_NAME					= "Peer to Peer Class";
}