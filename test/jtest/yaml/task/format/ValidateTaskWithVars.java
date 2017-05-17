package jtest.yaml.task.format;

import static org.junit.Assert.fail;

import org.junit.Test;
import org.naarani.ecantonchiro.yaml.YamlTaskLib;

public class ValidateTaskWithVars {

	@Test
	public void testTaskWithLongVarsList() {
		String file = "examples/basic/test/testTaskVar.yaml";
		YamlTaskLib lib = new YamlTaskLib();
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
