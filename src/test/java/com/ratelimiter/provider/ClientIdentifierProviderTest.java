package com.ratelimiter.provider;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIdentifierProviderTest {

    @Test
    void prefersApiKeyOverUserId() {
        ClientIdentifierProvider provider = new ClientIdentifierProvider("user123", "key456");
        assertEquals("apikey:key456", provider.getIdentifier());
    }

    @Test
    void fallsBackToUserIdWhenNoApiKey() {
        ClientIdentifierProvider provider = new ClientIdentifierProvider("user123", null);
        assertEquals("user:user123", provider.getIdentifier());
    }

    @Test
    void fallsBackToDefaultWhenNothingProvided() {
        ClientIdentifierProvider provider = new ClientIdentifierProvider(null, "");
        assertEquals("client:default", provider.getIdentifier());
    }
}
