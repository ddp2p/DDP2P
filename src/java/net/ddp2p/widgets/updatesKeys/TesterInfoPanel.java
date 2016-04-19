package net.ddp2p.widgets.updatesKeys;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Font;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JSeparator;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.data.D_Tester;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.widgets.updatesKeys.*;
public class TesterInfoPanel extends JPanel implements ActionListener {
	private D_Tester tester;
    public TesterInfoPanel(D_Tester t) {
    	super (new BorderLayout());
    	tester = t;
    	init();
    }
    public JPanel titlePanel(){
    	JLabel titleL = new JLabel("Tester Information");
    	titleL.setFont(new Font("Times New Roman",Font.BOLD,14));
    	titleL.setHorizontalAlignment(SwingConstants.CENTER);
        titleL.setVerticalAlignment(SwingConstants.CENTER);
    	JPanel titlePanel2 = new JPanel(new BorderLayout());
    	titlePanel2.add(titleL );
    	titlePanel2.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.SOUTH);
    	return titlePanel2;
    }
    public JPanel bodyPanel(){
    	JPanel bodyPanel2 = new JPanel(new GridBagLayout());
    	GridBagConstraints fieldConstraints = new GridBagConstraints();
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.gridwidth = 1;
        labelConstraints.weightx=0.0; 
        fieldConstraints.anchor = GridBagConstraints.WEST;
        fieldConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fieldConstraints.weightx=1.0; 
        fieldConstraints.insets = new Insets(1, 1, 1, 1); 
        JLabel nameL = new JLabel("Name: ");
		labelConstraints.gridx = 0; labelConstraints.gridy = 0;
		bodyPanel2.add(nameL, labelConstraints);	
		JTextField nameTxt = new JTextField(tester.name);
		nameTxt.setBackground(new Color(224,224,224));
		nameTxt.setColumns(17);
		fieldConstraints.gridx = 1; fieldConstraints.gridy = 0;
		bodyPanel2.add(nameTxt, fieldConstraints);
		JLabel publicKeyL = new JLabel("Public Key: ");
		labelConstraints.gridx = 0; labelConstraints.gridy = 1;
		bodyPanel2.add(publicKeyL, labelConstraints);	
		JTextField publicKeyTxt = new JTextField(tester.testerGID);
		publicKeyTxt.setBackground(new Color(224,224,224));
	    publicKeyTxt.setColumns(17); 
		fieldConstraints.gridx = 1; fieldConstraints.gridy = 1;
		bodyPanel2.add(publicKeyTxt, fieldConstraints);
		JLabel emailL = new JLabel("Email: ");
		labelConstraints.gridx = 0; labelConstraints.gridy = 2;
		bodyPanel2.add(emailL, labelConstraints);	
		JTextField emailTxt = new JTextField(tester.email);
		emailTxt.setBackground(new Color(224,224,224));
		emailTxt.setColumns(17);
		fieldConstraints.gridx = 1; fieldConstraints.gridy = 2;
		bodyPanel2.add(emailTxt, fieldConstraints);
		JLabel urlL = new JLabel("URL: ");
		labelConstraints.gridx = 0; labelConstraints.gridy = 3;
		bodyPanel2.add(urlL, labelConstraints);	
		JTextField urlTxt = new JTextField(tester.url);
		urlTxt.setBackground(new Color(224,224,224));
		urlTxt.setColumns(17);
		fieldConstraints.gridx = 1; fieldConstraints.gridy = 3;
		bodyPanel2.add(urlTxt, fieldConstraints);
		JLabel descL = new JLabel("Description: ");
		labelConstraints.gridx = 0; labelConstraints.gridy = 4;
		bodyPanel2.add(descL, labelConstraints);	
		JTextArea descTxt = new JTextArea(tester.description,3,16);
		descTxt.setLineWrap(true);
		descTxt.setWrapStyleWord(true);
	    descTxt.setSize(10,10);
	    descTxt.setBackground(new Color(224,224,224));
	    JScrollPane descScl = new JScrollPane(descTxt,ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		fieldConstraints.gridx = 1; fieldConstraints.gridy = 4;
		bodyPanel2.add(descScl, fieldConstraints);
	   bodyPanel2.setBackground(new Color(147,147,147));
    	return bodyPanel2;
    }
    public void init(){
        JPanel testerPanel = new JPanel(new BorderLayout());
    	testerPanel.add(titlePanel(),BorderLayout.NORTH );
    	testerPanel.add(bodyPanel());
    	this.add(testerPanel);
    }
        @Override
	public void actionPerformed(ActionEvent e) {
	}
}
