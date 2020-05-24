package ucm.tfg.pccomponentes.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_profile.*
import ucm.tfg.pccomponentes.R
import ucm.tfg.pccomponentes.list.MainActivity
import ucm.tfg.pccomponentes.notifications.MyFirebaseMessagingService
import ucm.tfg.pccomponentes.notifications.NotificationDocumentObject
import ucm.tfg.pccomponentes.utilities.CheckData

class Profile : AppCompatActivity() {

    /*
       Abrimos la ventana activity_register para permitir
       al usuario registrarse introduciendo todos los datos
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.sleep(2000)
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        iniciarPerfil()
    }

    /*
        Se configuran los botones y el título de la ventana. El botón de Registro intentará registrarse
         con las datos proporcionados, llevándonos a activity_profile en caso de éxito o
         mostrando un error si no se ha podido registrar el usuario
     */
    private fun iniciarPerfil() {

        title = "Perfil"

        userText.setText(FirebaseAuth.getInstance().getCurrentUser()?.getEmail().toString())

        applyButton.setOnClickListener {
            if (passwordText.text.isNotEmpty() && passwordConfirmText.text.isNotEmpty() && passwordText.text.isNotEmpty().equals(passwordConfirmText.text.isNotEmpty())) {

                // Si la contraseña no cumple los requisitos mínimos se muestra un error
                if (CheckData.checkPassword(passwordText.text.toString())) {

                    // Se actualiza la contraseña
                    FirebaseAuth.getInstance().getCurrentUser()
                            ?.updatePassword(passwordText.text.toString())
                                ?.addOnCompleteListener {
                                    if (it.isSuccessful){

                                        // Si los datos son correctos se inicia una instancia en Firestore para almacenar las preferencias de notificación del usuario
                                        val db = FirebaseFirestore.getInstance()

                                        // Recuperamos el token del móvil del usuario
                                        val token: String = MyFirebaseMessagingService.getInstanceToken()

                                        // Accedemos al documento del usuario
                                        val notificacionesUsuario: DocumentReference = db.collection("usuarios").document(userText.text.toString())

                                        // Creamos un objeto de tipo Notification, que contiene los booleanos de los dos tipos de notificaciones según la elección del usuario y el token del dispositivo móvil
                                        val notif: NotificationDocumentObject = NotificationDocumentObject(notifPush.isChecked, notifEmail.isChecked, token)

                                        // Insertamos el documento en la base de datos
                                        notificacionesUsuario.set(notif).addOnCompleteListener{
                                            if (it.isSuccessful){
                                                // Si se ha actualizado correctamente redirigimos al listado de componentes
                                                showList(userText.text.toString())
                                            } else {
                                                showAlert(it.toString())
                                                showAlert("No se ha podido actualizar el usuario, compruebe las opciones de notificación seleccionadas")
                                            }

                                        }

                                    } else {
                                        showAlert(it.toString())
                                        showAlert("No se ha podido actualizar el usuario, compruebe el usuario y la contraseña introducidos")
                                    }
                            }
                } else showAlert("La contraseña debe tener mínimo 6 caracteres")
            } else showAlert("Los campos de usuario y contraseña no pueden estar vacíos")
        }
    }

    private fun showAlert(mensajeError: String) {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(mensajeError)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showProfile(email: String, provider: ProviderType) {

        val profileIntent = Intent(this, Profile::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(profileIntent)
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