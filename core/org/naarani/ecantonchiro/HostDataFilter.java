package org.naarani.ecantonchiro;

import java.io.File;
import java.io.FileFilter;

public class HostDataFilter implements FileFilter {
	
	@Override
	public boolean accept( File arg0 ){
		return arg0.isFile() && ( arg0.getName().endsWith( "yaml") || arg0.getName().endsWith( "yml") );
	} 

}