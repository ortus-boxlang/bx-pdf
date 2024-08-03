
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

import java.util.Set;
import java.util.stream.Collectors;

import ortus.boxlang.modules.pdf.types.PDF;
import ortus.boxlang.modules.pdf.util.ModuleKeys;
import ortus.boxlang.modules.pdf.util.PDFUtil;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true, requiresBody = true )
public class Document extends Component {

	/**
	 * Constructor
	 */
	public Document() {
		super();
		// Uncomment and define declare argument to this Component
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.format, "string", "pdf" ), // "PDF|FlashPaper"
		    new Attribute( ModuleKeys.encryption, "string", "none" ), // "128-bit|40-bit|none"
		    new Attribute( ModuleKeys.localUrl, "boolean" ), // "yes|no"
		    new Attribute( Key.variable, "string" ), // "output variable name"

		    // PDF Generation options
		    new Attribute( ModuleKeys.backgroundVisible, "boolean", true ), // "yes|no"
		    new Attribute( ModuleKeys.bookmark, "boolean", true ), // "yes|no"
		    new Attribute( ModuleKeys.htmlBookmark, "boolean", false ), // If true, it is possible to convert outlines to a list of named anchors (<a
		    // name="anchor_id">label</a>) or a headings structure (<h1>...<h6>). Transforming of HTML
		    // hyperlinks to PDF hyperlinks (if not explicitly disabled). Hyperlink jumps within the same
		    // document are supported as well

		    // Formatting attributes
		    new Attribute( ModuleKeys.orientation, "string", "portrait", Set.of( Validator.valueOneOf( "portrait", "landscape" ) ) ), // "portrait|landscape"
		    new Attribute( Key.scale, "integer" ), // "percentage less than 100"
		    new Attribute( ModuleKeys.marginBottom, "double" ), // "number"
		    new Attribute( ModuleKeys.marginLeft, "double" ), // "number"
		    new Attribute( ModuleKeys.marginRight, "double" ), // "number"
		    new Attribute( ModuleKeys.marginTop, "double" ), // "number"
		    new Attribute( ModuleKeys.pageWidth, "double" ), // "page width in inches"
		    new Attribute( ModuleKeys.pageHeight, "double" ), // "page height in inches"

		    // Font handling attributes
		    new Attribute( ModuleKeys.fontEmbed, "boolean", true ), // "yes|no"
		    new Attribute( ModuleKeys.fontDirectory, "string" ), // "yes|no"

		    // Document security and file system attributes
		    new Attribute( ModuleKeys.openpassword, "string" ), // "password to open protected documents"
		    new Attribute( ModuleKeys.ownerPassword, "string" ), // "password"
		    new Attribute( ModuleKeys.pageType, "string" ), // "page type"
		    new Attribute( ModuleKeys.pdfa, "string", false ), // "yes|no"

		    // File creation atrributes
		    new Attribute( ModuleKeys.filename, "string" ), // "filename"
		    new Attribute( Key.overwrite, "string" ), // "yes|no"
		    new Attribute( ModuleKeys.saveAsName, "string" ), // "PDF filename"
		    new Attribute( ModuleKeys.src, "string" ), // "URL|pathname relative to web root"
		    new Attribute( ModuleKeys.srcfile, "string" ), // "absolute pathname to a file"
		    new Attribute(
		        ModuleKeys.mimeType,
		        "string",
		        "text/html",
		        Set.of(
		            Validator.valueOneOf( "text/html", "text/plain", "application/xml", "image/jpeg", "image/png", "image/bmp", "image/gif" )
		        )
		    ), // mimetype of the source (when attribute src or srcfile are defined)
		    new Attribute( ModuleKeys.tagged, "string" ), // "yes|no"
		    new Attribute( ModuleKeys.unit, "string" ), // "in|cm"
		    new Attribute( Key.userAgent, "string" ), // "HTTP user agent identifier"

		    new Attribute( ModuleKeys.proxyHost, "string" ), // "IP address or server name for proxy host"
		    new Attribute( Key.proxyPassword, "string" ), // "password for the proxy host"
		    new Attribute( Key.proxyPort, "string" ), // "port of the proxy host"
		    new Attribute( Key.proxyUser, "string" ), // "user name for the proxy host"

		    new Attribute( ModuleKeys.permissions, "string" ), // "permission list"
		    new Attribute( ModuleKeys.permissionspassword, "string" ), // "password to access restricted permissions"
		    new Attribute( ModuleKeys.userPassword, "string" ), // "password"
		    new Attribute( ModuleKeys.authPassword, "string" ), // "authentication password"
		    new Attribute( ModuleKeys.authUser, "string" ), // "authentication user name"

		    // Form field attributes - not implemented in standard module
		    new Attribute( ModuleKeys.formfields, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "yes|no"
		    new Attribute( ModuleKeys.formsType, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "FDF|PDF|HTML|XML"

		    // Placeholder for alternate variable name
		    new Attribute( Key._NAME, "string" )
		};
	}

	/**
	 * Describe what the invocation of your component does
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.foo Describe any expected arguments
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {

		executionState.put( ModuleKeys.documentItems, new Array() );
		executionState.put( ModuleKeys.documentSections, new Array() );

		String variable = attributes.getAsString( Key.variable );

		// Lucee and Adobe use different attributes for the variable name
		if ( variable != null && attributes.containsKey( Key._NAME ) ) {
			variable = attributes.getAsString( Key._NAME );
		}
		String	fileName		= attributes.getAsString( ModuleKeys.filename );
		String	browserFileName	= attributes.getAsString( ModuleKeys.saveAsName );
		String	mimeType		= attributes.getAsString( ModuleKeys.mimeType );

		// Ensure our font directories are absolute
		if ( attributes.getAsString( ModuleKeys.fontDirectory ) != null ) {
			attributes.put(
			    ModuleKeys.fontDirectory,
			    ListUtil.asList( attributes.getAsString( ModuleKeys.fontDirectory ), ListUtil.DEFAULT_DELIMITER )
			        .stream()
			        .map( StringCaster::cast )
			        .map( path -> path.substring( 0, 4 ).equalsIgnoreCase( "http" ) ? path
			            : FileSystemUtil.expandPath( context, path ).absolutePath().toString() )
			        .collect( Collectors.joining( ListUtil.DEFAULT_DELIMITER ) )
			);
		}

		StringBuffer	buffer			= new StringBuffer();
		Object			sourceFile		= null;
		PDF				pdf				= null;
		byte[]			binarySource	= null;

		if ( attributes.containsKey( ModuleKeys.src ) ) {
			attributes.put( ModuleKeys.srcfile, attributes.getAsString( ModuleKeys.src ) );
		}

		if ( attributes.getAsString( ModuleKeys.srcfile ) != null ) {
			// srcfile may be a URL, relative or absolute path
			String srcFile = attributes.getAsString( ModuleKeys.srcfile );
			if ( !srcFile.substring( 0, 4 ).equalsIgnoreCase( "http" ) ) {
				srcFile = FileSystemUtil.expandPath( context, srcFile ).absolutePath().toString();
			}
			sourceFile = FileSystemUtil.read( srcFile );
			if ( mimeType == null ) {
				attributes.put( ModuleKeys.mimeType, FileSystemUtil.getMimeType( srcFile ) );
			}
		} else {
			BodyResult bodyResult = processBody( context, body, buffer );

			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				return bodyResult;
			}
		}

		if ( sourceFile != null ) {
			if ( sourceFile instanceof String ) {
				buffer.append( sourceFile );
			} else {
				binarySource = ( byte[] ) sourceFile;
			}
		}

		if ( binarySource != null ) {
			pdf = PDFUtil.generatePDF( binarySource, context, attributes, executionState );
		} else {
			pdf = PDFUtil.generatePDF( buffer, context, attributes, executionState );
		}

		if ( variable != null ) {
			ExpressionInterpreter.setVariable(
			    context,
			    variable,
			    pdf.toBinary()
			);
			return DEFAULT_RETURN;
		} else if ( fileName != null ) {
			pdf.toFile(
			    FileSystemUtil.expandPath( context, fileName ).absolutePath().toString(),
			    attributes.getAsBoolean( Key.overwrite )
			);
			return DEFAULT_RETURN;
		} else {
			IStruct interceptorArgs = Struct.of(
			    Key.content, pdf.toBinary(),
			    Key.mimetype, "application/pdf",
			    ModuleKeys.filename, browserFileName != null ? browserFileName : "Document.pdf",
			    Key.reset, true,
			    Key.abort, true
			);
			runtime.getInterceptorService().announce( "writeToBrowser", interceptorArgs );
			// if we get here, the web runtime is not available
			throw new BoxRuntimeException(
			    " Web runtime not available.  The web-support module is required in order to write a PDF to the browser."
			);
		}

	}

}
