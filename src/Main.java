import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

//TIP 코드를 <b>실행</b>하려면 <shortcut actionId="Run"/>을(를) 누르거나
// 에디터 여백에 있는 <icon src="AllIcons.Actions.Execute"/> 아이콘을 클릭하세요.
public class Main {
    private static final byte[] JWT_SECRET = "my-very-secret-key".getBytes(StandardCharsets.UTF_8);
    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);

        //  게시판 API
        httpServer.createContext("/api/posts", new PostHandler());

        //  회원가입 API
        httpServer.createContext("/api/auth/signup", new SignupHandler());

        httpServer.setExecutor(null);
        httpServer.start();
        System.out.println("Server started");
    }
}