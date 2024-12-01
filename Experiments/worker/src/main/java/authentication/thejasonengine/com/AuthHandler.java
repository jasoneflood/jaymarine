package authentication.thejasonengine.com;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import authentication.thejasonengine.com.AuthService;

public class AuthHandler {

    /*private static AuthService authService = new AuthService();

    public static void handleLogin(RoutingContext context) {
        JsonObject body = context.getBodyAsJson();
        
        
        JWTAuthOptions options = new JWTAuthOptions().setJwks(null).setJWTKey("mysecretkey"); // Define a shared secret for signing JWT tokens

        // Setup JWTAuth with a secret key
        jwtAuth = JWTAuth.create(vertx, options);

        if (body != null) {
            String username = body.getString("username");
            String password = body.getString("password");

            // Validate the credentials via AuthService
            boolean isValid = authService.validateLogin(username, password);

            if (isValid) {
                context.response()
                    .putHeader("Content-Type", "application/json")
                    .end("{\"message\": \"Login successful!\"}");
            } else {
                context.response()
                    .setStatusCode(401)
                    .putHeader("Content-Type", "application/json")
                    .end("{\"message\": \"Invalid credentials\"}");
            }
        } else {
            context.response()
                .setStatusCode(400)
                .putHeader("Content-Type", "application/json")
                .end("{\"message\": \"Invalid request body\"}");
        }
    }*/
}