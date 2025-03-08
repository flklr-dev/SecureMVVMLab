package com.example.securemvvm.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home/{email}") {
        fun createRoute(email: String) = "home/$email"
    }
}