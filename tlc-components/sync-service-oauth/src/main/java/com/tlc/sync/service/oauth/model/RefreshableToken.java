package com.tlc.sync.service.oauth.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * A model which represents the refreshable token
 *
 * @author Maharaja S
 * @version 1.0
 */
@Getter
@Setter
@RequiredArgsConstructor
public class RefreshableToken {

    @Setter(AccessLevel.NONE)
    private final SyncServiceCategory syncServiceCategory;

    private Long id;
    private String token;
    private Long nextRefreshedAt;
}