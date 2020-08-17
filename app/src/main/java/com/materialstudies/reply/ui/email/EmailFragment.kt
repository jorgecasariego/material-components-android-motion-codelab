/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.materialstudies.reply.ui.email

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.theme.MaterialComponentsViewInflater
import com.google.android.material.transition.MaterialContainerTransform
import com.materialstudies.reply.R
import com.materialstudies.reply.data.EmailStore
import com.materialstudies.reply.databinding.FragmentEmailBinding
import com.materialstudies.reply.util.themeColor
import kotlin.LazyThreadSafetyMode.NONE

private const val MAX_GRID_SPANS = 3

/**
 * A [Fragment] which displays a single, full email.
 */
class EmailFragment : Fragment() {

    private val args: EmailFragmentArgs by navArgs()
    private val emailId: Long by lazy(NONE) { args.emailId }

    private lateinit var binding: FragmentEmailBinding
    private val attachmentAdapter = EmailAttachmentGridAdapter(MAX_GRID_SPANS)

    /**
     * 1. For simplicity, only the sharedElementEnterTransition is being set, as opposed to the
     * sharedElementReturnTransition. By default, the Android Transition system will automatically
     * reverse the enter transition when navigating back, if no return transition is set.
     *
     * 2. drawingViewId controls where in the view hierarchy the animating container will be placed.
     * This allows you to show the transition below or above other elements in your UI. In Reply's
     * case, you're running the container transform at the same level as your fragment container to
     * ensure it's drawn below the Bottom App Bar and Floating Action Button
     *
     * 3. scrimColor is a property on MaterialContainerTransform which controls the color of a
     * translucent shade drawn behind the animating container. By default, this is set to 32% black.
     * Here it's set to transparent, meaning no scrim will be drawn. This is because in a later step
     * you will add an exit transition to the list of emails that pairs nicely with the container
     * transform.
     *
     * 4. Lastly, we set the container colors of MaterialContainerTransform to colorSurface. When
     * MaterialContainerTransform animates between two views, there are three "containers" it draws
     * to the canvas: 1) a background container 2) a container for the start view and 3) a container
     * for the end view. All three of these containers can be given a fill color and are set to
     * transparent by default. Setting these background fill colors can be useful if your start or
     * end view doesn't itself draw a background, causing other elements to be seen beneath it
     * during animation. Since all of the containers in this example should be set to colorSurface,
     * we can use the setAllContainerColors helper method to ensure we don't run into any visual
     * issues
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Set up MaterialContainerTransform transition as sharedElementEnterTransition.
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(requireContext().themeColor(R.attr.colorSurface))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentEmailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.navigationIcon.setOnClickListener {
            findNavController().navigateUp()
        }

        val email = EmailStore.get(emailId)
        if (email == null) {
            showError()
            return
        }

        binding.run {
            this.email = email

            // Set up the staggered/masonry grid recycler
            attachmentRecyclerView.layoutManager = GridLayoutManager(
                requireContext(),
                MAX_GRID_SPANS
            ).apply {
                spanSizeLookup = attachmentAdapter.variableSpanSizeLookup
            }
            attachmentRecyclerView.adapter = attachmentAdapter
            attachmentAdapter.submitList(email.attachments)
        }
    }

    private fun showError() {
        // Do nothing
    }
}