package com.mohannic.taskarma

import android.app.WallpaperManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dao = remember(context) { TodoDatabase.getInstance(context).todoDao() }
    val todosState by dao.observeAll().collectAsState(initial = null)
    val todos = todosState ?: emptyList()

    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }

    // Automatic Wallpaper Update
    LaunchedEffect(todosState) {
        val currentTodos = todosState ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sharedPrefs = context.getSharedPreferences("taskarma_prefs", android.content.Context.MODE_PRIVATE)
            val currentHash = currentTodos.hashCode()
            val lastHash = sharedPrefs.getInt("last_wallpaper_hash", 0)

            if (currentHash != lastHash) {
                val bitmap = renderTodosToBitmap(context, currentTodos)
                val wallpaperManager = WallpaperManager.getInstance(context)
                try {
                    wallpaperManager.suggestDesiredDimensions(bitmap.width, bitmap.height)
                    wallpaperManager.setBitmap(bitmap)
                    sharedPrefs.edit().putInt("last_wallpaper_hash", currentHash).apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "My Tasks",
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (todos.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.TaskAlt,
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "All tasks completed",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "Your wallpaper is up to date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(items = todos, key = { it.id }) { todo ->
                    TodoItem(
                        todo = todo,
                        onToggle = {
                            scope.launch { dao.update(todo.copy(isDone = !todo.isDone)) }
                        },
                        onDelete = {
                            scope.launch { dao.delete(todo) }
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        if (showAddSheet) {
            AddTaskBottomSheet(
                onDismiss = { showAddSheet = false },
                onAdd = { text ->
                    scope.launch {
                        dao.insert(Todo(text = text))
                    }
                    showAddSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskBottomSheet(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
        ) {
            Text(
                "New task",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("What would you like to do?") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                ),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(
                    onClick = { if (text.isNotBlank()) onAdd(text) },
                    enabled = text.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun TodoItem(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (todo.isDone) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (todo.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Text(
            text = todo.text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                color = if (todo.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None
            )
        )
        
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
