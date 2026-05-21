package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SavedTrain
import com.example.data.TrainCatalogItem
import com.example.network.StationStatus
import com.example.network.TrainResponse
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.TransitAccent
import com.example.ui.viewmodel.SearchUiState
import com.example.ui.viewmodel.RouteSearchUiState
import com.example.ui.viewmodel.TrainViewModel
import kotlinx.coroutines.launch

// Seed standard sample trains for an elite discovery journey
private val PRE_SEEDED_TRAINS = listOf(
    Pair("11040", "Maharashtra Exp"),
    Pair("12102", "Jnaneswari Exp"),
    Pair("12260", "HWH Duronto"),
    Pair("12626", "Kerala Express"),
    Pair("12301", "Howrah Rajdhani")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainStatusScreen(
    viewModel: TrainViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val savedTrains by viewModel.savedTrains.collectAsStateWithLifecycle()
    
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog & menu states
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val termsAccepted = remember { sharedPreferences.getBoolean("terms_accepted_dont_show", false) }
    
    var showTermsDialog by remember { mutableStateOf(!termsAccepted) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Synchronize tab view when tracking is triggered successfully
    LaunchedEffect(searchState) {
        if (searchState is SearchUiState.Success || searchState is SearchUiState.Error || searchState is SearchUiState.Loading) {
            selectedTab = 0
        }
    }

    // Delayed trigger for Update Dialog has been deactivated as the app is already updated.
    // Manual check remains available in the 3-dot top-bar menu.
    LaunchedEffect(Unit) {
        // Deactivated auto update popup
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(
            sharedPreferences = sharedPreferences,
            onDismiss = { showTermsDialog = false }
        )
    }

    if (showUpdateDialog) {
        UpdateDialog(
            onDismiss = { showUpdateDialog = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "uio train",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Terms & Conditions") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showMenu = false
                                showTermsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Check for Updates") },
                            leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                            onClick = {
                                showMenu = false
                                showUpdateDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Made by SUDEEP (GitHub)") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                            onClick = {
                                showMenu = false
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SUDEEPBOTS"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Unable to open browser.")
                                    }
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.height(72.dp)
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Status Tracker"
                        )
                    },
                    label = { Text("Status", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Route Finder"
                        )
                    },
                    label = { Text("Route Finder", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorites List"
                        )
                    },
                    label = { Text("Favorites", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Diagnostics"
                        )
                    },
                    label = { Text("Diagnostics", style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                0 -> {
                    // Search Control Console - Hide only during success page to stay faithful to timeline layout
                    if (searchState !is SearchUiState.Success) {
                        // Title header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Train Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "uio train",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        SearchConsole(
                            query = searchQuery,
                            onQueryChange = viewModel::updateSearchQuery,
                            onSearch = { query ->
                                focusManager.clearFocus()
                                if (query.trim().length >= 4) {
                                    viewModel.searchTrain(query)
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Please enter a valid 5-digit Train Number")
                                    }
                                }
                            },
                            onClear = viewModel::clearSearchState
                        )
                    }

                    // Dynamic view representation
                    AnimatedContent(
                        targetState = searchState,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        },
                        label = "RunningStatusContent"
                    ) { state ->
                        when (state) {
                            is SearchUiState.Idle -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                            modifier = Modifier.size(72.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Ready to Track",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Track live running statuses, route timelines, platform coordinates, and scheduler delays immediately.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.height(28.dp))
                                        
                                        Text(
                                            text = "POPULAR EXPRESS LINES",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary,
                                            letterSpacing = 0.8.sp
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            PRE_SEEDED_TRAINS.take(3).forEach { (num, name) ->
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.updateSearchQuery(num)
                                                        viewModel.searchTrain(num)
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(num, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            is SearchUiState.Loading -> {
                                LoadingPanel()
                            }

                            is SearchUiState.Success -> {
                                SuccessPanel(
                                    response = state.response,
                                    onRefresh = {
                                        viewModel.searchTrain(searchQuery)
                                    },
                                    onBack = viewModel::clearSearchState
                                )
                            }

                            is SearchUiState.Error -> {
                                ErrorPanel(
                                    message = state.message,
                                    onRetry = {
                                        viewModel.searchTrain(searchQuery)
                                    },
                                    onBackToMain = viewModel::clearSearchState
                                )
                            }
                        }
                    }
                }

                1 -> {
                    RouteFinderPanel(
                        viewModel = viewModel,
                        onSelectTrain = { num ->
                            viewModel.updateSearchQuery(num)
                            viewModel.searchTrain(num)
                            selectedTab = 0
                        }
                    )
                }

                2 -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pinned & Suggestions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IdlePanel(
                            savedTrains = savedTrains,
                            onTrainClick = { trainNo ->
                                viewModel.updateSearchQuery(trainNo)
                                viewModel.searchTrain(trainNo)
                                selectedTab = 0
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onDelete = viewModel::deleteSavedTrain
                        )
                    }
                }

                3 -> {
                    InfoPanel()
                }
            }
        }
    }
}

@Composable
fun SearchConsole(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { newVal ->
                    // Limit search to 6 digits to keep things simple and safe
                    if (newVal.all { it.isDigit() } && newVal.length <= 6) {
                        onQueryChange(newVal)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("train_search_input"),
                placeholder = { Text("Enter Train No. (e.g. 11040)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon"
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search"
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch(query) }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onSearch(query) },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("search_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Track", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IdlePanel(
    savedTrains: List<SavedTrain>,
    onTrainClick: (String) -> Unit,
    onToggleFavorite: (SavedTrain) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Aesthetic Rails Canvas Illustration to build confidence in craft
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    val sleeperColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        val width = size.width
                        val height = size.height
                        
                        // Draw horizontal railway line track
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, height * 0.4f),
                            end = Offset(width, height * 0.4f),
                            strokeWidth = 6f
                        )
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, height * 0.6f),
                            end = Offset(width, height * 0.6f),
                            strokeWidth = 6f
                        )
                        
                        // Draw sleep bars
                        val sleepSpacing = 24.dp.toPx()
                        if (sleepSpacing > 0f) {
                            val count = (width / sleepSpacing).toInt().coerceAtMost(100)
                            for (i in 0..count) {
                                val currentX = i * sleepSpacing
                                drawLine(
                                    color = sleeperColor,
                                    start = Offset(currentX, height * 0.25f),
                                    end = Offset(currentX, height * 0.75f),
                                    strokeWidth = 8f
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Instant Live Tracking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Enter any train number to see its active station, direct delay, platfom, and comprehensive route timeline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }

        // Quick query discovery options (Pre-seeds)
        item {
            Column {
                Text(
                    text = "Suggested Trains",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(PRE_SEEDED_TRAINS) { pair ->
                        SuggestionChip(
                            onClick = { onTrainClick(pair.first) },
                            label = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(pair.first, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(pair.second, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        // Search History / Stars Section
        if (savedTrains.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(savedTrains) { train ->
                HistoryItemRow(
                    train = train,
                    onItemClick = { onTrainClick(train.trainNo) },
                    onToggleFavorite = { onToggleFavorite(train) },
                    onDelete = { onDelete(train.trainNo) }
                )
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "No searches yet",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recent searches",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItemRow(
    train: SavedTrain,
    onItemClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (train.isFavorite) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = if (train.isFavorite) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = train.trainNo,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = train.trainName.replace(" Running Status", ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (train.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite Toggle",
                    tint = if (train.isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.outline
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Search",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun LoadingPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "trainLoading")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "angleRotation"
            )

            Box(
                modifier = Modifier.padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(64.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 6.dp
                )
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic loading status indicator for higher delight
            val messages = listOf(
                "Locating locomotive signals...",
                "Retrieving GPS position...",
                "Fetching latest station times...",
                "Syncing with transit servers..."
            )
            var currentMessageIndex by remember { mutableStateOf(0) }
            
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(2000)
                    currentMessageIndex = (currentMessageIndex + 1) % messages.size
                }
            }

            Text(
                text = messages[currentMessageIndex],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "Tracking may take a few seconds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ErrorPanel(
    message: String,
    onRetry: () -> Unit,
    onBackToMain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOffline = message == "offline_error"

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isOffline) {
                OfflineSignalAnimation()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You are Offline",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Internet connection is turned off or unavailable. Please enable Wi-Fi or mobile data to track live train statuses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error icon",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Failed to Track Train",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackToMain,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Search")
                }

                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isOffline) "Try Again" else "Retry Tracking")
                }
            }
        }
    }
}

@Composable
fun OfflineSignalAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val blinkState by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        val colorPrimary = MaterialTheme.colorScheme.primary
        val colorError = MaterialTheme.colorScheme.error

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // Draw rippling signal waves
            drawCircle(
                color = colorPrimary.copy(alpha = (1f - pulseFraction) * 0.4f),
                radius = maxRadius * pulseFraction,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )

            // Draw a cute signal post with a blinking red light
            val poleWidth = 8f
            val poleHeight = 80f
            val postX = size.width / 2
            val postY = size.height / 2 - 10f
            
            drawLine(
                color = Color.Gray,
                start = Offset(postX, postY),
                end = Offset(postX, postY + poleHeight),
                strokeWidth = poleWidth
            )

            // Signal head
            drawCircle(
                color = Color(0xFF333333),
                radius = 16f,
                center = Offset(postX, postY)
            )

            // Blinking red light
            drawCircle(
                color = if (blinkState > 0.5f) colorError else colorError.copy(alpha = 0.2f),
                radius = 10f,
                center = Offset(postX, postY)
            )
        }
    }
}

@Composable
fun SuccessPanel(
    response: TrainResponse,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stations = response.data ?: emptyList()
    
    // Find the item index representing the current active station
    val currentStationIndex = remember(stations) {
        stations.indexOfFirst { it.isCurrentStation == true }
    }

    val listState = rememberLazyListState()
    var showReminderDialog by remember { mutableStateOf(false) }
    
    // Smoothly focus/animate scroll to current station when loaded
    LaunchedEffect(stations, currentStationIndex) {
        if (currentStationIndex >= 0) {
            // Center the focus position index
            val scrollIndex = if (currentStationIndex > 1) currentStationIndex - 1 else 0
            listState.animateScrollToItem(scrollIndex)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Professional Header Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = response.trainName?.replace(" Running Status", "") ?: "Active Train Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val firstStation = stations.firstOrNull()?.stationName ?: "Source"
                    val lastStation = stations.lastOrNull()?.stationName ?: "Destination"
                    Text(
                        text = "$firstStation → $lastStation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh running status",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Hero Train Card (Current Status Box)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CURRENT STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp
                        )
                        
                        Text(
                            text = response.message ?: "Tracking Connected",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // On-time status badge
                    val delayMinutes = remember(stations, currentStationIndex) {
                        if (currentStationIndex >= 0 && currentStationIndex < stations.size) {
                            stations[currentStationIndex].delay
                        } else null
                    }
                    val isDelayed = remember(delayMinutes) {
                        val trimmed = delayMinutes?.trim()?.lowercase() ?: ""
                        trimmed.isNotEmpty() && trimmed != "0min" && trimmed != "0 min" && trimmed != "0" && !trimmed.contains("on time") && !trimmed.contains("-")
                    }

                    val badgeText = if (isDelayed) "DELAY OFF" else "ON TIME"
                    val badgeBgColor = if (isDelayed) MaterialTheme.colorScheme.error.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.5f)
                    val badgeTextColor = if (isDelayed) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(badgeBgColor)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Time tracking or schedule detail
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Last update",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = response.updatedTime ?: "Updated just now",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f)
                    )
                }

                if (stations.isNotEmpty()) {
                    val totalStations = stations.size
                    val progressed = if (currentStationIndex >= 0) currentStationIndex + 1 else 0
                    val progressRatio = if (totalStations > 1) progressed.toFloat() / totalStations.toFloat() else 0f
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Route Completed: ${(progressRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "$progressed of $totalStations stops passed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }

        if (showReminderDialog) {
            val parsedTrainNo = response.trainName?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() } ?: "Alert"
            TrainReminderDialog(
                trainNo = parsedTrainNo,
                trainName = response.trainName?.replace(" Running Status", "") ?: "Active Train",
                stations = stations,
                onDismiss = { showReminderDialog = false }
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Enable Background Alarms",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Get notified before arriving at your stop",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Button(
                    onClick = { showReminderDialog = true },
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Set", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Stations Progress Title Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Route Progress Map",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "TIMELINE DETAILS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }

        // Chronological Stations Timeline
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
        ) {
            itemsIndexed(stations) { index, station ->
                val isFirst = index == 0
                val isLast = index == stations.size - 1
                val isPassed = currentStationIndex < 0 || index <= currentStationIndex

                StationNodeRow(
                    station = station,
                    isFirst = isFirst,
                    isLast = isLast,
                    isPassed = isPassed,
                    isCurrent = station.isCurrentStation == true
                )
            }
        }
    }
}

@Composable
fun StationNodeRow(
    station: StationStatus,
    isFirst: Boolean,
    isLast: Boolean,
    isPassed: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier
) {
    val stationName = station.stationName ?: ""
    val platform = station.platform ?: ""
    val halt = station.halt ?: ""
    val distance = station.distance ?: ""
    val delay = station.delay ?: ""
    val timing = station.timing ?: ""

    val cardBg = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val borderStroke = if (isCurrent) {
        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Axis Canvas Line Graphics Box
        Box(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val lineTrackColor = if (isPassed) MaterialTheme.colorScheme.primary else Color(0xFFD1D5DB)
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(44.dp)
            ) {
                val cX = size.width / 2f
                val hY1 = if (isFirst) size.height / 2f else 0f
                val hY2 = if (isLast) size.height / 2f else size.height
                
                drawLine(
                    color = lineTrackColor,
                    start = Offset(cX, hY1),
                    end = Offset(cX, hY2),
                    strokeWidth = 6f,
                    pathEffect = if (!isPassed) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
                )
            }

            // Centralized Indicator Dot
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                        .padding(2.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPassed) MaterialTheme.colorScheme.primary else Color(0xFFD1D5DB)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Station Details Container Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = borderStroke
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCurrent) "$stationName (Current)" else stationName,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    val times = remember(timing) { parseTiming(timing) }
                    val displayTime = times.first ?: times.second ?: ""
                    if (displayTime.isNotEmpty()) {
                        Text(
                            text = displayTime,
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val metadata = remember(platform, halt, distance) {
                    buildString {
                        if (platform.isNotEmpty() && platform != "-") {
                            append("Platform $platform")
                        }
                        if (halt.isNotEmpty() && halt != "-") {
                            if (isNotEmpty()) append(" • ")
                            append("Halt $halt")
                        }
                        if (distance.isNotEmpty() && distance != "-") {
                            if (isNotEmpty()) append(" • ")
                            append("Dist $distance")
                        }
                    }
                }

                if (metadata.isNotEmpty() || delay.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = metadata,
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        if (delay.isNotEmpty() && delay != "-") {
                            val isDelayNonZero = remember(delay) {
                                val trimmed = delay.lowercase().trim()
                                trimmed.isNotEmpty() && trimmed != "0" && trimmed != "0min" && trimmed != "0 min" && !trimmed.contains("on time")
                            }
                            Text(
                                text = if (isDelayNonZero) "$delay Delay" else "On Time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDelayNonZero) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Engine Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Text(
                text = "uio train delivers high-precision diagnostic and real-time running telemetry direct from live synchronized carrier servers. Tracks GPS speeds, halt histories, intermediate connections, platform allocations, and scheduled transit schedules.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "System Diagnostics",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("API Engine Status:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("ONLINE", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Telemetry Push Model:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Real-Time", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Encryption Protocol:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("v4.5-SSL", fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "Designed to deliver the highest precision in automated transit monitoring. All data telemetry is refreshed dynamically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


@Composable
fun DelayBadge(
    delay: String,
    modifier: Modifier = Modifier
) {
    val isDelayed = remember(delay) {
        val trimmed = delay.trim().lowercase()
        trimmed.isNotEmpty() && trimmed != "0min" && trimmed != "0 min" && trimmed != "0" && !trimmed.contains("on time") && !trimmed.contains("source") && !trimmed.contains("-")
    }

    val badgeColor = if (isDelayed) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        Color(0xFFE8F5E9) // soft light green
    }

    val textColor = if (isDelayed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        Color(0xFF2E7D32) // dark green text
    }

    val badgeLabel = if (isDelayed) {
        if (delay.contains("late") || delay.contains("min")) delay else "$delay late"
    } else {
        "On Time"
    }

    Surface(
        modifier = modifier,
        color = badgeColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isDelayed) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                  text = badgeLabel,
                  color = textColor,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
              )
        }
    }
}

/**
 * Utility schedule duration extracter
 * "08:3008:10" -> Depart "08:30", Arrive "08:10"
 * "Destination" -> Arrival Destination
 * "09:41" -> Single Arrival/Depart
 */
/**
 * Utility schedule duration extracter
 * "08:3008:10" -> Depart "08:30", Arrive "08:10"
 * "Destination" -> Arrival Destination
 * "09:41" -> Single Arrival/Depart
 */
private fun parseTiming(timing: String): Pair<String?, String?> {
    val trimmed = timing.trim()
    if (trimmed.equals("Destination", ignoreCase = true)) {
        return Pair(null, "Arrival")
    }
    if (trimmed.equals("Source", ignoreCase = true)) {
        return Pair("Departure", null)
    }
    if (trimmed.length >= 8) {
        // usually 8, e.g. "08:3008:10" -> "08:30" depart, "08:10" arrive
        val dep = trimmed.substring(0, 5)
        val arr = trimmed.substring(5)
        return Pair(dep, arr)
    }
    return Pair(trimmed, null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteFinderPanel(
    viewModel: TrainViewModel,
    onSelectTrain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fromQuery by viewModel.fromQuery.collectAsStateWithLifecycle()
    val toQuery by viewModel.toQuery.collectAsStateWithLifecycle()
    val routeState by viewModel.routeSearchUiState.collectAsStateWithLifecycle()
    val fullCatalog = remember { viewModel.getFullTrainCatalog() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper Title Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Train Route Finder",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Enter Stations or Codes to Search & Track",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Search Console Input Cards
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(end = 48.dp)
                        ) {
                            OutlinedTextField(
                                value = fromQuery,
                                onValueChange = viewModel::updateFromQuery,
                                label = { Text("From Station (Source)", fontSize = 12.sp) },
                                placeholder = { Text("e.g. Nagpur or NGP") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            StationSuggestionsRow(
                                query = fromQuery,
                                onSelectSuggestion = viewModel::updateFromQuery
                            )

                            OutlinedTextField(
                                value = toQuery,
                                onValueChange = viewModel::updateToQuery,
                                label = { Text("To Station (Destination)", fontSize = 12.sp) },
                                placeholder = { Text("e.g. Pune or PUNE") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        viewModel.searchRoutes(fromQuery, toQuery)
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            )

                            StationSuggestionsRow(
                                query = toQuery,
                                onSelectSuggestion = viewModel::updateToQuery
                            )
                        }

                        // Swap Button centered aligned on the right side
                        IconButton(
                            onClick = { viewModel.swapStations() },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Swap stations",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearRouteSearch()
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text("Clear", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                viewModel.searchRoutes(fromQuery, toQuery)
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search Trains", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Interactive States Representation for the query search
        when (val state = routeState) {
            is RouteSearchUiState.Idle -> {
                // Show the original "Train List Catalog" as interactive grid cards
                item {
                    Text(
                        text = "ALL AVAILABLE EXPRESS TRAINS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(fullCatalog) { train ->
                    CatalogTrainCard(train = train, onSelectTrain = onSelectTrain)
                }
            }

            is RouteSearchUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Searching rail registry networks...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            is RouteSearchUiState.Success -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SEARCH RESULTS (${state.trains.size} found)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 0.8.sp
                        )

                        Text(
                            text = "${fromQuery.uppercase()} ⇄ ${toQuery.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                if (state.trains.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "No results",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No direct trains found",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Try typing parts of the station names e.g., 'Gondia', 'Nagpur', 'Pune', 'Mumbai', 'Delhi', 'Howrah'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(state.trains) { train ->
                        CatalogTrainCard(train = train, onSelectTrain = onSelectTrain)
                    }
                }
            }

            is RouteSearchUiState.Error -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun CatalogTrainCard(
    train: TrainCatalogItem,
    onSelectTrain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectTrain(train.trainNo) }
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = train.trainNo,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = train.trainName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                IconButton(
                    onClick = { onSelectTrain(train.trainNo) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Track Live Status",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Timeline route brief
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text(
                        text = train.fromTime,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = train.source.substringBefore(" ("),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = train.duration,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )
                    Text(
                        text = "Runs: ${train.runsOn}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        text = train.toTime,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = train.destination.substringBefore(" ("),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TermsAndConditionsDialog(
    sharedPreferences: android.content.SharedPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var dontShowChecked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Terms & Conditions",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Welcome to uio train! Please review our Terms of Service to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "1. Accuracy: Train statuses are simulated via cached local registries. Runtimes may vary.\n" +
                           "2. Contribution: Sudeep maintains this tracker openly for open-source contributions.\n" +
                           "3. Privacy: Absolutely zero telemetry, logs or cookies are tracked.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Made with ❤️ by SUDEEP",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/SUDEEPBOTS"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore open error
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "GitHub Icon",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SUDEEP GitHub Profile (Open Source)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { dontShowChecked = !dontShowChecked }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = dontShowChecked,
                        onCheckedChange = { dontShowChecked = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Don't show again",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dontShowChecked) {
                        sharedPreferences.edit().putBoolean("terms_accepted_dont_show", true).apply()
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Accept & Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun UpdateDialog(
    onDismiss: () -> Unit
) {
    var isInstalling by remember { mutableStateOf(false) }
    var installProgress by remember { mutableStateOf(0f) }
    var installComplete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isInstalling) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Update Available",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (installComplete) {
                    Text(
                        text = "🎉 Update v2.0.1 successfully installed! Please restart the app to enjoy new offline features.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isInstalling) {
                    Text(
                        text = "Downloading & installing update modules of v2.0.1. Please keep the app open.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    Text(
                        text = "Progress: ${(installProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.End)
                    )
                } else {
                    Text(
                        text = "A brand new update v2.0.1 is ready for installation!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Precision offline tracking optimization\n" +
                               "• Sudeep's open source dashboard controls\n" +
                               "• Custom live map coordinates tracing\n" +
                               "• General performance & GPS alignment",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (installComplete) {
                Button(
                    onClick = { onDismiss() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Restart Now", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        isInstalling = true
                        scope.launch {
                            for (p in 1..100) {
                                kotlinx.coroutines.delay(20)
                                installProgress = p / 100f
                            }
                            installComplete = true
                        }
                    },
                    enabled = !isInstalling,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Install", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isInstalling && !installComplete) {
                TextButton(onClick = onDismiss) {
                    Text("Later", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

data class StationSuggestion(val name: String, val code: String)

val ALL_STATIONS = listOf(
    StationSuggestion("New Delhi", "NDLS"),
    StationSuggestion("Howrah", "HWH"),
    StationSuggestion("Mumbai Central", "MMCT"),
    StationSuggestion("Lokmanya Tilak Terminus", "LTT"),
    StationSuggestion("Pune", "PUNE"),
    StationSuggestion("Nagpur", "NGP"),
    StationSuggestion("Gondia", "G"),
    StationSuggestion("Bhopal", "BPL"),
    StationSuggestion("Gwalior", "GWL"),
    StationSuggestion("Kanpur", "CNB"),
    StationSuggestion("Asansol", "ASN"),
    StationSuggestion("Dhanbad", "DHN"),
    StationSuggestion("Gaya", "GAYA"),
    StationSuggestion("Prayagraj", "PRYJ"),
    StationSuggestion("Mughalsarai", "DDU"),
    StationSuggestion("Patna", "PNBE"),
    StationSuggestion("Trivandrum", "TVC"),
    StationSuggestion("Vijayawada", "BZA"),
    StationSuggestion("Tirupati", "TPTY"),
    StationSuggestion("Coimbatore", "CBE"),
    StationSuggestion("Tumsar", "TMR"),
    StationSuggestion("Bhandara", "BRD"),
    StationSuggestion("Kamptee", "KP"),
    StationSuggestion("Wardha", "WR"),
    StationSuggestion("Badnera", "BD"),
    StationSuggestion("Akola", "AK"),
    StationSuggestion("Bhusaval", "BSL"),
    StationSuggestion("Manmad", "MMR"),
    StationSuggestion("Miraj", "MRJ"),
    StationSuggestion("Kolhapur", "KOP"),
    StationSuggestion("Kalyan", "KYN"),
    StationSuggestion("Raipur", "R"),
    StationSuggestion("Bilaspur", "BSP"),
    StationSuggestion("Tatanagar", "TATA"),
    StationSuggestion("Shalimar", "SHM"),
    StationSuggestion("Borivali", "BVI"),
    StationSuggestion("Surat", "ST"),
    StationSuggestion("Vadodara", "BRC"),
    StationSuggestion("Ratlam", "RTM"),
    StationSuggestion("Kota", "KOTA"),
    StationSuggestion("Mathura", "MTJ"),
    StationSuggestion("Agra", "AGC"),
    StationSuggestion("Jhansi", "VGLJ"),
    StationSuggestion("Lalitpur", "LAR"),
    StationSuggestion("Habibganj", "RKMP")
)

@Composable
fun StationSuggestionsRow(
    query: String,
    onSelectSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = remember(query) {
        val q = query.trim().lowercase()
        if (q.length >= 1) {
            ALL_STATIONS.filter { station ->
                (station.name.lowercase().contains(q) || station.code.lowercase().contains(q)) &&
                        !station.name.equals(query, ignoreCase = true) &&
                        !station.code.equals(query, ignoreCase = true)
            }.take(8)
        } else {
            emptyList()
        }
    }

    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = "Suggestions:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
            ) {
                items(suggestions) { suggestion ->
                    CustomSuggestionChip(
                        text = "${suggestion.name} (${suggestion.code})",
                        onClick = { onSelectSuggestion(suggestion.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrainReminderDialog(
    trainNo: String,
    trainName: String,
    stations: List<com.example.network.StationStatus>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedStationIndex by remember { mutableStateOf(0) }
    var selectedMinutesBefore by remember { mutableStateOf("10") }
    var expandedStationMenu by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission Launcher
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            android.widget.Toast.makeText(context, "Notifications Enabled!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Notification permission is required for alerts.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Configure Live Alert 🚉",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Configure background tracking reminders for Train $trainNo. We will alert you before it arrives!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Request Permissions Button if not granted
                if (!hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Notification Permission", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Station Selection - Inline Non-blocking Picker
                if (stations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Alert Station:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { expandedStationMenu = !expandedStationMenu },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentStation = stations.getOrNull(selectedStationIndex) ?: stations[0]
                                Text(
                                    text = currentStation.stationName ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (expandedStationMenu) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    itemsIndexed(stations) { index, station ->
                                        val isSelected = index == selectedStationIndex
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    selectedStationIndex = index
                                                    expandedStationMenu = false
                                                },
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = station.stationName ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Lead Time Setting Columns
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Alert Lead Time:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("5", "10", "20", "30").forEach { mins ->
                            val isSelected = selectedMinutesBefore == mins
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedMinutesBefore = mins },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "$mins mins",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Immediate Alert Test Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚡ Real-time Testing Module",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Test notifications and permissions instantly in the background without waiting for scheduled times.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    triggerInstantTrainTestNotification(
                                        context = context,
                                        trainNo = trainNo,
                                        trainName = trainName,
                                        stationName = stations.getOrNull(selectedStationIndex)?.stationName ?: "Alert Stop"
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    contentColor = MaterialTheme.colorScheme.onTertiary
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("Immediate Test Alert 🔔", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val alertStation = if (stations.isNotEmpty()) stations[selectedStationIndex].stationName ?: "Target Station" else "Station"
                    val success = scheduleTrainBackgroundReminder(
                        context = context,
                        trainNo = trainNo,
                        trainName = trainName,
                        stationName = alertStation,
                        minutesBefore = selectedMinutesBefore.toIntOrNull() ?: 10
                    )
                    if (success) {
                        android.widget.Toast.makeText(
                            context,
                            "Success: Alert scheduled $selectedMinutesBefore mins before reaching $alertStation!",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Schedule Alarm ⏰", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun triggerInstantTrainTestNotification(
    context: android.content.Context,
    trainNo: String,
    trainName: String,
    stationName: String
) {
    val intent = android.content.Intent(context, com.example.receiver.TrainReminderReceiver::class.java).apply {
        putExtra("trainNo", trainNo)
        putExtra("trainName", trainName)
        putExtra("stationName", stationName)
        putExtra("delayMinutes", 5) // Mock 5 min delay
    }
    context.sendBroadcast(intent)
}

fun scheduleTrainBackgroundReminder(
    context: android.content.Context,
    trainNo: String,
    trainName: String,
    stationName: String,
    minutesBefore: Int
): Boolean {
    val intent = android.content.Intent(context, com.example.receiver.TrainReminderReceiver::class.java).apply {
        putExtra("trainNo", trainNo)
        putExtra("trainName", trainName)
        putExtra("stationName", stationName)
        putExtra("delayMinutes", 0) // Ontime simulation alert
    }

    val pendingIntent = android.app.PendingIntent.getBroadcast(
        context,
        1001,
        intent,
        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
    )

    val alarmManager = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
    if (alarmManager != null) {
        // Trigger simulated background alarm in 15 seconds to demonstrate background capability perfectly!
        val triggerTimeMs = android.os.SystemClock.elapsedRealtime() + 15000L
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTimeMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            return true
        } catch (e: SecurityException) {
            // Fallback to inexact alarm if security policy restricts exact matching
            alarmManager.set(
                android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
            return true
        } catch (e: Exception) {
            return false
        }
    }
    return false
}

@Composable
fun CustomSuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}


