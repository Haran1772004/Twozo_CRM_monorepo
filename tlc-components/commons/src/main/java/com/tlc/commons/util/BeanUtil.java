package com.tlc.commons.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * <p>
 * Provides the services for cloning the object using bean util
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public final class BeanUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeanUtil.class);

    /**
     * <p>
     * Clones the object using clone bean
     * </p>
     *
     * @param model Represents the model
     * @return The cloned object
     */
    public static <T> T clone(final T model) {
        try {
            return (T) BeanUtils.cloneBean(model);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException | NoSuchMethodException exception) {
            LOGGER.error(String.format("Failed to clone value from model Model - %s", model), exception);
            throw ErrorCode.get(ErrorCodes.BEAN_REPLICATION_FAILED);
        }
    }
}
