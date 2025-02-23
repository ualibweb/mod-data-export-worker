package org.folio.dew.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.client.HoldingClient;
import org.folio.dew.client.LocationClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ItemReferenceServiceTest extends BaseBatchTest {
  @MockBean
  private HoldingClient holdingClient;

  @MockBean
  private LocationClient locationClient;

  @Autowired
  private ItemReferenceService itemReferenceService;

  @Autowired
  private ObjectMapper objectMapper;

  @ParameterizedTest
  @ValueSource(strings = {
    "{ \"effectiveLocationId\": \"b4c3e3c0-03eb-4861-acc5-55e9c6ff7d34\" }",
    "{ \"permanentLocationId\": \"b4c3e3c0-03eb-4861-acc5-55e9c6ff7d34\" }",
    "{ \"permanentLocationId\": \"e4073cd8-12ff-411b-8996-65edfa7248fc\", \"temporaryLocationId\": \"b4c3e3c0-03eb-4861-acc5-55e9c6ff7d34\" }",
  })
  @SneakyThrows
  void shouldCalculateHoldingsEffectiveLocationIfEffectiveLocationIdIsAbsent(String response) {
    var holdingsId = UUID.randomUUID().toString();
    Mockito.when(holdingClient.getHoldingById(holdingsId)).thenReturn(objectMapper.readTree(response));
    Mockito.when(locationClient.getLocation("b4c3e3c0-03eb-4861-acc5-55e9c6ff7d34")).thenReturn(objectMapper.readTree("{ \"name\": \"Expected name\" }".getBytes()));
    assertThat(itemReferenceService.getHoldingEffectiveLocationCodeById(holdingsId), equalTo("Expected name"));
  }
}
