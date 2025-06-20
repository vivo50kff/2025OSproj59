// LibraryVerification.java
import org.apache.commons.math3.util.Pair;

public class LibraryVerification {
    public static void main(String[] args) {
        try {
            Pair<String, Integer> test = new Pair<>("test", 1);
            System.out.println("✅ Apache Commons Math loaded successfully!");
            System.out.println("Test pair: " + test.getFirst() + ", " + test.getSecond());
        } catch (Exception e) {
            System.out.println("❌ Library loading failed: " + e.getMessage());
        }
    }
}