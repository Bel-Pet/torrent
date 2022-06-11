package message;

import convert.Peer;

public class Interested  extends Message
{
    public Interested(int length, Peer peer)
    {
        this.length = length;
        this.peer = peer;
    }
}
