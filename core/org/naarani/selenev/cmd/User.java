package org.naarani.selenev.cmd;

import java.io.File;
import java.io.IOException;
import org.apache.log4j.Logger;
import org.naarani.selenev.ssh.ExecutionStatus;
import org.naarani.selenev.ssh.SshServerManager;
import org.naarani.selenev.ssh.centos.UserLibCentos;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

public class User extends ASelenevHostCmd {

	static private Logger logger = Logger.getLogger( User.class );

	@Override
	public ExecutionStatus exec( String[] args, String prv, String wk, SshServerManager svr ) throws Exception {
		OptionParser parser = super.basicParser();
        //
        parser.accepts( "name" ).withRequiredArg().describedAs( "Name of the user to create" );
        parser.accepts( "password" ).withRequiredArg().describedAs( "Password of the user to create" );
        parser.accepts( "generate_ssh_key", "Generate a SSH key for the user in question" );
        parser.accepts( "public_ssh_private_key", "Public private key inside user account" );
        parser.accepts( "ssh_key_passphrase", "Set the passphrase for the SSH key" ).withRequiredArg();
        parser.accepts( "sudoer", "Enable user to run sudo" );
        //
		if( args.length == 0 ){
			// zeroHelp( parser );
			throw new IOException( "wrong args [NONE]..." );
		} else {
	        OptionSet options = parser.parse( args );
	        // PRIVATE & WORKSPACE setting
	        if( options.has( "prv" ) ){
	        	prv = (String) options.valueOf( "prv" );
	        }
	        if( options.has( "wk" ) ) {
	        	wk = (String) options.valueOf( "wk" );
	        }
	        //
	        String name = (String) options.valueOf( "name" );
	        String password = (String) options.valueOf( "password" );
	        boolean generate_ssh_key = options.has( "generate_ssh_key" );
	        boolean public_ssh_private_key = options.has( "public_ssh_private_key" );
	        String ssh_key_passphrase = (String) options.valueOf( "ssh_key_passphrase" );
			boolean sudo = true;
			//
			UserLibCentos ul = new UserLibCentos();
			ExecutionStatus status = ul.createUser( svr, sudo, name, password );
			if( status != ExecutionStatus.Done )
				return status;
			File f1 = new File( prv, "users/key_" + name );
			File f2 = new File( prv, "users/key_" + name + ".pub" );
			if( !f1.exists() ){
				status = ul.generateSshKey( f1.getAbsolutePath(), svr, ssh_key_passphrase.getBytes() );
				if( status != ExecutionStatus.Done )
					return status;
			}
			if( generate_ssh_key ){
				status = ul.setUserSshKey( svr, sudo, name, f2 );
				if( status != ExecutionStatus.Done )
					return status;
			}
			if( public_ssh_private_key ){
				status = ul.setUserSshPrivateKey( svr, sudo, name, f1, false );
				if( status != ExecutionStatus.Done )
					return status;
			}
			return status;
		}
	}

}