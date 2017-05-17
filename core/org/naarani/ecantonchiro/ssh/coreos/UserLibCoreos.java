package org.naarani.ecantonchiro.ssh.coreos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.naarani.core.exceptions.StopAction;
import org.naarani.ecantonchiro.ssh.CliInteract;
import org.naarani.ecantonchiro.ssh.ExecutionStatus;
import org.naarani.ecantonchiro.ssh.IUserLib;
import org.naarani.ecantonchiro.ssh.SshServerManager;
import org.naarani.ecantonchiro.ssh.centos.FileLibCentos;

import com.jcraft.jsch.KeyPair;

public class UserLibCoreos implements IUserLib {
	
	FileLibCentos fil = new FileLibCentos();

	@Override
	public String checkLinuxDistro( SshServerManager svr ) throws StopAction {
		svr.getCmdLog().reset();
		return "UNKNOWN";
	}

	@Override
	public String checkKernel( SshServerManager svr ) throws StopAction {
		svr.getCmdLog().reset();
		return "UNKNOWN";
	}

	@Override
	public ExecutionStatus checkUserExists( SshServerManager svr, String name ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdLog().reset();
		ExecutionStatus status;
		status = fil.fileOrDirExists( svr, "/home/" + name );
		return status;
	}

	@Override
	// https://coreos.com/os/docs/latest/adding-users.html
	public ExecutionStatus createUser( final SshServerManager svr, boolean sudo, String name, final String pwd ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdLog().reset();
		ExecutionStatus status;
		if( checkUserExists( svr, name ) != ExecutionStatus.Done_CHECK_TRUE ){
			try {
				if( sudo )
					status = svr.sudoCommand( "useradd -p -U -m " + name + " -G sudo " );
				else
					status = svr.command( "useradd -p -U -m " + name + " -G sudo " );
			} catch ( StopAction e ){
				return ExecutionStatus.notConnected;
			}
		} else
			status = ExecutionStatus.Done;
		if( status == ExecutionStatus.Done ){
			try {
				if( sudo )
					status = svr.sudoCommand( "passwd " + name, new CliInteract(){
						
						@Override
						public String action(){
							String msg = svr.getCmdLog().getText();
							if( msg.endsWith( ": " ) ){
								return pwd + "\n";
							}
							return null;
						}
						
					});
				else
					status = svr.command( "passwd " + name, new CliInteract(){
						
						@Override
						public String action(){
							String msg = svr.getCmdLog().getText();
							if( msg.endsWith( ": " ) ){
								return pwd + "\n";
							}
							return null;
						}
						
					});
			} catch ( StopAction e ){
				return ExecutionStatus.notConnected;
			}
			return ExecutionStatus.Done;
		}
		return status;
	}
	
//	@Override
	public ExecutionStatus deleteUser( SshServerManager svr, boolean sudo, String name ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		svr.getCmdLog().reset();
		ExecutionStatus status;
		try {
			if( sudo )
				status = svr.sudoCommand( "userdel -r " + name );
			else
				status = svr.command( "userdel -r " + name );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		return status;
	}
	
	@Override
	public ExecutionStatus makeUserSudoer( SshServerManager svr, boolean sudo, String name ){
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		// FIXME
		return ExecutionStatus.Done;
	}

	@Override
	/**
	 * 
	 * 
	 * 
	 * @param svr [it's need mainly to gain accesso to LOG management, then to create key with lib]
	 *  
	 * @return
	 */
	public ExecutionStatus generateSshKey( String filename, SshServerManager svr, byte[] unlockIdentity ){
		try {
			KeyPair kpair = KeyPair.genKeyPair( svr.getJsch(), KeyPair.RSA ); // ONLY USE RsA the other is DEPRECATED for security reason
			if( unlockIdentity == null ){
				kpair.writePrivateKey( filename );
			} else {
				kpair.writePrivateKey( filename, unlockIdentity );
			}
		    kpair.writePublicKey( filename + ".pub", "created by javaKUBE: " + svr.getUser() );
	//	    System.out.println( "Finger print: " + kpair.getFingerPrint() );
		    kpair.dispose();
			return ExecutionStatus.Done;
	    } catch( Exception e ){
	    	StringWriter writer = new StringWriter();
	    	e.printStackTrace( new PrintWriter( writer ) );
			svr.getCmdErrLog().writeMsg( writer.toString() );
			return ExecutionStatus.unknownError;
	    }
	}
	
	@Override
	public ExecutionStatus setUserSshKey( SshServerManager svr, boolean sudo, String realname, File fileRef2 ) throws IOException {
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		String ref = sudo ? realname : "";
		String remotePath = "~" + ref + "/.ssh";
		ExecutionStatus status;
		if( sudo )
			status = fil.sudoFileOrDirExists( svr, remotePath );
		else
			status = fil.fileOrDirExists( svr, remotePath );
		if( status == ExecutionStatus.Done_CHECK_TRUE ){
			status = fil.sudoChown( svr, realname + ":" + realname, "~" + ref + "/.ssh", null );
			if( status != ExecutionStatus.Done ){
				return status;
			}
		} else if( status == ExecutionStatus.Done_CHECK_FALSE ){
			try {
				if( sudo )
					status = svr.sudoCommand( "mkdir ~" + ref + "/.ssh" );
				else
					status = svr.command( "mkdir ~" + ref + "/.ssh" );
				if( status != ExecutionStatus.Done ){
					return status;
				}
				status = fil.sudoChown( svr, realname + ":" + realname, "~" + ref + "/.ssh", null );
			} catch (StopAction e) {
				return ExecutionStatus.notConnected;
			}
			if( status != ExecutionStatus.Done )
				return status;
		} else {
			return status;
		}
		try {
			if( sudo )
				status = svr.sudoCommand( "chmod 700 ~" + ref + "/.ssh" );
			else
				status = svr.command( "chmod 700 ~" + ref + "/.ssh" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		if( status != ExecutionStatus.Done )
			return status;
		remotePath = "~" + ref + "/.ssh/authorized_keys";
		try {
			if( sudo ){
				status = fil.sudoCreateCatFile( svr, remotePath, fileRef2 );
				if( status != ExecutionStatus.Done )
					return status;
				status = fil.sudoChown( svr, realname + ":" + realname, remotePath, null );
			} else {
				FileInputStream in = new FileInputStream( fileRef2 );
				status = svr.scpTO( fileRef2.getName(), in, fileRef2.lastModified(), fileRef2.length(), remotePath );
				in.close();
			}
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		if( status != ExecutionStatus.Done )
			return status;
		if( sudo )
			status = fil.sudoFileOrDirExists( svr, remotePath );
		else
			status = fil.fileOrDirExists( svr, remotePath );
		if( status == ExecutionStatus.Done_CHECK_TRUE ){
			try {
				if( sudo )
					status = svr.sudoCommand( "chmod 600 ~" + ref + "/.ssh/authorized_keys" );
				else
					status = svr.command( "chmod 600 ~" + ref + "/.ssh/authorized_keys" );
			} catch (StopAction e) {
				return ExecutionStatus.notConnected;
			}
		} else if( status == ExecutionStatus.Done_CHECK_FALSE ){
			return status;
		} else {
			return status;
		}
		// # Disable password authentication forcing use of keys
		// PasswordAuthentication no
		return status;
	}
	
	@Override
	public ExecutionStatus setUserSshPrivateKey( SshServerManager svr, boolean sudo, String realname, File fileRef2, boolean closePasswordAccess ) throws IOException {
		if( !svr.isConnected() ){
			return ExecutionStatus.notConnected;
		}
		String ref = sudo ? realname : "";
		String remotePath = "~" + ref + "/.ssh";
		ExecutionStatus status;
		if( sudo )
			status = fil.sudoFileOrDirExists( svr, remotePath );
		else
			status = fil.fileOrDirExists( svr, remotePath );
		if( status == ExecutionStatus.Done_CHECK_TRUE ){
			status = fil.sudoChown( svr, realname + ":" + realname, "~" + ref + "/.ssh", null );
			if( status != ExecutionStatus.Done ){
				return status;
			}
		} else if( status == ExecutionStatus.Done_CHECK_FALSE ){
			try {
				if( sudo )
					status = svr.sudoCommand( "mkdir ~" + ref + "/.ssh" );
				else
					status = svr.command( "mkdir ~" + ref + "/.ssh" );
				if( status != ExecutionStatus.Done ){
					return status;
				}
				status = fil.sudoChown( svr, realname + ":" + realname, "~" + ref + "/.ssh", null );
			} catch (StopAction e) {
				return ExecutionStatus.notConnected;
			}
			if( status != ExecutionStatus.Done )
				return status;
		} else {
			return status;
		}
		try {
			if( sudo )
				status = svr.sudoCommand( "chmod 700 ~" + ref + "/.ssh" );
			else
				status = svr.command( "chmod 700 ~" + ref + "/.ssh" );
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		if( status != ExecutionStatus.Done )
			return status;
		remotePath = "~" + ref + "/.ssh/id_rsa";
		try {
			if( sudo ){
				status = fil.sudoCreateCatFile( svr, remotePath, fileRef2 );
				if( status != ExecutionStatus.Done )
					return status;
				status = fil.sudoChown( svr, realname + ":" + realname, remotePath, null );
			} else {
				FileInputStream in = new FileInputStream( fileRef2 );
				status = svr.scpTO( fileRef2.getName(), in, fileRef2.lastModified(), fileRef2.length(), remotePath );
				in.close();
			}
		} catch (StopAction e) {
			return ExecutionStatus.notConnected;
		}
		if( status != ExecutionStatus.Done )
			return status;
		if( sudo )
			status = fil.sudoFileOrDirExists( svr, remotePath );
		else
			status = fil.fileOrDirExists( svr, remotePath );
		if( status == ExecutionStatus.Done_CHECK_TRUE ){
			try {
				if( sudo )
					status = svr.sudoCommand( "chmod 600 ~" + ref + "/.ssh/id_rsa" );
				else
					status = svr.command( "chmod 600 ~" + ref + "/.ssh/id_rsa" );
			} catch (StopAction e) {
				return ExecutionStatus.notConnected;
			}
		} else if( status == ExecutionStatus.Done_CHECK_FALSE ){
			return status;
		} else {
			return status;
		}
		if( closePasswordAccess ){
			// Once you've checked you can successfully login to the server using your public/private key pair,
			// you can disable password authentication completely by adding the following setting to your /etc/ssh/sshd_config file:
			// # Disable password authentication forcing use of keys
			// PasswordAuthentication no
		}
		return status;
	}

}