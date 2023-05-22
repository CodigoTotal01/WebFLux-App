package com.bolsaidead.springboot.webflux.app.controllers;

import com.bolsaidead.springboot.webflux.app.models.dao.ProductoDao;
import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// All information returns with JSON format
@RestController
@RequestMapping("/api/productos")
public class ProductoRestController {
    @Autowired
    private ProductoDao dao;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
    @GetMapping//ruta por defecto
    public Flux<Producto> index () {
        Flux<Producto> productoFlux = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return  producto;
                })
                .doOnNext(producto -> log.info(producto.getNombre()));

        return productoFlux;
    }

    @GetMapping("/{id}")
    public Mono<Producto> show(@PathVariable String id){
//        Mono<Producto> productoMono  = dao.findById(id);
//        return productoMono;
        Flux<Producto> productosFlux = dao.findAll();

        //next emite el primer elemento de un flujo de datos en uno solo
        Mono<Producto> productoMono = productosFlux.filter(producto -> producto.getId().equals(id)).next();
        return productoMono;
    }

}
