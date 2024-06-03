package com.telifie.Models.Utilities.Network;

import com.telifie.Models.Utilities.Command;
import com.telifie.Models.Result;
import com.telifie.Models.Utilities.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.util.CharsetUtil;
import org.bson.BsonInvalidOperationException;
import org.bson.Document;

public class Https {

    public Https() throws Exception {
        this.start();
    }

    private void start() throws Exception {
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new HttpsServerInitializer(sslCtx));
            Channel ch = b.bind(443).sync().channel();
            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static class HttpsServerInitializer extends ChannelInitializer<SocketChannel> {
        private final SslContext sslCtx;

        public HttpsServerInitializer(SslContext sslCtx) {
            this.sslCtx = sslCtx;
        }

        @Override
        protected void initChannel(SocketChannel ch) {
            ChannelPipeline p = ch.pipeline();
            if (sslCtx != null) {
                p.addLast(sslCtx.newHandler(ch.alloc()));
            }
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
                Result result = new Result(406, query, "NO AUTH PROVIDED");
                if(authHeader != null){
                    Authentication auth = new Authentication(authHeader);
                    if(auth.isAuthenticated()){
                        Session session = new Session(auth.getUser(), "telifie");
                        if(request.method().name().equals("POST")){
                            try {
                                HttpContent content = (HttpContent) msg;
                                String requestBody = content.content().toString(CharsetUtil.UTF_8);
                                result = new Command(query).parseCommand(session, Document.parse(requestBody), request.method().name());
                            }catch(BsonInvalidOperationException e){
                                result = new Result(505, query, "MALFORMED JSON");
                            }
                        }else{
                            result = new Command(query).parseCommand(session, null, request.method().name());
                        }
                    }else{
                        result = new Result(403, query, "INVALID CREDENTIALS");
                    }
                }
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.valueOf(result.getStatusCode()),
                        Unpooled.copiedBuffer(result.toString(), CharsetUtil.UTF_8)
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
                response.headers().set(HttpHeaderNames.CONTENT_SECURITY_POLICY, "default-src 'none'");
                response.headers().set("X-Content-Type-Options", "nosniff");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }
}