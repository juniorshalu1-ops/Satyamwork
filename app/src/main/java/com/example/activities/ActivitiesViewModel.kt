package com.example.activities

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.database.AppRepository
import com.example.database.entities.EducationActivityEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActivitiesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AppRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = AppRepository(db)
    }

    val allActivities: StateFlow<List<EducationActivityEntity>> = repository.allActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCategory = MutableStateFlow("सभी")
    val searchQuery = MutableStateFlow("")

    val filteredActivities: StateFlow<List<EducationActivityEntity>> = combine(
        allActivities,
        selectedCategory,
        searchQuery
    ) { activities, category, query ->
        activities.filter { act ->
            val matchesCategory = (category == "सभी" || act.category.equals(category, ignoreCase = true))
            val matchesQuery = query.isBlank() || act.title.contains(query, ignoreCase = true) ||
                    act.description.contains(query, ignoreCase = true) ||
                    act.orderNumber.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun filterByCategory(cat: String) {
        selectedCategory.value = cat
    }

    fun search(q: String) {
        searchQuery.value = q
    }
}
