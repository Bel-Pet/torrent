package reactions;

import message.Message;

import java.util.BitSet;

public class BitfieldReaction extends Reaction {

    @Override
    public void react(Message message) {
        BitSet bitfield = BitSet.valueOf(message.getPayload());
        message.getPeer().setBitfield(bitfield);
    }
}
