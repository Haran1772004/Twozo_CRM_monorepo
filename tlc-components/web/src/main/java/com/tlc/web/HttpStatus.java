package com.tlc.web;

/**
 * HTTP Status codes
 *
 * @author Selvakumar
 * @version 1.0
 */
public enum HttpStatus {

    OK(200),
    ACCEPTED(202),
    NO_CONTENT(204),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    INTERNAL_SERVER_ERROR(500),
    PERMANENT_REDIRECT(301),
    TEMPORARY_REDIRECT(302),
    PROCESSING(102)
    ;

    final int code;

    HttpStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static boolean isRedirect(HttpStatus code) {
        return code == HttpStatus.TEMPORARY_REDIRECT || code == HttpStatus.PERMANENT_REDIRECT;
    }


}
