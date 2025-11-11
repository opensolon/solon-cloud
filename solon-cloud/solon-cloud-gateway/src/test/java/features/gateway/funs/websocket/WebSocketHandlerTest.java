package features.gateway.funs.websocket;

import org.junit.jupiter.api.Test;
import org.noear.solon.cloud.gateway.route.RouteFactoryManager;
import org.noear.solon.cloud.gateway.route.RouteHandler;
import org.noear.solon.cloud.gateway.route.handler.WebSocketRouteHandler;
import org.noear.solon.test.SolonTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSocket 路由处理器测试
 * 
 * @author noear
 * @since 3.7.1
 */
@SolonTest
public class WebSocketHandlerTest {
    
    private final WebSocketRouteHandler handler = new WebSocketRouteHandler();
    
    @Test
    public void testWebSocketProtocolSchemas() {
        // 测试 WebSocket 处理器支持的协议类型
        String[] schemas = handler.schemas();
        
        assertNotNull(schemas, "支持的协议列表不应为空");
        assertEquals(2, schemas.length, "WebSocket处理器应支持两种协议");
        assertArrayEquals(new String[]{"ws", "wss"}, schemas, "应支持ws和wss协议");
    }
    
    @Test
    public void testRouteFactoryManagerRegistration() {
        // 测试 WebSocket 处理器在路由工厂管理器中的注册
        RouteHandler wsHandler = RouteFactoryManager.getHandler("ws");
        RouteHandler wssHandler = RouteFactoryManager.getHandler("wss");
        
        assertNotNull(wsHandler, "ws协议处理器应被正确注册");
        assertNotNull(wssHandler, "wss协议处理器应被正确注册");
        assertTrue(wsHandler instanceof WebSocketRouteHandler, "ws处理器应为WebSocketRouteHandler类型");
        assertTrue(wssHandler instanceof WebSocketRouteHandler, "wss处理器应为WebSocketRouteHandler类型");
        assertSame(wsHandler, wssHandler, "ws和wss处理器应为同一个实例");
    }
    
    @Test
    public void testNonWebSocketProtocolHandling() {
        // 测试非WebSocket协议不应由WebSocket处理器处理
        RouteHandler httpHandler = RouteFactoryManager.getHandler("http");
        RouteHandler httpsHandler = RouteFactoryManager.getHandler("https");
        
        assertNotNull(httpHandler, "http协议处理器应存在");
        assertNotNull(httpsHandler, "https协议处理器应存在");
        assertFalse(httpHandler instanceof WebSocketRouteHandler, "http处理器不应是WebSocketRouteHandler类型");
        assertFalse(httpsHandler instanceof WebSocketRouteHandler, "https处理器不应是WebSocketRouteHandler类型");
    }
    
    @Test
    public void testHandlerInstanceCreation() {
        // 测试处理器实例创建
        WebSocketRouteHandler newHandler = new WebSocketRouteHandler();
        
        assertNotNull(newHandler, "WebSocket处理器应能被正确实例化");
        assertArrayEquals(new String[]{"ws", "wss"}, newHandler.schemas(), "新实例的协议支持应与默认实例相同");
    }
    
    @Test
    public void testHandlerSingletonPattern() {
        // 验证处理器在工厂中的单例模式
        RouteHandler handler1 = RouteFactoryManager.getHandler("ws");
        RouteHandler handler2 = RouteFactoryManager.getHandler("wss");
        RouteHandler handler3 = RouteFactoryManager.getHandler("ws");
        
        assertSame(handler1, handler2, "同一协议的不同获取应返回相同实例");
        assertSame(handler1, handler3, "多次获取同一协议应返回相同实例");
    }
}