/**
 * redpen: a text inspection tool
 * Copyright (c) 2014-2015 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.redpen.server.api;

import cc.redpen.RedPen;
import cc.redpen.RedPenException;
import cc.redpen.config.ValidatorConfiguration;
import cc.redpen.parser.DocumentParser;
import org.apache.wink.common.annotations.Workspace;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Resource to get and set RedPen configuration options.
 */
@Workspace(workspaceTitle = "RedPen", collectionTitle = "Configuration")
@Path("/config")
public class RedPenConfigurationResource {

    private static final Logger LOG = LoggerFactory.getLogger(RedPenConfigurationResource.class);

    @Context
    private ServletContext context;

    @Path("/redpens")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @WinkAPIDescriber.Description("Return the configuration for available redpens matching the supplied language (default is any language)")
    public Response getRedPens(@QueryParam("lang") @DefaultValue("") String lang) throws RedPenException {

        JSONObject response = new JSONObject();

        // add the known document formats
        try {
            response.put("documentParsers", DocumentParser.PARSER_MAP.keySet());

            // add matching configurations
            Map<String, RedPen> redpens = new RedPenService(context).getRedPens();
            final JSONObject redpensJSON = new JSONObject();
            response.put("redpens", redpensJSON);
            redpens.forEach(new BiConsumer<String, RedPen>() {
                @Override
                public void accept(String configurationName, RedPen redPen) {
                    if ((lang == null) || lang.isEmpty() || redPen.getConfiguration().getLang().contains(lang)) {
                        try {
                            // add specific configuration items
                            JSONObject config = new JSONObject();
                            config.put("lang", redPen.getConfiguration().getLang());
                            config.put("tokenizer", redPen.getConfiguration().getTokenizer().getClass().getName());

                            // add the names of the validators
                            JSONArray validatorConfigs = new JSONArray();
                            for (ValidatorConfiguration validator : redPen.getConfiguration().getValidatorConfigs()) {
                                validatorConfigs.put(validator.getConfigurationName());
                            }
                            config.put("validators", validatorConfigs);

                            redpensJSON.put(configurationName, config);
                        } catch (Exception e) {
                            LOG.error("Exception when rendering RedPen to JSON for configuration " + configurationName, e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("Exception when rendering RedPen to JSON", e);
        }

        return Response.ok().entity(response).build();
    }
}
