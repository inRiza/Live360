package com.example.nimons360.ui.family.create.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.nimons360.R
import com.google.android.material.card.MaterialCardView

class IconPickerAdapter(
    private val icons: List<Int>,
    private val onIconSelected: (Int) -> Unit
) : RecyclerView.Adapter<IconPickerAdapter.IconViewHolder>() {
    private var selectedPosition = 0

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val cvIconContainer: MaterialCardView = itemView.findViewById(R.id.cvIconContainer)

        // Binding
        fun bind(iconResId: Int, position: Int) {
            ivIcon.setImageResource(iconResId)

            // Cek Status Terpilih
            val isSelected = selectedPosition == position

            if (isSelected) {
                cvIconContainer.strokeWidth = 5
                cvIconContainer.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.blue_100)
                )
            } else {
                cvIconContainer.strokeWidth = 0
                cvIconContainer.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
                )
            }

            // Handle Klik
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = bindingAdapterPosition

                notifyItemChanged(previousPosition) // hapus border icon lama
                notifyItemChanged(selectedPosition) // tambah border icon baru

                onIconSelected(iconResId)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        // Inflate Layout
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(
            R.layout.item_icon_picker,
            parent,
            false
        )

        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val currentIcon = icons[position]
        holder.bind(
            iconResId = currentIcon,
            position = position
        )
    }

    override fun getItemCount(): Int {
        return icons.size
    }
}