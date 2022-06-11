package reactions;

import message.Message;
import message.Request;

import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;

public class RequestReaction extends Reaction {
    private final BlockingQueue<Pair<Integer, SocketChannel>> mustReadQueue;

    public RequestReaction(BlockingQueue<Pair<Integer, SocketChannel>> mustReadQueue) {
        this.mustReadQueue = mustReadQueue;
    }

    @Override
    public void react(Message message) {
        Request message_ = (Request) message;
        int pieceIdx = message_.getPieceIdx();
        Pair<Integer, SocketChannel> pair = new Pair<>(pieceIdx, message.getPeer().getChannel());
        try {
            mustReadQueue.put(pair);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
