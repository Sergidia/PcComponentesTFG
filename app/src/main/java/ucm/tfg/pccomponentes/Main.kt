package ucm.tfg.pccomponentes

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import ucm.tfg.pccomponentes.list.MainActivity
import ucm.tfg.pccomponentes.main.Profile
import ucm.tfg.pccomponentes.main.ProviderType
import ucm.tfg.pccomponentes.main.Register
import ucm.tfg.pccomponentes.utilities.CheckData

class Main : AppCompatActivity() {

    /*
        Al iniciar la aplicación abrimos la ventana activity_main para permitir
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

            // Independientemente de que hayamos o no actualizado el token, redirigimos a la vista del listado de componentes
            showList(email)
        }
        // Si no está autenticado generamos la vista del login
         else iniciarMain()
    }

    /*
        Se configuran los botones y el título de la ventana. El botón de Registro nos llevará a activity_register
        y el botón de Iniciar Sesión intentará iniciar sesión con las credenciales proporcionadas, llevándonos a
        activity_profile en caso de éxito o mostrando un error si no se ha podido iniciar sesión
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
                                        //showProfile(it.result?.user?.email ?: "", ProviderType.BASIC)
                                        // Redirigimos al listado de componentes
                                        showList(it.result?.user?.email ?: "")
                                    } else {
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
    }

    /*
        Método para mostrar errores con una ventana emergente
     */
    private fun showAlert(mensajeError: String) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(mensajeError)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    /*
        Redirección a la página del perfil de usuario
     */
    private fun showProfile(email: String, provider: ProviderType) {

        val profileIntent = Intent(this, Profile::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(profileIntent)
    }

    /*
        Redirección a la página de registro
     */
    private fun showRegister() {

        val signUpIntent = Intent(this, Register::class.java).apply {

        }
        startActivity(signUpIntent)
    }

    /*
        Redirección a la página de lista de componentes
     */
    private fun showList(email: String) {

        val listIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("email", email)
        }
        startActivity(listIntent)
    }
}