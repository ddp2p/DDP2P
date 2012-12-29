/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2012 Marius C. Silaghi
		Author: Marius Silaghi: msilaghi@fit.edu
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
package widgets.directories;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import com.almworks.sqlite4java.SQLiteException;

import config.Application;
import config.DD;
import config.DDIcons;

import util.DBInfo;
import util.DBInterface;
import util.DBListener;
import util.DBSelector;
import util.Util;
import widgets.components.BulletRenderer;
import static util.Util._;

@SuppressWarnings("serial")
public class Directories extends JTable {
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;

	BulletRenderer bulletRenderer = new BulletRenderer();
	public Directories() {
		super(new DirectoriesModel(Application.db));
		init();
	}
	public Directories(DBInterface _db) {
		super(new DirectoriesModel(_db));
		init();
	}
	public Directories(DirectoriesModel dm) {
		super(dm);
		init();
	}
	void init(){
		this.setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		initColumnSizes();
		this.getTableHeader().setToolTipText(
        _("Click to sort; Shift-Click to sort in reverse order"));
		this.setAutoCreateRowSorter(true);	
	}
	public JScrollPane getScrollPane(){
        JScrollPane scrollPane = new JScrollPane(this);
		this.setFillsViewportHeight(true);
		return scrollPane;
	}
    public JPanel getPanel() {
    	JPanel jp = new JPanel(new BorderLayout());
    	JScrollPane scrollPane = getScrollPane();
        scrollPane.setPreferredSize(new Dimension(400, 200));
        //jp.add(scrollPane, BorderLayout.CENTER);
        Application.directoriesData = new DirectoriesData();
        //jp.add(Application.directoriesData, BorderLayout.SOUTH);
        jp.add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, Application.directoriesData), BorderLayout.CENTER);
		return jp;
    }

	public TableCellRenderer getCellRenderer(int row, int column) {
		if ((column == DirectoriesModel.COL_UDP_ON)) return bulletRenderer;
		if ((column == DirectoriesModel.COL_TCP_ON)) return bulletRenderer;
		//if ((column == DirectoriesModel.COL_NAT)) return bulletRenderer;
		return super.getCellRenderer(row, column);
	}
	protected String[] columnToolTips = {null,null,_("A name you provide")};
    @SuppressWarnings("serial")
	protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
               java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                if(realIndex >= columnToolTips.length) return null;
				return columnToolTips[realIndex];
            }
        };
    }
	public void setUDPOn(String address, Boolean on){
		((DirectoriesModel) this.getModel()).setUDPOn(address, on);
	}
	public void setTCPOn(String address, Boolean on){
		((DirectoriesModel) this.getModel()).setTCPOn(address, on);
	}
	public void setNATOn(String address, Boolean on){
		((DirectoriesModel) this.getModel()).setNATOn(address, on);
	}
	public DirectoriesModel getModel(){
		return (DirectoriesModel) super.getModel();
	}
	private void initColumnSizes() {
        DirectoriesModel model = (DirectoriesModel)this.getModel();
        TableColumn column = null;
        Component comp = null;
        //Object[] longValues = model.longValues;
        TableCellRenderer headerRenderer =
            this.getTableHeader().getDefaultRenderer();
 
        for (int i = 0; i < model.getColumnCount(); i++) {
        	int headerWidth = 0;
        	int cellWidth = 0;
            column = this.getColumnModel().getColumn(i);
 
            comp = headerRenderer.getTableCellRendererComponent(
                                 null, column.getHeaderValue(),
                                 false, false, 0, 0);
            headerWidth = comp.getPreferredSize().width;
 
            for(int r=0; r<model.getRowCount(); r++) {
            	comp = this.getDefaultRenderer(model.getColumnClass(i)).
                             getTableCellRendererComponent(
                                 this, getValueAt(r, i),
                                 false, false, 0, i);
            	cellWidth = Math.max(comp.getPreferredSize().width, cellWidth);
            }
            if (DEBUG) {
                System.out.println("Initializing width of column "
                                   + i + ". "
                                   + "headerWidth = " + headerWidth
                                   + "; cellWidth = " + cellWidth);
            }
 
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }
	/**
	 * @param args
	 * @throws SQLiteException 
	 */
	public static void main(String[] args) throws SQLiteException {
		String dfname = Application.DELIBERATION_FILE;
		Application.db = new DBInterface(dfname);
		//DirectoriesTest dT = new DirectoriesTest(Application.db);
		//DirectoriesModel dirsM = new DirectoriesModel(Application.db);
		//Directories dirs = new Directories(dirsM);
		//JScrollPane scrollPane = new JScrollPane(dirs);
		//dirs.setFillsViewportHeight(true);
		DirectoriesTest.createAndShowGUI(Application.db);
	}

}

/**
 * 
 */
class DirectoriesModel extends AbstractTableModel implements TableModel, DBListener {
	private static final long serialVersionUID = 1L;
	private static final int COL_NAME = 2;
	static final int COL_NAT = 3;
	static final int COL_UDP_ON = 5;
	static final int COL_TCP_ON = 6;
	private static final boolean DEBUG = false;
	private static final boolean _DEBUG = true;
	Hashtable<String,Boolean> natByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Boolean> onUDPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Boolean> onTCPByIPport = new Hashtable<String,Boolean>();
	Hashtable<String,Integer> rowByIPport = new Hashtable<String,Integer>();
	DBInterface db;
	String ld;
	String _ld[];
	String columnNames[]={"IP","Port","Name","NAT Piercing","Relay","UDP","TCP"};
	int columns = columnNames.length-2;
	DirectoriesModel(DBInterface _db) {
		db = _db;
		db.addListener(this, new ArrayList<String>(Arrays.asList(table.application.TNAME)),
				DBSelector.getHashTable(table.application.TNAME, table.application.field, DD.APP_LISTING_DIRECTORIES));
		update(null, null);
	}
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}
	@Override
	public int getRowCount() {
		if(_ld==null) return 0;
		return _ld.length;
	}
	@Override
	public Object getValueAt(int row, int col) {
		if((_ld==null)||(_ld.length<=row)) return null;
		if(col == COL_UDP_ON)return this.onUDPByIPport.get(this.ipPort(row));
		if(col == COL_TCP_ON)return this.onTCPByIPport.get(this.ipPort(row));
		if(col == COL_NAT) return this.natByIPport.get(this.ipPort(row));
		
		String[] el = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		if(el.length<=col) return null;
		return el[col];
	}
	@Override
	public String getColumnName(int col) {
		return columnNames[col].toString();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return col==COL_NAME;
	}

	@Override
	public void setValueAt(Object value, int row, int col) {
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP) >= 0) return; 
		if((value+"").indexOf(DD.APP_LISTING_DIRECTORIES_SEP) >= 0) return; 
		String el[] = _ld[row].split(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		String result="";
		for(int k=0; k<columns; k++) {
			if(k > 0) result = result + DD.APP_LISTING_DIRECTORIES_ELEM_SEP;
			if(k==col) result = result + value;
			else if(k<el.length) result = result + el[k];
			else result = result+"";
		}
		_ld[row] = result;
		try {
			String dirs = Util.concat(_ld, DD.APP_LISTING_DIRECTORIES_SEP);
			if(DEBUG)System.out.println("Directories: setValueAt: Setting "+dirs);
			DD.setAppTextNoSync(DD.APP_LISTING_DIRECTORIES, dirs);
		} catch (SQLiteException e) {
			e.printStackTrace();
		}
		fireTableCellUpdated(row, col);
	}
	String ipPort(int k) {
		if((_ld==null)||(_ld.length<=k)) return null;
		int id1 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP);
		if(id1<0) return null;
		int id2 = _ld[k].indexOf(DD.APP_LISTING_DIRECTORIES_ELEM_SEP,id1+1);
		if(id2<0) return _ld[k];//id2 = _ld[k].length();
		return _ld[k].substring(0, id2);
	}
	void setUDPOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setUDPOn:"+address+" at "+on);
		onUDPByIPport.put(address, on);
		Integer Row = this.rowByIPport.get(address);
		if(Row == null) {
			if(DEBUG) System.out.println("DirectoriesModel:setUDPOn: No row for directory: "+address);
			return;
		}
		int row = Row.intValue();
		this.fireTableCellUpdated(row, COL_UDP_ON);
	}
	void setTCPOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setTCPOn:"+address+" at "+on);
		onTCPByIPport.put(address, on);
		Integer Row = this.rowByIPport.get(address);
		if(Row == null) {
			if(DEBUG) System.out.println("DirectoriesModel:setTCPOn: No row for directory: "+address);
			return;
		}
		int row = Row.intValue();
		this.fireTableCellUpdated(row, COL_TCP_ON);
	}
	void setNATOn(String address, Boolean on){
		if(DEBUG) System.out.println("DirectoriesModel:setNATOn:"+address+" at "+on);
		natByIPport.put(address, on);
		Integer Row = this.rowByIPport.get(address);
		if(Row == null) {
			if(DEBUG) System.out.println("DirectoriesModel:setNATOn: No row for directory: "+address);
			return;
		}
		int row = Row.intValue();
		this.fireTableCellUpdated(row, COL_NAT);
	}
	@Override
	public void update(ArrayList<String> table, Hashtable<String,DBInfo> info) {
		try {
			ld = DD.getAppText(DD.APP_LISTING_DIRECTORIES);
			if(DEBUG)System.out.println("Directories:update:"+ld);
			if(ld!=null) {
				_ld=ld.split(DD.APP_LISTING_DIRECTORIES_SEP);
				for(int k=0; k<_ld.length; k++) {
					if(DEBUG)System.out.println("Directories:update:"+k+" is "+_ld[k]);
					String ipPort = ipPort(k);
					if(ipPort == null) continue;
					this.rowByIPport.put(ipPort, new Integer(k));
				}
			}
		} catch (SQLiteException e) {
			e.printStackTrace();
		}		
		this.fireTableDataChanged();
	}
	@Override
	public Class<?> getColumnClass(int col) {
		if(col == COL_NAT) return Boolean.class;
		return String.class;
	}
/*
	@Override
	public void addTableModelListener(TableModelListener arg0) {		
	}


	@Override
	public void removeTableModelListener(TableModelListener arg0) {
		// 
	}
	*/
}
class DirectoriesTest extends JPanel {
    Directories tree;
    public DirectoriesTest(DBInterface db) {
    	super(new BorderLayout());
    	tree = new Directories( new DirectoriesModel(db));
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        add(scrollPane, BorderLayout.CENTER);
		//JScrollPane scrollPane = new JScrollPane(dirs);
		tree.setFillsViewportHeight(true);
    }
    public static void createAndShowGUI(DBInterface db) {
        JFrame frame = new JFrame("Directories Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        DirectoriesTest newContentPane = new DirectoriesTest(db);
        newContentPane.setOpaque(true);
        frame.setContentPane(newContentPane);
        frame.pack();
        frame.setVisible(true);
    }
}
