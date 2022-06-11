package message;

import convert.Peer;

public class Unchoke  extends Message {
    public Unchoke(int length, Peer peer) {
        this.length = length;
        this.peer = peer;
    }
}
