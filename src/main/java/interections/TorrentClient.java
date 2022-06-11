package interections;

import convert.*;
import reactions.*;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class TorrentClient {
    private final static int  MAX_NUM_PEERS = 10;

    private Connection connection;
    private final UserCommunication userInteraction;
    private final MessageManager messageManager;
    private Coordinator coordinator;
    private final Reader reader;
    private final Writer writer;

    private final Peer ourPeer;
    private final BlockingQueue<Peer> peers;
    private final BlockingQueue<Peer> connectedPeers;
    private final BlockingQueue<Peer> newConnectedPeers;
    private final Info info;
    private final Thread mainThread;
    private  boolean stop;

    public TorrentClient() {
        ourPeer = new Peer();
        mainThread = Thread.currentThread();
        peers = new LinkedBlockingQueue<>();
        connectedPeers = new LinkedBlockingQueue<>();
        newConnectedPeers = new LinkedBlockingQueue<>();
        userInteraction = new UserCommunication(this, peers);
        info = new Info();
        reader = new Reader();
        writer = new Writer();

        HashMap<Byte, Reaction> messageReactions = new HashMap<>();
        messageReactions.put((byte)0, new ChokeReaction());
        messageReactions.put((byte)1, new UncheckReaction());
        messageReactions.put((byte)2, new InterestedReaction());
        messageReactions.put((byte)3, new NotInterestedReaction());
        messageReactions.put((byte)4, new HaveReaction());
        messageReactions.put((byte)5, new BitfieldReaction());
        messageReactions.put((byte)6, new RequestReaction(reader.getMustReadQueue()));
        messageReactions.put((byte)7, new PieceReaction(writer.getMustWriteQueue(), info, ourPeer));
        messageReactions.put((byte)8, new CancelReaction(reader.getMustReadQueue()));
        messageManager = new MessageManager(messageReactions);
    }

    public void execute() throws IOException {
        PeerBehaviour ourPeerBehaviour = userInteraction.getInfoAboutOurPeer();
        if (ourPeerBehaviour.getAction() == null) {
            return;
        }
        switch (ourPeerBehaviour.getAction()) {
            case "-c" -> createTorrentFile(ourPeerBehaviour);
            case "-s" -> sendPeer(ourPeerBehaviour);
            case "-p" -> download(ourPeerBehaviour);
            default -> {
                System.out.println("Error: wrong input.");
                System.out.println("Some problem happened. For more information look in a log file. Torrent client is terminated.");
            }
        };
    }

    private void createTorrentFile(PeerBehaviour ourPeerBehaviour) {
        String pathToFile = ourPeerBehaviour.getPathToFile();
        String pathToTorrent = ourPeerBehaviour.getPathToTorrent();
        try {
            TorrentFile.create(pathToFile, pathToTorrent);
        } catch (IOException e) {
            System.out.println("Can't create torrent file");
        }
    }

    private void sendPeer(PeerBehaviour ourPeerBehaviour) throws IOException {
        String pathToTorrent = ourPeerBehaviour.getPathToTorrent();
        try(DataInputStream torrentFile = new DataInputStream(new FileInputStream(pathToTorrent))) {
            Parser.parseTorrent(torrentFile, info);
        } catch (IOException e) {
            System.out.println("Can't parse torrent file");
            return;
        }
        ourPeer.setSeeder(true);
        BitSet bitfield = new BitSet(info.getPiecesCount());

        for(int i = 0; i < info.getPiecesCount(); ++i) {
            bitfield.set(i);
        }

        ourPeer.setBitfield(bitfield);
        start(info, ourPeerBehaviour.getPathToFile());
    }

    private void download(PeerBehaviour ourPeerBehaviour) throws IOException {
        String pathToTorrent = ourPeerBehaviour.getPathToTorrent();
        try(DataInputStream torrentFile = new DataInputStream(new FileInputStream(pathToTorrent))) {
            Parser.parseTorrent(torrentFile, info);
        } catch (IOException e) {
            System.out.println("Can't parse torrent file");
            return;
        }

        ourPeer.setLeecher(true);
        String pathToFile = ourPeerBehaviour.getPathToFile() ;
        BitSet bitfield = new BitSet(info.getPiecesCount());
        ourPeer.setBitfield(bitfield);
        ourPeer.setCountPieces(info.getPiecesCount());
        start(info, pathToFile);

        Thread thread = new Thread(userInteraction);
        thread.start();

        try {
            while(!stop) {
                Peer peerToConnect = peers.take();
                connection.connectTo(peerToConnect);

                synchronized (connectedPeers) {
                    while (connectedPeers.size() == MAX_NUM_PEERS) {
                        connectedPeers.wait();
                    }
                }
            }
        }
        catch (InterruptedException ignored) {}
    }

    public void start(Info info, String pathToFile) throws IOException {

        if(ourPeer.isLeecher()) writer.initiate(pathToFile, info.getPieceLength());

        reader.initiate(pathToFile,
                info.getFileLength(),
                info.getPieceLength(),
                info.getPiecesCount());

        coordinator = new Coordinator(connectedPeers,
                newConnectedPeers,
                messageManager,
                writer.getReadyWriteQueue(),
                reader.getReadyReadQueue(),
                ourPeer, writer, info);

        connection = new Connection(messageManager,
                connectedPeers,
                newConnectedPeers,
                ourPeer,
                info);

        connection.processIncomingConnectionsAndMessages();
        coordinator.start();
        reader.start();
        if(ourPeer.isLeecher()) {
            writer.start();
        }
    }

    public void stop() throws IOException {
        stop = true;
        connection.stop();
        coordinator.stop();
        if(ourPeer.isLeecher()) writer.stop();
        reader.stop();
        mainThread.interrupt();
    }
}
