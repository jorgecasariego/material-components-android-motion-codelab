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

package com.materialstudies.reply.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialFadeThrough
import com.materialstudies.reply.R
import com.materialstudies.reply.data.Email
import com.materialstudies.reply.data.EmailStore
import com.materialstudies.reply.databinding.FragmentHomeBinding
import com.materialstudies.reply.ui.MainActivity
import com.materialstudies.reply.ui.MenuBottomSheetDialogFragment
import com.materialstudies.reply.ui.nav.NavigationModel

/**
 * A [Fragment] that displays a list of emails.
 */
class HomeFragment : Fragment(), EmailAdapter.EmailAdapterListener {

    private val args: HomeFragmentArgs by navArgs()

    private lateinit var binding: FragmentHomeBinding

    private val emailAdapter = EmailAdapter(this)

    // An on back pressed callback that handles replacing any non-Inbox HomeFragment with inbox
    // on back pressed.
    private val nonInboxOnBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            NavigationModel.setNavigationMenuItemChecked(NavigationModel.INBOX_ID)
            (requireActivity() as MainActivity)
                .navigateToHome(R.string.navigation_inbox, Mailbox.INBOX);
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 9. Set up MaterialFadeThrough enterTransition.
        /**
         * The above setup differs slightly from previous steps due to the way mailboxes work.
         * A mailbox is simply a HomeFragment plus an argument that tells the fragment which list
         * of emails to display. This, in addition to the fact that HomeFragment is set to
         * singleTop=true in our navigation graph, means you always navigate forward to new
         * mailboxes and don't need to deal with return or reenter transitions (transitions that
         * run when a fragment is popped). For this reason, all you need to worry about is setting
         * the HomeFragment enter and exit transitions.
         *
         * This is an example of an architectural scenario which affects the way transitions are
         * configured.
         */
        enterTransition = MaterialFadeThrough().apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Postpone explanation (3):
     * ---------------------
     * Typically, this first issue of the collapse not working is because when the Android
     * Transition system is trying to run your return transition, the list of emails hasn't been
     * inflated and populated into the RecyclerView yet. We need a way to wait until our
     * HomeFragment lays out our list before we start our transitions.The Android Transition system
     * provides methods to do just that - postponeEnterTransition and startPostponedEnterTransition.
     * If postponeEnterTransition is called, any entering transition to be run will be held until a
     * closing call to startPostponedEnterTransition is called. This gives us the opportunity to
     * "schedule" our transitions until after the RecyclerView has been populated with emails and
     * the transition is able to find the mappings you configured.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 3. Set up postponed enter transition.
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        // Only enable the on back callback if this home fragment is a mailbox other than Inbox.
        // This is to make sure we always navigate back to Inbox before exiting the app.
        nonInboxOnBackCallback.isEnabled = args.mailbox != Mailbox.INBOX
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            nonInboxOnBackCallback
        )

        binding.recyclerView.apply {
            val itemTouchHelper = ItemTouchHelper(ReboundingSwipeActionCallback())
            itemTouchHelper.attachToRecyclerView(this)
            adapter = emailAdapter
        }
        binding.recyclerView.adapter = emailAdapter

        EmailStore.getEmails(args.mailbox).observe(viewLifecycleOwner) {
            emailAdapter.submitList(it)
        }
    }

    /**
     * 1. The key part of the above snippet is the FragmentNavigatorExtras. The Android Transition
     * system will get the transition name set on the first parameter (cardView), and create a
     * mapping between that transition name and the transition name string passed in as the second
     * parameter (emailCardDetailTransitionName). This is how the system knows which views to find
     * and provide to the MaterialContainerTransform instance as the "start" and "end" views to
     * transform between.
     *
     * 4. Email list disappearing after click on item
     * The issue of the email list disappearing is because when navigating to a new Fragment using
     * the Navigation Component, the current Fragment is immediately removed and replaced with our
     * new, incoming Fragment. To keep the email list visible even after being replaced, you can
     * add an exit transition to HomeFragment.
     *
     * MDC-Android provides two transitions to do this for you - Hold and MaterialElevationScale.
     * The Hold transition simply keeps its target in its current position while
     * MaterialElevationScale runs a subtle scale animation.
     */
    override fun onEmailClicked(cardView: View, email: Email) {
        // 4. MaterialElevationScale
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

        /**
         * A Fragment is reentering when it is coming back into view after the current Fragment is
         * popped off the back stack. For HomeFragment, this happens when pressing back from
         * EmailFragment. Here, this will cause our list of emails to subtly scale in when we
         * return to the list.
         *
         * 5. Next, in order to ensure that the MaterialElevationScale transition is applied to the
         * home screen as a whole, instead of to each of the individual views in the hierarchy, mark
         * the RecyclerView in fragment_home.xml as a transition group.
         */
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.reply_motion_duration_large).toLong()
        }

        // 1. Set up MaterialElevationScale transition as exit and reenter transitions.
        val emailCardDetailTransitionName = getString(R.string.email_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(cardView to emailCardDetailTransitionName)
        val directions = HomeFragmentDirections.actionHomeFragmentToEmailFragment(email.id)
        findNavController().navigate(directions, extras)

    }

    override fun onEmailLongPressed(email: Email): Boolean {
        MenuBottomSheetDialogFragment
          .newInstance(R.menu.email_bottom_sheet_menu)
          .show(parentFragmentManager, null)

        return true
    }

    override fun onEmailStarChanged(email: Email, newValue: Boolean) {
        EmailStore.update(email.id) { isStarred = newValue }
    }

    override fun onEmailArchived(email: Email) {
        EmailStore.delete(email.id)
    }
}
