package com.ruijie.dapengine.provider;

import com.ruijie.dapengine.common.model.FetchResult;
import com.ruijie.dapengine.common.model.SyncCheckpoint;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 数据提供者 SPI 接口。
 *
 * <p>实现类通过 {@code type()} 方法返回提供者类型标识（HTTP / DB / MQ），
 * 由 {@code SyncExecutor} 根据同步配置的 {@code providerType} 选择对应实现。</p>
 *
 * <p>所有实现类必须注册为 Spring Bean（在 {@code DapEngineAutoConfiguration} 中声明），
 * 并由 {@code SyncConfigController} 和 {@code SyncExecutor} 通过 {@code List<DataProvider>} 注入。</p>
 */
public interface DataProvider {

    /**
     * 返回该提供者支持的类型标识，与 {@code ProviderType} 枚举名称对应。
     *
     * @return 类型字符串，如 "HTTP" / "DB" / "MQ"
     */
    String type();

    /**
     * 从外部数据源拉取增量或全量数据。
     *
     * @param config     数据源配置（含连接信息、查询 SQL、URL 等）
     * @param checkpoint 上次同步位点（用于增量参数绑定）
     * @return 拉取结果，records 为原始字段名 Map 列表
     */
    FetchResult fetch(SyncDataSourceConfig config, SyncCheckpoint checkpoint);

    /**
     * 试连接：验证数据源可达性，并返回字段预览（最多 5 条样例数据）。
     *
     * <p>该方法不应抛出异常：连接失败时返回 {@code success=false} 的 DTO，
     * 错误描述放入 {@code errorMsg} 字段。</p>
     *
     * @param config 数据源配置（可为未保存的草稿）
     * @return 试连接结果
     */
    TestConnectResultDTO testConnect(SyncDataSourceConfig config);

    /**
     * 流式分页拉取：每拉到一页数据立即回调 {@code pageConsumer}，而非全部积累到内存后返回。
     *
     * <p>默认实现调用 {@link #fetch} 并将结果整体作为单页回调，兼容不支持分页的 Provider（DB/MQ）。
     * 支持真正分页的 Provider（如 HTTP）应覆写此方法以避免大数据集 OOM。</p>
     *
     * <p>约定：
     * <ul>
     *   <li>若数据源无数据，{@code pageConsumer} 一次也不会被调用。</li>
     *   <li>{@code pageConsumer} 抛出的异常会向上传播，调用方负责清理（如回滚 tmp 表）。</li>
     * </ul>
     * </p>
     *
     * @param config       数据源配置
     * @param checkpoint   上次同步位点
     * @param pageConsumer 每页数据的回调处理器
     */
    default void fetchStreaming(SyncDataSourceConfig config, SyncCheckpoint checkpoint,
                                Consumer<List<Map<String, Object>>> pageConsumer) {
        FetchResult result = fetch(config, checkpoint);
        if (!result.isEmpty()) {
            pageConsumer.accept(result.getRecords());
        }
    }
}
