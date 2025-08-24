import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.util.Map;
import java.util.UUID;

public class SignupHandler implements HttpHandler {
    private final UserStore userStore = UserStore.getInstance();

    @Override
    public void handle(HttpExchange ex) {
        try {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
                HttpUtil.sendJson(ex, 405, SimpleJson.obj("error", "method_not_allowed"));
                return;
            }
            String body = HttpUtil.readBody(ex);
            Map<String, String> m = SimpleJson.parseToStringMap(body);
            String email = m.get("email");
            String password = m.get("password");

            if (!isEmail(email) || password == null || password.length() < 8) {
                HttpUtil.sendJson(ex, 400, SimpleJson.obj("error", "invalid_input"));
                return;
            }

            if (userStore.existsByEmail(email)) {
                HttpUtil.sendJson(ex, 409, SimpleJson.obj("error", "email_in_use"));
                return;
            }

            PasswordUtil.Hash h = PasswordUtil.hashPassword(password);
            UserStore.User user = new UserStore.User(
                    UUID.randomUUID().toString(),
                    email,
                    h.hash,
                    h.salt,
                    System.currentTimeMillis()
            );
            userStore.save(user);

            HttpUtil.sendJson(ex, 201, SimpleJson.obj("ok", true));
        } catch (Exception e) {
            HttpUtil.sendJson(ex, 500, SimpleJson.obj("error", "server_error", "message", e.getMessage()));
        }
    }

    private boolean isEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
}
