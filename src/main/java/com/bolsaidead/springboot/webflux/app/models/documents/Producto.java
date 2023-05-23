package com.bolsaidead.springboot.webflux.app.models.documents;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;
//javax validation para las validaciones, habilidtarlas para la vista en el controlador
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

// Los documentos en mongo se guardAN como BJSON (Binari JSON)
@Document(collection = "productos")
public class Producto {
    @Id
    private String id;

    @NotEmpty
    private String nombre;

    @NotNull
    private Double precio;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createAt;

    //indicamos que el objeto relacional se iten que validar
    @Valid
    private Categoria categoria;

    public String getId() {
        return id;
    }
    //PARA EL MANEJO DE SPRING DATA
    public Producto(){}

    public Producto(String nombre, Double precio) {
        this.nombre = nombre;
        this.precio = precio;
    }

    public Producto(String nombre, Double precio, Categoria categoria) {
//        this.nombre = nombre;
//        this.precio = precio;\
        this(nombre, precio);
        this.categoria = categoria;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }
}
