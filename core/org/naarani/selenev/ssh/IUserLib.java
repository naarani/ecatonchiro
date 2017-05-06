package org.naarani.selenev.ssh;

import java.io.File;
import java.io.IOException;

import org.naarani.core.exceptions.StopAction;

public interface IUserLib {

	public ExecutionStatus checkUserExists( SshServerManager svr, String name );

	public ExecutionStatus createUser( final SshServerManager svr, boolean sudo, String name, final String pwd );

	public ExecutionStatus makeUserSudoer( SshServerManager svr, boolean sudo, String name );
	
	public String checkLinuxDistro( SshServerManager svr ) throws StopAction;

	public String checkKernel( SshServerManager svr ) throws StopAction;

	public ExecutionStatus generateSshKey( String filename, SshServerManager svr, byte[] unlockIdentity );

	public ExecutionStatus setUserSshKey( SshServerManager svr, boolean sudo, String realname, File fileRef2 ) throws IOException;

	public ExecutionStatus setUserSshPrivateKey( SshServerManager svr, boolean sudo, String realname, File fileRef2, boolean closePasswordAccess ) throws IOException;

}