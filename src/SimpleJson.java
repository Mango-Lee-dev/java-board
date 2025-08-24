import java.util.HashMap;
import java.util.Map;

public class SimpleJson {
    public static Map<String, String> parseToStringMap(String jsonString) {
        // {"a":"b","n":1,"ok":true} -> Map<String,String> (숫자/불리언도 문자열로)
        Map<String, String> map = new HashMap<>();
        if (jsonString == null) { return map; }

        jsonString = jsonString.trim();
        if (!jsonString.startsWith("{") && !jsonString.startsWith("[")) { return map; }
        String inner = jsonString.substring(1, jsonString.length() - 1);
        if (inner.isEmpty()) { return map; }


        // 콤마 단순 분할(값 안에 콤마 없는 경우 전제)
        String[] parts = inner.split(",");
        for (String p : parts) {
            String [] kv = p.split(":", 2);
            if (kv.length != 2) continue;
            String k = stripQuotes(kv[0].trim());
            String vRaw = kv[1].trim();
            String v = stripQuotes(vRaw);
            map.put(k, v);
        }
        return map;
    }

    private static String stripQuotes(String s) {
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1).replace("\\\"", "\"");
        }
        return s;
    }

    //  간단한 JSON 생성기: 키-값 쌍 1~2개 빠르게 만들 때
    public static String obj(Object... kvs) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kvs.length; i += 2) {
            if (i > 0) { sb.append(","); }
            String k = String.valueOf(kvs[i]);
            Object v = (i + 1 < kvs.length) ? kvs[i + 1] : null;
            sb.append("\"").append(escape(k)).append("\":").append(toJsonValue(v));
        }

        sb.append("}");
        return sb.toString();
    }

    private static String toJsonValue(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        return "\"" + escape(String.valueOf(v)) + "\"";
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
