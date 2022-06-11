package interections;

public class Handshake
{
    private final static int PROTOCOL_NAME_LENGTH = 19;
    private final static int HASH_LENGTH = 20;
    private final static int PEER_ID_LENGTH = 20;

    private final byte[] peerID;
    private final byte[] infoHash;
    private final byte[] protocolName;
    private final int protocolNameLength;

    public Handshake() {
        this.protocolNameLength = PROTOCOL_NAME_LENGTH;
        this.infoHash = new byte[HASH_LENGTH];
        this.protocolName = new byte[PROTOCOL_NAME_LENGTH];
        this.peerID = new byte[20];
    }

    public void setPeerID(byte[] peerID) {
        System.arraycopy(peerID, 0, this.peerID, 0, PEER_ID_LENGTH);
    }

    public void setInfoHash(byte[] infoHash) {
        System.arraycopy(infoHash, 0, this.infoHash, 0, HASH_LENGTH);
    }

    public void setProtocolName(byte[] protocolName) {
        System.arraycopy(protocolName, 0, this.protocolName, 0, protocolNameLength);
    }

    public byte[] getPeerID() {
        return peerID;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public byte[] getProtocolName() {
        return protocolName;
    }

    public int getProtocolNameLength() {
        return protocolNameLength;
    }
}

