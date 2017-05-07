package org.naarani.selenev.cmd;

import org.apache.log4j.Logger;
import org.naarani.selenev.ssh.ExecutionStatus;
import org.naarani.selenev.ssh.SshServerManager;

public class Shell {

	static private Logger logger = Logger.getLogger( Shell.class );

	public ExecutionStatus shell( String command, String prv, String wk, SshServerManager svr, boolean sudo ) throws Exception {
		ExecutionStatus status = svr.command( command );
		svr.getCmdLog().reset();
		svr.getCmdErrLog().reset();
		if( sudo ){
			status = svr.sudoCommand( command );
		} else {
			status = svr.command( command );
		}
		return status;
	}

	public ExecutionStatus yum( String command, String prv, String wk, SshServerManager svr ) throws Exception {
		ExecutionStatus status = svr.command( command );
		svr.getCmdLog().reset();
		svr.getCmdErrLog().reset();
		status = svr.sudoCommand( "yum -y " + command );
		return status;
	}

}