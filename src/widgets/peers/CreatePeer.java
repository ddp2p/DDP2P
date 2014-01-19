package widgets.peers;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import widgets.components.CipherSelection;
import widgets.components.TranslatedLabel;

import static util.Util._;

@SuppressWarnings("serial")
public class CreatePeer extends JDialog implements ActionListener {
	static final String cOK = "OK";
	static final String cCANCEL = "CANCEL";
	private static final String NAME_CHARACTERS = null;
	private static final String SLOGAN_CHARACTERS = null;
	private static final String EMAIL_CHARACTERS = null;
	GridBagConstraints c = new GridBagConstraints();
	CipherSelection cipherSelection = new CipherSelection();
	JButton button_ok;
	JButton button_cancel;
	private JTextField name;
	private JTextField slogan;
	private boolean valid = false;
	private JTextField email; 
	public PeerInput getData() {
		PeerInput data = new PeerInput();
		data.valid = valid;
		data.name = name.getText();
		data.slogan = slogan.getText();
		data.email = email.getText();
		data.cipherSuite = this.cipherSelection.getSelectedCipherSuite();
		return data;
	}
	public CreatePeer (JFrame parent) {
		super(parent, _("Create Peer"), true);
		init(parent);
		showIt();
	}
	public CreatePeer (JFrame parent, PeerInput initial) {
		super(parent, _("Create Peer"), true);
		init(parent);
		initialize(initial);
		showIt();
	}
	public void showIt(){
	    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	    pack(); 
	    setVisible(true); // this shows it;
	}
	/**
	 * Sets only name, slogan and email (if non-null)
	 * @param initial
	 */
	public void initialize(PeerInput initial) {
		if (initial.name != null) this.name.setText(initial.name);
		if (initial.slogan != null) this.slogan.setText(initial.slogan);
		if (initial.email != null) this.email.setText(initial.email);
	}
	void init(JFrame parent) {
		//JButton bp;
	    if (parent != null) {
	    	Dimension parentSize = parent.getSize(); 
	    	Point p = parent.getLocation(); 
	    	setLocation(p.x + parentSize.width / 4, p.y + parentSize.height / 4);
	    }
	    
	    
		int y = 0;
	    JPanel messagePane = new JPanel();
	    getContentPane().add(messagePane);

	    messagePane.setLayout(new GridBagLayout());
		c.ipadx=10; c.gridx=0; c.gridy=y; c.anchor = GridBagConstraints.WEST; c.fill = GridBagConstraints.HORIZONTAL;
	    messagePane.add(new JLabel(_("Peer Name")), c);
	    c.gridx = 1;
	    messagePane.add(name = new JTextField(NAME_CHARACTERS), c);
	    y++;
	    
	    c.gridx = 0; c.gridy = y;
	    messagePane.add(new JLabel(_("Peer Slogan")), c);
	    c.gridx = 1; c.gridy = y;
	    messagePane.add(slogan = new JTextField(SLOGAN_CHARACTERS), c);
	    y++;
	    
	    c.gridx = 0; c.gridy = y;
	    messagePane.add(new JLabel(_("Email For Verification")), c);
	    c.gridx = 1; c.gridy = y;
	    messagePane.add(email = new JTextField(EMAIL_CHARACTERS), c);
	    y++;
	      
	    c.gridx = 0; c.gridy = y;
	    messagePane.add(new JLabel(_("Cipher-Suit")), c);
	    c.gridx = 1;
	    this.cipherSelection = new CipherSelection();			
	    messagePane.add(cipherSelection, c);
	    y++;

	      
	    JPanel buttonPane = new JPanel();
	    
	    button_ok = new JButton("CREATE");
	    button_ok.setActionCommand(cOK);
	    buttonPane.add(button_ok);
	    button_ok.addActionListener(this);
	    
	    button_cancel = new JButton("CANCEL");
	    button_cancel.setActionCommand(cCANCEL);
	    buttonPane.add(button_cancel);
	    button_cancel.addActionListener(this);
	    
	      
	    getContentPane().add(buttonPane, BorderLayout.SOUTH);
	}
	int getIndex(String[] items, String val){
		for (int i=0; i<items.length; i++) if(items[i].equals(val)) return i;
		return -1;
	}
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == button_ok) valid = true;
		setVisible(false); 
		dispose(); 
	}
}
