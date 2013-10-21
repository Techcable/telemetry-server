package org.mcstats.cron;

import org.mcstats.MCStats;
import org.mcstats.db.GraphStore;
import org.mcstats.db.MongoDBGraphStore;
import org.mcstats.generator.GeneratedData;
import org.mcstats.generator.aggregator.CountryAggregator;
import org.mcstats.generator.GraphGenerator;
import org.mcstats.generator.aggregator.DecoderAggregator;
import org.mcstats.generator.aggregator.IncrementAggregator;
import org.mcstats.generator.aggregator.ReflectionAggregator;
import org.mcstats.generator.aggregator.ReflectionDonutAggregator;
import org.mcstats.generator.aggregator.VersionDemographicsAggregator;
import org.mcstats.handler.ReportHandler;
import org.mcstats.model.Column;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MainlineGraphs implements Runnable {

    private Logger logger = Logger.getLogger(getClass().getSimpleName());

    private MCStats mcstats;

    /**
     * A list of all generators
     */
    private List<GraphGenerator> generators = new LinkedList<GraphGenerator>();

    public MainlineGraphs(MCStats mcstats) {
        this.mcstats = mcstats;

        // custom data

        // -- auth mode
        // -- game version
        // -- global stats
        // -- > Players
        // -- > Servers
        // -- java version, donut (Reflection2Aggregator)
        // -- operating system, donut
        // rank
        // -- revision
        // -- server locations
        // -- server software
        // -- system arch
        // -- system cores
        // -- version demographics
        // version trends

        generators.add(new IncrementAggregator("Global Statistics", "Servers"));
        generators.add(new ReflectionAggregator("players", "Global Statistics", "Players"));

        /* generators.add(new ReflectionAggregator("serverSoftware", "Server Software"));
        generators.add(new ReflectionAggregator("osarch", "System Arch"));
        generators.add(new ReflectionAggregator("cores", "System Cores"));

        generators.add(new VersionDemographicsAggregator("Version Demographics"));

        generators.add(new CountryAggregator("Server Locations"));

        generators.add(new ReflectionDonutAggregator("osname", "osversion", "Operating System"));
        generators.add(new ReflectionDonutAggregator("java_name", "java_version", "Java Version"));

        generators.add(new DecoderAggregator<Integer>("auth_mode", "Auth Mode", new DecoderAggregator.Decoder<Integer>() {
            public String decode(Integer value) {
                switch (value) {
                    case 0:
                        return "Online";
                    case 1:
                        return "Offline";
                    default:
                        return "Unknown";
                }
            }
        })); */
        /*

        generators.add(new ReflectionAggregator("minecraftVersion", "Game Version"));
        // generators.add(new ReflectionAggregator("revision", "MCStats Revision"));
        generators.add(new ReflectionAggregator("", ""));
        generators.add(new ReflectionAggregator("", ""));

        */
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        logger.info("Beginning graph generation");
        GraphStore store = mcstats.getGraphStore();

        long start = System.currentTimeMillis();

        for (GraphGenerator generator : generators) {
            logger.info("Generating graph for: " + generator);

            Map<Column, GeneratedData> data = generator.generate(mcstats);

            int epoch = ReportHandler.normalizeTime();

            for (Map.Entry<Column, GeneratedData> entry : data.entrySet()) {
                Column column = entry.getKey();
                GeneratedData columnData = entry.getValue();

                store.insert(column, epoch, columnData.getSum(), columnData.getCount(), columnData.getAverage(), columnData.getMax(), columnData.getMin());
            }

            logger.info("Aggregated: " + data);
        }

        ((MongoDBGraphStore) store).finishGeneration();
        logger.info("Finished graph generation in " + (System.currentTimeMillis() - start) + "ms");

    }
}