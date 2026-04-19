package com.example.nimons360.ui.family.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.nimons360.ui.family.list.components.FilterChipRow
import com.example.nimons360.ui.family.list.components.FamilyItem
import com.example.nimons360.ui.theme.Blue600
import com.example.nimons360.ui.theme.Grey300
import com.example.nimons360.ui.theme.Grey50
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Nimons360Theme
import com.example.nimons360.ui.theme.White
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    onAddFamilyClick: () -> Unit,
    onFamilyClick: (Int) -> Unit
) {
    // Observe State dari ViewModel
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val fetchedFamilies by viewModel.fetchedFamilies.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()

    // Families Grouping
    val pinnedFamilies = fetchedFamilies.filter { it.isPinned }
    val allFamilies = fetchedFamilies.filter { !it.isPinned }

    FamilyContent(
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        pinnedFamilies = pinnedFamilies,
        allFamilies = allFamilies,
        isLoading = isLoading,
        isLoadingMore = isLoadingMore,
        hasMore = hasMore,
        onSearchChange = { newQuery ->
            viewModel.updateSearchQuery(newQuery)
        },
        onFilterSelect = { newFilter ->
            viewModel.updateFilter(newFilter)
        },
        onPinClick = { clickedFamilyId ->
            // Pinned Family
            val family = fetchedFamilies.find { it.id == clickedFamilyId }

            if (family != null) {
                viewModel.togglePin(
                    familyId = family.id,
                    name = family.name,
                    iconUrl = family.iconUrl,
                    isCurrentlyPinned = family.isPinned
                )
            }
        },
        onFamilyClick = onFamilyClick,
        onAddFamilyClick = onAddFamilyClick,
        onLoadMore = { viewModel.loadNextPage() }
    )
}

@Composable
fun FamilyContent(
    searchQuery: String,
    selectedFilter: String,
    pinnedFamilies: List<FamilyModel>,
    allFamilies: List<FamilyModel>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onPinClick: (Int) -> Unit,
    onFamilyClick: (Int) -> Unit,
    onAddFamilyClick: () -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, hasMore, isLoadingMore) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 3 // trigger 3 item terakhir
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
            if (nearEnd && hasMore && !isLoadingMore && !isLoading) {
                onLoadMore()
            }
        }
    }

    Scaffold(
        containerColor = Grey50,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFamilyClick,
                containerColor = Blue600,
                contentColor = White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Family")
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search families...", color = Grey600) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Grey600)
                    },
                    shape = RoundedCornerShape(25.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = White,
                        focusedContainerColor = White,
                        unfocusedBorderColor = Grey300,
                        focusedBorderColor = Blue600,
                        cursorColor = Blue600,
                        focusedLeadingIconColor = Blue600,
                        unfocusedLeadingIconColor = Grey600
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(52.dp)
                )
            }

            item {
                FilterChipRow(
                    selectedFilter = selectedFilter,
                    onFilterSelect = onFilterSelect
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Blue600)
                    }
                }
            } else {
                if (pinnedFamilies.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pinned",
                            color = Grey600,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(
                        items = pinnedFamilies,
                        key = { it.id }
                    ) { family ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            FamilyItem(
                                name = family.name,
                                isPinned = family.isPinned,
                                iconUrl = family.iconUrl,
                                onPinClick = { onPinClick(family.id) },
                                onItemClick = { onFamilyClick(family.id) }
                            )
                        }
                    }
                }

                if (allFamilies.isNotEmpty()) {
                    item {
                        Text(
                            text = "All Families",
                            color = Grey600,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    items(
                        items = allFamilies,
                        key = { it.id }
                    ) { family ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            FamilyItem(
                                name = family.name,
                                isPinned = family.isPinned,
                                iconUrl = family.iconUrl,
                                onPinClick = { onPinClick(family.id) },
                                onItemClick = { onFamilyClick(family.id) }
                            )
                        }
                    }
                }

                if (pinnedFamilies.isEmpty() && allFamilies.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = "No families found",
                                color = Grey600,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Blue600
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
