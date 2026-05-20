package com.ruijie.dapengine.common.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP Header 
 *
 * <p> Header  {@code ${...}} 
 *  sysid + sign-server-auth </p>
 *
 * <h3></h3>
 * <ul>
 *   <li>{@code ${timestamp_ms}}  </li>
 *   <li>{@code ${timestamp_s}}   </li>
 *   <li>{@code ${uuid}}           UUID</li>
 *   <li>{@code ${md5(args)}}     MD5  32 </li>
 *   <li>{@code ${md5upper(args)}}  MD5  32 </li>
 *   <li>{@code ${sha256(args)}}  SHA-256  64 </li>
 *   <li>{@code ${base64(args)}}  Base64 UTF-8</li>
 *   <li>{@code ${vars.key}}       signVars </li>
 * </ul>
 *
 * <h3></h3>
 * <p>{@code +} </p>
 * <pre>
 *   ${md5upper(vars.appid+timestamp_ms+vars.secret)}
 * </pre>
 *
 * <h3></h3>
 * <p> {@link #evaluate} {@code timestamp_ms}  {@code timestamp_s}
 *  Header </p>
 *
 * <h3></h3>
 * <pre>
 *   sign-server-auth: ${vars.appid}|${timestamp_ms}|${md5upper(vars.appid+timestamp_ms+vars.secret)}
 * </pre>
 * <p> {@code appid}  {@code secret}  signVars  HTTP Header </p>
 */
public class HeaderExpressionEvaluator {

    private static final Pattern EXPR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    private HeaderExpressionEvaluator() {}

    /**
     *  Header 
     *
     * @param template Header  {@code ${...}} null 
     * @param signVars  Map{@code vars.key}  null
     * @return  Header 
     */
    public static String evaluate(String template, Map<String, String> signVars) {
        if (template == null || !template.contains("${")) {
            return template;
        }
        //  timestamp_ms 
        final long timestampMs = System.currentTimeMillis();
        final long timestampS  = timestampMs / 1000L;
        final String uuid      = UUID.randomUUID().toString().replace("-", "");

        Matcher matcher = EXPR_PATTERN.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String expr     = matcher.group(1).trim();
            String resolved = resolveExpr(expr, signVars, timestampMs, timestampS, uuid);
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    //   

    private static String resolveExpr(String expr, Map<String, String> vars,
                                      long tsMs, long tsS, String uuid) {
        if (expr.startsWith("md5upper(") && expr.endsWith(")")) {
            return hash("MD5", resolveArgs(expr.substring(9, expr.length() - 1), vars, tsMs, tsS, uuid))
                    .toUpperCase();
        }
        if (expr.startsWith("md5(") && expr.endsWith(")")) {
            return hash("MD5", resolveArgs(expr.substring(4, expr.length() - 1), vars, tsMs, tsS, uuid));
        }
        if (expr.startsWith("sha256(") && expr.endsWith(")")) {
            return hash("SHA-256", resolveArgs(expr.substring(7, expr.length() - 1), vars, tsMs, tsS, uuid));
        }
        if (expr.startsWith("base64(") && expr.endsWith(")")) {
            String raw = resolveArgs(expr.substring(7, expr.length() - 1), vars, tsMs, tsS, uuid);
            return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }
        return resolveSimple(expr, vars, tsMs, tsS, uuid);
    }

    /**
     *  {@code +} 
     */
    private static String resolveArgs(String args, Map<String, String> vars,
                                      long tsMs, long tsS, String uuid) {
        String[] parts = args.split("\\+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(resolveSimple(part.trim(), vars, tsMs, tsS, uuid));
        }
        return sb.toString();
    }

    private static String resolveSimple(String expr, Map<String, String> vars,
                                        long tsMs, long tsS, String uuid) {
        switch (expr) {
            case "timestamp_ms": return String.valueOf(tsMs);
            case "timestamp_s":  return String.valueOf(tsS);
            case "uuid":         return uuid;
            default:
                if (expr.startsWith("vars.")) {
                    String key = expr.substring(5);
                    return (vars != null) ? vars.getOrDefault(key, "") : "";
                }
                // 
                return expr;
        }
    }

    //  Hash  

    private static String hash(String algorithm, String text) {
        try {
            MessageDigest md     = MessageDigest.getInstance(algorithm);
            byte[]        digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            String        fmt    = "SHA-256".equals(algorithm) ? "%064x" : "%032x";
            return String.format(fmt, new BigInteger(1, digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Hash algorithm not available: " + algorithm, e);
        }
    }
}
