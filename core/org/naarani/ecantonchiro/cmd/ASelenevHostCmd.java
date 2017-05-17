package org.naarani.ecantonchiro.cmd;

import org.naarani.ecantonchiro.ssh.SshServerManager;

import joptsimple.OptionParser;

public class ASelenevHostCmd {

	public static void main( String[] args ) throws Exception {
		org.apache.log4j.BasicConfigurator.configure();
		new ASelenevHostCmd().exec( args, null, null, null );
	}

	public Object exec( String[] args, String prv, String wk, SshServerManager ssh ) throws Exception {
        return null;
	}

	public OptionParser basicParser() {
		OptionParser parser = new OptionParser();
        parser.accepts( "prv" ).withRequiredArg().describedAs( "Vault" ).defaultsTo( "private" );
        parser.accepts( "wk" ).withRequiredArg().describedAs( "Workspace" ).defaultsTo( "workspace/production" );
        parser.acceptsAll( java.util.Arrays.asList( "h", "?", "help" ), "show help" ).forHelp();
		return parser;
	}

}