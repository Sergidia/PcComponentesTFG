package ucm.tfg.pccomponentes.list;

import java.io.Serializable;

public class Item implements Serializable {
    private String codigo;
    private String nombre;
    private String foto;
    private String url;
    private String categoria;
    private boolean  valida;
    private double precio;

    public Item(String codigo, String nombre, String foto,double precio, String url, String categoria, boolean valida) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.foto = foto;
        this.precio = precio;
        this.categoria = categoria;
        this.url = url;
        this.valida = valida;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public boolean isValida() {
        return valida;
    }

    public void setValida(boolean valida) {
        this.valida = valida;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }
}
