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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.PDFEncryption;

import ortus.boxlang.modules.pdf.util.ModuleKeys;
import ortus.boxlang.modules.pdf.util.PDFUtil;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class PDF {

	private ITextRenderer		renderer			= new ITextRenderer();

	ArrayList<String>			validFontExtensions	= new ArrayList<String>( List.of( "ttf", "otf", "ttc", "woff", "afm", "pfb" ) );

	private static final Logger	logger				= LoggerFactory.getLogger( PDF.class );

	public ArrayList<IStruct>	documentParts		= new ArrayList<IStruct>();

	public static boolean		embedFonts			= false;
	public static boolean		applyBookmarks		= false;

	private static final double	defaultMarginTop	= -1d;
	private static final double	defaultMarginBottom	= -1d;
	private static final double	defaultMarginLeft	= -1d;
	private static final double	defaultMarginRight	= -1d;

	public static String		globalHeader		= "";
	public static String		globalFooter		= "";
	public static double		globalMarginTop		= -1d;
	public static double		globalMarginBottom	= -1d;
	public static double		globalMarginLeft	= -1d;
	public static double		globalMarginRight	= -1d;
	// @formatter:off
	private String				pageStyles			= """
		div.pdf-header {
			display: block; text-align: center;
			position: running(header);
		}
		div.pdf-footer {
			display: block; text-align: center;
			position: running(footer);
		}
		div.pdf-section{
			page-break-after: always;
		}
		@page {
			@top-center { content: element(header) }
			}
		@page {
			@bottom-center { content: element(footer) }
		}
	""";
	// @formatter:on

	private String				pageSize			= PDFUtil.PAGE_TYPES.get( ModuleKeys.A4 );
	private String				orientation			= "portrait";

	public PDF( IStruct attributes, IStruct executionState ) {
		parseDefaults( attributes, executionState );
		parseEncryption( attributes );
		if ( attributes.containsKey( ModuleKeys.fontDirectory ) ) {
			addFontDirectory( attributes.getAsString( ModuleKeys.fontDirectory ) );
		}
	};

	public PDF addDocumentItem(
	    String item ) {
		return addDocumentItem(
		    item,
		    globalHeader,
		    globalFooter,
		    globalMarginTop,
		    globalMarginBottom,
		    globalMarginLeft,
		    globalMarginRight
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
	    Double marginTop,
	    Double marginBottom,
	    Double marginLeft,
	    Double marginRight ) {
		documentParts.add(
		    Struct.of(
		        Key.content, item,
		        Key.header, header,
		        ModuleKeys.footer, footer,
		        ModuleKeys.marginTop, marginTop,
		        ModuleKeys.marginBottom, marginBottom,
		        ModuleKeys.marginLeft, marginLeft,
		        ModuleKeys.marginRight, marginRight
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
		    false,
		    validFontExtensions.stream().map( ext -> "*." + ext ).collect( Collectors.joining( "," ) ),
		    "namenocase",
		    "file"
		).forEach( font -> {
			try {
				renderer.getFontResolver().addFont( font.toAbsolutePath().toString(), font.getFileName().toString(), true );
			} catch ( IOException e ) {
				logger.atError().log(
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
	 * Generates the finalized PDF
	 *
	 * @return
	 */
	public PDF generate() {

		String content = "<html>\n<head>\n";

		content	+= "<style>\n" + pageStyles + "\n</style>\n";

		content	+= "</head>\n<body>\n";

		content	+= documentParts.stream()
		    .map( part -> {
					    String partContent = "";
					    try {
						    String item			= part.getAsString( Key.content );
						    String header		= part.getAsString( Key.header );
						    String footer		= part.getAsString( ModuleKeys.footer );
						    Double marginTop	= part.getAsDouble( ModuleKeys.marginTop );
						    Double marginBottom	= part.getAsDouble( ModuleKeys.marginBottom );
						    Double marginLeft	= part.getAsDouble( ModuleKeys.marginLeft );
						    Double marginRight	= part.getAsDouble( ModuleKeys.marginRight );

						    String partIdentifier = UUID.randomUUID().toString();

						    partContent += "<div class='pdf-section' id='" + partIdentifier + "'>\n";

						    if ( header != null ) {
							    partContent += "<div class='pdf-header'>" + header + "</div>\n";
						    }

						    if ( footer != null ) {
							    partContent += "<div class='pdf-footer'>" + footer + "</div>\n";
						    }

						    partContent += "<div class='content'>" + item + "</div>\n";

					    } catch ( BoxRuntimeException e ) {
						    logger.atError().log(
						        String.format(
						            "Error generating PDF for document part [%s].  The messageReceived was: %s",
						            part.getAsString( Key._NAME ),
						            e.getMessage()
						        ),
						        e
						    );
					    }

					    return partContent;
				    } );

		content	+= "</body>\n</html>";

		renderer.setDocument( PDFUtil.parseContent( content ) );

		return this;
	}

	/**
	 * Parses the encryption settings in the attributes
	 *
	 * @param attributes
	 */
	private void parseEncryption( IStruct attributes ) {
		PDFEncryption pdfEncryption = new PDFEncryption();
		if ( attributes.get( ModuleKeys.openpassword ) != null ) {
			pdfEncryption.setUserPassword( attributes.getAsString( ModuleKeys.openpassword ).getBytes() );
		}
		if ( attributes.get( ModuleKeys.ownerPassword ) != null ) {
			pdfEncryption.setOwnerPassword( attributes.getAsString( ModuleKeys.ownerPassword ).getBytes() );
		}
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

		parsePageStyles( attributes, globalFooter.trim().length() == 0 );

	}

	/**
	 * Parses the page styles for the PDF
	 *
	 * @param attributes
	 * @param showCounter
	 */
	public void parsePageStyles( IStruct attributes, Boolean showCounter ) {
		pageStyles += "@page{";
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
		pageStyles += "size: " + pageSize + " " + StringCaster.cast( attributes.getOrDefault( ModuleKeys.orientation, orientation ) ).toLowerCase();

		if ( globalMarginTop != -1d ) {
			pageStyles += "; margin-top: " + globalMarginTop + "mm";
		}
		if ( globalMarginBottom != -1d ) {
			pageStyles += "; margin-bottom: " + globalMarginBottom + "mm";
		}
		if ( globalMarginLeft != -1d ) {
			pageStyles += "; margin-left: " + globalMarginLeft + "mm";
		}
		if ( globalMarginRight != -1d ) {
			pageStyles += "; margin-right: " + globalMarginRight + "mm";
		}

		if ( showCounter ) {
			pageStyles += "@top-right { content: \"Page \" counter(pageNumber);}";
		}

		pageStyles += " }\n";

		if ( attributes.get( ModuleKeys.backgroundVisible ) != null && attributes.getAsBoolean( ModuleKeys.backgroundVisible ) ) {
			pageStyles += "background: none!important;\n";
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
			renderer.createPDF( outputStream );
			return outputStream.toByteArray();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	public void toFile( String filename ) {
		try ( OutputStream outputStream = Files.newOutputStream( Path.of( filename ), StandardOpenOption.CREATE ) ) {
			renderer.createPDF( outputStream );
		} catch ( IOException e ) {
			logger.atError().log( "Error creating PDF", e );
		}
	}
}
