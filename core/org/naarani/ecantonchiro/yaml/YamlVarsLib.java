package org.naarani.ecantonchiro.yaml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

public class YamlVarsLib {

	protected YamlReader reader;
	protected Map<String,String> lastItem;
	
	public YamlVarsLib(){}

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
	public Map getVars() throws YamlException {
		Object object = reader.read();
		if( object == null )
			return null;
	    lastItem = (Map<String,String>)object;
	    return lastItem;
	}

}