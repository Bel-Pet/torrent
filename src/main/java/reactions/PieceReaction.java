package reactions;


import convert.Info;
import convert.Peer;
import message.Message;
import message.Piece;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

public class PieceReaction extends Reaction {
    private final BlockingQueue<Pair<Integer, byte[]>> mustWriteQueue;
    private final Info torrentInfo;
    private final Peer ourPeer;

    public PieceReaction(BlockingQueue<Pair<Integer, byte[]>> mustWriteQueue, Info torrentInfo, Peer ourPeer) {
        this.mustWriteQueue = mustWriteQueue;
        this.torrentInfo = torrentInfo;
        this.ourPeer = ourPeer;
    }

    public void react(Message msg) throws NoSuchAlgorithmException, InterruptedException {
        Piece message = (Piece) msg;
        int pieceIdx = message.getPieceIdx();
        byte[] piece = message.getPiece();
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            md.update(piece);
            if(Arrays.equals(md.digest(), torrentInfo.getPieceHash(pieceIdx))) {
                mustWriteQueue.put(new Pair<>(pieceIdx, Arrays.copyOf(piece, piece.length)));
            } else {
                ourPeer.clearAskedPiece(pieceIdx);
            }
            message.getPeer().decreaseNumDoneRequestsForTime();
    }
}
