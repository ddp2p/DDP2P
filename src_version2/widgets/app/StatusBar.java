package widgets.app;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;

import hds.GUIStatusHistory;
import static util.Util.__;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import config.Application_GUI;
import config.ConstituentListener;
import config.DD;
import config.MotionsListener;
import config.OrgListener;
import config.PeerListener;
import data.D_Constituent;
import data.D_Motion;
import data.D_Organization;
import data.D_Peer;
import util.P2PDDSQLException;
import util.Util;
import widgets.components.DDLanguageSelector;

public class StatusBar extends JPanel implements OrgListener, MotionsListener, MouseListener, PeerListener, ConstituentListener, ActionListener{
	GUIStatusHistory status;
	JLabel peer_me;
	JLabel peer_selected;
	JLabel identity;
	JLabel organization;
	JLabel constituent_me;
	JLabel constituent_selected;
	JLabel motion;
	DDLanguageSelector language;
	
	final static int PEER_ME = 0;
	final static int PEER_SELECTED = 1;
	final static int IDENTITY = 2;
	final static int ORGANIZATION = 3;
	final static int CONSTITUENT_ME = 4;
	final static int CONSTITUENT_SELECTED = 5;
	final static int MOTION = 6;
	final static int LANGUAGE = 7;
	final static int AVAILABLE = 8;
	private static final String STATUS_BAR = "STATUS_BAR";
	private static final boolean _DEBUG = true;
	private static final boolean DEBUG = false;
	String[] available_labels;
	ImageIcon[] available_icons;
	Component[] components;
	ArrayList<Integer> shown = new ArrayList<Integer>();
	public StatusBar(GUIStatusHistory status) {
		super(new FlowLayout(FlowLayout.LEADING, 10, 0));
		//this.setMinimumSize(new Dimension(100,100));
		this.status = status;
		available_labels = new String[]{__("Myself (Peer)"),__("Selected Peer"),__("Identity"),__("Organization"),
				__("Myself (Constituent)"),__("Selected Constituent"),__("Motion"),__("Language")};
		available_icons = new ImageIcon[AVAILABLE];
		components = new Component[AVAILABLE];
		for(int k=0; k<AVAILABLE; k++){
			switch(k){
			case PEER_ME:
				available_icons[k] = DDIcons.getPeerMeImageIcon(available_labels[k]);
				break;
			case PEER_SELECTED:
				available_icons[k] = DDIcons.getPeerSelImageIcon(available_labels[k]);
				break;
			case ORGANIZATION:
				available_icons[k] = DDIcons.getOrgImageIcon(available_labels[k]);
				break;
			case MOTION:
				available_icons[k] = DDIcons.getMotImageIcon(available_labels[k]);
				break;
			case CONSTITUENT_ME:
				available_icons[k] = DDIcons.getConImageIcon(available_labels[k]);
				break;
			default:
				available_icons[k] = DDIcons.getStaImageIcon(available_labels[k]); // ideally have appropriate icons for each entry
			}
		}
		if (!set_stored_shown())
			set_default_shown();
		
		init_components();
		design();
		connectWidget();
		addMouseListener(this);
		
	}
	public void connectWidget() {
		MainFrame.status.addPeerMeStatusListener(this);
		MainFrame.status.addPeerSelectedStatusListener(this);
		MainFrame.status.addOrgStatusListener(this);
		MainFrame.status.addConstituentMeStatusListener(this);
		MainFrame.status.addConstituentSelectedStatusListener(this);
		MainFrame.status.addMotionStatusListener(this);
	}
	boolean set_stored_shown(){
		String stored_shown = null;
		try {
			stored_shown = DD.getExactAppText(STATUS_BAR);
		} catch (P2PDDSQLException e) {e.printStackTrace(); return false;}
		if(stored_shown != null){
			String[] _shown = stored_shown.split(Pattern.quote(":"));
			for(String s : _shown){shown.add(new Integer(s));}
			return true;
		}
		return false;
	}
	private void saveConfiguration() {
		String save = Util.concat(shown, ":", null);
		try {
			DD.setAppTextNoSync(STATUS_BAR, save);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	void set_default_shown(){
		shown.add(new Integer(ORGANIZATION));
		shown.add(new Integer(CONSTITUENT_ME));
		shown.add(new Integer(MOTION));
		shown.add(new Integer(LANGUAGE));
	}
	JButton back, forward;
	void init_components() {
		components[PEER_ME]= peer_me = new JLabel(available_icons[PEER_ME]); peer_me.setToolTipText(available_labels[PEER_ME]); //peer_me.setIcon(available_icons[PEER_ME]);
		components[PEER_SELECTED]= peer_selected = new JLabel(available_icons[PEER_SELECTED]); peer_selected.setToolTipText(available_labels[PEER_SELECTED]); //peer_selected.setIcon(available_icons[PEER_SELECTED]);
		components[IDENTITY]= identity = new JLabel(available_icons[IDENTITY]); identity.setToolTipText(available_labels[IDENTITY]);
		components[ORGANIZATION]= organization = new JLabel(available_icons[ORGANIZATION]); organization.setToolTipText(available_labels[ORGANIZATION]);
		components[CONSTITUENT_ME]= constituent_me = new JLabel(available_icons[CONSTITUENT_ME]); constituent_me.setToolTipText(available_labels[CONSTITUENT_ME]);
		components[CONSTITUENT_SELECTED]= constituent_selected = new JLabel(available_icons[CONSTITUENT_SELECTED]); constituent_selected.setToolTipText(available_labels[CONSTITUENT_SELECTED]);
		components[MOTION]= motion =new JLabel(available_icons[MOTION]); motion.setToolTipText(available_labels[MOTION]);
		components[LANGUAGE]= language =new DDLanguageSelector();
		
		this.add(back=new JButton(DDIcons.getBackImageIcon(__("Back"))));
		this.add(forward=new JButton(DDIcons.getForwImageIcon(__("Forward"))));
		back.setToolTipText(__("Back"));
		forward.setToolTipText(__("Forward"));
		back.setMargin(new Insets(0,0,0,0));
		forward.setMargin(new Insets(0,0,0,0));
		back.setActionCommand("BACK");
		forward.setActionCommand("FORWARD");
		back.addActionListener(this);
		forward.addActionListener(this);
		for(Component c:components) this.add(c);
		for(Component c:components) c.addMouseListener(this);
	}
	void closeAll(){
		for(Component c:components) c.setVisible(false);;
	}
	void design(){
		//make all invisible
		closeAll(); //this.removeAll();
		for(Integer i : shown){
			components[i.intValue()].setVisible(true);
		}
	}
	@Override
	public void motion_update(String motID, int col, D_Motion d_motion) {
		String text;
		if(d_motion != null) text = Util.trimmed(d_motion.getMotionTitle().title_document.getDocumentUTFString(), 50);
		else text = "";
		motion.setText(text);
	}
	@Override
	public void motion_forceEdit(String motID) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		String text;
		if(org != null) text = org.name;
		else text = "";
		organization.setText(text);
	}
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void constituentUpdate(D_Constituent c, boolean me, boolean selected) {
		if(DEBUG) System.out.println("StatusBar: update_const set const "+ (c==null?"null":c.getNameFull()));
		String text;
		if(c != null) text = c.getNameFull();
		else text = "";
		if(me) constituent_me.setText(text);
		if(selected) constituent_selected.setText(text);
	}
	@Override
	public void update_peer(D_Peer peer, String my_peer_name, boolean me, boolean selected) {
		if (DEBUG) System.out.println("StatusBar: update_peer set peer "+ my_peer_name+" me="+me+" sel="+selected+" peer="+peer);
		if (DEBUG) Util.printCallPath("here");
		String text;
		if(peer != null) text = Util.trimmed(peer.getName(), 50);
		else text = "";
		if (selected) peer_selected.setText(text);
		if (me) {
			if ((peer != null ) && (peer.getInstance() != null)) text += ":"+peer.getInstance();
			peer_me.setText(text);
		}
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(!e.isPopupTrigger()) return;
		configure(binarySearch(components, e.getSource()));
	}
	@Override
	public void mousePressed(MouseEvent e) {
		if(!e.isPopupTrigger()) return;
		configure(binarySearch(components, e.getSource()));
	}
	private int binarySearch(Component[] c, Object s) {
		//System.out.println("Status source:"+s);
		for(int k=0; k<c.length; k++)
			if(c[k]==s) return k;
		return PEER_ME;
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		if(!e.isPopupTrigger()) return;
		configure(binarySearch(components, e.getSource()));
	}
	public void configure(int def){
		int k = Application_GUI.ask(__("What to Toggle"), __("Toggle the fields:"), available_labels, available_labels[def], null);
		if(k==JOptionPane.CLOSED_OPTION) return;
		if(shown.contains(new Integer(k))) shown.remove(new Integer(k));
		else shown.add(new Integer(k));
		saveConfiguration();
		design();
	}
	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void actionPerformed(ActionEvent a) {
		int lastTab = MainFrame.status.getTab();
		if("BACK".equals(a.getActionCommand())) MainFrame.status.back();
		if("FORWARD".equals(a.getActionCommand())) MainFrame.status.forward();
		int crtTab = MainFrame.status.getTab();
		if ((crtTab != lastTab) && (crtTab > 0))
			MainFrame.tabbedPane.setSelectedIndex(crtTab);
	}
}
