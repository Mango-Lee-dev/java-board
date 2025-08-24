import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserStore {
    public static class User {
        public final String id;
        public final String email;
        public final byte[] passwordHash;
        public final byte[] salt;
        public final long createdAt;

        public User(String id, String email, byte[] passwordHash, byte[] salt, long createdAt) {
            this.id = id;
            this.email = email;
            this.passwordHash = passwordHash;
            this.salt = salt;
            this.createdAt = createdAt;
        }
    }

    //  Singleton
    private static final UserStore INSTANCE = new UserStore();
    public static UserStore getInstance() {
        return INSTANCE;
    }

    private final Map<String, User> byEmail = new ConcurrentHashMap<>();

    public boolean existsByEmail(String email) {
        if (email == null) { return false; }
        return byEmail.containsKey(email.toLowerCase());
    }

    public void save(User user) {
        byEmail.put(user.email.toLowerCase(), user);
    }

    public User findByEmail(String email) {
        if (email == null) { return null; }
        return byEmail.get(email.toLowerCase());
    }
}
