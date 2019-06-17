package com.wing.netty.tomcat;

import com.wing.netty.tomcat.http.WingRequest;
import com.wing.netty.tomcat.http.WingResponse;
import com.wing.netty.tomcat.http.WingServlet;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @author wing
 * @date 2019-06-16 12:16
 */
//Netty就是一个同时支持多协议的网络通信框架
public class WingTomcat {
    private int port;
    private Properties webxml = new Properties();
    private Map<String, WingServlet> servletMapping = new HashMap<String, WingServlet>();
    //1.配置Tomcat端口
    //2:配置web.xml 在web.xml中 编写 servlet-name  servlet-class
    //3:读取配置文件 url-pattern 和 servlet建立一个对应关系 保存在servletMapping 中
    //4:接收http请求  一串有规律的字符串
    //5:将收到的http请求封装层 request  response 对象
    //6: 从request 对象中获取url  并根据url 从servletMapping 中获取访问的servlet对象
    //7:调用对象中的service
    //8:将结果返回

    public void init(int port) {
        this.port = port;
        try {
            String path = this.getClass().getResource("/").getPath();
            FileInputStream fis = new FileInputStream(path + "web.properties");
            webxml.load(fis);
            for (Object k : webxml.keySet()) {
                String key = k.toString();
                if (key.endsWith(".url")) {
                    String servletName = key.replaceAll("\\.url$", "");
                    String url = (String) webxml.getProperty(key);
                    String className = (String) webxml.getProperty(servletName + ".className");
                    WingServlet servlet = (WingServlet) Class.forName(className).newInstance();
                    servletMapping.put(url, servlet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(int port) {
        init(port);
        //Netty封装了NIO，Reactor模型，Boss，worker
        //boss线程
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        //worker 线程
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // Netty服务
            //ServetBootstrap   ServerSocketChannel
            ServerBootstrap server = new ServerBootstrap();
            // 链路式编程
            server.group(bossGroup, workerGroup)
                    // 主线程处理类,看到这样的写法，底层就是用反射
                    .channel(NioServerSocketChannel.class)
                    // 子线程处理类 , Handler
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel client) throws Exception {
                            // 无锁化串行编程
                            //Netty对HTTP协议的封装，顺序有要求
                            // HttpResponseEncoder 编码器
                            client.pipeline().addLast(new HttpResponseEncoder());
                            // HttpRequestDecoder 解码器
                            client.pipeline().addLast(new HttpRequestDecoder());
                            // 业务逻辑处理
                            client.pipeline().addLast(new WingTomcatHandler());
                        }
                    })
                    // 针对主线程的配置 分配线程最大数量 128
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 针对子线程的配置 保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // 启动服务器
            ChannelFuture f = server.bind(port).sync();
            System.out.println("wing Tomcat 已启动，监听的端口是：" + port);
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            // 关闭线程池
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new WingTomcat().start(8090);
    }

    private class WingTomcatHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof HttpRequest) {
                HttpRequest req = (HttpRequest) msg;
                WingRequest request = new WingRequest(ctx, req);
                WingResponse response = new WingResponse(ctx, req);
                String url = request.getUrl();
                if (servletMapping.containsKey(url)) {
                    servletMapping.get(url).service(request, response);
                } else {
                    response.write("404 - Not Found ");
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            super.exceptionCaught(ctx, cause);
        }
    }

}
