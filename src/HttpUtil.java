import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpUtil {
    public static String readBody(HttpExchange ex) throws Exception {
        try (InputStream is = ex.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static void sendJson(HttpExchange ex, int code, String json) {
        try {
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            ex.sendResponseHeaders(code, out.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(out);
            }
        } catch (Exception e) {}

    }
}
