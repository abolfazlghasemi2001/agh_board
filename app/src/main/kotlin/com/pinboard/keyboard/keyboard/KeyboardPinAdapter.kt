package com.pinboard.keyboard.keyboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinboard.keyboard.R
import com.pinboard.keyboard.data.Pin
import com.pinboard.keyboard.databinding.ItemPinKeyboardBinding

class KeyboardPinAdapter(
    private val onInsert: (Pin) -> Unit,
    private val onLongPressDelete: (Pin) -> Unit,
    private val onToggleFavorite: (Pin) -> Unit
) : ListAdapter<Pin, KeyboardPinAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPinKeyboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemPinKeyboardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pin: Pin) {
            binding.kbItemTitle.text = pin.title
            binding.kbItemSnippet.text = pin.text
            binding.kbItemFavorite.setImageResource(
                if (pin.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            binding.root.setOnClickListener { onInsert(pin) }
            binding.root.setOnLongClickListener {
                onLongPressDelete(pin)
                true
            }
            binding.kbItemFavorite.setOnClickListener { onToggleFavorite(pin) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pin>() {
            override fun areItemsTheSame(oldItem: Pin, newItem: Pin) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Pin, newItem: Pin) = oldItem == newItem
        }
    }
}
