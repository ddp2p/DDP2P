package census;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import census.witnessGraph.witnessGraphNode;

import config.Application;
import data.D_Organization;
import data.D_Witness;
import util.*;
//Assumption: A constituent can only have one witness stance against another constituent.
class witnessGraph extends JFrame{
	int allID=0, trueId=0,falseID=0,tieID=0;
    public  void addComponentsToPane(Container pane) {
        if (!(pane.getLayout() instanceof BorderLayout)) {
            pane.add(new JLabel("Container doesn't use BorderLayout!"));
            return;
        }
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
		String[] columnNames1 = { "gid", "id", "Name",
				"Fuzzy Value" };
		Object[][] data1 = {
				{ "Kathy", "Smith", "Snowboarding", new Integer(5),
						new Boolean(false) },
				{ "John", "Doe", "Rowing", new Integer(3), new Boolean(true) },
				{ "Sue", "Black", "Knitting", new Integer(2),
						new Boolean(false) },
				{ "Jane", "White", "Speed reading", new Integer(20),
						new Boolean(true) },
				{ "Joe", "Brown", "Pool", new Integer(10), new Boolean(false) } 
		};

		final JTable table1 = new JTable(data1, columnNames1);
//		table1.setPreferredScrollableViewportSize(new Dimension(500, 70));
//		table1.setFillsViewportHeight(true);
//        table1.setPreferredSize(new Dimension(200, 100));
        JScrollPane scrollPane = new JScrollPane(table1);
        JPanel jp=new JPanel();
        
//        jp.add(scrollPane);
//        jp.setOpaque(true);
        //Add the scroll pane to this panel.
        pane.add(scrollPane, BorderLayout.CENTER);
        JButton button = new JButton("Button 1 (PAGE_START)");
        button.setPreferredSize(new Dimension(200, 100));
//        pane.add(table1, BorderLayout.CENTER);
        String l1_str = "Among "+ allID +" constituents, there are "+trueId+" true identities; "+falseID+" false identities; "+"and "+
tieID+" tied identities";;
        JLabel l1=new JLabel(l1_str);
        pane.add(l1, BorderLayout.PAGE_START);
        
        //Make the center component big, since that's the
        //typical usage of BorderLayout.
//        button = new JButton("Button 2 (CENTER)");
        
        button = new JButton("Button 3 (LINE_START)");
        pane.add(button, BorderLayout.LINE_START);
        
        button = new JButton("Long-Named Button 4 (PAGE_END)");
        pane.add(button, BorderLayout.PAGE_END);
        
        button = new JButton("5 (LINE_END)");
        pane.add(button, BorderLayout.LINE_END);
    }
    
	witnessGraph(){
		populateEdge();
		printEdge();
		buildTree();
		evaluateTree(r);
		printFuzzyTable();
		addComponentsToPane(this.getContentPane());
		
	}
	final double fuzzyFactor=0.9;//0.9 is used for this implementation.
	Integer e[][];//[2][4]=1 Means constituent 2 positively witnessed 4 and [3][4]=2 Means constituent 3 negatively witnessed 4.
	Hashtable <Integer,Double> fuzzyValueTable=new Hashtable <Integer,Double> ();;
	Hashtable <Integer,witnessGraphNode> ht;
	witnessGraphNode r;//root
	class witnessGraphNode{
		witnessGraphNode(){
		}
		
		double fuzzyValue=0.0;
		String constituentName;
		Integer constituentID;
		String neighborhoodID;//Not Implemented
		ArrayList<witnessGraphNode> parent;//List of parents
		ArrayList<witnessGraphNode> children;//List of children
	}
	//TODO:Read from database
	void populateEdge(){
		Integer maxRow = 0, maxColumn = 0,x,y,z;
		ArrayList<ArrayList<Object>> c = null;
		String sql="SELECT source_ID, target_ID,sense_y_n from witness";
		
		try {
			
			c=Application.db.select("SELECT max(source_ID) FROM witness", new String[]{});
			if(c!=null){
				maxRow=Integer.parseInt(util.Util.getString(c.get(0).get(0)));
			}
			c=Application.db.select("SELECT max(target_ID) FROM witness", new String[]{});
			if(c!=null){
				maxColumn=Integer.parseInt(util.Util.getString(c.get(0).get(0)));
			}
			e =new Integer[maxRow+1][maxColumn+1];
			for(int i=0;i<e.length;i++){
				for(int j=0;j<e[i].length;j++){
					e[i][j]=2;
				}
			}
			c=Application.db.select(sql, new String[]{});
			for(ArrayList a:c){
				x=Integer.parseInt(util.Util.getString(a.get(0)));
				y=Integer.parseInt(util.Util.getString(a.get(1)));
				z=Integer.parseInt(util.Util.getString(a.get(2)));
				e[x][y]=z;
//				System.out.println(x);
//				System.out.println(y);
//				System.out.println(z);
//				System.out.println();
			}
//			e[0][4]=1;
//			e[0][3]=1;
//			e[0][2]=0;
//			e[4][1]=1;
//			e[3][1]=0;
			
		} catch (P2PDDSQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		

		


	}
	void printEdge(){
		System.out.println("======Edge======");
		for(int i=0;i<e.length;i++){
			for(int j=0;j<e[i].length;j++){
				if(e[i][j]==1||e[i][j]==0)
				System.out.println("["+i+", "+j+"]"+"="+e[i][j]);
			}
		}
		System.out.println("================");
	}
	void buildTree(){
		ht=new Hashtable<Integer, witnessGraphNode>();
		int myConstituentID=0;
		r=new witnessGraphNode();//root
		r.constituentID=myConstituentID;
		r.fuzzyValue=1.0;
		ht.put(myConstituentID, r);
		
		for(int i=0;i<e.length;i++){
			for(int j=0;j<e[i].length;j++){
				if(e[i][j]==1||e[i][j]==0){
//					System.out.println("["+i+", "+j+"]"+"="+e[i][j]);
					witnessGraphNode parent,child;
					if(!ht.containsKey(i)){
						parent=new witnessGraphNode();
						parent.constituentID=i;
						ht.put(i, parent);
						
					}
					else {
						parent=ht.get(i);
					}
					if(!ht.containsKey(j)){
						child=new witnessGraphNode();
						child.constituentID=j;
						ht.put(j, child);	
						
					}
					else {
						child=ht.get(j);
					}
					if(parent.children==null)
						parent.children=new ArrayList<witnessGraphNode>();
					parent.children.add(child);
				}
			}
		}
	}
	
	//print fuzzy values
	void evaluateTree(witnessGraphNode n){
		
		if(n!=null){
//			System.out.println(n.constituentID+" "+n.fuzzyValue);
			if(n.children!=null){
				for(witnessGraphNode w:n.children){
					if(e[n.constituentID][w.constituentID]==1){
						w.fuzzyValue+=n.fuzzyValue*this.fuzzyFactor;
						fuzzyValueTable.put(w.constituentID, new Double(w.fuzzyValue));
						}
					else if(e[n.constituentID][w.constituentID]==0){
						w.fuzzyValue+=(-1)*n.fuzzyValue*this.fuzzyFactor;
						fuzzyValueTable.put(w.constituentID, new Double(w.fuzzyValue));
					}
					evaluateTree(w);
				}
			}
		}
		
	}
	void printFuzzyTable(){
		
		allID=fuzzyValueTable.size();
		Enumeration<Integer> e = this.fuzzyValueTable.keys();
		while(e.hasMoreElements()){
			int a =e.nextElement();
			double fv=fuzzyValueTable.get(a);
			if(fv<0) falseID++;
			else if(fv>0) trueId++;
			else tieID++;
		}
		System.out.println("Among "+allID+" constituents");
		System.out.println("There are "+trueId +" true identities:");
		e = this.fuzzyValueTable.keys();
		System.out.println("ConstituentID   Fuzzy Value");
		while(e.hasMoreElements()){
			int a =e.nextElement();
			double fv=fuzzyValueTable.get(a);
			if(fv>0) System.out.println(a+"                   "+fv);
		}
		System.out.println("==================================");
		System.out.println("There are "+falseID +" false identities:");
		e = this.fuzzyValueTable.keys();
		System.out.println("ConstituentID   Fuzzy Value");
		while(e.hasMoreElements()){
			int a =e.nextElement();
			double fv=fuzzyValueTable.get(a);
			if(fv<0) System.out.println(a+"                  "+fv);
		}
		System.out.println("==================================");
		System.out.println("There are "+tieID +" tied identities:");
		e = this.fuzzyValueTable.keys();
		System.out.println("ConstituentID   Fuzzy Value");
		while(e.hasMoreElements()){
			int a =e.nextElement();
			double fv=fuzzyValueTable.get(a);
			if(fv==0) System.out.println(a+"                     "+fv);
		}
		
	}
	/*
	 * TODO:When fuzzaValue is 0, there is a tie. 
	 * User has to make decision based on a list of paths(GUI) from himself to the tied node.
	 * He witness against some of the nodes in the paths and re-evaluate tree to break tie until fuzzyValue of the end node not equal to 0.
	 */
	
	void findPath(witnessGraphNode start, witnessGraphNode end){} 
	
	
	//print fuzzy values
	void printTree(witnessGraphNode n){
		if(n!=null){
			System.out.println(n.constituentID+": "+n.fuzzyValue);
			if(n.children!=null){
				for(witnessGraphNode w:n.children){
					System.out.println(w.constituentID+": "+w.fuzzyValue);
					printTree(w);
				}
			}
		}
		
	}
	
}



public class computeCensus extends JPanel{
	computeCensus(){
		
	}


	public String[] thresholdPCNC(String pco, String nco, String t) {
		

		try {
			
			ArrayList<ArrayList<Object>> c;
			String sql = "SELECT * FROM witness";
			String sql2 = "select target_ID, NC, PC, case when((PC+1.0*"+pco+")/(NC+"+nco+")>="+t+") then 'T' else 'F' end,name from"+
					"("+
					"		select target_ID, sum(N)AS NC, sum(Y) AS PC"+
					"		FROM"+
					"		(select target_ID, "+
					"		case when(S='Y') then cnt else '0' end AS Y,"+
					"		case when(S='N') then cnt else '0' end AS N"+
					"		FROM"+
					"		 ("+
					"		select "+
					"		 target_ID ,"+
					"		 count(*) AS cnt,"+
					"		 case when(sense_y_n='1') then 'Y' else 'N' end AS S"+
					"		 from witness"+
					"		 group by target_ID,sense_y_n"+
					"		)"+
					"		)"+
					"		 group by target_ID"+
					"		)t1, constituent where t1.target_ID=constituent.constituent_ID;";
//			c = Application.db.select(sql2, new String[]{pco,nco,t});
			c = Application.db.select(sql2, new String[]{});
			System.out.println("[Target_ID, Negative Count, Positive Count, ValidityOfIdentity,Constituent Name]");
			System.out.println(c);

		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
public static void populateWitnessData1(){
	//Populate simulated "witness" data
			Integer sense_y_n=0;
			Integer source_ID=2;
			Integer target_ID=0;
			String insertSql="INSERT INTO witness(source_ID, target_ID,sense_y_n) values (?,?,?)";
			String sense_y_n_str=""+sense_y_n;
			String source_ID_str=""+source_ID;
			String target_ID_str=""+target_ID;
			try {
				Application.db.delete("DELETE FROM witness", new String[]{});
				Application.db.insert(insertSql, new String[] {""+0,""+2,""+0});
				Application.db.insert(insertSql, new String[] {""+0,""+3,""+1});
				Application.db.insert(insertSql, new String[] {""+0,""+4,""+1});
				Application.db.insert(insertSql, new String[] {""+3,""+1,""+0});
				Application.db.insert(insertSql, new String[] {""+4,""+1,""+1});
			} catch (P2PDDSQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
}

public static void populateWitnessData2(){
	//Populate simulated "witness" data
			int sense_y_n=0;
			int source_ID=2;
			int target_ID=0;
			String insertSql="INSERT INTO witness(source_ID, target_ID,sense_y_n) values (?,?,?)";
			String sense_y_n_str=""+sense_y_n;
			String source_ID_str=""+source_ID;
			String target_ID_str=""+target_ID;
			try {
				Application.db.delete("DELETE FROM witness", new String[]{});
				Application.db.insert(insertSql, new String[] {""+0,""+2,""+1});
				Application.db.insert(insertSql, new String[] {""+0,""+3,""+1});
				Application.db.insert(insertSql, new String[] {""+0,""+4,""+1});
				Application.db.insert(insertSql, new String[] {""+3,""+1,""+0});
				Application.db.insert(insertSql, new String[] {""+4,""+1,""+1});
			} catch (P2PDDSQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
}


	public static void main(String[] args) {
		try {
			Application.db = new DBInterface(Application.DELIBERATION_FILE);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		computeCensus cc =new computeCensus();
//		cc.thresholdPCNC("1","1", "1");
//		JFrame jf = new JFrame();
//		jf.add(cc);
//		populateWitnessData();
//		populateWitnessData1();
		
//		wg.evaluateTree(wg.r);
//		wg.printTree(wg.r);
		
//		Hashtable <Integer,Double> fuzzyValueTable=new Hashtable <Integer,Double> ();
//		
//		fuzzyValueTable.put(1, 2.0);
//		System.out.println(fuzzyValueTable.size());
//
//		fuzzyValueTable.put(1, 2.0);
//		System.out.println(fuzzyValueTable.size());
//		Enumeration e=fuzzyValueTable.keys();
//		while(e.hasMoreElements()){
//			System.out.println(e.nextElement());	
//		}
		witnessGraph wg=new witnessGraph();
        wg.pack();
        wg.setVisible(true);
	}
}