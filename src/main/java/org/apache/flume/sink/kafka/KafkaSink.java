package org.apache.flume.sink.kafka;

import java.util.Properties;
import java.util.Map;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.flume.Channel;
import org.apache.flume.ChannelException;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.apache.flume.instrumentation.SinkCounter;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class KafkaSink extends AbstractSink implements Configurable {
	private static final Logger log = LoggerFactory.getLogger(KafkaSink.class);
	private Properties props;
	private SinkCounter sinkCounter;
	private Producer<byte[], byte[]> producer;

	@Override
	public Status process() throws EventDeliveryException {
		Channel channel = getChannel();
		Transaction tx = channel.getTransaction();
		String topic = props.getProperty("topic");
		int maxBatchSize = Integer.parseInt(props.getProperty("batch.size",	"100"));

		Status status = Status.READY;
		try {
			tx.begin();

			List<KeyedMessage<byte[], byte[]>> batch = Lists.newLinkedList();

			for (int i = 0; i < maxBatchSize; i++) {
				Event event = channel.take();
				if (event == null)
					break;
				String key = event.getHeaders().get("key");
				if (key == null) {
					batch.add(
                        new KeyedMessage<byte[], byte[]>(topic, event.getBody())
                    );
				} else {
					batch.add(
                        new KeyedMessage<byte[], byte[]>(topic, key.getBytes(), event.getBody())
                    );
				}
			}

			int batchSize = batch.size();

			if (batchSize == 0) {
				sinkCounter.incrementBatchEmptyCount();
				status = Status.BACKOFF;
			} else {
				if (batchSize < maxBatchSize) {
					sinkCounter.incrementBatchUnderflowCount();
				} else {
					sinkCounter.incrementBatchCompleteCount();
				}
				sinkCounter.addToEventDrainAttemptCount(batchSize);
				producer.send(batch);
			}
			tx.commit();
			sinkCounter.addToEventDrainSuccessCount(batchSize);
		} catch (Throwable t) {
			tx.rollback();
			if (t instanceof Error) {
				throw (Error) t;
			} else if (t instanceof ChannelException) {
				log.error(getName() + " " + this
						+ "Unable to get event from channel "
						+ channel.getName() + ". Exception folows." + t);
				status = Status.BACKOFF;
			} else {
				throw new EventDeliveryException("Failed to send events", t);
			}
		} finally {
			tx.close();
		}

		return status;
	}

	@Override
	public void configure(final Context context) {

		Preconditions.checkState(context.getString("topic") != null,
				"The parameter topic must be specified");

		if (sinkCounter == null) {
			sinkCounter = new SinkCounter(getName());
		}

		props = new Properties();
		Map<String, String> contextMap = context.getParameters();
		for (String key : contextMap.keySet()) {
			if (!key.equals("type") && 
                !key.equals("channel") && 
                !key.equals("topic") && 
                !key.equals("batch.size")) {

				props.setProperty(key, context.getString(key));
				log.info("key={},value={}", key, context.getString(key));
			}
		}
		ProducerConfig config = new ProducerConfig(props);
		producer = new Producer<byte[], byte[]>(config);
	}

	@Override
	public synchronized void start() {
		log.info("Starting {} {}", this, getName());
		sinkCounter.start();
		super.start();
		log.info("Started {} {}", this, getName());
	}

	@Override
	public synchronized void stop() {
		log.info("Stoping {} {}", this, getName());
		sinkCounter.stop();
		super.stop();
		log.info("Stopped {} {}", this, getName());
	}
}
