package com.creditrepairai.v2.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class CreditAgentGatewayTest {
    @Test
    fun rejectsCredentialBearingAndUnexpectedRoutes() {
        listOf("http://example.com", "https://user:password@example.com", "https://example.com/wrong-path", "https://example.com?token=hidden", "https://example.com/#fragment").forEach {
            assertEquals("", CreditAgentGateway.resolveEndpoint(it))
        }
    }
    @Test
    fun acceptsConvexSiteOriginAndBuildsTheAgentRoute() {
        assertEquals(
            "https://lovable-dinosaur-379.convex.site/credit-agent",
            CreditAgentGateway.resolveEndpoint("https://lovable-dinosaur-379.convex.site"),
        )
    }

    @Test
    fun keepsExplicitAgentRouteUnchanged() {
        assertEquals(
            "https://lovable-dinosaur-379.convex.site/credit-agent",
            CreditAgentGateway.resolveEndpoint("https://lovable-dinosaur-379.convex.site/credit-agent/"),
        )
    }
}
