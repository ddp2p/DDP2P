package net.ddp2p.io.auditable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import net.ddp2p.io.auditable.AuditableInputStream;
import net.ddp2p.io.auditable.AuditableOutputStream;

public class AuditableSocket extends Socket {
	public static boolean simulated = false;
	public int port;
	private long connection_audit_id = -1;
    private AuditableInputStream in;
    private AuditableOutputStream out;

    
    public AuditableSocket() { 
    	super(); 
    }

    public AuditableSocket(String host, int port) throws IOException {
        super(host, port);
    }

    public InputStream getInputStream() throws IOException {
    	if (! simulated) {
    		return super.getInputStream();
    	}
    	
        if (in == null) {
            in = new AuditableInputStream(super.getInputStream(), this);
        }
        return in;
    }

    public OutputStream getOutputStream() throws IOException {
        if (! simulated) {
        	super.getOutputStream();
        }

        if (out == null) {
            out = new AuditableOutputStream(super.getOutputStream(), this);
        }
        
        return out;
    }
    
    public synchronized void close() throws IOException {
        if (simulated) {
            getOutputStream().flush();
        	return;
        }
        
        super.close();
    }

	public void setConnectionAuditId(long conn_id) {
		connection_audit_id = conn_id;
	}
	
	public long connectionAuditId() {
		return connection_audit_id;
	}
	
}