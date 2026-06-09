package com.studentcorner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studentcorner.data.local.DownloadedPdfEntity
import com.studentcorner.data.local.PdfDownloadRepository
import com.studentcorner.data.model.Resource
import com.studentcorner.data.model.ResourceFilter
import com.studentcorner.data.repository.FirebaseRepository
import com.studentcorner.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResourcesUiState(
    val isLoading: Boolean = false,
    val allResources: List<Resource> = emptyList(),
    val filteredResources: List<Resource> = emptyList(),
    // saved ids are now a StateFlow kept in sync with Firestore
    val savedResourceIds: Set<String> = emptySet(),
    val filter: ResourceFilter = ResourceFilter(),
    val errorMessage: String? = null,
    // download state
    val downloadingIds: Set<String> = emptySet(),
    val downloadProgress: Map<String, Int> = emptyMap(),
    val downloadedIds: Set<String> = emptySet(),
    val downloadedPdfs: List<DownloadedPdfEntity> = emptyList(),
)

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val repository: FirebaseRepository,
    private val pdfRepo: PdfDownloadRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResourcesUiState())
    val uiState: StateFlow<ResourcesUiState> = _uiState.asStateFlow()

    init {
        loadResources()
        loadSavedResourceIds()
        observeDownloads()
    }

    // ── Resources ─────────────────────────────────────────────────────────────

    fun loadResources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val res = repository.getAllResources()) {
                is Result.Success -> _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        allResources = res.data,
                        filteredResources = applyFilter(res.data, s.filter),
                    )
                }
                is Result.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = res.message) }
                else -> {}
            }
        }
    }

    fun updateFilter(filter: ResourceFilter) {
        _uiState.update { s ->
            s.copy(filter = filter, filteredResources = applyFilter(s.allResources, filter))
        }
    }

    private fun applyFilter(resources: List<Resource>, filter: ResourceFilter): List<Resource> =
        resources.filter { r ->
            val q = filter.query
            (q.isBlank() || r.title.contains(q, true) || r.description.contains(q, true)) &&
            (filter.category.label == "All" || r.category == filter.category.label) &&
            (filter.subject.label == "All" || r.subject == filter.subject.label) &&
            (filter.stream.label == "All" || r.stream == filter.stream.label) &&
            (filter.classLevel.key.isBlank() || r.classLevel == filter.classLevel.key)
        }

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    private fun loadSavedResourceIds() {
        viewModelScope.launch {
            when (val res = repository.getSavedResourceIds()) {
                is Result.Success -> _uiState.update { it.copy(savedResourceIds = res.data.toSet()) }
                else -> {}
            }
        }
    }

    /**
     * Toggle bookmark and immediately update state — no need to re-open the tab.
     */
    fun toggleSave(resourceId: String) {
        viewModelScope.launch {
            val current = _uiState.value.savedResourceIds
            if (resourceId in current) {
                // Optimistic remove
                _uiState.update { it.copy(savedResourceIds = current - resourceId) }
                repository.unsaveResource(resourceId)
            } else {
                // Optimistic add
                _uiState.update { it.copy(savedResourceIds = current + resourceId) }
                repository.saveResource(resourceId)
            }
        }
    }

    fun getSavedResources(): List<Resource> {
        val saved = _uiState.value.savedResourceIds
        return _uiState.value.allResources.filter { it.id in saved }
    }

    // ── Downloads ─────────────────────────────────────────────────────────────

    private fun observeDownloads() {
        viewModelScope.launch {
            pdfRepo.allDownloads.collect { list ->
                _uiState.update { s ->
                    s.copy(
                        downloadedIds = list.map { it.resourceId }.toSet(),
                        downloadedPdfs = list,
                    )
                }
            }
        }
    }

    fun downloadPdf(resource: Resource) {
        val url = resource.downloadUrl ?: resource.pdfUrl ?: return
        viewModelScope.launch {
            _uiState.update { s ->
                s.copy(
                    downloadingIds = s.downloadingIds + resource.id,
                    downloadProgress = s.downloadProgress + (resource.id to 0),
                )
            }
            pdfRepo.downloadPdf(
                resourceId = resource.id,
                title = resource.title,
                subject = resource.subject,
                url = url,
                onProgress = { pct ->
                    _uiState.update { s ->
                        s.copy(downloadProgress = s.downloadProgress + (resource.id to pct))
                    }
                },
            )
            _uiState.update { s ->
                s.copy(
                    downloadingIds = s.downloadingIds - resource.id,
                    downloadProgress = s.downloadProgress - resource.id,
                )
            }
        }
    }

    fun getLocalPdfFile(resourceId: String) = pdfRepo.getLocalFile(resourceId)

    fun deletePdfs(ids: List<String>) {
        viewModelScope.launch { pdfRepo.deletePdfs(ids) }
    }
}
