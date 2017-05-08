package org.naarani.selenev.yaml;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncludeVars {

	static public boolean _DEBUG = true;
	
	public static IncludeVars setup( TaskAction taskAction, HashMap<String, String> list, Map<?, ?> map ) throws IOException {
		if( _DEBUG )
			System.out.println( "TESTING VAR OPTIONS AT TASK: " + taskAction.getName() );
		Object o = map.get( "include_vars" );
		if( o == null ){
			int a = 0;
			a++;
		} else if( o instanceof Map ){
			Map<String, ?> subMap = (Map<String, ?>) map.get( "include_vars" );
			String main = (String) subMap.get( "file" );
			if( main != null ) {
				File file = new File( taskAction.getWorkdir(), main );
				IncludeVars vars = new IncludeVars();
				vars.addFile( file );
				subMap.remove( "file" );
				while( subMap.size() > 0 ){
					if( subMap.get( "name" ) != null ){
						vars.setContainer( subMap.get( "name" ).toString() );
						subMap.remove( "name" );
						continue;
					}
					System.out.println( "too much VAR settings..." );
					break;
				}
				if( list.size() > 0 ) {
					int aa = 0;
					aa++;
				}
				if( _DEBUG )
					vars.printFiles();
				return vars;
			} else {
				main = (String)subMap.get( "dir" );
				if( main != null ){
					File dir = new File( taskAction.getWorkdir(), main );
					if( !dir.exists() ) {
						System.out.println( "missign file " + dir.getAbsolutePath() );
						return null;
					}
					subMap.remove( "dir" );
					String depth = null;
					List<String> extension = null;
					List<String> exclude = null;
					List<String> match = null;
					String container = null;
					while( subMap.size() > 0 ){
						if( subMap.get( "extensions" ) != null ){
							extension = oneOrMoreToList( subMap, "extensions" );
							subMap.remove( "extensions" );
							continue;
						} else if( subMap.get( "ignore_files" ) != null ){
							exclude = oneOrMoreToList( subMap, "ignore_files" );
							subMap.remove( "ignore_files" );
							continue;
						} else if( subMap.get( "depth" ) != null ){
							depth = subMap.get( "depth" ).toString();
							subMap.remove( "depth" );
							continue;
						} else if( subMap.get( "name" ) != null ){
							container = subMap.get( "name" ).toString();
							subMap.remove( "name" );
							continue;
						} else if( subMap.get( "files_matching" ) != null ){
							match = oneOrMoreToList( subMap, "files_matching" );
							subMap.remove( "files_matching" );
							continue;
						}
						System.out.println( "too much VAR settings..." );
						break;
					}
					IncludeVars vars = new IncludeVars();
					vars.setContainer( container );
					int recursive = Integer.MAX_VALUE;
					if( depth != null ){
						recursive = Integer.parseInt( depth );
					}
					ArrayList<File> fileList = new ArrayList<File>(); 
					parseDirectory( dir, match, exclude, extension, recursive, 0, fileList );
					for( int i = 0; i < fileList.size(); i++ ){
						vars.addFile( fileList.get( i ) );
					}
					if( list.size() > 0 ) {
						int aa = 0;
						aa++;
					}
					if( _DEBUG )
						vars.printFiles();
					return vars;
				} else {
					int aa = 0;
					aa++;
				}
			}
			int a = 0;
			a++;
			if( list.size() > 0 ) {
				int aa = 0;
				aa++;
			}
		} else {
			String value = o.toString();
			File file = new File( taskAction.getWorkdir(), "group_vars/" + value );
			IncludeVars vars = new IncludeVars();
			if( list.size() > 0 ) {
				int aa = 0;
				aa++;
			}
			vars.addFile( file );
			if( _DEBUG )
				vars.printFiles();
			return vars;
		}
		return null;
	}

	private static List<String> oneOrMoreToList( Map<String, ?> subMap, String tag ){
		List<String> list = null;
		Object o = subMap.get( tag );
		if( o instanceof Map ) {
			list = (List<String>)o;
		} else if( o instanceof List ) {
			list = (List)o;
		} else {
			list = new ArrayList<String>();
			list.add( o.toString() );
		}
		return list;
	}

	// final List<String> first, 
	private static void parseDirectory( File dir, final List<String> match, final List<String> exclude, final List<String> extension
			, int recursive, int start, ArrayList<File> fileList ){
		File[] t1List = dir.listFiles( new FileFilter(){

			@Override
			public boolean accept( File dir ){
				String name = dir.getName();
				if( dir.isDirectory() ){
					return false;
				}
		/*
				} else if( first != null ){
					for( int i = 0; i < first.size(); i++ ){
						if( first.get( i ).compareTo( name ) == 0 ){
							return true;
						}
					}
			*/
				if( match != null ){
					for( int i = 0; i < match.size(); i++ ){
						if( name.compareTo( match.get( i ) ) == 0 )
							return true;
					}
					return false;
				}
				if( exclude != null ){
					for( int i = 0; i < exclude.size(); i++ ){
						if( name.compareTo( exclude.get( i ) ) == 0 )
							return false;
					}
				}
				if( extension != null ){
					for( int i = 0; i < extension.size(); i++ ){
						if( name.endsWith( "." + extension.get( i ) ) )
							return true;
					}
					return false;
				}
				return true;
			}
			
		});
		if( t1List == null ) {
			return;
		}
		for( int i = 0; i < t1List.length; i++ ){
			fileList.add( t1List[i] );
		}
		if( recursive < start ){
			File[] t1Directory = dir.listFiles( new FilenameFilter(){
				
				@Override
				public boolean accept( File dir, String name ){
					if( dir.isDirectory() )
						return true;
					else
						return false;
				}
				
			});
			for( int i = 0; i < t1Directory.length; i++ ){
				parseDirectory( t1Directory[i], match, exclude, extension, recursive, start + 1, fileList );
			}
		}
	}

	protected boolean evaluated = false;
	protected String container;
	protected List<File> files = new ArrayList<File>();
	
	public void addFile( File file ) throws IOException{
		if( !file.exists() ) {
//			System.out.println( "missing file " + file.getAbsolutePath() );
			throw new IOException( "file missing.. : " + file.getAbsoluteFile() );
		}
		files.add( file );
	}
	
	public HashMap getVars() throws IOException {
		HashMap vars = new HashMap();
		for( int i = 0; i < files.size(); i++ ){
			YamlVarsLib yvl = new YamlVarsLib();
			yvl.setFile( files.get( i ).getAbsolutePath() );
			vars.putAll( yvl.getVars() );
		}
		return vars;
	}

	public void setContainer( String container ) {
		this.container = container;
	}

	private void printFiles(){
		for( int i = 0; i < files.size(); i++ ){
			System.out.println( "FILE: " + files.get( i ).getAbsolutePath() );
		}
		System.out.println( "" );
	}

}