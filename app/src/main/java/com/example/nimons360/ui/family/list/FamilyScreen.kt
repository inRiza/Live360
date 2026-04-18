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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nimons360.ui.family.list.components.FilterChipRow
import com.example.nimons360.ui.family.list.components.PinnedSection

@Composable
fun FamilyScreen(
    viewModel: FamilyViewModel,
    onAddFamilyClick: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val families by viewModel.families.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val pinnedFamilies = families.filter { it.isPinned }
    val allFamilies = families.filter { !it.isPinned }

    FamilyContent(
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        pinnedFamilies = pinnedFamilies,
        allFamilies = allFamilies,
        isLoading = isLoading,
        onSearchChange = { viewModel.updateSearchQuery(it) },
        onFilterSelect = { viewModel.updateFilter(it) },
        onPinClick = { clickedFamilyId ->
            // CARI data lengkap keluarga berdasarkan ID yang diklik
            val family = families.find { it.id == clickedFamilyId }
            if (family != null) {
                // KIRIM ke ViewModel untuk disimpan/dihapus dari Room Database
                viewModel.togglePin(
                    familyId = family.id,
                    name = family.name,
                    iconUrl = family.iconUrl,
                    isCurrentlyPinned = family.isPinned
                )
            }
        },
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
    onAddFamilyClick: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddFamilyClick,
                containerColor = Color(0xFFE3F2FD),
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
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Families", fontSize = 24.sp, fontWeight = FontWeight.Normal)
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search families...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color(0xFFF1F1F5),
                    focusedContainerColor = Color(0xFFF1F1F5),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(56.dp)
            )

            FilterChipRow(
                selectedFilter = selectedFilter,
                onFilterSelect = onFilterSelect
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                PinnedSection(
                    title = "PINNED",
                    families = pinnedFamilies,
                    onPinClick = onPinClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                PinnedSection(
                    title = "ALL FAMILIES",
                    families = allFamilies,
                    onPinClick = onPinClick
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}