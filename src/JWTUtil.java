import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class JWTUtil {
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    //  ayload 예: {"sub":"userId","email":"a@b.com","iat":..., "exp":...}
    public static String signHS256(byte[] secret, Map<String, Object> payload) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = toJson(payload);
        String header = B64.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String body = B64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + body;
        byte[] sig = hmac256(secret, signingInput);
        return signingInput + "." + B64.encodeToString(sig);
    }

    /* 검증 성공 시 payload(map) 반환, 실패 시 null */
    public static Map<String, Object> verifyHS256(byte[] secret, String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;
            String signingInput = parts[0] + "." + parts[1];
            byte[] expected = hmac256(secret, signingInput);
            byte[] given = B64D.decode(parts[2]);

            if (!MessageDigest.isEqual(expected, given)) {
                return null;
            }
            String payloadJson = new String(B64D.decode(parts[1]), StandardCharsets.UTF_8);
            return parseJson(payloadJson);
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] hmac256(byte[] secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    //  아주 단순한 JSON 직렬화/파싱 (중첩X)
    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(",");

            first = false;
            sb.append("\"").append(e.getKey()).append("\":");
            Object value = e.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append("\"").append(String.valueOf(value).replace("\"", "\\\"")).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        json = json.trim();

        if (!(json.startsWith("{") && json.endsWith("}"))) { return result; }
        String inner = json.substring(1, json.length() - 1).trim();

        if (inner.isEmpty()) { return result; }
        for (String part : inner.split(",")) {
            String[] kv = part.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim().replaceAll("^\"|\"$", "");
            String value = kv[1].trim();

            if (value.matches("^-?\\d+$")) result.put(key, Long.parseLong(value));
            else if ("true".equals(value) || "false".equals(value)) result.put(key, Boolean.parseBoolean(value));
            else result.put(key, value.replaceAll("^\"|\"$", ""));
        }
        return result;
    }
}
