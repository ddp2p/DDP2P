/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
		Florida Tech, Human Decision Support Systems Laboratory
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
package net.ddp2p.widgets.app;
import static net.ddp2p.common.util.Util.__;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.Language;
import net.ddp2p.common.config.MotionsListener;
import net.ddp2p.common.data.DDTranslation;
import net.ddp2p.common.data.D_Constituent;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.hds.GUIStatusHistory;
import net.ddp2p.common.hds.RPC;
import net.ddp2p.common.hds.StartUp;
import net.ddp2p.common.streaming.UpdateMessages;
import net.ddp2p.common.table.identity;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.DB_Import;
import net.ddp2p.common.util.GetOpt;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;
import net.ddp2p.common.wireless.BroadcastClient;
import net.ddp2p.common.wireless.BroadcastConsummerBuffer;
import net.ddp2p.widgets.census.CensusPanel;
import net.ddp2p.widgets.components.DatabaseFilter;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.constituent.ConstituentsPanel;
import net.ddp2p.widgets.directories.Directories;
import net.ddp2p.widgets.identities.MyIdentitiesTest;
import net.ddp2p.widgets.instance.Instances;
import net.ddp2p.widgets.justifications.JustificationEditor;
import net.ddp2p.widgets.justifications.JustificationViewer;
import net.ddp2p.widgets.justifications.Justifications;
import net.ddp2p.widgets.justifications.JustificationsByChoicePanel;
import net.ddp2p.widgets.keys.Keys;
import net.ddp2p.widgets.motions.MotionEditor;
import net.ddp2p.widgets.motions.Motions;
import net.ddp2p.widgets.news.NewsEditor;
import net.ddp2p.widgets.news.NewsTable;
import net.ddp2p.widgets.org.OrgEditor;
import net.ddp2p.widgets.org.Orgs;
import net.ddp2p.widgets.peers.OrganizationRequests;
import net.ddp2p.widgets.peers.PeerAddresses;
import net.ddp2p.widgets.peers.PeerInstanceContacts;
import net.ddp2p.widgets.peers.Peers;
import net.ddp2p.widgets.wireless.WLAN_widget;
public class MainFrame {
	static String TAB_SETTINGS = __("Settings");
	static int TAB_SETTINGS_ = 0;
	static final JPanel JunkControlPane = new JPanel();
	static String TAB_KEYS = __("Keys");
	static int TAB_KEYS_ = 1;
	static final JPanel JunkPanelKeys = new JPanel();
	static String TAB_ID = __("Identities");
	static int TAB_ID_ = 2;
	static String TAB_CONS = __("Constituents");
	static int TAB_CONS_ = 3;
	static String TAB_DIRS = __("Address Books");
	static int TAB_DIRS_ = 5;
	static String TAB_PEERS = __("Safes");
	static int TAB_PEERS_ = 6;
	static final JPanel JunkPanelPeers = new JPanel();
	static String TAB_ORGS = __("Organizations");
	static int TAB_ORGS_ = 7;
	static final JPanel JunkPanelOrgs = new JPanel();
	public static String TAB_MOTS = __("Motions");
	public static int TAB_MOTS_ = 8;
	public static final JPanel JunkPanelMots = new JPanel();
	static String TAB_JUSTS = __("Justifications");
	public static int TAB_JUSTS_ = 9;
	static final JPanel JunkPanelJusts = new JPanel();
	static String TAB_JBC = __("Debate");
	public static int TAB_JBC_ = 10;
	static final JPanel JunkPanelJBC = new JPanel();
	static String TAB_NEWS = __("News");
	static int TAB_NEWS_ = 11;
	static final JPanel JunkPanelNews = new JPanel();
	static String TAB_MAN = __("Adhoc");
	static int TAB_MAN_ = 12;
	static final JPanel JunkPanelMAN = new JPanel();
	static String TAB_CONN = __("Connections");
	static int TAB_CONN_ = 13;
	static final JPanel JunkPanelCONN = new JPanel();
	static String TAB_CLONE = __("Clones");
	static int TAB_CLONE_ = 14;
	static OrganizationRequests organizationRequests;
	static String TAB_REQUESTS = __("REQUESTS");
	static int TAB_REQUESTS_ = 15;
	static final JPanel JunkPanelCLONE = new JPanel();
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	static
	class tabOnFocus_changeListener implements ChangeListener {
		static boolean _settings = false;
		static final boolean _settings_reload = true;
		static boolean _keys = false;
		static final boolean _keys_reload = true;
		static boolean _peers = false; 
		static final boolean _peers_reload = false; 
		static final boolean _peers_disconnect = true; 
		static Peers _saved_peers = null; 
		static boolean _orgs = false;
		static final boolean _orgs_reload = false;
		static final boolean _orgs_disconnect = true; 
		static Orgs _saved_orgs = null; 
		static boolean _mots = false;
		static final boolean _mots_reload = false;
		static final boolean _mots_disconnect = true; 
		static Motions _saved_mots = null; 
		static boolean _justs = false;
		static final boolean _justs_reload = false;
		static final boolean _justs_disconnect = true; 
		static Justifications _saved_justs = null; 
		static boolean _debate = false;
		static final boolean _debate_reload = false;
		static final boolean _debate_disconnect = true; 
		static JustificationsByChoicePanel _saved_debate = null; 
		static boolean _news = false;
		static final boolean _news_reload = false;
		static final boolean _news_disconnect = true; 
		static NewsTable _saved_news = null; 
		static boolean _broadcast = false;
		static final boolean _broadcast_reload = false;
		static final boolean _broadcast_disconnect = true; 
		static WLAN_widget _saved_broadcast = null; 
		static boolean _connections = false;
		static final boolean _connections_reload = false;
		static final boolean _connections_disconnect = true; 
		static net.ddp2p.widgets.peers.PeerInstanceContacts _saved_connections = null; 
	    public void stateChanged(ChangeEvent e) {
	    	int index = MainFrame.tabbedPane.getSelectedIndex();
	    	MainFrame.status.setTab(index);
	    	if (!DD.preloadedControl) {
		    	if (MainFrame.TAB_SETTINGS_ == index) {
		    		if(!_settings) {
		    			ControlPane settings = null;
						try {
							GUI_Swing.controlPane = settings = new net.ddp2p.widgets.app.ControlPane();
						} catch (net.ddp2p.common.util.P2PDDSQLException e1) {
							e1.printStackTrace();
						}
		    			MainFrame.tabbedPane.setComponentAt(index, settings);
		    			_settings = true;
		    		}
		    	}else{
		    		if(_settings && _settings_reload){
		    			MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_SETTINGS_, MainFrame.JunkControlPane);
		    			GUI_Swing.controlPane = null;
		    			_settings = false;
		    		}
		    	}
	    	}
	    	if(MainFrame.TAB_KEYS_ == index) {
	    		if(!_keys) {
	    			Keys keys = new net.ddp2p.widgets.keys.Keys();
	    			MainFrame.tabbedPane.setComponentAt(index, keys.getPanel());
	    			_keys = true;
	    		}
	    	}else{
	    		if(_keys && _keys_reload){
	    			MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_KEYS_, MainFrame.JunkPanelKeys);
	    			_keys = false;
	    		}
	    	}
	    	if(MainFrame.TAB_ORGS_ == index) {
	    		if(!_orgs) {
	    			if(_saved_orgs==null) {
		    			_saved_orgs = new net.ddp2p.widgets.org.Orgs();
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_orgs.getComboPanel());
	    			}
		    		if(_orgs_reload || _orgs_disconnect) {
		    			_saved_orgs.connectWidget(); 
		    		}
	    			_orgs = true;
	    		}
	    	}else{
	    		if(_orgs){
	    			_orgs = false;
		    		if(_orgs_reload || _orgs_disconnect) {
		    			_saved_orgs.disconnectWidget();
		    		}
	    			if(_orgs_reload) {
	    				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_ORGS_, MainFrame.JunkPanelOrgs);
	    				_saved_orgs = null;
	    			}
	    		}
	    	}
	    	if (MainFrame.TAB_MOTS_ == index) {
	    		final boolean D = false;
				if (D || DD.DEBUG) System.out.println("DD.tabsChanged: motions index");
	    		if (!_mots) {
    				if (D || DD.DEBUG) System.out.println("DD.tabsChanged: !mots");
	    			if (_saved_mots == null) {
		    			_saved_mots = new net.ddp2p.widgets.motions.Motions();
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_mots.getComboPanel());
						if (D || DD.DEBUG) System.out.println("DD.tabsChanged: motions idx component set");
	    			}
		    		if (_mots_reload || _mots_disconnect) {
						if (D || DD.DEBUG) System.out.println("DD.tabsChanged: motions idx will connect");
		    			_saved_mots.connectWidget(); 
						if (D || DD.DEBUG) System.out.println("DD.tabsChanged: motions idx connected");
		    		}
	    			_mots = true;
	    		}
	    	} else {
	    		if (_mots) {
	    			_mots = false;
		    		if (_mots_reload || _mots_disconnect) {
		    			_saved_mots.disconnectWidget();
		    		}
	    			if (_mots_reload) {
	    				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_MOTS_, MainFrame.JunkPanelMots);
	    				_saved_mots = null;
	    			}
	    		}
				if (DD.DEBUG) System.out.println("DD.tabsChanged: motions index done");
	    	}
	    	if (MainFrame.TAB_JUSTS_ == index) {
	    		if (!_justs) {
	    			if (_saved_justs==null) {
		    			_saved_justs = new net.ddp2p.widgets.justifications.Justifications();
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_justs.getComboPanel());
	    			}
		    		if(_justs_reload || _justs_disconnect) {
		    			_saved_justs.connectWidget(); 
		    		}
	    			_justs = true;
	    		}
	    	} else {
	    		if (_justs) {
	    			_justs = false;
		    		if (_justs_reload || _justs_disconnect) {
		    			_saved_justs.disconnectWidget();
		    		}
	    			if (_justs_reload) {
	    				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_JUSTS_, MainFrame.JunkPanelJusts);
	    				_saved_justs = null;
	    			}
	    		}
	    	}
	    	if (MainFrame.TAB_JBC_ == index) {
	    		if (!_debate) {
	    			if (_saved_debate==null) {
		    			_saved_debate = new net.ddp2p.widgets.justifications.JustificationsByChoicePanel();
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_debate.getComboPanel());
	    			}
		    		if (_debate_reload || _debate_disconnect) {
		    			_saved_debate.connectWidget(); 
		    		}
	    			_debate = true;
	    		}
	    	} else {
	    		if (_debate) {
	    			_debate = false;
		    		if (_debate_reload || _debate_disconnect) {
		    			_saved_debate.disconnectWidget();
		    		}
	    			if (_debate_reload) {
	    				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_JBC_, MainFrame.JunkPanelJBC);
	    				_saved_debate = null;
	    			}
	    		}
	    	}
	    	if (MainFrame.TAB_NEWS_ == index) {
	    		if (!_news) {
	    			if(_saved_news==null) {
		    			_saved_news = new net.ddp2p.widgets.news.NewsTable();
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_news.getComboPanel());
	    			}
		    		if(_news_reload || _news_disconnect) {
		    			_saved_news.connectWidget(); 
		    		}
	    			_news = true;
	    		}
	    	}else{
	    		if(_news){
	    			_news = false;
		    		if(_news_reload || _news_disconnect) {
		    			_saved_news.disconnectWidget();
		    		}
	    			if(_news_reload) {
	    				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_NEWS_, MainFrame.JunkPanelNews);
	    				_saved_news = null;
	    			}
	    		}
	    	}
	    	if(MainFrame.TAB_MAN_ == index) {
	    		if(!_broadcast) {
	    			if(_saved_broadcast==null) {
		    			_saved_broadcast = new net.ddp2p.widgets.wireless.WLAN_widget(Application.getDB());
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_broadcast.getComboPanel());
	    			}
		    		if(_broadcast_reload || _broadcast_disconnect) {
		    			_saved_broadcast.connectWidget(); 
		    		}
	    			_broadcast = true;
	    		}
	    	}else{
	    		if(_broadcast){
	    			_broadcast = false;
		    		if(_broadcast_reload || _broadcast_disconnect) {
		    			_saved_broadcast.disconnectWidget();
		    		}
	    			if(_broadcast_reload) {
	    				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_MAN_, MainFrame.JunkPanelMAN);
	    				_saved_broadcast = null;
	    			}
	    		}
	    	}
	    	if(MainFrame.TAB_CONN_ == index) {
	    		if(!_connections) {
	    			if(_saved_connections==null) {
	    				_saved_connections = new net.ddp2p.widgets.peers.PeerInstanceContacts();
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_connections.getComboPanel());
	    			}
		    		if(_connections_reload || _connections_disconnect) {
		    			_saved_connections.connectWidget(); 
		    		}
		    		_connections = true;
	    		}
	    	}else{
	    		if(_connections){
	    			_broadcast = false;
		    		if(_connections_reload || _broadcast_disconnect) {
		    			_saved_connections.disconnectWidget();
		    		}
	    			if(_connections_reload) {
	    				MainFrame.tabbedPane.setComponentAt(MainFrame.TAB_CONN_, MainFrame.JunkPanelCONN);
	    				_saved_connections = null;
	    			}
	    		}
	    	}
	    }
	    public void _stateChanged(ChangeEvent e) {
	    	int index = MainFrame.tabbedPane.getSelectedIndex();
	    	String tabtitle = MainFrame.tabbedPane.getTitleAt(index);
	    	if(MainFrame.TAB_KEYS.equals(tabtitle)) {
	    		if(!_keys) {
	    			Keys keys = new net.ddp2p.widgets.keys.Keys();
	    			MainFrame.tabbedPane.setComponentAt(index, keys.getPanel());
	    			_keys = true;
	    		}
	    	}else{
	    		if(_keys && _keys_reload){
	    			int tabs = MainFrame.tabbedPane.getTabCount();
	    			for(int k=0; k<tabs; k++) {
	    		    	String _tabtitle = MainFrame.tabbedPane.getTitleAt(k);
	    		    	if(MainFrame.TAB_KEYS.equals(_tabtitle)){
	    		    		MainFrame.tabbedPane.setComponentAt(k, MainFrame.JunkPanelKeys);
	    		    		_keys = false;
	    		    		break;
	    		    	}
	    			}
	    		}
	    	}
	    	if(MainFrame.TAB_PEERS.equals(tabtitle)) {
	    		if(!_peers) {
	    			if(_saved_peers==null) {
		    			_saved_peers = new net.ddp2p.widgets.peers.Peers();
		    			MainFrame.tabbedPane.setComponentAt(index, _saved_peers.getPanel());
	    			}
		    		if(_peers_reload || _peers_disconnect) {
		    			_saved_peers.connectWidget(); 
		    		}
	    			_peers = true;
	    		}
	    	}else{
	    		if(_peers){
		    		_peers = false;
		    		if(_peers_reload || _peers_disconnect) {
		    			Application.peers.disconnectWidget();
		    		}
	    			if(_peers_reload){
		    			int tabs = MainFrame.tabbedPane.getTabCount();
		    			for(int k=0; k<tabs; k++) {
		    		    	String _tabtitle = MainFrame.tabbedPane.getTitleAt(k);
		    		    	if(MainFrame.TAB_PEERS.equals(_tabtitle)){
		    		    		MainFrame.tabbedPane.setComponentAt(k, MainFrame.JunkPanelPeers);
		    		    	}
		    		    	break;
		    			}
		    			_saved_peers = null;
	    			}
	    		}
	    	}
	    }
	}
	static void initGUILookAndFeel(){
		    System.setProperty("Quaqua.tabLayoutPolicy", "wrap");
	}
	/**
	 * Starts the spash with dimensions in DD.FRAME_XXX
	 * @return
	 */
	public static JFrame initMainFrameSplash(){
		JFrame _frame = new JFrameDropCatch(DD.APP_NAME);
		_frame.getRootPane().addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
	        	if(MainFrame._jbc!=null)MainFrame._jbc.adjustSize();
	        }
		});
		_frame.addWindowListener(new WindowAdapter() {
	            public void windowClosing(WindowEvent e) {
	            	DD.clean_exit();
	            }
	        });
		MainFrame.loadAppIcons(false, _frame);
	    String splash;
	    splash = MainFrame.IMAGE_START_SPLASH;
	    ImageIcon image = DDIcons.getImageIconFromResource(splash, _frame, "Starting Splash");
	    MainFrame.splash_label = new JLabel("", image, JLabel.CENTER);
	    _frame.add(MainFrame.splash_label);
	    _frame.pack();
	    _frame.setVisible(true);
		Rectangle r = _frame.getBounds();
		r.x = DD.FRAME_OFFSET ;
		r.width=DD.FRAME_WIDTH;
		r.y = DD.FRAME_HSTART ;
		r.width=DD.FRAME_HEIGHT;
		_frame.setBounds(r.x, r.y, r.width, r.height);
		return _frame;
	}
	/**
	 * No high quality icons available yet (would need some larger castles for that)
	 * @param quality
	 * @param _frame
	 */
	public static void loadAppIcons(boolean quality, JFrame _frame) {
		if(quality) {
			if (_frame != null) {
			java.awt.Image icon_conf = net.ddp2p.widgets.app.DDIcons.getImageFromResource(DDIcons.I_DDP2P40, _frame);
			_frame.setIconImage(icon_conf);
			java.util.List<Image> icons = new ArrayList<Image>();
			icons.add(icon_conf);
			java.awt.Image icon_conf32 = net.ddp2p.widgets.app.DDIcons.getImageFromResource(DDIcons.I_DDP2P32, _frame);
			icons.add(icon_conf32);
			MainFrame.frame.setIconImages(icons);
		}
		} else {
			java.awt.Image icon_conf = net.ddp2p.widgets.app.DDIcons.getImageFromResource(DDIcons.I_DDP2P40, _frame);
			_frame.setIconImage(icon_conf);
		}
	}
	public static void addTabPeer() {
	}
	public static Component makeOrgsPanel(net.ddp2p.widgets.org.OrgEditor orgEPane, net.ddp2p.widgets.org.Orgs orgsPane) {
		JPanel orgs = new JPanel();
		int y = 0;
		java.awt.GridBagLayout gbl = new java.awt.GridBagLayout();
		orgs.setLayout(gbl);
		java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
		JButton newOrgButton;
		newOrgButton = new JButton(__("New Organization"));
		newOrgButton.setActionCommand(DD.COMMAND_NEW_ORG);
		newOrgButton.addActionListener(((java.awt.event.ActionListener)Application.appObject));
		c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=0.0;c.weightx=10.0;c.anchor=GridBagConstraints.WEST;
		orgs.add(newOrgButton,c);
		c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;
		orgs.add(orgEPane,c);
		JSplitPane splitPanel = new JSplitPane( JSplitPane.VERTICAL_SPLIT, orgsPane.getScrollPane(),
				(orgs));
		return splitPanel;
	}
	/**
	 * 
	 * @param _medit this should be a MotionsListener only if it is a MotionEditor!
	 * @param motions
	 * @return
	 */
	public static Component makeMotionPanel( Component _medit, Component motions) {
		JScrollPane motion_scroll;
		if (motions instanceof Motions) {
			motion_scroll = ((Motions)motions).getScrollPane();
		} else {
			if (motions instanceof JScrollPane)
				motion_scroll = (JScrollPane)motions;
			else {
				motion_scroll = new JScrollPane(motions); 
				if (_DEBUG) System.out.println("MainFrame: makeMotionPanel: unexpected motion type: "+ motions);
			}
		}
		if (_medit instanceof MotionsListener) {
			JScrollPane edit_scroll;
			JSplitPane result;
			Dimension minimumSize;
			edit_scroll = new JScrollPane(_medit);
			result = new JSplitPane(JSplitPane.VERTICAL_SPLIT, motion_scroll, edit_scroll);
			result.setDividerLocation(.5);
			result.setResizeWeight(0.5);
			minimumSize = new Dimension(0, 100);
			motion_scroll.setMinimumSize(minimumSize);
			Dimension _minimumSize = new Dimension(0, MotionEditor.DIMY);
			edit_scroll.setMinimumSize(_minimumSize);
			if (DEBUG) System.out.println("MainFrame: makeMotionPanel: return splitter");
			return  result;
		} else {
			if (DEBUG) System.out.println("MainFrame: makeMotionPanel: return motion_scroll");
			return motion_scroll;
		}
	}
	public static JScrollPane makeCensusPanel(net.ddp2p.widgets.census.CensusPanel census){
		return census.getScrollPane();
	}
	/**
	 * Version with fixed Panel
	 * @param _nedit
	 * @param news
	 * @return
	 */
	public static JPanel _makeNewsPanel( NewsEditor _nedit, NewsTable news) {
		int y = 0;
		JPanel motion_panel = new JPanel();
		JScrollPane _news = news.getScrollPane();
		java.awt.GridBagLayout gbl = new java.awt.GridBagLayout();
		motion_panel.setLayout(gbl);
		java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
		c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;
		motion_panel.add(_news,c);
		c.fill=GridBagConstraints.BOTH;c.gridx=0;c.gridy=y++;c.weighty=5.0;c.weightx=10.0;c.anchor=GridBagConstraints.CENTER;c.insets=new java.awt.Insets(0,0,0,0);
		motion_panel.add(_nedit,c);
		return motion_panel;
	}
	/**
	 * Version with splitter
	 * @param _nedit
	 * @param news
	 * @return
	 */
	public static Component makeNewsPanel( NewsEditor _nedit, NewsTable news) {
		JScrollPane _news = news.getScrollPane();
		return new JSplitPane(JSplitPane.VERTICAL_SPLIT, _news, _nedit);
	}
	public static Component makeJBCViewPanel( Panel _jedit, JustificationsByChoicePanel jbc) {
		JSplitPane justification_panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				jbc, 
				new JScrollPane(_jedit));
		justification_panel.setDividerLocation(.7);
		justification_panel.setResizeWeight(0.7);
		return justification_panel; 
	}
	public static Component makeJustificationPanel( JustificationEditor _jedit,  JustificationViewer _jview, Justifications justifications) {
		JPanel vpanel = _jedit;
		if (Justifications.USING_VIEWER) {
			vpanel = new JPanel();
			if (_jedit != null) vpanel.add(_jedit);
			if (_jview != null) vpanel.add(_jview);
		}
		JSplitPane justification_panel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				justifications.getScrollPane(),
				new JScrollPane(vpanel));
		justification_panel.setDividerLocation(.5);
		justification_panel.setResizeWeight(0.5);
		return justification_panel;
	}
	public static JSplitPane makeWLanPanel(WLAN_widget wlan_widget) {
		JPanel hints_panel = new JPanel();
		MainFrame.wireless_hints = new JTextArea();
		MainFrame.wireless_hints.setEditable(false);
		MainFrame.wireless_hints.setText("DirectDemocracy P2P");
		hints_panel.add(MainFrame.wireless_hints);
		JLabel crt_cmd = (JLabel)Util_GUI.crtProcessLabel;
		crt_cmd.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		crt_cmd.setVerticalAlignment(SwingConstants.TOP);
		crt_cmd.setHorizontalAlignment(SwingConstants.LEFT);
		JPanel crt_cmd_panel = new JPanel();
		crt_cmd_panel.addMouseListener(new MouseListener(){
			@Override
			public void mouseClicked(MouseEvent e) {
			}
			@Override
			public void mousePressed(MouseEvent e) {
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				Util.stopCrtScript();
			}
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			@Override
			public void mouseExited(MouseEvent e) {
			}
		});
		crt_cmd_panel.add(crt_cmd);
		GridBagLayout bl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel dbg_panel = new JPanel(bl);
		int y = 0;
		c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(__("Client Sockets Number")), c);
		c.gridx=1; c.gridy=0; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets = new JTextField();
		MainFrame.client_sockets = clientsockets;
		clientsockets.setText("                     ");
		dbg_panel.add(clientsockets, c);
		y++;
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.EAST;
		dbg_panel.add(new JLabel(__("Client Sockets")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_val = new JTextField();
		MainFrame.client_sockets_val = clientsockets_val;
		clientsockets_val.setText("                     ");
		dbg_panel.add(clientsockets_val, c);
		y++;
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(__("SentMsgCounter")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_cntr = new JTextField();
		MainFrame.client_sockets_cntr = clientsockets_cntr;
		clientsockets_cntr.setText("                     ");
		dbg_panel.add(clientsockets_cntr, c);
		y++;
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(__("Adhoc long intermessage delay (seconds (> 0))")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_long_sleep_seconds = new JTextField();
		MainFrame.clientsockets_long_sleep_seconds  = clientsockets_long_sleep_seconds;
		clientsockets_long_sleep_seconds.setText(""+DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP);
		clientsockets_long_sleep_seconds.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				long seconds = 0;
				JTextField f = (JTextField)e.getSource();
				try{seconds = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP = seconds));
			}
		});
		clientsockets_long_sleep_seconds.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				long seconds = 0;
				JTextField f = (JTextField)e.getSource();
				try{seconds = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_SECONDS_DURATION_LONG_SLEEP = seconds));
			}
		});
		dbg_panel.add(clientsockets_long_sleep_seconds, c);
		y++;
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(__("Adhoc long intermessage delay start (minutes modulus (> 1))")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_long_sleep_minutes_start = new JTextField();
		MainFrame.clientsockets_long_sleep_minutes_start  = clientsockets_long_sleep_minutes_start;
		clientsockets_long_sleep_minutes_start.setText(""+DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP);
		clientsockets_long_sleep_minutes_start.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				int minutes = 0;
				JTextField f = (JTextField)e.getSource();
				try{minutes = Integer.parseInt(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP = minutes));
			}
		});
		clientsockets_long_sleep_seconds.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				int minutes = 0;
				JTextField f = (JTextField)e.getSource();
				try{minutes = Integer.parseInt(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MINUTE_START_LONG_SLEEP = minutes));
			}
		});
		dbg_panel.add(clientsockets_long_sleep_minutes_start, c);
		y++;
		c.gridx=0; c.gridy=y; c.anchor=GridBagConstraints.WEST;
		dbg_panel.add(new JLabel(__("Adhoc intermessage delay (milliseconds (> 0))")), c);
		c.gridx=1; c.gridy=y; c.anchor=GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
		JTextField clientsockets_sleep = new JTextField();
		MainFrame.clientsockets_sleep  = clientsockets_sleep;
		clientsockets_sleep.setText(""+DD.ADHOC_SENDER_SLEEP_MILLISECONDS);
		clientsockets_sleep.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				long millis = 0;
				JTextField f = (JTextField)e.getSource();
				try{millis = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MILLISECONDS = millis));
			}
		});
		clientsockets_sleep.addFocusListener(new FocusListener(){
			@Override
			public void focusGained(FocusEvent e) {}
			@Override
			public void focusLost(FocusEvent e) {
				long millis = 0;
				JTextField f = (JTextField)e.getSource();
				try{millis = Long.parseLong(f.getText());}catch(Exception i){}
				f.setText(""+(DD.ADHOC_SENDER_SLEEP_MILLISECONDS = millis));
			}
		});
		clientsockets_sleep.getDocument().addDocumentListener(new DocumentListener(){
			private void upd(DocumentEvent e) {
				String text=null;
				long millis = 0;
				try{
					text = e.getDocument().getText(0, e.getDocument().getLength());
					millis = Long.parseLong(text);
				}catch(Exception i){}
				DD.ADHOC_SENDER_SLEEP_MILLISECONDS = millis;
			}
			@Override
			public void insertUpdate(DocumentEvent e) {upd(e);}
			@Override
			public void removeUpdate(DocumentEvent e) {upd(e);}
			@Override
			public void changedUpdate(DocumentEvent e) {upd(e);}
		});
		dbg_panel.add(clientsockets_sleep, c);
		y++;
		if(net.ddp2p.common.wireless.BroadcastClient.broadcast_client_sockets!=null)
			clientsockets.setText(""+net.ddp2p.common.wireless.BroadcastClient.broadcast_client_sockets.length);
		JSplitPane p1, p2;
		JSplitPane wpanel =
				new JSplitPane(JSplitPane.VERTICAL_SPLIT, crt_cmd_panel ,
				(p1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, wlan_widget.getScrollPane(),
				(p2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,hints_panel, dbg_panel)))));
		p2.setResizeWeight(1.0);
		p1.setResizeWeight(1.0);
		return wpanel;
	}
	public static void askIdentity() {
	    MainFrame.identitiesPane = new MyIdentitiesTest(Application.getDB());
	}
	public static void createAndShowGUI() throws P2PDDSQLException{
			if (DD.DEBUG) System.out.println("createAndShowGUI: start");
			ImageIcon icon_conf = net.ddp2p.widgets.app.DDIcons.getConfigImageIcon("Config");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_peer = net.ddp2p.widgets.app.DDIcons.getPeerSelImageIcon("General Peer");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_org = net.ddp2p.widgets.app.DDIcons.getOrgImageIcon("General Organization");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_con = net.ddp2p.widgets.app.DDIcons.getConImageIcon("General Constituent");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_mot = net.ddp2p.widgets.app.DDIcons.getMotImageIcon("General Motion");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_bal = net.ddp2p.widgets.app.DDIcons.getBalanceImageIcon("Justifications");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_jus = net.ddp2p.widgets.app.DDIcons.getJusImageIcon("General Justification");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_key = net.ddp2p.widgets.app.DDIcons.getKeyImageIcon("Keys");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_news = net.ddp2p.widgets.app.DDIcons.getNewsImageIcon("General News");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_dir = net.ddp2p.widgets.app.DDIcons.getDirImageIcon("Dir");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_wireless = net.ddp2p.widgets.app.DDIcons.getWirelessImageIcon("Wireless");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_census = net.ddp2p.widgets.app.DDIcons.getCensusImageIcon("Census");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_mail = net.ddp2p.widgets.app.DDIcons.getMailPostImageIcon("Addresses");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			ImageIcon icon_identity = net.ddp2p.widgets.app.DDIcons.getIdentitiesImageIcon("Identities");//Util.createImageIcon("icons/sad.smiley10.gif","General Org");
			MainFrame.status = new GUIStatusHistory();
			MainFrame.tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
			MainFrame.tabbedPane.addChangeListener(new tabOnFocus_changeListener());
			Application.appObject = new AppListener(); 
	        MainFrame.clientConsole = new Console();
	        MainFrame.serverConsole = new Console();
	        DD.ed.addClientListener(MainFrame.clientConsole);
	        DD.ed.addServerListener(MainFrame.serverConsole);
	        Directories listing_dicts = (Directories)(Application.directory_status = new Directories()); 
	        if (MainFrame.identitiesPane == null)
	        	MainFrame.identitiesPane = new MyIdentitiesTest(Application.getDB());
	        if(DD.DEBUG) System.out.println("createAndShowGUI: identities started");
	        MainFrame.constituentsPane = new ConstituentsPanel(Application.getDB(), -1, -1, null);
	        GUI_Swing.constituents = MainFrame.constituentsPane;
			if(DD.DEBUG) System.out.println("createAndShowGUI: WLAN started");
	        CensusPanel _censusPane;
	        if(MainFrame.SONG){
	        	_censusPane = new CensusPanel();
	        	JScrollPane censusPane = makeCensusPanel(_censusPane);
	        }
			if(DD.DEBUG) System.out.println("createAndShowGUI: census added");
	    	if(DD.preloadedControl) {
	    		GUI_Swing.controlPane=MainFrame.controlPane = new ControlPane();
	    		MainFrame.tabbedPane.addTab(MainFrame.TAB_SETTINGS, icon_conf, MainFrame.controlPane, __("Configuration"));
	    	}else
	    		MainFrame.tabbedPane.addTab(MainFrame.TAB_SETTINGS, icon_conf, MainFrame.JunkControlPane, __("Configuration"));
	        MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_SETTINGS_, KeyEvent.VK_S);
	        MainFrame.tabbedPane.addTab(MainFrame.TAB_KEYS, icon_key, MainFrame.JunkPanelKeys, __("Keys"));
	        MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_KEYS_, KeyEvent.VK_K);
	        MainFrame.tabbedPane.addTab(MainFrame.TAB_ID, icon_identity, MainFrame.identitiesPane, __("Identities"));
	        MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_ID_, KeyEvent.VK_I);
	        MainFrame.tabbedPane.addTab(MainFrame.TAB_CONS, icon_con, MainFrame.constituentsPane, __("Constituents"));
	        MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_CONS_, KeyEvent.VK_C);
	        if(MainFrame.SONG)MainFrame.tabbedPane.addTab(__("Census"), icon_census, _censusPane, __("Census"));
	        MainFrame.tabbedPane.addTab(MainFrame.TAB_DIRS, icon_dir, listing_dicts.getPanel(), __("Directory"));
	        MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_DIRS_, KeyEvent.VK_D);
	        if(DD.DEBUG) System.out.println("createAndShowGUI: added tab census");
	        Application.peers = MainFrame.peersPluginsPane = new net.ddp2p.widgets.peers.Peers(Application.getDB());
			if(DD.DEBUG) System.out.println("createAndShowGUI: created peers");
			MainFrame.tabbedPane.addTab(MainFrame.TAB_PEERS, icon_peer, MainFrame.peersPluginsPane.getPanel(), __("Peers"));//peersPluginsPane.getScrollPane());
			if(DD.DEBUG) System.out.println("createAndShowGUI: got peer panel");
	        MainFrame.status.addOrgStatusListener(((Peers)Application.peers).privateOrgPanel);
			if(DD.DEBUG) System.out.println("createAndShowGUI: created added peer listener");
	        MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_PEERS_, KeyEvent.VK_P);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added peers");
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_ORGS, icon_org, MainFrame.JunkPanelOrgs, __("Organizations")); // tab_organization no longer used
	    	MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_ORGS_, KeyEvent.VK_O);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added orgs");
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_MOTS, icon_mot, MainFrame.JunkPanelMots, __("Motions"));
	    	MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_MOTS_, KeyEvent.VK_M);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added mots");
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_JUSTS, icon_bal, MainFrame.JunkPanelJusts, __("Justifications"));
	    	MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_JUSTS_, KeyEvent.VK_J);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added justs");
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_JBC, icon_jus, MainFrame.JunkPanelJBC, __("Debate"));
	    	MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_JBC_, KeyEvent.VK_D);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added debate");
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_NEWS, icon_news, MainFrame.JunkPanelNews, __("News"));
	    	MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_NEWS_, KeyEvent.VK_N);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added news");
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_MAN, icon_wireless, MainFrame.JunkPanelMAN, __("Mobile Adhoc Networks"));
	    	MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_MAN_, KeyEvent.VK_A);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added MAN");
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_CONN, icon_wireless, MainFrame.JunkPanelCONN, __("Connections"));
	    	MainFrame.tabbedPane.setMnemonicAt(MainFrame.TAB_CONN_, KeyEvent.VK_N);
			if(DD.DEBUG) System.out.println("createAndShowGUI: added CONN");
	    	if(GUI_Swing.orgs!=null) GUI_Swing.orgs.addOrgListener(MainFrame.status);
	        if (MainFrame.SONG) MainFrame.status.addOrgStatusListener(_censusPane);
	        MainFrame.my_peer_address = new PeerAddresses(true); 
	    	MainFrame.tabbedPane.addTab("Addr", icon_mail, MainFrame.my_peer_address.getScrollPane(), __("My Addresses"));
	    	MainFrame.status.addPeerMeStatusListener(MainFrame.my_peer_address);
	    	MainFrame.peer_addresses = new PeerAddresses(false);    	
	    	MainFrame.tabbedPane.addTab("IPs", icon_mail, MainFrame.peer_addresses.getScrollPane(), __("Addresses Selected Peer"));
	    	MainFrame.status.addPeerSelectedStatusListener(MainFrame.peer_addresses);
	    	MainFrame.peer_instances = new Instances();
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_CLONE, icon_identity, MainFrame.peer_instances.getPanel(), __("Instances"));
	    	MainFrame.status.addPeerSelectedStatusListener(MainFrame.peer_instances.getModel());
	    	MainFrame.organizationRequests = new OrganizationRequests();
	    	MainFrame.tabbedPane.addTab(MainFrame.TAB_REQUESTS, icon_identity, MainFrame.organizationRequests, __("Organization Requests"));
	    	MainFrame.status.addOrgStatusListener(MainFrame.organizationRequests);
			if(DD.DEBUG) System.out.println("createAndShowGUI: done tabs");
	        MainFrame.frame.remove(MainFrame.splash_label);
	        JPanel main = new JPanel(new BorderLayout());
	        main.add(MainFrame.tabbedPane, BorderLayout.CENTER);
	        StatusBar statusBar = new StatusBar(MainFrame.status);
	        JScrollPane status_bar = new JScrollPane(statusBar);
	        status_bar.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	        main.add(status_bar, BorderLayout.SOUTH);
	        MainFrame.frame.setContentPane(main);
	        MainFrame.frame.setVisible(true);		
			if(DD.DEBUG) System.out.println("createAndShowGUI: done frame");
			Rectangle r = MainFrame.frame.getBounds();
			r.x = DD.FRAME_OFFSET ;
			r.width=DD.FRAME_WIDTH;
			r.y = DD.FRAME_HSTART ;
			r.width=DD.FRAME_HEIGHT;
			MainFrame.frame.setBounds(r.x, r.y, r.width, r.height);
			if(Identity.default_id_branch==null){
				if(_DEBUG) System.out.println("DD:createAndShowGUI:No default identity!");
				MainFrame.tabbedPane.setSelectedComponent(MainFrame.identitiesPane);
				return;
			}
			long _organizationID = Identity.getDefaultOrgID();
			if (_organizationID <= 0) {
				if (_DEBUG) System.out.println("DD:createAndShowGUI:No default organization!");
				MainFrame.tabbedPane.setSelectedIndex(MainFrame.TAB_ORGS_);
				return;
			}
			if (net.ddp2p.common.data.D_Organization.isOrgAvailableForLID(_organizationID, DD.DEBUG) != 1) {
				if (_DEBUG) System.out.println("DD:createAndShowGUI: Temporary organization!");
				MainFrame.tabbedPane.setSelectedIndex(MainFrame.TAB_ORGS_);
				return;
			}
			MainFrame.status.setSelectedOrg(D_Organization.getOrgByLID_NoKeep(_organizationID, true));
			long _constID = -1;
			if ( (_constID = Identity.getDefaultConstituentIDForOrg(_organizationID) ) <= 0) {
				if (_DEBUG) System.out.println("DD:createAndShowGUI:No default constituent!");
				MainFrame.tabbedPane.setSelectedComponent(MainFrame.constituentsPane);
				return;
			}
			if (_DEBUG) System.out.println("DD:createAndShowGUI:Default org="+_organizationID+" const="+_constID+" selected!");
			MainFrame.status.setMeConstituent(D_Constituent.getConstByLID(_constID, true, false));
			MainFrame.tabbedPane.setSelectedIndex(MainFrame.TAB_MOTS_);
		}
	/**
	 * The preferred languages are specified in the (default) identity in the identities table
	 * @return
	 * @throws P2PDDSQLException
	 */
	static Language[] get_preferred_languages() throws P2PDDSQLException {
		ArrayList<ArrayList<Object>> id;
		id=Application.getDB().select("SELECT "+net.ddp2p.common.table.identity.preferred_lang +
				" FROM "+net.ddp2p.common.table.identity.TNAME+" AS i" +
				" WHERE i."+net.ddp2p.common.table.identity.default_id+"==1 LIMIT 1;",
				new String[]{});
		if(id.size()==0){
			if(DD.DEBUG)System.err.println("DD:get_preferred_languages:No default identity found!");
			return new Language[]{new Language("en", "US"),new Language("en", null)};
		}
		String preferred_lang = Util.getString(id.get(0).get(0));
		String[]langs=preferred_lang.split(Pattern.quote(":"));
		Language[]result = new Language[langs.length];
		for(int k=0; k<result.length; k++){
			String[]lcrt = langs[k].split(Pattern.quote("_"));
			if(lcrt.length==2) result[k]= new Language(lcrt[0],lcrt[1]);
			else if(lcrt.length==1) result[k]= new Language(lcrt[0],lcrt[0].toUpperCase());
			if(DD.DEBUG)System.err.println("DD:get_preferred_languages:Language="+langs[k]+"->"+result[k]);
		}
		return result;
	}
		public static ControlPane controlPane;
		public static boolean hasOrgPane = false;
		public static boolean hasPeersPane = false;
		public static ConstituentsPanel constituentsPane;
		public static MyIdentitiesTest identitiesPane;
		public static net.ddp2p.widgets.peers.Peers peersPluginsPane;
		public static Console clientConsole;
		public static Console serverConsole;
		public static JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		public static JFrame frame;
		public static JLabel splash_label;
		public static JustificationsByChoicePanel _jbc;
		public static Component tab_organization;	
		public static JTextArea wireless_hints;
		static Toolkit toolkit;
		public static JTextField client_sockets=null;
		public static JTextField client_sockets_val = null;
		public static JTextField client_sockets_cntr = null;
		public static JTextField clientsockets_sleep = null;
		static JTextField clientsockets_long_sleep_seconds;
		static JTextField clientsockets_long_sleep_minutes_start;
		public static GUIStatusHistory status = null;
		static PeerAddresses peer_addresses;
		static Instances peer_instances;
		static PeerAddresses my_peer_address;
		public static String getTopic(String globalID, String peer_ID) {
			String topic = null;
			try {
				topic = DD.getAppText(DD.MY_DEBATE_TOPIC);
				if(((topic == null)||(topic.length()==0)) && !DD.asking_topic) {
					DD.asking_topic = true;
					topic = Util.getString(JOptionPane.showInputDialog(JFrameDropCatch.mframe,
							__("Declare a topic for your discussions"),
							__("Topic"), JOptionPane.PLAIN_MESSAGE, null, null, null));
					if(topic != null) DD.setAppTextNoSync(DD.MY_DEBATE_TOPIC, topic);
					DD.asking_topic = false;
				}
			} catch (net.ddp2p.common.util.P2PDDSQLException e) {
				e.printStackTrace();
			}
			return topic;
		}
		public static void setBroadcastServerStatus_GUI(boolean run) {
			if (run) {
				if(EventQueue.isDispatchThread()){if(controlPane!=null)  controlPane.m_startBroadcastServer.setText(controlPane.STOP_BROADCAST_SERVER);
				}else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							if(controlPane!=null) controlPane.m_startBroadcastServer.setText(controlPane.STOP_BROADCAST_SERVER);
						}
					});
			} else {
				if(EventQueue.isDispatchThread()){if(controlPane!=null)  controlPane.m_startBroadcastServer.setText(controlPane.START_BROADCAST_SERVER);
				}else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							if(controlPane!=null) controlPane.m_startBroadcastServer.setText(controlPane.START_BROADCAST_SERVER);
						}
					});
			}
		}
		public static void setBroadcastClientStatus_GUI(boolean run) {
			if (run) {
				if(EventQueue.isDispatchThread()){ if(controlPane!=null) controlPane.m_startBroadcastClient.setText(controlPane.STOP_BROADCAST_CLIENT);
				}else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							if(controlPane!=null) controlPane.m_startBroadcastClient.setText(controlPane.STOP_BROADCAST_CLIENT);
						}
					});
			} else {
				if(EventQueue.isDispatchThread()){ if(controlPane!=null) controlPane.m_startBroadcastClient.setText(controlPane.START_BROADCAST_CLIENT);
				}else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							if(controlPane!=null) controlPane.m_startBroadcastClient.setText(controlPane.START_BROADCAST_CLIENT);
						}
					});
			}
		}
		public static void setSimulatorStatus_GUI(boolean run) {
			if (run) {
				if(EventQueue.isDispatchThread()){if(controlPane!=null)  controlPane.m_startSimulator.setText(controlPane.STOP_SIMULATOR);
				}else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							if(controlPane!=null) controlPane.m_startSimulator.setText(controlPane.STOP_SIMULATOR);
						}
					});
			}else{
				if(EventQueue.isDispatchThread()) controlPane.m_startSimulator.setText(controlPane.START_SIMULATOR);
				else
					EventQueue.invokeLater(new Runnable() {
						public void run(){
							controlPane.m_startSimulator.setText(controlPane.START_SIMULATOR);
						}
					});
			}
		}
		public static final boolean SONG = true;
		public static final String IMAGE_START_SPLASH = "LargeSplash.jpg";
		static public void main(String args[]) throws net.ddp2p.common.util.P2PDDSQLException {
			try {
				_main(args);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Application_GUI.ThreadsAccounting_unregisterThread();
		}
		static public void _main(String args[]) throws net.ddp2p.common.util.P2PDDSQLException {
			ArrayList<String> potentialDatabases = new ArrayList<String>();
			String dfname = Application.DELIBERATION_FILE;
			String pGID = null;
			String controlIP = null;
			boolean askIdentityFirst = false;
			char c;
			while ((c = GetOpt.getopt(args, "d:p:ciC:h")) != GetOpt.END) {
				switch (c) {
				case 'h':
					System.out.println("java -cp DD.jar:... widgets.app.MainFrame "
							+ "\n\t -c          Console mode"
							+ "\n\t -C IP       The IP address from which one can remote control"
							+ "\n\t -p GID      Load the peer with this GID as current peer"
							+ "\n\t -d file     Use file as current database (dir database in the same folder)"
							+ "\n\t -i          Start by asking first for identity (go to that tab first)");
					System.exit(-1);
				case 'c':
					System.out.println("CONSOLE");
					DD.GUI = false;
					break;
				case 'C':
					System.out.println("CONTROL IP");
					controlIP = GetOpt.optarg;
					break;
				case 'p':
					System.out.println("peer= "+GetOpt.optarg);
					pGID = GetOpt.optarg;
					break;
				case 'd':
					System.out.println("db+="+GetOpt.optarg);
					potentialDatabases.add(GetOpt.optarg);
					break;
				case 'i':
					askIdentityFirst = true;
					break;
				case GetOpt.END:
					System.out.println("REACHED END OF OPTIONS");
					break;
				case '?':
					System.out.println("Options ?:"+GetOpt.optopt);
					return;
				default:
					System.out.println("Error: "+c);
					return;
				}
			}
			if (GetOpt.optind < args.length) {
				for (int i = GetOpt.optind; i < args.length; i ++) {
					System.out.println("OPTS: \""+args[i]+"\""); // GetOpt.optind
					potentialDatabases.add(args[i]); 
				}
			}
			if (!Application.DELIBERATION_FILE.equals(dfname)) potentialDatabases.add(Application.DELIBERATION_FILE);
			DD.set_DEBUG();
			net.ddp2p.java.db.Vendor_JDBC_EMAIL_DB.initJDBCEmail();
			if (DD.GUI) {
				net.ddp2p.widgets.components.GUI_Swing.initSwingGUI();
				Application_GUI.ThreadsAccounting_registerThread();
				initGUILookAndFeel();
				MainFrame.toolkit = Toolkit.getDefaultToolkit();
				int screen_width = (int)MainFrame.toolkit.getScreenSize().getWidth();
				int screen_height = (int)MainFrame.toolkit.getScreenSize().getHeight();
				if (screen_width > 680) {
					DD.FRAME_OFFSET = (screen_width-600)/3;
					DD.FRAME_WIDTH = 600;
					DD.FRAME_HSTART = (screen_height-600)/3;
					DD.FRAME_HEIGHT = 450;
				} else {
					DD.FRAME_OFFSET = 0;
					DD.FRAME_WIDTH = screen_width;
					DD.FRAME_HSTART = 0;
					DD.FRAME_HEIGHT = screen_height;
				}
				MainFrame.frame = initMainFrameSplash();
			}
			DD.startTime = Util.CalendargetInstance();
			if (DD.ONLY_IP4) System.setProperty("java.net.preferIPv4Stack", "true");
			if (DD.DEBUG) System.out.println("User="+Application.USERNAME);
			if (DD.DEBUG) System.out.println("Params: ["+args.length+"]="+Util.concat(args, " ; "));
			if(DD.DEBUG) System.out.println("DD:run: try databases");
			Hashtable<String, String> errors_db = new Hashtable<String, String>();
			for (String attempt : potentialDatabases) {
				if (DD.DEBUG) System.out.println("DD:run: try db: "+attempt);
				String error = DD.try_open_database(attempt);
				if (error == null ) {
					if(DD.DEBUG) System.out.println("DD:run: try db success: "+attempt);
					break;
				}
				errors_db.put(attempt, error);
				if (args.length > 1) System.err.println(__("Failed attempt to open first choice file:")+" \""+attempt+"\": "+error);
			}
			if (DD.DEBUG) System.err.println(__("DD: main: Got DB from command line: ")+Application.getDB());
			if (Application.getDB() == null) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileFilter(new net.ddp2p.widgets.components.DatabaseFilter());
				chooser.setName(__("Select database"));
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				chooser.setMultiSelectionEnabled(false);
				int returnVal = chooser.showDialog(MainFrame.frame,__("Specify Database"));
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File fileDB = chooser.getSelectedFile();
					String error;
					try {
						error = DD.try_open_database(fileDB.getCanonicalPath());
						if(error != null )
							errors_db.put(fileDB.getPath(), error);
					} catch (IOException e) {
						e.printStackTrace();
						errors_db.put(fileDB.getPath(), e.getLocalizedMessage());
					}
				}
				if (Application.getDB() == null) {
					Application_GUI.warning(__("Missing database. Tried:\n "+Util.concat(errors_db, ",\n ", null, ": ")), __("Need to reinstall database!"));
					if (DD.DEBUG) Util.dumpThreads();
					System.exit(1);
				}
			}
			if (DD.DEBUG) System.err.println(__("DD: main: Got DB fin")+Application.getDB());
			StartUp.detect_OS_and_store_in_DD_OS_var();
			StartUp.fill_install_paths_all_OSs_from_DB(); 
			StartUp.switch_install_paths_to_ones_for_current_OS();
			if (DD.WARN_OF_INVALID_SCRIPTS_BASE_DIR != null) {
				Application_GUI.fixScriptsBaseDir(Application.CURRENT_SCRIPTS_BASE_DIR());
				StartUp.fill_install_paths_all_OSs_from_DB(); 
				StartUp.switch_install_paths_to_ones_for_current_OS();
			}
			if(DD.DEBUG) System.out.println("DD:run: done database, try import databases");
			DBInterface selected_db = Application.getDB(); 
			final String db_to_import = DD.getExactAppText (DD.APP_DB_TO_IMPORT);
			if (DD.DEBUG) System.out.println("DD:run: done database, try import database from: "+db_to_import);
			if (db_to_import != null) {
				boolean imported = net.ddp2p.common.util.DB_Import.import_db(db_to_import, Application.DELIBERATION_FILE);
				if(_DEBUG) System.out.println("DD:run: done database, importing database from: "+db_to_import+" result="+imported);
				int q=0;
				if (!imported) {
					q = Application_GUI.ask(__("Do you want to attempt import on the next startups?"),
							__("Want to be asked to import in the future?"),
							JOptionPane.YES_NO_OPTION);
				} else {
				}
				if (imported || (q != 0)) {
					if(_DEBUG) System.out.println("DD:run: done database, will clean "+DD.APP_DB_TO_IMPORT);
					Application.setDB(selected_db); 
					DD.setAppText(DD.APP_DB_TO_IMPORT, null);
				}
				Application_GUI.warning(__("Result trying to import data from old database:")+"\n"+db_to_import+"\n result="+(imported?__("Success"):__("Failure")), __("Importing database!"));
			}
			run_instance(selected_db, pGID, controlIP, askIdentityFirst);
		}
		public static void run_instance(DBInterface selected_db, String pGID, String controlIP, boolean askIdentityFirst) throws NumberFormatException, P2PDDSQLException {
			Application.setDB(selected_db);
			if(DD.DEBUG) System.out.println("DD:run: done database, done import databases");
			DDTranslation.db=Application.getDB();
			try {
				DD.load_listing_directories();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
			if (pGID != null && !"".equals(pGID.trim())) {
				Application.requestedPeerGID = pGID.trim();
			}
			Identity.init_Identity(true, true, false); 
	    	if (DD.DEBUG) System.err.println("DD:main: identity");
	    	/**
	    	 * Should not quit since the peer GID may have been passed as parameter
	    	 */
			Identity peer_identity_id = Application.getCurrent_Peer_ID();
			if (peer_identity_id != null) pGID = peer_identity_id.getPeerGID();
			if (DD.DEBUG) System.out.println("MainFrame: run: My peer GID: "+pGID);
			if (DD.DEBUG) System.out.println("MainFrame: run: init languages");		
			initTranslationSettings(Identity.getCurrentConstituentIdentity());
			if (controlIP != null) new RPC(controlIP).start();
			if (DD.GUI) {
				if(DD.DEBUG) System.out.println("DD:run: start GUI");		
				if (askIdentityFirst) {
					askIdentity(); 
				}
				createAndShowGUI();
			}
			if(DD.DEBUG) System.out.println("DD:run: start threads");
			new StartUpThread_GUI().start();
			DD.setAppText(DD.LAST_SOFTWARE_VERSION, DD.VERSION);
		}
		private static void initTranslationSettings(Identity const_identity_id) throws P2PDDSQLException {
	    	DDTranslation.preferred_languages = get_preferred_languages();
	    	if (const_identity_id != null) {
	    		DDTranslation.organizationID = D_Organization.getLIDbyGID(const_identity_id.organizationGID);
	    		DDTranslation.constituentID = D_Constituent.getLIDFromGID(const_identity_id.getConstituentGID(), DDTranslation.organizationID);
		    	DDTranslation.authorship_charset = const_identity_id.authorship_charset;
		    	if (const_identity_id.authorship_lang != null)
		    		DDTranslation.authorship_lang = new Language(const_identity_id.authorship_lang);
	    	}
	    	if (DDTranslation.authorship_charset == null)
		    	DDTranslation.authorship_charset = DD.get_authorship_charset();
	    	if (DDTranslation.authorship_lang == null)
		    	DDTranslation.authorship_lang = DD.get_authorship_lang();
	    	DDTranslation.preferred_charsets = DD.get_preferred_charsets();
		}
}
