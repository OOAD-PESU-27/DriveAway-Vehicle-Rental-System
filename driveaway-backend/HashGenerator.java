import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Generates bcrypt hash for staff password.
 * Run this in driveaway-backend to get a valid hash.
 */
public class HashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "password123";
        String hash = encoder.encode(password);
        
        System.out.println("\n========================================");
        System.out.println("PASSWORD HASH GENERATOR");
        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("Hash:     " + hash);
        System.out.println("========================================\n");
        System.out.println("Use this hash in MongoDB for staff user:");
        System.out.println("  password: \"" + hash + "\"");
        System.out.println("\n");
    }
}
