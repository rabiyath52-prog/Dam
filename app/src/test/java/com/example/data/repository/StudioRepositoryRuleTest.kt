package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ThemeFinish
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class StudioRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createWebsite_andObserve_succeedsForAuthenticatedOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore, auth)

        val websiteId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createWebsite(
                title = "Ekativa Gold",
                heading = "Kerala Heritage Chains",
                content = "22K handcrafted chains with hallmarked purity.",
                themeFinish = ThemeFinish.GOLD,
                generatedHtml = "<html><body><h1>Kerala Heritage Chains</h1></body></html>"
            ).getOrThrow()
        }
        assertTrue(websiteId.isNotEmpty())

        val websites = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeWebsites(uid).first { list -> list.any { it.id == websiteId } }
        }
        val created = websites.first { it.id == websiteId }
        assertEquals("Ekativa Gold", created.title)
        assertNotNull(created.createdAt)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updateWebsite(
                created.copy(title = "Ekativa Platinum", themeFinish = ThemeFinish.PLATINUM.name)
            ).getOrThrow()
        }

        val updated = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getWebsite(websiteId).getOrThrow()
        }
        assertEquals("Ekativa Platinum", updated?.title)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteWebsite(websiteId).getOrThrow()
        }
    }

    @Test
    fun createScript_andObserve_succeedsForAuthenticatedOwner() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = StudioRepository(firestore, auth)

        val scriptId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createScript(
                name = "gold_rate.py",
                code = "print('22K Gold Rate')",
                lastOutput = "22K Gold Rate"
            ).getOrThrow()
        }
        assertTrue(scriptId.isNotEmpty())

        val scripts = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeScripts(uid).first { list -> list.any { it.id == scriptId } }
        }
        assertEquals("gold_rate.py", scripts.first { it.id == scriptId }.name)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.deleteScript(scriptId).getOrThrow()
        }
    }

    @Test
    fun observeWebsites_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StudioRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createWebsite(
                title = "Alice Private Shop",
                heading = "Private",
                content = "Only Alice should read this.",
                themeFinish = ThemeFinish.SILVER,
                generatedHtml = "<html><body>Private</body></html>"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StudioRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                bobRepo.observeWebsites(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when Bob observes Alice's websites")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    @Test
    fun observeWebsites_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = StudioRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeWebsites(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_ekativa@test.com"
        const val BOB_EMAIL = "bob_ekativa@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
