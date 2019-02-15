/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.topologies.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opennms.netmgt.topologies.service.api.OnmsTopologyConsumer;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyEdge;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyException;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyMessage;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyPort;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyProtocol;
import org.opennms.netmgt.topologies.service.api.OnmsTopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnmsTopologyLogger implements OnmsTopologyConsumer {
    
    private final static Logger LOG = LoggerFactory.getLogger(OnmsTopologyLogger.class);

    private final OnmsTopologyProtocol m_protocol;
    private List<OnmsTopologyMessage> m_queue = new ArrayList<>();
    
    public OnmsTopologyLogger(String protocol) throws OnmsTopologyException {
        m_protocol =OnmsTopologyProtocol.create(protocol);
    }

    @Override
    public String getName() {
        return m_protocol.getId()+":Consumer:Logger";
    }

    @Override
    public Set<OnmsTopologyProtocol> getProtocols() {
        Set<OnmsTopologyProtocol> protocols = new HashSet<>();
        protocols.add(m_protocol);
        return protocols;
    }

    @Override
    public void consume(OnmsTopologyMessage message) {
        m_queue.add(message);
        StringBuffer txt = new StringBuffer();
        txt.append(getName());
        txt.append("-");
        txt.append(message.getMessagestatus());
        txt.append("-");
        txt.append(message.getMessagebody().getId());
        if (message.getMessagebody() instanceof OnmsTopologyVertex) {
            txt.append(":");
            txt.append(((OnmsTopologyVertex)message.getMessagebody()).getLabel());
        } else if (message.getMessagebody() instanceof OnmsTopologyEdge) {
            txt.append(":");
            OnmsTopologyEdge edge = (OnmsTopologyEdge)message.getMessagebody();
            OnmsTopologyPort source = edge.getSource();
            txt.append(source.getVertex().getId());
            txt.append(":");
            txt.append(source.getIfname());
            txt.append("|");
            OnmsTopologyPort target = edge.getTarget();
            txt.append(target.getVertex().getId());
            txt.append(":");
            txt.append(target.getIfname());
        }
        LOG.debug(txt.toString());
    }

    public List<OnmsTopologyMessage> getQueue() {
        synchronized (m_queue) {
            return new ArrayList<>(m_queue);            
        }
    }

    public OnmsTopologyProtocol getProtocol() {
        return m_protocol;
    }

}
