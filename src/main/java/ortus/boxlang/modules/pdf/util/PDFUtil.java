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

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;

import ortus.boxlang.modules.pdf.types.PDF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.IStruct;

public class PDFUtil {

	// private List<DocumentItem> documentItems;

	public static PDF generatePDF( StringBuffer buffer, IBoxContext context, IStruct attributes, IStruct executionState ) {

		PDF pdf = new PDF();

		if ( !buffer.isEmpty() ) {

		}

		return pdf.generate();
	}

	public org.w3c.dom.Document parseContent( String content ) {
		org.jsoup.nodes.Document doc = Jsoup.parse( content );
		// Should reuse W3CDom instance if converting multiple documents.
		return new W3CDom().fromJsoup( doc );
	}

}
