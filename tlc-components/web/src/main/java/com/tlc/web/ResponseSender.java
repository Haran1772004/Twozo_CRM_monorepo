package com.tlc.web;

import java.io.OutputStream;
import java.util.Map;


/**
 * @author Abishek
 * @version 1.0
 */
public interface ResponseSender
{
	OutputStream getOutputStream();

	void send(final Map<String, String> headers, final byte[] bytes);

	void send(final Map<String, String> headers, final byte[] bytes, HttpStatus httpStatus);

}
