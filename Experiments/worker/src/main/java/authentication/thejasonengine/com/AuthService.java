package authentication.thejasonengine.com;

public class AuthService {

    // Dummy users (In a real app, this could be replaced with a database or external service)
    private static final String VALID_USERNAME = "user";
    private static final String VALID_PASSWORD = "password123";

    public boolean validateLogin(String username, String password) {
        // Check if the provided credentials match the valid ones
        return VALID_USERNAME.equals(username) && VALID_PASSWORD.equals(password);
    }
}