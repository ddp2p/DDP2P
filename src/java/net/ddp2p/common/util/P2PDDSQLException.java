package net.ddp2p.common.util;
@SuppressWarnings("serial")
public class P2PDDSQLException extends Exception{
	Object _e;
	String msg;
	public P2PDDSQLException(String _msg) {
		super();
		msg = _msg;
	}
	public P2PDDSQLException(Exception e) {
		super();
		msg = e.getLocalizedMessage();
		_e = e;
	}
	public String toString(){
		return (msg==null)?Util.getString(_e):msg;
	}
}
