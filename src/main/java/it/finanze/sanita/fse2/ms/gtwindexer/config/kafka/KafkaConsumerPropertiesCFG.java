/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * 
 * Copyright (C) 2023 Ministero della Salute
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package it.finanze.sanita.fse2.ms.gtwindexer.config.kafka;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.finanze.sanita.fse2.ms.gtwindexer.enums.EventStatusEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 *
 *	Kafka consumer properties configuration.
 */
@Data
@Component
@Slf4j
public class KafkaConsumerPropertiesCFG {

	/**
	 * Client id.
	 */
	@Value("${kafka.consumer.client-id}")
	private String clientId;

	/**
	 * Group id consumer.
	 */
	@Value("${kafka.consumer.group-id-high}")
	private String consumerGroupIdHigh;
	
	@Value("${kafka.consumer.group-id-medium}")
	private String consumerGroupIdMedium;
	
	@Value("${kafka.consumer.group-id-low}")
	private String consumerGroupIdLow;
	
	@Value("${kafka.consumer.group-id-common}")
	private String consumerGroupIdCommon;

	/**
	 * Consumer key deserializer.
	 */
	@Value("${kafka.consumer.key-deserializer}")
	private String consumerKeyDeserializer;

	/**
	 * Consumer value deserializer.
	 */
	@Value("${kafka.consumer.value-deserializer}")
	private String consumerValueDeserializer;

	/**
	 * Consumer bootstrap server.
	 */
	@Value("${kafka.consumer.bootstrap-servers}")
	private String consumerBootstrapServers;

	/**
	 * Isolation level.
	 */
	@Value("${kafka.consumer.isolation.level}")
	private String isolationLevel;

	/**
	 * Flag autocommit.
	 */
	@Value("${kafka.consumer.auto-commit}")
	private String autoCommit;

	/**
	 * Flag auto offset reset.
	 */
	@Value("${kafka.consumer.auto-offset-reset}")
	private String autoOffsetReset;

	/**
	 * Eccezioni per dead letter.
	 */
	@Value("#{${kafka.consumer.dead-letter-exc}}")
	private List<String> deadLetterExceptions;

	/**
	 * Protocollo.
	 */
	@Value("${kafka.properties.security.protocol}")
	private String protocol;

	/**
	 * Meccanismo.
	 */
	@Value("${kafka.properties.sasl.mechanism}")
	private String mechanism;

	/**
	 * Config jaas.
	 */
	@Value("${kafka.properties.sasl.jaas.config}")
	private String configJaas;

	/**
	 * Truststore location.
	 */
	@Value("${kafka.properties.ssl.truststore.location}")
	private String trustoreLocation;

	/**
	 * Truststore password.
	 */
	@Value("${kafka.properties.ssl.truststore.password}")
	private transient char[] trustorePassword;

	/**
	 * Kafka retry.
	 */
	@Value("${kafka.retry}")
	private Integer nRetry;
	
	/**
	 * Eccezioni temporanee.
	 */
	@Value("#{${kafka.consumer.temporary-exc}}")
	private List<String> temporaryExceptions;

	public Optional<EventStatusEnum> asExceptionType(Exception e) {
		// Retrieve exception identifier
		EventStatusEnum status = null;

		String identifier = e.getClass().getCanonicalName();
		// Identify
		if(deadLetterExceptions.contains(identifier)) status = EventStatusEnum.BLOCKING_ERROR;
		if(temporaryExceptions.contains(identifier)) status = EventStatusEnum.NON_BLOCKING_ERROR;
		// Return
		return Optional.ofNullable(status);
	}

	public void deadLetterHelper(Exception e) {
		StringBuilder sb = new StringBuilder("LIST OF USEFUL EXCEPTIONS TO MOVE TO DEADLETTER OFFSET 'kafka.consumer.dead-letter-exc'. ");
		boolean continua = true;
		Throwable excTmp = e;
		Throwable excNext = null;

		while (continua) {
			if (excNext != null) {
				excTmp = excNext;
				sb.append(", ");
			}

			sb.append(excTmp.getClass().getCanonicalName());
			excNext = excTmp.getCause();

			if (excNext == null) {
				continua = false;
			}

		}

		log.error("{}", sb);
	}
}
