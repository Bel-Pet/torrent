package reactions;

import message.Message;

import java.security.NoSuchAlgorithmException;

public abstract class Reaction {
    public abstract void react(Message message) throws NoSuchAlgorithmException, InterruptedException;
}
