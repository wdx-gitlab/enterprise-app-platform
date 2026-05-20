package com.ruijie.dapengine.sdk;

import lombok.RequiredArgsConstructor;

/**
 * {@link DapEngineService} 实现，聚合 query / meta / sync 三个子服务。
 */
@RequiredArgsConstructor
public class DapEngineServiceImpl implements DapEngineService {

    private final MasterDataQueryService queryService;
    private final MasterDataMetaService metaService;
    private final MasterDataSyncService syncService;

    @Override
    public MasterDataQueryService query() {
        return queryService;
    }

    @Override
    public MasterDataMetaService meta() {
        return metaService;
    }

    @Override
    public MasterDataSyncService sync() {
        return syncService;
    }
}
