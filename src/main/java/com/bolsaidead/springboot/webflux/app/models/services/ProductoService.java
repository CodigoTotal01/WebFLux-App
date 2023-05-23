package com.bolsaidead.springboot.webflux.app.models.services;

import com.bolsaidead.springboot.webflux.app.models.documents.Categoria;
import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
//Se queda con toda la logica de negocio y la desacopla del controlador
public interface ProductoService {
    public Flux<Producto> findAll();

    public Flux<Producto> findAllConNombreUppercase();


    public Flux<Producto> findAllConNombreUppercaseRepeat();

    public Mono<Producto> findById(String id);
    public Mono<Producto> save(Producto producto);
    public Mono<Void> delete(Producto producto);

    // Categoria
    public Flux<Categoria> findAllCategoria();
    public Mono<Categoria> findCategoriaById(String id);
    public Mono<Categoria> saveCategoria(Categoria categoria);




}
