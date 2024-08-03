
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.modules.pdf.components;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class DocumentTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		testURLImage	= "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg";
	static String		tmpDirectory	= "src/test/resources/tmp/Document";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true, Path.of( "src/test/resources/boxlang.json" ).toString() );
		System.out.println( "Temp Directory Exists " + FileSystemUtil.exists( tmpDirectory ) );
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory, true, null );
		}
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the Component Document with CFML parsing" )
	@Test
	public void testComponentCF() {
		variables.put( Key.of( "testImage" ), testURLImage );
		// @formatter:off
		instance.executeSource(
		    """
		    <cfdocument format="pdf" variable="result">
				<cfdocumentitem type="header">
					<h1>Header</h1>
				</cfdocumentitem>
				<cfdocumentitem type="footer">
					<h1>Footer</h1>
				</cfdocumentitem>
		    	<cfdocumentsection name="Section 1">
		    		<h1>Section 1</h1>
		    	</cfdocumentsection>
		    	<cfdocumentsection name="Section 2">
		    		<h1>Section 2</h1>
		    	</cfdocumentsection>
				<cfdocumentsection src="#testImage#">
		    </cfdocument>
		      """,
		    context, BoxSourceType.CFTEMPLATE );
		// @formatter:on
		assertTrue( variables.get( result ) instanceof byte[] );
	}

	@DisplayName( "It tests the Component Document with BoxLang parsing" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:document format="pdf" variable="result">
		    	<bx:documentsection name="Section 1">
		    		<h1>Section 1</h1>
		    	</bx:documentsection>
		    	<bx:documentsection name="Section 2">
		    		<h1>Section 2</h1>
		    	</bx:documentsection>
		    </bx:document>
		      """,
		    context, BoxSourceType.BOXTEMPLATE );

		assertTrue( variables.get( result ) instanceof byte[] );
	}

	@DisplayName( "It tests the Component Document with BoxLang script parsing" )
	@Test
	public void testComponentScript() {
		// @formatter:off
		instance.executeSource(
		    """
		    document format="pdf" variable="result"{
		    	documentsection name="Section 1"{
		    		writeOutput("<h1>Section 1</h1>")
				}
		    	documentsection name="Section 2"{
		    		writeOutput("<h1>Section 2</h1>")
				}
			}
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		// @formatter:on
		assertTrue( variables.get( result ) instanceof byte[] );
	}

}
