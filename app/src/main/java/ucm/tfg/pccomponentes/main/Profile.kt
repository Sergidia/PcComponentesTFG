package ucm.tfg.pccomponentes.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_profile.*
import ucm.tfg.pccomponentes.Main
import ucm.tfg.pccomponentes.R
import ucm.tfg.pccomponentes.list.MainActivity
import ucm.tfg.pccomponentes.list.SeguidosView
import ucm.tfg.pccomponentes.notifications.MyFirebaseMessagingService
import ucm.tfg.pccomponentes.notifications.NotificationDocumentObject
import ucm.tfg.pccomponentes.utilities.CheckData

class Profile : AppCompatActivity() {

    private var bottomNavigationView: BottomNavigationView? = null

    /**
     * Abrimos la ventana activity_profile para permitir
     * al usuario actualizar sus datos
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.sleep(2000)
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Generamos el listener para el menú inferior
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        setListenerBottomMenu()

        val email: String = FirebaseAuth.getInstance().currentUser?.email.toString()

        // Se comprueba que el usuario está correctamente autenticado, de lo contrario se vuelve a la vista del login
        if (email == "" && email == "null") {
            showAlert("Error de autenticación")
            FirebaseAuth.getInstance().signOut()
            showMain()
        }
        else {

            // Se inicia una instancia en Firestore para almacenar las preferencias de notificación del usuario
            val db = FirebaseFirestore.getInstance()

            // Accedemos al documento del usuario
            val notificacionesUsuario: DocumentReference = db.collection("usuarios").document(email)
            val userDoc = notificacionesUsuario.get()
            userDoc.addOnSuccessListener {

                // Convertimos el documento en un Map
                val componente: Map<String, Any> = it.getData() as Map<String, Any>

                // Marcamos el radioButton que se corresponda con la preferencia del usuario guardada en la base de datos
                if (componente["email"] as Boolean)
                    radioGroupNotif.check(notifEmail.id)

                else radioGroupNotif.check(notifPush.id)
            }
            iniciarPerfil()
        }
    }

    /**
     * Vista del perfil del usuario donde podrá modificar su contraseña y las preferencias de notificación
     */
    private fun iniciarPerfil() {

        title = "Perfil"

        userText.setText(FirebaseAuth.getInstance().getCurrentUser()?.getEmail().toString())

        // Botón para aplicar cambio en el perfil (contraseña y/o notificaciones)
        applyButton.setOnClickListener {

            // Se inicia una instancia en Firestore para almacenar las preferencias de notificación del usuario
            val db = FirebaseFirestore.getInstance()

            // Recuperamos el token del móvil del usuario
            val token: String = MyFirebaseMessagingService.getInstanceToken()

            // Accedemos al documento del usuario
            val notificacionesUsuario: DocumentReference = db.collection("usuarios").document(userText.text.toString())

            // Si la contraseña está vacía significa que se quiere cambiar sólo el tipo de notificación
            if (passwordText.text.isNotEmpty() && passwordConfirmText.text.isNotEmpty()) {

                // Si no está vacía se comprueba si coinciden, porque el usuario querrá cambiar la contraseña
                if(passwordText.text.toString() == passwordConfirmText.text.toString()) {

                    // Si la contraseña no cumple los requisitos mínimos se muestra un error
                    if (CheckData.checkPassword(passwordText.text.toString())) {

                        // Se actualiza la contraseña
                        FirebaseAuth.getInstance().getCurrentUser()
                                ?.updatePassword(passwordText.text.toString())
                                ?.addOnCompleteListener {
                                    if (it.isSuccessful){

                                        // Creamos un objeto de tipo Notification, que contiene los booleanos de los dos tipos de notificaciones según la elección del usuario
                                        // y el token del dispositivo móvil
                                        val notif: NotificationDocumentObject = NotificationDocumentObject(notifPush.isChecked, notifEmail.isChecked, token)

                                        // Insertamos el documento en la base de datos
                                        notificacionesUsuario.set(notif).addOnCompleteListener{

                                            if (it.isSuccessful){
                                                // Si se ha actualizado correctamente redirigimos al listado de componentes
                                                Toast.makeText(applicationContext,
                                                        "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                                showList()

                                            } else {
                                                Log.d("Profile", "Error: $it")
                                                showAlert("No se ha podido actualizar el usuario, compruebe las opciones de notificación seleccionadas")
                                            }
                                        }

                                    } else {
                                        Log.d("Profile", "Error: $it")
                                        showAlert("No se ha podido actualizar el usuario, compruebe el usuario y la contraseña introducidos")
                                    }
                                }
                    } else showAlert("La contraseña debe tener mínimo 6 caracteres")
                } else showAlert("Las contraseñas introducidas no son iguales ")
            } else {

                // Creamos un objeto de tipo Notification, que contiene los booleanos de los dos tipos de notificaciones según la elección del usuario
                // y el token del dispositivo móvil
                val notif: NotificationDocumentObject = NotificationDocumentObject(notifPush.isChecked, notifEmail.isChecked, token)

                // Insertamos el documento en la base de datos
                notificacionesUsuario.set(notif).addOnCompleteListener {

                    if (it.isSuccessful) {
                        // Si se ha actualizado correctamente redirigimos al listado de componentes
                        Toast.makeText(applicationContext,
                                "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        showList()

                    } else {
                        Log.d("Profile", "Error: $it")
                        showAlert("No se han podido actualizar las preferencias de notificación")
                    }
                }
            }
        }

        // Opción para borrar permanentemente la cuenta
        deleteAccount.setOnClickListener {

            val account: String = userText.text.toString()

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Eliminar cuenta")
            builder.setMessage("¿Está seguro de querer borrar permanentemente su cuenta?")

            // Creamos un diálogo de alerta con dos opciones, sí o no. Además, si se pulsa sí se vuelve a crear
            // un segundo diálogo de alerta para pedirle de nuevo la confirmación del borrado de cuenta al usuario
            builder.setPositiveButton(android.R.string.yes) { dialog, which ->

                val builderYes = AlertDialog.Builder(this)
                builderYes.setTitle("Confirmar")
                builderYes.setMessage("Va a proceder a borrar su cuenta " + account + ". ¿Confirmar?")

                builderYes.setPositiveButton(android.R.string.yes) { dialog, which ->

                    // Si tanto en la primera alerta como en la segunda se ha dado a aceptar, se procede a borrar el usuario y su documento asociado
                    FirebaseFirestore.getInstance().collection("usuarios").document(account)
                            .delete()
                            .addOnSuccessListener {
                                FirebaseAuth.getInstance().currentUser?.delete()

                                Toast.makeText(applicationContext,
                                        "Se ha borrado la cuenta", Toast.LENGTH_LONG).show()

                                showMain()
                            }
                            .addOnFailureListener {
                                e -> Log.w("Profile", "Error al borrar el documento del usuario", e)
                                showAlert("No se ha podido borrar el usuario, inténtelo más tarde")
                            }
                }
                builderYes.setNegativeButton(android.R.string.no) { dialog, which ->
                    Toast.makeText(applicationContext,
                            "Borrado de cuenta cancelado", Toast.LENGTH_SHORT).show()
                }
                builderYes.show()
            }
            builder.setNegativeButton(android.R.string.no) { dialog, which ->
                Toast.makeText(applicationContext,
                        "Borrado de cuenta cancelado", Toast.LENGTH_SHORT).show()
            }
            builder.show()
        }
    }

    /**
     * Menú inferior con la redirección a la lista de seguidos o al listado general de componentes
     */
    private fun setListenerBottomMenu() {
        bottomNavigationView?.setOnNavigationItemSelectedListener {item: MenuItem ->
            when(item.itemId) {

                R.id.principal -> {
                    showList()
                    true
                }
                R.id.seguidos -> {
                    showSeguidos()
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Redirección a la página de inicio de sesión
     */
    private fun showMain() {

        val mainIntent = Intent(this, Main::class.java)
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(mainIntent)
    }

    /**
     * Redirección a la página de lista de componentes
     */
    private fun showList() {

        val listIntent = Intent(this, MainActivity::class.java)
        startActivity(listIntent)
        finish()
    }

    /**
     * Redirección a la página de lista de componentes
     */
    private fun showSeguidos() {

        val seguidosIntent = Intent(this, SeguidosView::class.java)
        startActivity(seguidosIntent)
        finish()
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