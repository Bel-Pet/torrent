package message;

import convert.Peer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Bitfield extends Message {
    byte[] bitfield;

    public Bitfield(int length, Peer peer) {
        this.length = length;
        bitfield = new byte[this.length];
        this.peer = peer;
    }

    public void parse(SocketChannel channel) throws IOException {
        int count = 0;
        ByteBuffer data = ByteBuffer.allocate(length);
        while (data.hasRemaining()) {
            count += channel.read(data);
        }
        if (count == -1 || count != length) throw new RuntimeException();

        data.rewind();
        data.get(bitfield);
    }

    @Override
    public byte[] getPayload() {
        return bitfield;
    }
}
