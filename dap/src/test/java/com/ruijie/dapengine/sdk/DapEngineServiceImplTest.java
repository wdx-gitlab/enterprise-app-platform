package com.ruijie.dapengine.sdk;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * DapEngineServiceImpl 单元测试。
 */
public class DapEngineServiceImplTest {

    @Test
    public void should_expose_all_sub_services() {
        MasterDataQueryService queryService = mock(MasterDataQueryService.class);
        MasterDataMetaService metaService = mock(MasterDataMetaService.class);
        MasterDataSyncService syncService = mock(MasterDataSyncService.class);

        DapEngineServiceImpl service = new DapEngineServiceImpl(
                queryService, metaService, syncService);

        assertThat(service.query()).isSameAs(queryService);
        assertThat(service.meta()).isSameAs(metaService);
        assertThat(service.sync()).isSameAs(syncService);
    }
}
