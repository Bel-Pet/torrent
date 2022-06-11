package message;

import convert.Peer;

public class Choke extends Message {
    public Choke(int length, Peer peer) {
        this.length = length;
        this.peer = peer;
    }
}
