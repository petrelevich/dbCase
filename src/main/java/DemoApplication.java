import org.flywaydb.core.Flyway;



public class DemoApplication {

    public static void flywayMigrations(String url, String user, String password) {
        var flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:/db/migration")
                .load();
        flyway.migrate();
    }
}
