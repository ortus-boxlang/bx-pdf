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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

import ortus.boxlang.modules.pdf.util.ModuleKeys;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class PDF {

	private ITextRenderer		renderer			= new ITextRenderer();

	ArrayList<String>			validFontExtensions	= new ArrayList<String>( List.of( "ttf", "otf", "ttc", "woff", "afm", "pfb" ) );

	private static final Logger	logger				= LoggerFactory.getLogger( PDF.class );

	public List<IStruct>		documentParts		= new ArrayList<IStruct>();

	public PDF() {

	}

	public PDF addDocumentItem(
	    Document item,
	    Document header,
	    Document footer,
	    double marginTop,
	    double marginBottom,
	    double marginLeft,
	    double marginRight ) {
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

	public PDF generate() {
		return this;
	}

	public byte[] toBinary() {
		return new byte[ 0 ];
	}

}
