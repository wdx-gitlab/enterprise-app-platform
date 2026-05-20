package com.ruijie.dapengine.admin.controller;

import com.ruijie.dapengine.sdk.MasterDataService;
import com.ruijie.dapengine.common.model.PageResult;
import com.ruijie.dapengine.common.model.Result;
import com.ruijie.dapengine.common.model.TreeNode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 主数据浏览 Admin API 控制器。
 *
 * <p>提供三个只读端点，供前端管理页面查询 dap_{subject} 动态主数据表：
 * <ul>
 *   <li>EP1 {@code GET /dap-engine/admin/browse/{subjectCode}/{code}} ：按编码查单条</li>
 *   <li>EP2 {@code GET /dap-engine/admin/browse/{subjectCode}} ：分页模糊搜索</li>
 *   <li>EP3 {@code GET /dap-engine/admin/browse/{subjectCode}/tree} ：树形查询</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/dap-engine/admin/browse")
public class MasterDataBrowseController {

    private final MasterDataService masterDataService;

    public MasterDataBrowseController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    /**
     * EP1: GET /dap-engine/admin/browse/{subjectCode}/{code}
     * 按编码查单条主数据，缓存命中时不访问 DB。
     *
     * @param subjectCode 主题编码
     * @param code        记录唯一编码
     * @return 字段 Map；不存在时 code=404
     */
    @GetMapping("/{subjectCode}/{code}")
    public Result<Map<String, Object>> getByCode(@PathVariable String subjectCode,
                                                  @PathVariable String code) {
        Map<String, Object> result = masterDataService.getByCode(subjectCode, code);
        if (result == null) {
            return Result.fail(404, "记录不存在: " + code);
        }
        return Result.ok(result);
    }

    /**
     * EP2: GET /dap-engine/admin/browse/{subjectCode}?keyword=&page=1&size=20
     * 按 name 字段模糊搜索，分页返回。
     *
     * @param subjectCode 主题编码
     * @param keyword     搜索关键词（空时返回全量分页）
     * @param page        页码（默认 1）
     * @param size        每页条数（默认 20，最大 100）
     * @return 分页结果
     */
    @GetMapping("/{subjectCode}")
    public Result<PageResult<Map<String, Object>>> search(
            @PathVariable String subjectCode,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size > 100) {
            size = 100;
        }
        if (page < 1) {
            page = 1;
        }
        PageResult<Map<String, Object>> result = masterDataService.search(subjectCode, keyword, page, size);
        return Result.ok(result);
    }

    /**
     * EP3: GET /dap-engine/admin/browse/{subjectCode}/tree?rootCode=
     * 树形查询。数据量超阈值时使用 WITH RECURSIVE CTE，否则内存建树。
     *
     * @param subjectCode 主题编码
     * @param rootCode    起始根节点编码（空时从最顶层开始）
     * @return 树节点列表
     */
    @GetMapping("/{subjectCode}/tree")
    public Result<List<TreeNode>> getTree(@PathVariable String subjectCode,
                                           @RequestParam(defaultValue = "") String rootCode) {
        List<TreeNode> tree = masterDataService.getTree(subjectCode, rootCode);
        return Result.ok(tree);
    }
}
