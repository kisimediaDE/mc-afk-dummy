package com.plugin.afkdummy.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ProfileIsolationTest {
    @Test void changingDummySkinNeverChangesAnotherProfileOrSharedEmptyMap() {
        var owner = new GameProfile(UUID.randomUUID(), "Owner");
        var dummy = new GameProfile(UUID.randomUUID(), "AFK_Test");
        var changed = SkinUtil.applySkin(dummy, new Property("textures", "first"));
        var changedAgain = SkinUtil.applySkin(changed, new Property("textures", "second"));
        assertTrue(owner.properties().isEmpty());
        assertTrue(dummy.properties().isEmpty());
        assertTrue(PropertyMap.EMPTY.isEmpty());
        assertEquals("first", changed.properties().get("textures").iterator().next().value());
        assertEquals("second", changedAgain.properties().get("textures").iterator().next().value());
        assertEquals(dummy.id(), changedAgain.id());
        assertEquals(dummy.name(), changedAgain.name());
    }
}
