package reactions;

import message.Have;
import message.Message;

public class HaveReaction  extends Reaction {

    @Override
    public void react(Message message) {
        Have message_ = (Have) message;
        int pieceIdx = message_.getPieceIdx();
        message.getPeer().setHavePiece(pieceIdx);
    }
}