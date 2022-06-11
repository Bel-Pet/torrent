package interections;

import convert.Peer;
import convert.PeerBehaviour;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

class UserCommunication implements Runnable {

    private final TorrentClient torrentClient;
    private final BlockingQueue<Peer> peers;
    boolean stop;

    UserCommunication(TorrentClient torrentClient, BlockingQueue<Peer> peers) {
        this.torrentClient = torrentClient;
        this.peers = peers;
    }

    public PeerBehaviour getInfoAboutOurPeer() {
        Scanner scanner = new Scanner(System.in);
        String action = scanner.next();
        PeerBehaviour ourPeerBehaviour = new PeerBehaviour();
        ourPeerBehaviour.setPathToTorrent(scanner.next());
        ourPeerBehaviour.setAction(action);
        ourPeerBehaviour.setPathToFile(scanner.next());
        return ourPeerBehaviour;
    }

    @Override
    public void run() {
        while (!stop) {
            Scanner scanner = new Scanner(System.in);
            String input = scanner.next();
            if (input.equals("stop")) {
                stop = true;
                try {
                    torrentClient.stop();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    Peer peer = new Peer();
                    peer.setPeerID(input.getBytes(StandardCharsets.US_ASCII));
                    peer.setIp(InetAddress.getByName(scanner.next()));
                    peer.setPort(scanner.nextInt());
                    peers.put(peer);
                } catch (UnknownHostException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
