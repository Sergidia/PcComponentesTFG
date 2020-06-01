package ucm.tfg.pccomponentes.list;

import ucm.tfg.pccomponentes.R;
import ucm.tfg.pccomponentes.main.Profile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends AppCompatActivity  implements Response.Listener<JSONObject>,Response.ErrorListener,SearchView.OnQueryTextListener{
   RecyclerView recycler;
   ArrayList<Item> datosTotales,datosMostrados, datosFiltrados;

   FirebaseFirestore db;
   String email;

   ProgressDialog progreso;
   Adapter rva;
   RequestQueue request;
   JsonObjectRequest jsonObjectRequest;
   JSONArray jsonArray;
   boolean isLoading;
   boolean usaBuscador;

   BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        email = getIntent().getExtras().getString("email");

        setContentView(R.layout.activity_list);
        recycler = (RecyclerView)findViewById(R.id.listItem);
        addListenerRecycler();
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.principal);
        setListenerBottomMenu();
        usaBuscador = false;
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recycler.setLayoutManager(llm);
        recycler.setHasFixedSize(true);
        datosTotales = new ArrayList<Item>();
        datosMostrados = new ArrayList<Item>();
        datosFiltrados = new ArrayList<Item>();
        handleSSLHandshake();
        request = Volley.newRequestQueue(getApplicationContext());
       // cargarWebService();
        comprobarListaIntereses();
        cargarListaComponentes();

        /*Map<String,Object> usuario= new HashMap<String,Object>();
        usuario.put("email", false);
        usuario.put("push", true);
        db.collection("usuarios").document("dmijares@ucm.es")
                .set(usuario)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("add","User added");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("error","Error adding document",e);
                    }
                });*/
    }

    private void comprobarListaIntereses(){
        db = FirebaseFirestore.getInstance();
        db.collection("usuarios").document(email).collection("interes").get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().size() == 0) {
                            }
                        }
                    }
                });

    }

    private void cargarListaComponentes() {
        db = FirebaseFirestore.getInstance();
        db.collection("componentes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String,Object> componente = new HashMap<String, Object>();
                                componente = document.getData();
                                Item aux = new Item(document.getId(),(String)componente.get("nombre"),
                                        (String)componente.get("img"),
                                        //Double.parseDouble((String)componente.get("precio")),
                                        ((Number)componente.get("precio")).doubleValue(),
                                        (String)componente.get("url"),
                                        (String)componente.get("categoria"),
                                        (Boolean)componente.get("valida"));
                                datosTotales.add(aux);
                                Log.d("hihi", document.getId() + " => " + document.getData());
                            }
                            cargarRecyclerView();
                        } else {
                            Log.d("buu", "Error getting documents: ", task.getException());
                        }
                    }
                });

    }
    private void cargarRecyclerView(){
        for(int j=0; j <10 && j< datosTotales.size();j++){
            datosMostrados.add(datosTotales.get(j));
        }
        // progreso.hide();
        this.rva= new Adapter(datosMostrados);
        rva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),datosMostrados.get(recycler.getChildAdapterPosition(v)).getCodigo(),Toast.LENGTH_SHORT).show();
                Intent componenteView = new Intent(MainActivity.this,ComponenteView.class);
                componenteView.putExtra("email",email);
                Bundle miBundle = new Bundle();
                miBundle.putSerializable("componente",datosMostrados.get(recycler.getChildAdapterPosition(v)));
                componenteView.putExtras(miBundle);
                MainActivity.this.startActivity(componenteView);
            }
        });
        recycler.setAdapter(rva);

    }

    private void setListenerBottomMenu() {
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.seguidos){

                    Intent i = new Intent(getApplicationContext(),SeguidosView.class);
                    i.putExtra("email", email);

                    startActivity(i);

                    overridePendingTransition(0,0);
                    return true;
                }
                else if(item.getItemId() == R.id.principal){

                }
                return false;
            }
        });
    }

    private void addListenerRecycler() {
        this.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (!isLoading) {
                    if (!recyclerView.canScrollVertically(1)) {
                        //bottom of list!

                        loadMore();
                        isLoading = true;


                    }
                }
            }
        });
    }

    private void loadMore() {
        if((!usaBuscador && (datosMostrados.size() != datosTotales.size())) || (usaBuscador & (datosMostrados.size() != datosFiltrados.size()))){
            datosMostrados.add(null);
            rva.notifyItemInserted(datosMostrados.size() - 1);
        }



        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if((!usaBuscador && (datosMostrados.size() != datosTotales.size())) || (usaBuscador & (datosMostrados.size() != datosFiltrados.size()))){
                    datosMostrados.remove(datosMostrados.size() - 1);
                }

                int scrollPosition = datosMostrados.size();
                rva.notifyItemRemoved(scrollPosition);
                int currentSize = scrollPosition;
                int nextLimit = currentSize + 10;
                if(usaBuscador){

                }else{
                    for(int i =0; i <10 && datosMostrados.size()<datosTotales.size(); i++){
                        datosMostrados.add(datosTotales.get(datosMostrados.size()));
                        currentSize++;
                    }
                }

                rva.notifyDataSetChanged();
                isLoading = false;
            }
        }, 2000);


    }


    private void cargarWebService() {
        this.progreso = new ProgressDialog(getApplicationContext());
        progreso.setMessage("Consultando...");
        //progreso.show();
        //String url = "http://ec2-52-47-193-84.eu-west-3.compute.amazonaws.com/prueba.php";
        String url ="https://192.168.1.37/ejemploBDRemota/wsJSONLista.php";
        jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,url,null,this,this);
        request.add(jsonObjectRequest);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Toast.makeText(getApplicationContext(),"No se puede conectar"+error.toString(),Toast.LENGTH_LONG).show();
        System.out.println();
        Log.d("ERROR: ",error.toString());
        progreso.hide();
    }

    @Override
    public void onResponse(JSONObject response) {
        try {
        Item  item = null;
        JSONArray json= response.optJSONArray("usuario");//usuario
            for(int i=0; i<json.length(); i++){
                String nombre, descrip, imagen;
                double precio;
                JSONObject jsonObject= null;
                jsonObject=json.getJSONObject(i);
                nombre = jsonObject.optString("nombre");
                descrip = jsonObject.optString("tipo");
                imagen = jsonObject.optString("imagen");
                precio = jsonObject.getDouble("precio");

                //item = new Item(nombre,descrip,imagen,precio);
               // datosTotales.add(item);
            }
            for(int j=0; j <10 && j< datosTotales.size();j++){
                datosMostrados.add(datosTotales.get(j));
            }
            progreso.hide();
             this.rva= new Adapter(datosMostrados);
            rva.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(),datosMostrados.get(recycler.getChildAdapterPosition(v)).getCodigo(),Toast.LENGTH_SHORT).show();
                    Intent componenteView = new Intent(MainActivity.this,ComponenteView.class);
                    Bundle miBundle = new Bundle();
                    miBundle.putSerializable("componente",datosMostrados.get(recycler.getChildAdapterPosition(v)));
                    componenteView.putExtras(miBundle);
                    MainActivity.this.startActivity(componenteView);
                }
            });
            recycler.setAdapter(rva);
        }
         catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(),"Error con la conexion con el servidor" +
                        " "+ response,Toast.LENGTH_LONG).show();
                progreso.hide();
            }
    }
    @SuppressLint("TrulyRandom")
    public static void handleSSLHandshake() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }};

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String arg0, SSLSession arg1) {
                    return true;
                }
            });
        } catch (Exception ignored) {
        }
    }
    @Override
    public boolean onQueryTextSubmit(String query) {
        try {
            filter(query);
            this.rva.setFilter(this.datosMostrados);
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        /*try {
            filter(newText);
            this.rva.setFilter(this.datosMostrados);
        }catch (Exception e){
            e.printStackTrace();
        }*/
        if(newText.equals("")){
            usaBuscador = false;
            datosMostrados.clear();
            datosFiltrados.clear();
            rva.notifyDataSetChanged();

        }
        return false;
    }
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.buscador,menu);
        MenuItem mi= menu.findItem(R.id.buscador);
        SearchView sv =(SearchView) mi.getActionView();
        sv.setOnQueryTextListener(this);
        mi.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {

                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                rva.setFilter(datosMostrados);
                return true;
            }
        });
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        switch (id){
            case R.id.opCerrarSesion:  Toast.makeText(getApplicationContext(),"cerrar sesion",Toast.LENGTH_SHORT).show();break;
            case R.id.op_perfil:  Intent i = new Intent(getApplicationContext(), Profile.class);
                i.putExtra("email", email);
                startActivity(i);
                overridePendingTransition(0,0);break;
            default: break;
        }
        return super.onOptionsItemSelected(item);
    }



    private void filter(String texto){
        if(texto != ""){
            try {
                usaBuscador = true;
                texto=texto.toLowerCase();
                for(Item i: this.datosTotales){
                    String itemSelec=i.getNombre().toLowerCase();
                    if(itemSelec.contains(texto)){
                        this.datosFiltrados.add(i);
                    }

                }
                this.datosMostrados.clear();
                int i = 0;
                while (i < 10 && i < datosFiltrados.size()){
                    datosMostrados.add(datosFiltrados.get(i));
                    i++;
                }
            }catch (Exception e){
                e.printStackTrace();
            }


        }

    }
}
