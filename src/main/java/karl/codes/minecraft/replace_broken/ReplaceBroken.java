package karl.codes.minecraft.replace_broken;

import com.google.common.collect.ImmutableMap;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Mod(
        name = ReplaceBroken.NAME,
        modid = ReplaceBroken.MODID,
        version = ReplaceBroken.VERSION)
@SideOnly(Side.CLIENT)
public class ReplaceBroken
{
    public static final String NAME = "Replace Broken";
    public static final String MODID = "replace-broken";
    public static final String VERSION = "0.1-SNAPSHOT";

    private static final Logger LOG = LogManager.getLogger(ReplaceBroken.class);

    private static final ConcurrentMap<Class,Boolean> SEEN = new ConcurrentHashMap<Class, Boolean>();
    private static final Map<Class,Boolean> INTERESTING = ImmutableMap.<Class,Boolean>builder()
            .put(net.minecraftforge.client.event.TextureStitchEvent.Pre.class, false)
            .put(net.minecraftforge.client.event.GuiOpenEvent.class, false)
            .put(net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Pre.class, false)
            .put(net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent.Post.class, false)
            .put(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Pre.class, false)
            .put(net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent.Post.class, false)
            .put(net.minecraftforge.client.event.sound.PlaySoundEvent17.class, false)
            .put(net.minecraftforge.client.event.sound.PlayStreamingSourceEvent.class, false)
            .put(net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent.Pre.class, false)

            .put(PlayerDestroyItemEvent.class, true)
            .build();

    private int currentItem = -1;

    @EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void brokenTool(PlayerDestroyItemEvent event) {
        if(!(event.entity instanceof EntityClientPlayerMP)) {
            // TODO log?
            return;
        }

        EntityClientPlayerMP player = (EntityClientPlayerMP)event.entity;

        if(!(event.original.stackSize == 0
                && player.getCurrentEquippedItem() == null
                && event.original.getItem().getMaxDamage() != 0)) {
            return;
        }

        if(!(currentItem >= 0 && currentItem < InventoryPlayer.getHotbarSize())) {
            return;
        }

        ItemStack candidateStack = player.inventory.mainInventory[currentItem + InventoryPlayer.getHotbarSize()];
        if(candidateStack == null) {
            return;
        }

        Item oldItem = event.original.getItem();
        Item newItem = candidateStack.getItem();
        Class<? extends Item> oldItemType = oldItem.getClass();
        Class<? extends Item> newItemType = newItem.getClass();

        if(!(oldItemType.isAssignableFrom(newItemType) || newItemType.isAssignableFrom(oldItemType))) {
            // incompatible
            return;
        }

        // possibly a broken tool!
        LOG.info("you want to replace broken {} with {}",oldItem,newItem);
    }

    @SubscribeEvent
    public void breakBlock(PlayerEvent.BreakSpeed event) {
        EntityPlayer player = (EntityPlayer)event.entity;

        currentItem = player.inventory.currentItem;
    }

    @SubscribeEvent
    public void event(Event event) {
        Class<? extends Event> type = event.getClass();
        Boolean show = INTERESTING.get(type);
        if(show == null) {
            show = SEEN.putIfAbsent(type,Boolean.TRUE) == null;
        }

        if(show) {
            LOG.info("EVENTSPY\n.put({}.class,false)",event.getClass().getCanonicalName());
        }
    }
}
