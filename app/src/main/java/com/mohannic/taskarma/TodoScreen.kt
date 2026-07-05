package com.mohannic.taskarma

import android.app.WallpaperManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohannic.taskarma.ui.theme.BrandPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    userName: String,
    onUserNameChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val dao = remember(context) { TodoDatabase.getInstance(context).todoDao() }
    val archivedState by dao.observeArchived().collectAsState(initial = emptyList())
    val archivedTodos = archivedState

    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var editingTodo by remember { mutableStateOf<Todo?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var isFirstEmission by remember { mutableStateOf(true) }

    // ── Groups ───────────────────────────────────────────────────────────────
    val groupDao = remember(context) { TodoDatabase.getInstance(context).todoGroupDao() }
    val groups by groupDao.observeAll().collectAsState(initial = emptyList())
    val lastViewedGroupId = remember { UserPreferences.getLastViewedGroupId(context) }
    var activeGroupId by remember { mutableLongStateOf(-1L) }
    var showCreateGroupSheet by remember { mutableStateOf(false) }
    var groupToRename by remember { mutableStateOf<TodoGroup?>(null) }
    var groupToDelete by remember { mutableStateOf<TodoGroup?>(null) }
    var groupMenu by remember { mutableStateOf<TodoGroup?>(null) }

    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 } }

    val defaultGroup = remember(groups) { groups.firstOrNull() }
    val activeGroup = remember(groups, activeGroupId) {
        groups.find { it.id == activeGroupId } ?: defaultGroup
    }

    // Ensure at least one default group always exists
    LaunchedEffect(groups, lastViewedGroupId) {
        if (groups.isEmpty()) {
            // Create default group — this will trigger recomposition via the Flow
            val defaultId = groupDao.ensureDefaultGroup()
            activeGroupId = defaultId
            UserPreferences.setLastViewedGroupId(context, defaultId)
            return@LaunchedEffect
        }
        if (activeGroupId == -1L) {
            activeGroupId = groups.find { it.id == lastViewedGroupId }?.id ?: groups.first().id
        }
    }

    val groupTodosState by activeGroup?.let { group ->
        dao.observeByGroup(group.id).collectAsState(initial = null)
    } ?: remember { mutableStateOf(null) }
    val todos = groupTodosState ?: emptyList()

    // Automatic Wallpaper Update — reflects the active group.
    // Uses snapshotFlow to debounce rapid tab switches and NonCancellable
    // to ensure setBitmap always completes (prevents system-default flash).
    LaunchedEffect(Unit) {
        snapshotFlow {
            Triple(groupTodosState, activeGroup, isDarkMode)
        }.collect { (currentTodosRaw, currentGroup, darkMode) ->
            @Suppress("UNCHECKED_CAST")
            val currentTodos = currentTodosRaw as? List<Todo> ?: return@collect
            val group = currentGroup as? TodoGroup ?: return@collect
            val dark = darkMode as Boolean

            withContext(Dispatchers.IO) {
                val currentHash = currentTodos.hashCode() * 31 +
                                  dark.hashCode() * 31 +
                                  group.id.hashCode()
                val lastHash = UserPreferences.getLastWallpaperHash(context)

                if (isFirstEmission || currentHash != lastHash) {
                    // NonCancellable ensures setBitmap completes even if
                    // the user switches tabs again before it finishes.
                    withContext(kotlinx.coroutines.NonCancellable) {
                        try {
                            val bitmap = renderTodosToBitmap(
                                context, group.name, currentTodos, isDark = dark
                            )
                            val wallpaperManager = WallpaperManager.getInstance(context)
                            try {
                                wallpaperManager.suggestDesiredDimensions(bitmap.width, bitmap.height)
                            } catch (e: SecurityException) {
                                android.util.Log.e("Taskarma", "Wallpaper hints denied", e)
                            }
                            wallpaperManager.setBitmap(
                                bitmap, null, true,
                                WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                            )
                            UserPreferences.setLastWallpaperHash(context, currentHash)
                            isFirstEmission = false
                        } catch (e: Exception) {
                            android.util.Log.e("Taskarma", "Failed to set wallpaper", e)
                        }
                    }
                }
            }
        }
    }

    val pendingCount = todos.count { !it.isDone }
    val doneCount    = todos.count { it.isDone }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProTopBar(
                userName      = userName,
                isDarkMode    = isDarkMode,
                isScrolled    = isScrolled,
                onToggleDark  = onToggleDarkMode,
                onProfileClick= { showProfileSheet = true }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick        = { showAddSheet = true },
                expanded       = !isScrolled,
                icon           = { Icon(Icons.Default.Add, contentDescription = null) },
                text           = { Text("Add Task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                shape          = RoundedCornerShape(20.dp),
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ── Google Tasks-style chip tabs ──────────────────────────────
            if (groups.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    groups.forEach { group ->
                        GroupTabChip(
                            name = group.name,
                            isSelected = group.id == activeGroupId,
                            onClick = {
                                activeGroupId = group.id
                                UserPreferences.setLastViewedGroupId(context, group.id)
                            }
                        )
                    }
                    // "+ New list" chip — perfectly aligned with other chips
                    Surface(
                        onClick = { showCreateGroupSheet = true },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "New list",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "New list",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Clean separator below tabs
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                )

                // ── Content header — group name + options (like Google Tasks) ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 4.dp, top = 14.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeGroup?.name ?: "My Tasks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Sort
                    IconButton(
                        onClick = { /* TODO: sort */ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort tasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    // 3-dot menu for the active group
                    Box {
                        var showGroupMenu by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showGroupMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "List options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showGroupMenu,
                            onDismissRequest = { showGroupMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename list") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, null, Modifier.size(20.dp))
                                },
                                onClick = {
                                    activeGroup?.let { groupToRename = it }
                                    showGroupMenu = false
                                }
                            )
                            if (activeGroup?.id != defaultGroup?.id) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Delete list",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline, null,
                                            Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        activeGroup?.let { groupToDelete = it }
                                        showGroupMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Stats strip
            if (todos.isNotEmpty()) {
                StatsStrip(pending = pendingCount, done = doneCount)
            }

            if (todos.isEmpty()) {
                EmptyState(
                    groupName = activeGroup?.name ?: "this group",
                    modifier = Modifier.weight(1f)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val pending = todos.filter { !it.isDone }
                    val done    = todos.filter { it.isDone }

                    if (pending.isNotEmpty()) {
                        item {
                            SectionLabel("Pending", pending.size)
                        }
                        items(items = pending, key = { it.id }) { todo ->
                            TodoItemCard(
                                todo = todo,
                                onToggle = { scope.launch { dao.update(todo.copy(isDone = !todo.isDone)) } },
                                onDelete = { scope.launch { dao.delete(todo) } },
                                onEdit = { editingTodo = todo },
                                onArchive = null
                            )
                        }
                    }

                    if (done.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            SectionLabel("Completed", done.size)
                        }
                        items(items = done, key = { it.id }) { todo ->
                            TodoItemCard(
                                todo = todo,
                                onToggle = { scope.launch { dao.update(todo.copy(isDone = !todo.isDone)) } },
                                onDelete = { scope.launch { dao.delete(todo) } },
                                onEdit = null,
                                onArchive = { scope.launch { dao.archiveById(todo.id) } }
                            )
                        }
                    }

                    // ── Archived section ─────────────────────────────────
                    if (archivedTodos.isNotEmpty()) {
                        item { Spacer(Modifier.height(8.dp)) }
                        item {
                            ExpandableSectionLabel(
                                title = "Archived",
                                count = archivedTodos.size,
                                expanded = showArchived,
                                onToggle = { showArchived = !showArchived }
                            )
                        }
                        if (showArchived) {
                            items(items = archivedTodos, key = { "archived_${it.id}" }) { todo ->
                                ArchivedItemCard(
                                    todo = todo,
                                    onUnarchive = { scope.launch { dao.unarchiveById(todo.id) } },
                                    onDelete    = { scope.launch { dao.delete(todo) } }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            AddTaskBottomSheet(
                groups = groups,
                initialGroupId = activeGroup?.id ?: -1L,
                onDismiss = { showAddSheet = false },
                onAdd = { text, groupId ->
                    if (groupId <= 0L) {
                        Toast.makeText(
                            context,
                            "Please create a list first",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@AddTaskBottomSheet
                    }
                    scope.launch { dao.insert(Todo(text = text, groupId = groupId)) }
                    showAddSheet = false
                }
            )
        }

        editingTodo?.let { todo ->
            EditTaskBottomSheet(
                currentText = todo.text,
                currentGroupId = todo.groupId,
                groups = groups,
                onDismiss = { editingTodo = null },
                onSave = { newText, newGroupId ->
                    scope.launch {
                        dao.update(todo.copy(text = newText, groupId = newGroupId))
                    }
                    editingTodo = null
                }
            )
        }

        if (showCreateGroupSheet) {
            CreateGroupBottomSheet(
                onDismiss = { showCreateGroupSheet = false },
                onCreate = { name ->
                    scope.launch {
                        val id = groupDao.insert(TodoGroup(name = name))
                        activeGroupId = id
                        UserPreferences.setLastViewedGroupId(context, id)
                    }
                    showCreateGroupSheet = false
                }
            )
        }

        groupToRename?.let { group ->
            RenameGroupBottomSheet(
                currentName = group.name,
                onDismiss = { groupToRename = null },
                onRename = { newName ->
                    scope.launch { groupDao.update(group.copy(name = newName)) }
                    groupToRename = null
                }
            )
        }

        if (groupToDelete != null) {
            val deleting = groupToDelete!!
            AlertDialog(
                onDismissRequest = { groupToDelete = null },
                title = { Text("Delete group?") },
                text = { Text("All tasks in \"${deleting.name}\" will be permanently deleted.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                groupDao.delete(deleting)
                                if (activeGroupId == deleting.id) {
                                    // Always ensure at least one group exists after deletion
                                    val fallbackId = groupDao.ensureDefaultGroup()
                                    activeGroupId = fallbackId
                                    UserPreferences.setLastViewedGroupId(context, fallbackId)
                                }
                            }
                            groupToDelete = null
                        }
                    ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { groupToDelete = null }) { Text("Cancel") }
                }
            )
        }

        if (showProfileSheet) {
            ProfileBottomSheet(
                currentName = userName,
                isDarkMode  = isDarkMode,
                onToggleDark= onToggleDarkMode,
                onSaveName  = { newName ->
                    onUserNameChanged(newName)
                    showProfileSheet = false
                },
                onDismiss   = { showProfileSheet = false }
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Top Bar
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProTopBar(
    userName: String,
    isDarkMode: Boolean,
    isScrolled: Boolean,
    onToggleDark: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val today = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }
    val displayName = userName.trim().split(" ").firstOrNull()?.let { ", $it" } ?: ""

    Surface(
        color = if (isScrolled) MaterialTheme.colorScheme.surface
                else MaterialTheme.colorScheme.background,
        shadowElevation = if (isScrolled) 4.dp else 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: greeting + date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = today,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "My Tasks$displayName",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // Dark mode toggle
            IconButton(
                onClick = onToggleDark,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AnimatedContent(
                    targetState = isDarkMode,
                    transitionSpec = { scaleIn() togetherWith scaleOut() },
                    label = "dark_icon"
                ) { dark ->
                    Icon(
                        imageVector = if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (dark) "Switch to light" else "Switch to dark",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // Profile avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(BrandPrimary, MaterialTheme.colorScheme.secondary)
                        )
                    )
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Stats Strip
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsStrip(pending: Int, done: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatChip(
            label = "Pending",
            value = "$pending",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Done",
            value = "$done",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Total",
            value = "${pending + done}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Section Label
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$count",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Empty State
// ────────────────────────────────────────────────────────────────────────────

// ────────────────────────────────────────────────────────────────────────────
// Group Tab Chip — Google Tasks-style pill tab
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun GroupTabChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent,
        border = if (!isSelected) BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ) else null,
        tonalElevation = 0.dp
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(groupName: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TaskAlt,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "No tasks in $groupName",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap + to add your first task.\nIt'll appear on your wallpaper instantly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Todo Item Card
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodoItemCard(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)?,
    onArchive: (() -> Unit)?,
) {
    val isDone = todo.isDone
    val cardAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.6f else 1f,
        label = "card_alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isDone)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox-style toggle — only this toggles completion
            IconButton(onClick = onToggle, modifier = Modifier.size(44.dp)) {
                Crossfade(targetState = isDone, label = "check_icon") { done ->
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle
                                      else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (done) "Mark incomplete" else "Mark complete",
                        tint = if (done) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = todo.text,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isDone)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = cardAlpha)
                    else
                        MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Compact action icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Edit (pending) or Archive (completed)
                if (isDone && onArchive != null) {
                    IconButton(onClick = onArchive, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (!isDone && onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Archived Item Card
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArchivedItemCard(
    todo: Todo,
    onUnarchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Archive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = todo.text,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Unarchive / restore
                IconButton(onClick = onUnarchive, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Unarchive,
                        contentDescription = "Restore",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Delete permanently
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.45f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Expandable Section Label
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun ExpandableSectionLabel(
    title: String,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "$count",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Add Task Bottom Sheet
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    groups: List<TodoGroup>,
    initialGroupId: Long,
    onDismiss: () -> Unit,
    onAdd: (String, Long) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedGroupId by remember { mutableLongStateOf(initialGroupId) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                "New Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "What do you want to get done?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        "e.g. Review design mockups",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        if (text.isNotBlank()) onAdd(text, selectedGroupId)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(Modifier.height(12.dp))
            GroupSelector(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onSelected = { selectedGroupId = it },
                expanded = groupMenuExpanded,
                onExpandedChange = { groupMenuExpanded = it }
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = { if (text.isNotBlank()) onAdd(text, selectedGroupId) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("Save Task", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Edit Task Bottom Sheet
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskBottomSheet(
    currentText: String,
    currentGroupId: Long,
    groups: List<TodoGroup>,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var text by remember(currentText) { mutableStateOf(currentText) }
    var selectedGroupId by remember(currentGroupId) { mutableLongStateOf(currentGroupId) }
    var groupMenuExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboard = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                "Edit Task",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Update your task description",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text(
                        "Task description",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        if (text.isNotBlank()) onSave(text, selectedGroupId)
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )
            Spacer(Modifier.height(12.dp))
            GroupSelector(
                groups = groups,
                selectedGroupId = selectedGroupId,
                onSelected = { selectedGroupId = it },
                expanded = groupMenuExpanded,
                onExpandedChange = { groupMenuExpanded = it }
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Cancel") }

                Button(
                    onClick = { if (text.isNotBlank()) onSave(text, selectedGroupId) },
                    enabled = text.isNotBlank() && (text != currentText || selectedGroupId != currentGroupId),
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("Update", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Group Selector
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupSelector(
    groups: List<TodoGroup>,
    selectedGroupId: Long,
    onSelected: (Long) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val selectedGroup = groups.find { it.id == selectedGroupId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        OutlinedTextField(
            value = selectedGroup?.name ?: "Select group",
            onValueChange = {},
            readOnly = true,
            label = { Text("Group") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.name) },
                    onClick = {
                        onSelected(group.id)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Profile Bottom Sheet
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    currentName: String,
    isDarkMode: Boolean,
    onToggleDark: () -> Unit,
    onSaveName: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editedName by remember(currentName) { mutableStateOf(currentName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboard = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .fillMaxWidth()
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(BrandPrimary, MaterialTheme.colorScheme.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = editedName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Update your name visible in the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            // Name field
            OutlinedTextField(
                value = editedName,
                onValueChange = { editedName = it },
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        if (editedName.isNotBlank()) onSaveName(editedName.trim())
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            )

            Spacer(Modifier.height(16.dp))

            // Dark mode toggle row inside profile
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isDarkMode) "Dark Mode" else "Light Mode",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Wallpaper adapts to mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onToggleDark() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    keyboard?.hide()
                    if (editedName.isNotBlank()) onSaveName(editedName.trim())
                },
                enabled = editedName.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save Changes", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}
