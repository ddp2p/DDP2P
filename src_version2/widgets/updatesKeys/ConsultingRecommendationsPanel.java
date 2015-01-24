package widgets.updatesKeys;

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

import javax.swing.ImageIcon;
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

import recommendationTesters.RecommenderOfTesters;

import config.Application;
import util.DBInterface;
import data.D_Tester;
import widgets.updatesKeys.*;
import widgets.app.DDIcons;

public class ConsultingRecommendationsPanel extends JPanel implements ActionListener {
	
	TestersListsTable knownTestersTable;
	TestersListsTable usedTestersTable;
	RecommendationsTable recommendationsTable;
    public ConsultingRecommendationsPanel(TestersListsTable knownTestersTable, TestersListsTable usedTestersTable, RecommendationsTable recommendationsTable) {
    	super (new BorderLayout());
    	this.knownTestersTable = knownTestersTable;
    	this.usedTestersTable = usedTestersTable;
    	this.recommendationsTable = recommendationsTable;
    	init();
    }
    public JPanel titlePanel(String title){
    	JLabel titleL = new JLabel(title);
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
    	bodyPanel2.setSize(100, 100);
    	GridBagConstraints knownTestersTableConstraints = new GridBagConstraints();
        GridBagConstraints usedTestersTableConstraints = new GridBagConstraints();
        GridBagConstraints recommendationsTableConstraints = new GridBagConstraints();
        usedTestersTableConstraints.anchor = GridBagConstraints.WEST;
        usedTestersTableConstraints.gridwidth = 1;
        usedTestersTableConstraints.weightx=0.0; // fixed width
        knownTestersTableConstraints.anchor = GridBagConstraints.WEST;
        knownTestersTableConstraints.gridwidth = 1;//GridBagConstraints.REMAINDER;
        knownTestersTableConstraints.weightx=0.0; //Give as much space as possible
        knownTestersTableConstraints.insets = new Insets(1, 15, 1, 15); //padding
        
        recommendationsTableConstraints.anchor = GridBagConstraints.WEST;
        recommendationsTableConstraints.gridwidth = GridBagConstraints.REMAINDER;
        recommendationsTableConstraints.weightx=0.0; //Give as much space as possible
        recommendationsTableConstraints.insets = new Insets(1, 15, 1, 15); //padding
        
        knownTestersTableConstraints.gridx = 0; knownTestersTableConstraints.gridy = 0;
		bodyPanel2.add(this.knownTestersTable.getScrollPane(), knownTestersTableConstraints);
		usedTestersTableConstraints.gridx = 1; usedTestersTableConstraints.gridy = 0;
		bodyPanel2.add(this.usedTestersTable.getScrollPane(), usedTestersTableConstraints);	
		recommendationsTableConstraints.gridx = 2; recommendationsTableConstraints.gridy = 0;
		bodyPanel2.add(this.recommendationsTable.getScrollPane(), recommendationsTableConstraints);
			
    	//return bodyPanel2;
		JPanel p = new JPanel();
		JPanel pknownTestersTable = new JPanel(new BorderLayout());
		pknownTestersTable.add(titlePanel("Known Testers"),BorderLayout.NORTH);
		pknownTestersTable.add(knownTestersTable.getScrollPane(), BorderLayout.CENTER);
		p.add(pknownTestersTable);
		
		
		
		JPanel pUsedTestersTable = new JPanel(new BorderLayout());
		pUsedTestersTable.add(titlePanel("Used Testers"),BorderLayout.NORTH);
		pUsedTestersTable.add(usedTestersTable.getScrollPane(), BorderLayout.CENTER);
		p.add(pUsedTestersTable);
		
		JPanel pRecommendationsTable = new JPanel(new BorderLayout());
		pRecommendationsTable.add(titlePanel("Recommendations Table"),BorderLayout.NORTH);
		pRecommendationsTable.add(recommendationsTable.getScrollPane(), BorderLayout.CENTER);
		p.add(pRecommendationsTable);
		
		return p;
	}
    
    public JPanel details(){
    	ImageIcon equation=null; 
		try{equation= DDIcons.getImageIconFromResource("equation.png", "Equation used for auto-calculating of the received ratings from nieboughrs");}catch(Exception e){e.printStackTrace();}
		JPanel p = new JPanel(new BorderLayout());
		JPanel p1 = new JPanel(new GridLayout(4,1));
		JPanel p2 = new JPanel(new GridLayout(2,1));
		p1.add(new JLabel("   k = "+RecommenderOfTesters.k_DIVERSITY_FACTOR_AS_TRADEOFF_AGAINST_PROXIMITY));
		p1.add(new JLabel("   n = "+RecommenderOfTesters.N_MAX_NUMBER_OF_TESTERS));
		p1.add(new JLabel("   f = "+RecommenderOfTesters.f_AMORTIZATION_OF_SCORE_APPLIED_AT_EACH_TRANSFER_BETWEEN_USERS));
		p1.add(new JLabel("   Pr = "+RecommenderOfTesters.Pr_PROBABILITY_OF_REPLACING_A_USED_TESTER_WITH_A_HIGHER_SCORED_ONE));
		p1.setBackground(new Color(181,174,165));
		//p2.setBackground(new Color(155,144,131));
		p.setBackground(new Color(155,144,131));
		p.add(p1, BorderLayout.EAST);
		//p.add(p2);
		p.add(new JLabel(equation));
		return p;
    }
    public void init(){
        JPanel testerPanel = new JPanel(new BorderLayout());
       // this.setSize(100, 100);
    	testerPanel.add(titlePanel("Consulting Recommendations"),BorderLayout.NORTH );
    	testerPanel.add(bodyPanel(),BorderLayout.CENTER );
    	testerPanel.add(details(),BorderLayout.SOUTH );
    	this.add(testerPanel);
            
    }
        @Override
	public void actionPerformed(ActionEvent e) {

	}
  
    
}