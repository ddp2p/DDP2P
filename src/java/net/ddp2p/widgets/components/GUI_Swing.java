package net.ddp2p.widgets.components;

import static net.ddp2p.common.util.Util.__;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.tree.TreePath;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;


import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Vendor_GUI_Dialogs;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Justification;
import net.ddp2p.common.data.D_Motion;
import net.ddp2p.common.data.D_Neighborhood;
import net.ddp2p.common.data.D_News;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.data.D_Peer;
import net.ddp2p.common.data.D_Vote;
import net.ddp2p.common.data.D_Witness;
import net.ddp2p.common.hds.PeerInput;
import net.ddp2p.common.util.DDP2P_ServiceRunnable;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.widgets.app.ControlPane;
import net.ddp2p.widgets.app.JFrameDropCatch;
import net.ddp2p.widgets.app.MainFrame;
import net.ddp2p.widgets.app.ThreadsAccounting;
import net.ddp2p.widgets.app.Util_GUI;
import net.ddp2p.widgets.constituent.ConstituentsPanel;
import net.ddp2p.widgets.constituent.ConstituentsWitness;
import net.ddp2p.widgets.org.Orgs;
import net.ddp2p.widgets.peers.CreatePeer;
import net.ddp2p.widgets.peers.DataHashesPanel;
import net.ddp2p.widgets.peers.PeerContacts;
import net.ddp2p.widgets.updates.UpdatesPanel;
import tools.Directories;

public class GUI_Swing implements Vendor_GUI_Dialogs {

	public static boolean DEBUG = true;
	//public static RegistrationServer rs; // Song's server
	public static ControlPane controlPane;
	public static ConstituentsPanel constituents;
	public static Orgs orgs;
	public static UpdatesPanel panelUpdates;
	public static PeerContacts peer_contacts;
	public static DataHashesPanel dataHashesPanel;

	@Override
	public void fixScriptsBaseDir(String dir) {
		Object options[] = new Object[]{__("Browse Scripts Folder")
				,__("Browse Base Folder")
				,__("Leave me alone!")
				,__("Exit Program")
				};
		int q = Application_GUI.ask(__("Wrong scripts path!"), 
				__("Setting wrong path for scripts:")+"\""+dir+"\n"+ 
				__("You can change the path from the control panel of the app, or may move scripts there.")+
						"\n"+__("Do you want to change now the scripts path?"),
				options, options[0], null);
		if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: opt="+q+" dir="+dir);
		switch (q) {
		case 0:
		{
			if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: scripts opt="+q);

			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable("Scripts Folder", false, false, dir) {
				@Override
				public void _run() {
					String dir = (String) this.ctx;
					final JFileChooser fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: scripts browse");
					int returnVal = fc.showOpenDialog(MainFrame.frame);
					if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: scripts val =" + returnVal);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            try {
							dir = file.getCanonicalPath();
							switch (DD.OS) {
							case DD.LINUX:
							case DD.MAC:
								DD.setAppText(DD.APP_LINUX_SCRIPTS_PATH, dir);
								break;
							case DD.WINDOWS:
								DD.setAppText(DD.APP_WINDOWS_SCRIPTS_PATH, dir);
							}
						} catch (IOException e) {
							e.printStackTrace();
						} catch (P2PDDSQLException e) {
							e.printStackTrace();
						}
			        } else {
			        }
				}
			});
		}
		break;
		case 1:
		{
			if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: base opt="+q);
			//if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: base browse "+MainFrame.frame);
			//Util.printCallPath("");

			SwingUtilities.invokeLater(new net.ddp2p.common.util.DDP2P_ServiceRunnable("Scripts Folder", false, false, null) {
				@Override
				public void _run() {
					String version = null;
					final JFileChooser fc = new JFileChooser();
					fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: base browse");
					int returnVal = fc.showOpenDialog(MainFrame.frame);
					if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: base val =" + returnVal);
					if (returnVal == JFileChooser.APPROVE_OPTION) {
			            File file = fc.getSelectedFile();
			            try {
							version = file.getCanonicalPath();
							Directories.setCrtPathsInDB(version);
							
						} catch (IOException e) {
							e.printStackTrace();
						}
			        } else {
			        }
					
				}});
		}
		break;
		case 2:
			if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: alone opt="+q);
			break;
		case 3:
			if (DEBUG) System.out.println("GUI_Swing: fixScriptsBaseDir: exiting opt="+q);
			System.exit(-1); break;
		}
	}

	@Override
	public void warning(String war, String title) {
		JOptionPane.showMessageDialog(JFrameDropCatch.mframe,
				war,
				title, JOptionPane.WARNING_MESSAGE);
	}

	@Override
	public void info(String inf, String title) {
		JOptionPane.showMessageDialog(JFrameDropCatch.mframe,
				inf,
				title, JOptionPane.INFORMATION_MESSAGE);
	}

	@Override
	public int ask(String war, String title, int type) {
		return
			JOptionPane.showConfirmDialog(JFrameDropCatch.mframe,
				war,
				title, type, JOptionPane.QUESTION_MESSAGE);
	}

	@Override
	public int ask(String title, String war, Object[] options,
			Object def_option, Object _icon) {
		Icon icon = (Icon) _icon;
		return
//	JOptionPane.showConfirmDialog(JFrameDropCatch.mframe, war,
//		title, type, JOptionPane.QUESTION_MESSAGE, options);
		JOptionPane.showOptionDialog(JFrameDropCatch.mframe, war, title, 
		        JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, //JOptionPane.PLAIN_MESSAGE,
		        icon, options, options[0]);
	}

	@Override
	public String input(String prompt, String title, int type) {
		String val=
			JOptionPane.showInputDialog(JFrameDropCatch.mframe,
							prompt, title,
					type);
		return val;
	}

	@Override
	public void setBroadcastServerStatus_GUI(boolean run) {
		MainFrame.setBroadcastServerStatus_GUI(run);
	}

	@Override
	public void setBroadcastClientStatus_GUI(boolean run) {
		MainFrame.setBroadcastClientStatus_GUI(run);
	}

	@Override
	public void setSimulatorStatus_GUI(boolean run) {
		MainFrame.setSimulatorStatus_GUI(run);
	}

	@Override
	public String html2text(String document) {
	    Html2Text parser = new Html2Text();
	    try {
	    	parser.parse(new StringReader(document));
	    } catch(IOException e){
	    	System.err.println(e);
	    }
	    document = parser.getText();
	    
		return null;
	}

	@Override
	public String[] getWitnessScores() {

    	TreePath tp = null;
		ConstituentsWitness dialog = new ConstituentsWitness(GUI_Swing.constituents.tree, tp, 0);
    	if (!dialog.accepted) return null;
    	String witness_category = Util_GUI.getJFieldText(dialog.witness_category);
    	String witness_category_trustworthiness = Util_GUI.getJFieldText(dialog.witness_category_trustworthiness);
		return new String[]{witness_category, witness_category_trustworthiness};
	}

	@Override
	public D_Constituent getMeConstituent() {
		return MainFrame.status.getMeConstituent();
	}

	/**
	 * Sets the functions Calling Swing dialogs
	 */
	public static void initSwingGUI() {
		Application_GUI.gui = new GUI_Swing();
	}
	@Override
	public void registerThread() {
		ThreadsAccounting.registerThread();
	}

	@Override
	public void unregisterThread() {
		ThreadsAccounting.unregisterThread();
	}

	@Override
	public String queryDatabaseFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new net.ddp2p.widgets.components.DatabaseFilter());
		chooser.setName(__("Select database to import from"));
		chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		File fileDB = null;

		int returnVal = chooser.showDialog(MainFrame.frame,__("Specify Database"));
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileDB = chooser.getSelectedFile();
			String error;
			try {
				error = DD.testProperDB(fileDB.getCanonicalPath());
				if (error == null){
					String db_to_import = fileDB.getAbsolutePath();
					return db_to_import;
				}
				Application_GUI.warning(__("Failure to open:")+fileDB+"\nError: "+error, __("Failure to open!"));
			} catch (IOException e) {
				e.printStackTrace();
				Application_GUI.warning(__("Failure to open:")+fileDB+"\nException: "+e.getLocalizedMessage(), __("Failure to open!"));
			}
		}
		return null;
	}

	@Override
	public void updateProgress(Object ctx, String string) {
		if (ctx == null) ctx = Util_GUI.crtProcessLabel;
		if (ctx != null) EventQueue.invokeLater(new net.ddp2p.widgets.app.RunnableCmd(string, ctx));
	}

	@Override
	public void eventQueue_invokeLater(Runnable rp) {
		EventQueue.invokeLater(rp);	
	}

	@Override
	public void clientUpdates_Start() {
		if (MainFrame.controlPane != null) MainFrame.controlPane.startClientUpdates.setText(ControlPane.START_CLIENT_UPDATES);
	}

	@Override
	public void clientUpdates_Stop() {
		if (MainFrame.controlPane != null) MainFrame.controlPane.startClientUpdates.setText(ControlPane.STOP_CLIENT_UPDATES);
	}

	@Override
	public void ThreadsAccounting_ping(String string) {
		ThreadsAccounting.ping(string);
	}

	@Override
	public void setClientUpdatesStatus(boolean b) {
		if(MainFrame.controlPane!=null) MainFrame.controlPane.setClientUpdatesStatus(false);
	}

	@Override
	public void peer_contacts_update() {
		if (GUI_Swing.peer_contacts != null) GUI_Swing.peer_contacts.update(D_Peer.peer_contacts);
	}

	@Override
	public boolean is_crt_peer(D_Peer candidate) {
		if (MainFrame.status == null) return false;
		return MainFrame.status.is_crt_peer(candidate);
	}

	@Override
	public boolean is_crt_org(D_Organization candidate) {
		if (MainFrame.status == null) return false;
		return MainFrame.status.is_crt_org(candidate);
	}
	
	@Override
	public boolean is_crt_const(D_Constituent candidate) {
		if (MainFrame.status == null) return false;
		return MainFrame.status.is_crt_const(candidate);
	}

	/**
	 * Set this as me in the GUI status and displays
	 */
	@Override
	public void setMePeer(D_Peer me) {
		if (MainFrame.status != null) MainFrame.status.setMePeer(me);
	}
	
	/**
	 * Query the user whether the new peer should have the attributes in pi, or other ones.
	 * The final ones are set in _data.
	 */

	@Override
	public D_Peer createPeer(PeerInput pi, PeerInput[] _data) {
		//
		CreatePeer dialog = new CreatePeer(MainFrame.frame, pi);
		PeerInput data = dialog.getData();
		if ( ! data.valid ) {
			System.out.println("createPeer: exit since data is not valid");
			//System.exit(-1);
			return null;
		}
		D_Peer peer = dialog.getDPeer();
		_data[0] = data;
		return peer;
	}

	@Override
	public void update_broadcast_client_sockets(Long msg_cnter) {
		Application_GUI.eventQueue_invokeLater(new DDP2P_ServiceRunnable("BroadcastClient:setText1", false, false, msg_cnter){
			// may be set daemon
			public void _run() {
				if (ctx != null)
					MainFrame.client_sockets_cntr.setText(""+ctx);
				else
					MainFrame.client_sockets.setText(""+net.ddp2p.common.wireless.BroadcastClient.broadcast_client_sockets.length);
				String t = "";
				for(int i = 0; i<net.ddp2p.common.wireless.BroadcastClient.broadcast_client_sockets.length; i++) {
					if(i>0) t+= " ;; ";
					t += net.ddp2p.common.wireless.BroadcastClient.broadcast_client_sockets[i].getLocalSocketAddress();
				}
				MainFrame.client_sockets_val.setText(t);
			}
		});
	}

	@Override
	public void inform_arrival(Object obj, D_Peer source) {
		// TODO Auto-generated method stub
		if (obj == null) {
			String from = "";
			if (source != null) from = source.getName()+" <"+source.getEmail()+">";
			System.out.println("GUI_Swing: inform_arrival: from "+from+" new\n"+obj);			
		}
		
		if (obj instanceof D_Peer) inform_arrival_peer((D_Peer) obj, source);
		else if (obj instanceof D_Organization) inform_arrival_org((D_Organization) obj, source);
		else if (obj instanceof D_Constituent) inform_arrival_cons((D_Constituent) obj, source);
		else if (obj instanceof D_Neighborhood) inform_arrival_neigh((D_Neighborhood) obj, source);
		else if (obj instanceof D_Witness) inform_arrival_witn((D_Witness) obj, source);
		else if (obj instanceof D_Motion) inform_arrival_moti((D_Motion) obj, source);
		else if (obj instanceof D_Justification) inform_arrival_just((D_Justification) obj, source);
		else if (obj instanceof D_Vote) inform_arrival_vote((D_Vote) obj, source);
		else if (obj instanceof D_News) inform_arrival_news((D_News) obj, source);
		else  {
			String from = "";
			if (source != null) from = source.getName()+" <"+source.getEmail()+">";
			System.out.println("GUI_Swing: inform_arrival: from "+from+" new\n"+obj);
		}
	}
	public void inform_arrival_peer(D_Peer obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:P GIDH="+obj.getGIDH()+" tit="+obj.getName_MyOrDefault());		
	}
	public void inform_arrival_org(D_Organization obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:O GIDH="+obj.getGIDH()+" tit="+obj.getOrgNameOrMy());	
	}
	public void inform_arrival_cons(D_Constituent obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:C GIDH="+obj.getGIDH()+" tit="+obj.getNameOrMy());
	}
	public void inform_arrival_neigh(D_Neighborhood obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:N "+obj.getName());		
	}
	public void inform_arrival_witn(D_Witness obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:W "+obj.global_witness_ID+" t="+obj.witnessed_constituentID+" s:"+obj.witnessing_constituentID);		
	}
	public void inform_arrival_moti(D_Motion obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:M GIDH="+obj.getGIDH()+" tit="+obj.getTitleStrOrMy());		
	}
	public void inform_arrival_just(D_Justification obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:J GIDH="+obj.getGIDH()+" tit="+obj.getTitleStrOrMy());		
	}
	public void inform_arrival_vote(D_Vote obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:V "+obj.getGID()+" m:"+obj.getMotionLID()+" c:"+obj.getConstituentLIDstr()+" j:"+obj.getJustificationLID()+":"+obj.getJustificationGID());		
	}
	public void inform_arrival_news(D_News obj, D_Peer source) {
		System.out.println("GUI_Swing: IAP:news GIDH="+obj.global_news_ID+" tit="+obj.getTitle());
	}

	long lastPlayThanks;
	@Override
	public boolean playThanks(){
		if (Application.CURRENT_SCRIPTS_BASE_DIR() == null) return false;
		long crt = Util.CalendargetInstance().getTimeInMillis();
		if ((crt - lastPlayThanks) < 10000) return false;
		lastPlayThanks =  Util.CalendargetInstance().getTimeInMillis();
		
		String wav_file = Application.CURRENT_SCRIPTS_BASE_DIR()+Application.WIRELESS_THANKS;
		int EXTERNAL_BUFFER_SIZE = 524288;
		File soundFile = new File(wav_file);
		AudioInputStream audioInputStream = null;
		try
		{audioInputStream = AudioSystem.getAudioInputStream(soundFile);}
		catch(Exception e) {e.printStackTrace();}

		AudioFormat format = audioInputStream.getFormat();
		SourceDataLine auline = null;
		//Describe a desired line
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		try
		{
			auline = (SourceDataLine) AudioSystem.getLine(info);
			//Opens the line with the specified format,
			//causing the line to acquire any required
			//system resources and become operational.
			auline.open(format);
		}
		catch(Exception e) {e.printStackTrace();}

		//Allows a line to engage in data I/O
		auline.start();
		int nBytesRead = 0;
		byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];
		try {
			while (nBytesRead != -1)
			{
				nBytesRead = audioInputStream.read(abData, 0, abData.length);
				if (nBytesRead >= 0)
				{
					//Writes audio data to the mixer via this source data line
					//NOTE : A mixer is an audio device with one or more lines
					auline.write(abData, 0, nBytesRead);
				}
			}
		}
		catch(Exception e) {e.printStackTrace();}
		finally
		{
			//Drains queued data from the line
			//by continuing data I/O until the
			//data line's internal buffer has been emptied
			auline.drain();
			//Closes the line, indicating that any system
			//resources in use by the line can be released
			auline.close();
		}		
		return true;
	}
}
class Html2Text extends HTMLEditorKit.ParserCallback {
 StringBuffer s;

 public Html2Text() {}

 public void parse(Reader in) throws IOException {
   s = new StringBuffer();
   ParserDelegator delegator = new ParserDelegator();
   // the third parameter is TRUE to ignore charset directive
   delegator.parse(in, this, Boolean.TRUE);
 }

 public void handleText(char[] text, int pos) {
	 //System.out.println("pos: "+pos);
	 s.append(" ");
	 s.append(text);
 }

 public String getText() {
   return s.toString();
 }
}
