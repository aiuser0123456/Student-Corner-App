package com.studentcorner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentcorner.data.model.Resource
import com.studentcorner.data.model.ResourceFilter
import com.studentcorner.data.repository.FirebaseRepository
import com.studentcorner.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResourcesUiState(
    val isLoading: Boolean = false,
    val allResources: List<Resource> = emptyList(),
    val filteredResources: List<Resource> = emptyList(),
    val savedResourceIds: Set<String> = emptySet(),
    val filter: ResourceFilter = ResourceFilter(),
    val errorMessage: String? = null,
)

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val repository: FirebaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResourcesUiState())
    val uiState: StateFlow<ResourcesUiState> = _uiState.asStateFlow()

    init {
        loadResources()
        loadSavedResourceIds()
    }

    fun loadResources() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getAllResources()) {
                is Result.Success -> {
                    val resources = result.data
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        allResources = resources,
                        filteredResources = applyFilter(resources, _uiState.value.filter),
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false, errorMessage = result.message
                )
                is Result.Loading -> {}
            }
        }
    }

    fun loadSavedResourceIds() {
        viewModelScope.launch {
            when (val result = repository.getSavedResourceIds()) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    savedResourceIds = result.data.toSet()
                )
                else -> {}
            }
        }
    }

    fun updateFilter(filter: ResourceFilter) {
        _uiState.value = _uiState.value.copy(
            filter = filter,
            filteredResources = applyFilter(_uiState.value.allResources, filter),
        )
    }

    fun toggleSave(resourceId: String) {
        viewModelScope.launch {
            val saved = _uiState.value.savedResourceIds
            if (resourceId in saved) {
                repository.unsaveResource(resourceId)
                _uiState.value = _uiState.value.copy(
                    savedResourceIds = saved - resourceId
                )
            } else {
                repository.saveResource(resourceId)
                _uiState.value = _uiState.value.copy(
                    savedResourceIds = saved + resourceId
                )
            }
        }
    }

    private fun applyFilter(resources: List<Resource>, filter: ResourceFilter): List<Resource> {
        return resources.filter { r ->
            val matchesQuery = filter.query.isBlank() ||
                r.title.contains(filter.query, ignoreCase = true) ||
                r.description.contains(filter.query, ignoreCase = true)

            val matchesCategory = filter.category.label == "All" ||
                r.category == filter.category.label

            val matchesSubject = filter.subject.label == "All" ||
                r.subject == filter.subject.label

            val matchesStream = filter.stream.label == "All" ||
                r.stream == filter.stream.label

            val matchesClass = filter.classLevel.key.isBlank() ||
                r.classLevel == filter.classLevel.key

            matchesQuery && matchesCategory && matchesSubject && matchesStream && matchesClass
        }
    }

    fun getSavedResources(): List<Resource> {
        val saved = _uiState.value.savedResourceIds
        return _uiState.value.allResources.filter { it.id in saved }
    }
}
