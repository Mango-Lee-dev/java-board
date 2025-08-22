import java.sql.*;
import java.time.LocalDateTime;

public class H2DatabaseInitializer {

    // H2 연결 정보
    private static final String DB_URL = "jdbc:h2:./data/testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) {
        System.out.println("=== H2 데이터베이스 초기화 시작 ===");

        // 1. 드라이버 로드
        try {
            Class.forName("org.h2.Driver");
            System.out.println("✓ H2 드라이버 로드 성공");
        } catch (ClassNotFoundException e) {
            System.err.println("✗ H2 드라이버를 찾을 수 없습니다. Maven/Gradle 의존성을 확인하세요.");
            e.printStackTrace();
            return;
        }

        // 2. 데이터베이스 연결 및 테이블 생성
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("✓ 데이터베이스 연결 성공");
            System.out.println("  URL: " + DB_URL);

            // 3. POST 테이블 생성
            createPostTable(conn);

            // 4. 샘플 데이터 삽입
            insertSampleData(conn);

            // 5. 데이터 확인
            verifyData(conn);

            // 6. H2 웹 콘솔 시작
            startH2Console();

            System.out.println("\n=== 초기화 완료 ===");
            System.out.println("H2 콘솔: http://localhost:8082");
            System.out.println("JDBC URL: " + DB_URL);
            System.out.println("사용자명: " + DB_USER);
            System.out.println("비밀번호: (빈 문자열)");

        } catch (SQLException e) {
            System.err.println("✗ 데이터베이스 오류 발생");
            e.printStackTrace();
        }
    }

    private static void createPostTable(Connection conn) throws SQLException {
        // 기존 테이블 삭제 (필요시)
        String dropTable = "DROP TABLE IF EXISTS post CASCADE";

        // POST 테이블 생성
        String createTable = """
            CREATE TABLE IF NOT EXISTS post (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                title VARCHAR(255) NOT NULL,
                content TEXT NOT NULL,
                author VARCHAR(100) NOT NULL,
                createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = conn.createStatement()) {
            // 테이블 삭제 및 재생성 (선택사항)
            // stmt.execute(dropTable);
            // System.out.println("✓ 기존 테이블 삭제");

            stmt.execute(createTable);
            System.out.println("✓ POST 테이블 생성 완료");

            // 테이블 구조 확인
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "POST", null);

            System.out.println("\n  테이블 구조:");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int size = columns.getInt("COLUMN_SIZE");
                System.out.println("    - " + columnName + " (" + dataType + ", " + size + ")");
            }
        }
    }

    private static void insertSampleData(Connection conn) throws SQLException {
        // 데이터가 이미 있는지 확인
        String checkQuery = "SELECT COUNT(*) FROM post";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkQuery)) {

            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("✓ 샘플 데이터가 이미 존재합니다 (개수: " + rs.getInt(1) + ")");
                return;
            }
        }

        // 샘플 데이터 삽입
        String insertQuery = """
            INSERT INTO post (title, content, author, createdAt) VALUES 
            (?, ?, ?, ?),
            (?, ?, ?, ?),
            (?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
            // 첫 번째 게시글
            pstmt.setString(1, "H2 데이터베이스 시작하기");
            pstmt.setString(2, "H2는 자바로 작성된 관계형 데이터베이스입니다. 가볍고 빠르며 임베디드 모드와 서버 모드를 모두 지원합니다.");
            pstmt.setString(3, "관리자");
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now().minusDays(2)));

            // 두 번째 게시글
            pstmt.setString(5, "IntelliJ IDEA에서 H2 사용하기");
            pstmt.setString(6, "IntelliJ IDEA에서 H2 데이터베이스를 설정하고 사용하는 방법을 알아봅시다. Maven이나 Gradle을 통해 쉽게 의존성을 추가할 수 있습니다.");
            pstmt.setString(7, "김개발");
            pstmt.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

            // 세 번째 게시글
            pstmt.setString(9, "REST API 만들기");
            pstmt.setString(10, "Java HttpServer와 H2 데이터베이스를 연동하여 간단한 REST API를 만들어보겠습니다.");
            pstmt.setString(11, "이자바");
            pstmt.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));

            int rowsInserted = pstmt.executeUpdate();
            System.out.println("✓ 샘플 데이터 " + rowsInserted + "개 삽입 완료");
        }
    }

    private static void verifyData(Connection conn) throws SQLException {
        String query = "SELECT * FROM post ORDER BY createdAt DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            System.out.println("\n  저장된 데이터:");
            while (rs.next()) {
                System.out.println("    ID: " + rs.getLong("id") +
                        ", 제목: " + rs.getString("title") +
                        ", 작성자: " + rs.getString("author"));
            }
        }
    }

    private static void startH2Console() {
        try {
            // H2 웹 콘솔 시작
            org.h2.tools.Server server = org.h2.tools.Server.createWebServer(
                    "-web",
                    "-webAllowOthers",
                    "-webPort", "8082"
            );
            server.start();
            System.out.println("\n✓ H2 웹 콘솔 시작됨");
        } catch (SQLException e) {
            System.err.println("⚠ H2 콘솔 시작 실패 (이미 실행 중일 수 있음)");
        }
    }
}

