package org.naarani.ecantonchiro.yaml;

import java.io.BufferedReader; 
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class YamlTaskLib {
	
	protected YamlReader reader;
	protected ArrayList<Map<String,String>> lastItem;
	// name
	// action - substitute | service, yum, apt, shell, copy, files (?) | TEMPLATE
		// copy: src=/etc/ansible/hosts dest=/etc/ansible/hosts owner=root group=root mode=0644
	// environment
	// with_item
	
	// ignore_errorss
	// register
	// when
	
	public YamlTaskLib(){

	}

	public void setFile( String file ) throws IOException {
		if( file.toLowerCase().startsWith( "http" ) ) {
			URL url = new URL( file );
			BufferedReader in = new BufferedReader(new InputStreamReader( url.openStream() ) );
			reader = new YamlReader( in );
			// verify when in shoudl be closed...
		} else {
			reader = new YamlReader( new FileReader( file ) );
		}
	}

	@SuppressWarnings("unchecked")
	public boolean next() throws YamlException {
		Object object = reader.read();
		if( object == null )
			return false;
	    lastItem = (ArrayList<Map<String,String>>)object;
	    return true;
	}

	protected ArrayList<TaskAction> tasks;
	
	public void loadtasks() throws IOException {
		tasks = new ArrayList<TaskAction>();
		for( int i = 0; i < lastItem.size(); i++ ){
			Map<String,?> map = (Map<String,?>)lastItem.get( i );
			TaskAction t = new TaskAction();
			t.setWorkdir( wk );
			String nam = (String)map.get( "name" );
			nam = nam == null ? "..." : nam;
			t.setName( nam );
			Object ref = map.get( "ignore_errors" );
			if( ref == null )
				ref = "false";
			t.setIgnoreErrors( Boolean.parseBoolean( ref.toString() ) );
			if( map.get( "when" ) != null )
				t.setWhenClause( map.get( "when" ).toString() );
			if( map.get( "remote_user" ) != null )
				t.setRemoteUser( map.get( "remote_user" ).toString() );
			if( map.get( "remote_password" ) != null )
				t.setRemotePwd( map.get( "remote_password" ).toString() );
			// ACTION execution shuld based on WHEN clause
			t.findAction( map );
			tasks.add( t );
		}
	}
	
	public void showtasks() throws IOException {
		for( int i = 0; i < tasks.size(); i++ ){
			TaskAction  t = tasks.get( i );
			System.out.println( "NAME [" + t.getAction() + "] : " + t.getName() );
		}
	}

	protected File wk = new File( "." );
	
	public void setWorkdir( File wk ){
		this.wk = wk;
	}

	public List<TaskAction> getTasks(){
		return tasks;
	}

}