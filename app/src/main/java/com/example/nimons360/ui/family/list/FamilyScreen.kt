package com.example.nimons360.ui.family.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nimons360.ui.family.list.components.FilterChipRow
import com.example.nimons360.ui.family.list.components.PinnedSection
import com.example.nimons360.ui.theme.Blue100
import com.example.nimons360.ui.theme.Grey100
import com.example.nimons360.ui.theme.Grey600
import com.example.nimons360.ui.theme.Nimons360Theme

@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    onAddFamilyClick: () -> Unit,
    onFamilyClick: (String) -> Unit
) {
    // Observe State dari ViewModel
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val families by viewModel.families.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Families Grouping
    val pinnedFamilies = families.filter { it.isPinned }
    val allFamilies = families.filter { !it.isPinned }

    FamilyContent(
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        pinnedFamilies = pinnedFamilies,
        allFamilies = allFamilies,
        isLoading = isLoading,
        onSearchChange = { newQuery ->
            viewModel.updateSearchQuery(newQuery)
        },
        onFilterSelect = { newFilter ->
            viewModel.updateFilter(newFilter)
        },
        onPinClick = { clickedFamilyId ->
            // Pinned Family
            val family = families.find { it.id == clickedFamilyId }

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
        onAddFamilyClick = onAddFamilyClick
    )
}

@Composable
fun FamilyContent(
    searchQuery: String,
    selectedFilter: String,
    pinnedFamilies: List<FamilyModel>,
    allFamilies: List<FamilyModel>,
    isLoading: Boolean,
    onSearchChange: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onPinClick: (String) -> Unit,
    onFamilyClick: (String) -> Unit,
    onAddFamilyClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // Tombol Tambah Keluarga
            FloatingActionButton(
                onClick = onAddFamilyClick,
                containerColor = Blue100,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Family")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(15.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text("Search families...", color = Grey600)
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Grey600)
                },
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Grey100,
                    focusedContainerColor = Grey100,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(55.dp)
            )

            // Filter
            FilterChipRow(
                selectedFilter = selectedFilter,
                onFilterSelect = onFilterSelect
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                PinnedSection(
                    title = "PINNED",
                    families = pinnedFamilies,
                    onPinClick = onPinClick,
                    onFamilyClick = onFamilyClick
                )

                Spacer(modifier = Modifier.height(10.dp))

                PinnedSection(
                    title = "ALL FAMILIES",
                    families = allFamilies,
                    onPinClick = onPinClick,
                    onFamilyClick = onFamilyClick
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// Preview Layar Utama
@Preview(showBackground = true)
@Composable
fun FamilyContentPreview() {
    Nimons360Theme {
        FamilyContent(
            searchQuery = "",
            selectedFilter = "All",
            pinnedFamilies = listOf(
                FamilyModel("1", "Keluarga Cemara", true, "")
            ),
            allFamilies = listOf(
                FamilyModel("2", "Weekend Gang", false, "")
            ),
            isLoading = false,
            onSearchChange = {}, onFilterSelect = {}, onPinClick = {}, onFamilyClick = {}, onAddFamilyClick = {}
        )
    }
}