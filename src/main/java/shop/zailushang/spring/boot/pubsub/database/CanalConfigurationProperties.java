package shop.zailushang.spring.boot.pubsub.database;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "canal")
public class CanalConfigurationProperties {
    private String serverHost;
    private Integer serverPort;
    private String destination;
    private String username;
    private String password;
    private String subscribeFilter;
}