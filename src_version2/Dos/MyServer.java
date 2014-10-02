package Dos;


public class MyServer
{
 public static MyServer server;
 
 // IH private MySendingThread mySendingThread;
 private MySending1 mySendingThread;
 //private MyReceivingThread myReceivingThread;
 
// public static String broadcast = "255.255.255.255";
 public static String broadcast = "127.0.0.1";

 static String db="deliberation-app.db";

 public static void main(String[] args){
	 try{
		 _main(args);
	 }catch(Exception e){e.printStackTrace();}
 }
 public static void _main(String[] args){
	 if(args.length != 0)
		db = args[0];
	 if(args.length > 1)
		broadcast = args[1];
      server = new MyServer();
 }
 
 public MyServer()
 {
    //IH  mySendingThread = new MySendingThread(broadcast);
	  mySendingThread = new MySending1(broadcast);
      mySendingThread.start();
      //myReceivingThread = new MyReceivingThread(broadcast);
      //myReceivingThread.start();
 }
}
