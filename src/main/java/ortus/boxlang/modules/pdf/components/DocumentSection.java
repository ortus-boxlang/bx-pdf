
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

import ortus.boxlang.modules.pdf.util.ModuleKeys;
import ortus.boxlang.modules.pdf.util.PDFUtil;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true, requiresBody = false )
public class DocumentSection extends Component {

	/**
	 * Constructor
	 */
	public DocumentSection() {
		super();
		// Uncomment and define declare argument to this Component
		declaredAttributes = new Attribute[] {
		    new Attribute( ModuleKeys.marginBottom, "numeric" ), // "number"
		    new Attribute( ModuleKeys.marginLeft, "numeric" ), // "number"
		    new Attribute( ModuleKeys.marginRight, "numeric" ), // "number"
		    new Attribute( ModuleKeys.marginTop, "numeric" ), // "number"
		    new Attribute(
		        ModuleKeys.mimeType,
		        "string",
		        Set.of(
		            Validator.valueOneOf( "text/html", "text/plain", "application/xml", "image/jpeg", "image/png", "image/bmp", "image/gif" )
		        ) ), // "text/plain|application/xmlimage/jpeg|image/png|image/bmp|image/gif"
		    new Attribute( Key._NAME, "string" ), // "bookmark for the section"
		    new Attribute( ModuleKeys.srcfile, "string" ), // "absolute path of file"
		    // URL attributes
		    new Attribute( ModuleKeys.src, "string" ), // "URL|path relative to web root"
		    new Attribute( ModuleKeys.userAgent, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "HTTP user agent identifier"
		    new Attribute( ModuleKeys.authPassword, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "authentication password"
		    new Attribute( ModuleKeys.authUser, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "authentication user name"

		};
	}

	// @formatter:off
	/**
	 * Divides a PDF document into sections. Used in conjunction with a `documentitem` component, each section can have unique headers, footers, and page numbers.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.marginBottom The bottom margin of the section in the unit specified in the `document` component.
	 * @attribute.marginLeft The left margin of the section in the unit specified in the `document` component.
	 * @attribute.marginRight The right margin of the section in the unit specified in the `document` component.
	 * @attribute.marginTop The top margin of the section in the unit specified in the `document` component.
	 * @attribute.mimeType The mime type of the content.  If the content is a file, the mime type is determined by the file extension.  If the content is a URL, the mime type is determined by the HTTP response.
	 * @attribute.name The name of the section.  This is used as a bookmark for the section.
	 * @attribute.srcfile The absolute path of the file to include in the section.
	 * @attribute.src The URL or path relative to the web root of the content to include in the section.
	 * @attribute.userAgent The HTTP user agent identifier to use when fetching the content from a URL. Not currently implemented
	 * @attribute.authPassword The authentication password to use when fetching the content from a URL. Not currently implemented
	 * @attribute.authUser The authentication user name to use when fetching the content from a URL. Not currently implemented
	 *
	 */
	// @formatter:on
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {

		context.getDefaultAssignmentScope().put( PDFUtil.DOCUMENT_LOCAL_VARIABLE, PDFUtil.DOCUMENT_LOCAL_PLACEHOLDERS );

		executionState.put( ModuleKeys.documentItems, new Array() );

		IStruct parentState = context.findClosestComponent( ModuleKeys.Document );
		if ( parentState == null ) {
			throw new BoxRuntimeException( "DocumentSection must be nested in the body of an Document component" );
		}
		attributes.put( ModuleKeys.documentItems, new Array() );

		StringBuffer buffer = new StringBuffer();

		processBody( context, body, buffer );

		attributes.put( Key.result, buffer.toString() );

		// Add our section to the document
		parentState.getAsArray( ModuleKeys.documentSections ).add( Struct.of(
		    Key.attributes, attributes,
		    Key.executionState, executionState
		) );

		return DEFAULT_RETURN;
	}

}
