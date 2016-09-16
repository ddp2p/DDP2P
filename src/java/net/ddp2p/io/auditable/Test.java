package net.ddp2p.io.auditable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import net.ddp2p.ASN1.Decoder;
import net.ddp2p.ASN1.Encoder;

class InterfaceSupplier {
	static ArrayList<String> ready_list = new ArrayList<String>();
	static ArrayList<String> busy_list = new ArrayList<String>();
	
	/**
	 * Reads IPs list of the alias interfaces into ready_list 
	 * 
	 * @param interface_list
	 * Path to interface_list file created by bash script.
	 * 
	 */
	public static void init(String interface_list) {
	    BufferedReader br = null;
	    try {
	        String sCurrentLine;
	        br = new BufferedReader(new FileReader(interface_list));

	        while ((sCurrentLine = br.readLine()) != null) {
	        	String[] arr = sCurrentLine.split(" ");
	        	ready_list.add(arr[1]);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            if (br != null) br.close();
	        } catch (IOException ex) {
	            ex.printStackTrace();
	        }
	    }
	}
	
	/**
	 * Moves the first element in ready_list to busy_list
	 * 
	 * @return first element in ready_list or null
	 */
	public static String getVacant() {
		if (ready_list.size() == 0)
			return null;
		
		String response = ready_list.remove(0);
		busy_list.add(response);
		
		return response;
	}
}

class ServerWorker extends Thread {
	SocketAddress endpoint;
	
	public ServerWorker(String interface_ip) {
		try {
			InetAddress addr = InetAddress.getByName(interface_ip);
			endpoint = new InetSocketAddress(addr, 7070);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		try {
			AuditableServerSocket listener = new AuditableServerSocket();
			listener.bind(endpoint);
			
            AuditableSocket socket = (AuditableSocket) listener.accept();

            long conn_id = Audit.newConnection(socket.getInetAddress().toString(), socket.getRemoteSocketAddress().toString());
            socket.setConnectionAuditId(conn_id);
            
            AuditableOutputStream os = (AuditableOutputStream) socket.getOutputStream();
            //os.write(new Encoder(999).getBytes());    
            os.write((new Encoder()).initSequence().addToSequence(new Encoder(999)).getBytes());
            
            listener.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class ClientWorker extends Thread {
	private String server_ip;
	private byte buffer[] = new byte[10240];
	
	public ClientWorker(String server_ip) {
		this.server_ip = server_ip;
	}
	
	public void run() {
		try {
			sleep(300);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			AuditableSocket socket 	= new AuditableSocket(server_ip, 7070);
			AuditableInputStream is = (AuditableInputStream) socket.getInputStream();
			
            long conn_id = Audit.newConnection(socket.getInetAddress().toString(), socket.getRemoteSocketAddress().toString());
            socket.setConnectionAuditId(conn_id);
            
			int len = is.read(buffer);
			if (len == 0){
				System.out.println("No data received!");
				return;
			}
			
			Decoder dec = new Decoder(buffer, 0, len);
			if (!dec.fetchAll(is)) {
				System.err.println("Buffer too small for receiving update answer!");
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

public class Test {
	
	/** Should use the given script to run this test */
	public static void main(String[] args) throws SocketException {
		int instances_count 	= Integer.valueOf(args[0]);
		String db_path 			= args[1];
		String interface_list 	= args[2];
		
		AuditableSocket.simulated = true;
		Audit.init(db_path);
		InterfaceSupplier.init(interface_list);
		
		for (int i = 0; i < instances_count; i++) {
			String vacant_interface = InterfaceSupplier.getVacant();
			(new ServerWorker(vacant_interface)).start();
			(new ClientWorker(vacant_interface)).start();	
		}
	}
}
