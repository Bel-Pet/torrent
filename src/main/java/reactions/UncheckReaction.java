package reactions;

import message.Message;

public class UncheckReaction extends Reaction {
    @Override
    public void react(Message message) {
        message.getPeer().setChokedMe(false);
    }
}