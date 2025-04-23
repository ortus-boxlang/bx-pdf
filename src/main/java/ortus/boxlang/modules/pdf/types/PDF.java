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
package ortus.boxlang.modules.pdf.types;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFEncryption;

import com.lowagie.text.pdf.PdfWriter;

import ortus.boxlang.modules.pdf.util.Base64PDFReplacementFactory;
import ortus.boxlang.modules.pdf.util.ModuleKeys;
import ortus.boxlang.modules.pdf.util.PDFUtil;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class PDF {

	/**
	 * The renderer instance used to create the final PDF
	 */
	private ITextRenderer				renderer;

	/**
	 * Valid font extensions which can be imported
	 */
	ArrayList<String>					validFontExtensions	= new ArrayList<String>( List.of( "ttf", "otf", "ttc", "woff", "afm", "pfb" ) );

	/**
	 * The logger instance
	 */
	private static final Logger			logger				= BoxRuntime.getInstance().getLoggingService().getLogger( PDF.class.getSimpleName() );

	/**
	 * The initial attributes provided to the component
	 */
	private IStruct						componentAttributes;

	/**
	 * An array of document sections
	 */
	public ArrayList<IStruct>			documentParts		= new ArrayList<IStruct>();

	/**
	 * An array containing book marks of the PDF document
	 */
	public ArrayList<String>			bookmarks			= new ArrayList<String>();

	/**
	 * The default settings for the PDF
	 */
	public boolean						embedFonts			= false;
	public boolean						bookmarkSections	= true;
	public boolean						bookmarkAnchors		= false;

	private static final double			defaultMarginTop	= 1d;
	private static final double			defaultMarginBottom	= 1d;
	private static final double			defaultMarginLeft	= 1d;
	private static final double			defaultMarginRight	= 1d;

	public String						globalHeader		= "";
	public String						globalFooter		= "";
	public String						globalMeasureUnit	= "in";
	public double						globalMarginTop		= 1d;
	public double						globalMarginBottom	= 1d;
	public double						globalMarginLeft	= 1d;
	public double						globalMarginRight	= 1d;

	/**
	 * Base controlling the PDF headers, footers and content styles
	 */
	// @formatter:off
	private String				baseStyles			= """
		div.bx-pdf-header {
			display: block;
			padding-bottom: .25in;
			width: inherit;
			position: running(header);
		}
		div.bx-pdf-footer {
			display: block;
			width: inherit;
			padding-bottom: .25in;
			position: running(footer);
		}
		div.image-block {
			margin-top: auto;
			margin-bottom: auto;
			max-width: 100%;
			max-height: 100%;
		}
		span.currentpagenumber:before, span.currentsectionpagenumber:before {
			content: counter(page);
		}
		span.totalpages:before, span.totalsectionpagecount:before {
			content: counter(pages);
		}
		@page {
			@top-center { content: element(header) }
		}
		@page {
			@bottom-center { content: element(footer) }
		}
	""";
	// @formatter:on

	/**
	 * Default Page size
	 */
	private String						pageSize			= PDFUtil.PAGE_TYPES.get( ModuleKeys.A4 );
	/**
	 * Default orientation
	 */
	private String						orientation			= "portrait";

	private HashMap<String, Integer>	encryptionTypes		= new HashMap<String, Integer>() {

																{
																	put( "none", 0 );
																	put( "40-bit", 1 );
																	put( "128-bit", 0 );
																}
															};

	/**
	 * Constructor
	 *
	 * @param attributes
	 * @param executionState
	 */
	public PDF( IStruct attributes, IStruct executionState ) {
		renderer			= newRenderer();
		componentAttributes	= attributes;
		parseDefaults( attributes, executionState );
		parseEncryption( attributes );
		if ( attributes.containsKey( ModuleKeys.fontDirectory ) ) {
			ListUtil.asList( attributes.getAsString( ModuleKeys.fontDirectory ), ListUtil.DEFAULT_DELIMITER )
			    .stream()
			    .map( StringCaster::cast )
			    .forEach( this::addFontDirectory );
		}
	};

	/**
	 * Constructor which generates the PDF from a remote URL
	 *
	 * @param attributes
	 * @param executionState
	 */
	public PDF( String url, IStruct attributes ) {
		renderer = newRenderer();
		SharedContext sharedContext = renderer.getSharedContext();
		sharedContext.setReplacedElementFactory( new Base64PDFReplacementFactory() );
		sharedContext.getTextRenderer().setSmoothingThreshold( 0 );
		componentAttributes = attributes;
		parseEncryption( attributes );
		generateFromRemote( url );
	};

	/**
	 * Constructs a new ITextRenderer
	 *
	 * @return
	 */
	private ITextRenderer newRenderer() {
		ITextRenderer	newRenderer		= new ITextRenderer();
		SharedContext	sharedContext	= newRenderer.getSharedContext();
		sharedContext.setPrint( true );
		sharedContext.setInteractive( false );
		return newRenderer;
	}

	/**
	 * Add a binary item to the document
	 *
	 * @param item
	 *
	 * @return
	 */
	public PDF addDocumentItem( byte[] item ) {
		return addDocumentItem(
		    item,
		    globalHeader,
		    globalFooter,
		    componentAttributes,
		    Struct.of()
		);
	}

	/**
	 * Add a string content item to the document
	 *
	 * @param item
	 *
	 * @return
	 */
	public PDF addDocumentItem( String item ) {
		return addDocumentItem(
		    item,
		    globalHeader,
		    globalFooter,
		    componentAttributes,
		    Struct.of()
		);
	}

	/**
	 * Adds an item to the PDF document
	 *
	 * @param item
	 * @param header
	 * @param footer
	 * @param marginTop
	 * @param marginBottom
	 * @param marginLeft
	 * @param marginRight
	 *
	 * @return
	 */
	public PDF addDocumentItem(
	    String item,
	    String header,
	    String footer,
	    IStruct attributes,
	    IStruct state ) {
		documentParts.add(
		    Struct.of(
		        Key.content, item,
		        Key.header, header,
		        ModuleKeys.footer, footer,
		        Key.attributes, attributes,
		        Key.executionState, state
		    )
		);
		return this;
	}

	/**
	 * Adds a binary item to the PDF document
	 *
	 * @param item
	 * @param header
	 * @param footer
	 * @param marginTop
	 * @param marginBottom
	 * @param marginLeft
	 * @param marginRight
	 *
	 * @return
	 */
	public PDF addDocumentItem(
	    byte[] item,
	    String header,
	    String footer,
	    IStruct attributes,
	    IStruct state ) {
		documentParts.add(
		    Struct.of(
		        Key.content, item,
		        Key.header, header,
		        ModuleKeys.footer, footer,
		        Key.attributes, attributes,
		        Key.executionState, state
		    )
		);
		return this;
	}

	/**
	 * Loads a font directory for use in the PDF
	 *
	 * @param directory
	 *
	 * @return
	 */
	public PDF addFontDirectory( String directory ) {
		FileSystemUtil.listDirectory(
		    directory,
		    true,
		    validFontExtensions.stream().map( ext -> "*." + ext ).collect( Collectors.joining( "," ) ),
		    "namenocase",
		    "file"
		).forEach( font -> {
			try {
				renderer.getFontResolver().addFont( font.toAbsolutePath().toString(), font.getFileName().toString(), true );
			} catch ( IOException e ) {
				logger.error(
				    String.format(
				        "Error adding font [%s].  The messageReceived was: %s",
				        font.toAbsolutePath().toString(),
				        e.getMessage()
				    ),
				    e
				);
			}
		} );
		return this;
	}

	/**
	 * Generates the PDF from a remote URL
	 *
	 * @param url
	 *
	 * @return
	 */
	public PDF generateFromRemote( String url ) {
		Document pdfDocument = PDFUtil.parseRemoteFile( url );
		postProcessRemoteDocument( pdfDocument, url );
		renderer.setDocument( pdfDocument );
		return this;
	}

	/**
	 * Generates the finalized PDF
	 *
	 * @return
	 */
	public PDF generate() {

		String	content			= "<html>\n<head>\n";

		// @formatter:off
		String				bodyContents	= IntStream.range( 0, documentParts.size() )
		    .mapToObj( idx -> {
				IStruct part = documentParts.get( idx );
				String partContent = "";
				if( idx > 0 ){
					partContent += "<div style='page-break-before: always;'></div>\n";
				}
				String partIdentifier = UUID.randomUUID().toString();
				partContent += "<div class='bx-pdf-section' id='" + partIdentifier + "'>\n";

				IStruct partAttributes = part.getAsStruct( Key.attributes );

				try {

					String header		= part.get( Key.header ) != null ? part.getAsString( Key.header ) : globalHeader;
					String footer		= part.get( ModuleKeys.footer ) != null ? part.getAsString( ModuleKeys.footer ) : globalFooter;
					String partName     = partAttributes.getAsString( Key._NAME );

					if( bookmarkSections && partName != null ) {
						bookmarks.add( "<bookmark name='" + partName + "' href='#" + partIdentifier + "'/>" );
					}

					if ( header != null ) {
						partContent += "<div class='bx-pdf-header'>" + header + "</div>\n";
					}

					if ( footer != null ) {
						partContent += "<div class='bx-pdf-footer'>" + footer + "</div>\n";
					}

					Object contentValue = part.get( Key.content );
					if( contentValue instanceof byte[] ){
						// binary content handling
						byte[] bytes		= ( byte[] ) part.get( Key.content );
						String mimeType		= partAttributes.getAsString( ModuleKeys.mimeType );

						partContent += "<div class='body-image' align='center'><img src='data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString( bytes ).trim() + "'/></div>\n";

					} else {
						String item			= StringCaster.cast( contentValue );

						partContent += "<div class='bx-pdf-content'>" + item + "</div>\n";

						if ( bookmarkAnchors ) {
							// Parse our content in to a document so we can extract bookmarks
							Document parsedFragment = PDFUtil.parseContent( partContent );
							NodeList anchors = parsedFragment.getElementsByTagName( "a" );

							for ( int i = 0; i < anchors.getLength(); i++ ) {

								Node anchor = anchors.item( i );
								String id = null;
								String title = null;
								// Parse the deprecated name attribute then fallback to id/content
								Node nameNode	= anchor.getAttributes().getNamedItem( "name" );
								if ( nameNode == null ) {
									nameNode = anchor.getAttributes().getNamedItem( "id" );
									id = nameNode.getNodeValue();
									title = anchor.getNodeValue();
								} else {
									id = nameNode.getNodeValue();
									title = anchor.getNodeValue().length() > 1 ? anchor.getTextContent() : id;
								}
								if ( nameNode != null ) {
									bookmarks.add( "<bookmark name='" + title + "' href='#" + id + "'/>" );
								}
							}
						}

					}


					partContent += "</div>\n";

				}catch ( BoxRuntimeException e ) {
					logger.error(
						String.format(
							"Error generating PDF for document part [%s].  The messageReceived was: %s",
							part.getAsString( Key._NAME ),
							e.getMessage()
						),
						e
					);
				}

				return partContent;
			} )
		    .collect( Collectors.joining( "\n" ) );
		// @formatter:on
		if ( !bookmarks.isEmpty() ) {
			content += "<bookmarks>\n" + bookmarks.stream().collect( Collectors.joining( "\n" ) ) + "\n</bookmarks>\n";
		}
		content	+= "<style type='text/css'>\n" + getPageStyles( componentAttributes, globalFooter.trim().length() == 0 ) + "\n</style>\n";
		content	+= "</head>\n<body>\n";

		content	+= bodyContents;

		content	+= "</body>\n</html>";

		Document parsedContent = PDFUtil.parseContent( content );

		postProcessContent( parsedContent );

		// // Useful for debugging the HTML of the PDF before generation
		// System.out.println( W3CDom.asString( parsedContent, null ) );

		renderer.setDocument( parsedContent );

		return this;
	}

	/**
	 * Parses the encryption settings in the attributes
	 *
	 * @param attributes
	 */
	private void parseEncryption( IStruct attributes ) {
		int encryptionType = encryptionTypes.get( attributes.getAsString( ModuleKeys.encryption ).toLowerCase() );
		if ( encryptionType == 0 && attributes.get( ModuleKeys.openpassword ) == null && attributes.get( ModuleKeys.ownerPassword ) == null ) {
			return;
		}

		/**
		 * Privilege types - https://javadoc.io/doc/com.itextpdf/itextpdf/5.1.0/constant-values.html
		 * public static final int ALLOW_ASSEMBLY 1024
		 * public static final int ALLOW_COPY 16
		 * public static final int ALLOW_DEGRADED_PRINTING 4
		 * public static final int ALLOW_FILL_IN 256
		 * public static final int ALLOW_MODIFY_ANNOTATIONS 32
		 * public static final int ALLOW_MODIFY_CONTENTS 8
		 * public static final int ALLOW_PRINTING 2052
		 * public static final int ALLOW_SCREENREADERS 512
		 */
		PDFEncryption pdfEncryption = new PDFEncryption(
		    attributes.get( ModuleKeys.openpassword ) != null ? attributes.getAsString( ModuleKeys.openpassword ).getBytes() : null,
		    attributes.get( ModuleKeys.ownerPassword ) != null ? attributes.getAsString( ModuleKeys.ownerPassword ).getBytes() : null,
		    attributes.get( ModuleKeys.ownerPassword ) != null ? 0 : 16,
		    encryptionType
		);

		renderer.setPDFEncryption( pdfEncryption );
	}

	/**
	 * Parses the default settings for the PDF
	 *
	 * @param attributes
	 * @param executionState
	 */
	private void parseDefaults( IStruct attributes, IStruct executionState ) {

		IStruct header = PDFUtil.extractHeaderFromState( executionState );
		if ( header != null ) {
			globalHeader = header.getAsString( Key.result );
		}

		IStruct footer = PDFUtil.extractFooterFromState( executionState );
		if ( footer != null ) {
			globalFooter = footer.getAsString( Key.result );
		}

		globalMarginTop		= DoubleCaster.cast( attributes.getOrDefault( ModuleKeys.marginTop, defaultMarginTop ) );
		globalMarginBottom	= DoubleCaster.cast( attributes.getOrDefault( ModuleKeys.marginBottom, defaultMarginBottom ) );
		globalMarginLeft	= DoubleCaster.cast( attributes.getOrDefault( ModuleKeys.marginLeft, defaultMarginLeft ) );
		globalMarginRight	= DoubleCaster.cast( attributes.getOrDefault( ModuleKeys.marginRight, defaultMarginRight ) );

		bookmarkSections	= attributes.getAsBoolean( ModuleKeys.bookmark );
		bookmarkAnchors		= attributes.getAsBoolean( ModuleKeys.htmlBookmark );

		if ( attributes.getAsBoolean( ModuleKeys.pdfa ) ) {
			renderer.setPDFXConformance( PdfWriter.PDFA1A );
		}

	}

	/**
	 * Parses the page style settings for the PDF
	 *
	 * @param attributes
	 * @param showCounter
	 */
	public String getPageStyles( IStruct attributes, Boolean showCounter ) {
		String pageStyles = baseStyles;
		globalMeasureUnit	= attributes.getAsString( ModuleKeys.unit );
		pageStyles			+= "@page{";
		if ( attributes.get( ModuleKeys.pageType ) != null ) {
			Key typeKey = Key.of( attributes.getAsString( ModuleKeys.pageType ) );
			if ( typeKey.equals( ModuleKeys.custom ) ) {
				String	pageHeight	= DoubleCaster.cast( attributes.getOrDefault( ModuleKeys.pageHeight, 11.7d ) ) + "in";
				String	pageWidth	= DoubleCaster.cast( attributes.getOrDefault( ModuleKeys.pageWidth, 8.3d ) ) + "in";
				pageSize = pageHeight + " " + pageWidth;
			} else if ( !PDFUtil.PAGE_TYPES.containsKey( typeKey ) ) {
				throw new BoxRuntimeException( String.format( "The page type [%s] is not a supported page size", typeKey.getName() ) );
			} else {
				pageSize = PDFUtil.PAGE_TYPES.get( typeKey );
				if ( PDFUtil.PAGE_DIMENSIONS.containsKey( pageSize ) ) {
					double[] dimensions = PDFUtil.PAGE_DIMENSIONS.get( pageSize );
					pageSize = dimensions[ 0 ] + "mm " + dimensions[ 1 ] + "mm";
				}
			}
		}
		pageStyles += "size: " + pageSize + " " + StringCaster.cast( attributes.getOrDefault( ModuleKeys.orientation, orientation ) ).toLowerCase() + ";\n";

		if ( attributes.getAsInteger( Key.scale ) != null ) {
			pageStyles += "scale( " + attributes.getAsInteger( Key.scale ).doubleValue() / 100d + " );\n";
		}

		pageStyles	+= "; margin-top: " + globalMarginTop + globalMeasureUnit;
		pageStyles	+= "; margin-bottom: " + globalMarginBottom + globalMeasureUnit;
		pageStyles	+= "; margin-left: " + globalMarginLeft + globalMeasureUnit;
		pageStyles	+= "; margin-right: " + globalMarginRight + globalMeasureUnit;

		if ( showCounter ) {
			pageStyles += "@top-right { content: \"Page \" counter(pageNumber);}";
		}

		pageStyles += " }\n";

		if ( attributes.get( ModuleKeys.backgroundVisible ) != null && attributes.getAsBoolean( ModuleKeys.backgroundVisible ) ) {
			pageStyles += "background: none!important;\n";
		}

		return pageStyles;
	}

	/**
	 * Post-processes the remote document
	 *
	 * @param parsedDocument
	 * @param sourceUrl
	 */
	void postProcessRemoteDocument( Document parsedDocument, String sourceUrl ) {
		URI uri;
		try {
			uri = new URI( sourceUrl );
		} catch ( URISyntaxException e ) {
			throw new BoxRuntimeException( "Error parsing URI " + sourceUrl, e );
		}
		String		baseURL	= uri.getScheme() + "://" + uri.getHost();
		NodeList	imgTags	= parsedDocument.getElementsByTagName( "img" );
		for ( int i = 0; i < imgTags.getLength(); i++ ) {
			Node	img	= imgTags.item( i );
			String	src	= img.getAttributes().getNamedItem( "src" ).getNodeValue();
			if ( !src.startsWith( "http" ) ) {
				if ( src.startsWith( "/" ) ) {
					src = baseURL + src;
				} else {
					src = baseURL + "/" + src;
				}
				img.getAttributes().getNamedItem( "src" ).setNodeValue( src );
			}
		}

		NodeList anchorTags = parsedDocument.getElementsByTagName( "a" );
		for ( int i = 0; i < anchorTags.getLength(); i++ ) {
			Node	anchor	= anchorTags.item( i );
			String	href	= anchor.getAttributes().getNamedItem( "href" ).getNodeValue();
			if ( !href.startsWith( "http" ) && !href.startsWith( "#" ) ) {
				if ( href.startsWith( "/" ) ) {
					href = baseURL + href;
				} else {
					href = baseURL + "/" + href;
				}
				anchor.getAttributes().getNamedItem( "href" ).setNodeValue( href );
			}
		}

		// Remove all meta tags
		NodeList metaTags = parsedDocument.getElementsByTagName( "meta" );
		for ( int i = 0; i < metaTags.getLength(); i++ ) {
			Node meta = metaTags.item( i );
			meta.getParentNode().removeChild( meta );
		}

		// Remove all script tags
		NodeList scriptTags = parsedDocument.getElementsByTagName( "script" );
		for ( int i = 0; i < scriptTags.getLength(); i++ ) {
			Node script = scriptTags.item( i );
			script.getParentNode().removeChild( script );
		}

	}

	/**
	 * Performs post-processing on the finalized PDF document
	 *
	 * @param parsDocument
	 */
	void postProcessContent( Document parsedDocument ) {
		boolean		localURL	= componentAttributes.getAsBoolean( ModuleKeys.localUrl );

		// Process image sources and convert to Base64 Href
		NodeList	imgTags		= parsedDocument.getElementsByTagName( "img" );

		for ( int i = 0; i < imgTags.getLength(); i++ ) {
			Node	img	= imgTags.item( i );
			String	src	= img.getAttributes().getNamedItem( "src" ).getNodeValue();
			if ( src.startsWith( "data:" ) ) {
				continue;
			} else if ( localURL || ( !localURL && src.startsWith( "http" ) ) ) {
				byte[]	bytes		= ( byte[] ) FileSystemUtil.read( src );
				String	mimeType	= FileSystemUtil.getMimeType( src );
				if ( mimeType.equals( "image/jpeg" ) ) {
					mimeType = "image/jpg";
				}
				src = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString( bytes ).trim();
			}
			img.getAttributes().getNamedItem( "src" ).setNodeValue( src );
		}

	}

	/**
	 * Returns the global header for this PDF
	 *
	 * @return
	 */
	public String getHeader() {
		return globalHeader;
	}

	/**
	 * Returns the global footer for this PDF
	 *
	 * @return
	 */
	public String getFooter() {
		return globalFooter;
	}

	/**
	 * Returns the global top margin for this PDF
	 *
	 * @return the margin value
	 */
	public double getMarginTop() {
		return globalMarginTop;
	}

	/**
	 * Returns the global bottom margin for this PDF
	 *
	 * @return the margin value
	 */
	public double getMarginBottom() {
		return globalMarginBottom;
	}

	/**
	 * Returns the global left margin for this PDF
	 *
	 * @return the margin value
	 */
	public double getMarginLeft() {
		return globalMarginLeft;
	}

	/**
	 * Returns the global right margin for this PDF
	 *
	 * @return the margin value
	 */
	public double getMarginRight() {
		return globalMarginRight;
	}

	/**
	 * Retrieves the renderer for this PDF
	 *
	 * @return
	 */
	public ITextRenderer getRenderer() {
		return renderer;
	}

	/**
	 * returns a binary representation of the PDF
	 *
	 * @return
	 */
	public byte[] toBinary() {
		try ( ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ) {
			renderer.layout();
			renderer.createPDF( outputStream, true );
			renderer.finishPDF();
			return outputStream.toByteArray();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	public void toFile(
	    String filename,
	    boolean overwrite ) {
		try (
		    OutputStream outputStream = Files.newOutputStream( Path.of( filename ), overwrite ? StandardOpenOption.CREATE : StandardOpenOption.CREATE_NEW ) ) {
			renderer.layout();
			renderer.createPDF( outputStream, true );
			renderer.finishPDF();
		} catch ( IOException e ) {
			logger.error( "Error creating PDF", e );
		}
	}
}
