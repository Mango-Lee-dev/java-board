import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(PostHandler.class.getName());
    private final PostDao postDao = new PostDao();
    private final Gson gson;

    // URL 패턴 정의
    private static final Pattern GET_BY_ID_PATTERN = Pattern.compile("^/api/posts/(\\d+)$");
    private static final Pattern BASE_PATH_PATTERN = Pattern.compile("^/api/posts/?$");

    public PostHandler() {
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .setPrettyPrinting()
                .create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // CORS 헤더 설정
        setCorsHeaders(exchange);

        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Preflight 요청 처리
        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            switch (method.toUpperCase()) {
                case "GET":
                    handleGet(exchange, path);
                    break;
                case "POST":
                    handlePost(exchange, path);
                    break;
                case "PUT":
                    handlePut(exchange, path);
                    break;
                case "DELETE":
                    handleDelete(exchange, path);
                    break;
                default:
                    sendResponse(exchange, 405, createErrorResponse("Method not allowed"));
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error", e);
            sendResponse(exchange, 500, createErrorResponse("Internal server error"));
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        try {
            Matcher matcher = GET_BY_ID_PATTERN.matcher(path);

            if (matcher.matches()) {
                // GET /posts/{id} - 단일 조회
                Long id = Long.parseLong(matcher.group(1));
                Post post = postDao.findById(id);

                if (post != null) {
                    sendResponse(exchange, 200, gson.toJson(post));
                } else {
                    sendResponse(exchange, 404, createErrorResponse("Post not found"));
                }
            } else if (BASE_PATH_PATTERN.matcher(path).matches()) {
                // GET /posts - 전체 조회
                List<Post> posts = postDao.findAll();
                sendResponse(exchange, 200, gson.toJson(posts));
            } else {
                sendResponse(exchange, 404, createErrorResponse("Invalid path"));
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during GET", e);
            sendResponse(exchange, 500, createErrorResponse("Database error: " + e.getMessage()));
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, createErrorResponse("Invalid ID format"));
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (!BASE_PATH_PATTERN.matcher(path).matches()) {
            sendResponse(exchange, 404, createErrorResponse("Invalid path"));
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(
                exchange.getRequestBody(), StandardCharsets.UTF_8)) {

            // JSON을 Post 객체로 직접 변환
            Post post = gson.fromJson(reader, Post.class);

            // 유효성 검증
            if (!isValidPost(post)) {
                sendResponse(exchange, 400, createErrorResponse("Invalid post data"));
                return;
            }

            // 저장
            Post createdPost = postDao.save(post);

            // 응답
            String jsonResponse = gson.toJson(createdPost);
            sendResponse(exchange, 201, jsonResponse);

        } catch (JsonSyntaxException e) {
            logger.log(Level.WARNING, "Invalid JSON in request", e);
            sendResponse(exchange, 400, createErrorResponse("Invalid JSON format: " + e.getMessage()));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during POST", e);
            sendResponse(exchange, 500, createErrorResponse("Database error: " + e.getMessage()));
        }
    }

    private void handlePut(HttpExchange exchange, String path) throws IOException {
        Matcher matcher = GET_BY_ID_PATTERN.matcher(path);

        if (!matcher.matches()) {
            sendResponse(exchange, 404, createErrorResponse("Invalid path"));
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(
                exchange.getRequestBody(), StandardCharsets.UTF_8)) {

            Long id = Long.parseLong(matcher.group(1));

            // JSON을 Post 객체로 변환
            Post post = gson.fromJson(reader, Post.class);
            post.setId(id);

            if (!isValidPost(post)) {
                sendResponse(exchange, 400, createErrorResponse("Invalid post data"));
                return;
            }

            // 존재 여부 확인
            Post existingPost = postDao.findById(id);
            if (existingPost == null) {
                sendResponse(exchange, 404, createErrorResponse("Post not found"));
                return;
            }

            boolean updated = postDao.update(post);
            if (updated) {
                Post updatedPost = postDao.findById(id);
                sendResponse(exchange, 200, gson.toJson(updatedPost));
            } else {
                sendResponse(exchange, 500, createErrorResponse("Update failed"));
            }

        } catch (JsonSyntaxException e) {
            sendResponse(exchange, 400, createErrorResponse("Invalid JSON format"));
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during PUT", e);
            sendResponse(exchange, 500, createErrorResponse("Database error: " + e.getMessage()));
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, createErrorResponse("Invalid ID format"));
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        Matcher matcher = GET_BY_ID_PATTERN.matcher(path);

        if (!matcher.matches()) {
            sendResponse(exchange, 404, createErrorResponse("Invalid path"));
            return;
        }

        try {
            Long id = Long.parseLong(matcher.group(1));

            // 존재 여부 확인
            Post existingPost = postDao.findById(id);
            if (existingPost == null) {
                sendResponse(exchange, 404, createErrorResponse("Post not found"));
                return;
            }

            boolean deleted = postDao.delete(id);
            if (deleted) {
                sendResponse(exchange, 204, ""); // No Content
            } else {
                sendResponse(exchange, 500, createErrorResponse("Delete failed"));
            }

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error during DELETE", e);
            sendResponse(exchange, 500, createErrorResponse("Database error: " + e.getMessage()));
        } catch (NumberFormatException e) {
            sendResponse(exchange, 400, createErrorResponse("Invalid ID format"));
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");

        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
            os.flush();
        }
    }

    private String createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("error", message);
        return gson.toJson(error);
    }

    private boolean isValidPost(Post post) {
        return post != null &&
                post.getTitle() != null && !post.getTitle().trim().isEmpty() &&
                post.getContent() != null && !post.getContent().trim().isEmpty() &&
                post.getAuthor() != null && !post.getAuthor().trim().isEmpty();
    }
}