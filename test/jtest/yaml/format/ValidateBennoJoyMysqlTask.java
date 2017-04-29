package jtest.yaml.format;

import static org.junit.Assert.*;
import org.junit.Test;
import org.naarani.selenev.yaml.YamlLib;

public class ValidateBennoJoyMysqlTask {

	@Test
	public void test() {
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
