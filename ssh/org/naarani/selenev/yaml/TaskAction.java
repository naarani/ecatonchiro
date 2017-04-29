package org.naarani.selenev.yaml;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TaskAction {

	String name;
	boolean ignoreErrors;
	String action;
	
	String whenClause;
//	String environment;
//	String register;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isIgnoreErrors() {
		return ignoreErrors;
	}

	public void setIgnoreErrors(boolean ignoreErrors) {
		this.ignoreErrors = ignoreErrors;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getWhenClause() {
		return whenClause;
	}

	public void setWhenClause(String whenClause) {
		this.whenClause = whenClause;
	}

	public void findAction( Map<String, String> map ) throws IOException {
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
//			if( v.compareToIgnoreCase( "include_vars" ) == 0 )
//				continue;
			if( action == null )
				setAction( v );
			else
				throw new IOException( "wrong action format [" + action + ":" + v + "] for name : " + name );
		}
	}
	
}