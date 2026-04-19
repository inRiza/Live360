package com.example.nimons360.ui.home.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nimons360.R
import com.example.nimons360.data.remote.dto.common.FamilyDiscover
import com.example.nimons360.databinding.ItemDiscoverFamilyBinding

class DiscoverFamilyAdapter(
    private val families: List<FamilyDiscover>,
    private val onJoinClick: (FamilyDiscover) -> Unit,
    private val onItemClick: (FamilyDiscover) -> Unit
) : RecyclerView.Adapter<DiscoverFamilyAdapter.ViewHolder>() {

    // ava colors
    private val avatarColors = listOf(
        0xFF4CAF50.toInt(), // green
        0xFF2196F3.toInt(), // blue
        0xFFE91E63.toInt(), // pink
    )

    inner class ViewHolder(val binding: ItemDiscoverFamilyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDiscoverFamilyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = families.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = families[position]
        val b = holder.binding

        b.tvFamilyName.text = item.name ?: "Unknown"

        Glide.with(b.root.context)
            .load(item.iconUrl)
            .placeholder(com.example.nimons360.R.drawable.bg_circle)
            .into(b.imgFamilyIcon)

        val members = item.members ?: emptyList()
        val avatarViews = listOf(b.member1, b.member2, b.member3)

        avatarViews.forEach { it.visibility = View.GONE }
        b.memberExtra.visibility = View.GONE

        members.take(3).forEachIndexed { index, member ->
            avatarViews[index].apply {
                visibility = View.VISIBLE
                text = getInitial(member.fullName)
                setTextColor(Color.WHITE)
                background.setTint(avatarColors[index % avatarColors.size])
            }
        }

        if (members.size > 3) {
            b.memberExtra.apply {
                visibility = View.VISIBLE
                text = "+${members.size - 3}"
            }
        }

        b.root.setOnClickListener { onItemClick(item) }
        b.btnJoin.setOnClickListener { onJoinClick(item) }
    }

    private fun getInitial(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        return name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .map { it[0].uppercaseChar() }
            .take(2)
            .joinToString("")
    }
}