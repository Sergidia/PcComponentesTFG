package ucm.tfg.pccomponentes.list;

import ucm.tfg.pccomponentes.Main;
import ucm.tfg.pccomponentes.R;
import ucm.tfg.pccomponentes.main.Profile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SeguidosView extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView recycler;
    private ArrayList<Item> datosTotales,datosMostrados, datosFiltrados;
    private ArrayList<Interes> listaIntereses;
    private HashMap<String, Interes> mapaInteres;

    private FirebaseFirestore db;
    private String email;

    private Adapter rva;
    private boolean isLoading;
    private boolean usaBuscador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTitle("Listado de seguimiento");
        super.onCreate(savedInstanceState);

        try {
            email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            // Se comprueba que el usuario está correctamente autenticado, de lo contrario se vuelve a la vista del login
            if (email == null || email.equals("") || email.equals("null")) {
                showAlert("Error de autenticación");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));

            } else {
                setContentView(R.layout.activity_seguidos_view);
                recycler = findViewById(R.id.listSeguidos);
                addListenerRecycler();

                bottomNavigationView = findViewById(R.id.bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.seguidos);
                setListenerBottomMenu();
                usaBuscador = false;

                LinearLayoutManager llm = new LinearLayoutManager(this);
                llm.setOrientation(LinearLayoutManager.VERTICAL);
                recycler.setLayoutManager(llm);

                recycler.setHasFixedSize(true);
                datosTotales = new ArrayList<>();
                datosMostrados = new ArrayList<>();
                datosFiltrados = new ArrayList<>();
                listaIntereses = new ArrayList<>();
                mapaInteres = new HashMap<>();

                cargarLista();
            }
        }
            catch (Exception e) {
                showAlert("Error interno");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));
            }

    }

    /**
     * Recuperación de los componentes de la base de datos
     */
    private void cargarLista() {

        db = FirebaseFirestore.getInstance();
        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                Map<String,Object> interes = document.getData();
                                Interes aux = new Interes(document.getId(),((Number)interes.get("precio")).doubleValue());
                                listaIntereses.add(aux);
                                mapaInteres.put(aux.getCodigo(),aux);
                            }

                            for(int i = 0; i< listaIntereses.size(); i++){

                                db.collection("componentes")
                                        .document(listaIntereses.get(i).getCodigo())
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {

                                                    DocumentSnapshot document = task.getResult();
                                                    Map<String,Object> componente = document.getData();

                                                    Item aux = new Item((String)componente.get("codigo"),(String)componente.get("nombre"),
                                                            (String)componente.get("img"),
                                                            ((Number)componente.get("precio")).doubleValue(),
                                                            (String)componente.get("url"),
                                                            (String)componente.get("categoria"),
                                                            (Boolean)componente.get("valida"));

                                                    datosTotales.add(aux);

                                                    if(datosTotales.size() == listaIntereses.size()){
                                                        cargarRecyclerView();
                                                    }

                                                } else {
                                                    Log.d("Listado", "Error al obtener los componentes seguidos: ", task.getException());
                                                }
                                            }
                                        });
                            }
                        }
                    }
                });
    }

    /**
     * Carga de la ficha del componente
     */
    private void cargarRecyclerView() {

        for(int j=0; j <10 && j< datosTotales.size();j++){
            datosMostrados.add(datosTotales.get(j));
        }

        this.rva= new Adapter(datosMostrados);
        rva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean interesado = false;
                Intent componenteView = new Intent(SeguidosView.this,ComponenteView.class);
                Bundle miBundle = new Bundle();

                if(mapaInteres.containsKey(datosMostrados.get(recycler.getChildAdapterPosition(v)).getCodigo())){
                    interesado = true;
                    miBundle.putSerializable("seguimiento",mapaInteres.get(datosMostrados.get(recycler.getChildAdapterPosition(v)).getCodigo()));
                }

                miBundle.putBoolean("interesado", interesado);
                miBundle.putSerializable("componente", datosMostrados.get(recycler.getChildAdapterPosition(v)));
                componenteView.putExtras(miBundle);

                SeguidosView.this.startActivity(componenteView);
            }
        });

        recycler.setAdapter(rva);
    }

    /**
     * Opciones de la hamburguesa: se redirecciona al perfil o se cierra sesión
     *
     * @param item menuItem
     * @return opcion elegida
     */
    public boolean onOptionsItemSelected(MenuItem item){

        int id = item.getItemId();
        Intent i;

        switch (id){

            case R.id.opCerrarSesion:
                FirebaseAuth.getInstance().signOut();
                i = new Intent(getApplicationContext(), Main.class);
                startActivity(i);
                overridePendingTransition(0,0);
                break;

            case R.id.op_perfil:
                i = new Intent(getApplicationContext(), Profile.class);
                startActivity(i);
                overridePendingTransition(0,0);
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Menú inferior con la redirección a la lista de seguidos o al listado general de componentes (en este último caso se refresca la vista)
     */
    private void setListenerBottomMenu() {

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if(item.getItemId() == R.id.seguidos){

                    recreate();
                    overridePendingTransition(0,0);

                    return true;
                }
                else if(item.getItemId() == R.id.principal){

                    Intent i = new Intent(getApplicationContext(),MainActivity.class);
                    startActivity(i);
                    overridePendingTransition(0,0);

                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Listener del recycler referente a los scrolls
     */
    private void addListenerRecycler() {

        /**
         * Creación del listener para el scroll
         */
        this.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            /**
             * Se comprueba si el usuario ha llegado al final del listado haciendo scroll
             *
             * @param recyclerView recyclerView
             * @param dx eje x
             * @param dy eje y
             */
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (!isLoading) {
                    if (!recyclerView.canScrollVertically(1)) {

                        //Final del listado, se llama al método que carga 10 más
                        loadMore();
                        isLoading = true;
                    }
                }
            }
        });
    }

    /**
     * Aumementa el número de componentes mostrados en el listado en 10
     */
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
                if(!usaBuscador)
                    for(int i =0; i <10 && datosMostrados.size()<datosTotales.size(); i++){
                        datosMostrados.add(datosTotales.get(datosMostrados.size()));
                    }

                rva.notifyDataSetChanged();
                isLoading = false;
            }
        }, 2000);
    }

    /**
     * Informa de que se ha aplicado un filtro al listado de componentes
     * @param query filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextSubmit(String query) {

        try {
            filter(query);
            this.rva.setFilter(this.datosMostrados);
        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }

    /**
     * Informa si se ha cambiado el texto del filtro de componentes
     * @param newText filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextChange(String newText) {

        if(newText.equals("")){
            usaBuscador = false;
            datosMostrados.clear();
            datosFiltrados.clear();
            rva.notifyDataSetChanged();
        }

        return true;
    }

    /**
     * Menú superior, con el filtro de componentes y la hamburguesa para la redirección al perfil y el cierre de sesión
     * @param menu menu de la aplicación
     * @return true
     */
    public boolean onCreateOptionsMenu(Menu menu){

        getMenuInflater().inflate(R.menu.buscador, menu);
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

    /**
     * Filtrado de componentes por texto
     *
     * @param texto filtro del usuario
     */
    private void filter(String texto){

        if(!texto.equals("")){
            try {

                usaBuscador = true;
                texto=texto.toLowerCase();
                for(Item i: this.datosTotales){
                    String itemSelec=i.getCodigo();
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

    /**
     * Método para mostrar errores con una ventana emergente
     *
     * @param mensajeError texto a mostrar
     */
    private void showAlert(String mensajeError) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Error");
        builder.setMessage(mensajeError);
        builder.setPositiveButton("Aceptar", null);
        AlertDialog dialog  = builder.create();
        dialog.show();
    }
}

