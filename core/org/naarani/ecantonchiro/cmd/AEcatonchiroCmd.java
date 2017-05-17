package org.naarani.ecantonchiro.cmd;

import joptsimple.OptionParser;

public class AEcatonchiroCmd {

	public static void main( String[] args ) throws Exception {
		org.apache.log4j.BasicConfigurator.configure();
		new AEcatonchiroCmd().exec( args, null, null );
	}

	public Object exec( String[] args, String prv, String wk ) throws Exception {
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