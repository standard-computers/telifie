package com.telifie.Models.Utilities.Servers;

import com.telifie.Models.Actions.Command;
import com.telifie.Models.Clients.AuthenticationClient;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.Authentication;
import com.telifie.Models.Utilities.Event;
import com.telifie.Models.Utilities.Log;
import com.telifie.Models.Utilities.Session;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;

public class Http {

    public Http() throws Exception {
        this.start();
    }

    private void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new HttpServerInitializer());
            Channel ch = b.bind(80).sync().channel();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            p.addLast(new HttpServerCodec());
            p.addLast(new HttpObjectAggregator(1048576));
            p.addLast(new HttpRequestHandler());
        }
    }

    private static class HttpRequestHandler extends SimpleChannelInboundHandler<Object> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof FullHttpRequest request) {
                String authHeader = request.headers().get(HttpHeaderNames.AUTHORIZATION);
                String query = new QueryStringDecoder(request.uri()).path().substring(1);
                Log.out(Event.Type.valueOf(request.method().toString()), "INBOUND HTTP REQUEST : " + ctx.channel().remoteAddress().toString() + "/" + query, "HTTx057");
                Result result = new Result(406, "NO AUTH PROVIDED");
                if(authHeader != null){
                    AuthenticationClient auths = new AuthenticationClient();
                    Authentication auth = new Authentication(authHeader);
                    if(auths.isAuthenticated(auth)){
                        Session session = new Session(auth.getUser(), "telifie");
                        if(request.method().name().equals("POST")){
                            try {
                                HttpContent content = (HttpContent) msg;
                                String requestBody = content.content().toString(CharsetUtil.UTF_8);
                                result = new Command(query).parseCommand(session, Document.parse(requestBody));
                            }catch(BsonInvalidOperationException e){
                                result = new Result(505, "MALFORMED JSON");
                            }
                        }else if(request.method().name().equals("GET")){
                            result = new Command(query).parseCommand(session, null);
                        }else{
                            result = new Result(404, query, "INVALID METHOD");
                        }
                    }else{
                        result = new Result(403, "INVALID CREDENTIALS");
                    }
                }
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(result.getStatusCode()), Unpooled.copiedBuffer(result.toString(), CharsetUtil.UTF_8));
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}