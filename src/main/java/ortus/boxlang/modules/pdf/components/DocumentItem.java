
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

import ortus.boxlang.modules.pdf.util.ModuleKeys;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

public class DocumentItem extends Component {

	/**
	 * Constructor
	 */
	public DocumentItem() {
		super();
		// Uncomment and define declare argument to this Component
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.type, "string" ), // "pagebreak|header|footer"
		    new Attribute( ModuleKeys.evalAtPrint, "string" ) // "true"
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
		// Replace this example component function body with your own implementation;
		// Example, passing through to a registered BIF
		// IStruct response = StructCaster.cast( runtime.getFunctionService().getGlobalFunction( Key.Foo ).invoke( context, attributes, false, Key.Foo ) );

		// Set the result(s) back into the page
		// ExpressionInterpreter.setVariable( context, attributes.getAsString( Key.variable ), response.getAsString( Key.output ) );

		return DEFAULT_RETURN;
	}

}
