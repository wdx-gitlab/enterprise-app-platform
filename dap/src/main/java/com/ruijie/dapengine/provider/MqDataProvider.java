package com.ruijie.dapengine.provider;

import com.ruijie.dapengine.common.model.FetchResult;
import com.ruijie.dapengine.common.model.SyncCheckpoint;
import com.ruijie.dapengine.common.model.SyncDataSourceConfig;
import com.ruijie.dapengine.common.model.TestConnectResultDTO;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MQ DataProvider 实现（TCP 探针模式）。
 *
 * <p>仅通过 TCP Socket 探测 MQ Broker 的网络可达性（连接超时 3 秒），
 * 不引入 Kafka/RocketMQ 客户端依赖。</p>
 *
 * <p>testConnect 始终返回：
 * <ul>
 *   <li>sourceFields = 空列表（消息字段结构无法通过 TCP 探测获取）</li>
 *   <li>sampleRows = 空列表</li>
 *   <li>warnings 包含提示：需在完整集成环境中验证 MQ 消费</li>
 * </ul>
 * </p>
 *
 * <p>fetch 方法抛出 {@code UnsupportedOperationException}：
 * MQ EVENT 模式不支持主动拉取，由外部推送驱动。</p>
 */
public class MqDataProvider implements DataProvider {

    /** TCP 连接探测超时（毫秒） */
    private static final int PROBE_TIMEOUT_MS = 3000;

    @Override
    public String type() {
        return "MQ";
    }

    @Override
    public TestConnectResultDTO testConnect(SyncDataSourceConfig config) {
        // 确定探测地址：优先 bootstrapServers（Kafka），其次 nameServer（RocketMQ）
        String servers = !isEmpty(config.getBootstrapServers())
                ? config.getBootstrapServers()
                : config.getNameServer();

        if (isEmpty(servers)) {
            return TestConnectResultDTO.failure("MQ",
                    "bootstrapServers or nameServer must not be empty for providerType=MQ.");
        }

        List<String> warnings = new ArrayList<>();
        warnings.add("[DAP Engine] MQ 数据源无法通过 TCP 探测获取字段结构，"
                + "请在完整集成环境中验证 MQ 消费正常后再配置 fieldMapping。");

        // 探测第一个地址（逗号分隔，取第一个）
        String firstAddr = servers.split(",")[0].trim();
        boolean reachable = probe(firstAddr, warnings);

        if (!reachable) {
            // warnings 中已追加具体错误
            TestConnectResultDTO result = new TestConnectResultDTO();
            result.setProviderType("MQ");
            result.setSuccess(false);
            result.setSourceFields(Collections.emptyList());
            result.setSampleRows(Collections.emptyList());
            result.setWarnings(warnings);
            result.setErrorMsg("TCP probe failed for address: " + firstAddr);
            return result;
        }

        TestConnectResultDTO result = new TestConnectResultDTO();
        result.setProviderType("MQ");
        result.setSuccess(true);
        result.setSourceFields(Collections.emptyList());
        result.setSampleRows(Collections.emptyList());
        result.setWarnings(warnings);
        return result;
    }

    @Override
    public FetchResult fetch(SyncDataSourceConfig config, SyncCheckpoint checkpoint) {
        throw new UnsupportedOperationException(
                "[DAP Engine] MqDataProvider does not support active fetch. "
                + "MQ sync uses EVENT mode driven by external message push.");
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * TCP Socket 探测 host:port，超时 {@value PROBE_TIMEOUT_MS} ms。
     *
     * @param address 格式：host:port，示例：broker1:9092
     * @return true 表示可达
     */
    private boolean probe(String address, List<String> warnings) {
        String host;
        int port;
        try {
            int colonIdx = address.lastIndexOf(':');
            if (colonIdx < 0) {
                warnings.add("Invalid address format (expected host:port): " + address);
                return false;
            }
            host = address.substring(0, colonIdx);
            port = Integer.parseInt(address.substring(colonIdx + 1));
        } catch (NumberFormatException e) {
            warnings.add("Invalid port in address: " + address);
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), PROBE_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            warnings.add("TCP probe failed for " + address + ": " + e.getMessage());
            return false;
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
