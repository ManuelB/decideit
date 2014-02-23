package de.apaxo.decide.ui.faces;

import com.sun.faces.renderkit.html_basic.TextareaRenderer;
import java.io.IOException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;

/**
 * A simple renderer for h:inputTextarea which allows to add some custom
 * attributes.
 */
public class InputTextareaRenderer extends TextareaRenderer {

	private static final String[] attributes = new String[] { "placeholder" };

	@Override
	protected void getEndTextToRender(FacesContext context,
			UIComponent component, String currentValue) throws IOException {
		final ResponseWriter originalResponseWriter = context
				.getResponseWriter();
		context.setResponseWriter(new ResponseWriterWrapper() {

			@Override
			public ResponseWriter getWrapped() {
				return originalResponseWriter;
			}

			@Override
			public void startElement(String name, UIComponent component)
					throws IOException {
				super.startElement(name, component);
				System.err.println(name);
				if ("textarea".equalsIgnoreCase(name)) {
					for (String attr : attributes) {
						final String value = (String) component.getAttributes()
								.get(attr);
						if (value != null) {
							super.writeAttribute(attr, value, attr);
						}
					}
				}
			}
		});

		super.getEndTextToRender(context, component, currentValue);
		context.setResponseWriter(originalResponseWriter); // Restore original
															// writer.
	}

}