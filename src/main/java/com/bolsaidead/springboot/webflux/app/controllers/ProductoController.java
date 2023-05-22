package com.bolsaidead.springboot.webflux.app.controllers;

import com.bolsaidead.springboot.webflux.app.models.dao.ProductoDao;
import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;

import java.time.Duration;

// contendra metodos handler para el request
@Controller
public class ProductoController {
    @Autowired
    private ProductoDao dao;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    @GetMapping("/listar")
    public String listar(Model model){
        Flux<Producto> productoFlux = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return  producto;
                });
        productoFlux.subscribe(producto -> log.info(producto.getNombre()));
        // Pasar datos a la vista, tendra una capa de abstraccion reactiva en el servelet
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // Para simular delay
    @GetMapping("/listar-datadriver")
    public String listarDataDriver(Model model){
        Flux<Producto> productoFlux = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return  producto;
                })
                .delayElements(Duration.ofSeconds(1));
        productoFlux.subscribe(producto -> log.info(producto.getNombre()));
        // Pasar datos a la vista, tendra una capa de abstraccion reactiva en el servelet, este caso solo mostrarea el flujo dde dos en dos, cada segundo
        model.addAttribute("productos", new ReactiveDataDriverContextVariable(productoFlux, 2)); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // Manejor de contra precion con chuked
    @GetMapping("/listar-full")
    public String listarFull(Model model){
        Flux<Producto> productoFlux = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return  producto;
                }).repeat(5000);// repetir 5000 veces el flujo
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // Manejor de contra precion con chuked - pero a vista en especifico
    @GetMapping("/listar-chunked")
    public String listarChunked(Model model){
        Flux<Producto> productoFlux = dao.findAll()
                .map(producto -> {
                    producto.setNombre(producto.getNombre().toUpperCase());
                    return  producto;
                }).repeat(5000);// repetir 5000 veces el flujo
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar-chunked"; // indica a que html se le retornara la informacion a de la vista
    }
}

