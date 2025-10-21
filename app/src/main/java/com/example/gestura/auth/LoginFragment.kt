package com.example.gestura.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gestura.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginFragment : Fragment() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val scope = MainScope()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val emailEt = view.findViewById<EditText>(R.id.etEmail)
        val passEt  = view.findViewById<EditText>(R.id.etPassword)
        val signIn  = view.findViewById<Button>(R.id.btnSignIn)
        val signUp  = view.findViewById<Button>(R.id.btnSignUp)
        val reset   = view.findViewById<TextView>(R.id.tvReset)
        val progress= view.findViewById<ProgressBar>(R.id.progress)
        val errorTv = view.findViewById<TextView>(R.id.tvError)

        fun setLoading(b: Boolean) {
            progress.visibility = if (b) View.VISIBLE else View.GONE
            signIn.isEnabled = !b; signUp.isEnabled = !b
        }

        emailEt.addTextChangedListener { errorTv.text = "" }
        passEt.addTextChangedListener { errorTv.text = "" }

        signIn.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass  = passEt.text.toString()
            scope.launch {
                setLoading(true)
                try {
                    require(email.isNotEmpty()) { "Email required" }
                    require(pass.isNotEmpty()) { "Password required" }
                    auth.signInWithEmailAndPassword(email, pass).await()
                    toHome()
                } catch (e: IllegalArgumentException) {
                    errorTv.text = e.message
                } catch (e: Exception) {
                    errorTv.text = e.localizedMessage ?: "Sign-in failed"
                } finally {
                    setLoading(false)
                }
            }
        }

        signUp.setOnClickListener {
            val email = emailEt.text.toString().trim()
            val pass  = passEt.text.toString()
            scope.launch {
                setLoading(true)
                try {
                    require(email.isNotEmpty()) { "Email required" }
                    require(pass.length >= 6) { "Password must be ≥ 6 characters" }
                    auth.createUserWithEmailAndPassword(email, pass).await()
                    // Optional: email verification (non-blocking)
                    runCatching { auth.currentUser?.sendEmailVerification()?.await() }
                    toHome()
                } catch (e: IllegalArgumentException) {
                    errorTv.text = e.message
                } catch (e: Exception) {
                    errorTv.text = e.localizedMessage ?: "Sign-up failed"
                } finally {
                    setLoading(false)
                }
            }
        }

        reset.setOnClickListener {
            val email = emailEt.text.toString().trim()
            scope.launch {
                setLoading(true)
                try {
                    require(email.isNotEmpty()) { "Enter your email first" }
                    auth.sendPasswordResetEmail(email).await()
                    Toast.makeText(requireContext(), "Reset email sent", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    errorTv.text = e.localizedMessage ?: "Reset failed"
                } finally {
                    setLoading(false)
                }
            }
        }
    }

    private fun toHome() {
        findNavController().navigate(
            R.id.aslFragment,
            null,
            androidx.navigation.navOptions {
                popUpTo(R.id.loginFragment) { inclusive = true }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
    }
}
