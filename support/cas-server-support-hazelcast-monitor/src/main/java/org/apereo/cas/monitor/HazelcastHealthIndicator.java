package org.apereo.cas.monitor;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.monitor.LocalMapStats;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * This is {@link HazelcastHealthIndicator}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@ToString
public class HazelcastHealthIndicator extends AbstractCacheHealthIndicator {
    private final String instanceName;
    private final long clusterSize;

    public HazelcastHealthIndicator(final long evictionThreshold, final long threshold,
                                    final String instanceName, final long clusterSize) {
        super(evictionThreshold, threshold);
        this.instanceName = instanceName;
        this.clusterSize = clusterSize;
    }

    @Override
    protected CacheStatistics[] getStatistics() {
        final List<CacheStatistics> statsList = new ArrayList<>();
        LOGGER.debug("Locating hazelcast instance [{}]...", instanceName);
        final HazelcastInstance instance = Hazelcast.getHazelcastInstanceByName(instanceName);
        instance.getConfig().getMapConfigs().keySet().forEach(key -> {
            final IMap map = instance.getMap(key);
            LOGGER.debug("Starting to collect hazelcast statistics for map [{}] identified by key [{}]...", map, key);
            statsList.add(new HazelcastStatistics(map, clusterSize));
        });
        return statsList.toArray(new CacheStatistics[statsList.size()]);
    }

    /**
     * The type Hazelcast statistics.
     */
    public static class HazelcastStatistics implements CacheStatistics {

        private static final int PERCENTAGE_VALUE = 100;

        private final IMap map;
        private final long clusterSize;

        protected HazelcastStatistics(final IMap map, final long clusterSize) {
            this.map = map;
            this.clusterSize = clusterSize;
        }

        @Override
        public long getSize() {
            return this.map.size();
        }

        @Override
        public long getCapacity() {
            return this.map.getLocalMapStats() != null ? this.map.getLocalMapStats().total() : 0;
        }

        @Override
        public long getEvictions() {
            if (this.map.getLocalMapStats() != null && this.map.getLocalMapStats().getNearCacheStats() != null) {
                return this.map.getLocalMapStats().getNearCacheStats().getMisses();
            }
            return 0;
        }

        @Override
        public String getName() {
            return this.map.getName();
        }

        @Override
        public int getPercentFree() {
            final long capacity = getCapacity();
            if (capacity == 0) {
                return 0;
            }
            return (int) ((capacity - getSize()) * PERCENTAGE_VALUE / capacity);
        }

        @Override
        public void toString(final StringBuilder builder) {
            final LocalMapStats localMapStats = map.getLocalMapStats();
            builder.append("Creation time: ").append(localMapStats.getCreationTime()).append(", ")
                .append("Cluster size: ").append(clusterSize).append(", ").append("Owned entry count: ")
                .append(localMapStats.getOwnedEntryCount()).append(", ").append("Backup entry count: ")
                .append(localMapStats.getBackupEntryCount()).append(", ").append("Backup count: ")
                .append(localMapStats.getBackupCount()).append(", ").append("Hits count: ")
                .append(localMapStats.getHits()).append(", ").append("Last update time: ")
                .append(localMapStats.getLastUpdateTime()).append(", ").append("Last access time: ")
                .append(localMapStats.getLastAccessTime()).append(", ").append("Locked entry count: ")
                .append(localMapStats.getLockedEntryCount()).append(", ").append("Dirty entry count: ")
                .append(localMapStats.getDirtyEntryCount()).append(", ").append("Total get latency: ")
                .append(localMapStats.getMaxGetLatency()).append(", ").append("Total put latency: ")
                .append(localMapStats.getTotalPutLatency()).append(", ").append("Total remove latency: ")
                .append(localMapStats.getTotalRemoveLatency()).append(", ").append("Heap cost: ")
                .append(localMapStats.getHeapCost());
            if (localMapStats.getNearCacheStats() != null) {
                builder.append(", Misses: ").append(localMapStats.getNearCacheStats().getMisses());
            }
        }
    }
}
