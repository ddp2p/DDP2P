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
package net.ddp2p.widgets.census;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import net.ddp2p.common.config.Application;
import net.ddp2p.common.config.Identity;
import net.ddp2p.common.config.OrgListener;
import net.ddp2p.common.data.D_Organization;
import net.ddp2p.common.util.DBInfo;
import net.ddp2p.common.util.DBListener;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.common.util.Util;

class Node {
	Node() {
		this.permanent = false;
		this.numFuzzyValue = 0;
		this.sumFuzzyValue = 0.0;
		this.sumPositiveFuzzyValue=0.0;
		this.sumNegativeFuzzyValue=0.0;
		this.numPositiveFuzzyValue=0;
		this.numNegativeFuzzyValue=0;
	}

	double fuzzyValue = 0.0;
	String valueString = "";
	String constituentName;
	Integer constituentID;
	String neighborhoodID;// Not Implemented
	ArrayList<Node> parent;// List of parents
	ArrayList<Node> children;// List of children
	public int positiveWitnessCount = 0;
	public int negativeWintesCount = 0;
	boolean used = false;// Not yet added to the queue
	public double sumPositiveFuzzyValue = 0;
	public double sumNegativeFuzzyValue = 0;
	int isRoot = 0;
	public boolean permanent;
	public int numFuzzyValue;
	public double sumFuzzyValue;
	public double averagedFuzzyValue;
	public int numNegativeFuzzyValue;
	public int numPositiveFuzzyValue;
	public int numMoreThanHalf;
	public int numLessThanHalf;
}

class ConstituentStatictics {
	String value;// Hold value for all metrics
	String threshold;// Pass or Fail

	ConstituentStatictics(String value, String threshold) {
		this.value = value;
		this.threshold = threshold;
	}
}

@SuppressWarnings("serial")
public class CensusFuzzyModel extends AbstractTableModel implements TableModel,
		DBListener, OrgListener {
	private static final boolean DEBUG = false;
	String name;
	int sign; // -1 for negative fuzzy, 0 for tie, 1 for positives
	private long organizationID;
	D_Organization organization;
	boolean debug = false;
	Hashtable<Integer, ConstituentStatictics> viewDataBuffer = new Hashtable<Integer, ConstituentStatictics>();
	final double fuzzyFactor = 0.9;// Parameter from user.
	Integer edgeTable[][];// [2][4]=1 Means constituent 2 positively witnessed 4
							// and
	// [3][4]=2 Means constituent 3 negatively witnessed 4.
	Hashtable<Integer, Node> graphDataBuffer;// A table holding the graph of
												// constituents. A mapping from
												// constituent ID to the node
												// representing the same
												// constituent
	Node r;// root of a graph describing the witness relations
	String[] columnNames = { "Constituent Name", "Value", "Threshold Test" };
	ArrayList tableData[] = new ArrayList[0];
	ArrayList<Integer> fuzzyNodes;// A list of nodes that has fuzzy values
	// User Parameter
	double positiveWitWeight = 1;
	double negativeWitWeight = 1;
	int metric = 0;
	protected Double pco = new Double(0);
	protected Double nco = new Double(0);
	protected Double t0 = new Double(0);// Threshold Value
	protected Double t1 = new Double(0);// Threshold Value
	protected Double t2 = new Double(0);// Threshold Value
	protected Double t3 = new Double(0);// Threshold Value

	public CensusFuzzyModel(int sign, long orgID) {
		Application.db.addListener(
				this,
				new ArrayList<String>(Arrays.asList(net.ddp2p.common.table.witness.TNAME,
						net.ddp2p.common.table.constituent.TNAME)), null);
		init(sign, orgID);
	}

	void init(int sign, long orgID) {
		if (DEBUG)
			System.out
					.println("CensusFuzzyModel:init:Census setting: " + orgID);
		this.sign = sign;
		this.organizationID = orgID;
		this.name = "Fuzzy:" + orgID + ":" + sign;
		update(null, null);
	}

	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		// System.out.println("update:this.organizationID"+this.organizationID);
		boolean empty = populateEdgeTable(this.organizationID);
		// int empty=loadWitnessStance(1);
		if (!empty) {
			if (metric == 0) {//metric value is the index of the tab
				populateGraph();
				naiveMetric1(pco.toString(), nco.toString(), t0.toString());
			} else if (metric == 1) {
				populateGraph();
				fuzzyMetric1(r);
			}
			else if (metric == 2) {
				populateGraph();
				fuzzyMetric2(r);
			}
			else if (metric == 3) {
				populateGraph();
				fuzzyMetric3(r);
			}
			populateTable();// Change the view of the table widget
			this.fireTableDataChanged();
		}
	}

	@Override
	public int getColumnCount() {
		return this.columnNames.length;
	}

	@Override
	public String getColumnName(int col) {
		return this.columnNames[col];
	}

	@Override
	public int getRowCount() {
		return tableData.length;
	}

	@Override
	public Object getValueAt(int row, int col) {
		return tableData[row].get(col);
	}

	/**
	 * Populate the edgeTable for current organization
	 * 
	 * @param organizationID
	 * @return true if witness table is empty, false if not empty
	 */
	boolean populateEdgeTable(long organizationID) {
		int sourceID, targetID, sense, maxRow = 0, maxColumn = 0;
		ArrayList<ArrayList<Object>> witness_stances = null;
		ArrayList<ArrayList<Object>> sqlResult = null;
		String sqlTestEmptyTable = "Select * from witness";
		boolean empty = true;// The table:witness is empty or not
		try {
			sqlResult = Application.db.select(sqlTestEmptyTable,
					new String[] {});
			if (sqlResult.size() != 0)
				empty = false;
		} catch (P2PDDSQLException e2) {
			e2.printStackTrace();
		}
		if (!empty) {
			String sql_witness_stances = "SELECT source_ID, target_ID,sense_y_n,witness_ID from witness,constituent "
					+ "where constituent.organization_ID=? and constituent.constituent_ID=witness.target_ID";
			try {
				sqlResult = Application.db.select(
						"SELECT max(source_ID) FROM witness", new String[] {});
				if (sqlResult != null) {
					maxRow = Integer.parseInt(net.ddp2p.common.util.Util.getString(sqlResult
							.get(0).get(0)));
				}
				sqlResult = Application.db.select(
						"SELECT max(target_ID) FROM witness", new String[] {});
				if (sqlResult != null) {
					maxColumn = Integer.parseInt(net.ddp2p.common.util.Util.getString(sqlResult
							.get(0).get(0)));
				}
				synchronized(this){
					edgeTable = new Integer[maxRow + 1][maxColumn + 1];
					for (int i = 0; i < edgeTable.length; i++) {
						for (int j = 0; j < edgeTable[i].length; j++) {
							edgeTable[i][j] = 2;
						}
					}
				}
				witness_stances = Application.db.select(sql_witness_stances,
						new String[] { Util.getString(organizationID) });
				for (ArrayList witnessStance : witness_stances) {
					sourceID = Integer.parseInt(net.ddp2p.common.util.Util
							.getString(witnessStance.get(0)));
					targetID = Integer.parseInt(net.ddp2p.common.util.Util
							.getString(witnessStance.get(1)));
					sense = Integer.parseInt(net.ddp2p.common.util.Util.getString(witnessStance
							.get(2)));// 1 or 0
					synchronized(this){
						if((sourceID>edgeTable.length)||(targetID>edgeTable[sourceID].length))
							continue;
						edgeTable[sourceID][targetID] = sense;
					}
				}
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
		}
		return empty;
	}

	/*
	 * TODO:When fuzzaValue is 0, there is a tie. User has to make decision
	 * based on a list of paths(GUI) from himself to the tied node. He witness
	 * against some of the nodes in the paths and re-evaluate tree to break tie
	 * until fuzzyValue of the end node not equal to 0.
	 */
	void findPath(Node start, Node end) {
	}

	// print fuzzy values
	void printTree(Node n) {
		if (n != null) {
			// System.out.println(n.constituentID + ": " + n.fuzzyValue);
			if (n.children != null) {
				for (Node w : n.children) {
					// System.out.println(w.constituentID + ": " +
					// w.fuzzyValue);
					printTree(w);
				}
			}
		}

	}

	/*
	 * Populate the graph with root=r from database
	 */
	void populateGraph() {
		graphDataBuffer = new Hashtable<Integer, Node>();
		ArrayList<ArrayList<Object>> d = null;
		long myConstituentID = 0;
		try {
			myConstituentID = Identity.getCurrentConstituentIdentity()
					.getDefaultConstituentIDForOrg(this.organizationID);// TODO:-1
		} catch (P2PDDSQLException e1) {
			e1.printStackTrace();
		}
		r = new Node();// root
		r.constituentID = (int) myConstituentID;
		r.fuzzyValue = 1.0;
		graphDataBuffer.put((int) myConstituentID, r);
		for (int i = 0; i < edgeTable.length; i++) {
			for (int j = 0; j < edgeTable[i].length; j++) {
				if (edgeTable[i][j] == 1 || edgeTable[i][j] == 0) {
					Node parent, child;
					if (!graphDataBuffer.containsKey(i)) {
						parent = new Node();
						parent.constituentID = i;
						parent.fuzzyValue = 0.0;
						graphDataBuffer.put(i, parent);
					} else {
						parent = graphDataBuffer.get(i);
					}
					if (!graphDataBuffer.containsKey(j)) {
						child = new Node();
						child.constituentID = j;
						child.fuzzyValue = 0.0;
						graphDataBuffer.put(j, child);
					} else {
						child = graphDataBuffer.get(j);
					}
					if (edgeTable[i][j] == 1) {
						child.positiveWitnessCount++;
					}
					if (edgeTable[i][j] == 0) {
						child.negativeWintesCount++;
					}
					if (parent.children == null)
						parent.children = new ArrayList<Node>();
					int c = 0;
					for (Node n : parent.children) {
						if (n.constituentID == child.constituentID)
							c++;
					}
					if (c == 0)
						parent.children.add(child);
				}

			}
		}
	}

	// Populate the data of the table(tableData) model
	void populateTable() {
		tableData = new ArrayList[viewDataBuffer.size()];
		Enumeration<Integer> e = this.viewDataBuffer.keys();
		ArrayList<ArrayList<Object>> constituentName = null;
		e = this.viewDataBuffer.keys();// Enumeration of constituent IDs
		int i = 0;// Index for tableData
		while (e.hasMoreElements()) {
			int constituentID = e.nextElement();
			int myConstituentID = 0;
			ConstituentStatictics statistic = viewDataBuffer.get(constituentID);
			ArrayList<Object> rowData = new ArrayList<Object>();
			try {
				constituentName = Application.db.select(
						"SELECT name from constituent where constituent_ID=?",
						new String[] { Util.getString(constituentID) });
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
//			System.out.println("constituentName:" + constituentName);
			try {
				myConstituentID = (int) Identity.getCurrentConstituentIdentity()
						.getDefaultConstituentIDForOrg(this.organizationID);
			} catch (P2PDDSQLException e1) {
				e1.printStackTrace();
			}
			if (constituentName.size() == 0)
				rowData.add("Can not find constituent with ID: "
						+ constituentID);// Can't find constituents
			else if (myConstituentID == constituentID)
				rowData.add(constituentName.get(0).get(0) + "(This is you!)");// 1st
																				// Column:
																				// Constituent
																				// Name
			else
				rowData.add(constituentName.get(0).get(0));// 1st Column:

			// Constituent Name
			if(statistic!=null){
				rowData.add(statistic.value);// 2nd column: Value
				rowData.add(statistic.threshold);// 3rd column: threshold testing:
			}else{
				System.err.println("CensusFuzzyModel:populateTable: statistics are null!");
			}
												// passed or fail
			tableData[i] = rowData;// Populate dat of i-th row
			i++;
		}
	}	

	/*
	 * Most intuitive metric
	 */
	public void naiveMetric1(String pco, String nco, String t) {
		viewDataBuffer = new Hashtable<Integer, ConstituentStatictics>();// Mapping
																			// from
																			// constituent_ID
																			// to
																			// his
																			// statics
		try {
			ArrayList<ArrayList<Object>> sqlResult;
			String currentOrgID = "" + this.organizationID;// Current
															// used/selected
															// organization ID
			String sql = "select target_ID, NC, PC, (PC+1.0*"
					+ pco
					+ ")/(NC+"
					+ nco
					+ "), case when((PC+1.0*"
					+ pco
					+ ")/(NC+"
					+ nco
					+ ")>="
					+ t
					+ ") then 'X' else '' end,name from"
					+ "("
					+ "		select target_ID, sum(N)AS NC, sum(Y) AS PC"
					+ "		FROM"
					+ "		(select target_ID, "
					+ "		case when(S='Y') then cnt else '0' end AS Y,"
					+ "		case when(S='N') then cnt else '0' end AS N"
					+ "		FROM"
					+ "		 ("
					+ "		select "
					+ "		 target_ID ,"
					+ "		 count(*) AS cnt,"
					+ "		 case when(sense_y_n='1') then 'Y' else 'N' end AS S"
					+ "		 from witness"
					+ "		 group by target_ID,sense_y_n"
					+ "		)"
					+ "		)"
					+ "		 group by target_ID"
					+ "		)t1, constituent where t1.target_ID=constituent.constituent_ID and constituent.organization_ID=?;";
			sqlResult = Application.db.select(sql,
					new String[] { currentOrgID }, DEBUG);
			for (ArrayList rowData : sqlResult) {
				int constituentID = Integer.parseInt(net.ddp2p.common.util.Util
						.getString(rowData.get(0)));
				String value = net.ddp2p.common.util.Util.getString(rowData.get(3));
				String threshold = net.ddp2p.common.util.Util.getString(rowData.get(4));
				viewDataBuffer.put(constituentID, new ConstituentStatictics(
						value, threshold));
			}
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Compute fuzzy value for each node in the graph and reset viewDataBuffer
	 */
	void fuzzyMetric1(Node root) {
		viewDataBuffer = new Hashtable<Integer, ConstituentStatictics>();
		fuzzyNodes = new ArrayList<Integer>();
		if (root != null) {
			Queue<Node> queue = new LinkedList<Node>();
			// Process Root
			queue.add(root);
			root.used = true;// Mark the node as used nodes.
			root.valueString += ",1.0";
			root.averagedFuzzyValue = 1.0;
			root.permanent = true;
			String thresholdTest="";
			if(root.averagedFuzzyValue>=t1) thresholdTest="X";
			viewDataBuffer.put(
					r.constituentID,
					new ConstituentStatictics(Util.getString(r.averagedFuzzyValue), thresholdTest));
			// Process root's child in only level 1.
			if (root.children != null) {
				/**
				 * The children of the root should not be biased by other's
				 * witness stances and should not be changed(permanent)
				 */
				for (Node child : root.children) {
					if (edgeTable[root.constituentID][child.constituentID] == 1) {
						child.averagedFuzzyValue = 1.0;
						child.valueString = "1.0";
						child.permanent = true;
						thresholdTest="";
						if(child.averagedFuzzyValue>=t1) thresholdTest="X";
						viewDataBuffer.put(
								child.constituentID,
								new ConstituentStatictics(Util.getString(child.averagedFuzzyValue), thresholdTest));
					} else if (edgeTable[root.constituentID][child.constituentID] == 0) {
						child.averagedFuzzyValue = 0.0;
						child.valueString = "0.0";
						child.permanent = true;
						thresholdTest="";
						if(child.averagedFuzzyValue>=t1) thresholdTest="X";
						viewDataBuffer.put(
								child.constituentID,
								new ConstituentStatictics(Util.getString(child.averagedFuzzyValue), thresholdTest));
					}
				}
			}
			// Process the rest of the nodes
			while (!queue.isEmpty()) {
				Node parent = queue.poll();// Get the root from the queue
				if (!parent.permanent) {
					parent.averagedFuzzyValue = parent.sumFuzzyValue
							/ parent.numFuzzyValue;
					parent.permanent = true;// Do not allow nodes at level n+1
											// change the averagedFuzzyValue of
											// nodes at level n
					
					thresholdTest="";
					if(parent.averagedFuzzyValue>=t1) thresholdTest="X";
					viewDataBuffer.put(
							parent.constituentID,
							new ConstituentStatictics(Util
									.getString(parent.averagedFuzzyValue),
									thresholdTest));
					System.out.println("fm1:rest nodes:"+parent.constituentID
							+":"+parent.averagedFuzzyValue+":"+t1+":"+
									thresholdTest);
				}
				if (parent.children != null) {
					for (Node child : parent.children) {
						/**
						 * Nodes are negatively witnessed and not the children
						 * of the root.
						 */
						if (edgeTable[parent.constituentID][child.constituentID] == 0
								&& !child.permanent) {
							child.sumFuzzyValue += 1 - parent.averagedFuzzyValue
									* this.fuzzyFactor;
							child.valueString += ","
									+ Util.getString(1 - parent.averagedFuzzyValue
											* this.fuzzyFactor);
							child.numFuzzyValue++;
							// fuzzyNodes.add(child.constituentID);
						} else if (edgeTable[parent.constituentID][child.constituentID] == 1
								&& !child.permanent) {
							child.sumFuzzyValue += parent.averagedFuzzyValue
									* this.fuzzyFactor;
							child.valueString += ","
									+ Util.getString(parent.averagedFuzzyValue
											* this.fuzzyFactor);
							child.numFuzzyValue++;
							// fuzzyNodes.add(child.constituentID);
						}

						if (!child.used) {// TODO:Reset graph when switching tab
							queue.add(child);
							child.used = true;
						}
					}
				}
			}
		}
	}

	/*
	 * Compute fuzzy value for each node in the graph and reset viewDataBuffer
	 */
	void fuzzyMetric2(Node root) {
		viewDataBuffer = new Hashtable<Integer, ConstituentStatictics>();
		fuzzyNodes = new ArrayList<Integer>();
		if (root != null) {
			Queue<Node> queue = new LinkedList<Node>();
			// Process Root
			queue.add(root);
			root.used = true;// Mark the node as used nodes.
			root.valueString += ",1.0";
			root.averagedFuzzyValue = 1.0;
			root.permanent = true;
			String thresholdTest="";
			if(root.averagedFuzzyValue>=t2) thresholdTest="X";
			viewDataBuffer.put(
					r.constituentID,
					new ConstituentStatictics(Util.getString(r.averagedFuzzyValue), thresholdTest));
			// Process root's child in only level 1.
			if (root.children != null) {
				/**
				 * The children of the root should not be biased by other's
				 * witness stances and should not be changed(permanent)
				 */
				for (Node child : root.children) {
					if (edgeTable[root.constituentID][child.constituentID] == 1) {
						child.averagedFuzzyValue = 1.0;
						child.valueString = "1.0";
						child.permanent = true;
						thresholdTest="";
						if(child.averagedFuzzyValue>=t2) thresholdTest="X";
						viewDataBuffer.put(
								child.constituentID,
								new ConstituentStatictics(Util.getString(child.averagedFuzzyValue), thresholdTest));
					} else if (edgeTable[root.constituentID][child.constituentID] == 0) {
						child.averagedFuzzyValue = 0.0;
						child.valueString = "0.0";
						child.permanent = true;
						thresholdTest="";
						if(child.averagedFuzzyValue>=t2) thresholdTest="X";
						viewDataBuffer.put(
								child.constituentID,
								new ConstituentStatictics(Util.getString(child.averagedFuzzyValue), thresholdTest));
					}
				}
			}
			// Process the rest of the nodes
			while (!queue.isEmpty()) {
				Node parent = queue.poll();// Get the root from the queue
				if (!parent.permanent) {
					parent.averagedFuzzyValue = parent.sumPositiveFuzzyValue
							/ parent.numPositiveFuzzyValue+parent.sumNegativeFuzzyValue;
					parent.permanent = true;// Do not allow nodes at level n+1
											// change the averagedFuzzyValue of
											// nodes at level n
					thresholdTest="";
					if(parent.averagedFuzzyValue>=t2) thresholdTest="X";
					viewDataBuffer.put(
							parent.constituentID,
							new ConstituentStatictics(Util
									.getString(parent.averagedFuzzyValue),
									thresholdTest));
				}
				if (parent.children != null) {
					for (Node child : parent.children) {
						/**
						 * Nodes are negatively witnessed and not the children
						 * of the root.
						 */
						if (edgeTable[parent.constituentID][child.constituentID] == 0
								&& !child.permanent) {
							child.sumFuzzyValue += 1 - parent.averagedFuzzyValue
									* this.fuzzyFactor;
							child.valueString += ","
									+ Util.getString(1 - parent.averagedFuzzyValue
											* this.fuzzyFactor);
							child.numNegativeFuzzyValue++;
							// fuzzyNodes.add(child.constituentID);
						} else if (edgeTable[parent.constituentID][child.constituentID] == 1
								&& !child.permanent) {
							child.sumPositiveFuzzyValue += parent.averagedFuzzyValue
									* this.fuzzyFactor;
							child.valueString += ","
									+ Util.getString(parent.averagedFuzzyValue
											* this.fuzzyFactor);
							child.numPositiveFuzzyValue++;
							// fuzzyNodes.add(child.constituentID);
						}

						if (!child.used) {// TODO:Reset graph when switching tab
							queue.add(child);
							child.used = true;
						}
					}
				}
			}
		}
	}

	/*
	 * Compute fuzzy value for each node in the graph and reset viewDataBuffer
	 */
	void fuzzyMetric3(Node root) {
		viewDataBuffer = new Hashtable<Integer, ConstituentStatictics>();
		fuzzyNodes = new ArrayList<Integer>();
		if (root != null) {
			Queue<Node> queue = new LinkedList<Node>();
			// Process Root
			queue.add(root);
			root.used = true;// Mark the node as used nodes.
			root.valueString += ",1.0";
			root.averagedFuzzyValue = 1.0;
			root.permanent = true;
			String thresholdTest="";
			if(root.averagedFuzzyValue>=t3) thresholdTest="X";
			viewDataBuffer.put(
					r.constituentID,
					new ConstituentStatictics(Util.getString(r.averagedFuzzyValue), thresholdTest));
			// Process root's child in only level 1.
			if (root.children != null) {
				/**
				 * The children of the root should not be biased by other's
				 * witness stances and should not be changed(permanent)
				 */
				for (Node child : root.children) {
					if (edgeTable[root.constituentID][child.constituentID] == 1) {
						child.averagedFuzzyValue = 1.0;
						child.valueString = "1.0";
						child.permanent = true;
						thresholdTest="";
						if(child.averagedFuzzyValue>=t3) thresholdTest="X";
						viewDataBuffer.put(
								child.constituentID,
								new ConstituentStatictics(Util.getString(child.averagedFuzzyValue), thresholdTest));
					} else if (edgeTable[root.constituentID][child.constituentID] == 0) {
						child.averagedFuzzyValue = 0.0;
						child.valueString = "0.0";
						child.permanent = true;
						thresholdTest="";
						if(child.averagedFuzzyValue>=t3) thresholdTest="X";
						viewDataBuffer.put(
								child.constituentID,
								new ConstituentStatictics(Util.getString(child.averagedFuzzyValue), thresholdTest));
					}
				}
			}
			// Process the rest of the nodes
			while (!queue.isEmpty()) {
				Node parent = queue.poll();// Get the root from the queue
				if (!parent.permanent) {
					parent.averagedFuzzyValue = parent.numMoreThanHalf
							/ parent.numMoreThanHalf+parent.numLessThanHalf;
					parent.permanent = true;// Do not allow nodes at level n+1
											// change the averagedFuzzyValue of
											// nodes at level n
					thresholdTest="";
					if(parent.averagedFuzzyValue>=t3) thresholdTest="X";
					viewDataBuffer.put(
							parent.constituentID,
							new ConstituentStatictics(Util
									.getString(parent.averagedFuzzyValue),
									thresholdTest));
				}
				if (parent.children != null) {
					for (Node child : parent.children) {
						/**
						 * Nodes are negatively witnessed and not the children
						 * of the root.
						 */
						if (edgeTable[parent.constituentID][child.constituentID] == 0
								&& !child.permanent) {
							double a= 1 - parent.averagedFuzzyValue* this.fuzzyFactor;
							if(a>=0.5){
								child.numMoreThanHalf++;
							}
							else child.numLessThanHalf++;
//							child.valueString += ","+ Util.getString(1 - parent.averagedFuzzyValue* this.fuzzyFactor);
//							child.numNegativeFuzzyValue++;
							// fuzzyNodes.add(child.constituentID);
						} else if (edgeTable[parent.constituentID][child.constituentID] == 1
								&& !child.permanent) {
							double a= parent.averagedFuzzyValue* this.fuzzyFactor;
							if(a>=0.5){
								child.numMoreThanHalf++;
							}
							else child.numLessThanHalf++;
//							child.valueString += ","+ Util.getString(parent.averagedFuzzyValue* this.fuzzyFactor);
//							child.numPositiveFuzzyValue++;
							// fuzzyNodes.add(child.constituentID);
						}

						if (!child.used) {// TODO:Reset graph when switching tab
							queue.add(child);
							child.used = true;
						}
					}
				}
			}
		}
	}

	
	@Override
	public void orgUpdate(String orgID, int col, D_Organization org) {
		if (DEBUG)
			System.out.println("CensusFuzzyModel:orgUpdate: Census setting: "
					+ orgID);
		this.organization = org;
		init(-1, Util.lval(orgID, -1));
	}

	@Override
	public void org_forceEdit(String orgID, D_Organization org) {
		// TODO Auto-generated method stub

	}

}
