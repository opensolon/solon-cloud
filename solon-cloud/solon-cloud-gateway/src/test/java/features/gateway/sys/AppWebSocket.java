package features.gateway.sys;

import org.noear.solon.net.annotation.ServerEndpoint;
import org.noear.solon.net.websocket.WebSocket;
import org.noear.solon.net.websocket.listener.SimpleWebSocketListener;

import java.io.IOException;

/**
 *
 * @author noear 2026/5/12 created
 *
 */
@ServerEndpoint("/ws")
public class AppWebSocket extends SimpleWebSocketListener {
    @Override
    public void onMessage(WebSocket socket, String text) throws IOException {
        socket.send("收到：" + text);
    }
}
