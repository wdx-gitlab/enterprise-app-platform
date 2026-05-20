package com.ruijie.uspportal.auth.integration.sso;

import com.ruijie.framework.sso.base.model.LocalUserInfo;
import com.ruijie.framework.sso.base.spi.RuiJieSsoVerifyProcessor;
import com.ruijie.uspportal.security.CurrentUserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

@Slf4j
@Service
/**
 * SSO 认证服务。
 *
 * <p>负责加载所有 SSO 校验处理器，并按配置顺序尝试完成当前请求的 SSO 登录识别。</p>
 */
public class SsoAuthenticationService {

    private final ApplicationContext applicationContext;

    @Value("${spring.ruijie.sso.checkTypeSort:${spring.ruijie.sso.check-type-sort:SID,USP}}")
    private String checkTypeSort;

    @Value("${usp.portal.default-tenant-code:_PLATFORM_}")
    private String defaultTenantCode;

    private volatile List<RuiJieSsoVerifyProcessor> processors = Collections.emptyList();

    /**
     * 创建 SsoAuthenticationService 实例。
     */
    public SsoAuthenticationService(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    /**
     * 初始化并加载全部 SSO 校验处理器。
     */
    public void init() {
        reloadProcessors();
    }

    /**
     * 执行登录认证。
     */
    public CurrentUserContext.CurrentUser authenticate(HttpServletRequest request, HttpServletResponse response) {
        for (RuiJieSsoVerifyProcessor processor : processors) {
            try {
                LocalUserInfo localUserInfo = processor.loginCheck(request, response);
                if (localUserInfo == null || !StringUtils.hasText(readString(localUserInfo, "getUserId"))) {
                    continue;
                }
                return mapCurrentUser(processor, localUserInfo);
            } catch (Exception ex) {
                log.warn("SSO processor [{}] failed: {}", processor.getClass().getName(), ex.getMessage());
            }
        }
        return null;
    }

    /**
     * 重新加载并排序所有 SSO 校验处理器。
     */
    private synchronized void reloadProcessors() {
        Map<String, RuiJieSsoVerifyProcessor> discovered = new LinkedHashMap<>();
        ServiceLoader<RuiJieSsoVerifyProcessor> loader = ServiceLoader.load(RuiJieSsoVerifyProcessor.class, applicationContext.getClassLoader());
        for (RuiJieSsoVerifyProcessor processor : loader) {
            processor.init(applicationContext);
            discovered.put(processor.getClass().getName(), processor);
        }
        processors = sortProcessors(discovered.values());
        log.info("Loaded SSO processors in order: {}", describeProcessors(processors));
    }

    /**
     * 按配置顺序对 SSO 校验处理器排序。
     *
     * @param loadedProcessors 已加载的处理器集合
     * @return 排序后的处理器列表
     */
    private List<RuiJieSsoVerifyProcessor> sortProcessors(Collection<RuiJieSsoVerifyProcessor> loadedProcessors) {
        List<String> order = new ArrayList<>();
        for (String item : checkTypeSort.split(",")) {
            if (StringUtils.hasText(item)) {
                order.add(item.trim().toUpperCase());
            }
        }

        List<RuiJieSsoVerifyProcessor> sorted = new ArrayList<>(loadedProcessors);
        Map<String, Integer> explicitOrder = new HashMap<>();
        for (int index = 0; index < order.size(); index++) {
            explicitOrder.put(order.get(index), index);
        }

        sorted.sort((left, right) -> {
            int leftOrder = explicitOrder.getOrDefault(left.getSSOType().toUpperCase(), Integer.MAX_VALUE);
            int rightOrder = explicitOrder.getOrDefault(right.getSSOType().toUpperCase(), Integer.MAX_VALUE);
            if (leftOrder != rightOrder) {
                return Integer.compare(leftOrder, rightOrder);
            }
            return left.getClass().getName().compareTo(right.getClass().getName());
        });
        return sorted;
    }

    /**
     * 生成当前处理器链的描述字符串。
     *
     * @param loadedProcessors 已排序的处理器列表
     * @return 处理器描述字符串
     */
    private String describeProcessors(List<RuiJieSsoVerifyProcessor> loadedProcessors) {
        List<String> result = new ArrayList<>();
        for (RuiJieSsoVerifyProcessor processor : loadedProcessors) {
            result.add(processor.getSSOType() + "=" + processor.getClass().getSimpleName());
        }
        return Arrays.toString(result.toArray(new String[0]));
    }

    @SuppressWarnings("unchecked")
    /**
     * 将 SSO 框架返回的用户对象映射为门户当前用户上下文。
     *
     * @param processor 命中的 SSO 处理器
     * @param localUserInfo SSO 框架用户信息
     * @return 当前用户上下文
     */
    private CurrentUserContext.CurrentUser mapCurrentUser(RuiJieSsoVerifyProcessor processor, LocalUserInfo localUserInfo) {
        Map<String, Object> ext = readMap(localUserInfo, "getExt");
        String userId = firstNonBlank(readString(localUserInfo, "getUserId"), stringValue(ext.get("userId")));
        String userNo = firstNonBlank(readString(localUserInfo, "getUserNo"), stringValue(ext.get("loginName")));
        String userName = firstNonBlank(readString(localUserInfo, "getUserName"), stringValue(ext.get("displayName")), userNo, userId);
        String tenantCode = firstNonBlank(
                stringValue(ext.get("tenantCode")),
                readString(localUserInfo, "getDeptCode"),
                defaultTenantCode
        );
        String sessionId = stringValue(ext.get("sessionId"));
        String authMode = firstNonBlank(stringValue(ext.get("authMode")), processor.getSSOType());
        Boolean admin = Boolean.valueOf(firstNonBlank(stringValue(ext.get("admin")), "false"));

        return CurrentUserContext.CurrentUser.builder()
                .userId(userId)
                .loginName(firstNonBlank(userNo, userName, userId))
                .displayName(firstNonBlank(userName, userNo, userId))
                .tenantCode(tenantCode)
                .sessionId(sessionId)
                .authMode(authMode)
                .admin(admin)
                .build();
    }

    /**
        * 通过反射读取对象上的字符串属性。
        *
        * @param target 目标对象
        * @param methodName 无参方法名
        * @return 字符串结果，失败时返回 {@code null}
     */
    private String readString(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * 通过反射读取对象上的扩展 Map。
     *
     * @param target 目标对象
     * @param methodName 无参方法名
     * @return Map 结果，失败时返回空 Map
     */
    private Map<String, Object> readMap(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            return value instanceof Map ? (Map<String, Object>) value : Collections.emptyMap();
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    /**
     * 将任意对象转换为字符串。
     *
     * @param value 原始对象
     * @return 字符串结果
     */
    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 返回首个非空白字符串。
     *
     * @param values 候选字符串列表
     * @return 首个有效字符串
     */
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
