package interections;

import convert.Peer;
import convert.Info;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Connection {

    private static final int MIN_PORT = 2323;
    private static final int MAX_PORT = 2333;


    private ServerSocketChannel serverSocket;
    private final BlockingQueue<Peer> connectedPeers;
    private final BlockingQueue<Peer> newConnectedPeers;
    private final Info info;
    private final Peer ourPeer;

    private Thread thread;
    private final Lock selectorRegisterLock = new ReentrantLock();
    private final MessageManager messageManager;
    private final Selector selector;
    private boolean stop;

    public Connection(MessageManager messageManager, BlockingQueue<Peer> connectedPeers, BlockingQueue<Peer> newConnectedPeers, Peer ourPeer, Info info) {
        for (int port = MIN_PORT; port <= MAX_PORT; ++port) {
            try {
                serverSocket = ServerSocketChannel.open();
                serverSocket.bind(new InetSocketAddress(port));
                ourPeer.setPort(port);

                String peerID = "1234567890123456" + port;
                ourPeer.setPeerID(peerID.getBytes(StandardCharsets.US_ASCII));
                System.out.println("Listen port " + port);
                System.out.println("PeerID: " + peerID);
            }
            catch (IOException e) {
                continue;
            }
            break;
        }
        if (serverSocket == null) throw new RuntimeException();

        this.messageManager = messageManager;
        this.connectedPeers = connectedPeers;
        this.newConnectedPeers = newConnectedPeers;
        this.info = info;
        this.ourPeer = ourPeer;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public void connectTo(Peer peer) {
        try {
            SocketChannel client = SocketChannel.open(new InetSocketAddress(peer.getIp(), peer.getPort()));
            messageManager.createHandshake(ourPeer.getPeerID(), info.getHandshakeHash());
            messageManager.sendHandshake(client);
            Handshake clientHandshake = messageManager.receiveHandshake(client);
            if (!messageManager.isValidHandshake(clientHandshake, peer.getPeerID())) {
                dropConnection(client);
                return;
            }

            peer.setChannel(client);
            try {
                client.setOption(StandardSocketOptions.TCP_NODELAY, true);
                client.configureBlocking(false);
                selectorRegisterLock.lock();
                try {
                    selector.wakeup();
                    client.register(selector, SelectionKey.OP_READ, peer);
                } finally {
                    selectorRegisterLock.unlock();
                }
                connectedPeers.put(peer);
                synchronized (newConnectedPeers) {
                    newConnectedPeers.put(peer);
                    newConnectedPeers.notify();
                }
            } catch (IOException e) {
                dropConnection(client);
            } catch (InterruptedException ignored){}
        } catch (IOException ignored) {}
    }

    public void proccessConnection(SelectionKey connectionKey) {
        SocketChannel client;
        try {
            ServerSocketChannel acceptor = (ServerSocketChannel) connectionKey.channel();
            client = acceptor.accept();
            messageManager.createHandshake(ourPeer.getPeerID(), info.getHandshakeHash());
            Handshake clientHandshake = messageManager.receiveHandshake(client);

            if (!messageManager.isValidHandshake(clientHandshake)) {
                dropConnection(client);
            }
            messageManager.sendHandshake(client);

            Peer peer = new Peer();
            peer.setChannel(client);
            peer.setPeerID(clientHandshake.getPeerID());

            try {
                client.setOption(StandardSocketOptions.TCP_NODELAY, true);
                client.configureBlocking(false);
                selectorRegisterLock.lock();
                try {
                    selector.wakeup();
                    client.register(selector, SelectionKey.OP_READ, peer);
                }
                finally {
                    selectorRegisterLock.unlock();
                }
                connectedPeers.put(peer);
                synchronized (newConnectedPeers) {
                    newConnectedPeers.put(peer);
                    newConnectedPeers.notify();
                }
            } catch (IOException e) {
                dropConnection(client);
            }
            catch (InterruptedException ignored){}
        }
        catch (IOException ignored) {}
    }

    public void dropConnection(SocketChannel channel) throws IOException {
        channel.close();
    }

    public void processIncomingConnectionsAndMessages() {
        thread = new Thread(() -> {
            try {
                serverSocket.configureBlocking(false);
                serverSocket.register(selector, SelectionKey.OP_ACCEPT);
            } catch (IOException e) {
                System.exit(1);
            }
            while (!stop) {
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();

                    if (key.isAcceptable()) {
                        proccessConnection(key);
                    }
                    if (key.isReadable()) {
                        try {
                            messageManager.receiveMessage((SocketChannel) key.channel(), (Peer) key.attachment());
                        }
                        catch (IOException e) {
                            synchronized (connectedPeers) {
                                connectedPeers.remove(key.attachment());
                                connectedPeers.notify();
                            }
                            try {
                                key.channel().close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                            key.cancel();
                        } catch (NoSuchAlgorithmException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if(!key.isValid()) {
                        try {
                            key.channel().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        key.cancel();
                    }
                    iterator.remove();
                }
            }
        }, "Input listener");

        thread.start();
    }

    public void stop() throws IOException {
        stop = true;
        thread.interrupt();
        for(Peer peer: connectedPeers) {
            peer.getChannel().close();
        }
    }
}
