package com.pinboard.keyboard.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pinboard.keyboard.R
import com.pinboard.keyboard.data.Pin
import com.pinboard.keyboard.databinding.ItemPinBinding

class PinAdapter(
    private val onClick: (Pin) -> Unit,
    private val onLongClick: (Pin) -> Unit,
    private val onFavoriteClick: (Pin) -> Unit
) : ListAdapter<Pin, PinAdapter.PinViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val binding = ItemPinBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PinViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PinViewHolder(private val binding: ItemPinBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pin: Pin) {
            binding.textTitle.text = pin.title
            binding.textSnippet.text = pin.text
            binding.textCategory.text = pin.category
            binding.textUsage.text = binding.root.context.getString(R.string.usage_count, pin.useCount)
            binding.btnFavorite.setImageResource(
                if (pin.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )

            binding.root.setOnClickListener { onClick(pin) }
            binding.root.setOnLongClickListener {
                onLongClick(pin)
                true
            }
            binding.btnFavorite.setOnClickListener { onFavoriteClick(pin) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Pin>() {
            override fun areItemsTheSame(oldItem: Pin, newItem: Pin) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Pin, newItem: Pin) = oldItem == newItem
        }
    }
}
