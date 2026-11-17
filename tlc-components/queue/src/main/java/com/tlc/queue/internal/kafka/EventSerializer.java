package com.tlc.queue.internal.kafka;

import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

/**
 * @author Abishek
 * @version 1.0
 */
public class EventSerializer implements Serializer<byte[]>
{
	@Override
	public void configure(Map<String , ?> paramMap , boolean paramBoolean)
	{		
	}

	@Override
	public byte[] serialize(String paramString , byte[] paramT)
	{
		return paramT;
	}

	@Override
	public void close()
	{
		
	}
	
}
