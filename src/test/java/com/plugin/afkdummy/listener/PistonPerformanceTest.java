package com.plugin.afkdummy.listener;

import com.plugin.afkdummy.AFKDummyPlugin;
import com.plugin.afkdummy.entity.*;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;

class PistonPerformanceTest {
    @Test void readsEachDummyOnceForWholePistonAndStillBlocksCollision() {
        var plugin = mock(AFKDummyPlugin.class);
        var manager = mock(DummyManager.class);
        when(plugin.getDummyManager()).thenReturn(manager);
        var world = mock(World.class);
        var session = mock(DummySession.class);
        when(session.getLocation()).thenReturn(new Location(world, 12, 64, 0));
        when(manager.getAllSessions()).thenReturn(Map.of(UUID.randomUUID(), session));
        var piston = mock(Block.class);
        var head = mock(Block.class);
        when(piston.getWorld()).thenReturn(world);
        when(piston.getRelative(BlockFace.EAST)).thenReturn(head);
        when(head.getLocation()).thenReturn(new Location(world, 1, 64, 0));
        var blocks = new ArrayList<Block>();
        for (int x = 1; x <= 11; x++) {
            var block = mock(Block.class);
            when(block.getLocation()).thenReturn(new Location(world, x, 64, 0));
            blocks.add(block);
        }
        var event = mock(BlockPistonExtendEvent.class);
        when(event.getBlock()).thenReturn(piston);
        when(event.getDirection()).thenReturn(BlockFace.EAST);
        when(event.getBlocks()).thenReturn(blocks);
        new PlayerListener(plugin).onPistonExtend(event);
        verify(event).setCancelled(true);
        verify(session, times(1)).getLocation();
    }
}
