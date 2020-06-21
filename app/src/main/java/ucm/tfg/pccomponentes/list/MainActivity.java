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

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.Map;


public class MainActivity extends AppCompatActivity  implements SearchView.OnQueryTextListener{

    private RecyclerView recycler;
    private ArrayList<Item> listadoComponentes;
    String filtro = "";

    private Adapter rva;
    private boolean isLoading;

    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTitle("Listado de componentes");
        super.onCreate(savedInstanceState);

        try {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            // Se comprueba que el usuario está correctamente autenticado, de lo contrario se vuelve a la vista del login
            if (email == null || email.equals("") || email.equals("null")) {
                showAlert("Error de autenticación");
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(), Main.class));
            }

            else {
                setContentView(R.layout.activity_list);
                recycler = findViewById(R.id.listItem);
                addListenerRecycler();

                bottomNavigationView = findViewById(R.id.bottom_navigation);
                bottomNavigationView.setSelectedItemId(R.id.principal);
                setListenerBottomMenu();

                LinearLayoutManager llm = new LinearLayoutManager(this);
                llm.setOrientation(LinearLayoutManager.VERTICAL);
                recycler.setLayoutManager(llm);

                recycler.setHasFixedSize(true);

                listadoComponentes = new ArrayList<>();

                db = FirebaseFirestore.getInstance();
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
     * Inicialización de la lista de componentes recuperando los 10 primeros de la base de datos
     */
    private void iniciarListaComponentes() {

        Log.d("Listado Main", "Se inicializa la lista de componentes");

        db.collection("componentes")
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Ampliación de la lista de componentes que se produce cuando el usuario hace scroll y llega al final de la lista actual. Se cargan 10 nuevos componentes a partir del
     * último cargado anteriormente (lastVisible). También se tiene en cuenta si se ha aplicado un filtro antes de hacer scroll
     */
    private void ampliarListaComponentes() {

        Log.d("Listado Main", "Final del scroll, número de componentes actuales cargados: " + listadoComponentes.size());

        Query docs;

        if (filtro.equals("")) {
            docs = db.collection("componentes")
                    .limit(10)
                    .startAfter(lastVisible);
        }
        else {
            docs = db.collection("componentes")
                    .whereGreaterThanOrEqualTo("nombre", filtro)
                    .limit(10)
                    .startAfter(lastVisible);
        }

        docs.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        rva.notifyDataSetChanged();
                        isLoading = false;
                    }
        });
    }

    /**
     * Recuperación de los 10 primeros componentes según el filtro del nombre escrito por el usuario
     */
    private void filtrarListaComponentes() {

        Log.d("Filtro Main", "Se solicita recuperar de la base de datos los componentes con filtro: " + filtro);

        db.collection("componentes")
                .whereGreaterThanOrEqualTo("nombre", filtro)
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {

                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(document);
                        }
                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Añade un componente a la lista de componentes
     *
     * @param document documento del componente
     */
    public void addComponente(QueryDocumentSnapshot document) {

        Log.d("Componente Main", "Se añade al a lista un documento con id: " + document.getId());

        Map<String,Object> componente = document.getData();
        Item component = new Item(
                document.getId(),
                (String)componente.get("nombre"),
                (String)componente.get("img"),
                ((Number)componente.get("precio")).doubleValue(),
                (String)componente.get("url"),
                (String)componente.get("categoria"),
                (Boolean)componente.get("valida"));

        listadoComponentes.add(component);
        setLastVisible(document);

        Log.d("Componente Main", "El documento se corresponde con el componente con nombre: " + componente.get("nombre"));
    }

    /**
     * Crea las fichas de los componentes y redirecciona a la vista ComponentView cuando se selecciona una de ellas
     */
    private void cargarRecyclerView(){

        this.rva= new Adapter(listadoComponentes);
        rva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent componenteView = new Intent(MainActivity.this, ComponenteView.class);
                Bundle miBundle = new Bundle();
                miBundle.putSerializable("componente", listadoComponentes.get(recycler.getChildAdapterPosition(v)));
                componenteView.putExtras(miBundle);

                MainActivity.this.startActivity(componenteView);
            }
        });

        recycler.setAdapter(rva);
    }

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
     * Filtra el listado de componentes con el texto escrito cuando el usuario pulsa en la lupa
     *
     * @param query filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextSubmit(String query) {

        listadoComponentes.clear();
        rva.notifyDataSetChanged();
        this.filtro = query;

        if(query.equals(""))
            iniciarListaComponentes();

        else
            filtrarListaComponentes();

        Log.d("Listado Main", "Aplicado filtro: " + query);

        return true;
    }

    /**
     * Filtra el listado de componentes con cada letra que se escriba en el formulario de búsqueda. No lo usamos
     * por bugs encontrados cuando se escribe rápidamente desde Android Studio con el teclado físico, que provoca
     * que se duplique, triplique... la información. Si se escribe a una velocidad normal no ocurre
     *
     * @param newText filtro escrito por el usuario
     * @return true
     */
    @Override
    public boolean onQueryTextChange(String newText) { return true; }

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
            public boolean onMenuItemActionCollapse(MenuItem item) { return true; }
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

            // Para el caso de cierre de sesión se añade un flag que cierra todas las ventanas abiertas anteriormente
            case R.id.opCerrarSesion:
                FirebaseAuth.getInstance().signOut();
                i = new Intent(getApplicationContext(), Main.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
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

                    Intent i = new Intent(getApplicationContext(), SeguidosView.class);
                    startActivity(i);
                    overridePendingTransition(0,0);

                    return true;
                }
                else if(item.getItemId() == R.id.principal){

                    recreate();
                    overridePendingTransition(0,0);

                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Método que almacena el último documento cargado en la lista, de esta forma cuando se llega al final del scroll cargamos 10 nuevos componentes desde el último de ellos.
     * Este método nos permite almacenar el documento desde dentro de los Listener
     *
     * @param last Documento Firebase
     */
    private void setLastVisible(DocumentSnapshot last) {
        this.lastVisible = last;
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
