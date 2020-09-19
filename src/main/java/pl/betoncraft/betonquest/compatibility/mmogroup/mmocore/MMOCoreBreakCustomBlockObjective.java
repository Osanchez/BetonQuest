package pl.betoncraft.betonquest.compatibility.mmogroup.mmocore;

import net.Indyuce.mmocore.api.block.BlockType;
import net.Indyuce.mmocore.api.block.SkullBlockType;
import net.Indyuce.mmocore.api.block.VanillaBlockType;
import net.Indyuce.mmocore.api.event.CustomBlockMineEvent;
import net.Indyuce.mmoitems.comp.mmocore.load.MMOItemsBlockType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import pl.betoncraft.betonquest.BetonQuest;
import pl.betoncraft.betonquest.Instruction;
import pl.betoncraft.betonquest.api.Objective;
import pl.betoncraft.betonquest.config.Config;
import pl.betoncraft.betonquest.exceptions.InstructionParseException;
import pl.betoncraft.betonquest.utils.PlayerConverter;

public class MMOCoreBreakCustomBlockObjective extends Objective implements Listener {

    private final String desiredBlockId;
    private final int neededAmount;

    private final boolean notify;
    private final int notifyInterval;

    public MMOCoreBreakCustomBlockObjective(final Instruction instruction) throws InstructionParseException {
        super(instruction);
        template = MMOBlockData.class;

        desiredBlockId = instruction.getOptional("block");
        neededAmount = instruction.getInt();

        notifyInterval = instruction.getInt(instruction.getOptional("notify"), 1);
        notify = instruction.hasArgument("notify") || notifyInterval > 0;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(final CustomBlockMineEvent event) {
        final String playerID = PlayerConverter.getID(event.getPlayer());
        if (!containsPlayer(playerID) || !checkConditions(playerID)) {
            return;
        }

        final String blockId = getBlockId(event.getBlockInfo().getBlock());
        if (!blockId.equals(desiredBlockId)) {
            return;
        }

        final MMOBlockData playerData = (MMOBlockData) dataMap.get(playerID);
        playerData.addPlacedBlock();

        if (playerData.getPlacedBlocks() == neededAmount) {
            completeObjective(playerID);
        } else if (notify && playerData.getPlacedBlocks() % notifyInterval == 0) {
            Config.sendNotify(playerID, "blocks_to_break",
                    new String[]{String.valueOf(neededAmount - playerData.getPlacedBlocks())},
                    "blocks_to_break,info");
        }

    }

    private String getBlockId(final BlockType blockType) {
        String actualBlockId = null;

        if (blockType instanceof VanillaBlockType) {
            final VanillaBlockType vanillaBlock = (VanillaBlockType) blockType;
            actualBlockId = vanillaBlock.getType().toString();

        } else if (blockType instanceof MMOItemsBlockType) {
            final MMOItemsBlockType mmoItemsBlock = (MMOItemsBlockType) blockType;
            actualBlockId = String.valueOf(mmoItemsBlock.getBlockId());

        } else if (blockType instanceof SkullBlockType) {
            final SkullBlockType skullBlock = (SkullBlockType) blockType;
            actualBlockId = skullBlock.getValue();
        }
        return actualBlockId;
    }


    @Override
    public void start() {
        Bukkit.getPluginManager().registerEvents(this, BetonQuest.getInstance());
    }

    @Override
    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public String getDefaultDataInstruction() {
        return "0";
    }

    @Override
    public String getProperty(final String name, final String playerID) {
        if (name.equalsIgnoreCase("left")) {
            return Integer.toString(neededAmount - ((MMOBlockData) dataMap.get(playerID)).getPlacedBlocks());
        } else if (name.equalsIgnoreCase("amount")) {
            return Integer.toString(((MMOBlockData) dataMap.get(playerID)).getPlacedBlocks());
        }
        return "";
    }

    public static class MMOBlockData extends ObjectiveData {

        private int amount;

        public MMOBlockData(final String instruction, final String playerID, final String objID) {
            super(instruction, playerID, objID);
            amount = Integer.parseInt(instruction);
        }

        private void addPlacedBlock() {
            amount++;
            update();
        }

        private int getPlacedBlocks() {
            return amount;
        }

        @Override
        public String toString() {
            return String.valueOf(amount);
        }
    }
}