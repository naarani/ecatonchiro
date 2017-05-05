package org.naarani.selenev;

import java.io.File;
import java.io.IOException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Run {

	public static void main( String[] args ) throws IOException {
		OptionParser parser = new OptionParser();
		// parser.accepts( "flag" );
		// parser.accepts( "count" ).withRequiredArg().ofType( Integer.class );
		// parser.accepts( "level" ).withOptionalArg().ofType( Level.class );
        parser.accepts( "prv" ).withRequiredArg().describedAs( "Vault" ).defaultsTo( "private" );
        parser.accepts( "wk" ).withRequiredArg().describedAs( "Workspace" ).defaultsTo( "workspace/production" );
        parser.accepts( "test", "Only list tast, not run 'em" );
        parser.acceptsAll( java.util.Arrays.asList( "h", "?", "help" ), "show help" ).forHelp();
		if( args.length == 0 ) {
			System.out.println( "" );
			System.out.println( "Selene V - automation engine (C) 2017 Diego Zanga" );
			System.out.println( "Version " + Version.VER );
			System.out.println( "" );
			System.out.println( "" );
			parser.printHelpOn( System.out );
		} else {
			org.apache.log4j.BasicConfigurator.configure();
			// parser.printHelpOn( System.out );
			// --------------------------------------------------------------------
	        OptionSet options = parser.parse( args );
	        boolean demo = options.has( "demo" );
	        String prv = (String) options.valueOf( "prv" );
	        String wk = (String) options.valueOf( "wk" );
	        String main = "";
	        if( options.nonOptionArguments().size() == 1 ) {
		        main = (String) options.nonOptionArguments().get( 0 );
	        } else {
				System.err.println( "wrong arguments..." );
				parser.printHelpOn( System.err );
	        	return;
	        } 
	        //
	        Engine engine = new Engine();
			engine.setWk( wk );
			engine.setPrivate( prv );
			engine.setDemo( demo );
			engine.setMain( main );
			engine.run();
		}
	}

}