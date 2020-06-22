package ucm.tfg.pccomponentes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import ucm.tfg.pccomponentes.list.MainActivity
import ucm.tfg.pccomponentes.main.Register
import ucm.tfg.pccomponentes.utilities.CheckData

class Main : AppCompatActivity() {

    /**
     * Al iniciar la aplicación abrimos la ventana activity_main para permitir
    al usuario iniciar sesión o registrarse
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.sleep(2000)
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Comprobamos si el usuario está autenticado
        val email: String = FirebaseAuth.getInstance().currentUser?.email.toString()

        // Si lo está recuperamos el token y comprobamos si debemos actualizarlo. Luego redirigimos al listado de componentes
        if (email != "" && email != "null") {
            // Comprobamos el token y lo actualizamos de hacer falta
            CheckData.actualizarToken(email)

            // Independientemente de que hayamos o no actualizado el token, redirigimos a la vista del listado de componentes si verificó su registro
            if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == true)
                showList()
        }
        // Si no está autenticado o verificado generamos la vista del login
         else iniciarMain()
    }

    /*

     */

    /**
     * Se configuran los botones y el título de la ventana. El botón de Registro nos llevará a activity_register
     * y el botón de Iniciar Sesión intentará iniciar sesión con las credenciales proporcionadas, llevándonos a
     * activity_list en caso de éxito o mostrando un error si no se ha podido iniciar sesión. El botón
     * Has olvidado tu contraseña enviará al usuario un email para poder recuperarla
     */
    private fun iniciarMain() {

        title = "Autenticación"

        signInButton.setOnClickListener {

            // Si alguno de los datos está vacío se muestra un error
            if (userText.text.isNotEmpty() && passwordText.text.isNotEmpty()) {

                // Si el usuario no es de tipo email se muestra un error
                if (CheckData.checkEmail(userText.text.toString())) {

                    // Si la contraseña no cumple los requisitos mínimos se muestra un error
                    if (CheckData.checkPassword(passwordText.text.toString())) {

                        // Se usa el tipo de inicio de sesión (ProviderType) BASIC, que es el de usuario + contraseña
                        // Habría otras opciones a configurar, como el inicio de sesión usando la cuenta de Google, de Facebook...
                        FirebaseAuth.getInstance()
                                .signInWithEmailAndPassword(userText.text.toString(),
                                        passwordText.text.toString()).addOnCompleteListener {

                                    if (it.isSuccessful){

                                        // Comprobamos si hace falta actualizar el token almacenado previamente
                                        CheckData.actualizarToken(it.result?.user?.email ?: "")

                                        // Comprobamos si el usuario ha verificado su registro por email
                                        if (FirebaseAuth.getInstance().currentUser?.isEmailVerified == true) {

                                            // Si el usuario verificó su registro redirigimos al listado de componentes
                                            showList()
                                        }

                                        // Si no verificó su registro se muestra un pop up con un recordatorio y se le vuelve a enviar el email
                                        else {
                                            showAlert("No se ha podido iniciar sesión, debe confirmar el registro desde el email recibido en su cuenta de correo")
                                            FirebaseAuth.getInstance().currentUser?.sendEmailVerification()
                                        }

                                    } else {
                                        Log.d("Login", "Error: $it")
                                        showAlert("No se ha podido iniciar sesión, compruebe los datos introducidos")
                                    }
                                }
                    } else showAlert("La contraseña debe tener mínimo 6 caracteres")
                } else showAlert("Se debe introducir una cuenta de correo electrónico")
            } else showAlert("Los campos de usuario y contraseña no pueden estar vacíos")
        }

        registerButton.setOnClickListener {
            showRegister()
        }

        forgotPassword.setOnClickListener {
            if (userText.text.isNotEmpty()) {
                FirebaseAuth.getInstance().sendPasswordResetEmail(userText.text.toString())
                Toast.makeText(this, "Se ha enviado un email para reestablecer su contraseña", Toast.LENGTH_LONG).show()
            }
            else showAlert("Introduzca su email y pulse " + getString(R.string.forgotPassword_hint))

        }
    }

    /**
     * Redirección a la página de registro
     */
    private fun showRegister() {

        val signUpIntent = Intent(this, Register::class.java)
        startActivity(signUpIntent)
    }

    /**
     * Redirección a la página del listado de componentes
     */
    private fun showList() {

        val listIntent = Intent(this, MainActivity::class.java)
        listIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(listIntent)
    }

    /**
     * Método para mostrar errores con una ventana emergente
     */
    private fun showAlert(mensajeError: String) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(mensajeError)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}