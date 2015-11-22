package net.ddp2p.widgets.updates;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Application_GUI;
import net.ddp2p.common.config.DD;
import net.ddp2p.common.recommendationTesters.RecommendationOfTestersSender;
import net.ddp2p.common.recommendationTesters.RecommenderOfTesters;
import net.ddp2p.common.util.DBInterface;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.widgets.components.GUI_Swing;
import net.ddp2p.widgets.dir_management.DirPanel;
import net.ddp2p.widgets.updatesKeys.*;
import static net.ddp2p.common.util.Util.__;
public class UpdatesPanel extends JPanel implements ActionListener, FocusListener {
    private static final boolean DEBUG = false;
	String numberString=__("Number");
    String percentageString=__("Percentage");
    String manualRatingString=__("Manual Rating");
    String autoRatingString=__("Automatic Rating");
    public JTextField numberTxt = new JTextField(3);
    public JTextField percentageTxt = new JTextField(3);
    public JRadioButton numberButton = new JRadioButton(numberString);
    public JRadioButton percentageButton = new JRadioButton(percentageString);
    public JRadioButton autoRatingButton = new JRadioButton(autoRatingString);
    public JRadioButton manualRatingButton = new JRadioButton(manualRatingString);
	public JCheckBox absoluteCheckBox =  new JCheckBox();
	private UpdatesKeysTable updateKeysTable;
	public UpdatesPanel() {
    	super( new GridLayout(2,1));
    	numberTxt.setText(""+DD.UPDATES_TESTERS_THRESHOLD_COUNT_DEFAULT);
    	percentageTxt.setText(""+DD.UPDATES_TESTERS_THRESHOLD_WEIGHT_DEFAULT);
    	init();
    	GUI_Swing.panelUpdates = this;
    }
   public JPanel buildTesterControlsPanel(){
		JButton recalculateTestersRating = new JButton(__("Recalculate Testers Rating"));
		JButton ConsultRecommender = new JButton(__("Consult the Recommender System"));
		JButton sendRecommendations = new JButton(__("Send Recommendations"));
		JPanel testerControls = new JPanel(new BorderLayout());
		testerControls.add(buildThresholdPanel() , BorderLayout.WEST);
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(recalculateTestersRating);
		recalculateTestersRating.addActionListener(this);
		recalculateTestersRating.setActionCommand("recalculate");
		buttonsPanel.add(sendRecommendations);
		buttonsPanel.setBackground(Color.DARK_GRAY);
		sendRecommendations.addActionListener(this);
		sendRecommendations.setActionCommand("sendRecommendations");
		buttonsPanel.add(ConsultRecommender);
		buttonsPanel.setBackground(Color.DARK_GRAY);
		ConsultRecommender.addActionListener(this);
		ConsultRecommender.setActionCommand("Consult");
		testerControls.add(buttonsPanel, BorderLayout.EAST);
		return testerControls;
	}
   public JPanel buildAutoManualPanel() {
	   boolean autoSeleced = true;
	   try {
		autoSeleced = DD.getAppBoolean(DD.AUTOMATIC_TESTERS_RATING_BY_SYSTEM);
	} catch (P2PDDSQLException e) {
		e.printStackTrace();
	}  
   	   JLabel ratingL = new JLabel("Testers Rating  : ");
   	   ratingL.setFont(new Font("Times New Roman",Font.BOLD,14));
       manualRatingButton.setMnemonic(KeyEvent.VK_M);
       manualRatingButton.setActionCommand(manualRatingString);
       manualRatingButton.setSelected(!autoSeleced);
       manualRatingButton.addActionListener(this);
       manualRatingButton.setFont(new Font(null,Font.BOLD,12));
       JLabel spaceL = new JLabel("       ");
       JLabel space2L = new JLabel("       ");
       autoRatingButton.setMnemonic(KeyEvent.VK_A);
       autoRatingButton.setActionCommand(autoRatingString);
       autoRatingButton.setSelected(autoSeleced);
       autoRatingButton.setFont(new Font(null,Font.BOLD,12));
       autoRatingButton.addActionListener(this);
       JPanel autoManualPanel = new JPanel();
       autoManualPanel.add(ratingL);
       autoManualPanel.add(autoRatingButton);
       autoManualPanel.add(spaceL);   
       autoManualPanel.add(manualRatingButton);
       autoManualPanel.add(space2L);
       ButtonGroup group = new ButtonGroup();
       group.add(manualRatingButton);
       group.add(autoRatingButton);
   	JPanel autoManualPanel2 = new JPanel(new BorderLayout());
   	autoManualPanel2.add(autoManualPanel,BorderLayout.WEST );
   	return autoManualPanel2;
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
    	percentageTxt.addActionListener(this);
    	percentageTxt.addFocusListener(this);
    	percentageTxt.setActionCommand(percentageString);
    	percentageTxt.setEnabled(false);
    	thresholdPanel.add(percentageButton);
    	thresholdPanel.add(percentageTxt);
    	thresholdPanel.add(percentageL);
        ButtonGroup group = new ButtonGroup();
        group.add(numberButton);
        group.add(percentageButton);
    	JPanel thresholdPanel2 = new JPanel(new BorderLayout());
    	thresholdPanel2.add(thresholdPanel,BorderLayout.WEST );
    	return thresholdPanel2;
    }
    public void init(){
        JPanel updatePanel = new JPanel(new BorderLayout());
        JLabel updateMirrorsTitleL = new JLabel(" Update Mirrors Preferences ");
        updateMirrorsTitleL.setFont(new Font("Times New Roman",Font.BOLD,20));
        updateMirrorsTitleL.setHorizontalAlignment(SwingConstants.CENTER);
        updateMirrorsTitleL.setVerticalAlignment(SwingConstants.CENTER);
        JPanel mirrorsTitilePanel = new JPanel(new BorderLayout());
        mirrorsTitilePanel.add(new JPanel(), BorderLayout.NORTH);
        mirrorsTitilePanel.add(updateMirrorsTitleL);
        mirrorsTitilePanel.add(new JPanel(), BorderLayout.SOUTH);
    	updatePanel.add(mirrorsTitilePanel,BorderLayout.NORTH );
    	UpdatesTable updateTable = new UpdatesTable(this);
    	JPanel updateTablePanel = new JPanel(new BorderLayout());
		updateTablePanel.add(updateTable.getTableHeader(),BorderLayout.NORTH);
		updateTablePanel.add(updateTable.getScrollPane());
        updatePanel.add(updateTablePanel);
        updatePanel.add(new JPanel(), BorderLayout.SOUTH);
        this.add(updatePanel);
        JPanel updateKeysPanel = new JPanel(new BorderLayout());
        JLabel updateKeysTitleL = new JLabel(" Testers Preferences ");
        updateKeysTitleL.setHorizontalAlignment(SwingConstants.CENTER);
        updateKeysTitleL.setVerticalAlignment(SwingConstants.CENTER);
        JPanel titilePanel = new JPanel(new BorderLayout());
        titilePanel.add(new JPanel(), BorderLayout.NORTH);
        titilePanel.add(updateKeysTitleL);
        titilePanel.add(buildAutoManualPanel(), BorderLayout.SOUTH);
    	updateKeysTitleL.setFont(new Font("Times New Roman",Font.BOLD,20));
    	updateKeysPanel.add(titilePanel,BorderLayout.NORTH );
        updateKeysTable = new UpdatesKeysTable();
    	JPanel updateKeysTablePanel = new JPanel(new BorderLayout());
		updateKeysTablePanel.add(updateKeysTable.getTableHeader(),BorderLayout.NORTH);
		updateKeysTablePanel.add(updateKeysTable.getScrollPane());
        updateKeysPanel.add(updateKeysTablePanel );
        updateKeysPanel.add(buildTesterControlsPanel(), BorderLayout.SOUTH);
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
       	if(txtType.equals(numberString)){
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
        		if(r.getActionCommand().equals(autoRatingString)&& r.isSelected()){
        			DD.setAppBoolean(DD.AUTOMATIC_TESTERS_RATING_BY_SYSTEM, true);
        			updateKeysTable.setColumnBackgroundColor(Color.GRAY);
        			refreshRecommendations();
        			updateKeysTable.getModel().update(null, null);
        			updateKeysTable.repaint();
        		}
        		if(r.getActionCommand().equals(manualRatingString)&& r.isSelected()){
        			DD.setAppBoolean(DD.AUTOMATIC_TESTERS_RATING_BY_SYSTEM, false);
        			updateKeysTable.setColumnBackgroundColor(Color.WHITE);
        			updateKeysTable.getModel().update(null, null);
        		}
        	}
        	if(e.getSource() instanceof JTextField){
        		handleTxtFiled(e.getActionCommand());	
        	}
        	if(e.getSource() instanceof JButton){
        		JButton b = (JButton)e.getSource();
        		if(b.getActionCommand().equals("Consult")){
        			showConsultingPanel();
        		}else if(b.getActionCommand().equals("recalculate")){
        			refreshRecommendations();
        		}else if(b.getActionCommand().equals("sendRecommendations")){
        			sendRecommendations();
        		}
        	}
        }
    	private void sendRecommendations() {
    		if(RecommenderOfTesters.knownTestersList_global == null || RecommenderOfTesters.usedTestersList_global == null){
				Application_GUI.warning("You need to click on recalculate ratings first!!", "No Data to Show");
				return;
			}
    		RecommendationOfTestersSender.announceRecommendation(RecommenderOfTesters.knownTestersList_global, RecommenderOfTesters.usedTestersList_global);
		}
		private void refreshRecommendations() {
			if (RecommenderOfTesters.runningRecommender == null) {
				Application_GUI.warning("Recommender Process Not Started Yet!", "Failure to Refresh Recommender Data!");
				return;
			}
			RecommenderOfTesters.runningRecommender.recommendTesters();
			updateKeysTable.getModel().update(null, null);
			updateKeysTable.repaint();
		}
		private void showConsultingPanel() {
			if(RecommenderOfTesters.knownTestersList_global == null){
				Application_GUI.warning("You need to click on recalculate ratings first!!", "No Data to Show");
				return;
			}
			TestersListsTable knownTestersTable = new TestersListsTable(new TestersListsModel(RecommenderOfTesters.knownTestersList_global)) ;
			TestersListsTable usedTestersTable = new TestersListsTable(new TestersListsModel(RecommenderOfTesters.usedTestersList_global)) ;
			RecommendationsTable recommendationsTable = new RecommendationsTable(new RecommendationsModel(RecommenderOfTesters.scoreMatrix_global, RecommenderOfTesters.sourcePeers_global, RecommenderOfTesters.receivedTesters_global)) ;
			ConsultingRecommendationsPanel p = new ConsultingRecommendationsPanel(knownTestersTable, usedTestersTable, recommendationsTable);
			JOptionPane.showMessageDialog(null,p,"Consulting Recommendations", JOptionPane.DEFAULT_OPTION, null);
    	}
		public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.setDB(new DBInterface(Application.DEFAULT_DELIBERATION_FILE));
			UpdatesPanel updatePanel = new UpdatesPanel();
			frame.setContentPane(updatePanel);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setSize(800,300);
			frame.setVisible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		public JTable getTestersTable() {
			return this.updateKeysTable;
		}
}
