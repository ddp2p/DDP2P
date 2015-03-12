package widgets.updates;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;

import util.P2PDDSQLException;

import config.Application;
import config.DD;
import util.DBInterface;
import static util.Util._;
import widgets.updatesKeys.*;

public class UpdatesPanel extends JPanel implements ActionListener, FocusListener {
    String numberString=_("Number");
    String percentageString=_("Percentage");
    public JTextField numberTxt = new JTextField(3);
    public JTextField percentageTxt = new JTextField(3);
    public JRadioButton numberButton = new JRadioButton(numberString);
    public JRadioButton percentageButton = new JRadioButton(percentageString);
	public JCheckBox absoluteCheckBox =  new JCheckBox();
	private UpdatesKeysTable updateKeysTable;
    public UpdatesPanel() {
    	super( new GridLayout(2,1));// hold two tables (UpdateTatble+QualitiesTable)
    	numberTxt.setText(""+DD.UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT);
    	percentageTxt.setText(""+DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT);
    	init();
    	Application.panelUpdates = this;
    }
    public JPanel buildThresholdPanel() {
    
    	JLabel thresholdL = new JLabel("Threshold  : ");
    	thresholdL.setFont(new Font("Times New Roman",Font.BOLD,14));
    	
        numberButton.setMnemonic(KeyEvent.VK_N);
        numberButton.setActionCommand(numberString);
        numberButton.setSelected(true);
        numberButton.addActionListener(this);
        numberButton.setFont(new Font(null,Font.BOLD,12));
        JLabel spaceL = new JLabel("       ");
//    	numberL.setFont(new Font(null,Font.BOLD,12));
    	
        numberTxt.addActionListener(this);
        numberTxt.addFocusListener(this);
        numberTxt.setActionCommand(numberString);
    	JPanel thresholdPanel = new JPanel();
    	
    	thresholdPanel.add(thresholdL);
    	thresholdPanel.add(numberButton);
    	thresholdPanel.add(numberTxt);
    	thresholdPanel.add(spaceL);
    	
    	
        percentageButton.setMnemonic(KeyEvent.VK_P);
        percentageButton.setActionCommand(percentageString);
        percentageButton.addActionListener(this);
        
        JLabel percentageL = new JLabel(" %");
//    	percentageL.setFont(new Font(null,Font.BOLD,12));
    	
    	percentageTxt.addActionListener(this);
    	percentageTxt.addFocusListener(this);
    	percentageTxt.setActionCommand(percentageString);
    	percentageTxt.setEnabled(false);
    	thresholdPanel.add(percentageButton);
    	thresholdPanel.add(percentageTxt);
    	thresholdPanel.add(percentageL);
    	
    	//Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(numberButton);
        group.add(percentageButton);

    	
    	JPanel thresholdPanel2 = new JPanel(new BorderLayout());
    	thresholdPanel2.add(thresholdPanel,BorderLayout.WEST );
    	return thresholdPanel2;
    }
    public void init(){
        JPanel updatePanel = new JPanel(new BorderLayout());
    	updatePanel.add(buildThresholdPanel(),BorderLayout.NORTH );
    	
    	UpdatesTable updateTable = new UpdatesTable(this);
    	JPanel updateTablePanel = new JPanel(new BorderLayout());
		updateTablePanel.add(updateTable.getTableHeader(),BorderLayout.NORTH);
		updateTablePanel.add(updateTable.getScrollPane());
        updatePanel.add(updateTablePanel);
        
        updatePanel.add(new JPanel(), BorderLayout.SOUTH);
        this.add(updatePanel);
        
        
        JPanel updateKeysPanel = new JPanel(new BorderLayout());
        JLabel updateKeysTitleL = new JLabel(" Update Keys Information ");
        updateKeysTitleL.setHorizontalAlignment(SwingConstants.CENTER);
        updateKeysTitleL.setVerticalAlignment(SwingConstants.CENTER);
        JPanel titilePanel = new JPanel(new BorderLayout());
        titilePanel.add(new JPanel(), BorderLayout.NORTH);
        titilePanel.add(updateKeysTitleL);
        titilePanel.add(new JPanel(), BorderLayout.SOUTH);
    	updateKeysTitleL.setFont(new Font("Times New Roman",Font.BOLD,20));
    	updateKeysPanel.add(titilePanel,BorderLayout.NORTH );
    	
        updateKeysTable = new UpdatesKeysTable();
    	JPanel updateKeysTablePanel = new JPanel(new BorderLayout());
		updateKeysTablePanel.add(updateKeysTable.getTableHeader(),BorderLayout.NORTH);
		updateKeysTablePanel.add(updateKeysTable.getScrollPane());
        updateKeysPanel.add(updateKeysTablePanel );
        updateKeysPanel.add(new JPanel(), BorderLayout.SOUTH);
        this.add(updateKeysPanel);
            
    }
    @Override
    public void focusGained(FocusEvent e) {
     
    }
    @Override
    public void focusLost(FocusEvent e) {
        if( e.getSource().equals(numberTxt))
     	   handleTxtFiled(numberString);
        if( e.getSource().equals(percentageTxt))
           handleTxtFiled(percentageString);
    }
    public void handleTxtFiled(String txtType){
    //	System.out.println("e.getSource() instanceof JTextField : "+ numberTxt.getText());
       	if(txtType.equals(numberString)){
       		// System.out.println("numberTxt.getText() : "+ numberTxt.getText());
    		try {
    			String text = numberTxt.getText();
 				try{if(text != null)text = ""+Integer.parseInt(text);}
				catch(Exception e){numberTxt.setText(text=""+DD.UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT);};
				if(text == null) text = ""+DD.UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT;
    			DD.setAppTextNoSync(DD.UPDATES_TESTERS_THRESHOLD_COUNT_VALUE, text);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return;
			}
    	}
       	if(txtType.equals(percentageString)){
      // 		System.out.println("percentageTxt.getText() : "+percentageTxt.getText());
    		try {
    			String text = percentageTxt.getText();
 				try{if(text != null)text = ""+Float.parseFloat(text);}
				catch(Exception e){percentageTxt.setText(text=""+DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT);};
				if(text == null) text = ""+DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT;
				DD.setAppTextNoSync(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_VALUE, text);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
				return;
			}
    	}  	
    }
        @Override
        public void actionPerformed(ActionEvent e) {
        	if(e.getSource() instanceof JRadioButton){
        		JRadioButton r = (JRadioButton)e.getSource();
        		if(r.getActionCommand().equals(numberString) && r.isSelected()){
        			numberTxt.setEnabled(true);
        			percentageTxt.setEnabled(false);
        			DD.setAppBoolean(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT, false);
        		}
        		if(r.getActionCommand().equals(percentageString)&& r.isSelected()){
        			numberTxt.setEnabled(false);
        			percentageTxt.setEnabled(true);
        			DD.setAppBoolean(DD.UPDATES_TESTERS_THRESHOLD_WEIGHT, true);
        		}
        	}
        	if(e.getSource() instanceof JTextField){
        		handleTxtFiled(e.getActionCommand());
        		
        	}
        }
    	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
			UpdatesPanel updatePanel = new UpdatesPanel();
			frame.setContentPane(updatePanel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setSize(800,300);
			frame.setVisible(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		public JTable getTestersTable() {
			return this.updateKeysTable;
		}
    
}