/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.utility.ProfileUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;

import javax.annotation.PostConstruct;

/**
 *	@author vincenzoingenito
 *
 *	Kafka topic configuration.
 */
@Data
@Component
public class KafkaTopicCFG {

	@Autowired
	private ProfileUtility profileUtility;

	/**
	 * Dispatcher indexer low priority Topic.
	 */
	@Value("${kafka.dispatcher-indexer.topic.low-priority}")
	private String dispatcherIndexerLowPriorityTopic;

	/**
	 * Dispatcher indexer medium priority Topic.
	 */
	@Value("${kafka.dispatcher-indexer.topic.medium-priority}")
	private String dispatcherIndexerMediumPriorityTopic;

	/**
	 * Dispatcher indexer high priority Topic.
	 */
	@Value("${kafka.dispatcher-indexer.topic.high-priority}")
	private String dispatcherIndexerHighPriorityTopic;

	/**
	 * Dispatcher indexer Dead letter Topic. 
	 */
	@Value("${kafka.dispatcher-indexer.deadletter.topic}")
	private String dispatcherIndexerDeadLetterTopic;
	
	/**
	 * Indexer publisher low priority Topic.
	 */
	@Value("${kafka.indexer-publisher.topic}")
	private String indexerPublisherTopic;
	
	/**
	 * Log publisher.
	 */
	@Value("${kafka.log.base-topic}")
	private String logPublisherTopic;
	
	/**
	 * Indexer status manager Topic.
	 */
	@Value("${kafka.statusmanager.topic}")
	private String statusManagerTopic;

	@PostConstruct
	public void afterInit() {
		if (profileUtility.isTestProfile()) {
			this.dispatcherIndexerLowPriorityTopic = Constants.Profile.TEST_PREFIX + this.dispatcherIndexerLowPriorityTopic;
			this.dispatcherIndexerMediumPriorityTopic = Constants.Profile.TEST_PREFIX + this.dispatcherIndexerMediumPriorityTopic;
			this.dispatcherIndexerHighPriorityTopic = Constants.Profile.TEST_PREFIX + this.dispatcherIndexerHighPriorityTopic;
			this.dispatcherIndexerDeadLetterTopic = Constants.Profile.TEST_PREFIX + this.dispatcherIndexerDeadLetterTopic;
			this.indexerPublisherTopic = Constants.Profile.TEST_PREFIX + this.indexerPublisherTopic;
			this.statusManagerTopic = Constants.Profile.TEST_PREFIX + this.statusManagerTopic;
		}
	}

}
