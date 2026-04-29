package com.wpi.backtoowner.ui.screens.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wpi.backtoowner.ui.theme.WpiHeaderMaroon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsDashboardScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val fromDate by viewModel.fromDate.collectAsStateWithLifecycle()
    val toDate by viewModel.toDate.collectAsStateWithLifecycle()
    var filtersVisible by remember { mutableStateOf(false) }
    val contentScroll = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Insights",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { filtersVisible = !filtersVisible },
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = if (filtersVisible) "Hide filters" else "Show filters",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WpiHeaderMaroon,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(contentScroll),
            ) {
                if (filtersVisible) {
                    InsightsFiltersSection(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        categoryFilter = categoryFilter,
                        availableCategories = availableCategories,
                        fromDate = fromDate,
                        toDate = toDate,
                        viewModel = viewModel,
                    )
                }
                when (val s = state) {
                InsightsUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 320.dp)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = WpiHeaderMaroon)
                            Spacer(Modifier.padding(12.dp))
                            Text("Loading posts_archive…", color = Color(0xFF666666))
                        }
                    }
                }
                is InsightsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(s.message, color = Color(0xFFB00020))
                        Spacer(Modifier.padding(12.dp))
                        Text(
                            "Ensure the Appwrite collection \"posts_archive\" exists with the same attributes as \"posts\". Pull down to retry.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                        )
                    }
                }
                is InsightsUiState.Ready -> {
                    val snap = s.snapshot
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        InsightCard(title = "Totals") {
                            Text(
                                "Posts matching filters: ${snap.filteredCount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1A1A1A),
                            )
                            if (snap.filteredCount != snap.archiveTotalCount) {
                                Text(
                                    "Total rows in archive: ${snap.archiveTotalCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Text(
                                "Historical rows from posts_archive. Live listings may be deleted when an item is returned; archive size does not decrease.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF666666),
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Spacer(Modifier.padding(8.dp))
                            Text("Lost vs found share", style = MaterialTheme.typography.labelLarge, color = WpiHeaderMaroon)
                            LostFoundBarChart(lost = snap.lostCount, found = snap.foundCount)
                        }
                        InsightCard(title = "Last 7 days (volume)") {
                            DailyVolumeBarChart(days = snap.lastSevenDays)
                        }
                        InsightCard(title = "7-day trend") {
                            TrendLineChart(days = snap.lastSevenDays)
                        }
                        InsightCard(title = "Top listing titles") {
                            if (snap.topTitles.isEmpty()) {
                                Text("No titles yet.", color = Color(0xFF888888))
                            } else {
                                TopTitlesHorizontalBars(items = snap.topTitles)
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

private enum class InsightsDatePickerTarget { FROM, TO }

private val insightsDateDisplayFmt: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())

/** ~48dp per row; cap height so four rows show and the rest scroll. */
private val InsightsCategoryDropdownMaxHeight = 48.dp * 4

private fun localDateToPickerMillis(d: LocalDate): Long =
    d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsightsFiltersSection(
    categoryFilter: String?,
    availableCategories: List<String>,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    viewModel: InsightsViewModel,
    modifier: Modifier = Modifier,
) {
    var datePickerTarget by remember { mutableStateOf<InsightsDatePickerTarget?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val categoryDialogScroll = rememberScrollState()
    val categoryFieldInteraction = remember { MutableInteractionSource() }
    val selectedCategory = categoryFilter?.trim()?.takeIf { it.isNotEmpty() }

    datePickerTarget?.let { target ->
        key(target, fromDate, toDate) {
            val initialMillis = when (target) {
                InsightsDatePickerTarget.FROM ->
                    fromDate?.let { localDateToPickerMillis(it) } ?: System.currentTimeMillis()
                InsightsDatePickerTarget.TO ->
                    toDate?.let { localDateToPickerMillis(it) } ?: System.currentTimeMillis()
            }
            val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
            DatePickerDialog(
                onDismissRequest = { datePickerTarget = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pickerState.selectedDateMillis?.let { ms ->
                                val ld = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                                when (target) {
                                    InsightsDatePickerTarget.FROM -> viewModel.setFromDate(ld)
                                    InsightsDatePickerTarget.TO -> viewModel.setToDate(ld)
                                }
                            }
                            datePickerTarget = null
                        },
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { datePickerTarget = null }) {
                        Text("Cancel")
                    }
                },
            ) {
                DatePicker(state = pickerState)
            }
        }
    }

    InsightCard(title = "Filters", modifier = modifier.fillMaxWidth()) {
        Text(
            "Category (listing title)",
            style = MaterialTheme.typography.labelLarge,
            color = WpiHeaderMaroon,
        )
        Text(
            "Same value as the category field when creating a post.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666),
            modifier = Modifier.padding(top = 4.dp),
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable(
                    interactionSource = categoryFieldInteraction,
                    indication = null,
                ) { categoryMenuExpanded = true },
            readOnly = true,
            value = selectedCategory ?: "",
            onValueChange = {},
            singleLine = true,
            placeholder = {
                Text(
                    text = "Choose a category...",
                    color = Color(0xFF888888),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            trailingIcon = {
                IconButton(onClick = { categoryMenuExpanded = !categoryMenuExpanded }) {
                    Icon(
                        imageVector = if (categoryMenuExpanded) {
                            Icons.Filled.ExpandLess
                        } else {
                            Icons.Filled.ExpandMore
                        },
                        contentDescription = "Choose category",
                        tint = WpiHeaderMaroon,
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WpiHeaderMaroon,
                unfocusedBorderColor = WpiHeaderMaroon,
                focusedTrailingIconColor = WpiHeaderMaroon,
                unfocusedTrailingIconColor = WpiHeaderMaroon,
                focusedTextColor = Color(0xFF1A1A1A),
                unfocusedTextColor = Color(0xFF1A1A1A),
                focusedPlaceholderColor = Color(0xFF888888),
                unfocusedPlaceholderColor = Color(0xFF888888),
            ),
        )

        // TextButton rows (not DropdownMenuItem): menu items only compose correctly inside DropdownMenu.
        if (categoryMenuExpanded) {
            Dialog(onDismissRequest = { categoryMenuExpanded = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = InsightsCategoryDropdownMaxHeight)
                            .verticalScroll(categoryDialogScroll)
                            .padding(vertical = 4.dp),
                    ) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.clearCategoryFilter()
                                categoryMenuExpanded = false
                            },
                        ) {
                            Text(
                                "All categories",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF1A1A1A),
                            )
                        }
                        if (availableCategories.isEmpty()) {
                            Text(
                                "No categories in archive yet. Pull to refresh after posts exist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF888888),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        } else {
                            availableCategories.forEach { cat ->
                                TextButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        viewModel.setCategoryFilter(cat)
                                        categoryMenuExpanded = false
                                    },
                                ) {
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF1A1A1A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.padding(top = 12.dp))
        Text("Created date", style = MaterialTheme.typography.labelLarge, color = WpiHeaderMaroon)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = { datePickerTarget = InsightsDatePickerTarget.FROM },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = fromDate?.format(insightsDateDisplayFmt) ?: "From…",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            OutlinedButton(
                onClick = { datePickerTarget = InsightsDatePickerTarget.TO },
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = toDate?.format(insightsDateDisplayFmt) ?: "To…",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
        if (fromDate != null || toDate != null) {
            TextButton(
                onClick = { viewModel.clearDateFilters() },
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Clear dates", color = WpiHeaderMaroon)
            }
        }
    }
}

@Composable
private fun InsightCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = WpiHeaderMaroon)
            Spacer(Modifier.padding(8.dp))
            content()
        }
    }
}
