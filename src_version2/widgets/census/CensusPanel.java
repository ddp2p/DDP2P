/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Song Qin
 Author: Song Qin: qsong2008@my.fit.edu
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
/* ------------------------------------------------------------------------- */
package widgets.census;
import static util.Util.__;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import MCMC.RandomNetworkSimulation;
import util.DBInterface;
import util.P2PDDSQLException;
import widgets.app.MainFrame;
import util.P2PDDSQLException;
import config.Application;
import config.Application_GUI;
import config.OrgListener;
import data.D_Organization;
public class CensusPanel extends JPanel implements OrgListener, ActionListener {
	private static final long serialVersionUID = 1L;
	public CensusFuzzy fuzzy_table;//The table widget
	JLabel fuzzyMetric1ThresholdLabel,c3,c10,c11;
	static JSlider naiveMetricThresholdSlider,fuzzyMetric1Panel1thresholdSlider,fuzzyMetric1Panel2thresholdSlider,fuzzyMetric1Panel3thresholdSlider;
	JButton c5;
	private JLabel pcoLabel;
	private JButton simulate;
	private JLabel ncoLabel;
	private JLabel naiveMetrciThresholdLabel;
	private JSlider pcoSlider;
	private JSlider ncoSlider;
	private JLabel org_label = new JLabel(__("No curent org"));
	D_Organization organization;
	public CensusPanel() {
		CensusFuzzy fuzzy_table = new CensusFuzzy();
		this.fuzzy_table = fuzzy_table;
		this.setLayout(new BorderLayout());
		addComponentsToPane();
	}
	public	CensusFuzzyModel getModel(){
		return fuzzy_table.getModel();
	}
	public CensusPanel(CensusFuzzy fuzzy_table) {
		this.fuzzy_table = fuzzy_table;
		this.setLayout(new BorderLayout());
     	org_label.setText(getLabelText());
		addComponentsToPane();
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		return scrollPane;
	}
    private String getLabelText() {
    	if((organization == null) || (organization.name == null)) return __("No current organization!");
		return __("Current Organization:")+" "+organization.name;
	}
    
    /**
     * 
     * @return A control panel for user to collect statistics for fuzzy metric 1
     */
	public JSplitPane fuzzyMetric1Panel1() {//TODO:Add picture description
		 fuzzyMetric1ThresholdLabel=new JLabel("Threshold");
		 fuzzyMetric1Panel1thresholdSlider=new JSlider(JSlider.HORIZONTAL,0,100,0);//Value from min=0 to max=1, initial=0
		 fuzzyMetric1Panel1thresholdSlider.setMajorTickSpacing(10);
		 fuzzyMetric1Panel1thresholdSlider.setPaintTicks(true);
		 Hashtable<Integer,JLabel> labelTable = new Hashtable<Integer,JLabel>();  
		 labelTable.put(new Integer(100), new JLabel("1.0"));
		 labelTable.put(new Integer(90), new JLabel("0.9"));
		 labelTable.put(new Integer(80), new JLabel("0.8"));
		 labelTable.put(new Integer(70), new JLabel("0.7"));
		 labelTable.put(new Integer(60), new JLabel("0.6"));
		 labelTable.put(new Integer(50), new JLabel("0.5"));
		 labelTable.put(new Integer(40), new JLabel("0.4"));
		 labelTable.put(new Integer(30), new JLabel("0.3"));
		 labelTable.put(new Integer(20), new JLabel("0.2"));
		 labelTable.put(new Integer(10), new JLabel("0.1"));
		 labelTable.put(new Integer(0), new JLabel("0.0"));  
		 fuzzyMetric1Panel1thresholdSlider.setLabelTable( labelTable );
		 fuzzyMetric1Panel1thresholdSlider.setPaintLabels(true);
		 //Update the value of threshold in the model
		 fuzzyMetric1Panel1thresholdSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				fuzzy_table.getModel().t1=Double.valueOf(fuzzyMetric1Panel1thresholdSlider.getValue()/100.0);
				System.out.println("fuzzyMetric1Panel1:fuzzy_table.getModel().t:"+fuzzy_table.getModel().t1);
				fuzzy_table.getModel().update(null, null);
			}});
		JPanel controlPanel = new JPanel();
		GroupLayout layout = new GroupLayout(controlPanel);
		controlPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		/**
		 * Figure is as following
		 * c6 
		 * fuzzyMetric1ThresholdLabel fuzzyMetric1Panel1thresholdSlider
		 */
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(fuzzyMetric1ThresholdLabel))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(fuzzyMetric1Panel1thresholdSlider))
		);
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(fuzzyMetric1ThresholdLabel)
						.addComponent(fuzzyMetric1Panel1thresholdSlider))
				);
		final ScalableImagePanel sip=new ScalableImagePanel();
		try {
			sip.loadImage("census/m2.jpg");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		sip.addComponentListener(new ComponentAdapter(){
			public void componentResized(java.awt.event.ComponentEvent evt) {
				sip.scaleImage();
			}
		});
		JScrollPane metricPictureScrollPanel=new JScrollPane(sip);
		sip.setBorder(BorderFactory.createTitledBorder("Description of the usage of this metric"));
		JSplitPane SplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,controlPanel, metricPictureScrollPanel);
		SplitPanel.setOneTouchExpandable(true);
		SplitPanel.setDividerLocation(150);
		return SplitPanel;
	}
	 /**
     * 
     * @return A control panel for user to collect statistics for fuzzy metric 2
     */
	public JSplitPane fuzzyMetric1Panel2() {//TODO:Add picture description
		 fuzzyMetric1ThresholdLabel=new JLabel("Threshold");
		 fuzzyMetric1Panel2thresholdSlider=new JSlider(JSlider.HORIZONTAL,0,100,0);//Value from min=0 to max=1, initial=0
		 fuzzyMetric1Panel2thresholdSlider.setMajorTickSpacing(10);
		 fuzzyMetric1Panel2thresholdSlider.setPaintTicks(true);
		 Hashtable<Integer,JLabel> labelTable = new Hashtable<Integer,JLabel>();  
		 labelTable.put(new Integer(100), new JLabel("1.0"));
		 labelTable.put(new Integer(90), new JLabel("0.9"));
		 labelTable.put(new Integer(80), new JLabel("0.8"));
		 labelTable.put(new Integer(70), new JLabel("0.7"));
		 labelTable.put(new Integer(60), new JLabel("0.6"));
		 labelTable.put(new Integer(50), new JLabel("0.5"));
		 labelTable.put(new Integer(40), new JLabel("0.4"));
		 labelTable.put(new Integer(30), new JLabel("0.3"));
		 labelTable.put(new Integer(20), new JLabel("0.2"));
		 labelTable.put(new Integer(10), new JLabel("0.1"));
		 labelTable.put(new Integer(0), new JLabel("0.0"));  
		 fuzzyMetric1Panel2thresholdSlider.setLabelTable( labelTable );
		 fuzzyMetric1Panel2thresholdSlider.setPaintLabels(true);
		 //Update the value of threshold in the model
		 fuzzyMetric1Panel2thresholdSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				fuzzy_table.getModel().t2=Double.valueOf(fuzzyMetric1Panel2thresholdSlider.getValue()/100.0);
				fuzzy_table.getModel().update(null, null);
			}});
		JPanel controlPanel = new JPanel();
		GroupLayout layout = new GroupLayout(controlPanel);
		controlPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		/**
		 * Figure is as following
		 * c6 
		 * fuzzyMetric1ThresholdLabel fuzzyMetric1Panel2thresholdSlider
		 */
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(fuzzyMetric1ThresholdLabel))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(fuzzyMetric1Panel2thresholdSlider))
		);
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(fuzzyMetric1ThresholdLabel)
						.addComponent(fuzzyMetric1Panel2thresholdSlider))
				);
		final ScalableImagePanel sip=new ScalableImagePanel();
		try {
			sip.loadImage("census/m3.jpg");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		sip.addComponentListener(new ComponentAdapter(){
			public void componentResized(java.awt.event.ComponentEvent evt) {
				sip.scaleImage();
			}
		});
		JScrollPane metricPictureScrollPanel=new JScrollPane(sip);
		sip.setBorder(BorderFactory.createTitledBorder("Description of the usage of this metric"));
		JSplitPane SplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,controlPanel, metricPictureScrollPanel);
		SplitPanel.setOneTouchExpandable(true);
		SplitPanel.setDividerLocation(150);
		return SplitPanel;
	}
	
	 /**
     * 
     * @return A control panel for user to collect statistics for fuzzy metric 3
     */
	public JSplitPane fuzzyMetric1Panel3() {//TODO:Add picture description
		 fuzzyMetric1ThresholdLabel=new JLabel("Threshold");
		 fuzzyMetric1Panel3thresholdSlider=new JSlider(JSlider.HORIZONTAL,0,100,0);//Value from min=0 to max=1, initial=0
		 fuzzyMetric1Panel3thresholdSlider.setMajorTickSpacing(10);
		 fuzzyMetric1Panel3thresholdSlider.setPaintTicks(true);
		 Hashtable<Integer,JLabel> labelTable = new Hashtable<Integer,JLabel>();  
		 labelTable.put(new Integer(100), new JLabel("1.0"));
		 labelTable.put(new Integer(90), new JLabel("0.9"));
		 labelTable.put(new Integer(80), new JLabel("0.8"));
		 labelTable.put(new Integer(70), new JLabel("0.7"));
		 labelTable.put(new Integer(60), new JLabel("0.6"));
		 labelTable.put(new Integer(50), new JLabel("0.5"));
		 labelTable.put(new Integer(40), new JLabel("0.4"));
		 labelTable.put(new Integer(30), new JLabel("0.3"));
		 labelTable.put(new Integer(20), new JLabel("0.2"));
		 labelTable.put(new Integer(10), new JLabel("0.1"));
		 labelTable.put(new Integer(0), new JLabel("0.0"));  
		 fuzzyMetric1Panel3thresholdSlider.setLabelTable( labelTable );
		 fuzzyMetric1Panel3thresholdSlider.setPaintLabels(true);
		 //Update the value of threshold in the model
		 fuzzyMetric1Panel3thresholdSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				fuzzy_table.getModel().t3=Double.valueOf(fuzzyMetric1Panel3thresholdSlider.getValue()/100.0);
				fuzzy_table.getModel().update(null, null);
			}});
		JPanel controlPanel = new JPanel();
		GroupLayout layout = new GroupLayout(controlPanel);
		controlPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		/**
		 * Figure is as following
		 * c6 
		 * fuzzyMetric1ThresholdLabel fuzzyMetric1Panel3thresholdSlider
		 */
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(fuzzyMetric1ThresholdLabel))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(fuzzyMetric1Panel3thresholdSlider))
		);
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(fuzzyMetric1ThresholdLabel)
						.addComponent(fuzzyMetric1Panel3thresholdSlider))
				);
		final ScalableImagePanel sip=new ScalableImagePanel();
		try {
			sip.loadImage("census/m4.jpg");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		sip.addComponentListener(new ComponentAdapter(){
			public void componentResized(java.awt.event.ComponentEvent evt) {
				sip.scaleImage();
			}
		});
		JScrollPane metricPictureScrollPanel=new JScrollPane(sip);
		sip.setBorder(BorderFactory.createTitledBorder("Description of the usage of this metric"));
		JSplitPane SplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,controlPanel, metricPictureScrollPanel);
		SplitPanel.setOneTouchExpandable(true);
		SplitPanel.setDividerLocation(150);
		return SplitPanel;
	}
	
		/**
	 * 
	 * @return
	 */
	public JSplitPane naiveMetricPanel() {//TODO:Add picture description
		pcoLabel=new JLabel("pco");
		 ncoLabel=new JLabel("nco");
		 naiveMetrciThresholdLabel= new JLabel("threshold");
		 pcoSlider=new JSlider(JSlider.HORIZONTAL,1,30,1);
		 ncoSlider=new JSlider(JSlider.HORIZONTAL,1,30,1);
		 naiveMetricThresholdSlider=new JSlider(JSlider.HORIZONTAL,1,30,1);
		 pcoSlider.setMajorTickSpacing(10);
		 pcoSlider.setMinorTickSpacing(1);
		 pcoSlider.setPaintTicks(true);
		 pcoSlider.setPaintLabels(true);
		 pcoSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				fuzzy_table.getModel().pco=Double.valueOf(pcoSlider.getValue());
				fuzzy_table.getModel().nco=Double.valueOf(ncoSlider.getValue());
				fuzzy_table.getModel().t0=Double.valueOf(naiveMetricThresholdSlider.getValue());
				fuzzy_table.getModel().update(null, null);
			}});
		 ncoSlider=new JSlider(JSlider.HORIZONTAL,1,30,1);
		 ncoSlider.setMajorTickSpacing(10);
		 ncoSlider.setMinorTickSpacing(1);
		 ncoSlider.setPaintTicks(true);
		 ncoSlider.setPaintLabels(true);
		 ncoSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				fuzzy_table.getModel().pco=Double.valueOf(pcoSlider.getValue());
				fuzzy_table.getModel().nco=Double.valueOf(ncoSlider.getValue());
				fuzzy_table.getModel().t0=Double.valueOf(naiveMetricThresholdSlider.getValue());
				fuzzy_table.getModel().update(null, null);
			}});
		 naiveMetricThresholdSlider=new JSlider(JSlider.HORIZONTAL,1,30,1);
		 naiveMetricThresholdSlider.setMajorTickSpacing(10);
		 naiveMetricThresholdSlider.setMinorTickSpacing(1);
		 naiveMetricThresholdSlider.setPaintTicks(true);
		 naiveMetricThresholdSlider.setPaintLabels(true);
		 naiveMetricThresholdSlider.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
				fuzzy_table.getModel().pco=Double.valueOf(pcoSlider.getValue());
				fuzzy_table.getModel().nco=Double.valueOf(ncoSlider.getValue());
				fuzzy_table.getModel().t0=Double.valueOf(naiveMetricThresholdSlider.getValue());
				fuzzy_table.getModel().update(null, null);
			}});
		JPanel controlPanel = new JPanel();
		GroupLayout layout = new GroupLayout(controlPanel);
		controlPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
								.addComponent(pcoLabel)
								.addComponent(ncoLabel)
								.addComponent(naiveMetrciThresholdLabel)
								)
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
							.addComponent(pcoSlider)
							.addComponent(ncoSlider)
							.addComponent(naiveMetricThresholdSlider))
		);
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(pcoLabel)
						.addComponent(pcoSlider))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(ncoLabel)
						.addComponent(ncoSlider))
					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
						.addComponent(naiveMetrciThresholdLabel)
						.addComponent(naiveMetricThresholdSlider))
				);	
		final ScalableImagePanel sip=new ScalableImagePanel();
		try {
			sip.loadImage("census/m1.jpg");
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		sip.addComponentListener(new ComponentAdapter(){
			public void componentResized(java.awt.event.ComponentEvent evt) {
				sip.scaleImage();
			}
		});
		JScrollPane metricPictureScrollPanel=new JScrollPane(sip);
		sip.setBorder(BorderFactory.createTitledBorder("Description of the usage of this metric"));
		JSplitPane SplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,controlPanel, metricPictureScrollPanel);
		SplitPanel.setOneTouchExpandable(true);
		SplitPanel.setDividerLocation(150);
		return SplitPanel;
	}
	
	/*
	 * A tab widget for user to specify which metric he will use.
	 */
	public JTabbedPane buildTabbedControlPanel(){
		final JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Naive Metric 1", naiveMetricPanel());//Metric Value=0
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		tabbedPane.addTab("Fuzzy Metric 1", fuzzyMetric1Panel1());//Metric Value=1
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		tabbedPane.addTab("Fuzzy Metric 2", fuzzyMetric1Panel2());//Metric Value=2
		tabbedPane.setMnemonicAt(2, KeyEvent.VK_3);
		tabbedPane.addTab("Fuzzy Metric 3", fuzzyMetric1Panel3());//Metric Value=3
		tabbedPane.setMnemonicAt(3, KeyEvent.VK_4);
		tabbedPane.addChangeListener(new ChangeListener(){
			@Override
			public void stateChanged(ChangeEvent e) {
//				System.out.println("Tab=" + tabbedPane.getSelectedIndex());
				int tabIndex=tabbedPane.getSelectedIndex();
				if(tabIndex==0){
					System.out.println("tabIndex "+tabIndex);
					fuzzy_table.getModel().metric=tabIndex;
					fuzzy_table.getModel().pco=Double.valueOf(pcoSlider.getValue());
					fuzzy_table.getModel().nco=Double.valueOf(ncoSlider.getValue());
					fuzzy_table.getModel().t0=Double.valueOf(naiveMetricThresholdSlider.getValue());
					fuzzy_table.getModel().update(null, null);
				}
				else if(tabIndex==1){
					System.out.println("tabIndex "+tabIndex);
					fuzzy_table.getModel().t1=Double.valueOf(fuzzyMetric1Panel1thresholdSlider.getValue());
					System.out.println("fuzzy_table.getModel().t1+"+fuzzy_table.getModel().t1);
					fuzzy_table.getModel().metric=tabIndex;
					fuzzy_table.getModel().update(null, null);
				}
				else if(tabIndex==2){
					System.out.println("tabIndex "+tabIndex);
					fuzzy_table.getModel().t2=Double.valueOf(fuzzyMetric1Panel2thresholdSlider.getValue());
					fuzzy_table.getModel().metric=tabIndex;
					fuzzy_table.getModel().update(null, null);
				}
				else if(tabIndex==3){
					System.out.println("tabIndex "+tabIndex);
					fuzzy_table.getModel().t3=Double.valueOf(fuzzyMetric1Panel3thresholdSlider.getValue());
					fuzzy_table.getModel().metric=tabIndex;
					fuzzy_table.getModel().update(null, null);
				}
			}
		});
		tabbedPane.setSelectedIndex(0);
		fuzzy_table.getModel().metric=tabbedPane.getSelectedIndex();;
		fuzzy_table.getModel().pco=Double.valueOf(pcoSlider.getValue());
		fuzzy_table.getModel().nco=Double.valueOf(ncoSlider.getValue());
		fuzzy_table.getModel().t0=Double.valueOf(naiveMetricThresholdSlider.getValue());
		fuzzy_table.getModel().update(null, null);
		
		return tabbedPane;
	}
	public CensusPanel addComponentsToPane() {
		JPanel pane = this;
		JSplitPane SplitPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT,fuzzy_table.getScrollPane(), buildTabbedControlPanel());
		SplitPanel.setOneTouchExpandable(true);
		SplitPanel.setDividerLocation(150);
		JPanel top = new JPanel();
		top.add(org_label);
		simulate = new JButton("Simulate");
	
		top.add(simulate);
		simulate.addActionListener(this);
		pane.add(top, BorderLayout.PAGE_START);//Indicate the current used/selected organization
		pane.add(SplitPanel, BorderLayout.CENTER);//Control panel for selecting metrics
		return this;
	}

	public static void main(String[] args) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
		} catch (P2PDDSQLException e) {
		}
		JFrame wg = new CensusFuzzy(-1, -1).getCensusFuzzyFrame();
		wg.pack();
		wg.setVisible(true);
	}
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		organization = org;
    	if(org_label!=null){
    		org_label.setText(getLabelText());
    		//add(org_label, BorderLayout.NORTH);
    	}
    	getModel().orgUpdate(orgID, col, org);
    }
	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == simulate) {
			if(organization==null){
				Application_GUI.warning(__("No organization selected"), __("Failure to simulate"));
				return;
			}
			D_Organization org = MainFrame.status.getSelectedOrg();
			if(org == null){
						Application_GUI.warning(__("Select organization!"), __("Generating data"));
						return;
			}
			String global_organization_id = org.global_organization_ID;
			int sizeActiveHonestConsts = 100;
			int nbAttackersIneligibleIDs_1 = 10;
			int nbAttackersWitnessForIneligible_2 = 20;
			int nbAttackersWitnessAgainstEligible_3 = 10;
			int nbIneligible = 0;
			new RandomNetworkSimulation(global_organization_id,
							sizeActiveHonestConsts,
							nbAttackersIneligibleIDs_1, nbAttackersWitnessForIneligible_2,
							nbAttackersWitnessAgainstEligible_3,
							nbIneligible).start();
		}
	}
}