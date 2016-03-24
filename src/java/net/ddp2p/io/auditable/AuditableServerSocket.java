package net.ddp2p.io.auditable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class AuditableServerSocket extends ServerSocket {

	public AuditableServerSocket() throws IOException {
		super();
	}
	
	public AuditableServerSocket(int port) throws IOException {
		super(port);
	}
	
	public void setSoTimeout(int timeout) throws SocketException {
		if (! AuditableSocket.simulated) { 
			super.setSoTimeout(timeout); 
			return; 
		}
	}
	
	public Socket accept() throws IOException {
		if (! AuditableSocket.simulated) 
			return super.accept();
		
		AuditableSocket socket = new AuditableSocket();
		implAccept(socket);
		
		return socket;
	}
}