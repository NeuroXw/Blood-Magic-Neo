package wayoftime.bloodmagic.will;

/** Last aura snapshot from the server (upstream ClientHandler.currentAura). No client-only classes. */
public final class ClientAura {
    private ClientAura() {}
    public static volatile DemonWillHolder current;
}
