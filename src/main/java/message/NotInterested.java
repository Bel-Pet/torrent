package message;

import convert.Peer;

public class NotInterested extends Message {
    public NotInterested(int length, Peer peer) {
        this.length = length;
        this.peer = peer;
    }
}
