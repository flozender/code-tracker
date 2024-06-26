/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.monitor.network;

import org.elasticsearch.util.component.AbstractComponent;
import org.elasticsearch.util.inject.Inject;
import org.elasticsearch.util.settings.Settings;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

/**
 * @author kimchy (shay.banon)
 */
public class NetworkService extends AbstractComponent {

    private final NetworkProbe probe;

    private final NetworkInfo info;

    @Inject public NetworkService(Settings settings, NetworkProbe probe) {
        super(settings);
        this.probe = probe;

        this.info = probe.networkInfo();

        if (logger.isDebugEnabled()) {
            StringBuilder netDebug = new StringBuilder("net_info");
            try {
                Enumeration<NetworkInterface> enum_ = NetworkInterface.getNetworkInterfaces();
                String hostName = InetAddress.getLocalHost().getHostName();
                netDebug.append("\nhost [").append(hostName).append("]\n");
                while (enum_.hasMoreElements()) {
                    NetworkInterface net = enum_.nextElement();

                    netDebug.append(net.getName()).append('\t').append("display_name [").append(net.getDisplayName()).append("]\n");
                    Enumeration<InetAddress> addresses = net.getInetAddresses();
                    netDebug.append("\t\taddress ");
                    while (addresses.hasMoreElements()) {
                        netDebug.append("[").append(addresses.nextElement()).append("] ");
                    }
                    netDebug.append('\n');
                    netDebug.append("\t\tmtu [").append(net.getMTU()).append("] multicast [").append(net.supportsMulticast()).append("] ptp [").append(net.isPointToPoint())
                            .append("] loopback [").append(net.isLoopback()).append("] up [").append(net.isUp()).append("] virtual [").append(net.isVirtual()).append("]")
                            .append('\n');
                }
            } catch (Exception ex) {
                netDebug.append("Failed to get Network Interface Info [" + ex.getMessage() + "]");
            }
            logger.debug(netDebug.toString());
        }

        if (logger.isTraceEnabled()) {
            logger.trace("ifconfig\n\n" + ifconfig());
        }
    }

    public NetworkInfo info() {
        return this.info;
    }

    public NetworkStats stats() {
        return probe.networkStats();
    }

    public String ifconfig() {
        return probe.ifconfig();
    }
}
