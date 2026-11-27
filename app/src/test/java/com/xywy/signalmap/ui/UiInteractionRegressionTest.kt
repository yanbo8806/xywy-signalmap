package com.xywy.signalmap.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.core.view.drawToBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.xywy.signalmap.ui.profile.NearbyRecordLimitCard
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import com.xywy.signalmap.MainUiState
import com.xywy.signalmap.domain.cell.LogReplayState
import com.xywy.signalmap.domain.cell.TestLogAnalysis
import com.xywy.signalmap.domain.cell.SignalHistoryRecord
import com.xywy.signalmap.domain.cell.SimSignalHistory
import com.xywy.signalmap.domain.model.CellSignal
import com.xywy.signalmap.domain.model.RadioTechnology
import com.xywy.signalmap.domain.model.LayerMetadata
import com.xywy.signalmap.domain.model.TestLogDocument
import com.xywy.signalmap.domain.model.TestLogMetadata
import com.xywy.signalmap.domain.model.TestLogSample
import com.xywy.signalmap.domain.model.TestNetworkEvent
import com.xywy.signalmap.ui.cell.CellScreen
import com.xywy.signalmap.ui.cell.NetworkEventText
import com.xywy.signalmap.ui.cell.RecentRecordsSection
import com.xywy.signalmap.ui.map.MapOverlayLayout
import com.xywy.signalmap.ui.map.MapPanelExpansionState
import com.xywy.signalmap.ui.map.MapToolbar
import com.xywy.signalmap.ui.map.MapSimSignalBadgeState
import com.xywy.signalmap.ui.map.MapSimSignalChip
import com.xywy.signalmap.ui.map.SearchCard
import com.xywy.signalmap.ui.map.TestRecordCard
import com.xywy.signalmap.ui.profile.LayerCard
import com.xywy.signalmap.ui.profile.ReplayMapControls
import com.xywy.signalmap.ui.profile.LogReplayDialog
import com.xywy.signalmap.ui.components.GlassPanelCard
import com.xywy.signalmap.ui.components.XyCard
import com.xywy.signalmap.ui.theme.SignalMapTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w360dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiInteractionRegressionTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `event title and detail both seek the selected sample`() {
        var clicks = 0
        compose.setContent {
            SignalMapTheme {
                NetworkEventText(
                    event = TestNetworkEvent(1_700_000_000_000, 0, "小区变化", "点击此处跳转采样", "参数变化推断"),
                    onClick = { clicks++ },
                )
            }
        }
        compose.onNodeWithText("小区变化", useUnmergedTree = true).performTouchInput { click() }
        compose.onNodeWithText("点击此处跳转采样", useUnmergedTree = true).performTouchInput { click() }
        compose.runOnIdle { assertEquals(2, clicks) }
    }

    @Test
    fun `overlay blocks map taps in blank page space`() {
        var mapClicks = 0
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().background(Color.Magenta).clickable { mapClicks++ })
                    AppTabOverlay(AppTab.Cells) { Text("占用小区") }
                }
            }
        }
        compose.onRoot().performTouchInput { click(Offset(180f, 500f)) }
        compose.runOnIdle { assertEquals(0, mapClicks) }
        val pixel = Color(capture("opaque-tab").getPixel(180, 500))
        assertTrue(pixel.green > 0.5f, "Page background must not reveal magenta map")
    }

    @Test
    fun `tab switching retains each page scroll position`() {
        var currentTab by mutableStateOf<AppTab?>(AppTab.Cells)
        var lastIndex = -1
        compose.setContent {
            SignalMapTheme {
                AppTabOverlay(currentTab) {
                    val listState = rememberLazyListState()
                    lastIndex = listState.firstVisibleItemIndex
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("list")) {
                        items(60) { index -> Text("item-$index", Modifier.height(60.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("list").performScrollToIndex(25)
        compose.runOnIdle { currentTab = null }
        compose.waitForIdle()
        compose.runOnIdle { currentTab = AppTab.Cells }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(25, lastIndex) }
    }

    @Test
    fun `all five map tools fit on a narrow screen with dual SIM HUD`() {
        var selected = ""
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.size(320.dp, 560.dp).padding(12.dp)) {
                    MapOverlayLayout(
                        panelsExpanded = false,
                        toolbar = {
                            MapToolbar(
                                MapPanelExpansionState(), false, false,
                                { selected = "search" }, { selected = "filter" },
                                { selected = "record" }, { selected = "tool" }, { selected = "create" },
                            )
                        },
                        panels = {},
                        signalHud = { Text("SIM1 / SIM2") },
                    )
                }
            }
        }
        listOf("搜索", MapFilterText.title, MapTestRecordText.title, MapToolText.title, "新建").forEach {
            compose.onNodeWithContentDescription(it).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription("新建").performTouchInput { click() }
        compose.runOnIdle { assertEquals("create", selected) }
        capture("map-toolbar-narrow")
    }

    @Test
    fun `map search and signal values remain readable at 320 dp`() {
        var searches = 0
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.size(320.dp, 640.dp).padding(12.dp)) {
                    MapOverlayLayout(
                        panelsExpanded = true,
                        toolbar = {
                            MapToolbar(MapPanelExpansionState(searchExpanded = true), false, false, {}, {}, {}, {}, {})
                        },
                        panels = {
                            SearchCard(
                                state = MainUiState(searchQuery = "120.8 34.4", locationPermissionGranted = true, phonePermissionGranted = true),
                                onSearchQueryChanged = {}, onSubmitSearchQuery = { searches++ },
                                onJumpToCoordinate = {}, onRequestPermissions = {}, onCollapse = {},
                            )
                        },
                        compactSignalHud = {
                            MapSimSignalChip(MapSimSignalBadgeState(0, "SIM1", "LTE", "-108 dBm", "12 dB", true))
                        },
                    )
                }
            }
        }
        compose.onNodeWithText("查询").assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithText("跳转坐标").assertIsDisplayed()
        compose.onNodeWithText("-108 dBm").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, searches) }
        capture("map-search-narrow")
    }

    @Test
    fun `shared nearest count saves only a positive integer at narrow width`() {
        var limit by mutableStateOf(200)
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.size(320.dp, 640.dp).padding(16.dp)) {
                    NearbyRecordLimitCard(limit) { limit = it }
                }
            }
        }
        compose.onNodeWithText("保存并应用到所有图层").assertIsNotEnabled()
        compose.onNodeWithText("每个图层的记录数").performTextReplacement("0")
        compose.onNodeWithText("保存并应用到所有图层").assertIsNotEnabled()
        compose.onNodeWithText("每个图层的记录数").performTextReplacement("abc")
        compose.onNodeWithText("保存并应用到所有图层").assertIsNotEnabled()
        compose.onNodeWithText("每个图层的记录数").performTextReplacement("350")
        compose.onNodeWithText("保存并应用到所有图层").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(350, limit) }
        compose.onNodeWithText("当前已保存：每层 350 条").assertIsDisplayed()
        capture("nearby-count-narrow")
    }

    @Test
    fun `editable layer exports and edit action remain reachable at 320 dp`() {
        val actions = mutableListOf<String>()
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.size(320.dp, 640.dp).padding(16.dp)) {
                    LayerCard(
                        layer = LayerMetadata("one", "测试自建图层", "manual", 0, true, 1, userEditable = true),
                        onToggleAlwaysRenderAll = { actions += "all:$it" },
                        onToggleVisibility = {},
                        onEdit = { actions += "edit" }, onDelete = { actions += "delete" },
                        onExportCsv = { actions += "csv" }, onExportKml = { actions += "kml" },
                    )
                }
            }
        }
        compose.onNodeWithText("CSV").assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithText("KML").assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithContentDescription("编辑图层样式").assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithContentDescription("删除图层").assertIsDisplayed().performTouchInput { click() }
        compose.onNodeWithText("全部显示").assertIsDisplayed()
        compose.onNodeWithText("仅显示地图中心最近的 200 条，移动地图后更新。").assertIsDisplayed()
        compose.onAllNodes(isToggleable()).assertCountEquals(2)
        compose.onAllNodes(isToggleable())[1].performClick()
        compose.runOnIdle { assertEquals(listOf("csv", "kml", "edit", "delete", "all:true"), actions) }
        capture("layer-card-narrow")
    }

    @Test
    fun `history table scrolls all columns and separates LTE and NR`() {
        val history = SimSignalHistory(
            1, 0, "SIM1", listOf(
                SignalHistoryRecord(1_700_000_000_000, CellSignal(RadioTechnology.LTE, true, rsrp = -100, sinr = 22)),
                SignalHistoryRecord(1_700_000_001_000, CellSignal(RadioTechnology.NR, true, rsrp = -90, sinr = 33)),
            ),
        )
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.size(320.dp, 640.dp).padding(16.dp)) {
                    RecentRecordsSection(history, RadioTechnology.NR)
                }
            }
        }
        compose.onNodeWithText("LTE").assertIsDisplayed()
        compose.onNodeWithText("NR").assertIsDisplayed()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange))
            .performTouchInput { swipeLeft() }
        compose.onNodeWithText("22").assertIsDisplayed()
        compose.onNodeWithText("33").assertIsDisplayed()
        capture("history-table-scrolled")
    }

    @Test
    fun `recording controls stay reachable in dark theme with large fonts`() {
        var stopped = false
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.3f),
            ) {
                SignalMapTheme(darkTheme = true) {
                    Box(Modifier.size(320.dp, 640.dp).background(MaterialTheme.colorScheme.background).padding(12.dp)) {
                        TestRecordCard(MainUiState(isTestRecording = true), {}, {}, { stopped = true }, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText(MapTestRecordText.stopAction).assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertTrue(stopped) }
        capture("recording-dark-large-font")
    }

    @Test
    fun `dark glass and tinted cards supply readable default text colors`() {
        var glassColor = Color.Unspecified
        var cardColor = Color.Unspecified
        var expected = Color.Unspecified
        compose.setContent {
            SignalMapTheme(darkTheme = true) {
                expected = MaterialTheme.colorScheme.onSurface
                Column {
                    GlassPanelCard { glassColor = LocalContentColor.current; Text("测试记录") }
                    XyCard(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)) {
                        cardColor = LocalContentColor.current
                        Text("卡片标题")
                    }
                }
            }
        }
        compose.runOnIdle {
            assertEquals(expected, glassColor)
            assertEquals(expected, cardColor)
        }
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-land-mdpi")
    fun `replay analysis remains scrollable in landscape and events seek`() {
        val time = 1_700_000_000_000L
        val event = TestNetworkEvent(time + 1000, 0, "回放测试事件", "事件详情点击可跳转", "系统状态")
        val document = TestLogDocument(
            TestLogMetadata("replay", "横屏回放测试", importedAtMillis = time, startedAtMillis = time, endedAtMillis = time + 1000, sampleCount = 2),
            listOf(TestLogSample(time), TestLogSample(time + 1000, events = listOf(event))),
        )
        var selectedIndex = -1
        compose.setContent {
            SignalMapTheme {
                LogReplayDialog(LogReplayState(document, TestLogAnalysis.create(document.samples)), {}, { selectedIndex = it }, {}, {})
            }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        compose.onNodeWithText("回放测试事件", useUnmergedTree = true).assertIsDisplayed()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, selectedIndex) }
    }

    @Test
    fun `tall map panels can scroll to their bottom action`() {
        var clicked = false
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.size(320.dp, 380.dp)) {
                    MapOverlayLayout(
                        panelsExpanded = true,
                        toolbar = { Text("工具栏", Modifier.height(48.dp)) },
                        panels = {
                            Text("长面板", Modifier.height(900.dp))
                            Button(onClick = { clicked = true }) { Text("保存图层") }
                        },
                        signalHud = { Text("SIM1 / SIM2") },
                        compactSignalHud = { Text("紧凑双卡信息") },
                    )
                }
            }
        }
        compose.onNodeWithText("保存图层").performScrollTo().assertIsDisplayed().performTouchInput { click() }
        compose.runOnIdle { assertTrue(clicked) }
    }

    @Test
    fun `empty replay disables playback and never displays one of zero`() {
        val document = TestLogDocument(
            TestLogMetadata("empty", "空记录", importedAtMillis = 0, startedAtMillis = 0, endedAtMillis = 0, sampleCount = 0),
            emptyList(),
        )
        compose.setContent {
            SignalMapTheme {
                ReplayMapControls(LogReplayState(document, TestLogAnalysis.create(emptyList())), {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("播放").assertIsNotEnabled()
        compose.onNodeWithText("无采样点").assertIsDisplayed()
    }

    @Test
    fun `cell permission screen is opaque and remains actionable`() {
        var requested = false
        compose.setContent {
            SignalMapTheme {
                Box(Modifier.fillMaxSize().background(Color.Magenta)) {
                    CellScreen(MainUiState(), PaddingValues(0.dp), { requested = true })
                }
            }
        }
        compose.onNodeWithText("立即授权").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(requested) }
        val pixel = Color(capture("permission-screen").getPixel(2, 2))
        assertTrue(pixel.green > 0.5f)
    }

    private fun capture(name: String): Bitmap = compose.runOnIdle {
        val bitmap = compose.activity.window.decorView.drawToBitmap()
        val directory = File("build/reports/ui-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap
    }
}
