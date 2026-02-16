package ereh.won.otbackend.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "prime.cache")
public class PrimeCacheProperties {
    private Backend backend = Backend.MEMORY;
    private Redis redis = new Redis();
    private Postgres postgres = new Postgres();

    @Getter
    @Setter
    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
    }

    @Getter
    @Setter
    public static class Postgres {
        private String host = "localhost";
        private int port = 5432;
        private String username = "postgres";
        private String password = "postgres";
        private String database = "postgres";
    }

    public enum Backend {
        MEMORY,
        REDIS,
        POSTGRES
    }
}
