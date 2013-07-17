package widgets.dir_fw_terms;

import static util.Util._;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.JComboBox;
import util.P2PDDSQLException;

import config.Application;
import data.D_TermsInfo;
import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.Util;
import table.directory_forwarding_terms;

@SuppressWarnings("serial")
public class TermsModel extends AbstractTableModel implements TableModel, DBListener{
	public static final int TABLE_COL_PRIORITY = 0;
	public static final int TABLE_COL_TOPIC = 1;
	public static final int TABLE_COL_AD = 2;
	public static final int TABLE_COL_PLAINTEXT = 3;
	public static final int TABLE_COL_PAYMENT = 4;
	public static final int TABLE_COL_SERVICE = 5;
	public static final int TABLE_COL_PRIORITY_TYPE = 6;
	private static final boolean _DEBUG = true;
	public static boolean DEBUG = false;
	JComboBox<String> comboBox;
	JComboBox<String> serviceCBox;
	JComboBox<String> priorityTypeCBox;
	public long peerID =-1;
	public String dirAddr;
	public long _peerID =-1;
	public String _dirAddr;

	private DBInterface db;
	HashSet<Object> tables = new HashSet<Object>();
	String columnNames[]={_("Priority"),_("Topic"),_("AD"),_("Plaintext"),_("Payment"), _("Service"),  _("Priority Type")};

	ArrayList<D_TermsInfo> data = new ArrayList<D_TermsInfo>(); // rows of type D_TermInfo -> Bean
	private TermsPanel panel;
	
	public TermsModel(DBInterface _db, TermsPanel _panel) { // constructor with dataSource -> DBInterface _db
		db = _db;
		panel = _panel;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.directory_forwarding_terms.TNAME)), null);
		// DBSelector.getHashTable(table.organization.TNAME, table.organization.organization_ID, ));
	//	update(null, null);
	}
    public void setPeerID(long peerID2){
    	this._peerID = this.peerID = peerID2;
    }
    public void setDirAddr(String dirAddr){
    	this._dirAddr = this.dirAddr = dirAddr;
    }
    public int getLastPriority(){
    	if(data == null || data.size()==0)
    		return 0;
    	return (data.get(getRowCount()-1).priority);
    	
    }
     public JComboBox getPriorityTypeComboBox(){
		if(DEBUG) System.out.println("DirModel: getModeComboBox: start:");
		
		priorityTypeCBox = new JComboBox<String>();
		priorityTypeCBox.addItem("Normal");
		priorityTypeCBox.addItem("Proactive");
		priorityTypeCBox.setSelectedIndex(0);
		
		return priorityTypeCBox;
	}
    public JComboBox getServiceComboBox(){
		if(DEBUG) System.out.println("DirModel: getModeComboBox: start:");
		
		serviceCBox = new JComboBox<String>();
		serviceCBox.addItem("All Services");
		serviceCBox.addItem("Forward");
		serviceCBox.addItem("Address");
		serviceCBox.addItem("Start up com.");
		serviceCBox.setSelectedIndex(0);
		
		return serviceCBox;
	}
	@Override
	public int getRowCount() {
		return data.size();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int col) {
		if(DEBUG) System.out.println("TermsModel:getColumnName: col Header["+col+"]="+columnNames[col]);
		return Util.getString(columnNames[col]);
	}

	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_SERVICE || col == TABLE_COL_PRIORITY_TYPE) return getValueAt(0, col).getClass();
		if(col == TABLE_COL_PRIORITY) return String.class;
		return Boolean.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		switch(columnIndex){
		case TABLE_COL_PRIORITY:
			return false;
		case TABLE_COL_PAYMENT:
			return false;
		}
		return true;
	}

	//@SuppressWarnings({"rawtypes", "unchecked" })
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {// a cell
		if((rowIndex<0) || (rowIndex>=data.size())) return null;
		if((columnIndex<0) || (columnIndex>this.getColumnCount())) return null;
		D_TermsInfo crt = data.get(rowIndex);
		if(crt==null) return null;
		switch(columnIndex){
		case TABLE_COL_TOPIC:
			return data.get(rowIndex).topic;
		case TABLE_COL_AD:
			return data.get(rowIndex).ad;
		case TABLE_COL_PLAINTEXT:
			return data.get(rowIndex).plaintext;
		case TABLE_COL_PAYMENT:
			return data.get(rowIndex).payment;
		case TABLE_COL_PRIORITY:
			return data.get(rowIndex).priority;
		case TABLE_COL_SERVICE:
			return  serviceCBox.getItemAt(data.get(rowIndex).service);
		case TABLE_COL_PRIORITY_TYPE:
			return  priorityTypeCBox.getItemAt(data.get(rowIndex).priority_type); 
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int row, int col) {
		if(DEBUG) System.out.println("TermsModel:setValueAt: r="+row +", c="+col+" val="+aValue);
		if((row<0) || (row>=data.size())) return;
		if((col<0) || (col>this.getColumnCount())) return;
		D_TermsInfo crt = data.get(row);
		if(DEBUG) System.out.println("TermsModel:setValueAt: old crt="+crt);
		switch(col) {
		case TABLE_COL_TOPIC:
			crt.topic = ((Boolean) aValue).booleanValue();
			if(crt.topic && (getPanel().peerTopicTxt == null || getPanel().peerTopicTxt.getText().trim().equals("") ))
			{   Application.warning(_("Topic field has no value"), _("No topic assigned"));
				crt.topic = false;
			}	
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_AD:
			crt.ad = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_PLAINTEXT:
			crt.plaintext = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_PAYMENT:
			if(_DEBUG) System.out.println("TermsModel:setValueAt: PAYMENT");
			crt.payment = ((Boolean) aValue).booleanValue();
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_SERVICE:
			crt.service = getIndex(Util.getString(aValue), serviceCBox);
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		case TABLE_COL_PRIORITY_TYPE:
			crt.priority_type = getIndex(Util.getString(aValue), priorityTypeCBox);
			try {
				crt.storeNoSync("update");
			} catch (P2PDDSQLException e) {
				e.printStackTrace();
			}
			break;
		}
		fireTableCellUpdated(row, col);
	}
   public int getIndex(String item, JComboBox cbox){
   		for( int i=0; i<cbox.getItemCount(); i++){
   			if(cbox.getItemAt(i).equals(item))
   				return i;	
   		}
   		return -1;	
   }
	@Override
	public void addTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub

	}

	static ArrayList<ArrayList<Object>> getTerms(long _peerID2, String dirAddr){
		String sql;
		String[]params;
		if(dirAddr!=null) {
			sql = "SELECT "+directory_forwarding_terms.fields_terms+
				" FROM  "+directory_forwarding_terms.TNAME+
			    " WHERE "+directory_forwarding_terms.peer_ID+" =? " +
				" AND "+directory_forwarding_terms.dir_addr+" =? "+
				" ORDER BY "+directory_forwarding_terms.priority+";";
			params = new String[]{""+_peerID2, dirAddr};
		}else{
			sql = "SELECT "+directory_forwarding_terms.fields_terms+
					" FROM  "+directory_forwarding_terms.TNAME+
				    " WHERE "+directory_forwarding_terms.peer_ID+" =? AND "+
				    directory_forwarding_terms.dir_addr+" IS NULL "+
				    " ORDER BY "+directory_forwarding_terms.priority+";";
			params = new String[]{""+_peerID2};
		}
		ArrayList<ArrayList<Object>> u;
		try {
			u = Application.db.select(sql, params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
			return null;
		}
		return u;
	}
	
	@Override
	public void update(ArrayList<String> table, Hashtable<String, DBInfo> info) {
		if(DEBUG) System.out.println("TermsModel: update: start: table="+Util.concat(table, " ", "null"));
		ArrayList<ArrayList<Object>> u;
		u = getTerms(_peerID, _dirAddr); if(u==null) return;
		if((u.size()==0) && (_peerID!=0) && (_dirAddr!=null)) {
			u = getTerms(_peerID, null); if(u==null) return;
			if(u.size()==0) {
				u = getTerms(0, _dirAddr); if(u==null) return;
				if(u.size()==0) {
					u = getTerms(0, null); if(u==null) return;
					getPanel().setGeneralGlobal();
					_dirAddr = null; _peerID = 0;
				}else{
					getPanel().setGeneralDirectory();
					_peerID = 0;
				}
			}else{
				getPanel().setGeneralPeer();
				_dirAddr = null;
			}
		}
		data = new ArrayList<D_TermsInfo>();
		for(ArrayList<Object> _u :u){
			D_TermsInfo ui = new D_TermsInfo(_u);
			if(DEBUG) System.out.println("TermsModel: update: add: "+ui);
			data.add(ui); // add a new item to data list (rows)
		}
		this.fireTableDataChanged();
	}
	public int getPriority(int row){
		return 	data.get(row).priority;
	}
	public void shiftByOne(int priority)throws P2PDDSQLException{
		System.out.println("data size: "+ data.size());
		for(int i=priority-1; i< data.size(); i++ ){
			data.get(i).priority -= 1;//data.get(i).priority;
			data.get(i).storeNoSync("update");
		}	
	}
	public void swap(int r1 ,int r2) throws P2PDDSQLException{
		D_TermsInfo temp;
		temp = data.get(r1);
		data.set(r1, data.get(r2));
		data.set(r2, temp);
		int priorityTemp = data.get(r1).priority;
		data.get(r1).priority = data.get(r2).priority;
		data.get(r2).priority = priorityTemp;
		data.get(r1).storeNoSync("update");
		data.get(r2).storeNoSync("update");
		this.fireTableDataChanged();
	}   
    private TermsPanel getPanel() {
		return panel;
	}
	public void refresh() {
		data.removeAll(data);
		update(null, null);
	}

	public void setTable(TermsTable termsTable) {
		tables.add(termsTable);
	}
/*	
	@Override
	public void actionPerformed(ActionEvent e) {
		TableJButton bb =(TableJButton)e.getSource();
		QualitesTable q = new QualitesTable(data.get(bb.rowNo));
		JPanel p = new JPanel(new BorderLayout());
		p.add(q.getScrollPane());
		final JFrame frame = new JFrame();
		frame.setContentPane(p);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		frame.pack();
		frame.setSize(600,300);
		frame.setVisible(true);
        JButton okBt = new JButton("   OK   ");
        okBt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            frame.hide();
           }
        });

		p.add(okBt,BorderLayout.SOUTH);
//		JOptionPane.showMessageDialog(null,p);
  //      JOptionPane.showMessageDialog(null,p,"Test Qualities Info", JOptionPane.DEFAULT_OPTION, null);
        

	}
*/
/*	public static void main(String args[]) {
		JFrame frame = new JFrame();
		try {
			Application.db = new DBInterface(Application.DEFAULT_DELIBERATION_FILE);
			JPanel test = new JPanel();
			//frame.add(test);
			test.setLayout(new BorderLayout());
			UpdatesTable t = new UpdatesTable(Application.db);
			//t.getColumnModel().getColumn(TABLE_COL_QOT_ROT).setCellRenderer(new ComboBoxRenderer());
			test.add(t);
			//PeersTest newContentPane = new PeersTest(db);
			//newContentPane.setOpaque(true);
			//frame.setContentPane(t.getScrollPane());
			test.add(t.getTableHeader(),BorderLayout.NORTH);
			frame.setContentPane(test);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setSize(800,300);
			frame.setVisible(true);
		} catch (P2PDDSQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
*/
	public long get_TermID(int row) {
		if(row<0) return -1;
		try{
			return data.get(row).term_ID;
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}

}
