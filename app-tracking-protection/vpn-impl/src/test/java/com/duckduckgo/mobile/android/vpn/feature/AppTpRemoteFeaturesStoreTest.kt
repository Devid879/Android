package com.duckduckgo.mobile.android.vpn.feature

import android.content.SharedPreferences
import androidx.core.content.edit
import com.duckduckgo.common.test.CoroutineTestRule
import com.duckduckgo.common.test.api.InMemorySharedPreferences
import com.duckduckgo.feature.toggles.api.Toggle
import com.duckduckgo.feature.toggles.api.Toggle.State
import com.duckduckgo.mobile.android.vpn.prefs.VpnSharedPreferencesProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class AppTpRemoteFeaturesStoreTest {
    @get:Rule
    var coroutineRule = CoroutineTestRule()

    private lateinit var appTpRemoteFeaturesStore: AppTpRemoteFeaturesStore
    private lateinit var preferences: SharedPreferences

    @Before
    fun setup() = runTest {
        preferences = InMemorySharedPreferences()

        appTpRemoteFeaturesStore = AppTpRemoteFeaturesStore(
            coroutineRule.testScope,
            coroutineRule.testDispatcherProvider,
            object : VpnSharedPreferencesProvider {
                override fun getSharedPreferences(
                    name: String,
                    multiprocess: Boolean,
                    migrate: Boolean,
                ): SharedPreferences {
                    return preferences
                }
            },
            Moshi.Builder().build(),
        )
    }

    @Test
    fun `test value set`() {
        val expected = Toggle.State(enable = true)
        appTpRemoteFeaturesStore.set("key", expected)

        assertEquals(expected, appTpRemoteFeaturesStore.get("key"))
    }

    @Test
    fun `test get when value is missing`() {
        assertNull(appTpRemoteFeaturesStore.get("key"))
    }

    @Test
    fun `test get when value is not present`() {
        val expected = Toggle.State(enable = true)
        appTpRemoteFeaturesStore.set("key", expected)

        assertNull(appTpRemoteFeaturesStore.get("wrong key"))
    }

    @Test
    fun `test load values`() {
        val expected = Toggle.State(enable = true)
        // use this store just to populate the preferences
        appTpRemoteFeaturesStore.set("key", expected)

        // create a new store, it should load expected value
        val store = AppTpRemoteFeaturesStore(
            coroutineRule.testScope,
            coroutineRule.testDispatcherProvider,
            object : VpnSharedPreferencesProvider {
                override fun getSharedPreferences(
                    name: String,
                    multiprocess: Boolean,
                    migrate: Boolean,
                ): SharedPreferences {
                    return preferences
                }
            },
            Moshi.Builder().build(),
        )

        assertEquals(expected, store.get("key"))
    }

    @Test
    fun `test preference change propagation`() {
        val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(State::class.java)
        assertNull(appTpRemoteFeaturesStore.get("key"))

        val expected = Toggle.State(enable = true)

        // add something directly from the preferences API, this simulates a different process storing data
        preferences.edit { putString("key", adapter.toJson(expected)) }

        assertEquals(expected, appTpRemoteFeaturesStore.get("key"))
    }
}
