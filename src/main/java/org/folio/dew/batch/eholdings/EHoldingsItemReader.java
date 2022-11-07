package org.folio.dew.batch.eholdings;

import static java.util.Collections.singletonList;
import static org.folio.dew.client.KbEbscoClient.ACCESS_TYPE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.PACKAGE;
import static org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum.RESOURCE;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.KbEbscoClient;
import org.folio.dew.config.properties.EHoldingsJobProperties;
import org.folio.dew.domain.dto.EHoldingsExportConfig;
import org.folio.dew.domain.dto.EHoldingsExportConfig.RecordTypeEnum;
import org.folio.dew.domain.dto.eholdings.EHoldingsResourceDTO;
import org.folio.dew.domain.dto.eholdings.ResourcesData;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class EHoldingsItemReader extends CsvItemReader<EHoldingsResourceDTO> {

  private static final int PAGE_OFFSET_STEP = 1;

  private final KbEbscoClient kbEbscoClient;
  private final RecordTypeEnum recordType;
  private final List<String> titleFields;
  private final String titleSearchFilters;
  private final String recordId;

  protected EHoldingsItemReader(KbEbscoClient kbEbscoClient, EHoldingsExportConfig exportConfig,
                                EHoldingsJobProperties jobProperties) {
    super(1L, 1L, jobProperties.getKbEbscoChunkSize());
    setOffsetStep(PAGE_OFFSET_STEP);
    this.kbEbscoClient = kbEbscoClient;
    this.recordId = exportConfig.getRecordId();
    this.recordType = exportConfig.getRecordType();
    this.titleFields = exportConfig.getTitleFields();
    this.titleSearchFilters = exportConfig.getTitleSearchFilters() + "&sort=name";
  }

  @Override
  protected List<EHoldingsResourceDTO> getItems(int offset, int limit) {
    if (recordType == RESOURCE) {
      var resourceById = kbEbscoClient.getResourceById(recordId, ACCESS_TYPE);
      var resourceIncluded = resourceById.getIncluded();
      var resourceData = resourceById.getData();
      resourceData.setIncluded(resourceIncluded);

      return getEHoldingsResources(singletonList(resourceData));
    }

    if (recordType == PACKAGE && CollectionUtils.isNotEmpty(titleFields)) {
      var parameters = kbEbscoClient.constructParams(offset, limit, titleSearchFilters, ACCESS_TYPE);
      var packageResources = kbEbscoClient.getResourcesByPackageId(recordId, parameters);

      return getEHoldingsResources(packageResources.getData());
    }

    return Collections.emptyList();
  }

  @Override
  protected void doOpen() {
    setMaxItemCount(getTotalCount());
  }

  private int getTotalCount() {
    if (recordType == PACKAGE) {
      if (CollectionUtils.isEmpty(titleFields)) {
        return 1;
      }
      var parameters = kbEbscoClient.constructParams(1, 1, titleSearchFilters);
      var resources = kbEbscoClient.getResourcesByPackageId(recordId, parameters);
      var totalResults = resources.getMeta().getTotalResults();
      return totalResults > 0 ? totalResults : 1;
    } else if (recordType == RESOURCE) {
      return 1;
    } else {
      return 0;
    }
  }

  private List<EHoldingsResourceDTO> getEHoldingsResources(List<ResourcesData> resourcesData) {
    return resourcesData.stream()
      .map(data -> EHoldingsResourceDTO.builder()
        .resourcesData(data)
        .build())
      .collect(Collectors.toList());
  }
}
