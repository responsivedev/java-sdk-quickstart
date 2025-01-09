package dev.responsive.quickstart;

import dev.responsive.kafka.api.ResponsiveKafkaStreams;
import dev.responsive.kafka.api.config.ResponsiveConfig;
import dev.responsive.kafka.api.config.StorageBackend;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaClientSupplier;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cassandra.CassandraAutoConfiguration;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;

@Configuration
@EnableKafka
@EnableKafkaStreams
@SpringBootApplication(exclude = {CassandraDataAutoConfiguration.class, CassandraAutoConfiguration.class})
public class WordCountSpringBoot {

  public static void main(String[] args) {
    SpringApplication.run(WordCountSpringBoot.class, args);
  }

  @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
  public KafkaStreamsConfiguration streamsConfigs() {
    Map<String, Object> props = new HashMap<>();

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
    props.put(ResponsiveConfig.MONGO_ENDPOINT_CONFIG, "mongodb://localhost:27017");

    return new KafkaStreamsConfiguration(props);
  }

  @Bean
  public StreamsBuilderFactoryBeanConfigurer configure() {
    return factoryBean -> {
      factoryBean.setKafkaStreamsCustomizer(new ResponsiveStreamsCustomizer());
    };
  }

  @Bean
  public KStream<String, Long> buildWordCountTopology(final StreamsBuilder builder) {
    final KStream<String, String> source = builder.stream("words");
    final KStream<String, Long> output =
        source.flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
        .groupBy((key, value) -> value)
        .count(Materialized.as("counts-store"))
        .toStream();
    output.to("output", Produced.with(Serdes.String(), Serdes.Long()));
    return output;
  }

  public static class ResponsiveStreamsCustomizer implements KafkaStreamsCustomizer {

    @Override
    public KafkaStreams initKafkaStreams(
        Topology topology,
        Properties properties,
        KafkaClientSupplier clientSupplier
    ) {
      return new ResponsiveKafkaStreams(topology, properties, clientSupplier);
    }

    @Override
    public void customize(final KafkaStreams kafkaStreams) {
      // do nothing here
    }
  }

}
