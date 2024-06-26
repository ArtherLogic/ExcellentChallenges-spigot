package su.nightexpress.excellentchallenges.data;

import su.nightexpress.excellentchallenges.data.object.ChallengeUser;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentchallenges.ChallengesPlugin;
import su.nightexpress.nightcore.database.AbstractUserManager;

import java.util.UUID;

public class UserManager extends AbstractUserManager<ChallengesPlugin, ChallengeUser> {

    public UserManager(@NotNull ChallengesPlugin plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public ChallengeUser createUserData(@NotNull UUID uuid, @NotNull String name) {
        return new ChallengeUser(plugin, uuid, name);
    }
}
