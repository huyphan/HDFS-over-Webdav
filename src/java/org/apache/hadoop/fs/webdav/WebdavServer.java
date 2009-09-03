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

import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.Server;

public class WebdavServer {

    private static final Log LOG = LogFactory.getLog(WebdavServer.class);

    public static String WEB_APP_CONTEXT = "webAppContext";

    private Server webServer;

    public WebdavServer(String bindAddress, int port) throws Exception {
        LOG.info("Initializing webdav server");

        webServer = new Server();
        webServer.configure("conf/jetty.xml");

        SocketListener listener = new SocketListener();
        listener.setPort(port);
        listener.setHost(bindAddress);
        webServer.addListener(listener);
    }

    public void start() throws Exception {
        webServer.start();
    }

    public static void main(String[] args) throws Exception {
        String usage = "WebdavServer";
        String header = "Run a webdav interface to a hadoop filesystem.";
        Options options = new Options();
        options.addOption("l", "listen", true, "address to listen to");
        options.addOption("p", "port", true, "port to bind to");
        options.addOption("n", "fs", true, "value for fs.default.name (eg. namenode:port)");
        options.addOption("h", "help", false, "print usage information");
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(usage, header, options, "");
            return;
        }
        int port = Integer.parseInt(cmd.getOptionValue("port", "19800"));

        // we use this cheesy way of passing the configuration to the WebdavServlet because
        //  jetty-5 doesn't have a way to send it in the WebdavServlet constructor
        Configuration config = new Configuration();
        String fsDefaultName = cmd.getOptionValue("fs", null);
        if (fsDefaultName != null) {
            config.set("fs.default.name", fsDefaultName);
        }
        WebdavServlet.setConf(config);

        WebdavServer server = new WebdavServer(cmd.getOptionValue("l", "0.0.0.0"), port);
        LOG.info("Starting webdav server");
        server.start();
    }

}
