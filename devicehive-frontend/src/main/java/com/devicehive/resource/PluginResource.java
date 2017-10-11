package com.devicehive.resource;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Constants;
import com.devicehive.model.updates.PluginUpdate;
import com.devicehive.vo.PluginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Api(tags = {"Plugin"}, description = "Plugin management operations", consumes = "application/json")
@Path("/plugin")
@Produces({"application/json"})
public interface PluginResource {

    @POST
    @Path("/register")
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_PLUGIN')")
    @ApiOperation(value = "Register Plugin", notes = "Registers plugin in DH Server")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Authorization token", required = true, dataType = "string", paramType = "header")
    })
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "Returns plugin uuid, topic name and health check period",
                    response = PluginVO.class),
            @ApiResponse(code = 400, message = "healthCheckUrl doesn't respond")
    })
    Response register(
            @ApiParam(name = "waitTimeout", value = "Wait timeout")
            @DefaultValue(Constants.DEFAULT_WAIT_TIMEOUT)
            @Min(value = Constants.MIN_WAIT_TIMEOUT, message = "Timeout can't be less than " + Constants.MIN_WAIT_TIMEOUT + " seconds. ")
            @Max(value = Constants.MAX_WAIT_TIMEOUT, message = "Timeout can't be more than " + Constants.MAX_WAIT_TIMEOUT + " seconds. ")
            @QueryParam("waitTimeout")
            long timeout,
            @ApiParam(name = "deviceIds", value = "Device ids")
            @QueryParam("deviceIds")
                    String deviceIdsString,
            @ApiParam(name = "networkIds", value = "Network ids")
            @QueryParam("networkIds")
                    String networkIdsString,
            @ApiParam(name = "pollType", value = "Polls for commands/notifications.", allowableValues = "Command,Notification",
                    defaultValue = "Command")
            @QueryParam("pollType")
                    String pollType,
            @ApiParam(name = "names", value = "Command/Notification names")
            @QueryParam("names")
                    String namesString,
            @ApiParam(name = "timestamp", value = "Timestamp to start from")
            @QueryParam("timestamp")
                    String timestamp,
            @ApiParam(value = "Filter body", defaultValue = "{}", required = true)
                    PluginUpdate filterToCreate);
    
}
