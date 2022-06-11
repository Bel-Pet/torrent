package message;

import convert.Peer;

public class MessageFactory {
    public Message create(int id, int length, Peer peer) {
        return switch (id) {
            case 0 -> new Choke(length, peer);
            case 1 -> new Unchoke(length, peer);
            case 2 -> new Interested(length, peer);
            case 3 -> new NotInterested(length, peer);
            case 4 -> new Have(length, peer);
            case 5 -> new Bitfield(length, peer);
            case 6 -> new Request(length, peer);
            case 7 -> new Piece(length, peer);
            case 8 -> new Cancel(length, peer);
            default -> null;
        };
    }
}
