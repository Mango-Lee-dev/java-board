import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class PostDao {
    public List<Post> findAll() throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM post ORDER BY createdAt DESC";

        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                Post post = extractPostFromResultSet(rs);
                posts.add(post);
            }
        }
        return posts;
    }

    public Post save(Post post) throws SQLException {
        // 옵션 1: createdAt을 INSERT에서 제외 (DB의 DEFAULT CURRENT_TIMESTAMP 사용)
        String sql = "INSERT INTO post (title, content, author) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, post.getTitle());
            ps.setString(2, post.getContent());
            ps.setString(3, post.getAuthor());
            // createdAt은 데이터베이스가 자동으로 설정

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creation failed, no rows affected.");
            }

            // 생성된 ID 가져오기
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    post.setId(rs.getLong(1));

                    // 생성된 게시글의 전체 정보를 다시 조회하여 createdAt 포함
                    Post savedPost = findById(post.getId());
                    if (savedPost != null) {
                        post.setCreatedAt(savedPost.getCreatedAt());
                    }
                } else {
                    throw new SQLException("Creation failed, no ID obtained");
                }
            }
        }
        return post;
    }

    public Post findById(Long id) throws SQLException {
        String sql = "SELECT * FROM post WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractPostFromResultSet(rs);
                }
            }
        }
        return null;
    }

    public boolean update(Post post) throws SQLException {
        String sql = "UPDATE post SET title = ?, content = ?, author = ? WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getTitle());
            ps.setString(2, post.getContent());
            ps.setString(3, post.getAuthor());
            ps.setLong(4, post.getId());

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }

    public boolean delete(Long id) throws SQLException {
        String sql = "DELETE FROM post WHERE id = ?";

        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);

            int affectedRows = ps.executeUpdate();

            return affectedRows > 0;
        }
    }

    public List<Post> findAllWithPagination(int offset, int limit) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM post ORDER BY createdAt DESC LIMIT ? OFFSET ?";

        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(extractPostFromResultSet(rs));
                }
            }
        }
        return posts;
    }

    public List<Post> findByAuthor(String author) throws SQLException {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM post WHERE author = ? ORDER BY createdAt DESC";

        try (Connection conn = DatabaseUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, author);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    posts.add(extractPostFromResultSet(rs));
                }
            }
        }
        return posts;
    }

    private Post extractPostFromResultSet(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getLong("id"));
        post.setTitle(rs.getString("title"));
        post.setContent(rs.getString("content"));
        post.setAuthor(rs.getString("author"));

        Timestamp timeStamp = rs.getTimestamp("createdAt");
        if (timeStamp != null) {
            post.setCreatedAt(timeStamp.toLocalDateTime());
        }
        return post;
    }


}
