package com.tlc.validator;

import java.io.Serializable;

/**
 * @author Abishek
 * @version 1.0
 */
public interface TlcModel extends Serializable
{

    Long id();

    Long orgId();

    Object identity();
}
