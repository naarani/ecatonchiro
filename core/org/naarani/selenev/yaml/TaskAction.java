package org.naarani.selenev.yaml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TaskAction {

	HashMap includedVars = new HashMap();
	
	String name;
	boolean ignoreErrors;
	String action;
	
	String whenClause;
	String include_vars;
	IncludeVars vars;
	
	String remoteUser, remotePwd;
	
	boolean sudo;
//	String environment;
//	String register;

	/*
	 * REGISTER EXAMPLE
	 * 
- hosts: web_servers

  tasks:

     - shell: /usr/bin/foo
       register: foo_result
       ignore_errors: True

     - shell: /usr/bin/bar
       when: foo_result.rc == 5
       	 */

	protected File workdir = new File( "." );
	
	public TaskAction(){
		
	}
	
	public File getWorkdir(){
		return workdir;
	}
	
	public void setWorkdir( File dir ){
		workdir = dir;
	}

	public String getName(){
		return name;
	}

	public void setName(String name){
		this.name = name;
	}

	public String getRemoteUser(){
		return remoteUser;
	}

	public void setRemoteUser(String remoteUser){
		this.remoteUser = remoteUser;
	}

	public String getRemotePwd(){
		return remotePwd;
	}

	public void setRemotePwd(String remotePwd){
		this.remotePwd = remotePwd;
	}

	public boolean isIgnoreErrors(){
		return ignoreErrors;
	}

	public void setIgnoreErrors(boolean ignoreErrors){
		this.ignoreErrors = ignoreErrors;
	}

	public boolean isSudo(){
		return sudo;
	}

	public void setSudo(boolean sudo){
		this.sudo = sudo;
	}

	public String getAction(){
		return action;
	}

	public void setAction(String action){
		this.action = action;
	}

	public String getWhenClause(){
		return whenClause;
	}

	public void setWhenClause(String whenClause){
		this.whenClause = whenClause;
	}

	public void findAction( Map<String, ?> map ) throws IOException {
		HashMap<String, String> listmap = new HashMap<String, String>();
		//
		Set<String> set = map.keySet();
		Iterator<String> it = set.iterator();
		while( it.hasNext() ){
			String v = it.next();
			if( v.compareToIgnoreCase( "name" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "when" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "ignore_errors" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "register" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "with_items" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "delegate_to" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "environment" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "notify" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "remote_password" ) == 0 )
				continue;
			if( v.compareToIgnoreCase( "remote_user" ) == 0 )
				continue;
//			if( v.compareToIgnoreCase( "include_vars" ) == 0 )
//				continue;
			listmap.put( v, v );
		}
		if( listmap.size() == 1 ){
			setAction( listmap.keySet().iterator().next() );
			if( listmap.get( "include_vars" ) != null ){
				listmap.remove( "include_vars" );
				vars = IncludeVars.setup( this, listmap, map );
			} else {
				Object o = map.get( getAction() );
				includedVars.put( "CMD", o );
			}
		} else if( listmap.size() == 0 ){
			throw new IOException( "wrong action format [MISSING ACTION...] for name : " + name );
		} else {
			if( listmap.get( "include_vars" ) != null ){
				setAction( "include_vars" );
				listmap.remove( "include_vars" );
				vars = IncludeVars.setup( this, listmap, map );
			} else {
				if( listmap.get( "sudo" ) != null ) {
					listmap.remove( "sudo" );
					if( Boolean.parseBoolean( ( (String)map.get( "sudo" ) ).toLowerCase().replace( "yes", "true" ) ) ) {
						setSudo( true );
					}
					setAction( listmap.keySet().iterator().next() );
					Object o = map.get( getAction() );
					includedVars.put( "CMD", o );
				} else {
					Iterator<String> s = listmap.keySet().iterator();
					throw new IOException( "wrong action format [" + s.next() + ":" + s.next() + "] for name : " + name );
				}
			}
		}
	}

	public HashMap getVars() throws IOException {
		if( vars != null ){
			includedVars.putAll( vars.getVars() );
			vars = null;
		}
		return includedVars;
	}
	
}