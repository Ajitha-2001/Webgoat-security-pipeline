import java.sql.Connection;
import java.sql.Statement;

public class SemgrepSecurityGateDemo {

    public void unsafeQuery(Connection connection, String username) throws Exception {
        Statement statement = connection.createStatement();

        String query =
            "SELECT * FROM users WHERE username = '" + username + "'";

        statement.executeQuery(query);
    }
}
