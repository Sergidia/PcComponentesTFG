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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Map;

public class SeguidosView extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private RecyclerView recycler;
    private ArrayList<Item> listadoComponentes;
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

        Log.d("Listado Seguidos", "Se inicializa la lista de componentes");

        // Se recuperan los 10 primeros documentos de la lista de interés del usuario
        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        // Una vez recuperados se itera sobre el resultado de la query. Cada documento recuperado se corresponde con un documento de 'interés',
                        // por lo que se busca el documento de tipo 'componente' que corresponda y se añade a la lista de componentes.
                        // Además, guardamos el documento de 'interés' en la variable "lastVisibleSeguidos"
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {

                            addComponente(recuperarComponente(document));
                            setLastVisibleSeguido(document);
                        }

                        cargarRecyclerView();
                        isLoading = false;
                    }
                });
    }

    /**
     * Método que devuelve el documento de la colección 'componentes' que tenga el mismo nombre que el documento de 'interés'
     *
     * @param document Documento de interés del usuario
     * @return Documento del componente relacionado con ese interés
     */
    private Task<DocumentSnapshot> recuperarComponente(QueryDocumentSnapshot document) {
        Task<DocumentSnapshot> task = db.collection("componentes")
                .document(document.getId())
                .get();

        return task;
    }

    /**
     * Ampliación de la lista de componentes que se produce cuando el usuario hace scroll y llega al final de la lista actual. Se cargan 10 nuevos componentes a partir del
     * último cargado anteriormente (lastVisibleSeguido).
     */
    private void ampliarListaComponentes() {

        Log.d("Listado Seguidos", "Final del scroll, número de componentes actuales cargados: " + listadoComponentes.size());

        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .startAfter(lastVisibleSeguido)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            for (QueryDocumentSnapshot document : task.getResult()) {

                                addComponente(recuperarComponente(document));
                                setLastVisibleSeguido(document);

                            }
                            rva.notifyDataSetChanged();
                            isLoading = false;
                        }
                    }
                });
    }

    /**
     * Recuperación de los 10 primeros componentes según el filtro del nombre escrito por el usuario. Para el caso de la clase 'Seguidos' podríamos usar el método de
     * iniciarListaComponentes porque el filtro no se aplica a la colección de 'interés', cuyos documentos sólo guardan id y precio, sino que se aplica a la colección de
     * 'componentes'. Por ello el filtro se aplica siempre en el método 'addComponente'. Hemos decidido mantener los métodos separados pese a la repetición del código para
     * hacer más fácil la lectura del mismo y por posibles cambios futuros en la base de datos (por ejemplo, podríamos añadir un campo nombre en el documento de 'interés' y
     * aplicar el filtro directamente en este método, pero decidimos no hacerlo así para no tener dos documentos relacionados con datos repetidos pese a que la base de datos no es relacional
     */
    private void filtrarListaComponentes() {

        Log.d("Filtro Seguidos", "Se solicita recuperar de la base de datos los componentes con filtro: " + filtro);

        db.collection("usuarios")
                .document(email)
                .collection("interes")
                .limit(10)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                addComponente(recuperarComponente(document));
                                setLastVisibleSeguido(document);
                            }
                            cargarRecyclerView();
                            isLoading = false;
                        }
                    }
                });
    }

    /**
     * Añade un componente a la lista de componentes si su nombre coincide con el filtro
     *
     * @param task documento del componente
     */
    public void addComponente(Task<DocumentSnapshot> task) {

        task.addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                String added = "No añadido";

                Log.d("Componente Seguidos", "Documento con id: " + documentSnapshot.getId());

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

                    added = "Añadido";

                } else {

                    String nombre = (String) componente.get("nombre");

                    if (StringUtils.containsIgnoreCase(nombre,filtro)) {

                        Item component = new Item(
                                documentSnapshot.getId(),
                                (String) componente.get("nombre"),
                                (String) componente.get("img"),
                                ((Number) componente.get("precio")).doubleValue(),
                                (String) componente.get("url"),
                                (String) componente.get("categoria"),
                                (Boolean) componente.get("valida"));

                        listadoComponentes.add(component);

                        added = "Añadido";
                    }
                }

                rva.notifyDataSetChanged();
                isLoading = false;

                Log.d("Componente Seguidos", "El documento se corresponde con el componente con nombre: " + componente.get("nombre") + ". " + added);

            }
        });
    }

    /**
     * Crea las fichas de los componentes y redirecciona a la vista ComponentView cuando se selecciona una de ellas
     */
    private void cargarRecyclerView() {

        Log.d("Recycler Seguidos", "Componentes actuales en el listado: " + listadoComponentes.size());

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

        Log.d("Listado Seguidos", "Aplicado filtro: " + query);

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
            public boolean onMenuItemActionExpand(MenuItem item) { return true; }

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
     * Menú inferior con la redirección al listado general de componentes a la lista de seguidos (en este último caso se refresca la vista)
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

                    // No se crea un intent nuevo del tipo 'MainActivity' porque el listado general de componentes siempre se abrirá al iniciar la aplicación,
                    // por lo que cerrando el actual (Seguidos) volveríamos a dicha vista siempre, evitando lecturas innecesarias a la base de datos y manteniendo
                    // al usuario en el lugar del listado donde lo dejó. De querer refrescar la vista lo podría hacer pulsando 'Principal' desde MainActivity
                    finish();
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
    private void setLastVisibleSeguido(DocumentSnapshot last) {
        this.lastVisibleSeguido = last;
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

