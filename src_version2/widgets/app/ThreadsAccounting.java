package widgets.app;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import ASN1.Encoder;
import util.Util;
import widgets.org.Orgs;
import widgets.threads.ThreadsView;
import static util.Util.__;

class Thread_Info {
	String name;
	String state;
	Calendar creation_date;
	String _creation_date;
	Calendar last_ping_date;
	String _last_ping_date;
	Thread thread;
	public String toString() {
		return "ThreadInfo["
				+"\n name = "+name
				+"\n state = "+state
				+"\n thread = "+thread
				+"\n...]";
	}
}

@SuppressWarnings("serial")
public class ThreadsAccounting extends AbstractTableModel implements TableModel {
	static final Hashtable<Thread, Thread_Info> running = new Hashtable<Thread, Thread_Info>();
	static final ArrayList<Thread_Info> view = new ArrayList<Thread_Info>();
	static final Object monitor_running = new Object();
	static final String CREATION = __("CREATION");
	static final String REPLICATED_CREATION = __("2nd CREATION");
	/**
	 * 10s for deciding hot entries
	 */
	public static final long HOT = ThreadsView.HOT_SEC*1000;
	private static final boolean DEBUG = false;
	
	public static void registerThread() {
		registerThread(Thread.currentThread().getName());
	}
	public static void registerThread(String name) {
		if (ThreadsAccounting.DEBUG) System.out.println("ThreadsAccounting:register "+name);
		Thread crt = Thread.currentThread();
		Thread_Info old_info, info = new Thread_Info();
		info.name = name;
		info.creation_date = Util.CalendargetInstance();
		info._creation_date = Encoder.getGeneralizedTime(info.creation_date);
		info.last_ping_date = info.creation_date;
		info._last_ping_date = info._creation_date;
		info.state = CREATION;
		synchronized (monitor_running) {
			if ((old_info = running.get(crt)) != null) {
				System.out.println("ThreadsAccounting:register Already existing: "+old_info);
				Util.printCallPath("ThreadsAccounting:register "+crt);
				old_info._last_ping_date = info._creation_date;
				old_info.last_ping_date = info.creation_date;
				old_info.state = REPLICATED_CREATION;
				int idx = view.indexOf(old_info);
				SwingUtilities.invokeLater(new UpdateThreadsViewRunnable(idx, 
						UpdateThreadsViewRunnable.UPDATE));
				return;
			}
			running.put(crt, info);
			view.add(info);
			SwingUtilities.invokeLater(new UpdateThreadsViewRunnable(view.size()-1, 
					UpdateThreadsViewRunnable.INSERT));
		}
	}
	static
	class UpdateThreadsViewRunnable implements Runnable {
		public static final int INSERT = 0;
		public static final int DELETE = 1;
		public static final int UPDATE = 2;
		int row_inserted = -1;
		int row_deleted = -1;
		int row_changed = -1;
		int rows = -1;
		UpdateThreadsViewRunnable(){
			rows = 1;
		}
		UpdateThreadsViewRunnable(int row, int type){
			switch(type) {
			case INSERT:
				row_inserted = row;
				break;
			case DELETE:
				row_deleted = row;
				break;
			case UPDATE:
				row_changed = row;
				break;
			}
		}
		@Override
		public void run() {
			try{
				_run();
			}catch(Exception e) {
				System.out.println("ThreadsAccounting:run:Found");
			}
		}
		public void _run() {
			//return;
			
			synchronized(ThreadsAccounting.monitor_running) {
				rows = 0;
				if(rows >= 0) {
					ThreadsAccounting.instance().fireTableDataChanged();
					return;
				}
				if(row_inserted >=0 )
					ThreadsAccounting.instance().fireTableRowsInserted(row_inserted, row_inserted);
				if(row_deleted >=0 )
					ThreadsAccounting.instance().fireTableRowsDeleted(row_deleted, row_deleted);
				if(row_changed >=0 )
					ThreadsAccounting.instance().fireTableRowsUpdated(row_changed, row_changed);
			}
			
		}
	}
	public static void unregisterThread() {
		Thread crt = Thread.currentThread();
		unregisterThread(crt);
	}
	public static void unregisterThread(Thread crt) {
		if (ThreadsAccounting.DEBUG) System.out.println("ThreadsAccounting:unregister "+crt.getName());
		synchronized (monitor_running) {
			Thread_Info info = running.remove(crt);
			if (info == null) {
				System.out.println("ThreadsAccounting:unregister unexisting: "+crt.getName());
				Util.printCallPath("ThreadsAccounting:unregister");
				return;
			}
			int idx = view.indexOf(info);
			view.remove(info);
			SwingUtilities.invokeLater(new UpdateThreadsViewRunnable(idx, 
					UpdateThreadsViewRunnable.DELETE));
		}
	}
	public static void ping(String state) {
		Thread crt = Thread.currentThread();
		Thread_Info old_info;
		synchronized (monitor_running) {
			if ((old_info = running.get(crt)) == null) {
				old_info = new Thread_Info();
				
				if (! SwingUtilities.isEventDispatchThread() && ! EventQueue.isDispatchThread()) {
					System.out.println("ThreadsAccounting:ping Already unexisting: "+crt);
					Util.printCallPath("ThreadsAccounting:ping unexisting");
					old_info.name = __("Unknown Thread:")+" "+crt.getName();
				} else {
					System.out.println("ThreadsAccounting:ping from Swing Event: "+crt);
					old_info.name = "Swing Event:"+" "+crt.getName();
				}
				
				old_info.creation_date = Util.CalendargetInstance();
				old_info._creation_date = Encoder.getGeneralizedTime(old_info.creation_date);
				old_info._last_ping_date = old_info._creation_date;
				old_info.last_ping_date = old_info.creation_date;
				old_info.state = CREATION;
				running.put(crt, old_info);
				view.add(old_info);
				SwingUtilities.invokeLater(new UpdateThreadsViewRunnable(view.size()-1, 
						UpdateThreadsViewRunnable.INSERT));
				return;
			}
			old_info.last_ping_date = Util.CalendargetInstance();
			old_info._last_ping_date = Encoder.getGeneralizedTime(old_info.last_ping_date);
			old_info.state = state;
			int idx = view.indexOf(old_info);
			SwingUtilities.invokeLater(new UpdateThreadsViewRunnable(idx, 
					UpdateThreadsViewRunnable.UPDATE));
		}
	}

	@Override
	public Class<?> getColumnClass(int col) {
		if(col == TABLE_COL_HOT) return Boolean.class;
		return String.class;
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int col) {
		return this.columnNames[col];
	}

	@Override
	public int getRowCount() {
		return view.size();
	}

	@Override
	public Object getValueAt(int row, int col) {
		//Object result = null;
		Thread_Info info;
		try{
			synchronized(monitor_running) {
				info = view.get(row);
			}
			if (info == null) return null;
		}catch(Exception e) {
			return null;
		}
		switch(col) {
		case TABLE_COL_NAME:
			return info.name;
		case TABLE_COL_STATE:
			return info.state;
		case TABLE_COL_HOT:
			long lapse = Util.CalendargetInstance().getTimeInMillis() - info.last_ping_date.getTimeInMillis();
			return new Boolean(lapse < HOT); 
		case TABLE_COL_CREATION:
			return info._creation_date;
		case TABLE_COL_PING:
			return info._last_ping_date;
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}

	@Override
	public void setValueAt(Object val, int row, int col) {
	}
	String columnNames[]={__("Name"),__("State"),__("Hot"),__("Creation"),__("Ping")};
	public static final int TABLE_COL_NAME = 0;
	public static final int TABLE_COL_STATE = 1;
	public static final int TABLE_COL_HOT = 2;
	public static final int TABLE_COL_CREATION = 3;
	public static final int TABLE_COL_PING = 4;
	ArrayList<ThreadsView> tables= new ArrayList<ThreadsView>();

	public void setTable(ThreadsView threadsView) {
		tables.add(threadsView);
	}
	final static ThreadsAccounting model = new ThreadsAccounting(); 
	public static ThreadsAccounting instance() {
		return model;
	}
}
