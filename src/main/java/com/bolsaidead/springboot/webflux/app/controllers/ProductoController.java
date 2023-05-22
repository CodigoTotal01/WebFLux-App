package com.bolsaidead.springboot.webflux.app.controllers;

import com.bolsaidead.springboot.webflux.app.models.documents.Producto;
import com.bolsaidead.springboot.webflux.app.models.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

// contendra metodos handler para el request
@Controller
public class ProductoController {
    @Autowired
    private ProductoService service;

    private static final Logger log = LoggerFactory.getLogger(ProductoController.class);

    @GetMapping("/listar")
    public Mono<String> listar(Model model){
        Flux<Producto> productoFlux = service.findAllConNombreUppercase();
        productoFlux.subscribe(producto -> log.info(producto.getNombre()));
        // Pasar datos a la vista, tendra una capa de abstraccion reactiva en el servelet
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return Mono.just("listar");
    }
    //registrar
    @GetMapping("/form")
    public Mono<String> crear (Model model){
        model.addAttribute("producto", new Producto());
        model.addAttribute("titulo", "Formulario de producto");
        return Mono.just("form");
    }

    @GetMapping("/form/{id}")
    public Mono<String> editar(@PathVariable String id, Model model){
        Mono<Producto> productoMono = service.findById(id).doOnNext(producto -> {
            log.info("Producto: " + producto.getNombre());
        });
        model.addAttribute("producto", productoMono);
        model.addAttribute("titulo", "Editar  producto");
        return Mono.just("form");
    }



    // Guardar, se enviara los datos que estan poblados en el objeto producto y se hidrata xd
    @PostMapping("/form")
    public Mono<String> guardar(Producto producto){
        return service.save(producto).doOnNext(productoGuardado-> {
            log.info("Producto guardado: " + productoGuardado.getNombre()+ " Id:" + productoGuardado.getId() );
        }).thenReturn("redirect:/listar"); // REtorna un mono string, guarda el valor de la respuesta
    }

    // Para simular delay
    @GetMapping("/listar-datadriver")
    public String listarDataDriver(Model model){
        Flux<Producto> productoFlux = service.findAllConNombreUppercase()
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
        Flux<Producto> productoFlux = service.findAllConNombreUppercaseRepeat();// repetir 5000 veces el flujo
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar";
    }

    // Manejor de contra precion con chuked - pero a vista en especifico
    @GetMapping("/listar-chunked")
    public String listarChunked(Model model){
        Flux<Producto> productoFlux = service.findAllConNombreUppercaseRepeat();// repetir 5000 veces el flujo
        model.addAttribute("productos", productoFlux); // automaticamente se subcribira en gracias a thymelif
        model.addAttribute("titulo", "Listado de productos");
        return "listar-chunked"; // indica a que html se le retornara la informacion a de la vista
    }
}

