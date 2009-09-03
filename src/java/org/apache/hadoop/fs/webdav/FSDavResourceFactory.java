/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.webdav;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UnixUserGroupInformation;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavMethods;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.DavResourceFactory;
import org.apache.jackrabbit.webdav.DavResourceLocator;
import org.apache.jackrabbit.webdav.DavServletRequest;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;

public class FSDavResourceFactory implements DavResourceFactory {

    private static final Log LOG = LogFactory.getLog(FSDavResourceFactory.class);

    private final ResourceConfig resourceConfig;
    private final Configuration conf;

    public FSDavResourceFactory(ResourceConfig resourceConfig,
                                Configuration conf) {
        this.resourceConfig = resourceConfig;
        this.conf = conf;
    }

    public DavResource createResource(DavResourceLocator locator,
                                      DavSession session) throws DavException {

        try {
            return new FSDavResource(this, locator, session, resourceConfig, conf);
        } catch (IOException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    public DavResource createResource(DavResourceLocator locator,
                                      DavServletRequest request,
                                      DavServletResponse response) throws DavException {

        try {
            return new FSDavResource(this,
                                     locator,
                                     request.getDavSession(),
                                     resourceConfig,
                                     conf,
                                     DavMethods.isCreateCollectionRequest(request));
        } catch (IOException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR,
                                   ex.getMessage());
        }
    }
}
