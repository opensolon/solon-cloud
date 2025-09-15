package demo;

import org.noear.solon.annotation.Component;
import org.noear.solon.cloud.model.BreakerException;
import org.noear.solon.cloud.service.CloudBreakerService;

/**
 *
 * @author noear 2025/9/15 created
 *
 */
@Component
public class CloudBreakerServiceImpl implements CloudBreakerService {
    @Override
    public AutoCloseable entry(String breakerName) throws BreakerException {
        return null;
    }
}
