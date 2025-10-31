package maidong.WebSocket;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.internal.StringUtil;

/**
 * 行情对接地址：http://39.107.99.235:1008/market
 */
@ChannelHandler.Sharable
public class Handler extends SimpleChannelInboundHandler<Object>{
	
	private Client client;
	private WebSocketClientHandshaker handshaker;
	
	public Handler(Client client) {
		this.client = client;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		// TODO Auto-generated method stub
		//判断握手是否完成，如果没有完成，则手动完成
		final Channel ch = ctx.channel();
		if(!handshaker.isHandshakeComplete()) {
			handshaker.finishHandshake(ch, (FullHttpResponse)msg);

			//握手完成后，订阅产品
			JSONObject event = new JSONObject();
			event.put("Key", "btcusdt,ethusdt");
			ctx.writeAndFlush(new TextWebSocketFrame(event.toJSONString()));

			return;
		}

		if(msg instanceof TextWebSocketFrame) {
			TextWebSocketFrame text = (TextWebSocketFrame)msg;
			String str = text.text();
			JSONObject json = JSONObject.parseObject(str);

			if (!StringUtil.isNullOrEmpty(json.getString("body"))) {
				JSONObject body = JSONObject.parseObject(json.getString("body"));

				String StockCode = body.getString("StockCode");
				double Price = body.getDoubleValue("Price");
				double Open = body.getDoubleValue("Open");
				double LastClose = body.getDoubleValue("LastClose");
				double High = body.getDoubleValue("High");
				double Low = body.getDoubleValue("Low");
				double Diff = body.getDoubleValue("Diff");
				double DiffRate = body.getDoubleValue("DiffRate");
				double BP1 = body.getDoubleValue("BP1");
				double BV1 = body.getDoubleValue("BV1");
				double SP1 = body.getDoubleValue("SP1");
				double SV1 = body.getDoubleValue("SV1");
				double TotalVol = body.getDoubleValue("TotalVol");
				String Time = body.getString("Time");
				String LastTime = body.getString("LastTime");

				JSONArray BS = body.getJSONArray("BS");
				JSONArray Depth = body.getJSONArray("Depth");

				//处理业务逻辑.....

				System.out.println(StockCode);
			}


		} else if(msg instanceof BinaryWebSocketFrame) {
			System.out.println("Client accept BinaryWebSocketFrame msg");
		} else if(msg instanceof PongWebSocketFrame) {
			System.out.println("Client accept PongWebSocketFrame msg");
		}else if(msg instanceof PingWebSocketFrame) {
			System.out.println("Client accept PingWebSocketFrame msg");
			ctx.writeAndFlush(new PongWebSocketFrame());
		}else if(msg instanceof CloseWebSocketFrame) {
			CloseWebSocketFrame close = (CloseWebSocketFrame)msg;
			ByteBuf content = close.content();
			byte [] data = new byte[content.readableBytes()-2];
			content.getBytes(2, data);
			String str = new String(data);
			System.out.println("Client accept CloseWebSocketFrame msg:" + str);
		}
	}

	/**
	 * 握手连接
	 * @param ctx
	 * @throws Exception
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		handshaker = WebSocketClientHandshakerFactory.newHandshaker(
				client.getUri(), WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), 655360);
		handshaker.handshake(ctx.channel());
	}

	/**
	 * 连接断开
	 * @param ctx
	 * @throws Exception
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// TODO Auto-generated method stub
		super.channelInactive(ctx);
		System.out.println("Websocket Client disconnected");
		ctx.executor().schedule(new Runnable() {
			
			public void run() {
				// TODO Auto-generated method stub
				client.doConnect();
			}
		}, 1000, TimeUnit.MILLISECONDS);
	}

	/**
	 * 异常处理
	 * @param ctx
	 * @param cause
	 * @throws Exception
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Auto-generated method stub
		cause.printStackTrace();
		ctx.close();
	}

	/**
	 * 心跳
	 * @param ctx
	 * @param evt
	 * @throws Exception
	 */
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		// TODO Auto-generated method stub
		if(evt instanceof IdleStateEvent) {
			IdleStateEvent e = (IdleStateEvent)evt;
			if(e.state() == IdleState.READER_IDLE) {
				throw new Exception("Websocket Read time out");
			}else if(e.state() == IdleState.WRITER_IDLE) {
				String stamp = String.valueOf(new Date().getTime()/1000);
				JSONObject event = new JSONObject();
				event.put("ping", stamp);
				ctx.writeAndFlush(new TextWebSocketFrame(event.toJSONString()));
			}
		}else {
			super.userEventTriggered(ctx, evt);
		}
	}
}
