package org.naarani.core.exceptions;

public class StopAction extends Exception {
	
	private static final long serialVersionUID = 1L;

	protected String descr;

	public StopAction() {
		super();
		this.descr = "NONE";
	}

	public StopAction( String msg, Exception e ){
		super( msg + ":" + e.getMessage(), e );
		this.descr = msg;
	}

	public StopAction( Exception e ){
		super( e.getMessage() );
		this.descr = e.getMessage();
	}

	public StopAction( String descr ){
		super( descr );
		this.descr = descr;
	}

	public String toString(){
		return descr;
	}
	
}