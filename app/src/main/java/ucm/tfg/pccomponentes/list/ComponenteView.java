package ucm.tfg.pccomponentes.list;

import ucm.tfg.pccomponentes.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ComponenteView extends AppCompatActivity implements Response.Listener<JSONObject>,Response.ErrorListener {
    boolean seguido;
    TextView nombre;
    TextView precio;
    TextView componente;
    TextView descripcion;
    ImageView imagen;
    CheckBox seguir;
    EditText precioNoti;
    Button btnActualiza;

   private Interes seguimiento;
    FirebaseFirestore db;

    RequestQueue request;
    JsonObjectRequest jsonObjectRequest;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_componente_view);
        nombre = findViewById(R.id.nombreComponente);
        precio = findViewById(R.id.precioComponente);
        descripcion = findViewById(R.id.descripcionComponente);
        componente = findViewById(R.id.tipoComponente);
        imagen = findViewById(R.id.imagenComponente);
        seguir = findViewById(R.id.checkBoxComponente);
        precioNoti = findViewById(R.id.precioNoti);
        btnActualiza = findViewById(R.id.btnActualizar);
        db = FirebaseFirestore.getInstance();
        Bundle recepcion = getIntent().getExtras();
        Item item = null;
        if(recepcion != null){
            item = (Item) recepcion.getSerializable("componente");
            nombre.setText(item.getNombre());
            precio.setText(String.valueOf(item.getPrecio()));
            componente.setText(item.getCategoria());
            descripcion.setText(item.getUrl());
            Picasso.get()
                    .load(item.getFoto())
                    .resize(300,300)
                    .into(imagen);
            seguido = (Boolean) recepcion.getBoolean("interesado");

            if(seguido){
                seguimiento = (Interes) recepcion.getSerializable("seguimiento");
                seguir.setChecked(true);
                precioNoti.setText(String.valueOf(seguimiento.getPrecioMax()));
            }
        }


        btnActualiza.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualizaNotificacion();
            }
        });


    }
    public void actualizaNotificacion(){
        if(seguir.isChecked()){

            if(!seguido || (seguido &&(seguimiento.getPrecioMax() != Double.parseDouble(precioNoti.getText().toString())))){
                Map<String,Object> aux = new HashMap<String, Object>();

                aux.put("codigo",seguimiento.getCodigo());
                aux.put("precio",precioNoti.getText().toString());

                db.collection("usuarios").document("mineira@ucm.es")
                        .collection("interes").document(seguimiento.getCodigo())
                        .set(aux)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                seguimiento.setPrecioMax(Double.parseDouble(precioNoti.getText().toString()));
                                if(!seguido){
                                    seguido = true;
                                    Toast.makeText(getApplicationContext(),"Ahora sigues este componente",Toast.LENGTH_SHORT).show();
                                }
                                else{
                                    Toast.makeText(getApplicationContext(),"Precio de seguimiento actualizado",Toast.LENGTH_SHORT).show();
                                }

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),"Error en la peticion",Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
        else{
            if(seguido){
                db.collection("usuarios").document("mineira@ucm.es")
                        .collection("interes").document(seguimiento.getCodigo())
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                seguido = false;
                                Toast.makeText(getApplicationContext(),"Ya no sigues este componente",Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(getApplicationContext(),"Error al intentar dejar de seguir el producto",Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
    private void cargarWebService() {
        request = Volley.newRequestQueue(getApplicationContext());
        String url ="https://192.168.1.37/ejemploBDRemota/wsJSONLista.php";
        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,url,null,this,this);
        request.add(jsonObjectRequest);
    }
    @Override
    public void onErrorResponse(VolleyError error) {

    }

    @Override
    public void onResponse(JSONObject response) {
        try {
            Item  item = null;
            JSONArray json= response.optJSONArray("consulta");
            if(json != null){
                if(json.length() ==1){
                    seguido = true;
                    seguir.setChecked(true);
                    precioNoti.setText(json.getJSONObject(0).optInt("precioNoti"));
                }
                else{
                    seguido = false;
                    seguir.setChecked(false);
                    precioNoti.setText(0);
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Error con la conexion con el servidoR" +
                    " "+ response,Toast.LENGTH_LONG).show();

        }
    }
}