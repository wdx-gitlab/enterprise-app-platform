package com.ruijie.dapengine.sdk;

import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.QuerySpec;
import com.ruijie.dapengine.common.model.TreeNode;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * {@link MasterDataQueryService} 实现，委托给 {@link MasterDataService}。
 */
@RequiredArgsConstructor
public class MasterDataQueryServiceImpl implements MasterDataQueryService {

    private final MasterDataService masterDataService;

    @Override
    public Map<String, Object> getByCode(String subject, String code) {
        return masterDataService.getByCode(subject, code);
    }

    @Override
    public <T> T getByCode(String subject, String code, Class<T> clazz) {
        return masterDataService.getByCode(subject, code, clazz);
    }

    @Override
    public Map<String, Map<String, Object>> batchGetByCodes(String subject, List<String> codes) {
        return masterDataService.batchGetByCodes(subject, codes);
    }

    @Override
    public boolean exists(String subject, String code) {
        return masterDataService.exists(subject, code);
    }

    @Override
    public <T> List<T> query(String subject, Map<String, Object> conditions, Class<T> clazz) {
        return masterDataService.query(subject, conditions, clazz);
    }

    @Override
    public List<Map<String, Object>> list(String subject, QuerySpec spec) {
        return masterDataService.list(subject, spec);
    }

    @Override
    public <T> List<T> list(String subject, QuerySpec spec, Class<T> clazz) {
        return masterDataService.list(subject, spec, clazz);
    }

    @Override
    public PageResult<Map<String, Object>> page(String subject, QuerySpec spec, int page, int size) {
        return masterDataService.page(subject, spec, page, size);
    }

    @Override
    public <T> PageResult<T> page(String subject, QuerySpec spec, int page, int size, Class<T> clazz) {
        return masterDataService.page(subject, spec, page, size, clazz);
    }

    @Override
    public long count(String subject, QuerySpec spec) {
        return masterDataService.count(subject, spec);
    }

    @Override
    public <T> PageResult<T> search(String subject, String keyword, int page, int size, Class<T> clazz) {
        return masterDataService.search(subject, keyword, page, size, clazz);
    }

    @Override
    public List<TreeNode> getTree(String subject, String rootCode) {
        return masterDataService.getTree(subject, rootCode);
    }
}
