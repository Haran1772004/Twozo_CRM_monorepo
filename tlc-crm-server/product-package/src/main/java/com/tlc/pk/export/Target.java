package com.tlc.pk.export;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor( access = AccessLevel.PRIVATE )
public enum Target
{
    EMAIL( 1 ),
    DOWNLOAD( 2 );

    private final int target;
}