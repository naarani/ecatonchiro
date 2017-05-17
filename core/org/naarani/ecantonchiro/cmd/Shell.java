package org.naarani.ecantonchiro.cmd;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.naarani.core.exceptions.StopAction;
import org.naarani.ecantonchiro.ssh.ExecutionStatus;
import org.naarani.ecantonchiro.ssh.SshServerManager;

public class Shell {

	static private Logger logger = Logger.getLogger( Shell.class );

	public ExecutionStatus shell( String command, String prv, String wk, SshServerManager svr, boolean sudo ) throws Exception {
		svr.getCmdLog().reset();
		svr.getCmdErrLog().reset();
		ExecutionStatus status;
		if( sudo ){
			status = svr.sudoCommand( command );
		} else {
			status = svr.command( command );
		}
		return status;
	}

	public ExecutionStatus yum( String command, String prv, String wk, SshServerManager svr ) throws Exception {
		svr.getCmdLog().reset();
		svr.getCmdErrLog().reset();
		ExecutionStatus status = svr.sudoCommand( "yum -y " + command );
		return status;
	}

	public ExecutionStatus yum( HashMap map, String prv, String wk, SshServerManager svr ) throws Exception {
		svr.getCmdLog().reset();
		svr.getCmdErrLog().reset();
		//
		String state = (String) map.get( "state" );
		String name = (String)map.get( "name" );
		if( state == null )
			throw new StopAction( "missing STATE param in YUM " + state );
		if( name == null )
			throw new StopAction( "missing NAME param in YUM " + state );
		if( state.compareTo( "present" ) == 0 || state.compareTo( "installed" ) == 0 || state.compareTo( "latest" ) == 0 ) {
			state = " install ";
		} else if( state.compareTo( "absent" ) == 0 || state.compareTo( "removed" ) == 0 ){
			state = " remove ";
		} else {
			throw new StopAction( "wrong STATE param in YUM " + state );
		}
		String command = state + " " + name;
		ExecutionStatus status = svr.sudoCommand( "yum -y " + command );
		return status;
	}

}