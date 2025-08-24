import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class PasswordUtil {
    public static class Hash {
        public final byte[] hash;
        public final byte[] salt;
        public Hash(byte[] hash, byte[] salt) {
            this.hash = hash;
            this.salt = salt;
        }
    }

    private static final SecureRandom RAND = new SecureRandom();
    private static final int ITER = 120_000;
    private static final int KEYLEN = 256;
    private static final String ALG = "PBKDF2WithHmacSHA256";

    public static Hash hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            RAND.nextBytes(salt);
            byte[] hash = derive(password, salt);
            return new Hash(hash, salt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String password, byte[] salt, byte[] expected) {
        try {
            byte[] got = derive(password, salt);
            return MessageDigest.isEqual(got, expected);
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITER, KEYLEN);
        SecretKeyFactory f = SecretKeyFactory.getInstance(ALG);
        return f.generateSecret(spec).getEncoded();
    }
}
