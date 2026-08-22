package com.pinboard.keyboard.keyboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.pinboard.keyboard.R
import com.pinboard.keyboard.data.Pin
import com.pinboard.keyboard.data.PinRepository
import com.pinboard.keyboard.databinding.KeyboardViewBinding
import com.pinboard.keyboard.ui.MainActivity
import com.pinboard.keyboard.util.CATEGORY_ALL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PinBoardIME : InputMethodService() {

    private enum class Tab { RECENT, FAVORITES, CATEGORIES }

    private lateinit var repository: PinRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var binding: KeyboardViewBinding
    private lateinit var themedContext: ContextThemeWrapper
    private lateinit var adapter: KeyboardPinAdapter

    private var allPins: List<Pin> = emptyList()
    private var searchQuery: String = ""
    private var selectedCategory: String = CATEGORY_ALL
    private var currentTab: Tab = Tab.RECENT

    override fun onCreate() {
        super.onCreate()
        repository = PinRepository(this)
    }

    override fun onCreateInputView(): View {
        themedContext = ContextThemeWrapper(this, R.style.Theme_PinBoard)
    val themedInflater = LayoutInflater.from(themedContext).cloneInContext(themedContext)
    binding = KeyboardViewBinding.inflate(themedInflater)

        adapter = KeyboardPinAdapter(
            onInsert = { pin -> insertPin(pin) },
            onLongPressDelete = { pin -> deletePin(pin) },
            onToggleFavorite = { pin -> toggleFavorite(pin) }
        )
        binding.kbRecyclerPins.layoutManager = LinearLayoutManager(this)
        binding.kbRecyclerPins.adapter = adapter

        binding.kbEditSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString().orEmpty()
                refreshList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        setupTabs()
        binding.kbBtnSettings.setOnClickListener { openSettings() }

        observePins()
        return binding.root
    }

    private fun setupTabs() {
        val tabViews = listOf(
            binding.kbTabRecent to Tab.RECENT,
            binding.kbTabFavorites to Tab.FAVORITES,
            binding.kbTabCategories to Tab.CATEGORIES
        )
        tabViews.forEach { (view, tab) ->
            view.setOnClickListener { selectTab(tab) }
        }
        selectTab(Tab.RECENT)
    }

    private fun selectTab(tab: Tab) {
        currentTab = tab
        binding.kbTabRecent.isSelected = tab == Tab.RECENT
        binding.kbTabFavorites.isSelected = tab == Tab.FAVORITES
        binding.kbTabCategories.isSelected = tab == Tab.CATEGORIES

        binding.kbCategoryScroll.visibility = if (tab == Tab.CATEGORIES) View.VISIBLE else View.GONE
        if (tab != Tab.CATEGORIES) {
            selectedCategory = CATEGORY_ALL
        }
        refreshList()
    }

    private fun observePins() {
        serviceScope.launch {
            repository.allPins.collect { pins ->
                allPins = pins
                rebuildCategoryChips(pins)
                refreshList()
            }
        }
    }

    private fun rebuildCategoryChips(pins: List<Pin>) {
        val group = binding.kbChipGroupCategories
        val previouslySelected = selectedCategory
        group.removeAllViews()

        val allChip = Chip(themedContext).apply {
            text = getString(R.string.all_category)
            tag = CATEGORY_ALL
            isCheckable = true
            textSize = 12f
        }
        group.addView(allChip)

        val categories = pins.map { it.category }.distinct().sorted()
        categories.forEach { category ->
            val chip = Chip(themedContext).apply {
                text = category
                tag = category
                isCheckable = true
                textSize = 12f
            }
            group.addView(chip)
        }

        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            chip.setOnCheckedChangeListener { view, isChecked ->
                if (isChecked) {
                    selectedCategory = view.tag as String
                    refreshList()
                }
            }
        }

        val toSelect = (0 until group.childCount)
            .map { group.getChildAt(it) as Chip }
            .firstOrNull { it.tag == previouslySelected }
            ?: allChip
        toSelect.isChecked = true
    }

    private fun refreshList() {
        var list = allPins

        when (currentTab) {
            Tab.RECENT -> {
                // no extra filter, just show everything sorted by recency below
            }
            Tab.FAVORITES -> list = list.filter { it.isFavorite }
            Tab.CATEGORIES -> {
                if (selectedCategory != CATEGORY_ALL) {
                    list = list.filter { it.category == selectedCategory }
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) || it.text.contains(searchQuery, ignoreCase = true)
            }
        }
        list = list.sortedByDescending { it.lastUsed }

        adapter.submitList(list)
        binding.kbRecyclerPins.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
        binding.kbTextEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    /** ⚡ درج مستقیم متن با یک لمس */
    private fun insertPin(pin: Pin) {
        currentInputConnection?.commitText(pin.text, 1)
        serviceScope.launch { repository.markUsed(pin) }
    }

    /** 🗑️ حذف با نگه‌داشتن روی پین */
    private fun deletePin(pin: Pin) {
        serviceScope.launch { repository.delete(pin) }
        Toast.makeText(this, R.string.pin_deleted, Toast.LENGTH_SHORT).show()
    }

    private fun toggleFavorite(pin: Pin) {
        serviceScope.launch { repository.update(pin.copy(isFavorite = !pin.isFavorite)) }
    }

    /** ⚙️ ورود مستقیم به مدیریت */
    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
