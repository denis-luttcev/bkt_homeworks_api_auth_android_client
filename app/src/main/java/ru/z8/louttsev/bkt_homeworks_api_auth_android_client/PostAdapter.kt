package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
class PostAdapter(private val filler: LayoutFiller) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_card_layout, parent, false)

        return PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return repository.getListItemCount()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, itemPosition: Int) {
        val post = repository.getItemByPosition(itemPosition)
        val postPosition = repository.getPostPosition(itemPosition)

        filler.initPostCardLayout(holder.itemView, post, postPosition)
        filler.initPostView(holder.itemView, post)
    }
}

class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)