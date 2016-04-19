package net.ddp2p.io.auditable;
import net.ddp2p.common.util.P2PDDSQLException;
import net.ddp2p.java.db.DB_Implementation_JDBC_SQLite;
public class Audit {
	public static final boolean DEBUG = false;
	private static DB_Implementation_JDBC_SQLite db;
	private static long last_packet_time;
	public static void init(String db_filename) {
		Audit.last_packet_time 	= System.nanoTime();
		try {
			db = new DB_Implementation_JDBC_SQLite();
			db.open(db_filename);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public static synchronized long newConnection(String server, String client) {
		long connection_id = -1;
		try {
	        java.util.Date date	= new java.util.Date();
	        String time = new java.sql.Timestamp(date.getTime()).toString();
			String[] params = new String[] {server, client, time};
			connection_id = db.insert("INSERT INTO connection (server, client, created) VALUES (?, ?, ?);", params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
		return connection_id;
	}
	private static synchronized void add(String[] params) {
		Audit.last_packet_time 	= System.nanoTime();
		try {
			db.insert("INSERT INTO packet (connection, sender, tag, size, delay) VALUES (?, ?, ?, ?, ?);", params, DEBUG);
		} catch (P2PDDSQLException e) {
			e.printStackTrace();
		}
	}
	public static synchronized void write(long connection_id, String sender, int b) {
		String[] params = new String[] { 
				String.valueOf(connection_id),  
				sender, 
				String.valueOf(2), 
				String.valueOf(Integer.SIZE), 
				String.valueOf(System.nanoTime() - last_packet_time)
		};
		add(params);
	}
	public static synchronized void write(long connection_id, String sender, byte[] b) {
		String[] params = new String[] { 
				String.valueOf(connection_id),  
				sender, 
				String.valueOf(b[0] & 0x1f), 
				String.valueOf(b.length), 
				String.valueOf(System.nanoTime() - last_packet_time)
		};
		add(params);
	}
	public static synchronized void write(long connection_id, String sender, byte[] b, int off, int len) {
		String[] params = new String[] { 
				String.valueOf(connection_id),  
				sender, 
				String.valueOf(b[off] & 0x1f), 
				String.valueOf(len), 
				String.valueOf(System.nanoTime() - last_packet_time)
		};
		add(params);		
	}
	public static synchronized void read(long connection_id, String sender, int b) {
		String[] params = new String[] { 
				String.valueOf(connection_id), 
				sender,  
				String.valueOf(2), 
				String.valueOf(Integer.SIZE), 
				String.valueOf(System.nanoTime() - last_packet_time)
		};
		add(params);
	}
	public static synchronized void read(long connection_id, String sender, byte[] b, int len) {
		String[] params = new String[] { 
				String.valueOf(connection_id), 
				sender,  
				String.valueOf(b[0] & 0x1f), 
				String.valueOf(len), 
				String.valueOf(System.nanoTime() - last_packet_time)
		};
		add(params);
	}
	public static synchronized void read(long connection_id, String sender, byte[] b, int off, int len) {
		String[] params = new String[] { 
				String.valueOf(connection_id), 
				sender,  
				String.valueOf(b[off] & 0x1f), 
				String.valueOf(len), 
				String.valueOf(System.nanoTime() - last_packet_time)
		};
		add(params);
	}
}
