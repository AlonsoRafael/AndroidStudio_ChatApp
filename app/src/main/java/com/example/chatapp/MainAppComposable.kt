package com.example.chatapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chatapp.feature.auth.signin.SignInScreen
import com.example.chatapp.feature.auth.signup.SignUpScreen
import com.example.chatapp.feature.auth.forgotpassword.ForgotPasswordScreen
import com.example.chatapp.feature.chat.ChatScreen
import com.example.chatapp.feature.home.HomeScreen
import com.example.chatapp.feature.profile.ProfileScreen
import com.example.chatapp.feature.profile.UserProfileViewScreen
import com.example.chatapp.feature.contacts.ContactsScreen
import com.example.chatapp.feature.groups.GroupsScreen
import com.example.chatapp.feature.groups.GroupDetailsScreen
import com.example.chatapp.feature.groups.GroupProfileScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun MainApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val start = if (currentUser != null) "home" else "login"
        NavHost(navController = navController, startDestination = start) {

            composable("login") {
                SignInScreen(navController)
            }
            composable("signup") {
                SignUpScreen(navController)
            }
            composable("forgot-password") {
                ForgotPasswordScreen(navController)
            }
            composable("home") {
                HomeScreen(navController)
            }
            composable("profile") {
                ProfileScreen(navController)
            }
            composable("contacts") {
                ContactsScreen(navController)
            }
            composable("groups") {
                GroupsScreen(navController)
            }
            composable("group-details/{groupId}", arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )) {
                val groupId = it.arguments?.getString("groupId") ?: ""
                GroupDetailsScreen(navController, groupId)
            }
            composable("group-profile/{groupId}", arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                }
            )) {
                val groupId = it.arguments?.getString("groupId") ?: ""
                GroupProfileScreen(navController, groupId)
            }
            composable("user-profile/{userId}", arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                }
            )) {
                val userId = it.arguments?.getString("userId") ?: ""
                UserProfileViewScreen(navController, userId)
            }
            composable("chat/{channelId}&{channelName}", arguments = listOf(
                navArgument("channelId") {
                    type = NavType.StringType
                },
                navArgument("channelName") {
                    type = NavType.StringType
                }
            )) {
                val channelId = it.arguments?.getString("channelId") ?: ""
                val channelName = it.arguments?.getString("channelName") ?: ""
                ChatScreen(navController, channelId, channelName)
            }
            composable("group-chat/{groupId}&{groupName}", arguments = listOf(
                navArgument("groupId") {
                    type = NavType.StringType
                },
                navArgument("groupName") {
                    type = NavType.StringType
                }
            )) {
                val groupId = it.arguments?.getString("groupId") ?: ""
                val groupName = it.arguments?.getString("groupName") ?: ""
                ChatScreen(navController, groupId, groupName, isGroup = true)
            }
        }
    }
}