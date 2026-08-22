package com.pinboard.keyboard.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pinboard.keyboard.R
import com.pinboard.keyboard.data.Pin
import com.pinboard.keyboard.databinding.ActivityMainBinding
import com.pinboard.keyboard.util.CATEGORY_ALL
import com.pinboard.keyboard.util.CATEGORY_FAVORITES
import com.pinboard.keyboard.util.SortMode
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: PinViewModel by lazy {
        androidx.lifecycle.ViewModelProvider(this)[PinViewModel::class.java]
    }
    private lateinit var adapter: PinAdapter

    private val createBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { performBackup(it) }
        }

    private val openBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { performRestore(it) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSearch()
        setupRecyclerView()
        setupFab()
        observeCategories()
        observePins()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.inflateMenu(R.menu.toolbar_menu)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_recent -> { viewModel.setSortMode(SortMode.RECENT); true }
                R.id.sort_usage -> { viewModel.setSortMode(SortMode.USAGE); true }
                R.id.sort_title -> { viewModel.setSortMode(SortMode.TITLE); true }
                R.id.action_backup -> { createBackupLauncher.launch(getString(R.string.backup_file_name)); true }
                R.id.action_restore -> { openBackupLauncher.launch(arrayOf("application/json")); true }
                else -> false
            }
        }
    }

    private fun performBackup(uri: android.net.Uri) {
        viewModel.backupTo(uri) { result ->
            result.onSuccess { count ->
                Toast.makeText(this, getString(R.string.backup_success, count), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, R.string.backup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performRestore(uri: android.net.Uri) {
        viewModel.restoreFrom(uri) { result ->
            result.onSuccess { count ->
                Toast.makeText(this, getString(R.string.restore_success, count), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(this, R.string.restore_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSearch() {
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = PinAdapter(
            onClick = { pin -> openEditDialog(pin) },
            onLongClick = { pin -> confirmDelete(pin) },
            onFavoriteClick = { pin -> viewModel.toggleFavorite(pin) }
        )
        binding.recyclerPins.layoutManager = LinearLayoutManager(this)
        binding.recyclerPins.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            EditPinDialog(existingPin = null) { pin -> viewModel.addOrUpdate(pin) }
                .show(supportFragmentManager, "add_pin")
        }
    }

    private fun openEditDialog(pin: Pin) {
        EditPinDialog(existingPin = pin) { updated -> viewModel.addOrUpdate(updated) }
            .show(supportFragmentManager, "edit_pin")
    }

    private fun confirmDelete(pin: Pin) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(pin) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    rebuildCategoryChips(categories)
                }
            }
        }
    }

    private fun rebuildCategoryChips(categories: List<String>) {
        val group = binding.chipGroupCategories
        val previouslySelectedTag = group.checkedChipId.takeIf { it != -1 }
            ?.let { group.findViewById<Chip>(it)?.tag as? String } ?: CATEGORY_ALL

        group.removeAllViews()

        val allChip = createChip(getString(R.string.all_category), CATEGORY_ALL)
        group.addView(allChip)

        val favChip = createChip(getString(R.string.favorites_category), CATEGORY_FAVORITES)
        group.addView(favChip)

        categories.forEach { category ->
            group.addView(createChip(category, category))
        }

        val toSelect = group.children().firstOrNull { (it.tag as? String) == previouslySelectedTag }
            ?: allChip
        toSelect.isChecked = true
    }

    private fun createChip(label: String, tagValue: String): Chip {
        val chip = Chip(this)
        chip.text = label
        chip.tag = tagValue
        chip.isCheckable = true
        chip.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) viewModel.setCategory(view.tag as String)
        }
        return chip
    }

    private fun observePins() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pins.collect { pins ->
                    adapter.submitList(pins)
                    binding.textEmpty.visibility = if (pins.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                }
            }
        }
    }

    private fun com.google.android.material.chip.ChipGroup.children(): List<Chip> =
        (0 until childCount).mapNotNull { getChildAt(it) as? Chip }
}
