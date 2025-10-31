package maidong.WebSocket;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 行情对接地址：http://39.107.99.235:1008/market
 */
public class Client {
	
	private Channel channel = null;
	private Bootstrap boot = null;
	private Handler handler;
	private EventLoopGroup group = new NioEventLoopGroup();

	private int port = 80;
	private String host = "39.107.99.235";
	
	public Client() {
		
		handler = new Handler(this);
		boot = new Bootstrap();
		boot.group(group)
		.channel(NioSocketChannel.class)
		.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				// TODO Auto-generated method stub
				ChannelPipeline pipeline = ch.pipeline();
				pipeline.addLast("HttpClientCodec", new HttpClientCodec());
				pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(65536));
				//pipeline.addLast("WebSocketCompressionHandler", new WebSocketCompressionHandler(9));
				pipeline.addLast("IdleStateHandler", new IdleStateHandler(0, 10, 180, TimeUnit.SECONDS));
				pipeline.addLast("NettyWebsocketClientHandler", handler);
			}
			
		});
	}
	
	public void doConnect() {
		if(channel != null && channel.isActive()) return;
		try {
			ChannelFuture cf = boot.connect(host, port);
			cf.addListener(new ChannelFutureListener() {
				
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					// TODO Auto-generated method stub
					if(future.isSuccess()) {
						channel = future.channel();
						System.out.println("connect to WebSocket server successfully");
					}else {
						System.out.println("failed to connect to WebSocket server, try after 3s");
						future.channel().eventLoop().schedule(new Runnable(){

							@Override
							public void run() {
								// TODO Auto-generated method stub
								doConnect();
							}
							
						}, 3, TimeUnit.SECONDS);
					}
				}
				
			});
			// handler.handshakeFuture().sync();
			// cf.channel().closeFuture().sync();
		} catch (Exception e) {
			// TODO: handle exception
			//e.printStackTrace();
			e.printStackTrace();
			App.sleep(1000);
			doConnect();
		}
	}
	
	public URI getUri() throws Exception {
		URI uri = new URI("ws://" + host + ":" + port + "/ws");
		return uri;
	}
	
}
