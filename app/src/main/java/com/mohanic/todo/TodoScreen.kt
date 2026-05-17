package com.mohanic.todo

import android.app.WallpaperManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TodoScreen(modifier: Modifier = Modifier) {
    val todos = remember { mutableStateListOf<Todo>() }
    var input by remember { mutableStateOf("") }
    var nextId by remember { mutableStateOf(1L) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("New todo") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = input.trim()
                        if (trimmed.isNotEmpty()) {
                            todos.add(Todo(id = nextId++, text = trimmed))
                            input = ""
                        }
                    },
                ) {
                    Text("Add")
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val snapshot = todos.toList()
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                val bitmap = renderTodosToBitmap(context, snapshot)
                                WallpaperManager.getInstance(context).setBitmap(bitmap)
                            }
                        }
                        snackbarHostState.showSnackbar(
                            if (result.isSuccess) "Wallpaper updated" else "Failed to set wallpaper",
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Set as Wallpaper")
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items = todos, key = { it.id }) { todo ->
                    TodoRow(
                        todo = todo,
                        onToggle = {
                            val index = todos.indexOfFirst { it.id == todo.id }
                            if (index != -1) {
                                todos[index] = todos[index].copy(isDone = !todos[index].isDone)
                            }
                        },
                        onDelete = { todos.removeAll { it.id == todo.id } },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoRow(
    todo: Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = todo.isDone, onCheckedChange = { onToggle() })
        Text(
            text = todo.text,
            modifier = Modifier.weight(1f),
            textDecoration = if (todo.isDone) TextDecoration.LineThrough else TextDecoration.None,
        )
        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}
