package lab.app1;

import org.noear.solon.net.annotation.ServerEndpoint;
import org.noear.solon.net.websocket.WebSocket;
import org.noear.solon.net.websocket.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author noear 2025/11/12 created
 *
 */
@ServerEndpoint
public class DemoWebSocket implements WebSocketListener {
    static Logger log = LoggerFactory.getLogger(DemoWebSocket.class);

    @Override
    public void onOpen(WebSocket socket) {
        log.warn("onOpen: {}", "it's ok");
    }

    @Override
    public void onMessage(WebSocket socket, String text) throws IOException {
        log.warn("onMessage: {}", text);
    }

    @Override
    public void onMessage(WebSocket socket, ByteBuffer binary) throws IOException {

    }

    @Override
    public void onClose(WebSocket socket) {
        log.warn("close");
    }

    @Override
    public void onError(WebSocket socket, Throwable error) {
        log.error(error.getMessage(), error);
    }
}
