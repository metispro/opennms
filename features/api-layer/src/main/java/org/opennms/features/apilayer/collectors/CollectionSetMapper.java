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

package org.opennms.features.apilayer.collectors;

import java.util.Date;
import java.util.List;

import org.opennms.integration.api.v1.collectors.CollectionSet;
import org.opennms.integration.api.v1.collectors.resource.AttributeBuilder;
import org.opennms.integration.api.v1.collectors.resource.CollectionSetBuilder;
import org.opennms.integration.api.v1.collectors.resource.CollectionSetResource;
import org.opennms.integration.api.v1.collectors.resource.CollectionSetResourceBuilder;
import org.opennms.integration.api.v1.collectors.resource.GenericTypeResource;
import org.opennms.integration.api.v1.collectors.resource.IpInterfaceResource;
import org.opennms.integration.api.v1.collectors.resource.NodeResource;
import org.opennms.integration.api.v1.collectors.resource.NumericAttribute;
import org.opennms.integration.api.v1.collectors.resource.Resource;
import org.opennms.integration.api.v1.collectors.resource.ResourceBuilder;
import org.opennms.integration.api.v1.collectors.resource.StringAttribute;
import org.opennms.integration.api.v1.dao.NodeDao;
import org.opennms.integration.api.v1.model.Node;
import org.opennms.netmgt.collection.api.AttributeGroup;
import org.opennms.netmgt.collection.api.AttributeType;
import org.opennms.netmgt.collection.api.CollectionAttribute;
import org.opennms.netmgt.collection.api.CollectionResource;
import org.opennms.netmgt.collection.api.CollectionSetVisitor;
import org.opennms.netmgt.collection.support.builder.DeferredGenericTypeResource;
import org.opennms.netmgt.collection.support.builder.InterfaceLevelResource;
import org.opennms.netmgt.collection.support.builder.NodeLevelResource;
import org.opennms.netmgt.model.ResourceTypeUtils;

import com.google.common.base.Enums;

/**
 * Helper class that maps @{@link CollectionSet} from Integration API to @{@link org.opennms.netmgt.collection.api.CollectionSet} and vice versa.
 */
public class CollectionSetMapper {

    private NodeDao nodeDao;

    public CollectionSetMapper(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

    /**
     *  Maps @{@link org.opennms.netmgt.collection.api.CollectionSet} to Integration API @{@link CollectionSet} and builds it.
     */
    @SuppressWarnings("unchecked")
    public CollectionSet buildCollectionSet(CollectionSetBuilder builder, org.opennms.netmgt.collection.api.CollectionSet collectionSet) {

        collectionSet.visit(new CollectionSetVisitor() {
            CollectionSetResourceBuilder resourceBuilder;
            String groupName = null;

            @Override
            public void visitCollectionSet(org.opennms.netmgt.collection.api.CollectionSet set) {

            }

            @Override
            public void visitResource(CollectionResource resource) {
                resourceBuilder = new CollectionSetResourceBuilder();
                if (resource.getResourceTypeName().equals(CollectionResource.RESOURCE_TYPE_NODE)) {
                    resourceBuilder.withResource(buildNodeResource(resource));
                } else if (resource.getResourceTypeName().equals(CollectionResource.RESOURCE_TYPE_IF)) {
                    IpInterfaceResource ipInterfaceResource = new ResourceBuilder()
                            .withInstance(resource.getInstance())
                            .buildIpInterfaceResource(buildNodeResource(resource));
                    resourceBuilder.withResource(ipInterfaceResource);
                } else {
                    GenericTypeResource genericTypeResource = new ResourceBuilder()
                            .withInstance(resource.getInstance())
                            .withType(resource.getResourceTypeName())
                            .buildGenericTypeResource(buildNodeResource(resource));
                    resourceBuilder.withResource(genericTypeResource);
                }
            }

            @Override
            public void visitGroup(AttributeGroup group) {
                groupName = group.getName();
            }

            @Override
            public void visitAttribute(CollectionAttribute attribute) {
                if (attribute.getType().equals(AttributeType.STRING)) {
                    StringAttribute stringAttribute = new AttributeBuilder()
                            .withName(attribute.getName())
                            .withStringValue(attribute.getStringValue())
                            .buildString();
                    resourceBuilder.withStringAttribute(stringAttribute);
                } else {
                    NumericAttribute numericAttribute = new AttributeBuilder()
                            .withName(attribute.getName())
                            .withGroup(groupName)
                            .withType((attribute.getType() == AttributeType.COUNTER) ? NumericAttribute.Type.COUNTER : NumericAttribute.Type.GAUGE)
                            .withNumericValue(attribute.getNumericValue().doubleValue())
                            .buildNumeric();
                    resourceBuilder.withNumericAttribute(numericAttribute);
                }
            }

            @Override
            public void completeAttribute(CollectionAttribute attribute) {

            }

            @Override
            public void completeGroup(AttributeGroup group) {

            }

            @Override
            public void completeResource(CollectionResource resource) {
                builder.withCollectionSetResource(resourceBuilder.build());
            }

            @Override
            public void completeCollectionSet(org.opennms.netmgt.collection.api.CollectionSet set) {
                builder.withTimeStamp(set.getCollectionTimestamp().getTime());
                builder.withStatus(CollectionSet.Status.SUCCEEDED);
            }
        });
        return builder.build();
    }

    private NodeResource buildNodeResource(CollectionResource collectionResource) {
        String nodeCriteria = getNodeCriteriaFromResource(collectionResource);
        Node node = nodeDao.getNodeByCriteria(nodeCriteria);
        NodeResource nodeResource = new ResourceBuilder()
                .withNodeId(node.getId())
                .withForeignId(node.getForeignId())
                .withForeignSource(node.getForeignSource())
                .withNodeLabel(node.getLabel())
                .buildNodeResource();
        return nodeResource;
    }

    private String getNodeCriteriaFromResource(CollectionResource resource) {
        String nodeCriteria = null;
        if (resource.getParent() != null) {
            String[] resourcePathArray = resource.getParent().elements();
            if (ResourceTypeUtils.FOREIGN_SOURCE_DIRECTORY.equals(resourcePathArray[0])
                    && resourcePathArray.length == 3) {
                // parent denotes nodeCriteria, form fs:fid
                nodeCriteria = resourcePathArray[1] + ":" + resourcePathArray[2];
            } else if (checkNumeric(resourcePathArray[0])) {
                // parent denotes nodeId
                nodeCriteria = resourcePathArray[0];
            }
        }
        return nodeCriteria;
    }

    private boolean checkNumeric(String nodeCriteria) {
        try {
            Integer.parseInt(nodeCriteria);
            return true;
        } catch (NumberFormatException e) {
            // not a number
            return false;
        }
    }

    /**
     * Maps @{@link CollectionSet} from Integration API to @{@link org.opennms.netmgt.collection.api.CollectionSet} and builds it.
     */
    public static org.opennms.netmgt.collection.api.CollectionSet buildCollectionSet(org.opennms.netmgt.collection.support.builder.CollectionSetBuilder builder, CollectionSet collectionSet) {
        for (CollectionSetResource collectionSetResource : collectionSet.getCollectionSetResources()) {
            Resource resource = collectionSetResource.getResource();
            if (resource == null) {
                continue;
            }
            if (resource.getResourceType().equals(Resource.Type.NODE)) {
                NodeResource nodeResource = (NodeResource) resource;
                NodeLevelResource nodeLevelResource = new NodeLevelResource(nodeResource.getNodeId());
                addAttributes(collectionSetResource, builder, nodeLevelResource);
            } else if (resource.getResourceType().equals(Resource.Type.INTERFACE)) {
                IpInterfaceResource ipResource = (IpInterfaceResource) resource;
                NodeLevelResource nodeLevelResource = new NodeLevelResource(ipResource.getNodeResource().getNodeId());
                InterfaceLevelResource interfaceLevelResource = new InterfaceLevelResource(nodeLevelResource, ipResource.getInstance());
                addAttributes(collectionSetResource, builder, interfaceLevelResource);
            } else if (resource.getResourceType().equals(Resource.Type.GENERIC)) {
                GenericTypeResource genericTypeResource = (GenericTypeResource) resource;
                NodeLevelResource nodeLevelResource = new NodeLevelResource(genericTypeResource.getNodeResource().getNodeId());
                DeferredGenericTypeResource deferredGenericTypeResource = new DeferredGenericTypeResource(nodeLevelResource, genericTypeResource.getType(), genericTypeResource.getInstance());
                addAttributes(collectionSetResource, builder, deferredGenericTypeResource);
            }
        }
        builder.withTimestamp(new Date(collectionSet.getTimeStamp()));
        return builder.build();
    }

    @SuppressWarnings({ "unchecked" })
    public static void  addAttributes(CollectionSetResource collectionSetResource, org.opennms.netmgt.collection.support.builder.CollectionSetBuilder builder, org.opennms.netmgt.collection.support.builder.Resource resource) {
        List<NumericAttribute> numericAttributes = collectionSetResource.getNumericAttributes();
        for (NumericAttribute numericAttribute : numericAttributes) {
            AttributeType attributeType = Enums.getIfPresent(AttributeType.class, numericAttribute.getType().name()).or(AttributeType.GAUGE);
            builder.withNumericAttribute(resource, numericAttribute.getGroup(), numericAttribute.getName(), numericAttribute.getValue(), attributeType);
        }
        List<StringAttribute> stringAttributes = collectionSetResource.getStringAttributes();
        for (StringAttribute stringAttribute : stringAttributes) {
            builder.withStringAttribute(resource, stringAttribute.getGroup(), stringAttribute.getName(), stringAttribute.getValue());
        }
    }
}
