package org.dylanpiergies.uberlistener;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UberListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(UberListener.class);

    private static final int PORT_MIN = 0;
    private static final int PORT_MAX = 65535;

    private static final String CONNECT_MESSAGE = "Hello from port {0}.";

    public static void main(final String[] args) throws IOException {
        Selector selector;
        selector = Selector.open();

        for (int port = PORT_MIN; port <= PORT_MAX; port++) {
            ServerSocketChannel channel;
            channel = ServerSocketChannel.open();
            try {
                channel.socket()
                        .bind(new InetSocketAddress(port));
            } catch (final BindException e) {
                LOGGER.info("Couldn't bind to port " + port + ": " + e.getMessage());
                channel.close();
                continue;
            }
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_ACCEPT);
        }

        while (true) {
            int numKeys = 0;
            try {
                numKeys = selector.select();
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
                continue;
            }
            if (numKeys == 0) {
                continue;
            }

            final Set<SelectionKey> keys = selector.selectedKeys();
            keys.stream()
                    .filter(key -> (key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT)
                    .forEach(key -> {
                        final ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                        try {
                            final SocketChannel channel = serverChannel.accept();
                            final int port = ((InetSocketAddress) channel.getLocalAddress()).getPort();
                            final ByteBuffer buffer = ByteBuffer.allocate(1024);
                            final String message = MessageFormat.format(CONNECT_MESSAGE, port);
                            buffer.put(message.getBytes());
                            channel.write(buffer);
                            LOGGER.info(message);
                            channel.close();
                        } catch (final IOException e) {
                            LOGGER.error(e.getMessage(), e);
                            return;
                        }
                    });
            keys.clear();
        }
    }
}
