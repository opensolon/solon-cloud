package demo.rocketmq5;

import org.apache.rocketmq.client.apis.SessionCredentials;
import org.apache.rocketmq.client.apis.SessionCredentialsProvider;

//这个实现类，（相对于 StaticSessionCredentialsProvider）方便配置自动注入
public class SessionCredentialsProviderImpl implements SessionCredentialsProvider {
    private String accessKey;
    private String accessSecret;
    private String securityToken;

    private SessionCredentials sessionCredentials;

    @Override
    public SessionCredentials getSessionCredentials() {
        if (sessionCredentials == null) {
            if (securityToken == null) {
                sessionCredentials = new SessionCredentials(accessKey, accessSecret);
            } else {
                sessionCredentials = new SessionCredentials(accessKey, accessSecret, securityToken);
            }
        }

        return sessionCredentials;
    }
}