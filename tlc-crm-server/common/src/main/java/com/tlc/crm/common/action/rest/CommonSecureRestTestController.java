package com.tlc.crm.common.action.rest;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.common.action.rest.secure.AbstractSecureRestAction;

import com.tlc.web.HttpMethod;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;
import com.tlc.web.undertow.rest.annotation.ApiRoute;

// TODO Sample class to test the rest standard API

@RestApiHandler(path = "/entity")
public class CommonSecureRestTestController extends AbstractSecureRestAction {

    @ApiRoute(path = "/{id}", method = HttpMethod.GET)
    public void get(final CrmRequest request, final CrmResponse response) {

    }

    @ApiRoute(path = "/{id}", method = HttpMethod.PUT)
    public void put(final CrmRequest request, final CrmResponse response) {

    }

    @ApiRoute(path = "/{page}/{size}", method = HttpMethod.GET)
    public void getAll(final CrmRequest request, final CrmResponse response) {

    }

    @ApiRoute(method = HttpMethod.POST)
    public void post(final CrmRequest request, final CrmResponse response) {

    }

    @ApiRoute(path = "/{id}", method = HttpMethod.PATCH)
    public void patch(final CrmRequest request, final CrmResponse response) {

    }

    @ApiRoute(path = "/{id}", method = HttpMethod.DELETE)
    public void delete(final CrmRequest request, final CrmResponse response) {

    }

    @ApiRoute(method = HttpMethod.OPTIONS)
    public void options(final CrmRequest request, final CrmResponse response) {

    }
}
