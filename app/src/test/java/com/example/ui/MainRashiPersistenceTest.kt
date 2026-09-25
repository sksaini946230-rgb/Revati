package com.example.ui

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The main rashi chosen in Settings used to live in memory only: it was Mesh
 * again after every launch, and the morning notification — which reads
 * `user_rashi_id` — always carried Mesh because nothing wrote that key.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainRashiPersistenceTest {

    private val app get() = RuntimeEnvironment.getApplication()
    private val prefs get() = app.getSharedPreferences("astroveda_prefs", Context.MODE_PRIVATE)

    @Before
    fun clear() {
        prefs.edit().clear().commit()
    }

    @Test
    fun `the main rashi survives a restart and is what the notification reads`() {
        MainViewModel(app).setDefaultRashi(5)
        assertEquals(5, prefs.getInt("user_rashi_id", 1))

        val afterRestart = MainViewModel(app)
        assertEquals(5, afterRestart.defaultRashiId.value)
        assertEquals(5, afterRestart.selectedRashiId.value)
    }

    @Test
    fun `looking at another sign on the Rashifal tab does not change it`() {
        val vm = MainViewModel(app)
        vm.setDefaultRashi(3)
        vm.selectRashi(9)
        assertEquals(9, vm.selectedRashiId.value)
        assertEquals(3, vm.defaultRashiId.value)
        assertEquals(3, prefs.getInt("user_rashi_id", 1))
    }

    @Test
    fun `a stored value outside 1 to 12 is not believed`() {
        prefs.edit().putInt("user_rashi_id", 13).commit()
        assertEquals(1, MainViewModel(app).defaultRashiId.value)

        val vm = MainViewModel(app)
        vm.setDefaultRashi(0)
        assertEquals(13, prefs.getInt("user_rashi_id", 1))
        assertEquals(1, vm.defaultRashiId.value)
    }
}
