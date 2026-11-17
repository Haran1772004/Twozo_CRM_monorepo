package com.tlc.sql.common.api.meta;

import lombok.Getter;

import java.util.Objects;
import java.util.Set;

/**
 * @author Abishek
 * @version 1.0
 */
@Getter
public class UKDefinition
{
	private final Set<String> columns;
	private final String name;
	
	public UKDefinition(String name, Set<String> columns)
	{
		this.name = Objects.requireNonNull(name);
		this.columns = Set.copyOf(columns);
	}

    @Override
	public String toString()
	{
		return name + "=" + columns;
	}
}
