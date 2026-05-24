package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CGPA Count", appName)
  }

  @Test
  fun testViewModelInitialization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.AppDatabase.getDatabase(context)
    val repository = com.example.data.AppRepository(database.appDao())
    
    // Test seeding
    kotlinx.coroutines.runBlocking {
      repository.seedInitialDataIfNecessary()
    }
    
    val viewModel = com.example.ui.GpaViewModel(repository)
    org.junit.Assert.assertNotNull(viewModel.uiState.value)
  }

  @Test
  fun testMainActivityLaunch() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        org.junit.Assert.assertNotNull(activity)
      }
    }
  }
}
