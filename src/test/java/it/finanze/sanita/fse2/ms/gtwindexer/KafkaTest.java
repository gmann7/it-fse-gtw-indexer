/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package it.finanze.sanita.fse2.ms.gtwindexer;

import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.EMPTY_JSON;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.FAILURE_RESPONSE_INI_DTO;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.SUCCESS_RESPONSE_INI_DTO;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.getFakeDeleteRequest;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.getFakeRetryRequest;
import static it.finanze.sanita.fse2.ms.gtwindexer.TestConstants.testWorkflowInstanceId;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniMetadataUpdateReqDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniTraceResponseDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;

import it.finanze.sanita.fse2.ms.gtwindexer.client.impl.IniClient;
import it.finanze.sanita.fse2.ms.gtwindexer.config.Constants;
import it.finanze.sanita.fse2.ms.gtwindexer.config.kafka.KafkaTopicCFG;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IndexerValueDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.request.IniDeleteRequestDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.dto.response.IniPublicationResponseDTO;
import it.finanze.sanita.fse2.ms.gtwindexer.enums.ProcessorOperationEnum;
import it.finanze.sanita.fse2.ms.gtwindexer.exceptions.BlockingIniException;
import it.finanze.sanita.fse2.ms.gtwindexer.service.IKafkaSRV;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(Constants.Profile.TEST)
@EmbeddedKafka
class KafkaTest extends AbstractTest {
	
	@SpyBean
	private IKafkaSRV kafkaSRV;

	@Autowired
	private KafkaTopicCFG kafkaTopicCFG;

	@SpyBean
	private IniClient iniClient;

	@SpyBean
	private RestTemplate restTemplate;

	@Test
	@Description("Publication - Success")
	void kafkaListenerPublicationSuccessTest() throws ExecutionException, InterruptedException {
		String topicLow = kafkaTopicCFG.getDispatcherIndexerLowPriorityTopic();
		String topicMedium = kafkaTopicCFG.getDispatcherIndexerMediumPriorityTopic();
		String topicHigh = kafkaTopicCFG.getDispatcherIndexerHighPriorityTopic();

		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);

		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();

		records.put(new TopicPartition(topicLow, 0), new ArrayList<>());
		records.put(new TopicPartition(topicMedium, 0), new ArrayList<>());
		records.put(new TopicPartition(topicHigh, 0), new ArrayList<>());
		
		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(testWorkflowInstanceId, "String", ProcessorOperationEnum.PUBLISH));

		this.kafkaInit(topicLow, testWorkflowInstanceId, kafkaValue);
		this.kafkaInit(topicMedium, testWorkflowInstanceId, kafkaValue);
		this.kafkaInit(topicHigh, testWorkflowInstanceId, kafkaValue);

		ConsumerRecord<String, String> recordLow = new ConsumerRecord<String,String>(topicLow, 1, 0, topicLow, kafkaValue);
		ConsumerRecord<String, String> recordMedium = new ConsumerRecord<String,String>(topicMedium, 1, 0, topicMedium, kafkaValue);
		ConsumerRecord<String, String> recordHigh = new ConsumerRecord<String,String>(topicHigh, 1, 0, topicHigh, kafkaValue);

		IniPublicationResponseDTO responseDTO = new IniPublicationResponseDTO();
		responseDTO.setEsito(true);
		doReturn(responseDTO).when(restTemplate)
				.postForObject(anyString(), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertDoesNotThrow(() -> kafkaSRV.lowPriorityListener(recordLow, headers));
		assertDoesNotThrow(() -> kafkaSRV.mediumPriorityListener(recordMedium, headers));
		assertDoesNotThrow(() -> kafkaSRV.highPriorityListener(recordHigh, headers));
	}

	 
	@Test
	@Description("Replace - Success")
	void kafkaListenerReplaceSuccessTest() {
		String topicLow = kafkaTopicCFG.getDispatcherIndexerLowPriorityTopic();

		Map<String, Object> map = new HashMap<>();
		MessageHeaders headers = new MessageHeaders(map);
		Map<TopicPartition, List<ConsumerRecord<String, String>>> records = new LinkedHashMap<>();
		records.put(new TopicPartition(topicLow, 0), new ArrayList<>());

		final String kafkaValue = new Gson().toJson(new IndexerValueDTO(testWorkflowInstanceId, "String", ProcessorOperationEnum.REPLACE));

		ConsumerRecord<String, String> recordLow = new ConsumerRecord<String,String>(topicLow, 1, 0, topicLow, kafkaValue);
		IniPublicationResponseDTO responseDTO = new IniPublicationResponseDTO();
		responseDTO.setEsito(true);

		doReturn(new ResponseEntity<>(responseDTO, HttpStatus.OK)).when(restTemplate)
						.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniPublicationResponseDTO.class));

		assertDoesNotThrow(() -> kafkaSRV.lowPriorityListener(recordLow, headers));
	}

	@Test
	void retryTestUpdateSuccess() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
				getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doReturn(new ResponseEntity<>(SUCCESS_RESPONSE_INI_DTO, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertDoesNotThrow(() -> kafkaSRV.retryUpdateListener(key, value));
	}

	@Test
	void retryDeleteTestSuccess() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doReturn(new ResponseEntity<>(SUCCESS_RESPONSE_INI_DTO, HttpStatus.OK)).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertDoesNotThrow(() -> kafkaSRV.retryDeleteListener(key, value));
	}

	@Test
	void retryDeleteTestFailure() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doReturn(FAILURE_RESPONSE_INI_DTO).when(iniClient).delete(any(IniDeleteRequestDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryDeleteListener(key, value));
	}

	@Test
	void retryDeleteTestWithInvalidPayload() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			EMPTY_JSON
		);
		// Provide mock knowledge
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), anyString());
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(BlockingIniException.class,() -> kafkaSRV.retryDeleteListener(key, value));
	}

	@Test
	void retryDeleteTestWithBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(NullPointerException.class).when(iniClient).delete(any(IniDeleteRequestDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(NullPointerException.class, () -> kafkaSRV.retryDeleteListener(key, value));
	}

	@Test
	void retryDeleteTestWithResourceAccessBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(ResourceAccessException.class).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(ResourceAccessException .class, () -> kafkaSRV.retryDeleteListener(key, value));
	}

	@Test
	void retryDeleteTestWithHttpClientBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
				getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(HttpClientErrorException.class).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryDeleteListener(key, value));
	}

	@Test
	void retryDeleteTestWithUnknownError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
			kafkaTopicCFG.getDispatcherIndexerDeleteRetryTopic(),
			getFakeDeleteRequest()
		);
		// Provide mock knowledge
		doThrow(RuntimeException.class).when(iniClient).delete(any(IniDeleteRequestDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryDeleteListener(key, value));
	}

	@Test
	void retryUpdateTestWithResourceAccessBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerUpdateRetryTopic(),
				"{\"key\":\"value\"}"
		);
		// Provide mock knowledge
		doThrow(ResourceAccessException.class).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(ResourceAccessException .class, () -> kafkaSRV.retryUpdateListener(key, value));
	}

	@Test
	void retryUpdateTestWithHttpClientBlockingError() {
		// Create fake request
		SimpleImmutableEntry<ConsumerRecord<String, String>, MessageHeaders> req = getFakeRetryRequest(
				kafkaTopicCFG.getDispatcherIndexerUpdateRetryTopic(),
				"{\"key\":\"value\"}"
		);
		// Provide mock knowledge
		doThrow(HttpClientErrorException.class).when(restTemplate)
				.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(IniTraceResponseDTO.class));
		doNothing().when(kafkaSRV).sendStatusMessage(anyString(), any(), any(), nullable(String.class));
		// Start
		ConsumerRecord<String, String> key = req.getKey();
		MessageHeaders value = req.getValue();
		assertNotNull(key);
		assertNotNull(value);
		assertThrows(BlockingIniException.class, () -> kafkaSRV.retryUpdateListener(key, value));
	}

}
