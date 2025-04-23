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
package ortus.boxlang.modules.pdf.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.parser.Parser;
import org.w3c.dom.Document;

import ortus.boxlang.modules.pdf.types.PDF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class PDFUtil {

	/**
	 * Page size type maps
	 */
	public static final HashMap<Key, String>		PAGE_TYPES					= new HashMap<Key, String>() {

																					{
																						put( ModuleKeys.ISOB5, "B5" );
																						put( ModuleKeys.ISOB4, "B4" );
																						put( ModuleKeys.ISOB3, "B3" );
																						put( ModuleKeys.ISOB2, "B2" );
																						put( ModuleKeys.ISOB1, "B1" );
																						put( ModuleKeys.ISOB0, "B0" );
																						put( ModuleKeys.JISB5, "JIS-B5" );
																						put( ModuleKeys.JISB4, "JIS-B4" );
																						put( ModuleKeys.HALFLETTER, "A6" );
																						put( ModuleKeys.LETTER, "letter" );
																						put( ModuleKeys.TABLOID, "tabloid" );
																						put( ModuleKeys.LEDGER, "ledger" );
																						put( ModuleKeys.LEGAL, "legal" );

																						put( ModuleKeys.A10, "A10" );
																						put( ModuleKeys.A9, "A9" );
																						put( ModuleKeys.A8, "A8" );
																						put( ModuleKeys.A7, "A7" );
																						put( ModuleKeys.A6, "A6" );
																						put( ModuleKeys.A5, "A5" );
																						put( ModuleKeys.A4, "A4" );
																						put( ModuleKeys.A3, "A3" );
																						put( ModuleKeys.A2, "A2" );
																						put( ModuleKeys.A1, "A1" );
																						put( ModuleKeys.A0, "A0" );
																					}
																				};
	/**
	 * Page dimension map, in millimeters ( width, height ) for page sizes which are not supported via CSS
	 */
	public static final HashMap<String, double[]>	PAGE_DIMENSIONS				= new HashMap<String, double[]>() {

																					{
																						put( "A0", new double[] { 841, 1189 } );
																						put( "A1", new double[] { 594, 841 } );
																						put( "A2", new double[] { 420, 594 } );
																						put( "A6", new double[] { 105, 148 } );
																						put( "A7", new double[] { 74, 105 } );
																						put( "A8", new double[] { 52, 74 } );
																						put( "A9", new double[] { 37, 52 } );
																						put( "A10", new double[] { 26, 37 } );
																						put( "A10", new double[] { 26, 37 } );
																						put( "B0", new double[] { 1000, 1414 } );
																						put( "B1", new double[] { 707, 1000 } );
																						put( "B2", new double[] { 500, 707 } );
																						put( "B3", new double[] { 353, 500 } );
																						put( "tabloid", new double[] { 431.8, 279.4 } );
																					}
																				};

	/**
	 * Local variable struct used for placeholders
	 **/
	public static final String						DOCUMENT_LOCAL_VARIABLE		= "bxdocument";
	/**
	 * Local variable struct used for placeholders
	 */
	public static final IStruct						DOCUMENT_LOCAL_PLACEHOLDERS	= Struct.of(
	    ModuleKeys.currentpagenumber, "<span class='currentpagenumber'/>",
	    ModuleKeys.totalpages, "<span class='totalpages'/>",
	    ModuleKeys.currentsectionpagenumber, "<span class='currentsectionpagenumber'/>",
	    ModuleKeys.totalsectionpagecount, "<span class='totalsectionpagecount'/>"
	);

	/**
	 * Generate a PDF from a byte array
	 *
	 * @param contents
	 * @param context
	 * @param attributes
	 * @param executionState
	 *
	 * @return
	 */
	public static PDF generatePDF( byte[] contents, IBoxContext context, IStruct attributes, IStruct executionState ) {
		PDF pdf = new PDF( attributes, executionState );

		pdf.addDocumentItem(
		    contents,
		    null,
		    null,
		    attributes,
		    executionState
		);

		return pdf.generate();
	}

	/**
	 * Generate a PDF from a string buffer
	 *
	 * @param buffer
	 * @param context
	 * @param attributes
	 * @param executionState
	 *
	 * @return
	 */
	public static PDF generatePDF( StringBuffer buffer, IBoxContext context, IStruct attributes, IStruct executionState ) {

		PDF pdf = new PDF( attributes, executionState );

		if ( buffer.toString().trim().length() > 0 ) {
			pdf.addDocumentItem( buffer.toString() );
		}

		executionState.getAsArray( ModuleKeys.documentSections )
		    .stream()
		    .map( section -> StructCaster.cast( section ) )
		    .forEach( section -> {
			    IStruct sectionState	= section.getAsStruct( Key.executionState );
			    IStruct sectionAttributes = section.getAsStruct( Key.attributes );
			    IStruct sectionHeader	= extractHeaderFromState( sectionState );
			    IStruct sectionFooter	= extractFooterFromState( sectionState );

			    Object sourceContent	= sectionAttributes.getAsString( Key.result );

			    if ( sectionAttributes.containsKey( ModuleKeys.src ) ) {
				    sectionAttributes.put( ModuleKeys.srcfile, sectionAttributes.getAsString( ModuleKeys.src ) );
			    }

			    if ( sectionAttributes.containsKey( ModuleKeys.srcfile ) ) {
				    String srcFile = sectionAttributes.getAsString( ModuleKeys.srcfile );
				    if ( !srcFile.substring( 0, 4 ).equalsIgnoreCase( "http" ) ) {
					    srcFile = FileSystemUtil.expandPath( context, srcFile ).absolutePath().toString();
				    }
				    sourceContent = FileSystemUtil.read( srcFile );
				    if ( !sectionAttributes.containsKey( ModuleKeys.mimeType ) ) {
					    sectionAttributes.put( ModuleKeys.mimeType, FileSystemUtil.getMimeType( srcFile ) );
				    }
			    }

			    boolean isBinarySource = sourceContent instanceof byte[];

			    if ( isBinarySource ) {
				    pdf.addDocumentItem(
				        ( byte[] ) sourceContent,
				        sectionHeader != null ? sectionHeader.getAsStruct( Key.attributes ).getAsString( Key.result ) : null,
				        sectionFooter != null ? sectionFooter.getAsStruct( Key.attributes ).getAsString( Key.result ) : null,
				        sectionAttributes,
				        sectionState
				    );
			    } else {
				    pdf.addDocumentItem(
				        StringCaster.cast( sourceContent ),
				        sectionHeader != null ? sectionHeader.getAsStruct( Key.attributes ).getAsString( Key.result ) : null,
				        sectionFooter != null ? sectionFooter.getAsStruct( Key.attributes ).getAsString( Key.result ) : null,
				        sectionAttributes,
				        sectionState
				    );
			    }

		    } );

		return pdf.generate();
	}

	/**
	 * Parses and santizes an html 5 string in to a DOM document
	 *
	 * @param content
	 *
	 * @return
	 */
	public synchronized static org.w3c.dom.Document parseContent( String content ) {
		org.jsoup.nodes.Document doc;
		try {
			doc = Jsoup.parse( content, "", Parser.xmlParser() );
		} catch ( Exception e ) {
			// If parsing fails, try to parse as HTML5
			doc = Jsoup.parse( content, "", Parser.htmlParser() );
		}

		// Should reuse W3CDom instance if converting multiple documents.
		return new W3CDom().fromJsoup( doc );
	}

	public synchronized static org.w3c.dom.Document parseRemoteFile( String url ) {
		try {
			return new W3CDom().fromJsoup( Jsoup.connect( url ).get() );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Extract the header from a component execution state
	 *
	 * @param executionState
	 *
	 * @return The found header struct or else null
	 */
	public static IStruct extractHeaderFromState( IStruct executionState ) {
		return executionState.getAsArray( ModuleKeys.documentItems )
		    .stream()
		    .map( StructCaster::cast )
		    .filter( item -> item.getAsString( Key.type ).toLowerCase() == "header" )
		    .findFirst()
		    .orElse( null );
	}

	/**
	 * Extract the footer from a component execution state
	 *
	 * @param executionState
	 *
	 * @return The found footer struct or else null
	 */
	public static IStruct extractFooterFromState( IStruct executionState ) {
		return executionState.getAsArray( ModuleKeys.documentItems )
		    .stream()
		    .map( StructCaster::cast )
		    .filter( item -> item.getAsString( Key.type ).toLowerCase() == "footer" )
		    .findFirst()
		    .orElse( null );
	}

	/**
	 * Utility methods to view the generated HTML to be parsed
	 *
	 * @param document
	 *
	 * @return
	 *
	 * @throws TransformerException
	 */
	public static String documentToString( Document document ) throws TransformerException {
		Transformer transformer = getTransformer();
		transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
		transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
		transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "4" );

		StringWriter stringWriter = new StringWriter();
		transformer.transform( new DOMSource( document ), new StreamResult( stringWriter ) );
		return stringWriter.toString();
	}

	private static Transformer getTransformer() throws TransformerConfigurationException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		return transformerFactory.newTransformer();
	}

}
