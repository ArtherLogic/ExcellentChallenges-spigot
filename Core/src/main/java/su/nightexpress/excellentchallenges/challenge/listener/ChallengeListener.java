package su.nightexpress.excellentchallenges.challenge.listener;

import su.nightexpress.excellentchallenges.challenge.GeneratedChallenge;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nexmedia.engine.api.manager.AbstractListener;
import su.nexmedia.engine.utils.PDCUtil;
import su.nightexpress.excellentchallenges.ExcellentChallengesPlugin;
import su.nightexpress.excellentchallenges.Keys;
import su.nightexpress.excellentchallenges.api.event.PlayerChallengeCompleteEvent;
import su.nightexpress.excellentchallenges.api.event.PlayerChallengeObjectiveEvent;
import su.nightexpress.excellentchallenges.challenge.ChallengeManager;
import su.nightexpress.excellentchallenges.challenge.ChallengeCategory;
import su.nightexpress.excellentchallenges.config.Config;
import su.nightexpress.excellentchallenges.config.Lang;
import su.nightexpress.excellentchallenges.data.object.ChallengeUser;

public class ChallengeListener extends AbstractListener<ExcellentChallengesPlugin> {

    public ChallengeListener(@NotNull ChallengeManager challengeManager) {
        super(challengeManager.plugin());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoin(@NotNull PlayerJoinEvent e) {
        Player player = e.getPlayer();
        plugin.getChallengeManager().updateChallenges(player, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChallengeObjectiveEvent(PlayerChallengeObjectiveEvent e) {
        Player player = e.getPlayer().getPlayer();
        if (player == null) return;

        GeneratedChallenge progress = e.getChallenge();
        String objId = e.getObjective();
        plugin.getMessage(Lang.CHALLENGE_NOTIFY_PROGRESS)
            .replace(progress.replacePlaceholders(objId))
            .send(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChallengeCompleteEvent(PlayerChallengeCompleteEvent e) {
        Player player = e.getPlayer().getPlayer();
        if (player == null) return;

        GeneratedChallenge challenge = e.getChallenge();
        ChallengeUser user = e.getUser();
        ChallengeCategory type = challenge.getType();

        challenge.getRewards().forEach(reward -> reward.give(player));

        user.addCompletedChallenge(challenge);

        if (user.isAllChallengesCompleted(type)) {
            type.getCompletionRewards().forEach(reward -> reward.give(player));
        }

        plugin.getMessage(Lang.CHALLENGE_NOTIFY_COMPLETED)
            .replace(challenge.replacePlaceholders())
            .send(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChallengeEntitySpawn(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        if (Config.OBJECTIVES_ANTI_GLITCH_ENTITY_SPAWN_REASONS.get().contains(reason)) {
            PDCUtil.set(e.getEntity(), Keys.ENTITY_TRACKED, true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCake(PlayerInteractEvent event) {
        if (event.useInteractedBlock() == Event.Result.DENY) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CAKE) return;

        Player player = event.getPlayer();
        PlayerItemConsumeEvent consumeEvent = new PlayerItemConsumeEvent(player, new ItemStack(Material.CAKE), EquipmentSlot.HAND);
        this.plugin.getPluginManager().callEvent(consumeEvent);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChallengeInventoryHandler(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory.getType() != InventoryType.BREWING) return;

        BrewerInventory brewerInventory = (BrewerInventory) inventory;
        BrewingStand stand = brewerInventory.getHolder();
        if (stand == null) return;

        boolean canBrew = plugin.getChallengeNMS().canBrew(stand);
        if (canBrew) return;

        Player player = (Player) event.getWhoClicked();
        this.plugin.runTask(task -> {
            boolean canBrewSure = plugin.getChallengeNMS().canBrew(stand);
            if (!canBrewSure) return;

            BrewingStand stand2 = brewerInventory.getHolder();
            PDCUtil.set(stand2, Keys.BREWING_HOLDER, player.getUniqueId().toString());
            stand2.update();
        });
    }
}