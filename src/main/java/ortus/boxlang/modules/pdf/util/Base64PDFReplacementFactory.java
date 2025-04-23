package ortus.boxlang.modules.pdf.util;

import java.io.IOException;
import java.util.Base64;

import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

import com.lowagie.text.Image;

public class Base64PDFReplacementFactory implements ReplacedElementFactory {

	public ReplacedElement createReplacedElement( LayoutContext layoutContext, BlockBox box, UserAgentCallback callback, int cssWidth, int cssHeight ) {
		Element element = box.getElement();
		if ( element == null ) {
			return null;
		}
		String nodeName = element.getNodeName();
		if ( nodeName.equals( "img" ) ) {
			String	attribute	= element.getAttribute( "src" );
			FSImage	fsImage;
			try {
				fsImage = buildImage( attribute, callback );
			} catch ( IOException e ) {
				fsImage = null;
			}
			if ( fsImage != null ) {
				if ( cssWidth != -1 || cssHeight != -1 ) {
					fsImage.scale( cssWidth, cssHeight );
				} else {
					fsImage.scale( box.getMaxWidth(), 0 );
				}

				ITextImageElement imageElement = new ITextImageElement( fsImage );
				return imageElement;
			}
		}
		return null;
	}

	protected FSImage buildImage( String srcAttr, UserAgentCallback callback ) throws IOException {
		FSImage fsImage;
		if ( srcAttr.startsWith( "data:image/" ) ) {
			String	b64encoded		= srcAttr.substring( srcAttr.indexOf( "base64," ) + "base64,".length(), srcAttr.length() );
			byte[]	decodedBytes	= Base64.getDecoder().decode( b64encoded );
			fsImage = new ITextFSImage( Image.getInstance( decodedBytes ) );
		} else {
			fsImage = new ITextFSImage( Image.getInstance( callback.getBinaryResource( srcAttr ) ) );
		}
		return fsImage;
	}

	public void remove( Element e ) {
	}

	public void reset() {
	}

	@Override
	public void setFormSubmissionListener( FormSubmissionListener listener ) {
	}
}