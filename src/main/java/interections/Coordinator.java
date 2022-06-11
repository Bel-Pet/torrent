package interections;

import convert.Peer;
import convert.Info;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

public class Coordinator implements Runnable {

    private final static int MAX_NUM_REQUESTS = 4;
    private final BlockingQueue<Trio<Integer, byte[], SocketChannel>> readyReadQueue;
    private final BlockingQueue<Integer> readyWriteQueue;
    private final BlockingQueue<Peer> newConnectedPeers;
    private final BlockingQueue<Peer> connectedPeers;
    private final Info info;
    private Thread thread;
    private final Peer ourPeer;
    boolean has_all = false;
    boolean stop = false;

    private final Writer writer;
    private final MessageManager messageManager;

    public Coordinator(BlockingQueue<Peer> connectedPeers, BlockingQueue<Peer> newConnectedPeers, MessageManager messageManager,
                       BlockingQueue<Integer> readyWriteQueue,
                       BlockingQueue<Trio<Integer, byte[], SocketChannel>> readyReadQueue,
                       Peer ourPeer, Writer writer, Info info) {
        this.readyReadQueue = readyReadQueue;
        this.readyWriteQueue = readyWriteQueue;
        this.messageManager = messageManager;
        this.connectedPeers = connectedPeers;
        this.newConnectedPeers = newConnectedPeers;
        this.info = info;
        this.ourPeer = ourPeer;
        this.writer = writer;
    }

    public void start() {
        thread = new Thread(this, "Coordinator");
        thread.start();
    }

    @Override
    public void run() {
        synchronized (newConnectedPeers) {
            try {
                while(newConnectedPeers.isEmpty()) {
                    newConnectedPeers.wait();
                }
            }
            catch (InterruptedException ignored) {}
        }
        while (!stop) {
            Peer newPeer = newConnectedPeers.poll();
            if(newPeer != null) {
                ByteBuffer message = messageManager.generateBitfield(ourPeer.getBitfield(), info.getPiecesCount());
                try {
                    messageManager.sendMessage(newPeer.getChannel(), message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                newPeer.setHasOurBitfield();

                ByteBuffer msg = messageManager.generateUnchoke();
                try {
                    messageManager.sendMessage(newPeer.getChannel(), msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            if (ourPeer.isLeecher()) {
                Integer pieceIdx = readyWriteQueue.poll();
                if (pieceIdx != null) {
                    ourPeer.setHavePiece(pieceIdx);
                    ByteBuffer message = messageManager.generateHave(pieceIdx);
                    for (Peer peer : connectedPeers) {
                        try {
                            messageManager.sendMessage(peer.getChannel(), message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        message.flip();
                    }
                }
            }

            Trio<Integer, byte[], SocketChannel> data = readyReadQueue.poll();
            if (data != null) {
                ByteBuffer message = messageManager.generatePiece(data.second, data.first);
                try {
                    messageManager.sendMessage(data.third, message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (!stop && ourPeer.isLeecher()) {
                has_all = true;

                for (int i = 0; i < info.getPiecesCount(); ++i) {
                    if (!ourPeer.havePiece(i)) {
                        has_all = false;

                        if(!ourPeer.isAskedPiece(i)) {
                            for (Peer peer : connectedPeers) {
                                if(!peer.getChokedMe() && peer.getNumDoneRequestsForTime() <= MAX_NUM_REQUESTS) {
                                    if (peer.havePiece(i)) {
                                        ByteBuffer message = messageManager.generateRequest(i);
                                        try {
                                            messageManager.sendMessage(peer.getChannel(), message);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        peer.increaseNumDoneRequests();
                                        ourPeer.setAskedPiece(i);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (has_all) {
                    stop = true;
                    try {
                        writer.stop();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void stop() {
        stop = true;
        thread.interrupt();
    }
}
