package org.naarani.selenev;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import org.apache.log4j.Logger;
import org.naarani.core.exceptions.StopAction;
import org.naarani.selenev.cmd.ASelenevCmd;
import org.naarani.selenev.cmd.ASelenevHostCmd;
import org.naarani.selenev.cmd.Hosts;
import org.naarani.selenev.cmd.User;
import org.naarani.selenev.jclouds.digitalocean.ProvisionDigitalOcean;
import org.naarani.selenev.ssh.SshServerManager;
import org.naarani.selenev.yaml.IncludeVars;
import org.naarani.selenev.yaml.TaskAction;
import org.naarani.selenev.yaml.YamlTaskLib;
import com.esotericsoftware.yamlbeans.YamlWriter;

public class Engine {

	static protected Logger logger = Logger.getLogger( Engine.class );

	protected String main;
	protected File wk;
	protected File prv;
	protected boolean demo;

	public boolean isDemo(){
		return demo;
	}

	public void setDemo(boolean demo){
		this.demo = demo;
	}

	public String getMain() {
		return main;
	}

	public void setMain(String main) {
		this.main = main;
	}

	public String getPrivate() {
		return prv.getAbsolutePath();
	}

	public void setPrivate(String prv) {
		this.prv = new File( prv );
	}

	public String getWk() {
		return wk.getAbsolutePath();
	}

	public void setWk(String wk) {
		this.wk = new File( wk );
	}

	protected ScriptEngine se; 
	
	public void run(){
		System.out.println( "setup embedded vars" );
		vars = new HashMap();
		vars.put( "nohosts", !new File( prv, "hosts" ).exists() 
				&& new File( wk, "hostdata" ).listFiles( new HostDataFilter() ) == null );
		try {
			File secrets = new File( prv, "secrets.yaml" );
			if( secrets.exists() ) {
				IncludeVars v1 = new IncludeVars();
				v1.addFile( secrets );
				vars.putAll( v1.getVars() );
			}
		} catch ( IOException e ){
			logger.error( "secrets.yaml errror... ", e );
		}
		//
		System.out.println( "setup javascript engine" );
        ScriptEngineManager sem = new ScriptEngineManager();
        se = sem.getEngineByName( "JavaScript" );
        //
        System.out.println( "running " + main + " workspace " + wk );
		int cmdDone = 0;
		int cmdSkipped = 0;
		int cmdErrors = 0;
		YamlTaskLib lib = new YamlTaskLib();
		lib.setWorkdir( wk );
		try {
			lib.setFile( main );
			while( lib.next() ){
				lib.loadtasks();
				if( demo ) { 
					lib.showtasks();
				} else {
					List<TaskAction> tasks = lib.getTasks();
					for( int i = 0; i < tasks.size(); i++ ){
						TaskAction t = tasks.get( i );
						System.out.println( "TASK: [" + t.getName() + "]" );
						try {
							if( execute( t ) ){
								System.out.println( "   Done " );
								cmdDone ++;
							} else {
								System.out.println( "   Skipped (when sentence) " );
								cmdSkipped ++;
							}
							System.out.println( "" );
						} catch ( Exception e ){
							System.out.println( "   Error " );
							cmdErrors ++;
							if( t.isIgnoreErrors() ){
								
							} else {
								// logger.error( "Output [" + t.getName() + "] on ", e );
								throw e;
							}
						}
					}
				}
			}
		} catch( StopAction e ) {
			logger.error( "TASK STOPPED: " + main, e );
		} catch( Exception e ) {
			logger.error( "generic error in " + main, e );
		}
		System.out.println( "" );
		System.out.println( "[ TASK done " + cmdDone + ", skipped " + cmdSkipped + ", errors " + cmdErrors + " ]");
		System.out.println( "" );
	}

	protected HashMap vars = new HashMap();
	
	protected boolean execute( TaskAction t ) throws Exception {
		ASelenevCmd cmd;
		ASelenevHostCmd hcmd;
		List<SshServerManager> hosts = (List<SshServerManager>) vars.get( "hosts" );
		String action = t.getAction();
		String when = t.getWhenClause();
		if( when != null ){
			if( when.length() != 0 ){
				try {
					String myExpression = evalVars( when );
		            if( !(boolean) se.eval( myExpression ) )
						return false;
		        } catch ( ScriptException e ){
		        	throw new StopAction( "When sentence scripting error", e );
		        }
			}
		}
		switch ( action ){
		case "provisionDigitalOcean":
			cmd = new ProvisionDigitalOcean();
			execution( t, cmd );
			break;
		case "hosts":
			cmd = new Hosts();
			execution( t, cmd );
			break;
		case "include_vars":
			include_vars( t );
			break;
		case "user":
			for( int i = 0; i < hosts.size(); i++ ){
				SshServerManager ssh = hosts.get( i );
				hcmd = new User();
				execution( t, hcmd, ssh );
			}
			break;
		default:
			throw new StopAction( "unknown CMD " + action );
		}
		return true;
	}

	private void include_vars( TaskAction t ) throws StopAction, IOException {
		HashMap l = t.getVars();
		vars.putAll( l );
	}

	private void execution( TaskAction t, ASelenevCmd executable ) throws Exception {
		try {
			Object map = t.getVars().get( "CMD" );
			String[] args;
			if( map == null ) {
				args = new String[0];
			} else {
				StringTokenizer st = new StringTokenizer( (String)map, " " );
				args = new String[ st.countTokens() ];
				int i = 0;
				while (st.hasMoreElements()) {
					String val = (String) st.nextElement();
					args[ i ] = val;
					i++;
				}
			}
			// if ARGS contains VARIABLES, resolve 'em BEFORE run TASK and passing ARGS
			args = evalVars( args );
			Object o = executable.exec( args, prv.getAbsolutePath(), wk.getAbsolutePath() );
			vars.put( t.getAction(), o );
		} catch ( Exception e ){
			throw new StopAction( "failure: " + e.getMessage(), e );
		}
	}

	private void execution( TaskAction t, ASelenevHostCmd executable, SshServerManager ssh ) throws Exception {
		try {
			Object map = t.getVars().get( "CMD" );
			String[] args;
			if( map == null ) {
				args = new String[0];
			} else {
				StringTokenizer st = new StringTokenizer( (String)map, " " );
				args = new String[ st.countTokens() ];
				int i = 0;
				while (st.hasMoreElements()) {
					String val = (String) st.nextElement();
					args[ i ] = val;
					i++;
				}
			}
			// if ARGS contains VARIABLES, resolve 'em BEFORE run TASK and passing ARGS
			args = evalVars( args );
			Object o = executable.exec( args, prv.getAbsolutePath(), wk.getAbsolutePath(), ssh );
			vars.put( t.getAction(), o );
		} catch ( Exception e ){
			throw new StopAction( "failure: " + e.getMessage(), e );
		}
	}
	/**
	// if ARGS contains VARIABLES, resolve 'em BEFORE run TASK and passing ARGS
	 * 
	 * @param args
	 * @return
	 */
	private String[] evalVars( String[] args ) throws StopAction {
		for( int i = 0; i < args.length; i++ ){
			args[i] = evalVars( args[i] );
		}
		return args;
	}

	private String evalVars( String arg ) throws StopAction {
		while( true ) {
			int pos = arg.indexOf( "{{" );
			if( pos != -1 ){
				int end = arg.indexOf( "}}", pos );
				if( end != -1 ){
					String key = arg.substring( pos +2, end );
					Object value = vars.get( key.trim() );
					if( value == null ) {
						throw new StopAction( "unknow VAR: " + key );
					}
					arg = arg.substring( 0, pos ) + value.toString() + arg.substring( end + 2 );
				} else {
					throw new StopAction( "broken VAR: starts with {{ but not end with }}" );
				}
			} else {
				break;
			}
		}
		return arg;
	}

	static public void updateYaml( File fileYaml, Object item ) throws IOException {
		YamlWriter writer = new YamlWriter( new FileWriter( fileYaml ) );
		writer.write( item );
		writer.close();
	}

}