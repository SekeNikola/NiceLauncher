package com.nicelauncher.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nicelauncher.data.AppItem
import com.nicelauncher.data.AppRepository
import com.nicelauncher.data.AppWithIcon
import com.nicelauncher.data.BackgroundMode
import com.nicelauncher.data.HomeApp
import com.nicelauncher.data.HomeAppsStore
import com.nicelauncher.data.LauncherWeather
import com.nicelauncher.data.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(application)
    private val weatherRepository = WeatherRepository()
    private val store = HomeAppsStore(application)

    private val _homeApps = MutableStateFlow<List<AppWithIcon>>(emptyList())
    val homeApps: StateFlow<List<AppWithIcon>> = _homeApps.asStateFlow()

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    val allApps: StateFlow<List<AppItem>> = _allApps.asStateFlow()

    // Incremented each unlock so LaunchedEffect(unlockKey) always fires
    private val _unlockKey = MutableStateFlow(0)
    val unlockKey: StateFlow<Int> = _unlockKey.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _showDrawer = MutableStateFlow(false)
    val showDrawer: StateFlow<Boolean> = _showDrawer.asStateFlow()

    private val _isDarkMode = MutableStateFlow(store.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _noteText = MutableStateFlow(store.getNoteText())
    val noteText: StateFlow<String> = _noteText.asStateFlow()

    private val _iconSizePercent = MutableStateFlow(store.getIconSizePercent())
    val iconSizePercent: StateFlow<Float> = _iconSizePercent.asStateFlow()

    private val _userAvatarUri = MutableStateFlow(store.getUserAvatarUri()?.let { Uri.parse(it) })
    val userAvatarUri: StateFlow<Uri?> = _userAvatarUri.asStateFlow()

    private val _wallpaperUri = MutableStateFlow(store.getWallpaperUri()?.let { Uri.parse(it) })
    val wallpaperUri: StateFlow<Uri?> = _wallpaperUri.asStateFlow()

    private val _userName = MutableStateFlow(store.getUserName())
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _weather = MutableStateFlow(LauncherWeather(store.getWeatherCity().uppercase(), null, null))
    val weather: StateFlow<LauncherWeather> = _weather.asStateFlow()

    private val _clockAppPackage = MutableStateFlow(store.getClockAppPackage())
    val clockAppPackage: StateFlow<String?> = _clockAppPackage.asStateFlow()

    private val _weatherAppPackage = MutableStateFlow(store.getWeatherAppPackage())
    val weatherAppPackage: StateFlow<String?> = _weatherAppPackage.asStateFlow()

    init {
        loadApps()
        refreshWeather()
    }

    fun updateNoteText(text: String) {
        _noteText.value = text
        store.setNoteText(text)
    }

    fun updateIconSizePercent(percent: Float) {
        _iconSizePercent.value = percent
        store.setIconSizePercent(percent)
    }

    fun updateUserAvatar(uri: Uri?) {
        _userAvatarUri.value = uri
        store.setUserAvatarUri(uri?.toString())
    }

    fun updateWallpaper(uri: Uri?) {
        _wallpaperUri.value = uri
        store.setWallpaperUri(uri?.toString())
    }

    fun updateUserName(name: String) {
        _userName.value = name
        store.setUserName(name)
    }

    fun updateWeatherCity(city: String) {
        val cleaned = city.trim().ifBlank { "Ljubljana" }
        store.setWeatherCity(cleaned)
        _weather.value = LauncherWeather(cleaned.uppercase(), null, null)
        refreshWeather()
    }

    fun refreshWeather() {
        val city = store.getWeatherCity()
        viewModelScope.launch(Dispatchers.IO) {
            _weather.value = runCatching {
                weatherRepository.fetchCityWeather(city)
            }.getOrElse {
                LauncherWeather(city.uppercase(), null, null)
            }
        }
    }

    private fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val installed = repository.getInstalledApps()
            _allApps.value = installed
            val appMap = installed.associateBy { it.packageName }
            val homeList = store.getHomeApps().mapNotNull { homeApp ->
                appMap[homeApp.packageName]?.let { app ->
                    AppWithIcon(
                        app = app,
                        customImageUri = homeApp.customImageUri?.let { Uri.parse(it) },
                        backgroundMode = homeApp.backgroundMode,
                    )
                }
            }
            _homeApps.value = homeList
        }
    }

    fun onResume() {
        _isUnlocked.value = true
        _unlockKey.value++
    }

    fun onPause() {
        _isUnlocked.value = false
    }

    fun addAppToHome(packageName: String) {
        val app = _allApps.value.find { it.packageName == packageName } ?: return
        val current = _homeApps.value.toMutableList()
        if (current.none { it.app.packageName == packageName }) {
            current.add(AppWithIcon(app, null, BackgroundMode.BLURRED))
            _homeApps.value = current
            persistHomeApps()
        }
        _showDrawer.value = false
    }

    fun removeFromHome(packageName: String) {
        _homeApps.value = _homeApps.value.filter { it.app.packageName != packageName }
        persistHomeApps()
    }

    fun updateCustomImage(packageName: String, uri: Uri?) {
        _homeApps.value = _homeApps.value.map { item ->
            if (item.app.packageName == packageName)
                item.copy(customImageUri = uri, backgroundMode = BackgroundMode.PHOTO)
            else item
        }
        persistHomeApps()
    }

    fun updateBackgroundMode(packageName: String, mode: BackgroundMode) {
        _homeApps.value = _homeApps.value.map { item ->
            if (item.app.packageName == packageName) {
                if (mode != BackgroundMode.PHOTO) {
                    item.copy(backgroundMode = mode, customImageUri = null)
                } else {
                    item.copy(backgroundMode = mode)
                }
            } else item
        }
        persistHomeApps()
    }

    fun launchApp(packageName: String) {
        repository.launchApp(packageName)
    }

    fun setClockApp(packageName: String) {
        _clockAppPackage.value = packageName
        store.setClockAppPackage(packageName)
    }

    fun setWeatherApp(packageName: String) {
        _weatherAppPackage.value = packageName
        store.setWeatherAppPackage(packageName)
    }

    fun openDrawer() { _showDrawer.value = true }
    fun closeDrawer() { _showDrawer.value = false }

    fun toggleDarkMode() {
        val new = !_isDarkMode.value
        _isDarkMode.value = new
        store.setDarkMode(new)
    }

    private fun persistHomeApps() {
        val snapshot = _homeApps.value
        viewModelScope.launch(Dispatchers.IO) {
            store.saveHomeApps(snapshot.map {
                HomeApp(it.app.packageName, it.customImageUri?.toString(), it.backgroundMode)
            })
        }
    }
}
