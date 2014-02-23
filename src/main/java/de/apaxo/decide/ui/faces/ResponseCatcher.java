package de.apaxo.decide.ui.faces;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.Response;

/**
 * This is a response wrapper which saves all of the output to a char array, so
 * it can be retrieved as a string afterwards with the toString() method. We
 * only support capturing text output currently.
 */
public class ResponseCatcher extends org.apache.catalina.connector.ResponseFacade implements HttpServletResponse {

	/** the backing output stream for text content */
	CharArrayWriter output;

	/** a writer for the servlet to use */
	PrintWriter writer;

	/** a real response object to pass tricky methods to */
	HttpServletResponse response;

	/**
	 * Create the response wrapper.
	 */
	public ResponseCatcher(HttpServletResponse response) {
		super(new Response());
		this.response = response;
		output = new CharArrayWriter();
		writer = new PrintWriter(output, true);
	}

	/**
	 * Return a print writer so it can be used by the servlet. The print writer
	 * is used for text output.
	 */
	public PrintWriter getWriter() {
		return writer;
	}

	public void flushBuffer() throws IOException {
		writer.flush();
	}

	public boolean isCommitted() {
		return false;
	}

	public boolean containsHeader(String arg0) {
		return false;
	}

	/* wrapped methods */
	public String encodeURL(String arg0) {
		return response.encodeURL(arg0);
	}

	public String encodeRedirectURL(String arg0) {
		return response.encodeRedirectURL(arg0);
	}

	public String encodeUrl(String arg0) {
		return response.encodeUrl(arg0);
	}

	public String encodeRedirectUrl(String arg0) {
		return response.encodeRedirectUrl(arg0);
	}

	public String getCharacterEncoding() {
		return response.getCharacterEncoding();
	}

	public String getContentType() {
		return response.getContentType();
	}

	public int getBufferSize() {
		return response.getBufferSize();
	}

	public Locale getLocale() {
		return response.getLocale();
	}

	public void sendError(int arg0, String arg1) throws IOException {
		response.sendError(arg0, arg1);
	}

	public void sendError(int arg0) throws IOException {
		response.sendError(arg0);
	}

	public void sendRedirect(String arg0) throws IOException {
		response.sendRedirect(arg0);
	}

	@Override
	public void addCookie(Cookie arg0) {
		response.addCookie(arg0);
	}

	@Override
	public void setDateHeader(String arg0, long arg1) {
		response.setDateHeader(arg0, arg1);
	}

	@Override
	public void addDateHeader(String arg0, long arg1) {
		response.addDateHeader(arg0, arg1);
	}

	@Override
	public void setHeader(String arg0, String arg1) {
		response.setHeader(arg0, arg1);
	}

	@Override
	public void addHeader(String arg0, String arg1) {
		response.addHeader(arg0, arg1);
	}

	@Override
	public void setIntHeader(String arg0, int arg1) {
		response.setIntHeader(arg0, arg1);
	}

	@Override
	public void addIntHeader(String arg0, int arg1) {
		response.addIntHeader(arg0, arg1);
	}

	@Override
	public void setContentLength(int arg0) {
		response.setContentLength(arg0);
	}

	@Override
	public void setContentType(String arg0) {
		response.setContentType(arg0);
	}

	/* null ops */
	@Override
	public void setStatus(int arg0) {
	}

	@Override
	public void setStatus(int arg0, String arg1) {
	}

	@Override
	public void setBufferSize(int arg0) {
	}

	@Override
	public void resetBuffer() {
	}

	@Override
	public void reset() {
	}

	@Override
	public void setLocale(Locale arg0) {
	}

	/* unsupported methods */
	public ServletOutputStream getOutputStream() throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Return the captured content.
	 */
	@Override
	public String toString() {
		return output.toString();
	}

	@Override
	public void setCharacterEncoding(String arg0) {

	}

	@Override
	public String getHeader(String arg0) {
		return response.getHeader(arg0);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return response.getHeaderNames();
	}

	@Override
	public Collection<String> getHeaders(String arg0) {
		return response.getHeaders(arg0);
	}

	@Override
	public int getStatus() {
		return 0;
	}
}