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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

    private RecyclerView recycler;
    private ArrayList<Item> listadoComponentes;
    private ArrayList<Interes> listadoSeguidos;
    private HashMap<String, Interes> mapaInteres;
    String filtro = "";

    private Adapter rva;
    private boolean isLoading;
    private  boolean primeraVez;

    private FirebaseFirestore db;
    private String email;
    private DocumentSnapshot lastVisibleSeguido;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isLoading = true;
        primeraVez = true;
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

                LinearLayoutManager llm = new LinearLayoutManager(this);
                llm.setOrientation(LinearLayoutManager.VERTICAL);
                recycler.setLayoutManager(llm);

                recycler.setHasFixedSize(true);
                listadoComponentes = new ArrayList<>();
                listadoSeguidos = new ArrayList<>();
                mapaInteres = new HashMap<>();

                db = FirebaseFirestore.getInstance();
                isLoading = true;
                iniciarListaComponentes();
            }
        }
            catch (Exception e) {
                showAlert("Error interno");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));
            }

    }

    /**
     * Inicialización de la lista de componentes recuperando los 10 primeros seguidos de la base de datos
     */
    private void iniciarListaComponentes() {

        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            Map<String,Object> interes = document.getData();
                            Interes aux = new Interes(
                                    document.getId(),
                                    ((Number)interes.get("precio")).doubleValue()
                            );

                            listadoSeguidos.add(aux);
                            mapaInteres.put(aux.getCodigo(), aux);
                            setLastVisibleSeguido(document);
                        }

                        for(int i = 0; i < listadoSeguidos.size(); i++){

                            Task<DocumentSnapshot> task = db.collection("componentes")
                                    .document(listadoSeguidos.get(i).getCodigo())
                                    .get();

                            addComponente(task); //Hay que cambiar los OnComplete de ampliarListaComponentes y filtrarListaComponentes por OnSuccess y luego llamar a esta función para reutilizar código
                        }

                        /*for(int i = 0; i < listadoSeguidos.size(); i++){

                            Log.d("Inicio", String.valueOf(i));
                            db.collection("componentes")
                                    .document(listadoSeguidos.get(i).getCodigo())
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful()) {

                                                DocumentSnapshot document = task.getResult();
                                                Map<String,Object> componente = document.getData();

                                                Item component = new Item(
                                                        document.getId(),
                                                        (String)componente.get("nombre"),
                                                        (String)componente.get("img"),
                                                        ((Number)componente.get("precio")).doubleValue(),
                                                        (String)componente.get("url"),
                                                        (String)componente.get("categoria"),
                                                        (Boolean)componente.get("valida"));

                                                addListadoComponentes(component);
                                            }
                                        }
                                    });
                        }*/
                        Log.d("Listado antes recycler", String.valueOf(listadoComponentes.size()));
                        if(listadoComponentes.size() == listadoSeguidos.size()){
                            cargarRecyclerView();
                            isLoading = false;
                        }
                    }
                });
    }

    /**
     * Ampliación de la lista de componentes que se produce cuando el usuario hace scroll y llega al final de la lista actual. Se cargan 10 nuevos componentes a partir del
     * último cargado anteriormente (lastVisibleSeguido). También se tiene en cuenta si se ha aplicado un filtro antes de hacer scroll
     */
    private void ampliarListaComponentes() {

        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .startAfter(lastVisibleSeguido)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            int cont = listadoSeguidos.size();
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                Map<String,Object> interes = document.getData();
                                Interes aux = new Interes(
                                        document.getId(),
                                        ((Number)interes.get("precio")).doubleValue()
                                );

                                listadoSeguidos.add(aux);
                                mapaInteres.put(aux.getCodigo(),aux);
                                setLastVisibleSeguido(document);
                            }

                            for(int i = cont; i < listadoSeguidos.size(); i++){

                                db.collection("componentes")
                                        .document(listadoSeguidos.get(i).getCodigo())
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {

                                                    DocumentSnapshot document = task.getResult();
                                                    Map<String,Object> componente = document.getData();

                                                    if (filtro.equals("")) {

                                                        Item component = new Item(
                                                                document.getId(),
                                                                (String)componente.get("nombre"),
                                                                (String)componente.get("img"),
                                                                ((Number)componente.get("precio")).doubleValue(),
                                                                (String)componente.get("url"),
                                                                (String)componente.get("categoria"),
                                                                (Boolean)componente.get("valida"));

                                                        listadoComponentes.add(component);
                                                        rva.notifyDataSetChanged();
                                                    }
                                                    else {

                                                        String nombre = (String)componente.get("nombre");

                                                        if (nombre.contains(filtro)) {

                                                            Item component = new Item(
                                                                    document.getId(),
                                                                    (String)componente.get("nombre"),
                                                                    (String)componente.get("img"),
                                                                    ((Number)componente.get("precio")).doubleValue(),
                                                                    (String)componente.get("url"),
                                                                    (String)componente.get("categoria"),
                                                                    (Boolean)componente.get("valida"));

                                                            listadoComponentes.add(component);
                                                        }
                                                    }
                                                }
                                            }
                                        });
                            }
                        }

                    }
                });
        isLoading = false;
    }

    /**
     * Recuperación de los 10 primeros componentes según el filtro del nombre escrito por el usuario
     */
    private void filtrarListaComponentes() {

        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                Map<String,Object> interes = document.getData();
                                Interes aux = new Interes(
                                        document.getId(),
                                        ((Number)interes.get("precio")).doubleValue()
                                );

                                listadoSeguidos.add(aux);
                                mapaInteres.put(aux.getCodigo(),aux);
                                setLastVisibleSeguido(document);
                            }

                            for(int i = 0; i < listadoSeguidos.size(); i++){

                                db.collection("componentes")
                                        .document(listadoSeguidos.get(i).getCodigo())
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {

                                                    DocumentSnapshot document = task.getResult();
                                                    Map<String,Object> componente = document.getData();

                                                    String nombre = (String)componente.get("nombre");

                                                    if (nombre.contains(filtro)) {

                                                        Item component = new Item(
                                                                document.getId(),
                                                                (String)componente.get("nombre"),
                                                                (String)componente.get("img"),
                                                                ((Number)componente.get("precio")).doubleValue(),
                                                                (String)componente.get("url"),
                                                                (String)componente.get("categoria"),
                                                                (Boolean)componente.get("valida"));

                                                        listadoComponentes.add(component);
                                                    }
                                                }
                                            }
                                        });
                            }
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Añade un componente a la lista de componentes
     *
     * @param task documento del componente
     */
    public void addComponente(Task<DocumentSnapshot> task) {

        task.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                Map<String, Object> componente = documentSnapshot.getData();

                if (filtro.equals("")) {

                    Item component = new Item(
                            documentSnapshot.getId(),
                            (String) componente.get("nombre"),
                            (String) componente.get("img"),
                            ((Number) componente.get("precio")).doubleValue(),
                            (String) componente.get("url"),
                            (String) componente.get("categoria"),
                            (Boolean) componente.get("valida"));

                    listadoComponentes.add(component);

                } else {

                    String nombre = (String) componente.get("nombre");

                    if (nombre.contains(filtro)) {

                        Item component = new Item(
                                documentSnapshot.getId(),
                                (String) componente.get("nombre"),
                                (String) componente.get("img"),
                                ((Number) componente.get("precio")).doubleValue(),
                                (String) componente.get("url"),
                                (String) componente.get("categoria"),
                                (Boolean) componente.get("valida"));

                        listadoComponentes.add(component);
                    }
                }

                if(listadoComponentes.size() == listadoSeguidos.size()){
                    if(primeraVez){
                        cargarRecyclerView();
                        primeraVez = false;
                    }
                    rva.notifyDataSetChanged();
                    isLoading = false;
                }

            }
        });

        Log.d("Listado addComponent", String.valueOf(listadoComponentes.size()));
    }

    /**
     * Crea las fichas de los componentes y redirecciona a la vista ComponentView cuando se selecciona una de ellas
     */
    private void cargarRecyclerView() {

        Log.d("Listado recycler", String.valueOf(listadoComponentes.size()));
        Log.d("Listado recycler seg", String.valueOf(listadoSeguidos.size()));

        this.rva= new Adapter(listadoComponentes);
        rva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent componenteView = new Intent(SeguidosView.this, ComponenteView.class);
                Bundle miBundle = new Bundle();
                miBundle.putSerializable("componente", listadoComponentes.get(recycler.getChildAdapterPosition(v)));
                componenteView.putExtras(miBundle);

                SeguidosView.this.startActivity(componenteView);
            }
        });

        recycler.setAdapter(rva);
    }

    /*
    private void cargarRecyclerView() {

        this.rva= new Adapter(listadoComponentes);
        rva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean interesado = false;
                Intent componenteView = new Intent(SeguidosView.this,ComponenteView.class);
                Bundle miBundle = new Bundle();

                if(mapaInteres.containsKey(listadoComponentes.get(recycler.getChildAdapterPosition(v)).getCodigo())){
                    interesado = true;
                    miBundle.putSerializable("seguimiento",mapaInteres.get(listadoComponentes.get(recycler.getChildAdapterPosition(v)).getCodigo()));
                }

                miBundle.putBoolean("interesado", interesado);
                miBundle.putSerializable("componente", listadoComponentes.get(recycler.getChildAdapterPosition(v)));
                componenteView.putExtras(miBundle);

                SeguidosView.this.startActivity(componenteView);
            }
        });

        recycler.setAdapter(rva);
    }
     */

    /**
     * Listener del recycler referente a los scrolls
     */
    private void addListenerRecycler() {

        /*
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
                        ampliarListaComponentes();
                        isLoading = true;
                    }
                }
            }
        });
    }

    /**
     * (No se usa) Serviría para aplicar el filtro una vez se pulse en la lupa, pero como lo hacemos en onQueryTextChange dinámicamente cuando escribe no es necesario, pero sí obligatorio ponerlo por el implements
     *
     * @param query filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    /**
     * Filtra el listado de componentes con cada letra que se escriba en el formulario de búsqueda
     *
     * @param newText filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextChange(String newText) {

        Log.d("Listado antes del clear", String.valueOf(listadoComponentes.size()));
        listadoComponentes.clear();
        rva.notifyDataSetChanged();
        this.filtro = newText;

        if(newText.equals(""))
            iniciarListaComponentes();

        else
            filtrarListaComponentes();

        return true;
    }

    /**
     * Menú superior, con el filtro de componentes y la hamburguesa para la redirección al perfil y el cierre de sesión
     *
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
                //rva.setFilter(listadoComponentes);
                return true;
            }
        });
        return true;
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

    private void setLastVisibleSeguido(DocumentSnapshot last) {
        this.lastVisibleSeguido = last;
    }

    private void addListadoComponentes(Item componente) {
        listadoComponentes.add(componente);
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

