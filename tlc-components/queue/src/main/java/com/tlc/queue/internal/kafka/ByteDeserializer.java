package com.tlc.queue.internal.kafka;

import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

/**
 * @author Abishek
 * @version 1.0
 */
public class ByteDeserializer implements Deserializer<byte[]>
{
	@Override
	public void configure(Map<String , ?> paramMap , boolean paramBoolean)
	{
	}

	@Override
	public byte[] deserialize(String paramString , byte[] paramArrayOfByte)
	{
		return paramArrayOfByte;
	}

	@Override
	public void close()
	{		
	}
}
