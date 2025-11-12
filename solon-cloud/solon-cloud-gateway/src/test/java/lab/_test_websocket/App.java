package lab._test_websocket;

import org.noear.solon.Solon;

/**
 *
 * @author noear 2025/11/12 created
 *
 */
public class App {
    public static void main(String[] args) {
        Solon.start(App.class, new String[]{"--cfg=_test_websocket.yml"});
    }
}
