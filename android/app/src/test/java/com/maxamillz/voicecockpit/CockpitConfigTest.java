package com.maxamillz.voicecockpit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class CockpitConfigTest {
    @Test public void usesReservedMacIpByDefault() {
        assertEquals("wss://192.168.68.81:8443", CockpitConfig.DEFAULT_ENDPOINT);
    }

    @Test public void acceptsSecureWebSocketEndpoint() {
        assertEquals("wss://voice.example.lan:8766", CockpitConfig.requireEndpoint(" wss://voice.example.lan:8766 "));
    }

    @Test public void rejectsInsecureOrMalformedEndpoints() {
        assertThrows(IllegalArgumentException.class, () -> CockpitConfig.requireEndpoint("ws://192.168.68.14:8765"));
        assertThrows(IllegalArgumentException.class, () -> CockpitConfig.requireEndpoint("https://voice.example.lan"));
        assertThrows(IllegalArgumentException.class, () -> CockpitConfig.requireEndpoint("wss:///missing-host"));
    }
}
