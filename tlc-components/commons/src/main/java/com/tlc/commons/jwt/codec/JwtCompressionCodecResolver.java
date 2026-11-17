package com.tlc.commons.jwt.codec;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import io.jsonwebtoken.CompressionCodec;
import io.jsonwebtoken.CompressionCodecResolver;
import io.jsonwebtoken.CompressionException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.impl.compression.GzipCompressionCodec;

/**
 * A resolver that resolves the Compression codec for a given JWT header
 *
 * @author Selvakumar
 * @version 1.0
 */
public class JwtCompressionCodecResolver implements CompressionCodecResolver {

    public static final CompressionCodec GZIP_COMPRESSION = new GzipCompressionCodec();

    /**
     * Resolves compression codec
     *
     * @param header JWT header
     * @return {@link  CompressionCodec}
     * @throws CompressionException
     */
    @Override
    public CompressionCodec resolveCompressionCodec(Header header) throws CompressionException {
        if (GZIP_COMPRESSION.getAlgorithmName().equals(header.getCompressionAlgorithm())) {
            return GZIP_COMPRESSION;
        } else {
            throw ErrorCode.get(ErrorCodes.INVALID_JWT_HEADER);
        }
    }

}
