package com.example.securemvvm.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.securemvvm.ui.login.LoginScreen
import com.example.securemvvm.ui.registration.RegistrationScreen
import com.example.securemvvm.ui.home.HomeScreen
import androidx.fragment.app.FragmentActivity

@Composable
fun NavGraph(navController: NavHostController, activity: FragmentActivity) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = { email ->
                    Log.d("NavGraph", "Login success, navigating to home with email: $email")
                    navController.navigate(Screen.Home.createRoute(email)) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                activity = activity
            )
        }

        composable(Screen.Register.route) {
            RegistrationScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Home.route,
            arguments = listOf(
                navArgument("email") { 
                    type = NavType.StringType 
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email")
            Log.d("NavGraph", "Displaying HomeScreen with email: $email")
            HomeScreen(email = email, navController = navController)
        }
    }
}

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home/{email}") {
        fun createRoute(email: String) = "home/$email"
    }
}  