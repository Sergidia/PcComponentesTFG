package ucm.tfg.pccomponentes.list;

import ucm.tfg.pccomponentes.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
implements View.OnClickListener{

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_LOADING = 1;
    private ArrayList<Item> listDatos;
    private View.OnClickListener listener;

    public Adapter(ArrayList<Item> listDatos) {
        this.listDatos = listDatos;
    }

    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        RecyclerView.ViewHolder v=null;
        if(i == VIEW_TYPE_ITEM){
          View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_list,null,false);
          view.setOnClickListener(this);
          v = new ViewHolderDatos(view);
      }
      else {
          View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_loading,null,false);
          v = new ViewHolderCarga(view);
        }
        return v;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if(holder instanceof ViewHolderDatos){
                adaptarItem((ViewHolderDatos) holder,position);

            }
            else if(holder instanceof  ViewHolderCarga){
                showLoadingView((ViewHolderCarga) holder, position);
            }


    }

    private void showLoadingView(ViewHolderCarga holder, int position) {

    }

    private void adaptarItem(ViewHolderDatos vhd, int pos) {
        vhd.categoria.setText(listDatos.get(pos).getCategoria());
        vhd.nombre.setText(listDatos.get(pos).getNombre());
        Picasso.get()
                .load(listDatos.get(pos).getFoto())
                .resize(300,300)
                .into(vhd.foto);
        vhd.precio.setText(String.valueOf(listDatos.get(pos).getPrecio()));
    }


    @Override
    public int getItemCount() {
        return listDatos == null ? 0 : listDatos.size();
    }
    public void setOnClickListener(View.OnClickListener listen){
        this.listener = listen;
    }
    @Override
    public void onClick(View v) {
        if(listener != null){
           listener.onClick(v);
        }
    }
    public int getItemViewType(int position) {
        return listDatos.get(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_ITEM;
    }

    private class ViewHolderDatos extends RecyclerView.ViewHolder {
        private TextView nombre,categoria,precio;
        private ImageView foto;
        ViewHolderDatos(@NonNull View itemView) {
            super(itemView);
            nombre = (TextView)itemView.findViewById(R.id.idDocum);
            categoria= (TextView)itemView.findViewById(R.id.idDescr);
            foto= (ImageView)itemView.findViewById(R.id.idImagen);
            precio=(TextView)itemView.findViewById(R.id.idPrecio);
        }

    }
    private class ViewHolderCarga extends RecyclerView.ViewHolder{
        private ProgressBar barra;
        public ViewHolderCarga(@NonNull View itemView) {
            super(itemView);
            barra = itemView.findViewById(R.id.progressBar);
        }
    }
    public void setFilter(ArrayList<Item> ali){
        this.listDatos = ali;
        notifyDataSetChanged();
    }
}
