package com.tlc.sql.common.registrar.model;

import com.tlc.commons.service.registrar.model.RegistrarData;

import com.tlc.sql.common.registrar.SqlResourceRegistrar;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;

/**
 * <p>
 * A model that holds the data of the {@link SqlResourceRegistrar} which is needed for registering the resources/actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
@Setter
@Getter
public class SqlRegistrarData extends RegistrarData {

    private URL metaFileUrl;
    private URL dataFileUrl;

}