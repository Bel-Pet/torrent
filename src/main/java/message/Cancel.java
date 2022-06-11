package message;

import convert.Peer;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Cancel  extends Message {
    private final int length;
    private int pieceIdx;

    public Cancel(int length, Peer peer) {
        this.length = length;
        this.peer = peer;
    }

    public void parse(SocketChannel channel) throws IOException {
        int count = 0;
        ByteBuffer idx = ByteBuffer.allocate(4);
        while (idx.hasRemaining()) {
            count += channel.read(idx);
        }
        if (count == -1 || count != length) throw new RuntimeException();

        idx.rewind();
        pieceIdx = idx.getInt();
    }

    public int getPieceIdx()
    {
        return pieceIdx;
    }
}
