
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.modules.pdf.types.PDF;
import ortus.boxlang.modules.pdf.util.ModuleKeys;
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
	static String		testFile		= tmpDirectory + "/test.pdf";
	static String		testBinaryFile	= tmpDirectory + "/test.jpg";

	@BeforeAll
	public static void setUp() throws MalformedURLException, IOException {
		instance = BoxRuntime.getInstance( true, Path.of( "src/test/resources/boxlang.json" ).toString() );
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
		if ( !FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.createDirectory( tmpDirectory, true, null );
		}
		if ( !FileSystemUtil.exists( testBinaryFile ) ) {
			BufferedInputStream urlStream = new BufferedInputStream( URI.create( testURLImage ).toURL().openStream() );
			FileSystemUtil.write( testBinaryFile, urlStream.readAllBytes(), true );
		}
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			// FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}
	}

	@BeforeEach
	public void setupEach() {
		if ( FileSystemUtil.exists( testFile ) ) {
			FileSystemUtil.deleteFile( testFile );
		}
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
		    <cfdocument format="pdf" variable="result" isTestMode=true >
				<cfdocumentitem type="header">
					<h1>Header</h1>
				</cfdocumentitem>
				<cfdocumentitem type="footer">
					<h1>Footer</h1>
					<cfoutput><p>Page #cfdocument.currentpagenumber# of #cfdocument.totalpages#</p></cfoutput>
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

		PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );

		// Combined h1 tags for header footer and body
		assertEquals( 8, pdfObject.getRenderer().getDocument().getElementsByTagName( "h1" ).getLength() );
		// document section image
		assertEquals( 1, pdfObject.getRenderer().getDocument().getElementsByTagName( "img" ).getLength() );
		// page placeholders
		assertEquals( 6, pdfObject.getRenderer().getDocument().getElementsByTagName( "span" ).getLength() );
	}

	@DisplayName( "It tests the Component Document with BoxLang script parsing" )
	@Test
	public void testComponentScript() {
		assertFalse( variables.containsKey( ModuleKeys.bxPDF ) );
		// @formatter:off
		instance.executeSource(
		    """
		    bx:document format="pdf" variable="result" isTestMode=true{
		    	bx:documentsection name="Section 1"{
		    		writeOutput("<h1>Section 1</h1>")
				}
		    	bx:documentsection name="Section 2"{
		    		writeOutput("<h1>Section 2</h1>")
				}
			}
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		// @formatter:on
		assertTrue( variables.get( result ) instanceof byte[] );

		PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );

		// Combined h1 tags for body sections
		assertEquals( 2, pdfObject.getRenderer().getDocument().getElementsByTagName( "h1" ).getLength() );
	}

	@DisplayName( "It tests the Component Document with BoxLang parsing" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:document format="pdf" variable="result" isTestMode=true>
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

		PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );

		// Combined h1 tags for body sections
		assertEquals( 2, pdfObject.getRenderer().getDocument().getElementsByTagName( "h1" ).getLength() );
	}

	@DisplayName( "It tests the ability to write to a file" )
	@Test
	public void testFileWrite() {
		String testFile = tmpDirectory + "/test.pdf";
		variables.put( Key.of( "testImage" ), testURLImage );
		variables.put( Key.of( "outputFile" ), testFile );
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:document format="pdf" filename="#outputFile#" isTestMode=true>
				<bx:documentitem type="header">
					<h1>Header</h1>
				</bx:documentitem>
				<bx:documentitem type="footer">
					<h1>Footer</h1>
					<bx:output><p>Page #bxdocument.currentpagenumber# of #bxdocument.totalpages#</p></bx:output>
				</bx:documentitem>
		    	<bx:documentsection name="Section 1">
		    		<h1>Section 1</h1>
		    	</bx:documentsection>
		    	<bx:documentsection name="Section 2">
		    		<h1>Section 2</h1>
		    	</bx:documentsection>
				<bx:documentsection src="#testImage#">
		    </bx:document>
		      """,
		    context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertTrue( FileSystemUtil.exists( testFile ) );

		PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );

		// Combined h1 tags for header footer and body
		assertEquals( 8, pdfObject.getRenderer().getDocument().getElementsByTagName( "h1" ).getLength() );
		// document section image
		assertEquals( 1, pdfObject.getRenderer().getDocument().getElementsByTagName( "img" ).getLength() );
		// page placeholders
		assertEquals( 6, pdfObject.getRenderer().getDocument().getElementsByTagName( "span" ).getLength() );
	}

	@DisplayName( "It tests local and remote URL resolution for images in the document" )
	@Test
	public void testURLResolution() {
		variables.put( Key.of( "testImage" ), testURLImage );
		variables.put( Key.of( "testLocalImage" ), testURLImage );
		variables.put( Key.of( "outputFile" ), testFile );
		// @formatter:off
		instance.executeSource(
		    """
		    <bx:document format="pdf" filename="#outputFile#" isTestMode=true>
		    	<bx:output><img src="#testImage#"/></bx:output>
		    	<bx:output><img src="#testLocalImage#"/></bx:output>
		    </bx:document>
		      """,
		    context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertTrue( FileSystemUtil.exists( testFile ) );
	}

	@DisplayName( "It tests generating a PDF from a remote URL" )
	@Test
	public void testRemoteURLGeneration() {
		variables.put( Key.of( "outputFile" ), testFile );
		// @formatter:off
		instance.executeSource(
		    """
		    bx:document format="pdf" src="https://www.raymondcamden.com" filename="#outputFile#" overwrite=true isTestMode=true;
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		// @formatter:on
		assertTrue( FileSystemUtil.exists( testFile ) );

		// Un-comment to Debug generated HTML
		// PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );
		// try {
		// FileSystemUtil.write( "src/test/resources/tmp/Document/test.html", PDFUtil.documentToString( pdfObject.getRenderer().getDocument() ) );
		// // System.out.println( "document content: " + PDFUtil.documentToString( parsedDocument ) );
		// } catch ( TransformerException e ) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	@DisplayName( "Can create a document section without HTML content" )
	@Test
	public void testEmptyDocumentSection() {
		variables.put( Key.of( "outputFile" ), testFile );
		// @formatter:off
		instance.executeSource(
		    """
				bx:document format="pdf" filename="#outputFile#" overwrite=true bookmark=false isTestMode=true{
					bx:documentsection name="Section 1" {
						writeoutput('ra');
					}
				}
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		// @formatter:on
		assertTrue( FileSystemUtil.exists( testFile ) );

		PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );
		assertEquals( 0, pdfObject.getRenderer().getDocument().getElementsByTagName( "h1" ).getLength() );
	}

	@DisplayName( "Can create a document section with a remote image in the document section" )
	@Test
	public void testDocumentSectionWithRemoteImage() {
		variables.put( Key.of( "outputFile" ), testFile );
		// @formatter:off
		instance.executeSource(
		    """
				<bx:set testImage = "https://ortus-public.s3.amazonaws.com/logos/ortus-medium.jpg"/>
				<bx:document format="pdf" filename="#outputFile#" overwrite=true isTestMode=true>
					<!--- Header for all sections --->
					<bx:documentitem type="header">
						<h1>This is my Header</h1>
					</bx:documentitem>
					<!--- Footer for all sections --->
					<bx:documentitem type="footer">
						<h1>This is My Footer</h1>
						<bx:output><p>Page #bxdocument.currentpagenumber# of #bxdocument.totalpages#</p></bx:output>
					</bx:documentitem>
					<!--- Document section, which will be bookmarked as "Section 1" --->
					<bx:documentsection name="Section 1">
						<h1>Section 1</h1>
					</bx:documentsection>
					<!--- Document section, which will be bookmarked as "Section 2" --->
					<bx:documentsection name="Section 2">
						<h1>Section 2</h1>
					</bx:documentsection>
					<!--- Document section, which contains an image --->
					<bx:documentsection src="#testImage#">
				</bx:document>
		      """,
		    context, BoxSourceType.BOXTEMPLATE );
		// @formatter:on
		assertTrue( FileSystemUtil.exists( testFile ) );

		PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );
		assertEquals( 8, pdfObject.getRenderer().getDocument().getElementsByTagName( "h1" ).getLength() );
	}

	@DisplayName( "Will correctly create bookmarks for document sections" )
	@Test
	public void testBookmarkSections() {
		variables.put( Key.of( "outputFile" ), testFile );
		// @formatter:off
		instance.executeSource(
		    """
				bx:document format="pdf" filename="#outputFile#" overwrite=true bookmark=true isTestMode=true{
					bx:documentsection name="Bookmark 1" {
						writeoutput('ra');
					}

					bx:documentsection name="Bookmark 2" {
						writeoutput('<h1>Not a Bookmark</h1>');
					}
				}
		      """,
		    context, BoxSourceType.BOXSCRIPT );
		// @formatter:on
		assertTrue( FileSystemUtil.exists( testFile ) );

		PDF pdfObject = ( PDF ) variables.get( ModuleKeys.bxPDF );
		assertEquals( 1, pdfObject.getRenderer().getDocument().getElementsByTagName( "h1" ).getLength() );
		// NodeList headElements = pdfObject.getRenderer().getDocument().getElementsByTagName( "head" );
		// assertEquals( "bookmarks", headElements.item( 0 ).getChildNodes().item( 0 ).getNodeName() );
		assertEquals( 2, pdfObject.getRenderer().getDocument().getElementsByTagName( "bookmark" ).getLength() );
	}

}
