flume-ng-kafka-sink
===================

Apache FlumeNG sink to push events to Kafka 0.8+

Build/Install
-------------

Get or build kafka 0.8 jar ( http://mvnrepository.com/artifact/org.apache.kafka/kafka_2.10/0.8.0 )

Build flume-ng-kafka-sink 

    mvn package

Add both jar to flume classpath ( flume-ng/lib )

Configuration
-------------

    # Sink configuration
    agent.sinks.kafkaSink.type = org.apache.flume.sink.kafka.KafkaSink
    agent.sinks.kafkaSink.channel = memoryChannel
    agent.sinks.kafkaSink.topic = flume
    agent.sinks.kafkaSink.batch.size = 100
    # Producer configuration
    agent.sinks.kafkaSink.metadata.broker.list = localhost:9092
    agent.sinks.kafkaSink.serializer.class = kafka.serializer.DefaultEncoder
    agent.sinks.kafkaSink.producer.type = async
    agent.sinks.kafkaSink.batch.num.messages = 100
    agent.sinks.kafkaSink.queue.buffering.max.ms = 100
