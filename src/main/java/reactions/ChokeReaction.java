package reactions;

import message.Message;

public class ChokeReaction  extends Reaction {
    @Override
    public void react(Message message) {
        message.getPeer().setChokedMe(true);
    }
}
