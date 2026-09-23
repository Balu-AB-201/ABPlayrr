package app.vitune.android.ui.components.themed

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import app.vitune.android.R
import app.vitune.android.ui.screens.settings.SwitchSettingsEntry
import app.vitune.android.utils.center
import app.vitune.android.utils.color
import app.vitune.android.utils.semiBold
import app.vitune.core.ui.Dimensions
import app.vitune.core.ui.LocalAppearance
import app.vitune.core.ui.utils.roundedShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

class TabsBuilder @PublishedApi internal constructor() {
    companion object {
        @Composable
        inline fun rememberTabs(crossinline content: TabsBuilder.() -> Unit) = rememberSaveable(
            saver = listSaver(
                save = { it },
                restore = { it.toImmutableList() }
            )
        ) {
            TabsBuilder().apply(content).tabs.values.toImmutableList()
        }
    }

    @PublishedApi
    internal val tabs = mutableMapOf<String, Tab>()

    fun tab(
        key: Int,
        @StringRes title: Int,
        @DrawableRes icon: Int,
        canHide: Boolean = true
    ): Tab = tab(key.toString(), title, icon, canHide)

    fun tab(
        key: String,
        @StringRes title: Int,
        @DrawableRes icon: Int,
        canHide: Boolean = true
    ): Tab {
        require(key.isNotBlank()) { "key cannot be blank" }
        require(!tabs.containsKey(key)) { "key already exists" }
        require(icon != 0) { "icon is 0" }

        val ret = Tab.ResourcesTab(key, title, icon, canHide)
        tabs += key to ret
        return ret
    }

    fun tab(
        key: Int,
        title: String,
        @DrawableRes icon: Int,
        canHide: Boolean = true
    ): Tab = tab(key.toString(), title, icon, canHide)

    fun tab(
        key: String,
        title: String,
        @DrawableRes icon: Int,
        canHide: Boolean = true
    ): Tab {
        require(key.isNotBlank()) { "key cannot be blank" }
        require(title.isNotBlank()) { "title cannot be blank" }
        require(!tabs.containsKey(key)) { "key already exists" }
        require(icon != 0) { "icon is 0" }

        val ret = Tab.StaticTab(key, title, icon, canHide)
        tabs += key to ret
        return ret
    }
}

@Parcelize
sealed class Tab : Parcelable {
    abstract val key: String

    @IgnoredOnParcel
    abstract val title: @Composable () -> String

    @get:DrawableRes
    abstract val icon: Int
    abstract val canHide: Boolean

    data class ResourcesTab(
        override val key: String,
        @param:StringRes private val titleRes: Int,
        @param:DrawableRes override val icon: Int,
        override val canHide: Boolean
    ) : Tab() {
        @IgnoredOnParcel
        override val title: @Composable () -> String = { stringResource(titleRes) }
    }

    data class StaticTab(
        override val key: String,
        private val titleText: String,
        @param:DrawableRes override val icon: Int,
        override val canHide: Boolean
    ) : Tab() {
        @IgnoredOnParcel
        override val title: @Composable () -> String = { titleText }
    }
}

@Composable
inline fun NavigationRail(
    topIconButtonId: Int,
    noinline onTopIconButtonClick: () -> Unit,
    tabIndex: Int,
    crossinline onTabIndexChange: (Int) -> Unit,
    hiddenTabs: ImmutableList<String>,
    crossinline setHiddenTabs: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    tabsEditingTitle: String = stringResource(R.string.tabs),
    crossinline content: TabsBuilder.() -> Unit
) {
    val (colorPalette, typography) = LocalAppearance.current
    val tabs = TabsBuilder.rememberTabs(content)
    var editing by remember { mutableStateOf(false) }

    if (editing) DefaultDialog(
        onDismiss = { editing = false },
        horizontalPadding = 0.dp
    ) {
        BasicText(
            text = tabsEditingTitle,
            style = typography.s.center.semiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn {
            items(items = tabs, key = { it.key }) { tab ->
                SwitchSettingsEntry(
                    title = tab.title(),
                    text = null,
                    isChecked = tab.key !in hiddenTabs,
                    onCheckedChange = {
                        if (!it && hiddenTabs.size == tabs.size - 1) return@SwitchSettingsEntry
                        setHiddenTabs(if (it) hiddenTabs - tab.key else hiddenTabs + tab.key)
                    },
                    isEnabled = tab.canHide &&
                        (tab.key in hiddenTabs || hiddenTabs.size < tabs.size - 1)
                )
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .combinedClickable(onClick = onTopIconButtonClick, onLongClick = { editing = true })
        ) {
            Image(
                painter = painterResource(topIconButtonId),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colorPalette.textSecondary),
                modifier = Modifier.size(24.dp)
            )
        }

        val transition = updateTransition(targetState = tabIndex, label = "bottom_navigation")

        tabs.fastForEachIndexed { index, tab ->
            AnimatedVisibility(
                visible = tabIndex == index || tab.key !in hiddenTabs,
                label = "tab_visibility"
            ) {
                val selectedProgress by transition.animateFloat(label = "selected_progress") {
                    if (it == index) 1f else 0f
                }

                val textColor by transition.animateColor(label = "text_color") {
                    if (it == index) colorPalette.text else colorPalette.textDisabled
                }

                val itemModifier = Modifier
                    .size(width = 72.dp, height = 58.dp)
                    .clip(24.dp.roundedShape)
                    .combinedClickable(
                        onClick = { onTabIndexChange(index) },
                        onLongClick = { editing = true }
                    )
                    .graphicsLayer {
                        scaleX = 0.94f + selectedProgress * 0.06f
                        scaleY = 0.94f + selectedProgress * 0.06f
                    }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = itemModifier
                ) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(tab.icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colorPalette.text),
                            modifier = Modifier
                                .size(Dimensions.navigationRail.iconOffset * 2)
                                .graphicsLayer {
                                    alpha = 0.62f + selectedProgress * 0.38f
                                    translationY = (1f - selectedProgress) * 3.dp.toPx()
                                }
                        )

                        BasicText(
                            text = tab.title(),
                            style = typography.xs.semiBold.center.color(textColor),
                            modifier = Modifier.padding(top = 2.dp),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
