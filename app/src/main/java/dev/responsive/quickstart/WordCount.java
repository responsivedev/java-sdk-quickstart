package dev.responsive.quickstart;

import dev.responsive.kafka.api.config.ResponsiveConfig;
import dev.responsive.kafka.api.config.StorageBackend;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;

public class WordCount {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "responsive-word-count");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // these configs make for snappier demos
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10_000);
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1_000);
        props.put(ResponsiveConfig.STORE_FLUSH_RECORDS_TRIGGER_CONFIG, 3);

        props.put(ResponsiveConfig.RESPONSIVE_LICENSE_CONFIG, "<YOUR_LICENSE>");
        props.put(ResponsiveConfig.STORAGE_BACKEND_TYPE_CONFIG, StorageBackend.MONGO_DB.name());
        props.put(ResponsiveConfig.MONGO_CONNECTION_STRING_CONFIG, "mongodb://localhost:27017");

        final StreamsBuilder builder = new StreamsBuilder();

        final KStream<String, String> source = builder.stream("words");
        source.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
            .groupBy((key, value) -> value)
            .count(Materialized.as("counts-store"))
            .toStream()
            .to("output", Produced.with(Serdes.String(), Serdes.Long()));

        final Topology topology = builder.build();

        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

}
