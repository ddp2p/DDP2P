package widgets.components;

import static util.Util.__;
import hds.PeerInput;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import config.Application_GUI;
import config.DD;

import javax.swing.tree.TreePath;

import config.Vendor_GUI_Dialogs;
import data.D_Constituent;
import data.D_Constituent;
import data.D_Organization;
import data.D_Peer;
import util.DDP2P_ServiceRunnable;
import util.P2PDDSQLException;
import widgets.app.ControlPane;
import widgets.app.JFrameDropCatch;
import widgets.app.MainFrame;
import widgets.app.ThreadsAccounting;
import widgets.app.Util_GUI;
import widgets.constituent.ConstituentsPanel;
import widgets.constituent.ConstituentsWitness;
import widgets.org.Orgs;
import widgets.peers.CreatePeer;
import widgets.peers.PeerContacts;
import widgets.updates.UpdatesPanel;

public class GUI_Swing implements Vendor_GUI_Dialogs {

	//public static RegistrationServer rs; // Song's server
	public static ControlPane controlPane;
	public static ConstituentsPanel constituents;
	public static Orgs orgs;
	public static UpdatesPanel panelUpdates;
	public static PeerContacts peer_contacts;

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
		switch (q) {
		case 0:
		{
			final JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = fc.showOpenDialog(MainFrame.frame);
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
		break;
		case 1:
		{
			String version = null;
			final JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnVal = fc.showOpenDialog(MainFrame.frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fc.getSelectedFile();
	            try {
					version = file.getCanonicalPath();
					tools.Directories.setCrtPathsInDB(version);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
	        } else {
	        }
		}
		break;
		case 2:	break;
		case 3:  System.exit(-1); break;
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
		chooser.setFileFilter(new widgets.components.DatabaseFilter());
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
		if (ctx != null) EventQueue.invokeLater(new widgets.app.RunnableCmd(string, ctx));
	}

	@Override
	public void eventQueue_invokeLater(Runnable rp) {
		EventQueue.invokeLater(rp);	
	}

	@Override
	public void clientUpdates_Start() {
		if(MainFrame.controlPane!=null) MainFrame.controlPane.startClientUpdates.setText(ControlPane.START_CLIENT_UPDATES);
	}

	@Override
	public void clientUpdates_Stop() {
		if(MainFrame.controlPane!=null) MainFrame.controlPane.startClientUpdates.setText(ControlPane.STOP_CLIENT_UPDATES);
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
		Application_GUI.eventQueue_invokeLater(new DDP2P_ServiceRunnable("BroadcastClient:setText1", true, msg_cnter){
			public void _run() {
				if (ctx != null)
					MainFrame.client_sockets_cntr.setText(""+ctx);
				else
					MainFrame.client_sockets.setText(""+wireless.BroadcastClient.broadcast_client_sockets.length);
				String t = "";
				for(int i = 0; i<wireless.BroadcastClient.broadcast_client_sockets.length; i++) {
					if(i>0) t+= " ;; ";
					t += wireless.BroadcastClient.broadcast_client_sockets[i].getLocalSocketAddress();
				}
				MainFrame.client_sockets_val.setText(t);
			}
		});
	}

	@Override
	public void inform_arrival(Object obj, D_Peer source) {
		// TODO Auto-generated method stub
		String from = "";
		if (source != null) from = source.getName()+" <"+source.getEmail()+">";
		System.out.println("GUI_Swing: inform_arrival: from "+from+" new\n"+obj);
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
