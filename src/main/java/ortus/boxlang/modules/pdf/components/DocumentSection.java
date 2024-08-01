
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

@BoxComponent( allowsBody = true, requiresBody = true )
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
		        "text/html",
		        Set.of(
		            Validator.valueOneOf( "text/html", "text/plain", "application/xml", "image/jpeg", "image/png", "image/bmp", "image/gif" )
		        ) ), // "text/plain|application/xmlimage/jpeg|image/png|image/bmp|image/gif"
		    new Attribute( Key._NAME, "string" ), // "bookmark for the section"
		    new Attribute( ModuleKeys.srcfile, "string" ), // "absolute path of file"
		    // URL attributes
		    new Attribute( ModuleKeys.src, "string" ), // "URL|path relative to web root"
		    new Attribute( ModuleKeys.userAgent, "string" ), // "HTTP user agent identifier"
		    new Attribute( ModuleKeys.authPassword, "string" ), // "authentication password"
		    new Attribute( ModuleKeys.authUser, "string" ), // "authentication user name"

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
