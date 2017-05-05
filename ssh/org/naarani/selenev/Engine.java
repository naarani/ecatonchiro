package org.naarani.selenev;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.naarani.core.exceptions.StopAction;
import org.naarani.selenev.jclouds.digitalocean.ProvisionDigitalOcean;
import org.naarani.selenev.yaml.TaskAction;
import org.naarani.selenev.yaml.YamlTaskLib;

public class Engine {

	static protected Logger logger = Logger.getLogger( Engine.class );

	protected String main;
	protected File wk;
	protected File prv;
	protected boolean demo;

	public boolean isDemo() {
		return demo;
	}

	public void setDemo(boolean demo) {
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

	public void run(){
		System.out.println( "running " + main + " workspace " + wk );
		YamlTaskLib lib = new YamlTaskLib();
		lib.setWorkdir( wk );
		try {
			lib.setFile( main );
			while( lib.next() ){
				vars = new HashMap();
				lib.loadtasks();
				if( demo ) { 
					lib.showtasks();
				} else {
					List<TaskAction> tasks = lib.getTasks();
					for( int i = 0; i < tasks.size(); i++ ){
						TaskAction t = tasks.get( i );
						System.out.println( "TASK: [" + t.getName() + "]" );
						try {
							execute( t );
							logger.debug( "   Done " );
						} catch ( Exception e ){
							logger.error( "Output [" + t.getName() + "] on ", e );
							if( t.isIgnoreErrors() ){
								
							} else {
								throw e;
							}
						}
					}
				}
			}
		} catch( Exception e ) {
			logger.error( "generic error in " + main, e );
		}
	}

	protected HashMap vars = new HashMap();
	
	protected void execute( TaskAction t ) throws Exception {
		String action = t.getAction();
		switch ( action ){
		case "provisionDigitalOcean":
			provisionDigitalOcean( t );
			break;

		case "include_vars":
			include_vars( t );
			break;

		default:
			break;
		}
	}

	private void include_vars( TaskAction t ) throws StopAction, IOException {
		HashMap l = t.getVars();
		vars.putAll( l );
	}

	private void provisionDigitalOcean( TaskAction t ) throws Exception {
		ProvisionDigitalOcean cd = new ProvisionDigitalOcean();
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
			Object o = cd.exec( args, prv.getAbsolutePath(), wk.getAbsolutePath() );
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
	private String[] evalVars(String[] args) {
		// TODO Auto-generated method stub
		return args;
	}

}