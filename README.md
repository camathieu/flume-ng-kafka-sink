flume-ng-kafka-sink
===================

Apache FlumeNG sink to push events to Kafka 0.8+

topic selection :
 - statically via sink configuration
 - dynamically via event header "topic"

partition key :
 - from event header "key"

By default ( no "key" header ) event are spread
in a round robin fashion across topic's partitions.

With the default kafka partitioner event with the same key
will always go to the same partition and therefore to the same consumer.
you can override this behaviour by providing a partitioner.class to
the kafka server configuration

Build/Install
-------------

Build flume-ng-kafka-sink 

    mvn package

Configuration
-------------

    agent.sources   = httpSource
    agent.channels  = memoryChannel
    agent.sinks     = kafkaSink

    # Source configuration
    agent.sources.httpSource.type = http
    agent.sources.httpSource.channels = memoryChannel
    agent.sources.httpSource.port = 8026
    agent.sources.httpSource.handler = org.apache.flume.source.http.JSONHandler

    # Sink configuration
    agent.sinks.kafkaSink.type = org.apache.flume.sink.kafka.KafkaSink
    agent.sinks.kafkaSink.channel = memoryChannel
        # If omitted topic must be provided by event header topic
    agent.sinks.kafkaSink.topic = flume
    agent.sinks.kafkaSink.batch.size = 100
        # Producer configuration
    agent.sinks.kafkaSink.metadata.broker.list = localhost:9092
    agent.sinks.kafkaSink.serializer.class = kafka.serializer.DefaultEncoder
    agent.sinks.kafkaSink.producer.type = async
    agent.sinks.kafkaSink.batch.num.messages = 100
    agent.sinks.kafkaSink.queue.buffering.max.ms = 100

Test
----
    curl -X POST -d "[{"headers":{"topic":"test","key":"key"},"body":'{"foo":"bar"}'}]" http://localhost:8026

Reporting
---------

This sink follow the standard flume reporting interface

To enable json reporting you may start flume with the following options
    
    -Dflume.monitoring.type=http -Dflume.monitoring.port=34545

Just get /metrics to get them
    
    curl http://localhost:34545/metrics 2> /dev/null | python -mjson.tool
    {
        "CHANNEL.memoryChannel": {
            "ChannelCapacity": "10000",
            "ChannelFillPercentage": "0.0",
            "ChannelSize": "0",
            "EventPutAttemptCount": "5783",
            "EventPutSuccessCount": "5783",
            "EventTakeAttemptCount": "5805",
            "EventTakeSuccessCount": "5783",
            "StartTime": "1404126896721",
            "StopTime": "0",
            "Type": "CHANNEL"
        },
        "SINK.kafkaSink": {
            "BatchCompleteCount": "57",
            "BatchEmptyCount": "21",
            "BatchUnderflowCount": "1",
            "ConnectionClosedCount": "0",
            "ConnectionCreatedCount": "0",
            "ConnectionFailedCount": "0",
            "EventDrainAttemptCount": "5783",
            "EventDrainSuccessCount": "5783",
            "StartTime": "1404126896722",
            "StopTime": "0",
            "Type": "SINK"
        },
        "SOURCE.httpSource": {
            "AppendAcceptedCount": "0",
            "AppendBatchAcceptedCount": "5783",
            "AppendBatchReceivedCount": "5783",
            "AppendReceivedCount": "0",
            "EventAcceptedCount": "5783",
            "EventReceivedCount": "5783",
            "OpenConnectionCount": "0",
            "StartTime": "1404126896808",
            "StopTime": "0",
            "Type": "SOURCE"
        }
    }
