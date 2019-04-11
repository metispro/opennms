/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2019 The OpenNMS Group, Inc.
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

package org.opennms.core.tracing.api;

import io.opentracing.Tracer;

/**
 * This registry will fetch the tracer from any @{@link TracerWrapper} registered.
 * If no @{@link TracerWrapper} is registered, it will get @{@link io.opentracing.noop.NoopTracer} from
 * {@link io.opentracing.util.GlobalTracer}
 */
public interface TracerRegistry {

    /**
     *
     * @param serviceName Service that should be associated for tracing.
     * @return Tracer that is registered or by default NoopTracer.
     */
    Tracer getTracer(String serviceName);

    /**
     * This will allow clients to fetch any new tracer that is registered after initialization.
     * @return true if any tracer is registered.
     */
    boolean isRegistered();
}
