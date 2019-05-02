/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.flows.rest.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.api.SnmpInterfaceDao;
import org.opennms.netmgt.flows.api.ConversationKey;
import org.opennms.netmgt.flows.api.Directional;
import org.opennms.netmgt.flows.api.FlowRepository;
import org.opennms.netmgt.flows.api.TrafficSummary;
import org.opennms.netmgt.flows.filter.api.ExporterNodeFilter;
import org.opennms.netmgt.flows.filter.api.Filter;
import org.opennms.netmgt.flows.filter.api.NodeCriteria;
import org.opennms.netmgt.flows.filter.api.SnmpInterfaceIdFilter;
import org.opennms.netmgt.flows.filter.api.TimeRangeFilter;
import org.opennms.netmgt.flows.rest.FlowRestService;
import org.opennms.netmgt.flows.rest.model.FlowGraphUrlInfo;
import org.opennms.netmgt.flows.rest.model.FlowNodeDetails;
import org.opennms.netmgt.flows.rest.model.FlowNodeSummary;
import org.opennms.netmgt.flows.rest.model.FlowSeriesColumn;
import org.opennms.netmgt.flows.rest.model.FlowSeriesResponse;
import org.opennms.netmgt.flows.rest.model.FlowSnmpInterface;
import org.opennms.netmgt.flows.rest.model.FlowSummaryResponse;
import org.opennms.netmgt.model.OnmsCategory;
import org.springframework.transaction.support.TransactionOperations;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;

public class FlowRestServiceImpl implements FlowRestService {
    private final FlowRepository flowRepository;
    private final NodeDao nodeDao;
    private final SnmpInterfaceDao snmpInterfaceDao;
    private final TransactionOperations transactionOperations;
    private String flowGraphUrl;

    public FlowRestServiceImpl(FlowRepository flowRepository, NodeDao nodeDao,
                               SnmpInterfaceDao snmpInterfaceDao, TransactionOperations transactionOperations) {
        this.flowRepository = Objects.requireNonNull(flowRepository);
        this.nodeDao = Objects.requireNonNull(nodeDao);
        this.snmpInterfaceDao = Objects.requireNonNull(snmpInterfaceDao);
        this.transactionOperations = Objects.requireNonNull(transactionOperations);
    }

    @Override
    public Long getFlowCount(UriInfo uriInfo) {
        return waitForFuture(flowRepository.getFlowCount(getFiltersFromQueryString(uriInfo.getQueryParameters())));
    }

    @Override
    public List<FlowNodeSummary> getFlowExporters() {
        return transactionOperations.execute(status -> this.nodeDao.findAllHavingFlows().stream())
                .map(n -> new FlowNodeSummary(n.getId(),
                        n.getForeignId(), n.getForeignSource(), n.getLabel(),
                        n.getCategories().stream().map(OnmsCategory::getName).collect(Collectors.toList())))
                .sorted(Comparator.comparingInt(FlowNodeSummary::getId))
                .collect(Collectors.toList());
    }

    @Override
    public FlowNodeDetails getFlowExporter(Integer nodeId) {
        final List<FlowSnmpInterface> ifaces = transactionOperations.execute(status -> this.snmpInterfaceDao.findAllHavingFlows(nodeId)).stream()
                .map(iface -> new FlowSnmpInterface(iface.getIfIndex(),
                            iface.getIfName(),
                            iface.getIfAlias(),
                            iface.getIfDescr()))
                .collect(Collectors.toList());

        return new FlowNodeDetails(nodeId, ifaces);
    }

    @Override
    public List<String> getApplications(String matchingPrefix, long limit, UriInfo uriInfo) {
        final List<Filter> filters = getFiltersFromQueryString(uriInfo.getQueryParameters());
        return waitForFuture(flowRepository.getApplications(matchingPrefix, limit, filters));
    }

    @Override
    public FlowSummaryResponse getApplicationSummary(Integer N, Set<String> applications, boolean includeOther,
                                                     UriInfo uriInfo) {
        return getSummary(N, applications, includeOther, uriInfo, "application",
                flowRepository::getTopNApplicationSummaries, flowRepository::getApplicationSummaries);
    }

    @Override
    public FlowSeriesResponse getApplicationSeries(long step, Integer N, Set<String> applications,
                                                   boolean includeOther, UriInfo uriInfo) {
        return getSeries(step, N, applications, includeOther, uriInfo, "application",
                flowRepository::getTopNApplicationSeries,
                flowRepository::getApplicationSeries);
    }

    @Override
    public List<String> getHosts(String pattern, long limit, UriInfo uriInfo) {
        final List<Filter> filters = getFiltersFromQueryString(uriInfo.getQueryParameters());
        return waitForFuture(flowRepository.getHosts(pattern, limit, filters));
    }

    @Override
    public FlowSummaryResponse getHostSummary(Integer N, Set<String> hosts, boolean includeOther, UriInfo uriInfo) {
        return getSummary(N, hosts, includeOther, uriInfo, "host", flowRepository::getTopNHostSummaries,
                flowRepository::getHostSummaries);
    }

    @Override
    public FlowSeriesResponse getHostSeries(long step, Integer N, Set<String> hosts, boolean includeOther,
                                            UriInfo uriInfo) {
        return getSeries(step, N, hosts, includeOther, uriInfo, "host", flowRepository::getTopNHostSeries,
                flowRepository::getHostSeries);
    }

    @Override
    public FlowSummaryResponse getTopNConversations(int N, UriInfo uriInfo) {
        final List<Filter> filters = getFiltersFromQueryString(uriInfo.getQueryParameters());
        final TimeRangeFilter timeRangeFilter = getRequiredTimeRangeFilter(filters);

        final List<TrafficSummary<ConversationKey>> summary =
                waitForFuture(flowRepository.getTopNConversations(N, filters));

        final FlowSummaryResponse response = new FlowSummaryResponse();
        response.setStart(timeRangeFilter.getStart());
        response.setEnd(timeRangeFilter.getEnd());
        response.setHeaders(Lists.newArrayList("Location", "Protocol", "Source IP",
                "Dest. IP", "Application", "Bytes In", "Bytes Out"));
        response.setRows(summary.stream()
                .map(sum -> {
                    final ConversationKey key = sum.getEntity();
                    return Lists.newArrayList((Object)key.getLocation(), key.getProtocol(),
                            key.getLowerIp(), key.getUpperIp(), key.getApplication(), sum.getBytesIn(), sum.getBytesOut());
                })
                .collect(Collectors.toList()));
        return response;
    }

    @Override
    public FlowSeriesResponse getTopNConversationsSeries(long step, int N, UriInfo uriInfo) {
        final List<Filter> filters = getFiltersFromQueryString(uriInfo.getQueryParameters());
        final TimeRangeFilter timeRangeFilter = getRequiredTimeRangeFilter(filters);
        final Table<Directional<ConversationKey>, Long, Double> series =
                waitForFuture(flowRepository.getTopNConversationsSeries(N, step, filters));

        final FlowSeriesResponse response = new FlowSeriesResponse();
        response.setStart(timeRangeFilter.getStart());
        response.setEnd(timeRangeFilter.getEnd());
        response.setColumns(series.rowKeySet().stream()
                .map(d -> {
                    final ConversationKey key = d.getValue();
                    final String applicationTag = key.getApplication() != null ? String.format(" [%s]", key.getApplication()) : "";
                    final FlowSeriesColumn column = new FlowSeriesColumn();
                    column.setLabel(String.format("%s <-> %s%s", key.getLowerIp(), key.getUpperIp(), applicationTag));
                    column.setIngress(d.isIngress());
                    return column;
                })
                .collect(Collectors.toList()));
        populateResponseFromTable(series, response);
        return response;
    }

    protected static List<Filter> getFiltersFromQueryString(MultivaluedMap<String, String> queryParams) {
        final List<Filter> filters = new ArrayList<>();

        final String start = queryParams.getFirst("start");
        long startMs;
        if (start != null) {
            startMs = Long.parseLong(start);
        } else {
            // 4 hours ago
            startMs = -TimeUnit.HOURS.toMillis(4);
        }

        final String end = queryParams.getFirst("end");
        long endMs;
        if (end != null) {
            endMs = Long.parseLong(end);
        } else {
            // Now
            endMs = System.currentTimeMillis();
        }
        endMs = getEffectiveEnd(endMs);
        startMs = getEffectiveStart(startMs, endMs);
        filters.add(new TimeRangeFilter(startMs, endMs));

        final String ifIndexStr = queryParams.getFirst("ifIndex");
        if (ifIndexStr != null) {
            int ifIndex = Integer.parseInt(ifIndexStr);
            filters.add(new SnmpInterfaceIdFilter(ifIndex));
        }

        final String exporterNodeCriteria = queryParams.getFirst("exporterNode");
        if (exporterNodeCriteria != null) {
            try {
                filters.add(new ExporterNodeFilter(new NodeCriteria(exporterNodeCriteria)));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid node criteria: " + exporterNodeCriteria);
            }
        }

        return filters;
    }

    private static TimeRangeFilter getRequiredTimeRangeFilter(Collection<Filter> filters) {
        final Optional<TimeRangeFilter> filter = filters.stream()
                .filter(f -> f instanceof TimeRangeFilter)
                .map(f -> (TimeRangeFilter)f)
                .findFirst();
        if (!filter.isPresent()) {
            throw new BadRequestException("Time range is required.");
        }
        return filter.get();
    }

    private static long getEffectiveStart(long start, long effectiveEnd) {
        // If start is negative, subtract it from the end
        long effectiveStart = start >= 0 ? start : effectiveEnd + start;
        // Make sure the resulting start time is not negative
        effectiveStart = Math.max(effectiveStart, 0);
        return effectiveStart;
    }

    private static long getEffectiveEnd(long end) {
        // If end is not strictly positive, use the current timestamp
        return end > 0 ? end : new Date().getTime();
    }

    private static <T> T waitForFuture(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException|ExecutionException e) {
            throw new WebApplicationException("Failed to execute query: " + e.getMessage(), e);
        }
    }

    private static void populateResponseFromTable(Table<?, Long, Double> table, FlowSeriesResponse response) {
        final List<Long> timestamps = table.columnKeySet().stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());

        final List<List<Double>> values = new LinkedList<>();
        for (Object rowKey : table.rowKeySet()) {
            final List<Double> column = new ArrayList<>(timestamps.size());
            for (Long ts : timestamps) {
                Double val = table.get(rowKey, ts);
                if (val == null) {
                    val = Double.NaN;
                }
                column.add(val);
            }
            values.add(column);
        }

        response.setTimestamps(timestamps);
        response.setValues(values);
    }

    @Override
    public FlowGraphUrlInfo getFlowGraphUrlInfo(UriInfo uriInfo) {

        if (Strings.isNullOrEmpty(flowGraphUrl)) {
            return null;
        }

        long flowCount = waitForFuture(
                flowRepository.getFlowCount(getFiltersFromQueryString(uriInfo.getQueryParameters())));
        FlowGraphUrlInfo graphUrlInfo = new FlowGraphUrlInfo();

        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String flowUrl = getFlowGraphUrl();
        final String formattedGraphUrl = flowUrl.replaceAll("\\$nodeId", queryParams.getFirst("exporterNode"))
                .replaceAll("\\$ifIndex", queryParams.getFirst("ifIndex"))
                .replaceAll("\\$start", queryParams.getFirst("start"))
                .replaceAll("\\$end", queryParams.getFirst("end"));
        graphUrlInfo.setFlowGraphUrl(formattedGraphUrl);
        graphUrlInfo.setFlowCount(flowCount);
        return graphUrlInfo;
    }

    public String getFlowGraphUrl() {
        return flowGraphUrl;
    }

    public void setFlowGraphUrl(String flowGraphUrl) {
        this.flowGraphUrl = flowGraphUrl;
    }

    private boolean isNullOrEmptyOrContainsNullOrEmpty(Collection<String> collection) {
        return collection == null || collection.isEmpty() || collection.contains(null) || collection.contains("");
    }

    private void validateNOrSetQueryParameters(Integer N, Collection<String> collection, String collectionName) {
        if (N == null && isNullOrEmptyOrContainsNullOrEmpty(collection)) {
            // If neither the top N or the collection are set that is an error
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity(String.format("One of 'N' or '%s' query parameters must be present", collectionName)).build());
        } else if (N != null && !isNullOrEmptyOrContainsNullOrEmpty(collection)) {
            // If both are set that is also an error
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN)
                    .entity(String.format("Only one of 'N' or '%s' query parameters should be set", collectionName)).build());
        }
    }

    private interface TopNSeriesProvider {
        CompletableFuture<Table<Directional<String>, Long, Double>> getSeries(Integer N, long step,
                                                                              boolean includeOther,
                                                                              List<Filter> filters);
    }

    private interface SpecificEntitiesSeriesProvider {
        CompletableFuture<Table<Directional<String>, Long, Double>> getSeries(Set<String> entities, long step,
                                                                              boolean includeOther,
                                                                              List<Filter> filters);
    }

    private interface TopNSummaryProvider {
        CompletableFuture<List<TrafficSummary<String>>> getSummary(Integer N, boolean includeOther,
                                                                   List<Filter> filters);
    }

    private interface SpecificEntitiesSummaryProvider {
        CompletableFuture<List<TrafficSummary<String>>> getSummary(Set<String> entities, boolean includeOther,
                                                                   List<Filter> filters);
    }

    private FlowSummaryResponse getSummary(Integer N, Set<String> entities, boolean includeOther, UriInfo uriInfo,
                                           String entitiesLabel, TopNSummaryProvider topNSummaryProvider,
                                           SpecificEntitiesSummaryProvider specificEntitiesSummaryProvider) {
        validateNOrSetQueryParameters(N, entities, entitiesLabel);

        final List<Filter> filters = getFiltersFromQueryString(uriInfo.getQueryParameters());
        final TimeRangeFilter timeRangeFilter = getRequiredTimeRangeFilter(filters);

        final List<TrafficSummary<String>> summary;

        if (N != null) {
            summary = waitForFuture(topNSummaryProvider.getSummary(N, includeOther, filters));
        } else {
            summary = waitForFuture(specificEntitiesSummaryProvider.getSummary(entities, includeOther, filters));
        }

        final FlowSummaryResponse response = new FlowSummaryResponse();
        response.setStart(timeRangeFilter.getStart());
        response.setEnd(timeRangeFilter.getEnd());
        response.setHeaders(Lists.newArrayList(entitiesLabel, "Bytes In", "Bytes Out"));
        response.setRows(summary.stream()
                .map(sum -> Arrays.asList((Object) sum.getEntity(), sum.getBytesIn(), sum.getBytesOut()))
                .collect(Collectors.toList()));
        return response;
    }

    private FlowSeriesResponse getSeries(long step, Integer N, Set<String> entities, boolean includeOther,
                                         UriInfo uriInfo, String entitiesLabel, TopNSeriesProvider topNSeriesProvider
            , SpecificEntitiesSeriesProvider specificEntitiesSeriesProvider) {
        validateNOrSetQueryParameters(N, entities, entitiesLabel);

        final List<Filter> filters = getFiltersFromQueryString(uriInfo.getQueryParameters());
        final TimeRangeFilter timeRangeFilter = getRequiredTimeRangeFilter(filters);

        final Table<Directional<String>, Long, Double> series;

        if (N != null) {
            series = waitForFuture(topNSeriesProvider.getSeries(N, step, includeOther, filters));
        } else {
            series = waitForFuture(specificEntitiesSeriesProvider.getSeries(entities, step, includeOther, filters));
        }

        final FlowSeriesResponse response = new FlowSeriesResponse();
        response.setStart(timeRangeFilter.getStart());
        response.setEnd(timeRangeFilter.getEnd());
        response.setColumns(series.rowKeySet().stream()
                .map(d -> {
                    final FlowSeriesColumn column = new FlowSeriesColumn();
                    column.setLabel(d.getValue());
                    column.setIngress(d.isIngress());
                    return column;
                })
                .collect(Collectors.toList()));
        populateResponseFromTable(series, response);
        return response;
    }

}
