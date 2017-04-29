package jtest.yaml.format;

import static org.junit.Assert.*;
import org.junit.Test;
import org.naarani.selenev.yaml.YamlLib;

public class ValidateBennoJoyTask {

	@Test
	public void testNginxBennoJoy() {
		String file = "https://github.com/bennojoy/nginx/blob/master/tasks/main.yml";
		YamlLib lib = new YamlLib();
		try {
			lib.setFile( file );
			while( lib.next() ){
				lib.loadtasks();
				lib.showtasks();
			}
		} catch( Exception e ) {
			e.printStackTrace();
			fail( "generic problem" + e.getMessage() );
		}
	}

	@Test
	public void testTaskMysqlBennoJoy() {
		String file = "https://raw.githubusercontent.com/bennojoy/mysql/master/tasks/main.yml";
		YamlLib lib = new YamlLib();
		try {
			lib.setFile( file );
			while( lib.next() ){
				lib.loadtasks();
				lib.showtasks();
			}
		} catch( Exception e ) {
			e.printStackTrace();
			fail( "generic problem" + e.getMessage() );
		}
	}
}
